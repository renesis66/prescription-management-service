package com.prescription.service

import com.prescription.domain.Prescription
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Introspected
@Serdeable
data class DrugInteraction(
    val severity: InteractionSeverity,
    val description: String,
    val medications: List<String>
)

enum class InteractionSeverity {
    MILD, MODERATE, SEVERE
}

@Introspected
@Serdeable
data class DosageAlert(
    val type: DosageAlertType,
    val message: String,
    val recommendation: String
)

enum class DosageAlertType {
    MAX_DOSE_EXCEEDED, FREQUENCY_TOO_HIGH, DURATION_TOO_LONG
}

@Introspected
@Serdeable
data class ClinicalAlert(
    val prescriptionId: String,
    val patientId: String,
    val alertType: ClinicalAlertType,
    val severity: AlertSeverity,
    val message: String,
    val recommendation: String,
    val createdAt: LocalDateTime
)

enum class ClinicalAlertType {
    DRUG_INTERACTION, DOSAGE_ALERT, ALLERGY_ALERT
}

enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Singleton
open class ClinicalDecisionService {

    private val drugInteractions = mapOf(
        "warfarin" to listOf(
            DrugInteraction(
                severity = InteractionSeverity.SEVERE,
                description = "Increased bleeding risk when combined with aspirin",
                medications = listOf("aspirin", "acetylsalicylic acid")
            )
        ),
        "amoxicillin" to listOf(
            DrugInteraction(
                severity = InteractionSeverity.MODERATE,
                description = "May reduce effectiveness of oral contraceptives",
                medications = listOf("ethinyl estradiol", "levonorgestrel")
            )
        )
    )

    private val maxDosages = mapOf(
        "acetaminophen" to MaxDosage(4000.0, "mg", "24h"),
        "ibuprofen" to MaxDosage(3200.0, "mg", "24h"),
        "amoxicillin" to MaxDosage(6000.0, "mg", "24h")
    )

    private val maxDurations = mapOf(
        "prednisone" to 14,
        "hydrocodone" to 7,
        "oxycodone" to 7
    )

    data class MaxDosage(val max: Double, val unit: String, val period: String)

    fun checkDrugInteractions(newPrescription: Prescription, existingPrescriptions: List<Prescription>): List<DrugInteraction> {
        val interactions = mutableListOf<DrugInteraction>()
        val newMedication = newPrescription.medicationName.lowercase()
        
        val potentialInteractions = drugInteractions[newMedication] ?: emptyList()
        
        for (interaction in potentialInteractions) {
            for (existingPrescription in existingPrescriptions) {
                if (existingPrescription.status.name == "ACTIVE") {
                    val existingMedication = existingPrescription.medicationName.lowercase()
                    if (interaction.medications.contains(existingMedication)) {
                        interactions.add(interaction)
                    }
                }
            }
        }
        
        return interactions
    }

    fun checkDosageAlerts(prescription: Prescription): List<DosageAlert> {
        val alerts = mutableListOf<DosageAlert>()
        val medication = prescription.medicationName.lowercase()
        val maxDosage = maxDosages[medication]
        
        if (maxDosage != null && prescription.unit == maxDosage.unit) {
            val dailyDose = (24.0 / prescription.frequencyHours) * prescription.dosage
            
            if (dailyDose > maxDosage.max) {
                alerts.add(
                    DosageAlert(
                        type = DosageAlertType.MAX_DOSE_EXCEEDED,
                        message = "Daily dose of ${dailyDose}${prescription.unit} exceeds maximum recommended dose of ${maxDosage.max}${maxDosage.unit}",
                        recommendation = "Consider reducing dose or frequency. Maximum daily dose should not exceed ${maxDosage.max}${maxDosage.unit}."
                    )
                )
            }
        }
        
        if (prescription.frequencyHours < 4) {
            alerts.add(
                DosageAlert(
                    type = DosageAlertType.FREQUENCY_TOO_HIGH,
                    message = "Dosing frequency is very high (less than 4 hours between doses)",
                    recommendation = "Consider extending the interval between doses to reduce risk of side effects."
                )
            )
        }
        
        val maxDuration = maxDurations[medication]
        if (maxDuration != null) {
            val durationDays = ChronoUnit.DAYS.between(prescription.startDate, prescription.endDate)
            
            if (durationDays > maxDuration) {
                alerts.add(
                    DosageAlert(
                        type = DosageAlertType.DURATION_TOO_LONG,
                        message = "Treatment duration of $durationDays days exceeds recommended maximum of $maxDuration days",
                        recommendation = "Consider limiting treatment duration to $maxDuration days or provide additional monitoring."
                    )
                )
            }
        }
        
        return alerts
    }

    fun generateClinicalAlerts(prescription: Prescription, existingPrescriptions: List<Prescription>): List<ClinicalAlert> {
        val alerts = mutableListOf<ClinicalAlert>()
        val now = LocalDateTime.now()
        
        val interactions = checkDrugInteractions(prescription, existingPrescriptions)
        for (interaction in interactions) {
            alerts.add(
                ClinicalAlert(
                    prescriptionId = prescription.prescriptionId,
                    patientId = prescription.patientId,
                    alertType = ClinicalAlertType.DRUG_INTERACTION,
                    severity = mapSeverity(interaction.severity),
                    message = "Drug interaction detected: ${interaction.description}",
                    recommendation = "Review interaction between ${prescription.medicationName} and ${interaction.medications.joinToString(", ")}. Consider alternative medications or additional monitoring.",
                    createdAt = now
                )
            )
        }
        
        val dosageAlerts = checkDosageAlerts(prescription)
        for (dosageAlert in dosageAlerts) {
            alerts.add(
                ClinicalAlert(
                    prescriptionId = prescription.prescriptionId,
                    patientId = prescription.patientId,
                    alertType = ClinicalAlertType.DOSAGE_ALERT,
                    severity = mapDosageAlertSeverity(dosageAlert.type),
                    message = dosageAlert.message,
                    recommendation = dosageAlert.recommendation,
                    createdAt = now
                )
            )
        }
        
        return alerts
    }

    private fun mapSeverity(interactionSeverity: InteractionSeverity): AlertSeverity {
        return when (interactionSeverity) {
            InteractionSeverity.MILD -> AlertSeverity.LOW
            InteractionSeverity.MODERATE -> AlertSeverity.MEDIUM
            InteractionSeverity.SEVERE -> AlertSeverity.CRITICAL
        }
    }

    private fun mapDosageAlertSeverity(alertType: DosageAlertType): AlertSeverity {
        return when (alertType) {
            DosageAlertType.MAX_DOSE_EXCEEDED -> AlertSeverity.CRITICAL
            DosageAlertType.FREQUENCY_TOO_HIGH -> AlertSeverity.HIGH
            DosageAlertType.DURATION_TOO_LONG -> AlertSeverity.MEDIUM
        }
    }
}