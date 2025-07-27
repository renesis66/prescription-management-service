package com.prescription.domain

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Introspected
@Serdeable
data class Prescription(
    val prescriptionId: String,
    val patientId: String,
    
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val medicationName: String,
    
    @field:Positive
    val dosage: Double,
    
    @field:Pattern(regexp = "^(mg|g|ml|tablets|capsules)$")
    val unit: String,
    
    @field:Min(1)
    @field:Max(24)
    val frequencyHours: Int,
    
    @JsonFormat(pattern = "HH:mm")
    val startTime: LocalTime,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd") 
    val endDate: LocalDate,
    
    val status: PrescriptionStatus,
    
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val prescribedBy: String,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val createdAt: LocalDateTime,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val updatedAt: LocalDateTime
)

enum class PrescriptionStatus {
    ACTIVE, COMPLETED, CANCELLED
}

@Introspected
@Serdeable
data class PrescriptionSchedule(
    val prescriptionId: String,
    val patientId: String,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    val scheduledDate: LocalDate,
    
    @JsonFormat(pattern = "HH:mm")
    val scheduledTime: LocalTime,
    
    @field:Positive
    val dosage: Double,
    
    @field:Pattern(regexp = "^(mg|g|ml|tablets|capsules)$")
    val unit: String,
    
    val status: ScheduleStatus
)

enum class ScheduleStatus {
    PENDING, TAKEN, MISSED
}

@Introspected
@Serdeable
data class CreatePrescriptionRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val medicationName: String,
    
    @field:Positive
    val dosage: Double,
    
    @field:Pattern(regexp = "^(mg|g|ml|tablets|capsules)$")
    val unit: String,
    
    @field:Min(1)
    @field:Max(24)
    val frequencyHours: Int,
    
    @JsonFormat(pattern = "HH:mm")
    val startTime: LocalTime,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @field:Future
    val startDate: LocalDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @field:Future
    val endDate: LocalDate,
    
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val prescribedBy: String
)

@Introspected
@Serdeable
data class UpdatePrescriptionRequest(
    @field:Size(min = 1, max = 100)
    val medicationName: String? = null,
    
    @field:Positive
    val dosage: Double? = null,
    
    @field:Pattern(regexp = "^(mg|g|ml|tablets|capsules)$")
    val unit: String? = null,
    
    @field:Min(1)
    @field:Max(24)
    val frequencyHours: Int? = null,
    
    @JsonFormat(pattern = "HH:mm")
    val startTime: LocalTime? = null,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate? = null,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate? = null,
    
    val status: PrescriptionStatus? = null
)