package com.healthcare.prescription.controller

import com.healthcare.prescription.domain.*
import com.healthcare.prescription.repository.PrescriptionRepository
import com.healthcare.prescription.service.ClinicalDecisionService
import com.healthcare.prescription.service.ScheduleService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrescriptionControllerSimpleTest {

    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var scheduleService: ScheduleService
    private lateinit var clinicalDecisionService: ClinicalDecisionService
    private lateinit var controller: PrescriptionController

    @BeforeEach
    fun setUp() {
        prescriptionRepository = mock()
        scheduleService = mock()
        clinicalDecisionService = mock()
        controller = PrescriptionController(prescriptionRepository, scheduleService, clinicalDecisionService)
    }

    @Test
    fun `should return prescriptions for valid patient ID`() {
        val patientId = "123e4567-e89b-12d3-a456-426614174000"
        val mockPrescriptions = listOf(
            Prescription(
                prescriptionId = "456e7890-e89b-12d3-a456-426614174001",
                patientId = patientId,
                medicationName = "amoxicillin",
                dosage = 500.0,
                unit = "mg",
                frequencyHours = 8,
                startTime = LocalTime.of(8, 0),
                startDate = LocalDate.now().plusDays(1),
                endDate = LocalDate.now().plusDays(8),
                status = PrescriptionStatus.ACTIVE,
                prescribedBy = "Dr. Smith",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        whenever(prescriptionRepository.getPrescriptionsByPatient(patientId))
            .thenReturn(mockPrescriptions)

        val response = controller.getPatientPrescriptions(patientId)
        
        assertTrue(response.body().success)
        assertEquals(1, response.body().data?.size)
        assertEquals("amoxicillin", response.body().data?.get(0)?.medicationName)
    }

    @Test
    fun `should create prescription with clinical alerts`() {
        val patientId = "123e4567-e89b-12d3-a456-426614174000"
        val prescriptionData = CreatePrescriptionRequest(
            medicationName = "amoxicillin",
            dosage = 500.0,
            unit = "mg",
            frequencyHours = 8,
            startTime = LocalTime.of(8, 0),
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(8),
            prescribedBy = "Dr. Smith"
        )

        val mockPrescription = Prescription(
            prescriptionId = "456e7890-e89b-12d3-a456-426614174001",
            patientId = patientId,
            medicationName = prescriptionData.medicationName,
            dosage = prescriptionData.dosage,
            unit = prescriptionData.unit,
            frequencyHours = prescriptionData.frequencyHours,
            startTime = prescriptionData.startTime,
            startDate = prescriptionData.startDate,
            endDate = prescriptionData.endDate,
            status = PrescriptionStatus.ACTIVE,
            prescribedBy = prescriptionData.prescribedBy,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(prescriptionRepository.createPrescription(any(), any()))
            .thenReturn(mockPrescription)
        whenever(prescriptionRepository.getPrescriptionsByPatient(patientId))
            .thenReturn(emptyList())
        whenever(clinicalDecisionService.generateClinicalAlerts(any(), any()))
            .thenReturn(emptyList())
        whenever(scheduleService.generateSchedule(any()))
            .thenReturn(emptyList())

        val response = controller.createPrescription(patientId, prescriptionData)

        assertTrue(response.body().success)
        assertEquals("amoxicillin", response.body().data?.medicationName)
    }
}