package com.healthcare.prescription.repository

import com.healthcare.prescription.config.PrescriptionManagementDynamoDb
import com.healthcare.prescription.domain.*
import jakarta.inject.Singleton
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import java.time.LocalDate
import java.time.LocalTime

@Singleton
open class ScheduleRepository(
    @PrescriptionManagementDynamoDb private val scheduleTable: DynamoDbTable<ScheduleDynamoItem>
) {

    fun createScheduleEntry(schedule: PrescriptionSchedule): PrescriptionSchedule {
        val item = ScheduleDynamoItem(
            pk = "PRESCRIPTION#${schedule.prescriptionId}",
            sk = "SCHEDULE#${schedule.scheduledDate}#${schedule.scheduledTime}",
            gsi1Pk = "PATIENT#${schedule.patientId}",
            gsi1Sk = "SCHEDULE#${schedule.scheduledDate}#${schedule.scheduledTime}",
            prescriptionId = schedule.prescriptionId,
            patientId = schedule.patientId,
            scheduledDate = schedule.scheduledDate.toString(),
            scheduledTime = schedule.scheduledTime.toString(),
            dosage = schedule.dosage,
            unit = schedule.unit,
            status = schedule.status.name
        )
        
        scheduleTable.putItem(item)
        return toDomainModel(item)
    }

    fun getScheduleByPrescription(prescriptionId: String): List<PrescriptionSchedule> {
        val queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(
                QueryConditional.keyEqualTo(
                    Key.builder()
                        .partitionValue("PRESCRIPTION#$prescriptionId")
                        .build()
                )
            )
            .build()

        return scheduleTable.query(queryRequest)
            .flatMap { it.items() }
            .map { toDomainModel(it) }
            .toList()
    }

    fun getPatientSchedule(patientId: String, date: String? = null): List<PrescriptionSchedule> {
        val queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(
                QueryConditional.keyEqualTo(
                    Key.builder()
                        .partitionValue("PATIENT#$patientId")
                        .build()
                )
            )
            .build()

        return scheduleTable.index("GSI1")
            .query(queryRequest)
            .flatMap { it.items() }
            .filter { 
                if (date != null) {
                    it.scheduledDate == date
                } else {
                    true
                }
            }
            .map { toDomainModel(it) }
            .toList()
    }

    fun updateScheduleStatus(
        prescriptionId: String,
        scheduledDate: String,
        scheduledTime: String,
        status: ScheduleStatus
    ): PrescriptionSchedule? {
        val key = Key.builder()
            .partitionValue("PRESCRIPTION#$prescriptionId")
            .sortValue("SCHEDULE#$scheduledDate#$scheduledTime")
            .build()

        val existingItem = scheduleTable.getItem(key) ?: return null
        
        val updatedItem = existingItem.copy(status = status.name)
        scheduleTable.putItem(updatedItem)
        
        return toDomainModel(updatedItem)
    }

    fun deleteScheduleByPrescription(prescriptionId: String) {
        val schedules = getScheduleByPrescription(prescriptionId)
        
        schedules.forEach { schedule ->
            val key = Key.builder()
                .partitionValue("PRESCRIPTION#${schedule.prescriptionId}")
                .sortValue("SCHEDULE#${schedule.scheduledDate}#${schedule.scheduledTime}")
                .build()
            scheduleTable.deleteItem(key)
        }
    }

    private fun toDomainModel(item: ScheduleDynamoItem): PrescriptionSchedule {
        return PrescriptionSchedule(
            prescriptionId = item.prescriptionId,
            patientId = item.patientId,
            scheduledDate = LocalDate.parse(item.scheduledDate),
            scheduledTime = LocalTime.parse(item.scheduledTime),
            dosage = item.dosage,
            unit = item.unit,
            status = ScheduleStatus.valueOf(item.status)
        )
    }
}