# Java Spring Boot API

## Description

Use this skill when creating, reviewing, or improving Java Spring Boot API applications in this repository. It provides repository-level guidance for building high-quality, maintainable, production-ready Spring Boot services that follow established best practices for architecture, validation, security, testing, and configuration.

## Instructions

### 1. Project Setup and Structure

- Prefer Maven (`pom.xml`) or Gradle (`build.gradle`) for dependency and build management.
- Use Spring Boot starters to simplify dependency setup, for example:
  - `spring-boot-starter-web` for REST APIs
  - `spring-boot-starter-data-jpa` for persistence
  - `spring-boot-starter-validation` for Bean Validation
  - `spring-boot-starter-security` when authentication/authorization is required
- Organize code by feature/domain rather than by technical layer when practical, for example:
  - `com.example.app.order`
  - `com.example.app.user`
  - `com.example.app.auth`
- Keep package naming consistent and domain-oriented to improve maintainability and team readability.

### 2. Dependency Injection and Components

- Use constructor injection for required dependencies.
- Prefer `private final` fields for injected dependencies.
- Use Spring stereotypes appropriately:
  - `@RestController` for API endpoints
  - `@Service` for business logic
  - `@Repository` for persistence access
  - `@Configuration` and `@ConfigurationProperties` for config classes
- Keep components focused and single-purpose.

### 3. Configuration and Environment Management

- Store configuration in `application.yml` or `application.properties`.
- Prefer YAML for readability and hierarchical structure when configuration is complex.
- Use `@ConfigurationProperties` for strongly typed configuration.
- Separate environment-specific settings using Spring profiles such as:
  - `application-dev.yml`
  - `application-prod.yml`
  - `application-test.yml`
- Do not hardcode secrets. Use environment variables, Docker/Kubernetes secrets, or a secret manager such as HashiCorp Vault or AWS Secrets Manager.

### 4. API Design

- Build RESTful APIs with clear resource naming and consistent endpoint patterns.
- Use HTTP semantics correctly for create, read, update, and delete operations.
- Use DTOs to expose data to clients and avoid exposing persistence entities directly.
- Validate incoming payloads with Java Bean Validation using annotations such as:
  - `@Valid`
  - `@NotNull`
  - `@Size`
  - `@Min` / `@Max`
- Return explicit, consistent response models and status codes.
- Prefer a predictable API contract over ad hoc behavior.

### 5. Error Handling and Validation

- Implement a global exception handler with `@ControllerAdvice` and `@ExceptionHandler`.
- Return structured, developer-friendly error payloads with consistent fields such as:
  - timestamp
  - status
  - error
  - message
  - path
- Validate request payloads at the boundary and fail fast.
- Handle domain-specific exceptions deliberately and map them to meaningful HTTP responses.

### 6. Service Layer

- Put core business logic inside `@Service` classes.
- Keep services stateless whenever possible.
- Use `@Transactional` on service methods when persistence changes are involved.
- Apply transactions at the most granular level necessary to avoid unnecessary locking or broad transaction scope.
- Keep controllers thin and delegate to services for logic.

### 7. Data Access

- Use Spring Data JPA repositories by extending `JpaRepository` or `CrudRepository` for common CRUD behaviors.
- Use repository abstractions for data access instead of embedding persistence logic in entities or services.
- Use `@Query` or the JPA Criteria API for non-trivial queries.
- Use projections or query-specific DTOs to fetch only the data required by the caller.
- Avoid leaking persistence details into API contracts.

### 8. Logging

- Use SLF4J for logging.
- Declare loggers as:

```java
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
```

- Prefer parameterized logging over string concatenation:

```java
logger.info("Processing user {}...", userId);
```

- Log meaningful events without excessive noise or sensitive data.

### 9. Testing

- Write unit tests for services, validators, and other core logic with JUnit 5.
- Use Mockito or equivalent mocking frameworks for isolating dependencies.
- Use `@SpringBootTest` for full integration tests that load the application context.
- Use test slice annotations such as:
  - `@WebMvcTest` for controller testing
  - `@DataJpaTest` for repository testing
- Consider Testcontainers for reliable integration tests involving databases, brokers, and other infrastructure dependencies.
- Test both success and failure scenarios, especially validation and error paths.

### 10. Security

- Use Spring Security for authentication and authorization.
- Encode passwords using a strong hashing algorithm such as BCrypt.
- Sanitize untrusted input and protect against common web vulnerabilities.
- Prevent SQL injection by using Spring Data JPA or parameterized queries.
- Prevent XSS by properly encoding output and avoiding unsafe rendering.
- Apply least-privilege access controls and secure API endpoints based on roles or permissions.

## Best Practices Summary

When generating or reviewing Spring Boot API code in this repository:

- Prefer simple, maintainable designs over cleverness.
- Use constructor injection and immutable dependencies.
- Keep the controller/service/repository separation clear.
- Validate inputs and handle errors consistently.
- Externalize configuration.
- Protect secrets and secure endpoints.
- Write tests that verify real behaviors.

## Example Usage

Use this skill when asked to:

- build a new Spring Boot REST API
- review an existing controller/service/repository design
- suggest project structure and package organization
- add validation, exception handling, or security controls
- create tests for a Spring Boot application
- improve coding standards in Java API code

Example prompt:

```text
Create a Spring Boot REST API for managing orders with validation, DTOs, service logic, and repository access.
```

## Output Expectations

When responding to requests in this repository, prefer:

- production-ready Spring Boot code patterns
- explicit, typed configuration and validation
- clear layered architecture without unnecessary complexity
- comments or examples only when they add practical value
- secure, testable, maintainable designs aligned with industry best practices

## Notes

This skill is a baseline standard for Java Spring Boot API work in this repository. If a user asks for a different stack or a non-standard architecture, adapt carefully without violating the repository's coding standards or the user's explicit requirements.
