import { Request, Response } from 'express';
import { PrescriptionController } from '../controllers/PrescriptionController';
import { PrescriptionModel } from '../models/PrescriptionModel';
import { ScheduleService } from '../services/ScheduleService';
import { ClinicalDecisionService } from '../services/ClinicalDecisionService';

jest.mock('../models/PrescriptionModel');
jest.mock('../services/ScheduleService');
jest.mock('../services/ClinicalDecisionService');

describe('PrescriptionController', () => {
  let controller: PrescriptionController;
  let mockPrescriptionModel: jest.Mocked<PrescriptionModel>;
  let mockScheduleService: jest.Mocked<ScheduleService>;
  let mockClinicalService: jest.Mocked<ClinicalDecisionService>;
  let mockRequest: Partial<Request>;
  let mockResponse: Partial<Response>;

  beforeEach(() => {
    mockPrescriptionModel = new PrescriptionModel() as jest.Mocked<PrescriptionModel>;
    mockScheduleService = new ScheduleService() as jest.Mocked<ScheduleService>;
    mockClinicalService = new ClinicalDecisionService() as jest.Mocked<ClinicalDecisionService>;
    
    controller = new PrescriptionController();
    (controller as any).prescriptionModel = mockPrescriptionModel;
    (controller as any).scheduleService = mockScheduleService;
    (controller as any).clinicalDecisionService = mockClinicalService;

    mockRequest = {};
    mockResponse = {
      json: jest.fn(),
      status: jest.fn().mockReturnThis(),
    };
  });

  describe('getPatientPrescriptions', () => {
    it('should return prescriptions for a valid patient ID', async () => {
      const patientId = '123e4567-e89b-12d3-a456-426614174000';
      const mockPrescriptions = [
        {
          prescriptionId: '456e7890-e89b-12d3-a456-426614174001',
          patientId,
          medicationName: 'amoxicillin',
          dosage: 500,
          unit: 'mg',
          frequencyHours: 8,
          startTime: '08:00',
          startDate: '2024-01-15',
          endDate: '2024-01-25',
          status: 'ACTIVE' as const,
          prescribedBy: 'Dr. Smith',
          createdAt: '2024-01-15T10:30:00Z',
          updatedAt: '2024-01-15T10:30:00Z'
        }
      ];

      mockRequest.params = { patientId };
      mockPrescriptionModel.getPrescriptionsByPatient.mockResolvedValue(mockPrescriptions);

      await controller.getPatientPrescriptions(mockRequest as Request, mockResponse as Response);

      expect(mockResponse.json).toHaveBeenCalledWith({
        success: true,
        data: mockPrescriptions
      });
    });

    it('should return 400 for invalid patient ID', async () => {
      mockRequest.params = { patientId: 'invalid-uuid' };

      await controller.getPatientPrescriptions(mockRequest as Request, mockResponse as Response);

      expect(mockResponse.status).toHaveBeenCalledWith(400);
      expect(mockResponse.json).toHaveBeenCalledWith({
        success: false,
        error: expect.stringContaining('Validation error')
      });
    });
  });

  describe('createPrescription', () => {
    it('should create prescription with clinical alerts', async () => {
      const patientId = '123e4567-e89b-12d3-a456-426614174000';
      const prescriptionData = {
        medicationName: 'amoxicillin',
        dosage: 500,
        unit: 'mg',
        frequencyHours: 8,
        startTime: '08:00',
        startDate: '2024-01-15',
        endDate: '2024-01-25',
        prescribedBy: 'Dr. Smith'
      };

      const mockPrescription = {
        prescriptionId: '456e7890-e89b-12d3-a456-426614174001',
        patientId,
        ...prescriptionData,
        status: 'ACTIVE' as const,
        createdAt: '2024-01-15T10:30:00Z',
        updatedAt: '2024-01-15T10:30:00Z'
      };

      const mockAlerts = [
        {
          prescriptionId: mockPrescription.prescriptionId,
          patientId,
          alertType: 'DRUG_INTERACTION' as const,
          severity: 'MEDIUM' as const,
          message: 'Drug interaction detected',
          recommendation: 'Review interaction',
          createdAt: '2024-01-15T10:30:00Z'
        }
      ];

      mockRequest.params = { patientId };
      mockRequest.body = prescriptionData;
      
      mockPrescriptionModel.createPrescription.mockResolvedValue(mockPrescription);
      mockPrescriptionModel.getPrescriptionsByPatient.mockResolvedValue([]);
      mockClinicalService.generateClinicalAlerts.mockResolvedValue(mockAlerts);
      mockScheduleService.generateSchedule.mockResolvedValue([]);

      await controller.createPrescription(mockRequest as Request, mockResponse as Response);

      expect(mockResponse.status).toHaveBeenCalledWith(201);
      expect(mockResponse.json).toHaveBeenCalledWith({
        success: true,
        data: mockPrescription,
        clinicalAlerts: mockAlerts
      });
    });
  });
});