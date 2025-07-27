import { Prescription } from '../types';

export interface DrugInteraction {
  severity: 'MILD' | 'MODERATE' | 'SEVERE';
  description: string;
  medications: string[];
}

export interface DosageAlert {
  type: 'MAX_DOSE_EXCEEDED' | 'FREQUENCY_TOO_HIGH' | 'DURATION_TOO_LONG';
  message: string;
  recommendation: string;
}

export interface ClinicalAlert {
  prescriptionId: string;
  patientId: string;
  alertType: 'DRUG_INTERACTION' | 'DOSAGE_ALERT' | 'ALLERGY_ALERT';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  message: string;
  recommendation: string;
  createdAt: string;
}

export class ClinicalDecisionService {
  private drugInteractions: Map<string, DrugInteraction[]>;
  private maxDosages: Map<string, { max: number; unit: string; period: string }>;
  private maxDurations: Map<string, number>;

  constructor() {
    this.drugInteractions = new Map();
    this.maxDosages = new Map();
    this.maxDurations = new Map();
    this.initializeClinicalData();
  }

  private initializeClinicalData(): void {
    this.drugInteractions.set('warfarin', [
      {
        severity: 'SEVERE',
        description: 'Increased bleeding risk when combined with aspirin',
        medications: ['aspirin', 'acetylsalicylic acid']
      }
    ]);

    this.drugInteractions.set('amoxicillin', [
      {
        severity: 'MODERATE',
        description: 'May reduce effectiveness of oral contraceptives',
        medications: ['ethinyl estradiol', 'levonorgestrel']
      }
    ]);

    this.maxDosages.set('acetaminophen', { max: 4000, unit: 'mg', period: '24h' });
    this.maxDosages.set('ibuprofen', { max: 3200, unit: 'mg', period: '24h' });
    this.maxDosages.set('amoxicillin', { max: 6000, unit: 'mg', period: '24h' });

    this.maxDurations.set('prednisone', 14);
    this.maxDurations.set('hydrocodone', 7);
    this.maxDurations.set('oxycodone', 7);
  }

  async checkDrugInteractions(newPrescription: Prescription, existingPrescriptions: Prescription[]): Promise<DrugInteraction[]> {
    const interactions: DrugInteraction[] = [];
    const newMedication = newPrescription.medicationName.toLowerCase();
    
    const potentialInteractions = this.drugInteractions.get(newMedication) || [];
    
    for (const interaction of potentialInteractions) {
      for (const existingPrescription of existingPrescriptions) {
        if (existingPrescription.status === 'ACTIVE') {
          const existingMedication = existingPrescription.medicationName.toLowerCase();
          if (interaction.medications.includes(existingMedication)) {
            interactions.push(interaction);
          }
        }
      }
    }
    
    return interactions;
  }

  async checkDosageAlerts(prescription: Prescription): Promise<DosageAlert[]> {
    const alerts: DosageAlert[] = [];
    const medication = prescription.medicationName.toLowerCase();
    const maxDosage = this.maxDosages.get(medication);
    
    if (maxDosage && prescription.unit === maxDosage.unit) {
      const dailyDose = (24 / prescription.frequencyHours) * prescription.dosage;
      
      if (dailyDose > maxDosage.max) {
        alerts.push({
          type: 'MAX_DOSE_EXCEEDED',
          message: `Daily dose of ${dailyDose}${prescription.unit} exceeds maximum recommended dose of ${maxDosage.max}${maxDosage.unit}`,
          recommendation: `Consider reducing dose or frequency. Maximum daily dose should not exceed ${maxDosage.max}${maxDosage.unit}.`
        });
      }
    }
    
    if (prescription.frequencyHours < 4) {
      alerts.push({
        type: 'FREQUENCY_TOO_HIGH',
        message: 'Dosing frequency is very high (less than 4 hours between doses)',
        recommendation: 'Consider extending the interval between doses to reduce risk of side effects.'
      });
    }
    
    const maxDuration = this.maxDurations.get(medication);
    if (maxDuration) {
      const startDate = new Date(prescription.startDate);
      const endDate = new Date(prescription.endDate);
      const durationDays = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
      
      if (durationDays > maxDuration) {
        alerts.push({
          type: 'DURATION_TOO_LONG',
          message: `Treatment duration of ${durationDays} days exceeds recommended maximum of ${maxDuration} days`,
          recommendation: `Consider limiting treatment duration to ${maxDuration} days or provide additional monitoring.`
        });
      }
    }
    
    return alerts;
  }

  async generateClinicalAlerts(prescription: Prescription, existingPrescriptions: Prescription[]): Promise<ClinicalAlert[]> {
    const alerts: ClinicalAlert[] = [];
    const now = new Date().toISOString();
    
    const interactions = await this.checkDrugInteractions(prescription, existingPrescriptions);
    for (const interaction of interactions) {
      alerts.push({
        prescriptionId: prescription.prescriptionId,
        patientId: prescription.patientId,
        alertType: 'DRUG_INTERACTION',
        severity: this.mapSeverity(interaction.severity),
        message: `Drug interaction detected: ${interaction.description}`,
        recommendation: `Review interaction between ${prescription.medicationName} and ${interaction.medications.join(', ')}. Consider alternative medications or additional monitoring.`,
        createdAt: now
      });
    }
    
    const dosageAlerts = await this.checkDosageAlerts(prescription);
    for (const dosageAlert of dosageAlerts) {
      alerts.push({
        prescriptionId: prescription.prescriptionId,
        patientId: prescription.patientId,
        alertType: 'DOSAGE_ALERT',
        severity: this.mapDosageAlertSeverity(dosageAlert.type),
        message: dosageAlert.message,
        recommendation: dosageAlert.recommendation,
        createdAt: now
      });
    }
    
    return alerts;
  }

  private mapSeverity(interactionSeverity: 'MILD' | 'MODERATE' | 'SEVERE'): 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' {
    switch (interactionSeverity) {
      case 'MILD': return 'LOW';
      case 'MODERATE': return 'MEDIUM';
      case 'SEVERE': return 'CRITICAL';
      default: return 'MEDIUM';
    }
  }

  private mapDosageAlertSeverity(alertType: 'MAX_DOSE_EXCEEDED' | 'FREQUENCY_TOO_HIGH' | 'DURATION_TOO_LONG'): 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' {
    switch (alertType) {
      case 'MAX_DOSE_EXCEEDED': return 'CRITICAL';
      case 'FREQUENCY_TOO_HIGH': return 'HIGH';
      case 'DURATION_TOO_LONG': return 'MEDIUM';
      default: return 'MEDIUM';
    }
  }
}