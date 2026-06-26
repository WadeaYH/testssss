package com.wadea.todocicd.controller;

import com.wadea.todocicd.dto.TodoRequest;
import com.wadea.todocicd.dto.TodoResponse;
import com.wadea.todocicd.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * TodoController - the HTTP entry point for everything Todo-related.
 *
 * WHY this class is deliberately "thin" (almost no logic of its own): a
 * controller's only job is the HTTP plumbing - reading the incoming
 * request, calling exactly one service method, and wrapping whatever comes
 * back into the right ResponseEntity/status code. ALL business logic and ALL
 * database access lives in TodoService/TodoServiceImpl instead. This
 * separation is what makes it possible to unit-test business logic
 * (TodoServiceTest) completely independently of HTTP/Spring MVC, and to
 * integration-test the HTTP layer (TodoControllerIntegrationTest) without
 * duplicating business-rule assertions.
 */
@RestController
// @RestController = @Controller + @ResponseBody. WHY: it tells Spring this
// class handles HTTP requests, AND that every method's return value should be
// serialized directly into the HTTP response body (as JSON, via Jackson)
// rather than treated as the name of a server-rendered view template.
@RequestMapping("/api/todos")
// WHY a class-level @RequestMapping: every endpoint in this controller shares
// the "/api/todos" prefix, so we declare it exactly once here instead of
// repeating it on all five methods below.
@RequiredArgsConstructor
// Lombok constructor injection for `todoService` - see TodoServiceImpl for
// the full explanation of why constructor injection is preferred.
public class TodoController {

    private final TodoService todoService;

    /**
     * GET /api/todos
     * WHY 200 OK even when the list is empty: an empty list is still a
     * perfectly valid, successful result ("you have zero todos") - it is NOT
     * an error condition, so it must never be a 404.
     */
    @GetMapping
    public ResponseEntity<List<TodoResponse>> getAllTodos() {
        List<TodoResponse> todos = todoService.getAllTodos();
        return ResponseEntity.ok(todos); // ResponseEntity.ok(body) == status 200 + this body
    }

    /**
     * GET /api/todos/{id}
     * WHY no explicit error handling here for "not found": if
     * todoService.getTodoById throws TodoNotFoundException, we deliberately
     * let it propagate straight out of this method - GlobalExceptionHandler
     * catches it and turns it into a 404 response. This method only ever
     * needs to describe the HAPPY path.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodoById(@PathVariable Long id) {
        // @PathVariable: binds the "{id}" segment of the URL to this method
        // parameter. Spring automatically converts the URL's string segment
        // into a Long for us, and returns a 400 on its own if it isn't
        // numeric at all (e.g. "/api/todos/abc").
        TodoResponse todo = todoService.getTodoById(id);
        return ResponseEntity.ok(todo);
    }

    /**
     * POST /api/todos
     * WHY 201 Created (not 200 OK): per HTTP semantics, 201 specifically
     * means "a new resource was created as a result of this request," which
     * is exactly what happened here - 200 would be technically misleading.
     * WHY we also set a Location header pointing at the new resource's URL:
     * this is standard REST practice that lets a client immediately know
     * where to GET/PUT/DELETE the resource it just created, without having
     * to guess the URL pattern itself.
     */
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
            @Valid @RequestBody TodoRequest request,
            UriComponentsBuilder uriBuilder) {
        // @Valid: triggers Bean Validation on the incoming `request` BEFORE
        // this method body even runs. If TodoRequest.title is blank, Spring
        // throws MethodArgumentNotValidException automatically - handled by
        // GlobalExceptionHandler - and createTodo()'s body never executes at
        // all for invalid input.
        // @RequestBody: tells Spring "deserialize the raw JSON HTTP request
        // body into a TodoRequest object" (via Jackson), instead of looking
        // for URL query parameters.
        TodoResponse created = todoService.createTodo(request);

        URI location = uriBuilder
                .path("/api/todos/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        // Builds an absolute URI like "http://localhost:8080/api/todos/7"
        // from the newly created todo's generated id.

        return ResponseEntity.created(location).body(created);
        // ResponseEntity.created(uri) == status 201 + a "Location: <uri>" header.
    }

    /**
     * PUT /api/todos/{id}
     * WHY PUT (not PATCH) matches our service's behavior: PUT means "replace
     * this resource's full state with what I'm sending you," which is
     * exactly what TodoServiceImpl.updateTodo does (it overwrites
     * title/description/completed wholesale, it doesn't merge partial
     * fields). PATCH would imply partial-update semantics instead, which
     * this API does not support.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TodoResponse> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody TodoRequest request) {
        TodoResponse updated = todoService.updateTodo(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/todos/{id}
     * WHY 204 No Content (not 200 OK): the operation succeeded, but there is
     * nothing meaningful left to return - the resource is gone. Sending a
     * response body for a successful DELETE would be unusual and is
     * explicitly what HTTP 204 exists for: "success, intentionally empty body."
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build(); // status 204, empty body
    }
}
