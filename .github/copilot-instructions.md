# Copilot Instructions for this Java repository

## Purpose
This repository is a Java application, and code changes should follow clean, maintainable, production-ready practices. Prefer simple, explicit solutions over clever or overly abstract designs.

## General coding standards
- Write clear, readable Java code that follows the repository's existing style and naming conventions.
- Prefer small, single-responsibility classes and methods.
- Favor explicit behavior over hidden magic.
- Keep code consistent with the surrounding project structure and existing patterns.
- Avoid unnecessary dependencies, complexity, or dead code.

## Java and framework guidance
- Prefer modern Java LTS versions when compatible with the project (for example Java 17+ if the project supports it).
- If this repository uses Spring Boot, follow standard conventions for Spring application development.
- Use dependency injection via constructors and prefer `private final` fields for required collaborators.
- Use `@RestController`, `@Service`, `@Repository`, and other Spring stereotypes appropriately.
- Keep controllers thin; move business logic into service classes.
- Use DTOs for API payloads and avoid exposing persistence entities directly unless the project intentionally does so.

## Architecture and package organization
- Organize code by feature/domain when practical rather than by technical layer alone.
- Keep package names consistent and domain-oriented.
- Place configuration in `application.yml`/`application.properties` or equivalent config classes.
- Use environment-specific configuration for dev/test/prod when applicable.
- Do not hardcode secrets or environment-specific values.

## API and validation
- Design REST endpoints with clear, consistent resource naming and HTTP semantics.
- Validate incoming request data using Bean Validation (`@Valid`, `@NotNull`, `@Size`, `@Min`, `@Max`, etc.) when applicable.
- Return consistent response models and explicit HTTP status codes.
- Use a centralized exception-handling approach for predictable error responses.
- Provide meaningful validation and business error messages.

## Error handling and logging
- Handle exceptions deliberately and map them to clear application-level behaviors.
- Use SLF4J logging and prefer parameterized logging over string concatenation.
- Log significant events and state changes without exposing sensitive information.
- Keep logs actionable and avoid noisy or excessive debug output.

## Persistence and data access
- Use repository abstractions for data access instead of embedding persistence logic in business classes.
- Prefer Spring Data repository patterns for common CRUD operations.
- Keep persistence logic separate from API and domain logic.
- Use transactions at the service layer where data consistency requires them.

## Testing expectations
- Add or update tests for behavior changes.
- Prefer JUnit 5 for unit and integration tests.
- Use Mockito or similar mocking frameworks when isolating dependencies.
- Test both success and failure paths, especially validation and error handling.
- Keep tests focused on real behavior rather than implementation details.

## Security considerations
- Follow least-privilege principles for authentication and authorization.
- Protect secrets and avoid committing credentials to source control.
- Sanitize untrusted input and avoid insecure patterns.
- Use secure defaults for API security and configuration.

## Contribution guidance
- Before implementing a change, inspect the existing code and follow the same structure, naming, and conventions used in the repository.
- Prefer minimal, focused changes that solve the current problem without broad refactoring.
- Keep comments useful and sparse; prefer code that is self-explanatory.
- Ensure new code is readable, testable, and maintainable.

## Checklist for generated code
When generating or editing Java code in this repository, ensure the result:
- compiles cleanly with the project build
- follows project conventions and naming patterns
- uses clear method and class responsibilities
- has validation and error handling where needed
- includes tests for meaningful behavior changes
- avoids security and configuration mistakes

These instructions are intentionally lightweight but should be used as the baseline for Java development in this repository.
