# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

- `./gradlew build` - Build the application
- `./gradlew test` - Run JUnit 5 tests
- `./gradlew run` - Run the application locally
- `./gradlew clean` - Clean build artifacts
- `./gradlew test --tests "ClassName"` - Run specific test class
- `./gradlew test jacocoTestReport` - Run tests with coverage report

## Architecture Overview

This is a Kotlin/Micronaut prescription management service. The architecture follows Micronaut's dependency injection and reactive patterns:

**Controllers** (`src/main/kotlin/com/prescription/controller/`) handle HTTP requests using Micronaut's `@Controller` annotation. The main `PrescriptionController` uses Jakarta Bean Validation for request validation and returns structured API responses.

**Services** contain business logic:
- `ScheduleService` generates medication schedules using Kotlin's date/time APIs
- `ClinicalDecisionService` provides drug interaction checking and dosage validation using immutable data classes and enum types

**Repositories** (`src/main/kotlin/com/prescription/repository/`) provide DynamoDB data access using AWS SDK v2 Enhanced Client. Uses `@Singleton` for dependency injection and proper Kotlin null safety.

**Domain Model** (`src/main/kotlin/com/prescription/domain/`) uses Kotlin data classes with:
- `@Introspected` for Micronaut reflection-free operation
- `@Serdeable` for JSON serialization
- Jakarta validation annotations for request validation
- Proper enum types for status fields

**DynamoDB Integration**:
- Uses AWS SDK v2 Enhanced Client with `@DynamoDbBean` annotations
- Optimized access patterns for prescription and schedule queries
- `PrescriptionDynamoItem` and `ScheduleDynamoItem` for persistence layer
- Proper separation between domain models and DynamoDB items

**Configuration** (`src/main/kotlin/com/prescription/config/`) uses Micronaut's `@Factory` pattern for bean creation and `@Value` for property injection from `application.yml`.

## Key Kotlin/Micronaut Patterns

**Dependency Injection**: Constructor injection with `@Singleton` services. Micronaut performs compile-time DI analysis.

**Null Safety**: Kotlin's null safety eliminates potential NPEs. Use `?` for nullable types and `!!` only when absolutely certain.

**Data Classes**: Immutable by default with automatic `equals()`, `hashCode()`, and `toString()` methods.

**Extension Functions**: Used for converting between domain models and DynamoDB items.

**Coroutines**: Service methods can use `suspend` for reactive operations, though current implementation uses blocking calls.

## Testing Strategy

Tests use JUnit 5 with Micronaut Test framework:
- `@MicronautTest` for integration tests with full application context
- `@MockBean` for mocking dependencies in controller tests  
- AssertJ for fluent assertions
- Mockito Kotlin for mocking

Test structure mirrors main source with same package structure.

## Environment Configuration

Uses `application.yml` for configuration with environment variable substitution. AWS credentials can be provided via environment variables or AWS credential chain.

## Architecture Notes

This service leverages Kotlin's type safety and Micronaut's performance benefits. Key architectural decisions:

- Compile-time dependency injection for fast startup
- Immutable data classes for thread safety
- Enhanced DynamoDB client for optimal performance
- Jakarta validation for robust request validation