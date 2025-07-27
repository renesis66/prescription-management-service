# Prescription Management Service

A Node.js/TypeScript service for managing clinical prescriptions with DynamoDB backend, featuring automated schedule generation and clinical decision support.

## Features

- **Prescription Management**: Create, read, update, and delete prescriptions
- **Schedule Generation**: Automatic generation of medication schedules based on dosing frequency
- **Clinical Decision Support**: Drug interaction checking, dosage validation, and clinical alerts
- **DynamoDB Integration**: Optimized data access patterns for prescriptions and schedules
- **Validation**: Comprehensive input validation with Joi
- **Testing**: Unit tests with Jest

## API Endpoints

### Prescriptions
- `GET /api/patients/{patientId}/prescriptions` - Get patient prescriptions
- `POST /api/patients/{patientId}/prescriptions` - Create new prescription
- `PUT /api/prescriptions/{id}` - Update prescription
- `DELETE /api/prescriptions/{id}` - Delete prescription
- `GET /api/prescriptions/{id}/schedule` - Get prescription schedule

### Health Check
- `GET /health` - Service health status

## Installation

```bash
npm install
```

## Configuration

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
```

Required environment variables:
- `AWS_REGION` - AWS region for DynamoDB
- `PRESCRIPTIONS_TABLE` - DynamoDB table name for prescriptions
- `PRESCRIPTION_SCHEDULES_TABLE` - DynamoDB table name for schedules
- `AWS_ACCESS_KEY_ID` - AWS access key
- `AWS_SECRET_ACCESS_KEY` - AWS secret key

## Development

```bash
npm run dev     # Start development server
npm run build   # Build for production
npm start       # Start production server
npm test        # Run tests
npm run lint    # Lint code
npm run typecheck # Type checking
```

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
POST /api/patients/123e4567-e89b-12d3-a456-426614174000/prescriptions
Content-Type: application/json

{
  "medicationName": "amoxicillin",
  "dosage": 500,
  "unit": "mg",
  "frequencyHours": 8,
  "startTime": "08:00",
  "startDate": "2024-01-15",
  "endDate": "2024-01-25",
  "prescribedBy": "Dr. Smith"
}
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
    "startDate": "2024-01-15",
    "endDate": "2024-01-25",
    "status": "ACTIVE",
    "prescribedBy": "Dr. Smith",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
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

## Testing

Run the test suite:

```bash
npm test
```

Run tests with coverage:

```bash
npm test -- --coverage
```

## License

MIT