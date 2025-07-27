# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

- `npm run dev` - Start development server with hot reload
- `npm run build` - Compile TypeScript to JavaScript  
- `npm run typecheck` - Run TypeScript type checking without compilation
- `npm run lint` - Run ESLint on TypeScript files
- `npm test` - Run Jest test suite
- `npm test -- --coverage` - Run tests with coverage report
- `npm test -- --watch` - Run tests in watch mode
- `npm test src/__tests__/SpecificFile.test.ts` - Run a single test file

## Architecture Overview

This is a prescription management service built with Express/TypeScript and DynamoDB. The architecture follows a layered approach:

**Controllers** (`src/controllers/`) handle HTTP requests/responses and coordinate between services. The main `PrescriptionController` orchestrates prescription operations, schedule generation, and clinical alerts.

**Models** (`src/models/`) provide DynamoDB data access layers. `PrescriptionModel` handles prescription CRUD operations using specific access patterns, while `ScheduleModel` manages medication schedules. Both use the DynamoDB DocumentClient with proper PK/SK patterns and GSI queries.

**Services** contain business logic:
- `ScheduleService` generates medication schedules based on prescription frequency and duration
- `ClinicalDecisionService` provides drug interaction checking, dosage validation, and clinical alerts

**DynamoDB Design Patterns**:
- Prescriptions table uses `PATIENT#{id}` as PK and `PRESCRIPTION#{id}` as SK
- GSI1 allows direct prescription lookup via `PRESCRIPTION#{id}` 
- GSI2 enables status-based queries via `STATUS#{status}`
- Schedule table uses `PRESCRIPTION#{id}` as PK and `SCHEDULE#{date}#{time}` as SK
- Schedule GSI1 enables patient schedule queries via `PATIENT#{id}`

**Key Integration Points**:
- When prescriptions are created/updated, schedules are automatically regenerated
- Clinical decision support runs during prescription creation, checking against existing patient prescriptions
- Schedule deletion is triggered when prescriptions are deleted or timing parameters change

## Environment Setup

Copy `.env.example` to `.env` and configure AWS credentials and DynamoDB table names. The service requires `PRESCRIPTIONS_TABLE` and `PRESCRIPTION_SCHEDULES_TABLE` environment variables.

## Testing Strategy

Tests use Jest with mocked dependencies. Controller tests mock the model and service layers. Service tests focus on business logic like schedule generation algorithms and clinical rule evaluation.

## API Design

All endpoints follow a consistent response format with `success` boolean and `data`/`error` fields. Clinical alerts are included in prescription creation responses when applicable. UUIDs are validated using Joi schemas.