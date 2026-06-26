package com.wadea.todocicd.repository;

import com.wadea.todocicd.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TodoRepository - our data access layer for the Todo entity.
 *
 * WHY this is just an interface with no implementation written anywhere:
 * Spring Data JPA generates the actual implementation for us automatically,
 * at application startup, using a dynamic proxy. We never write SQL or DAO
 * boilerplate (no "SELECT * FROM todos WHERE ..." Java code) for standard
 * CRUD operations - we only declare WHAT we need, not HOW to do it.
 */
@Repository
// @Repository: marks this as a Spring-managed bean belonging to the
// persistence layer. WHY it's not strictly required on JpaRepository
// interfaces (Spring Data already detects them automatically) - we keep it
// anyway for explicitness/readability, and because it enables Spring's
// automatic persistence-exception translation (e.g. turning a raw JDBC
// SQLException into a clearer Spring DataAccessException) for any custom
// query methods we add.
public interface TodoRepository extends JpaRepository<Todo, Long> {
    // Extending JpaRepository<Todo, Long> immediately gives us, for free:
    //   save(Todo)            -> INSERT or UPDATE
    //   findById(Long)         -> SELECT ... WHERE id = ?
    //   findAll()              -> SELECT * FROM todos
    //   deleteById(Long)       -> DELETE ... WHERE id = ?
    //   existsById(Long)       -> SELECT COUNT(*) ... WHERE id = ?
    // <Todo, Long> means: "manage the Todo entity, whose primary key is a Long."

    /**
     * Custom finder method: returns every todo whose `completed` flag matches
     * the given value.
     *
     * WHY we don't need to write any SQL or any method body at all: Spring
     * Data JPA parses the METHOD NAME itself at startup ("findBy" + "Completed"
     * matches the `completed` field on the Todo entity) and generates the
     * equivalent of:
     *   SELECT * FROM todos WHERE completed = ?
     * This naming-convention-based query generation is one of Spring Data
     * JPA's signature features - it eliminates a huge amount of repetitive
     * DAO code for simple lookups like this one.
     *
     * @param completed the completion status to filter by
     * @return every Todo whose completed field equals the given value
     */
    List<Todo> findByCompleted(Boolean completed);
}
