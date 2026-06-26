package com.wadea.todocicd.exception;

/**
 * TodoNotFoundException - thrown whenever code looks up a Todo by an id that
 * doesn't exist in the database (see TodoServiceImpl.getTodoById/updateTodo/
 * deleteTodo).
 *
 * WHY extend RuntimeException (an "unchecked" exception) rather than Exception
 * (a "checked" exception):
 *   - Checked exceptions would force every method in the call chain - the
 *     repository call site, every service method, every controller method -
 *     to either declare "throws TodoNotFoundException" or wrap calls in
 *     try/catch, even though 99% of those layers have no meaningful way to
 *     "handle" a missing todo themselves.
 *   - Instead, by making it unchecked, it can bubble up automatically, all the
 *     way from deep inside TodoServiceImpl, straight up through the
 *     controller, to be caught in EXACTLY ONE place: GlobalExceptionHandler.
 *     That is precisely the "throw low, catch high" principle - decide WHERE
 *     to handle an error based on who actually knows what to do about it (the
 *     HTTP layer, which knows how to turn it into a 404 response), not WHERE
 *     it happened to occur.
 */
public class TodoNotFoundException extends RuntimeException {

    /**
     * @param id the id that could not be found, included directly in the
     *           message so logs and API error responses are immediately
     *           actionable (e.g. "Todo not found with id: 42") instead of a
     *           generic, unhelpful "not found".
     */
    public TodoNotFoundException(Long id) {
        super("Todo not found with id: " + id);
    }
}
