# todo-cicd

A simple Todo REST API built with Java 17 and Spring Boot 3, created as a learning project for practicing CI/CD pipelines.

## Tech Stack

| Tool | Purpose |
|---|---|
| Java 17 | Language runtime |
| Spring Boot 3.2.5 | Application framework |
| Maven | Build tool / dependency management |
| Spring Web | REST controllers, embedded Tomcat |
| Spring Data JPA | Repository/ORM layer (backed by Hibernate) |
| H2 Database | In-memory SQL database, zero setup required |
| Lombok | Removes getter/setter/constructor boilerplate |
| JUnit 5 + Mockito | Unit and integration testing |

## Project Structure

```
todo-cicd/
├── src/main/java/com/wadea/todocicd/
│   ├── controller/TodoController.java      # REST endpoints
│   ├── service/TodoService.java            # Service interface (the contract)
│   ├── service/TodoServiceImpl.java        # Business logic
│   ├── repository/TodoRepository.java      # Spring Data JPA repository
│   ├── model/Todo.java                     # JPA entity
│   ├── dto/TodoRequest.java                # Inbound payload (POST/PUT)
│   ├── dto/TodoResponse.java               # Outbound payload (all endpoints)
│   ├── dto/ErrorResponse.java              # Consistent error payload shape
│   ├── exception/TodoNotFoundException.java
│   ├── exception/GlobalExceptionHandler.java
│   └── TodoCicdApplication.java            # main() entry point
├── src/main/resources/application.properties
├── src/test/java/com/wadea/todocicd/
│   ├── unit/TodoServiceTest.java                    # Mockito-based unit tests
│   └── integration/TodoControllerIntegrationTest.java  # MockMvc + real H2 DB
├── pom.xml
└── README.md
```

> Note: `dto/ErrorResponse.java` and `exception/GlobalExceptionHandler.java` were
> added beyond the originally-listed file tree because the requirements
> explicitly call for a `@RestControllerAdvice` global exception handler with a
> consistent error response body - this is where that logic naturally lives.

## Prerequisites

You need **JDK 17** and **Maven 3.6+** installed locally.

Check what you have:

```bash
java -version
mvn -version
```

If you don't have Maven installed, you have two options:
1. Install it (e.g. `choco install maven` on Windows, `brew install maven` on macOS, or download from [maven.apache.org](https://maven.apache.org/download.cgi)).
2. Generate a Maven Wrapper once you have Maven available elsewhere: `mvn -N wrapper:wrapper`, which adds `mvnw`/`mvnw.cmd` scripts so future contributors don't need Maven pre-installed - only a JDK.

## How to Run Locally

From the `todo-cicd` directory:

```bash
mvn spring-boot:run
```

Or build a jar and run it directly:

```bash
mvn clean package
java -jar target/todo-cicd-0.0.1-SNAPSHOT.jar
```

The app starts on **http://localhost:8080**.

### H2 Console

While the app is running, open **http://localhost:8080/h2-console** in a browser to inspect the live in-memory database. Use these connection settings:

- **JDBC URL:** `jdbc:h2:mem:tododb`
- **User Name:** `sa`
- **Password:** *(leave blank)*

## How to Run the Tests

Run everything (unit + integration tests):

```bash
mvn test
```

Run only one test class:

```bash
mvn test -Dtest=TodoServiceTest
mvn test -Dtest=TodoControllerIntegrationTest
```

Run the full build, including tests, exactly like a CI pipeline would:

```bash
mvn clean verify
```

## API Reference

Base URL: `http://localhost:8080/api/todos`

| Method | Endpoint | Description | Success Status |
|---|---|---|---|
| GET | `/api/todos` | Get all todos | 200 |
| GET | `/api/todos/{id}` | Get one todo by id | 200 (404 if missing) |
| POST | `/api/todos` | Create a new todo | 201 |
| PUT | `/api/todos/{id}` | Replace an existing todo | 200 (404 if missing) |
| DELETE | `/api/todos/{id}` | Delete a todo | 204 (404 if missing) |

### Example Requests (curl)

**Create a todo:**

```bash
curl -i -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"Buy milk\", \"description\": \"2% milk, 1 gallon\"}"
```

**Get all todos:**

```bash
curl -i http://localhost:8080/api/todos
```

**Get one todo by id:**

```bash
curl -i http://localhost:8080/api/todos/1
```

**Update a todo:**

```bash
curl -i -X PUT http://localhost:8080/api/todos/1 \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"Buy oat milk\", \"description\": \"Almond is fine too\", \"completed\": true}"
```

**Delete a todo:**

```bash
curl -i -X DELETE http://localhost:8080/api/todos/1
```

**Error example - requesting a todo that doesn't exist:**

```bash
curl -i http://localhost:8080/api/todos/999
```

```json
{
  "timestamp": "2026-06-26T10:15:30.123",
  "status": 404,
  "error": "Not Found",
  "message": "Todo not found with id: 999",
  "path": "/api/todos/999"
}
```

> Windows `cmd.exe` users: replace the escaped `\"` quoting above with PowerShell's
> here-string or simply use a REST client like Postman/Insomnia/the VS Code
> "REST Client" extension if curl's quoting feels awkward.

## Design Notes (Why things are built this way)

- **DTOs instead of exposing the entity directly:** `TodoRequest`/`TodoResponse`
  keep the public API contract decoupled from the database schema, and prevent
  clients from ever setting fields like `id` or `createdAt` themselves.
- **Service interface + impl:** `TodoController` depends on the `TodoService`
  interface, not `TodoServiceImpl` directly, which keeps the controller
  testable/swappable.
- **`@RestControllerAdvice` for errors:** all error responses across every
  endpoint share one consistent JSON shape (`ErrorResponse`), instead of each
  controller method handling its own errors differently.
- **H2 + `ddl-auto=create-drop`:** the schema is rebuilt from the `@Entity`
  classes every time the app starts and wiped when it stops - ideal for
  learning and for CI pipelines, where every run should start from a clean
  slate. A real production system would use `validate` plus a migration tool
  like Flyway or Liquibase instead.
