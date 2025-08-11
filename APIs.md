# Prescription Management Service APIs

This document describes all the REST API endpoints supported by the prescription management service.

## Base URL
All endpoints are prefixed with `/api`

## Authentication
All endpoints (except `/health`) require authentication with valid JWT tokens and appropriate role-based access control.

## Endpoints

### 1. Get Patient Prescriptions
**GET** `/api/patients/{patientId}/prescriptions`

Retrieves all prescriptions for a specific patient.

**Path Parameters:**
- `patientId` (string, required): UUID format patient identifier

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "prescriptionId": "uuid",
      "patientId": "uuid",
      "medicationName": "string",
      "dosage": 5.0,
      "unit": "mg",
      "frequencyHours": 8,
      "startTime": "08:00",
      "startDate": "2024-01-15",
      "endDate": "2024-01-29",
      "status": "ACTIVE",
      "prescribedBy": "Dr. Smith",
      "createdAt": "2024-01-01T10:00:00Z",
      "updatedAt": "2024-01-01T10:00:00Z"
    }
  ],
  "error": null
}
```

**Status Codes:**
- 200: Success
- 400: Bad request (invalid patient ID format)

### 2. Create Prescription
**POST** `/api/patients/{patientId}/prescriptions`

Creates a new prescription for a patient with clinical decision support.

**Path Parameters:**
- `patientId` (string, required): UUID format patient identifier

**Request Body:**
```json
{
  "medicationName": "Aspirin",
  "dosage": 5.0,
  "unit": "mg",
  "frequencyHours": 8,
  "startTime": "08:00",
  "startDate": "2024-01-15",
  "endDate": "2024-01-29",
  "prescribedBy": "Dr. Smith"
}
```

**Validation Rules:**
- `medicationName`: 1-100 characters, not blank
- `dosage`: Positive number
- `unit`: Must be one of: "mg", "g", "ml", "tablets", "capsules"
- `frequencyHours`: 1-24 hours
- `startDate/endDate`: Must be future dates, end date after start date
- `prescribedBy`: 1-100 characters, not blank

**Response:**
```json
{
  "success": true,
  "data": {
    "prescriptionId": "uuid",
    "patientId": "uuid",
    "medicationName": "Aspirin",
    "dosage": 5.0,
    "unit": "mg",
    "frequencyHours": 8,
    "startTime": "08:00",
    "startDate": "2024-01-15",
    "endDate": "2024-01-29",
    "status": "ACTIVE",
    "prescribedBy": "Dr. Smith",
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-01T10:00:00Z"
  },
  "clinicalAlerts": [
    {
      "prescriptionId": "uuid",
      "patientId": "uuid",
      "alertType": "DRUG_INTERACTION",
      "severity": "MODERATE",
      "message": "Potential interaction detected",
      "recommendation": "Monitor patient closely",
      "createdAt": "2024-01-01T10:00:00Z"
    }
  ],
  "error": null
}
```

**Status Codes:**
- 201: Created successfully
- 400: Validation error or business rule violation

### 3. Update Prescription
**PUT** `/api/prescriptions/{id}`

Updates an existing prescription. Automatically regenerates schedule if timing parameters change.

**Path Parameters:**
- `id` (string, required): UUID format prescription identifier

**Request Body (all fields optional):**
```json
{
  "medicationName": "New Medication",
  "dosage": 10.0,
  "unit": "mg",
  "frequencyHours": 12,
  "startTime": "09:00",
  "startDate": "2024-01-20",
  "endDate": "2024-02-03",
  "status": "ACTIVE"
}
```

**Validation Rules:** Same as create prescription (when provided)

**Response:**
```json
{
  "success": true,
  "data": {
    "prescriptionId": "uuid",
    "patientId": "uuid",
    "medicationName": "New Medication",
    "dosage": 10.0,
    "unit": "mg",
    "frequencyHours": 12,
    "startTime": "09:00",
    "startDate": "2024-01-20",
    "endDate": "2024-02-03",
    "status": "ACTIVE",
    "prescribedBy": "Dr. Smith",
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-15T14:30:00Z"
  },
  "error": null
}
```

**Status Codes:**
- 200: Updated successfully  
- 400: Validation error
- 404: Prescription not found

### 4. Delete Prescription
**DELETE** `/api/prescriptions/{id}`

Deletes a prescription and its associated schedule.

**Path Parameters:**
- `id` (string, required): UUID format prescription identifier

**Response:**
```json
{
  "success": true,
  "data": "Prescription deleted successfully",
  "error": null
}
```

**Status Codes:**
- 200: Deleted successfully
- 400: Error during deletion
- 404: Prescription not found

### 5. Get Prescription Schedule
**GET** `/api/prescriptions/{id}/schedule`

Retrieves the medication schedule for a specific prescription.

**Path Parameters:**
- `id` (string, required): UUID format prescription identifier

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "prescriptionId": "uuid",
      "patientId": "uuid",
      "scheduledDate": "2024-01-15",
      "scheduledTime": "08:00",
      "dosage": 5.0,
      "unit": "mg",
      "status": "PENDING"
    }
  ],
  "error": null
}
```

**Schedule Status Values:**
- `PENDING`: Not yet taken
- `TAKEN`: Successfully administered
- `MISSED`: Missed administration time

**Status Codes:**
- 200: Success
- 400: Error retrieving schedule

### 6. Health Check
**GET** `/api/health`

Returns service health status. No authentication required.

**Response:**
```json
{
  "status": "healthy",
  "service": "prescription-management-service",
  "timestamp": "2024-01-01T10:00:00.000Z"
}
```

**Status Codes:**
- 200: Service is healthy

## Data Models

### Prescription Status
- `ACTIVE`: Currently active prescription
- `COMPLETED`: Treatment completed
- `CANCELLED`: Prescription cancelled

### Clinical Alert Types
- `DRUG_INTERACTION`: Drug interaction detected
- `DOSAGE_ALERT`: Dosage concerns (max dose, frequency, duration)
- `ALLERGY_ALERT`: Patient allergy concerns

### Alert Severity Levels
- `LOW`: Informational
- `MEDIUM`: Requires attention
- `HIGH`: Immediate action required

## Error Response Format
All endpoints return consistent error responses:

```json
{
  "success": false,
  "data": null,
  "error": "Error description message"
}
```

## Notes
- All date fields use ISO format (YYYY-MM-DD)
- All time fields use 24-hour format (HH:mm)
- All datetime fields use ISO 8601 format with UTC timezone
- UUID validation is enforced for all ID parameters
- The service automatically generates medication schedules upon prescription creation
- Clinical decision support runs automatically during prescription creation
- Schedule regeneration occurs automatically when timing parameters are updated