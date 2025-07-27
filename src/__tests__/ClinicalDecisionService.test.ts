import { ClinicalDecisionService } from '../services/ClinicalDecisionService';
import { Prescription } from '../types';

describe('ClinicalDecisionService', () => {
  let service: ClinicalDecisionService;
  let mockPrescription: Prescription;

  beforeEach(() => {
    service = new ClinicalDecisionService();
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const weekLater = new Date();
    weekLater.setDate(weekLater.getDate() + 8);
    
    mockPrescription = {
      prescriptionId: '456e7890-e89b-12d3-a456-426614174001',
      patientId: '123e4567-e89b-12d3-a456-426614174000',
      medicationName: 'amoxicillin',
      dosage: 500,
      unit: 'mg',
      frequencyHours: 8,
      startTime: '08:00',
      startDate: tomorrow.toISOString().split('T')[0],
      endDate: weekLater.toISOString().split('T')[0],
      status: 'ACTIVE',
      prescribedBy: 'Dr. Smith',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
  });

  describe('checkDrugInteractions', () => {
    it('should detect drug interactions', async () => {
      const existingPrescriptions: Prescription[] = [
        {
          ...mockPrescription,
          prescriptionId: 'existing-id',
          medicationName: 'ethinyl estradiol'
        }
      ];

      const interactions = await service.checkDrugInteractions(mockPrescription, existingPrescriptions);
      
      expect(interactions).toHaveLength(1);
      expect(interactions[0].severity).toBe('MODERATE');
      expect(interactions[0].description).toContain('oral contraceptives');
    });

    it('should not detect interactions with inactive prescriptions', async () => {
      const existingPrescriptions: Prescription[] = [
        {
          ...mockPrescription,
          prescriptionId: 'existing-id',
          medicationName: 'ethinyl estradiol',
          status: 'COMPLETED'
        }
      ];

      const interactions = await service.checkDrugInteractions(mockPrescription, existingPrescriptions);
      
      expect(interactions).toHaveLength(0);
    });
  });

  describe('checkDosageAlerts', () => {
    it('should detect max dose exceeded', async () => {
      const highDosePrescription: Prescription = {
        ...mockPrescription,
        medicationName: 'acetaminophen',
        dosage: 2000,
        unit: 'mg',
        frequencyHours: 4
      };

      const alerts = await service.checkDosageAlerts(highDosePrescription);
      
      expect(alerts).toHaveLength(1);
      expect(alerts[0].type).toBe('MAX_DOSE_EXCEEDED');
      expect(alerts[0].message).toContain('exceeds maximum recommended dose');
    });

    it('should detect high frequency dosing', async () => {
      const highFreqPrescription: Prescription = {
        ...mockPrescription,
        frequencyHours: 2
      };

      const alerts = await service.checkDosageAlerts(highFreqPrescription);
      
      expect(alerts).toHaveLength(1);
      expect(alerts[0].type).toBe('FREQUENCY_TOO_HIGH');
    });

    it('should detect long duration treatment', async () => {
      const startDate = new Date();
      startDate.setDate(startDate.getDate() + 1);
      const endDate = new Date();
      endDate.setDate(endDate.getDate() + 32); // 31 days later
      
      const longTreatment: Prescription = {
        ...mockPrescription,
        medicationName: 'prednisone',
        startDate: startDate.toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0]
      };

      const alerts = await service.checkDosageAlerts(longTreatment);
      
      expect(alerts).toHaveLength(1);
      expect(alerts[0].type).toBe('DURATION_TOO_LONG');
    });
  });

  describe('generateClinicalAlerts', () => {
    it('should generate comprehensive clinical alerts', async () => {
      const highDosePrescription: Prescription = {
        ...mockPrescription,
        medicationName: 'acetaminophen',
        dosage: 2000,
        unit: 'mg',
        frequencyHours: 2
      };

      const alerts = await service.generateClinicalAlerts(highDosePrescription, []);
      
      expect(alerts.length).toBeGreaterThan(0);
      expect(alerts.every(alert => alert.prescriptionId === highDosePrescription.prescriptionId)).toBe(true);
      expect(alerts.every(alert => alert.patientId === highDosePrescription.patientId)).toBe(true);
      expect(alerts.every(alert => alert.createdAt)).toBe(true);
    });
  });
});