package com.wadea.todocicd.service;

import com.wadea.todocicd.dto.TodoRequest;
import com.wadea.todocicd.dto.TodoResponse;
import com.wadea.todocicd.exception.TodoNotFoundException;
import com.wadea.todocicd.model.Todo;
import com.wadea.todocicd.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TodoServiceImpl - the concrete implementation of TodoService.
 *
 * This is where all our BUSINESS LOGIC lives: looking todos up, deciding what
 * counts as "not found", deciding how a create/update request maps onto the
 * Todo entity, and converting entities into the DTOs the rest of the app
 * exchanges with the outside world.
 */
@Service
// @Service: a specialization of @Component. WHY use @Service specifically
// rather than the generic @Component - it has no technical difference today,
// but it documents INTENT: this class holds business logic, which makes the
// codebase easier to navigate ("the service layer is everything annotated
// @Service"). Spring also registers it as a singleton bean, so exactly one
// instance is created and shared/injected everywhere it's needed.
@RequiredArgsConstructor
// Lombok: generates a constructor that takes every `final` field below
// (here, just `todoRepository`) as a parameter.
// WHY constructor injection instead of field injection (@Autowired directly
// on the field):
//   1. It lets `todoRepository` be `final` - guaranteed to be set exactly
//      once and never reassigned, which the compiler enforces for us.
//   2. It makes this class's dependencies explicit and visible in its public
//      API (the constructor signature), instead of hidden as private fields
//      that only reflection-based DI can populate.
//   3. It makes unit testing trivial: in TodoServiceTest we simply call
//      `new TodoServiceImpl(mockRepository)` directly - no Spring container,
//      no reflection hacks, required at all.
@Transactional
// @Transactional at the CLASS level: every public method below runs inside a
// database transaction by default. WHY this matters even for single-statement
// methods like getTodoById: it ensures consistent read behaviour and means
// if we ever extend a method later to perform multiple repository calls (e.g.
// update two related rows), they automatically either all succeed together or
// all roll back together - we don't have to remember to add @Transactional
// later and risk a half-applied change.
public class TodoServiceImpl implements TodoService {

    /**
     * WHY `final`: combined with @RequiredArgsConstructor above, this
     * guarantees the repository reference can never be null after
     * construction and can never be reassigned later - it's immutable for
     * the lifetime of this service bean.
     */
    private final TodoRepository todoRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    // readOnly = true: WHY add this on top of the class-level @Transactional -
    // it's a hint to Hibernate/the database driver that this method will
    // never INSERT/UPDATE/DELETE anything. Hibernate can then skip "dirty
    // checking" (the bookkeeping it normally does to detect changed entities
    // that need to be saved), which is a small but free performance win for
    // pure read operations like this one.
    public List<TodoResponse> getAllTodos() {
        // findAll() -> SELECT * FROM todos, returns every row as a Todo entity.
        return todoRepository.findAll()
                .stream() // turn the List<Todo> into a Stream<Todo> so we can transform it
                .map(this::mapToResponse) // convert each Todo entity -> TodoResponse DTO
                .toList(); // collect back into an immutable List<TodoResponse> (Java 16+)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public TodoResponse getTodoById(Long id) {
        Todo todo = todoRepository.findById(id)
                // findById returns an Optional<Todo> - WHY: it forces callers
                // to explicitly handle the "not found" case at compile time,
                // instead of silently risking a NullPointerException later.
                .orElseThrow(() -> new TodoNotFoundException(id));
                // orElseThrow: if the Optional is empty (no row with this id),
                // immediately throw our custom exception. This is the ONE
                // place getTodoById can fail, and it fails loudly and clearly.
        return mapToResponse(todo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TodoResponse createTodo(TodoRequest request) {
        // Build a brand-new entity from the incoming request. Note we never
        // set `id` (the database assigns it) or `createdAt` (Todo's
        // @PrePersist hook assigns it automatically - see Todo.java).
        Todo todo = Todo.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                // WHY default to false here instead of leaving it null:
                // a client is allowed to omit `completed` entirely when
                // creating a todo (it's not a required field on TodoRequest),
                // and "a brand-new todo defaults to not completed" is exactly
                // the business rule stated in the requirements. This is a
                // business decision, so it belongs here in the service layer,
                // not hardcoded into the DTO or the entity.
                .completed(request.getCompleted() != null ? request.getCompleted() : false)
                .build();

        // save(): since `todo.id` is null, Spring Data JPA/Hibernate knows
        // this is a brand-new row and generates an INSERT statement. The
        // returned object is the SAME entity but now with its
        // database-generated `id` and `createdAt` populated.
        Todo saved = todoRepository.save(todo);
        return mapToResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TodoResponse updateTodo(Long id, TodoRequest request) {
        // Re-use the same "find or throw" logic as getTodoById: we can only
        // update a todo that genuinely exists.
        Todo existingTodo = todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));

        // Mutate the MANAGED entity's fields directly (Lombok's @Data gave us
        // these setters on Todo).
        // WHY this works without calling save() explicitly at the end: inside
        // an @Transactional method, `existingTodo` is a "managed" entity -
        // Hibernate is actively tracking it. Any field changes made to it
        // are automatically detected ("dirty checking") and flushed as an
        // UPDATE statement when the transaction commits at the end of this
        // method. We call save() anyway below purely for clarity/explicitness
        // for readers of this code, even though it's technically redundant.
        existingTodo.setTitle(request.getTitle());
        existingTodo.setDescription(request.getDescription());
        // For update, unlike create, a null `completed` in the request simply
        // means "leave it unchanged" would also be a defensible design - but
        // here we treat the request as the full, authoritative new state
        // (a "PUT replaces the whole resource" semantic, which is the correct
        // interpretation of HTTP PUT), defaulting a missing value to false
        // exactly like createTodo does, for consistency between both methods.
        existingTodo.setCompleted(request.getCompleted() != null ? request.getCompleted() : false);
        // Note: we deliberately never touch existingTodo.createdAt here - see
        // @Column(updatable = false) on Todo.createdAt, which makes this
        // field immutable at the database level regardless.

        Todo saved = todoRepository.save(existingTodo);
        return mapToResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTodo(Long id) {
        // WHY check existence first instead of just calling
        // todoRepository.deleteById(id) directly: deleteById() silently does
        // nothing if the id doesn't exist (it issues a DELETE that simply
        // matches zero rows) - it would NEVER throw, leaving the controller
        // unable to tell the client "that todo never existed" with a 404.
        // Explicitly checking existsById() first lets us surface that as a
        // clear TodoNotFoundException, consistent with every other method
        // in this service.
        if (!todoRepository.existsById(id)) {
            throw new TodoNotFoundException(id);
        }
        todoRepository.deleteById(id);
    }

    /**
     * Converts a Todo JPA entity into the TodoResponse DTO clients receive.
     *
     * WHY centralize this mapping in one private helper instead of repeating
     * the same five-field copy in every public method above: classic DRY -
     * if TodoResponse ever gains a new field, there is exactly one place in
     * this entire class that needs to change.
     */
    private TodoResponse mapToResponse(Todo todo) {
        return TodoResponse.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .description(todo.getDescription())
                .completed(todo.getCompleted())
                .createdAt(todo.getCreatedAt())
                .build();
    }
}
