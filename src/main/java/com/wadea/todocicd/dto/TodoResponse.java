package com.wadea.todocicd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TodoResponse - the shape of data WE RETURN to clients for every endpoint.
 *
 * WHY a separate "Response DTO" instead of returning the Todo entity directly
 * from our controllers:
 *
 *   1. STABLE PUBLIC CONTRACT: this class defines exactly what shape of JSON
 *      our API promises to clients. We can freely refactor the Todo entity
 *      (rename a column, add an internal-only field, split a table) without
 *      changing what consumers of our API see, as long as we keep this DTO's
 *      shape the same.
 *
 *   2. NO ACCIDENTAL LEAKS: if we ever add a sensitive or purely-internal
 *      field to the Todo entity later (e.g. an internal audit flag, a soft-
 *      delete marker, a foreign key to an internal-only table), it will NOT
 *      automatically appear in API responses, because this response DTO only
 *      exposes the fields we explicitly listed below.
 *
 *   3. AVOIDS LAZY-LOADING/SERIALIZATION SURPRISES: returning JPA entities
 *      directly from a controller can trigger subtle bugs with Hibernate
 *      proxies and lazy-loaded fields when Jackson tries to serialize them to
 *      JSON. A plain DTO has none of that machinery, so serialization is
 *      simple and predictable.
 *
 * This class mirrors every field on Todo because the requirements say "what
 * we return to the client (all fields)" - but note it is a DELIBERATE
 * decision to list them again here, not a shortcut, precisely so the API
 * contract and the database entity are free to diverge later.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoResponse {

    /** The todo's database-assigned unique identifier. */
    private Long id;

    /** The todo's title. */
    private String title;

    /** The todo's optional description. */
    private String description;

    /** Whether the todo has been completed. */
    private Boolean completed;

    /** When the todo was originally created (server-assigned, read-only). */
    private LocalDateTime createdAt;
}
