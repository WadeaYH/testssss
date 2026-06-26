package com.wadea.todocicd.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Todo - our JPA entity.
 *
 * WHY this class is called a "model"/"entity": it is a direct, 1-to-1 mapping
 * between a Java class and a row in a relational database table. Every field
 * below becomes a column. Hibernate (the JPA implementation Spring Boot wires
 * up for us) reads the annotations on this class to know how to CREATE the
 * table, and how to translate Java objects into SQL INSERT/UPDATE/SELECT/DELETE
 * statements and back again.
 *
 * WHY we keep this class "dumb" (just data, no business logic): business rules
 * belong in the service layer (see TodoServiceImpl), not in the entity. This
 * separation makes the entity easy to reason about and keeps persistence
 * concerns (how data is stored) separate from business concerns (what the data
 * means and how it should be manipulated).
 */
@Entity // Tells Hibernate/JPA: "this class maps to a database table."
@Table(name = "todos") // Explicit table name. WHY: "todo" alone is a reserved-ish/ambiguous
                        // word in some SQL dialects, and being explicit avoids relying on
                        // Hibernate's default pluralization/naming behavior.
@Data // Lombok: generates getters, setters, toString(), equals(), and hashCode()
      // for every field below, so we don't have to hand-write ~40 lines of boilerplate.
@Builder // Lombok: generates a fluent builder, e.g. Todo.builder().title("Buy milk").build().
         // WHY: builders make object construction readable when there are several fields,
         // and avoid "telescoping constructors" (many overloaded constructors).
@NoArgsConstructor // Lombok: generates a public Todo() {} constructor.
                    // WHY required: JPA/Hibernate uses reflection to instantiate entities
                    // when reading rows back from the database, and it requires a
                    // no-argument constructor to do that.
@AllArgsConstructor // Lombok: generates a constructor with every field as a parameter.
                     // WHY: @Builder internally needs this (or an equivalent) constructor
                     // to build a fully-populated object in one call.
public class Todo {

    /**
     * Primary key. WHY Long (not long): the wrapper type can be null, which
     * matters here because a brand-new Todo (not yet saved) has no id yet -
     * the database assigns it only once the row is actually inserted.
     */
    @Id // Marks this field as the table's primary key.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // IDENTITY strategy: delegate id generation to the database's native
    // auto-increment column. WHY: H2 (and most relational DBs) support this
    // natively, it's simple to reason about, and we don't need the extra
    // complexity of a separate sequence/table-based generator for a project
    // this size.
    private Long id;

    /**
     * The todo's title. Required field - every todo must have one.
     */
    @Column(nullable = false)
    // WHY enforce this at the DATABASE level too (not only in the DTO layer):
    // defense in depth. Bean Validation on TodoRequest (see TodoRequest.java)
    // stops bad data at the HTTP boundary, but the nullable=false column
    // constraint guarantees the database itself can never end up with a null
    // title, even if some other code path bypasses the DTO/validation layer
    // in the future (e.g. a batch import job).
    private String title;

    /**
     * Optional free-text details about the todo.
     */
    @Column(length = 2000)
    // WHY length=2000: Hibernate's default VARCHAR length is 255 characters,
    // which is fine for a title but often too short for a free-text
    // description. We widen it explicitly rather than relying on the default.
    private String description;

    /**
     * Whether the todo has been completed yet. Defaults to false for new todos.
     * WHY Boolean (capital B) instead of primitive boolean: it keeps this
     * field consistent/symmetrical with the rest of the class, and Lombok's
     * @Builder.Default below needs a real default expression regardless of
     * primitive vs wrapper - using the wrapper also matches the DTO type
     * exactly (see TodoRequest/TodoResponse), so there's no implicit
     * boxing/unboxing mismatch when mapping between them.
     */
    @Column(nullable = false)
    @Builder.Default
    // @Builder.Default: WHY needed here specifically - by default, Lombok's
    // @Builder ignores field initializers entirely (it would otherwise build
    // a Todo with completed=null when .completed(...) isn't called explicitly).
    // @Builder.Default tells Lombok "use this initializer as the real default
    // when the builder caller doesn't set this field."
    private Boolean completed = false;

    /**
     * Timestamp of when this todo was first created.
     * WHY LocalDateTime (not Instant/Date): LocalDateTime is the simplest,
     * most beginner-friendly date/time type in java.time for a single-timezone
     * learning project. (A production, multi-timezone system would likely
     * prefer Instant/OffsetDateTime instead.)
     */
    @Column(nullable = false, updatable = false)
    // updatable = false: WHY - once a todo is created, its creation timestamp
    // must never change again, even if some other code accidentally includes
    // this field in a later UPDATE statement. This tells Hibernate to simply
    // omit the column from any SQL UPDATE it generates for this entity.
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback: Hibernate automatically invokes this method
     * immediately before this entity is INSERTed into the database for the
     * very first time.
     *
     * WHY set createdAt here instead of as a field initializer
     * (e.g. "= LocalDateTime.now()"):
     *   1. It fires at the moment the row is actually persisted, which is the
     *      true "created at" instant - not whenever the Java object happened
     *      to be constructed in memory (those can differ, e.g. if a Todo
     *      object sits in memory for a while before being saved).
     *   2. It works no matter which constructor or the builder was used to
     *      create the object, since it's wired into the JPA persistence
     *      lifecycle itself rather than object construction.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
