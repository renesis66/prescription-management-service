package com.prescription.service

import com.prescription.domain.Prescription
import com.prescription.domain.PrescriptionSchedule
import com.prescription.domain.ScheduleStatus
import com.prescription.repository.ScheduleRepository
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.LocalTime

@Singleton
open class ScheduleService(
    private val scheduleRepository: ScheduleRepository
) {

    fun generateSchedule(prescription: Prescription): List<PrescriptionSchedule> {
        val schedules = mutableListOf<PrescriptionSchedule>()
        var currentDate = prescription.startDate
        
        while (!currentDate.isAfter(prescription.endDate)) {
            var currentHour = 0
            
            while (currentHour < 24) {
                val scheduleTime = prescription.startTime.plusHours(currentHour.toLong())
                
                // Skip if this would be the next day
                if (scheduleTime.isBefore(prescription.startTime) && currentHour > 0) {
                    break
                }
                
                // Skip if this goes beyond the end date
                if (currentDate == prescription.endDate && currentHour + prescription.startTime.hour >= 24) {
                    break
                }
                
                val adjustedTime = if (scheduleTime.hour >= 24) {
                    LocalTime.of(scheduleTime.hour % 24, scheduleTime.minute)
                } else {
                    scheduleTime
                }
                
                val schedule = PrescriptionSchedule(
                    prescriptionId = prescription.prescriptionId,
                    patientId = prescription.patientId,
                    scheduledDate = currentDate,
                    scheduledTime = adjustedTime,
                    dosage = prescription.dosage,
                    unit = prescription.unit,
                    status = ScheduleStatus.PENDING
                )
                
                schedules.add(scheduleRepository.createScheduleEntry(schedule))
                currentHour += prescription.frequencyHours
            }
            
            currentDate = currentDate.plusDays(1)
        }
        
        return schedules
    }

    fun getPrescriptionSchedule(prescriptionId: String): List<PrescriptionSchedule> {
        return scheduleRepository.getScheduleByPrescription(prescriptionId)
    }

    fun getPatientSchedule(patientId: String, date: String? = null): List<PrescriptionSchedule> {
        return scheduleRepository.getPatientSchedule(patientId, date)
    }

    fun updateScheduleStatus(
        prescriptionId: String,
        scheduledDate: String,
        scheduledTime: String,
        status: ScheduleStatus
    ): PrescriptionSchedule? {
        return scheduleRepository.updateScheduleStatus(prescriptionId, scheduledDate, scheduledTime, status)
    }

    fun deleteScheduleForPrescription(prescriptionId: String) {
        scheduleRepository.deleteScheduleByPrescription(prescriptionId)
    }
}