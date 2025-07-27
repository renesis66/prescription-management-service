package com.prescription.service

import com.prescription.domain.Prescription
import com.prescription.domain.PrescriptionStatus
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@MicronautTest
class ClinicalDecisionServiceTest {

    @Inject
    lateinit var service: ClinicalDecisionService

    private lateinit var mockPrescription: Prescription

    @BeforeEach
    fun setUp() {
        val tomorrow = LocalDate.now().plusDays(1)
        val weekLater = LocalDate.now().plusDays(8)
        
        mockPrescription = Prescription(
            prescriptionId = "456e7890-e89b-12d3-a456-426614174001",
            patientId = "123e4567-e89b-12d3-a456-426614174000",
            medicationName = "amoxicillin",
            dosage = 500.0,
            unit = "mg",
            frequencyHours = 8,
            startTime = LocalTime.of(8, 0),
            startDate = tomorrow,
            endDate = weekLater,
            status = PrescriptionStatus.ACTIVE,
            prescribedBy = "Dr. Smith",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `should detect drug interactions`() {
        val existingPrescriptions = listOf(
            mockPrescription.copy(
                prescriptionId = "existing-id",
                medicationName = "ethinyl estradiol"
            )
        )

        val interactions = service.checkDrugInteractions(mockPrescription, existingPrescriptions)
        
        assertThat(interactions).hasSize(1)
        assertThat(interactions[0].severity).isEqualTo(InteractionSeverity.MODERATE)
        assertThat(interactions[0].description).contains("oral contraceptives")
    }

    @Test
    fun `should not detect interactions with inactive prescriptions`() {
        val existingPrescriptions = listOf(
            mockPrescription.copy(
                prescriptionId = "existing-id",
                medicationName = "ethinyl estradiol",
                status = PrescriptionStatus.COMPLETED
            )
        )

        val interactions = service.checkDrugInteractions(mockPrescription, existingPrescriptions)
        
        assertThat(interactions).isEmpty()
    }

    @Test
    fun `should detect max dose exceeded`() {
        val highDosePrescription = mockPrescription.copy(
            medicationName = "acetaminophen",
            dosage = 2000.0,
            unit = "mg",
            frequencyHours = 4
        )

        val alerts = service.checkDosageAlerts(highDosePrescription)
        
        assertThat(alerts).hasSize(1)
        assertThat(alerts[0].type).isEqualTo(DosageAlertType.MAX_DOSE_EXCEEDED)
        assertThat(alerts[0].message).contains("exceeds maximum recommended dose")
    }

    @Test
    fun `should detect high frequency dosing`() {
        val highFreqPrescription = mockPrescription.copy(frequencyHours = 2)

        val alerts = service.checkDosageAlerts(highFreqPrescription)
        
        assertThat(alerts).hasSize(1)
        assertThat(alerts[0].type).isEqualTo(DosageAlertType.FREQUENCY_TOO_HIGH)
    }

    @Test
    fun `should detect long duration treatment`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = startDate.plusDays(31)
        
        val longTreatment = mockPrescription.copy(
            medicationName = "prednisone",
            startDate = startDate,
            endDate = endDate
        )

        val alerts = service.checkDosageAlerts(longTreatment)
        
        assertThat(alerts).hasSize(1)
        assertThat(alerts[0].type).isEqualTo(DosageAlertType.DURATION_TOO_LONG)
    }

    @Test
    fun `should generate comprehensive clinical alerts`() {
        val highDosePrescription = mockPrescription.copy(
            medicationName = "acetaminophen",
            dosage = 2000.0,
            unit = "mg",
            frequencyHours = 2
        )

        val alerts = service.generateClinicalAlerts(highDosePrescription, emptyList())
        
        assertThat(alerts).isNotEmpty
        assertThat(alerts.all { it.prescriptionId == highDosePrescription.prescriptionId }).isTrue
        assertThat(alerts.all { it.patientId == highDosePrescription.patientId }).isTrue
        assertThat(alerts.all { it.createdAt != null }).isTrue
    }
}