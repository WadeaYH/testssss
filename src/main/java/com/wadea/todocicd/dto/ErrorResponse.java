package com.wadea.todocicd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ErrorResponse - the consistent JSON shape we send back for EVERY error,
 * across every endpoint (see GlobalExceptionHandler).
 *
 * WHY a dedicated, structured error shape instead of just returning the raw
 * exception message as plain text: a real client (a frontend app, a mobile
 * app, another service) needs to parse error responses programmatically.
 * Giving every error the exact same JSON shape - timestamp/status/error/
 * message/path - means client code can write ONE generic error handler that
 * works for every endpoint in this API, instead of guessing at a different
 * shape per error type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** Exact moment the error was generated - useful for correlating with server logs. */
    private LocalDateTime timestamp;

    /** The numeric HTTP status code, e.g. 404. Duplicated here (it's also on the
     *  HTTP response itself) purely for convenience, so clients can log/inspect
     *  the body alone without needing the surrounding HTTP envelope. */
    private int status;

    /** Short machine-friendly category, e.g. "Not Found". */
    private String error;

    /** Human-readable explanation of exactly what went wrong, e.g.
     *  "Todo not found with id: 42". */
    private String message;

    /** The request path that triggered the error, e.g. "/api/todos/42" -
     *  helpful when several endpoints can throw the same exception type. */
    private String path;
}
