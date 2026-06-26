package com.wadea.todocicd.service;

import com.wadea.todocicd.dto.TodoRequest;
import com.wadea.todocicd.dto.TodoResponse;

import java.util.List;

/**
 * TodoService - the contract for all Todo business operations.
 *
 * WHY define an interface separately from its implementation
 * (TodoServiceImpl), instead of just writing one concrete class:
 *   1. TESTABILITY: TodoController depends on this interface, not the
 *      concrete class. In tests we can inject a mock/fake implementation
 *      without touching any real database or any controller code.
 *   2. DECOUPLING: callers (the controller) only need to know WHAT
 *      operations are available, never HOW they're implemented. We could
 *      swap in a completely different TodoServiceImpl (e.g. one backed by a
 *      remote API instead of JPA) without changing the controller at all.
 *   3. CONVENTION: this "interface + Impl" pattern is the standard, widely
 *      recognized shape of a Spring service layer, making the codebase
 *      immediately familiar to any other Spring developer.
 *
 * WHY every method here works with DTOs (TodoRequest/TodoResponse), never
 * the Todo entity directly: this interface IS the boundary between our
 * persistence model (Todo, which only the repository/service should ever
 * touch) and the rest of the application. Keeping entities fully contained
 * within the service layer means the controller (and anything calling this
 * service in the future) never accidentally depends on JPA-specific details.
 */
public interface TodoService {

    /**
     * @return every todo currently stored, mapped to its response shape.
     */
    List<TodoResponse> getAllTodos();

    /**
     * @param id the todo's database id
     * @return the matching todo
     * @throws com.wadea.todocicd.exception.TodoNotFoundException if no todo
     *         exists with that id
     */
    TodoResponse getTodoById(Long id);

    /**
     * Creates and persists a brand-new todo.
     *
     * @param request the incoming title/description/completed payload
     * @return the newly created todo, including its generated id and createdAt
     */
    TodoResponse createTodo(TodoRequest request);

    /**
     * Overwrites an existing todo's fields with the given request's values.
     *
     * @param id      the todo to update
     * @param request the new title/description/completed values
     * @return the updated todo
     * @throws com.wadea.todocicd.exception.TodoNotFoundException if no todo
     *         exists with that id
     */
    TodoResponse updateTodo(Long id, TodoRequest request);

    /**
     * Permanently removes a todo.
     *
     * @param id the todo to delete
     * @throws com.wadea.todocicd.exception.TodoNotFoundException if no todo
     *         exists with that id
     */
    void deleteTodo(Long id);
}
