import { Prescription, PrescriptionSchedule } from '../types';
import { ScheduleModel } from '../models/ScheduleModel';

export class ScheduleService {
  private scheduleModel: ScheduleModel;

  constructor() {
    this.scheduleModel = new ScheduleModel();
  }

  async generateSchedule(prescription: Prescription): Promise<PrescriptionSchedule[]> {
    const schedules: PrescriptionSchedule[] = [];
    const startDate = new Date(prescription.startDate);
    const endDate = new Date(prescription.endDate);
    
    for (let date = new Date(startDate); date <= endDate; date.setDate(date.getDate() + 1)) {
      const currentDate = date.toISOString().split('T')[0];
      
      for (let hour = 0; hour < 24; hour += prescription.frequencyHours) {
        const [startHour, startMinute] = prescription.startTime.split(':').map(Number);
        const scheduledHour = (startHour + hour) % 24;
        
        if (hour + startHour >= 24 && date.getTime() === endDate.getTime()) {
          break;
        }
        
        const scheduledTime = `${scheduledHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}`;
        
        const schedule: PrescriptionSchedule = {
          prescriptionId: prescription.prescriptionId,
          patientId: prescription.patientId,
          scheduledDate: currentDate,
          scheduledTime,
          dosage: prescription.dosage,
          unit: prescription.unit,
          status: 'PENDING'
        };
        
        schedules.push(schedule);
      }
    }
    
    for (const schedule of schedules) {
      await this.scheduleModel.createScheduleEntry(schedule);
    }
    
    return schedules;
  }

  async getPrescriptionSchedule(prescriptionId: string): Promise<PrescriptionSchedule[]> {
    return this.scheduleModel.getScheduleByPrescription(prescriptionId);
  }

  async getPatientSchedule(patientId: string, date?: string): Promise<PrescriptionSchedule[]> {
    return this.scheduleModel.getPatientSchedule(patientId, date);
  }

  async updateScheduleStatus(
    prescriptionId: string, 
    scheduledDate: string, 
    scheduledTime: string, 
    status: 'PENDING' | 'TAKEN' | 'MISSED'
  ): Promise<PrescriptionSchedule | null> {
    return this.scheduleModel.updateScheduleStatus(prescriptionId, scheduledDate, scheduledTime, status);
  }

  async deleteScheduleForPrescription(prescriptionId: string): Promise<void> {
    return this.scheduleModel.deleteScheduleByPrescription(prescriptionId);
  }
}