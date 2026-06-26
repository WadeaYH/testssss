package com.wadea.todocicd.unit;

import com.wadea.todocicd.dto.TodoRequest;
import com.wadea.todocicd.dto.TodoResponse;
import com.wadea.todocicd.exception.TodoNotFoundException;
import com.wadea.todocicd.model.Todo;
import com.wadea.todocicd.repository.TodoRepository;
import com.wadea.todocicd.service.TodoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TodoServiceTest - UNIT tests for TodoServiceImpl.
 *
 * WHY this is called a "unit" test (and lives in the `unit` package): it
 * tests exactly ONE class (TodoServiceImpl) completely in isolation. Its only
 * collaborator, TodoRepository, is entirely faked out with Mockito - no
 * Spring context is started, no real/in-memory database is touched, and no
 * HTTP layer is involved at all. This makes these tests extremely fast
 * (milliseconds) and lets them pinpoint business-logic bugs precisely inside
 * the service layer, independent of persistence or web concerns. Compare
 * with TodoControllerIntegrationTest, which deliberately tests several real
 * layers wired together at once.
 */
@ExtendWith(MockitoExtension.class)
// @ExtendWith(MockitoExtension.class): WHY this is required - it's what
// activates Mockito's annotation processing (@Mock, @InjectMocks below) for
// this test class, and automatically calls Mockito.openMocks(...)/closes
// resources for us before/after each test, so we never have to do that
// bookkeeping by hand.
@DisplayName("TodoService unit tests")
class TodoServiceTest {

    /**
     * @Mock: creates a fake TodoRepository. By default every method on it
     * does nothing and returns null/empty/zero - we explicitly program the
     * exact responses we want with when(...).thenReturn(...) inside each test
     * below. WHY mock this rather than using a real repository/database:
     * these tests should verify TodoServiceImpl's OWN logic only (e.g. "does
     * it throw TodoNotFoundException when the repository says the row isn't
     * there"), not whether Hibernate/H2 work correctly - that's exactly what
     * the integration tests are for instead.
     */
    @Mock
    private TodoRepository todoRepository;

    /**
     * The real, actual implementation under test - NOT a mock.
     * @InjectMocks: Mockito creates a genuine TodoServiceImpl instance and
     * injects the @Mock TodoRepository above into it automatically (using
     * its constructor, since TodoServiceImpl takes TodoRepository as a
     * constructor parameter via Lombok's @RequiredArgsConstructor). This is
     * the standard @Mock + @InjectMocks pairing used throughout the Mockito
     * ecosystem: real object under test, fake collaborators around it.
     */
    @InjectMocks
    private TodoServiceImpl todoService;

    // Shared sample data reused across multiple tests, set up fresh before
    // each individual test method runs (see @BeforeEach below).
    private Todo sampleTodo;

    /**
     * @BeforeEach: runs before EVERY single @Test method in this class.
     * WHY: it guarantees each test starts from the exact same known state,
     * so tests can never accidentally influence each other depending on
     * execution order (test order independence is a core property of a
     * trustworthy test suite). Note we only need to (re)build `sampleTodo`
     * here - `todoService` and `todoRepository` are already freshly created
     * for us by MockitoExtension before every single test method.
     */
    @BeforeEach
    void setUp() {
        sampleTodo = Todo.builder()
                .id(1L)
                .title("Buy milk")
                .description("2% milk, 1 gallon")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getAllTodos() returns every todo mapped to TodoResponse")
    void getAllTodos_returnsAllTodosMappedToResponse() {
        // ARRANGE: tell the mock repository exactly what to return when
        // findAll() is called - simulating "the database currently has
        // exactly one row."
        when(todoRepository.findAll()).thenReturn(List.of(sampleTodo));

        // ACT: call the real method under test.
        List<TodoResponse> result = todoService.getAllTodos();

        // ASSERT: verify both the size AND the actual field-by-field mapping
        // from entity -> response DTO worked correctly.
        assertEquals(1, result.size());
        assertEquals(sampleTodo.getId(), result.get(0).getId());
        assertEquals(sampleTodo.getTitle(), result.get(0).getTitle());
        assertEquals(sampleTodo.getDescription(), result.get(0).getDescription());
        assertEquals(sampleTodo.getCompleted(), result.get(0).getCompleted());

        // verify(...): confirms the service actually called
        // todoRepository.findAll() exactly once - i.e. it genuinely delegated
        // to the repository instead of, say, returning a hardcoded list.
        verify(todoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getTodoById() returns the matching todo when it exists")
    void getTodoById_whenTodoExists_returnsTodoResponse() {
        // ARRANGE: simulate "a row with id=1 exists" by wrapping it in an
        // Optional, exactly like the real Spring Data JPA method signature.
        when(todoRepository.findById(1L)).thenReturn(Optional.of(sampleTodo));

        // ACT
        TodoResponse result = todoService.getTodoById(1L);

        // ASSERT
        assertEquals(1L, result.getId());
        assertEquals("Buy milk", result.getTitle());
        verify(todoRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getTodoById() throws TodoNotFoundException when no todo matches")
    void getTodoById_whenTodoMissing_throwsTodoNotFoundException() {
        // ARRANGE: simulate "no row with this id" - Optional.empty() is
        // exactly what Spring Data JPA's findById returns in that case.
        when(todoRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT combined: assertThrows runs the lambda and fails the
        // test unless it throws EXACTLY the given exception type.
        TodoNotFoundException exception = assertThrows(
                TodoNotFoundException.class,
                () -> todoService.getTodoById(99L)
        );

        // Also assert on the message, so we know the exception is genuinely
        // informative and not just the right type with empty/wrong content.
        assertEquals("Todo not found with id: 99", exception.getMessage());
    }

    @Test
    @DisplayName("createTodo() saves a new todo and defaults completed to false when omitted")
    void createTodo_savesNewTodoAndReturnsResponse() {
        // ARRANGE: the request omits `completed` entirely (null) - we expect
        // the service to apply the "default to false" business rule itself.
        TodoRequest request = TodoRequest.builder()
                .title("Walk the dog")
                .description("Around the block")
                .completed(null)
                .build();

        // The mock repository's save() simply returns whatever Todo entity
        // it's given, but with id/createdAt populated - simulating what a
        // real database INSERT + identity column would do.
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
            Todo todoBeingSaved = invocation.getArgument(0);
            todoBeingSaved.setId(42L);
            return todoBeingSaved;
        });

        // ACT
        TodoResponse result = todoService.createTodo(request);

        // ASSERT: the returned response reflects both our input and the
        // service's default-completed-to-false business rule.
        assertEquals(42L, result.getId());
        assertEquals("Walk the dog", result.getTitle());
        assertFalse(result.getCompleted());

        // ArgumentCaptor: lets us inspect the EXACT Todo object the service
        // passed into todoRepository.save(...), so we can assert the service
        // built it correctly BEFORE persistence even happens.
        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(todoCaptor.capture());
        assertFalse(todoCaptor.getValue().getCompleted());
    }

    @Test
    @DisplayName("updateTodo() overwrites fields and returns the updated todo when it exists")
    void updateTodo_whenTodoExists_updatesAndReturnsResponse() {
        // ARRANGE: the repository "has" sampleTodo (id=1) when looked up...
        when(todoRepository.findById(1L)).thenReturn(Optional.of(sampleTodo));
        // ...and save() just returns whatever entity it's handed (simulating
        // an UPDATE that succeeds and returns the same row).
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TodoRequest request = TodoRequest.builder()
                .title("Buy oat milk instead")
                .description("Almond is fine too")
                .completed(true)
                .build();

        // ACT
        TodoResponse result = todoService.updateTodo(1L, request);

        // ASSERT: every field was overwritten with the new request's values.
        assertEquals(1L, result.getId());
        assertEquals("Buy oat milk instead", result.getTitle());
        assertEquals("Almond is fine too", result.getDescription());
        assertTrue(result.getCompleted());

        verify(todoRepository, times(1)).findById(1L);
        verify(todoRepository, times(1)).save(any(Todo.class));
    }

    @Test
    @DisplayName("updateTodo() throws TodoNotFoundException when no todo matches")
    void updateTodo_whenTodoMissing_throwsTodoNotFoundException() {
        when(todoRepository.findById(99L)).thenReturn(Optional.empty());
        TodoRequest request = TodoRequest.builder().title("Doesn't matter").build();

        assertThrows(
                TodoNotFoundException.class,
                () -> todoService.updateTodo(99L, request)
        );

        // WHY this verify(...never()) matters: it proves the service bailed
        // out BEFORE attempting to save anything when the todo doesn't
        // exist - i.e. it never issues a pointless/incorrect write.
        verify(todoRepository, never()).save(any(Todo.class));
    }

    @Test
    @DisplayName("deleteTodo() deletes the todo when it exists")
    void deleteTodo_whenTodoExists_deletesSuccessfully() {
        // ARRANGE: simulate "row with id=1 exists."
        when(todoRepository.existsById(1L)).thenReturn(true);

        // ACT
        todoService.deleteTodo(1L);

        // ASSERT: confirm the actual delete call happened with the right id.
        verify(todoRepository, times(1)).existsById(1L);
        verify(todoRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTodo() throws TodoNotFoundException when no todo matches")
    void deleteTodo_whenTodoMissing_throwsTodoNotFoundException() {
        // ARRANGE: simulate "no row with this id exists."
        when(todoRepository.existsById(99L)).thenReturn(false);

        // ACT + ASSERT
        assertThrows(
                TodoNotFoundException.class,
                () -> todoService.deleteTodo(99L)
        );

        // WHY verify deleteById was NEVER called: this is the most important
        // assertion in this test - it proves the service checks existence
        // FIRST and never blindly issues a DELETE for a row that was never
        // there to begin with.
        verify(todoRepository, never()).deleteById(anyLong());
    }
}
