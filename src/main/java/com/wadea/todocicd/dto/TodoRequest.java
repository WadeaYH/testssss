package com.wadea.todocicd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TodoRequest - the shape of data WE ACCEPT from clients on POST and PUT.
 *
 * WHY we use a separate "Request DTO" instead of letting controllers accept
 * the Todo entity directly from the HTTP request body (this is one of the
 * most important best practices in this whole project, so read closely):
 *
 *   1. SECURITY / OVER-POSTING PROTECTION: if our controller accepted a Todo
 *      entity directly, a malicious (or just careless) client could send
 *      {"id": 999, "createdAt": "2099-01-01T00:00:00", "title": "hi"} and
 *      overwrite fields it has no business setting. Because TodoRequest only
 *      *has* the fields title/description/completed, it's structurally
 *      impossible for a client to set id or createdAt through this endpoint.
 *
 *   2. DECOUPLING: our internal database schema (the Todo entity) is free to
 *      change independently of our public API contract (this DTO). E.g. we
 *      could rename an internal entity field, or split the table, without
 *      breaking every client that calls our API.
 *
 *   3. VALIDATION lives naturally here: Bean Validation annotations (like
 *      @NotBlank below) describe exactly what a valid INCOMING request looks
 *      like, separate from database-level constraints on the entity.
 *
 * "DTO" = Data Transfer Object: a plain object whose only job is carrying
 * data across a boundary (here: the HTTP request boundary) - it has no
 * persistence annotations and no business logic.
 */
@Data // Lombok: getters/setters/toString/equals/hashCode.
      // WHY needed here specifically: Spring's Jackson integration uses the
      // setters (or, for records/final fields, constructor binding) to
      // populate this object from the incoming JSON request body.
@Builder // Lets our tests construct request payloads fluently, e.g.
         // TodoRequest.builder().title("Buy milk").build().
@NoArgsConstructor // Jackson (the JSON library Spring Boot uses) needs a
                    // no-args constructor to deserialize JSON into this class.
@AllArgsConstructor // Pairs with @Builder (see Todo.java for the same reasoning).
public class TodoRequest {

    /**
     * The todo's title. Required - this is the one field that genuinely must
     * be present for a todo to make any sense.
     */
    @NotBlank(message = "Title is required and cannot be blank")
    // @NotBlank: WHY chosen over @NotNull - @NotBlank rejects null, an empty
    // string "", AND a whitespace-only string "   ". A title that's just
    // spaces is just as useless to a user as no title at all, so we reject
    // both cases with one annotation. This is enforced automatically by
    // Spring (via the @Valid annotation on the controller method) BEFORE our
    // code ever runs - invalid requests get an automatic 400 Bad Request.
    private String title;

    /**
     * Optional free-text details. No annotation needed beyond @Size, since
     * "no description" is a perfectly valid todo.
     */
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    // WHY validate max length here too: it mirrors the @Column(length = 2000)
    // constraint on the Todo entity (see Todo.java) so that an over-long
    // description is rejected immediately with a clear 400 message, instead
    // of failing later with a confusing database-level error.
    private String description;

    /**
     * Whether the todo is already completed.
     * WHY this is allowed (and optional) on create, not just update: it lets
     * a client import/create an already-completed todo in a single call
     * (e.g. when migrating data from another system). If the client omits
     * this field entirely, it will simply be null here - our service layer
     * (TodoServiceImpl) is responsible for treating "null" as "false" on
     * creation, since the DTO itself shouldn't bake in that business rule.
     */
    private Boolean completed;
}
