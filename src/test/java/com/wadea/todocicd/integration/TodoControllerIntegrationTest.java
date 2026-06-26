package com.wadea.todocicd.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wadea.todocicd.dto.TodoRequest;
import com.wadea.todocicd.model.Todo;
import com.wadea.todocicd.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TodoControllerIntegrationTest - INTEGRATION tests for the full Todo API.
 *
 * WHY this is called an "integration" test (and lives in the `integration`
 * package), unlike TodoServiceTest: it wires together SEVERAL real layers at
 * once - the actual TodoController, the actual TodoServiceImpl, the actual
 * TodoRepository/Hibernate, and a real (in-memory) H2 database - and drives
 * them all through real HTTP-style requests. Nothing is mocked here. This
 * verifies that all the pieces are correctly wired together AND that our
 * configuration (application.properties, component scanning, JSON
 * serialization, exception handling) actually works end-to-end - things a
 * pure unit test, by design, cannot tell us.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
// @SpringBootTest: boots the ENTIRE real Spring application context, exactly
// like running the app for real - every @Component/@Service/@RestController/
// @RestControllerAdvice gets created and wired together.
// webEnvironment = MOCK (the default): WHY we don't need
// webEnvironment = RANDOM_PORT/DEFINED_PORT here - we are not making real
// network/socket calls. MockMvc (below) simulates HTTP requests directly
// against Spring MVC's dispatcher in-memory, which is faster and doesn't
// require an actual open TCP port, while still exercising the real
// controller/filter/serialization pipeline.
@AutoConfigureMockMvc
// @AutoConfigureMockMvc: tells Spring Boot to create and configure a MockMvc
// bean for us automatically, fully wired to the application context above,
// so we can simply @Autowired it instead of building it by hand.
@DisplayName("Todo API integration tests")
class TodoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    // MockMvc: lets us issue requests like "GET /api/todos" and assert on the
    // resulting status code/headers/JSON body, without starting a real
    // network server - it's the standard tool for testing Spring MVC
    // controllers thoroughly.

    @Autowired
    private TodoRepository todoRepository;
    // We also inject the REAL repository directly (not mocked) so our tests
    // can independently set up and verify database state, in addition to
    // whatever the HTTP responses themselves tell us.

    @Autowired
    private ObjectMapper objectMapper;
    // ObjectMapper: the same Jackson component Spring Boot uses internally to
    // convert Java objects <-> JSON. We reuse it here to turn our TodoRequest
    // Java objects into JSON strings to send as request bodies.

    /**
     * @BeforeEach: runs before every single @Test below.
     * WHY we manually clear the table here: @SpringBootTest reuses the SAME
     * application context (and therefore the same in-memory H2 database)
     * across every test method in this class for speed - Spring's
     * ddl-auto=create-drop schema creation only happens ONCE, when the
     * context first starts, not per test. Without this cleanup, data created
     * by one test (e.g. createTodo_...) would leak into and break the
     * assumptions of a completely unrelated test that runs after it
     * (e.g. getAllTodos_whenNoTodos_...). Deleting everything before each
     * test guarantees full test isolation regardless of execution order.
     */
    @BeforeEach
    void cleanDatabase() {
        todoRepository.deleteAll();
    }

    // -------------------------------------------------------------------
    // GET /api/todos
    // -------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/todos returns an empty list when there are no todos")
    void getAllTodos_whenNoneExist_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/todos returns every existing todo")
    void getAllTodos_whenSomeExist_returnsThemAll() throws Exception {
        // ARRANGE: insert two real rows directly through the repository.
        todoRepository.save(Todo.builder().title("Buy milk").completed(false).build());
        todoRepository.save(Todo.builder().title("Walk the dog").completed(true).build());

        // ACT + ASSERT
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
        // WHY we don't assert exact array ORDER here: findAll() makes no
        // ordering guarantee unless we explicitly ask for one (e.g. via
        // Sort), so a robust test should not depend on row order.
    }

    // -------------------------------------------------------------------
    // GET /api/todos/{id}
    // -------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/todos/{id} returns the matching todo when it exists")
    void getTodoById_whenExists_returnsTodo() throws Exception {
        Todo saved = todoRepository.save(
                Todo.builder().title("Buy milk").description("Whole milk").completed(false).build());

        mockMvc.perform(get("/api/todos/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.description").value("Whole milk"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("GET /api/todos/{id} returns 404 with a clear error body when missing")
    void getTodoById_whenMissing_returns404() throws Exception {
        mockMvc.perform(get("/api/todos/{id}", 999L))
                .andExpect(status().isNotFound()) // HTTP 404
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Todo not found with id: 999"))
                .andExpect(jsonPath("$.path").value("/api/todos/999"));
        // WHY we assert on the full ErrorResponse shape, not just the status
        // code: this confirms GlobalExceptionHandler is genuinely wired up
        // end-to-end and producing our consistent, structured error format -
        // not just that Spring's own default fallback error page kicked in.
    }

    // -------------------------------------------------------------------
    // POST /api/todos
    // -------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/todos creates a new todo and returns 201 with a Location header")
    void createTodo_withValidRequest_returns201AndPersists() throws Exception {
        TodoRequest request = TodoRequest.builder()
                .title("Read a book")
                .description("Finish chapter 3")
                .build(); // completed deliberately omitted -> should default to false

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // HTTP 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Read a book"))
                .andExpect(jsonPath("$.description").value("Finish chapter 3"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.createdAt").exists());

        // Cross-check directly against the database too, not just the HTTP
        // response - proving the data was genuinely persisted, not just
        // echoed back in the response without being saved.
        List<Todo> allTodos = todoRepository.findAll();
        assertEquals(1, allTodos.size());
        assertEquals("Read a book", allTodos.get(0).getTitle());
        assertFalse(allTodos.get(0).getCompleted());
    }

    @Test
    @DisplayName("POST /api/todos with a blank title returns 400 and saves nothing")
    void createTodo_withBlankTitle_returns400() throws Exception {
        // ARRANGE: violates @NotBlank on TodoRequest.title.
        TodoRequest invalidRequest = TodoRequest.builder().title("   ").build();

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()) // HTTP 400
                .andExpect(jsonPath("$.status").value(400));

        // WHY this assertion matters as much as the status code: it proves
        // invalid input is rejected at the validation boundary and NEVER
        // reaches the database at all.
        assertEquals(0, todoRepository.count());
    }

    // -------------------------------------------------------------------
    // PUT /api/todos/{id}
    // -------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/todos/{id} updates an existing todo and returns 200")
    void updateTodo_whenExists_returns200AndUpdates() throws Exception {
        Todo saved = todoRepository.save(
                Todo.builder().title("Old title").description("Old description").completed(false).build());

        TodoRequest updateRequest = TodoRequest.builder()
                .title("New title")
                .description("New description")
                .completed(true)
                .build();

        mockMvc.perform(put("/api/todos/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.completed").value(true));

        // Confirm the change was actually committed to the database, by
        // re-reading it directly through the repository.
        Todo reloaded = todoRepository.findById(saved.getId()).orElseThrow();
        assertEquals("New title", reloaded.getTitle());
        assertTrue(reloaded.getCompleted());
    }

    @Test
    @DisplayName("PUT /api/todos/{id} returns 404 when the todo doesn't exist")
    void updateTodo_whenMissing_returns404() throws Exception {
        TodoRequest updateRequest = TodoRequest.builder().title("Doesn't matter").build();

        mockMvc.perform(put("/api/todos/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Todo not found with id: 999"));
    }

    // -------------------------------------------------------------------
    // DELETE /api/todos/{id}
    // -------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/todos/{id} removes an existing todo and returns 204")
    void deleteTodo_whenExists_returns204AndRemoves() throws Exception {
        Todo saved = todoRepository.save(Todo.builder().title("Temporary todo").completed(false).build());

        mockMvc.perform(delete("/api/todos/{id}", saved.getId()))
                .andExpect(status().isNoContent()) // HTTP 204
                .andExpect(content().string("")); // no response body at all
        // WHY content().string("") instead of jsonPath(...) here: jsonPath
        // tries to PARSE the response body as JSON first, which throws a
        // parse error on a genuinely empty body instead of failing the
        // assertion cleanly. content().string("") is the correct way to
        // assert "the body is empty" for a 204 response.

        // WHY check the repository afterward: proves the row is genuinely
        // gone from the database, not merely that the HTTP layer claimed
        // success.
        assertFalse(todoRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("DELETE /api/todos/{id} returns 404 when the todo doesn't exist")
    void deleteTodo_whenMissing_returns404() throws Exception {
        mockMvc.perform(delete("/api/todos/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Todo not found with id: 999"));
    }
}
