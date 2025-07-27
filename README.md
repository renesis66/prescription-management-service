# Prescription Management Service

A Kotlin/Micronaut microservice for managing clinical prescriptions with DynamoDB backend, featuring automated schedule generation and clinical decision support.

## Features

- **Prescription Management**: Create, read, update, and delete prescriptions
- **Schedule Generation**: Automatic generation of medication schedules based on dosing frequency
- **Clinical Decision Support**: Drug interaction checking, dosage validation, and clinical alerts
- **DynamoDB Integration**: Optimized data access patterns using AWS SDK v2 Enhanced Client
- **Validation**: Comprehensive input validation using Jakarta Bean Validation
- **Testing**: JUnit 5 tests with Micronaut Test framework

## Technology Stack

- **Kotlin 1.9.20**: Modern JVM language with null safety and concise syntax
- **Micronaut 4.2.1**: Cloud-native JVM framework with dependency injection and reactive streams
- **AWS SDK v2**: Enhanced DynamoDB client with improved performance
- **Jakarta Validation**: Bean validation for request validation
- **JUnit 5**: Modern testing framework with Micronaut Test integration

## API Endpoints

### Prescriptions
- `GET /api/patients/{patientId}/prescriptions` - Get patient prescriptions
- `POST /api/patients/{patientId}/prescriptions` - Create new prescription
- `PUT /api/prescriptions/{id}` - Update prescription
- `DELETE /api/prescriptions/{id}` - Delete prescription
- `GET /api/prescriptions/{id}/schedule` - Get prescription schedule

### Health Check
- `GET /api/health` - Service health status

## Installation

### Prerequisites
- Java 17+
- Gradle 8.4+
- AWS credentials configured

### Build and Run

```bash
# Build the application
./gradlew build

# Run tests
./gradlew test

# Run the application
./gradlew run

# Or build and run JAR
./gradlew build
java -jar build/libs/prescription-management-service-0.1-all.jar
```

## Configuration

Create `.env` file from example:
```bash
cp .env.example .env
```

Required environment variables:
- `AWS_REGION` - AWS region for DynamoDB (default: us-east-1)
- `PRESCRIPTIONS_TABLE` - DynamoDB table name for prescriptions
- `PRESCRIPTION_SCHEDULES_TABLE` - DynamoDB table name for schedules
- `AWS_ACCESS_KEY_ID` - AWS access key
- `AWS_SECRET_ACCESS_KEY` - AWS secret key

## DynamoDB Schema

### Prescriptions Table
- **PK**: `PATIENT#{patientId}`
- **SK**: `PRESCRIPTION#{prescriptionId}`
- **GSI1PK**: `PRESCRIPTION#{prescriptionId}`
- **GSI1SK**: `METADATA`
- **GSI2PK**: `STATUS#{status}`
- **GSI2SK**: `{createdAt}`

### Prescription Schedules Table
- **PK**: `PRESCRIPTION#{prescriptionId}`
- **SK**: `SCHEDULE#{date}#{time}`
- **GSI1PK**: `PATIENT#{patientId}`
- **GSI1SK**: `SCHEDULE#{date}#{time}`

## Clinical Decision Support

The service includes built-in clinical decision support features:

- **Drug Interaction Checking**: Detects interactions between medications
- **Dosage Validation**: Checks for maximum daily dose limits
- **Frequency Validation**: Alerts for potentially unsafe dosing frequencies
- **Duration Monitoring**: Warns about extended treatment durations

## Example Usage

### Create Prescription

```bash
curl -X POST http://localhost:3000/api/patients/123e4567-e89b-12d3-a456-426614174000/prescriptions \
  -H "Content-Type: application/json" \
  -d '{
    "medicationName": "amoxicillin",
    "dosage": 500,
    "unit": "mg",
    "frequencyHours": 8,
    "startTime": "08:00",
    "startDate": "2024-12-01",
    "endDate": "2024-12-10",
    "prescribedBy": "Dr. Smith"
  }'
```

### Response

```json
{
  "success": true,
  "data": {
    "prescriptionId": "456e7890-e89b-12d3-a456-426614174001",
    "patientId": "123e4567-e89b-12d3-a456-426614174000",
    "medicationName": "amoxicillin",
    "dosage": 500,
    "unit": "mg",
    "frequencyHours": 8,
    "startTime": "08:00",
    "startDate": "2024-12-01",
    "endDate": "2024-12-10",
    "status": "ACTIVE",
    "prescribedBy": "Dr. Smith",
    "createdAt": "2024-07-27T15:30:00Z",
    "updatedAt": "2024-07-27T15:30:00Z"
  },
  "clinicalAlerts": [
    {
      "alertType": "DOSAGE_ALERT",
      "severity": "MEDIUM",
      "message": "Treatment duration exceeds recommended maximum",
      "recommendation": "Consider limiting treatment duration or additional monitoring"
    }
  ]
}
```

## Development

### Project Structure
```
src/
├── main/kotlin/com/prescription/
│   ├── controller/         # HTTP controllers
│   ├── service/           # Business logic services
│   ├── repository/        # DynamoDB repositories
│   ├── domain/           # Data classes and entities
│   ├── config/           # Configuration classes
│   └── Application.kt    # Main application class
├── test/kotlin/          # Test classes
└── main/resources/       # Configuration files
```

### Key Features

1. **Type Safety**: Kotlin's null safety eliminates NPE risks
2. **Performance**: Compiled JVM bytecode with excellent performance characteristics
3. **Dependency Injection**: Micronaut's compile-time DI for fast startup
4. **Reactive Streams**: Built-in support for reactive programming
5. **Native Compilation**: Optional GraalVM native image support

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "ClinicalDecisionServiceTest"

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Code Quality

```bash
# Kotlin code style check
./gradlew ktlintCheck

# Format code
./gradlew ktlintFormat
```

## Benefits

- **Performance**: Fast startup time and low memory footprint
- **Type Safety**: Compile-time error detection prevents runtime errors
- **Ecosystem**: Access to mature JVM ecosystem and libraries
- **Scalability**: Excellent horizontal scaling characteristics
- **Monitoring**: Superior observability with Micrometer/Prometheus integration

## License

MIT