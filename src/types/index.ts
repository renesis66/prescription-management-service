export interface Prescription {
  prescriptionId: string;
  patientId: string;
  medicationName: string;
  dosage: number;
  unit: string;
  frequencyHours: number;
  startTime: string;
  startDate: string;
  endDate: string;
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  prescribedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface PrescriptionSchedule {
  prescriptionId: string;
  patientId: string;
  scheduledDate: string;
  scheduledTime: string;
  dosage: number;
  unit: string;
  status: 'PENDING' | 'TAKEN' | 'MISSED';
}

export interface DynamoDBItem {
  PK: string;
  SK: string;
  GSI1PK: string;
  GSI1SK: string;
  GSI2PK?: string;
  GSI2SK?: string;
}

export interface CreatePrescriptionRequest {
  medicationName: string;
  dosage: number;
  unit: string;
  frequencyHours: number;
  startTime: string;
  startDate: string;
  endDate: string;
  prescribedBy: string;
}

export interface UpdatePrescriptionRequest {
  medicationName?: string;
  dosage?: number;
  unit?: string;
  frequencyHours?: number;
  startTime?: string;
  startDate?: string;
  endDate?: string;
  status?: 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
}