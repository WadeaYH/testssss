package com.wadea.todocicd.exception;

import com.wadea.todocicd.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - ONE centralized place that converts exceptions
 * thrown anywhere in this application into well-formed HTTP error responses.
 *
 * WHY centralize this instead of try/catch in every controller method:
 *   - DRY: without this class, every single controller method would need its
 *     own try/catch around TodoNotFoundException, duplicating the same
 *     "build a 404 response" logic five times over.
 *   - CONSISTENCY: every endpoint in the whole API returns errors in the
 *     exact same JSON shape (see ErrorResponse), which is exactly what a
 *     well-designed REST API should guarantee to its clients.
 *   - SEPARATION OF CONCERNS: controllers stay focused purely on the "happy
 *     path" (see TodoController) - they don't need to know or care how errors
 *     are formatted at all.
 */
@RestControllerAdvice
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody combined.
// WHY: it tells Spring "intercept exceptions thrown from ANY @RestController
// in this application, run them through the matching @ExceptionHandler method
// below, and serialize whatever that method returns directly as the HTTP
// response body (as JSON), instead of trying to render an error HTML page."
public class GlobalExceptionHandler {

    /**
     * Handles the case where TodoServiceImpl couldn't find a todo by id.
     *
     * @param ex      the exception that was thrown - already contains a
     *                clear, specific message (see TodoNotFoundException).
     * @param request gives us the original request path, so the response
     *                body can tell the client exactly which URL failed.
     * @return a 404 NOT FOUND response with a structured, human-readable body.
     */
    @ExceptionHandler(TodoNotFoundException.class)
    // @ExceptionHandler: WHY this works without any explicit wiring in the
    // controller - Spring registers this class as a global listener for
    // uncaught exceptions. The moment TodoServiceImpl throws
    // TodoNotFoundException and nothing else catches it, it propagates up
    // through TodoController, and Spring intercepts it here automatically
    // before it would otherwise crash the request with a generic 500 error.
    public ResponseEntity<ErrorResponse> handleTodoNotFound(
            TodoNotFoundException ex, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value()) // 404
                .error(HttpStatus.NOT_FOUND.getReasonPhrase()) // "Not Found"
                .message(ex.getMessage()) // e.g. "Todo not found with id: 42"
                .path(request.getRequestURI()) // e.g. "/api/todos/42"
                .build();

        // ResponseEntity.status(404).body(...) -> WHY use ResponseEntity here
        // too (and not just @ResponseStatus(NOT_FOUND) on the method): it lets
        // us return a status code AND a custom JSON body together explicitly,
        // which is exactly what a REST client needs to both detect the error
        // (via status code) and understand WHY (via the message body).
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles Bean Validation failures - i.e. whenever a @Valid-annotated
     * TodoRequest fails one of its constraints (like @NotBlank on title).
     *
     * WHY handle this too, even though the task only explicitly asked for
     * TodoNotFoundException handling: without this handler, a validation
     * failure would still return 400 automatically (Spring does that part for
     * us), but the response BODY would use Spring's default, more verbose
     * error format instead of our consistent ErrorResponse shape. Handling it
     * here keeps every single error response - 404s AND 400s - in one
     * predictable shape for API consumers.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // Collect every individual field error (e.g. "title: must not be
        // blank") into one combined, readable message instead of just
        // reporting the first one.
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value()) // 400
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase()) // "Bad Request"
                .message("Validation failed: " + fieldErrors)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Catch-all safety net for any exception we didn't anticipate.
     *
     * WHY include this: without it, an unexpected bug (e.g. a NullPointerException
     * somewhere) would still return a 500, but Spring Boot's default error page
     * can leak internal details (like stack trace fragments) depending on
     * configuration. Catching Exception here guarantees EVERY error response
     * from this API - expected or not - uses the same safe, structured shape,
     * and never accidentally exposes internal implementation details to a client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred: " + ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
