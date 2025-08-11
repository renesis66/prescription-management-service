package com.healthcare.prescription.repository

import com.healthcare.prescription.domain.*
import jakarta.inject.Singleton
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Singleton
open class PrescriptionRepository(
    private val prescriptionTable: DynamoDbTable<PrescriptionDynamoItem>
) {

    fun createPrescription(patientId: String, prescription: CreatePrescriptionRequest): Prescription {
        val prescriptionId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val nowString = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
        
        val item = PrescriptionDynamoItem(
            pk = "PATIENT#$patientId",
            sk = "PRESCRIPTION#$prescriptionId",
            gsi1Pk = "PRESCRIPTION#$prescriptionId",
            gsi1Sk = "METADATA",
            gsi2Pk = "STATUS#ACTIVE",
            gsi2Sk = nowString,
            prescriptionId = prescriptionId,
            patientId = patientId,
            medicationName = prescription.medicationName,
            dosage = prescription.dosage,
            unit = prescription.unit,
            frequencyHours = prescription.frequencyHours,
            startTime = prescription.startTime.toString(),
            startDate = prescription.startDate.toString(),
            endDate = prescription.endDate.toString(),
            status = "ACTIVE",
            prescribedBy = prescription.prescribedBy,
            createdAt = nowString,
            updatedAt = nowString
        )
        
        prescriptionTable.putItem(item)
        return toDomainModel(item)
    }

    fun getPrescriptionsByPatient(patientId: String): List<Prescription> {
        val queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(
                QueryConditional.keyEqualTo(
                    Key.builder()
                        .partitionValue("PATIENT#$patientId")
                        .build()
                )
            )
            .build()

        return prescriptionTable.query(queryRequest)
            .flatMap { it.items() }
            .map { toDomainModel(it) }
            .toList()
    }

    fun getPrescriptionById(prescriptionId: String): Prescription? {
        val queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(
                QueryConditional.keyEqualTo(
                    Key.builder()
                        .partitionValue("PRESCRIPTION#$prescriptionId")
                        .sortValue("METADATA")
                        .build()
                )
            )
            .build()

        return prescriptionTable.index("GSI1")
            .query(queryRequest)
            .flatMap { it.items() }
            .firstOrNull()
            ?.let { toDomainModel(it) }
    }

    fun updatePrescription(prescriptionId: String, updates: UpdatePrescriptionRequest): Prescription? {
        val existing = getPrescriptionById(prescriptionId) ?: return null
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
        
        val updatedItem = PrescriptionDynamoItem(
            pk = "PATIENT#${existing.patientId}",
            sk = "PRESCRIPTION#$prescriptionId",
            gsi1Pk = "PRESCRIPTION#$prescriptionId",
            gsi1Sk = "METADATA",
            gsi2Pk = "STATUS#${updates.status?.name ?: existing.status.name}",
            gsi2Sk = now,
            prescriptionId = prescriptionId,
            patientId = existing.patientId,
            medicationName = updates.medicationName ?: existing.medicationName,
            dosage = updates.dosage ?: existing.dosage,
            unit = updates.unit ?: existing.unit,
            frequencyHours = updates.frequencyHours ?: existing.frequencyHours,
            startTime = updates.startTime?.toString() ?: existing.startTime.toString(),
            startDate = updates.startDate?.toString() ?: existing.startDate.toString(),
            endDate = updates.endDate?.toString() ?: existing.endDate.toString(),
            status = updates.status?.name ?: existing.status.name,
            prescribedBy = existing.prescribedBy,
            createdAt = existing.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z",
            updatedAt = now
        )

        prescriptionTable.putItem(updatedItem)
        return toDomainModel(updatedItem)
    }

    fun deletePrescription(prescriptionId: String): Boolean {
        val existing = getPrescriptionById(prescriptionId) ?: return false
        
        val key = Key.builder()
            .partitionValue("PATIENT#${existing.patientId}")
            .sortValue("PRESCRIPTION#$prescriptionId")
            .build()

        prescriptionTable.deleteItem(key)
        return true
    }

    fun getActivePrescriptions(): List<Prescription> {
        val queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(
                QueryConditional.keyEqualTo(
                    Key.builder()
                        .partitionValue("STATUS#ACTIVE")
                        .build()
                )
            )
            .build()

        return prescriptionTable.index("GSI2")
            .query(queryRequest)
            .flatMap { it.items() }
            .map { toDomainModel(it) }
            .toList()
    }

    private fun toDomainModel(item: PrescriptionDynamoItem): Prescription {
        return Prescription(
            prescriptionId = item.prescriptionId,
            patientId = item.patientId,
            medicationName = item.medicationName,
            dosage = item.dosage,
            unit = item.unit,
            frequencyHours = item.frequencyHours,
            startTime = java.time.LocalTime.parse(item.startTime),
            startDate = java.time.LocalDate.parse(item.startDate),
            endDate = java.time.LocalDate.parse(item.endDate),
            status = PrescriptionStatus.valueOf(item.status),
            prescribedBy = item.prescribedBy,
            createdAt = LocalDateTime.parse(item.createdAt.removeSuffix("Z")),
            updatedAt = LocalDateTime.parse(item.updatedAt.removeSuffix("Z"))
        )
    }
}