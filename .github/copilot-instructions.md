# Copilot Instructions for this Spring Boot REST API repository

## Project context
This repository is a Spring Boot REST API project built with Maven and tested with JUnit 5. All code generation and modifications should follow Spring Boot conventions, clean Java practices, and repository-level consistency.

## Core principles
- Prefer simple, readable, production-ready solutions over clever abstractions.
- Keep code explicit, testable, and maintainable.
- Follow the existing project structure and naming conventions before introducing new patterns.
- Make small, focused changes that solve the current problem without unrelated refactoring.

## Java and Maven standards
- Use Java LTS versions supported by the project; prefer modern Java syntax and idioms consistent with the configured Maven toolchain.
- Keep `pom.xml` as the source of truth for dependencies, plugins, and build configuration.
- Prefer official Spring Boot starters and Maven-managed dependencies over custom/manual dependency management.
- Add dependencies only when they are clearly needed and align with the current architecture.
- Keep builds reproducible: avoid hardcoded local paths, environment-specific assumptions, or unstable plugin behavior.
- Use Maven commands like `mvn clean test` and `mvn package` as the standard validation flow unless the repo documents otherwise.

## Spring Boot architecture
- Use Spring Boot default conventions unless the project clearly defines a different pattern.
- Use constructor injection for all required dependencies.
- Prefer `private final` fields for injected collaborators.
- Keep controllers thin and focused on HTTP concerns.
- Place business logic in `@Service` classes.
- Put data access in repository or persistence-layer abstractions.
- Use `@Configuration` and `@ConfigurationProperties` for typed configuration when appropriate.
- Externalize environment-specific settings via `application.yml`, `application.properties`, or profile-specific files such as `application-dev.yml` and `application-test.yml`.
- Never commit secrets, credentials, API keys, or production environment values to source control.

## Package and code organization
- Use a consistent, domain-oriented package structure such as:
  - `com.example.app.user`
  - `com.example.app.order`
  - `com.example.app.common`
  - `com.example.app.config`
- Prefer feature/domain grouping over purely technical folders when practical.
- Keep classes focused and single-purpose.
- Name classes, methods, and variables with clear business meaning.
- Avoid overly generic names like `Manager`, `Helper`, or `Util` unless they are clearly justified.

## REST API design
- Build RESTful APIs with clear resource names and predictable URL patterns.
- Use proper HTTP methods and status codes.
- Return explicit response models rather than exposing raw persistence entities to API clients.
- Prefer DTOs for request and response payloads.
- Validate incoming payloads using Bean Validation annotations such as:
  - `@Valid`
  - `@NotNull`
  - `@Size`
  - `@Min`
  - `@Max`
  - `@Pattern`
- Fail fast at the boundary and return meaningful validation errors.
- Keep APIs consistent across endpoints, serialization, and error shapes.

## Service layer and domain logic
- Put core business logic in `@Service` classes.
- Keep services stateless whenever possible.
- Use transaction boundaries around persistence operations that require atomicity.
- Apply `@Transactional` at the service layer and avoid broad transaction scopes when a narrower boundary is sufficient.
- Keep controllers focused on request parsing, validation, and response mapping.

## Persistence and data access
- Use Spring Data repositories such as `JpaRepository` or `CrudRepository` for common CRUD behavior.
- Keep persistence concerns out of controllers and DTOs.
- Use query methods, `@Query`, or repository abstractions for non-trivial queries.
- Avoid leaking persistence details into API contracts.
- Use entities and database mappings deliberately, with clear business intent.

## Error handling and validation
- Implement a central exception-handling strategy using `@ControllerAdvice` and `@ExceptionHandler` when appropriate.
- Return structured error responses with consistent fields such as:
  - timestamp
  - status
  - error
  - message
  - path
- Map domain-specific exceptions to meaningful HTTP responses.
- Do not swallow exceptions silently.
- Ensure validation and business rules fail with clear, actionable feedback.

## Logging and observability
- Use SLF4J logging with `LoggerFactory`.
- Prefer parameterized logging instead of string concatenation.
- Example:
  ```java
  private static final Logger logger = LoggerFactory.getLogger(MyService.class);

  logger.info("Processing order {} for user {}", orderId, userId);
  ```
- Log important business events and errors without exposing sensitive information.
- Keep logs concise and actionable.

## Testing requirements
- Write tests for behavior changes, especially service logic, validation, controllers, and error flows.
- Use JUnit 5 as the standard test framework.
- Use Mockito for mocking collaborators and isolating units under test.
- Prefer `@WebMvcTest` for controller-focused tests.
- Prefer `@DataJpaTest` for repository persistence tests.
- Use `@SpringBootTest` for integration scenarios that require the full application context.
- Verify both success and failure paths.
- Avoid tests that only assert implementation details rather than observable behavior.
- Keep tests deterministic, fast, and independent of local environment assumptions.

## Security and configuration
- Follow least-privilege principles for authentication and authorization.
- Use Spring Security patterns and secure defaults when authn/authz is needed.
- Never hardcode user secrets, password hashes, API keys, or tokens.
- Sanitize untrusted input and guard against common web vulnerabilities.
- Prefer secure defaults for configuration and deployment.

## Code quality expectations
- Keep methods concise and readable.
- Prefer clear naming over short cryptic identifiers.
- Remove dead code, unused imports, and redundant logic.
- Do not add comments for obvious code; add comments only when they explain why, not what.
- Prefer standard library and framework patterns over custom reimplementations.

## Generated code checklist
Before finishing a change, verify that the code:
- compiles successfully with Maven
- follows the repository’s package and naming conventions
- uses Spring Boot conventions correctly
- includes validation and meaningful error handling where required
- has appropriate JUnit 5 tests for changed behavior
- avoids security risks and hardcoded secrets
- remains readable and maintainable

## Standard validation commands
Use these commands when appropriate:
- `mvn clean test`
- `mvn package`
- `mvn spring-boot:run` for local application startup

When implementing or reviewing work in this repository, favor production-ready Spring Boot REST API patterns aligned with Maven and JUnit 5 best practices.
