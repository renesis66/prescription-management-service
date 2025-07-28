package com.healthcare.prescription.controller

import com.healthcare.prescription.domain.*
import com.healthcare.prescription.repository.PrescriptionRepository
import com.healthcare.prescription.service.ClinicalDecisionService
import com.healthcare.prescription.service.ScheduleService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.validation.Validated
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import java.util.*

@Controller("/api")
@Validated
class PrescriptionController(
    private val prescriptionRepository: PrescriptionRepository,
    private val scheduleService: ScheduleService,
    private val clinicalDecisionService: ClinicalDecisionService
) {

    @Get("/patients/{patientId}/prescriptions")
    fun getPatientPrescriptions(
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") 
        patientId: String
    ): HttpResponse<ApiResponse<List<Prescription>>> {
        return try {
            val prescriptions = prescriptionRepository.getPrescriptionsByPatient(patientId)
            HttpResponse.ok(ApiResponse.success(prescriptions))
        } catch (e: Exception) {
            HttpResponse.badRequest(ApiResponse.error<List<Prescription>>(e.message ?: "Unknown error occurred"))
        }
    }

    @Post("/patients/{patientId}/prescriptions")
    fun createPrescription(
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") 
        patientId: String,
        @Valid @Body prescriptionData: CreatePrescriptionRequest
    ): HttpResponse<ApiResponseWithAlerts<Prescription>> {
        return try {
            // Validate end date is after start date
            if (!prescriptionData.endDate.isAfter(prescriptionData.startDate)) {
                return HttpResponse.badRequest(
                    ApiResponseWithAlerts.error<Prescription>("End date must be after start date")
                )
            }
            
            val prescription = prescriptionRepository.createPrescription(patientId, prescriptionData)
            
            val existingPrescriptions = prescriptionRepository.getPrescriptionsByPatient(patientId)
            val clinicalAlerts = clinicalDecisionService.generateClinicalAlerts(
                prescription,
                existingPrescriptions.filter { it.prescriptionId != prescription.prescriptionId }
            )
            
            scheduleService.generateSchedule(prescription)
            
            HttpResponse.created(
                ApiResponseWithAlerts.success(
                    prescription,
                    if (clinicalAlerts.isNotEmpty()) clinicalAlerts else null
                )
            )
        } catch (e: Exception) {
            HttpResponse.badRequest(ApiResponseWithAlerts.error<Prescription>(e.message ?: "Unknown error occurred"))
        }
    }

    @Put("/prescriptions/{id}")
    fun updatePrescription(
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") 
        id: String,
        @Valid @Body updates: UpdatePrescriptionRequest
    ): HttpResponse<ApiResponse<Prescription>> {
        return try {
            val updatedPrescription = prescriptionRepository.updatePrescription(id, updates)
                ?: return HttpResponse.notFound(ApiResponse.error<Prescription>("Prescription not found"))

            // Regenerate schedule if timing parameters changed
            if (updates.frequencyHours != null || updates.startTime != null || 
                updates.startDate != null || updates.endDate != null) {
                scheduleService.deleteScheduleForPrescription(id)
                scheduleService.generateSchedule(updatedPrescription)
            }
            
            HttpResponse.ok(ApiResponse.success(updatedPrescription))
        } catch (e: Exception) {
            HttpResponse.badRequest(ApiResponse.error<Prescription>(e.message ?: "Unknown error occurred"))
        }
    }

    @Delete("/prescriptions/{id}")
    fun deletePrescription(
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") 
        id: String
    ): HttpResponse<ApiResponse<String>> {
        return try {
            val deleted = prescriptionRepository.deletePrescription(id)
            
            if (!deleted) {
                return HttpResponse.notFound(ApiResponse.error<String>("Prescription not found"))
            }

            scheduleService.deleteScheduleForPrescription(id)
            
            HttpResponse.ok(ApiResponse.success("Prescription deleted successfully"))
        } catch (e: Exception) {
            HttpResponse.badRequest(ApiResponse.error<String>(e.message ?: "Unknown error occurred"))
        }
    }

    @Get("/prescriptions/{id}/schedule")
    fun getPrescriptionSchedule(
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$") 
        id: String
    ): HttpResponse<ApiResponse<List<PrescriptionSchedule>>> {
        return try {
            val schedule = scheduleService.getPrescriptionSchedule(id)
            HttpResponse.ok(ApiResponse.success(schedule))
        } catch (e: Exception) {
            HttpResponse.badRequest(ApiResponse.error<List<PrescriptionSchedule>>(e.message ?: "Unknown error occurred"))
        }
    }

    @Get("/health")
    fun health(): HttpResponse<Map<String, Any>> {
        return HttpResponse.ok(
            mapOf(
                "status" to "healthy",
                "service" to "prescription-management-service",
                "timestamp" to java.time.Instant.now().toString()
            )
        )
    }
}

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(true, data)
        fun <T> error(message: String): ApiResponse<T> = ApiResponse(false, error = message)
    }
}

data class ApiResponseWithAlerts<T>(
    val success: Boolean,
    val data: T? = null,
    val clinicalAlerts: List<com.healthcare.prescription.service.ClinicalAlert>? = null,
    val error: String? = null
) {
    companion object {
        fun <T> success(data: T, alerts: List<com.healthcare.prescription.service.ClinicalAlert>? = null): ApiResponseWithAlerts<T> = 
            ApiResponseWithAlerts(true, data, alerts)
        fun <T> error(message: String): ApiResponseWithAlerts<T> = ApiResponseWithAlerts(false, error = message)
    }
}