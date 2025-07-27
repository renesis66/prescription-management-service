import { Request, Response } from 'express';
import { PrescriptionModel } from '../models/PrescriptionModel';
import { ScheduleService } from '../services/ScheduleService';
import { ClinicalDecisionService } from '../services/ClinicalDecisionService';
import { CreatePrescriptionRequest, UpdatePrescriptionRequest } from '../types';
import { 
  validateRequest, 
  createPrescriptionSchema, 
  updatePrescriptionSchema,
  patientIdSchema,
  prescriptionIdSchema 
} from '../utils/validation';

export class PrescriptionController {
  private prescriptionModel: PrescriptionModel;
  private scheduleService: ScheduleService;
  private clinicalDecisionService: ClinicalDecisionService;

  constructor() {
    this.prescriptionModel = new PrescriptionModel();
    this.scheduleService = new ScheduleService();
    this.clinicalDecisionService = new ClinicalDecisionService();
  }

  async getPatientPrescriptions(req: Request, res: Response): Promise<void> {
    try {
      const patientId = validateRequest(patientIdSchema, req.params.patientId);
      
      const prescriptions = await this.prescriptionModel.getPrescriptionsByPatient(patientId);
      
      res.json({
        success: true,
        data: prescriptions
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      });
    }
  }

  async createPrescription(req: Request, res: Response): Promise<void> {
    try {
      const patientId = validateRequest(patientIdSchema, req.params.patientId);
      const prescriptionData = validateRequest(createPrescriptionSchema, req.body) as CreatePrescriptionRequest;
      
      const prescription = await this.prescriptionModel.createPrescription(patientId, {
        ...prescriptionData,
        status: 'ACTIVE'
      });
      
      const existingPrescriptions = await this.prescriptionModel.getPrescriptionsByPatient(patientId);
      const clinicalAlerts = await this.clinicalDecisionService.generateClinicalAlerts(
        prescription, 
        existingPrescriptions.filter(p => p.prescriptionId !== prescription.prescriptionId)
      );
      
      await this.scheduleService.generateSchedule(prescription);
      
      res.status(201).json({
        success: true,
        data: prescription,
        clinicalAlerts: clinicalAlerts.length > 0 ? clinicalAlerts : undefined
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      });
    }
  }

  async updatePrescription(req: Request, res: Response): Promise<void> {
    try {
      const prescriptionId = validateRequest(prescriptionIdSchema, req.params.id);
      const updates = validateRequest(updatePrescriptionSchema, req.body) as UpdatePrescriptionRequest;
      
      const updatedPrescription = await this.prescriptionModel.updatePrescription(prescriptionId, updates);
      
      if (!updatedPrescription) {
        res.status(404).json({
          success: false,
          error: 'Prescription not found'
        });
        return;
      }

      if (updates.frequencyHours || updates.startTime || updates.startDate || updates.endDate) {
        await this.scheduleService.deleteScheduleForPrescription(prescriptionId);
        await this.scheduleService.generateSchedule(updatedPrescription);
      }
      
      res.json({
        success: true,
        data: updatedPrescription
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      });
    }
  }

  async deletePrescription(req: Request, res: Response): Promise<void> {
    try {
      const prescriptionId = validateRequest(prescriptionIdSchema, req.params.id);
      
      const deleted = await this.prescriptionModel.deletePrescription(prescriptionId);
      
      if (!deleted) {
        res.status(404).json({
          success: false,
          error: 'Prescription not found'
        });
        return;
      }

      await this.scheduleService.deleteScheduleForPrescription(prescriptionId);
      
      res.json({
        success: true,
        message: 'Prescription deleted successfully'
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      });
    }
  }

  async getPrescriptionSchedule(req: Request, res: Response): Promise<void> {
    try {
      const prescriptionId = validateRequest(prescriptionIdSchema, req.params.id);
      
      const schedule = await this.scheduleService.getPrescriptionSchedule(prescriptionId);
      
      res.json({
        success: true,
        data: schedule
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      });
    }
  }
}