# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Service Startup Commands

### Prerequisites
- Java 17+
- Docker or Rancher Desktop (for DynamoDB Local)

### Quick Start

**✅ CURRENT STATUS: Service starts successfully with DynamoDB bean conflicts resolved**

#### Step 1: Test Service (Works)
```bash
# Run tests to verify service works
./gradlew test
```

#### Step 2: Start Docker/Rancher Desktop (Optional - for DynamoDB Local)
```bash
open -a "Rancher Desktop"
# Wait for Rancher Desktop to fully start (may take 30-60 seconds)
```

#### Step 3: Start DynamoDB Local and Create Tables (Optional)
```bash
# Start DynamoDB Local (if not already running from other services)
docker-compose up -d dynamodb-local

# Create prescriptions table
aws dynamodb create-table \
    --table-name prescriptions \
    --attribute-definitions \
        AttributeName=PK,AttributeType=S \
        AttributeName=SK,AttributeType=S \
    --key-schema \
        AttributeName=PK,KeyType=HASH \
        AttributeName=SK,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --endpoint-url http://localhost:8000 \
    --region us-east-1

# Create prescription-schedules table
aws dynamodb create-table \
    --table-name prescription-schedules \
    --attribute-definitions \
        AttributeName=PK,AttributeType=S \
        AttributeName=SK,AttributeType=S \
    --key-schema \
        AttributeName=PK,KeyType=HASH \
        AttributeName=SK,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --endpoint-url http://localhost:8000 \
    --region us-east-1
```

#### Step 4: Start Service (Now Works Successfully)
```bash
# Build the service
./gradlew build

# Start service (runs on port 8084)
./gradlew run
```

#### Step 5: Test Service Health
```bash
# Test health endpoint
curl http://localhost:8084/api/health

# Expected response:
# {"status":"healthy","service":"prescription-management-service","timestamp":"2025-08-12T02:18:38.533630Z"}
```

### Recent Fixes Applied ✅

#### DynamoDB Configuration Issues Resolved:
The service startup issues have been resolved by implementing service-specific DynamoDB qualifiers as required by the microservices architecture.

**Changes Made:**
- Added `@PrescriptionManagementDynamoDb` qualifier annotation for service-specific DynamoDB beans
- Updated DynamoDB configuration with `@Requires(notEnv = ["test"])` for production isolation
- Simplified DynamoDB client configuration to match medication-catalog-service pattern
- Changed service port from 3000 to 8084 to avoid conflicts with other services
- Updated repositories to use qualified DynamoDB injection

**Benefits:**
- Service can now run alongside other microservices without bean conflicts
- Follows CLAUDE.md microservices standards for DynamoDB configuration
- Clean startup without dependency injection issues
- Consistent configuration pattern across services

### Troubleshooting

1. **Port conflicts:**
   ```bash
   # Check if port 8084 is in use
   lsof -i :8084
   kill <PID>  # If needed
   ```

2. **DynamoDB connection issues:**
   - Ensure DynamoDB Local is running on port 8000
   - Check Docker/Rancher Desktop is running
   - Verify tables exist with correct schema

3. **Service won't start:**
   - Ensure no other services are using qualified bean names
   - Check logs for specific error messages
   - Try `./gradlew clean build` to clear any build artifacts

## Development Commands

### Known Working Commands
```bash
./gradlew test              # ✅ All tests pass
./gradlew build             # ✅ Build succeeds  
./gradlew run               # ✅ Starts successfully on port 8084
./gradlew clean             # ✅ Clean build works
```

### API Testing Commands
```bash
# Health check
curl http://localhost:8084/api/health

# Test API endpoints (require valid UUIDs)
curl http://localhost:8084/api/patients/550e8400-e29b-41d4-a716-446655440000/prescriptions
```

### Additional Commands
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

## Recent Migration History

**Project Evolution**: This project was originally implemented in Node.js/TypeScript with Express.js, then completely migrated to Kotlin/Micronaut for improved performance and maintainability.

**Migration Completed**: All Node.js files have been removed and replaced with Kotlin/Micronaut equivalents while maintaining 100% API compatibility.

**Key Migration Changes**:
- Express.js controllers → Micronaut `@Controller` with reactive streams
- Joi validation → Jakarta Bean Validation annotations
- AWS SDK v2 DocumentClient → Enhanced DynamoDB client with type safety
- Jest tests → JUnit 5 with Micronaut Test framework
- npm/package.json → Gradle with build.gradle.kts

**Current Status**: 
- ✅ Full Kotlin/Micronaut implementation complete
- ✅ All tests passing (`./gradlew test`)
- ✅ DynamoDB integration working with Enhanced Client
- ✅ Clinical decision support system implemented
- ✅ Schedule generation algorithms migrated
- ✅ API endpoints maintain exact compatibility with original Node.js version

**Important Dependencies**:
- `runtimeOnly("org.yaml:snakeyaml")` is required for `application.yml` parsing
- All service/repository classes must be `open` for Micronaut AOP proxying
- Uses `@Serdeable` instead of `@SerdeImport` for JSON serialization

**Build & Test Status**: Service builds and tests successfully. Docker warnings in test output are expected (test resources trying to connect to Docker).