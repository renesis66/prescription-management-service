package com.prescription.domain

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey

@DynamoDbBean
data class PrescriptionDynamoItem(
    @get:DynamoDbPartitionKey
    var pk: String = "",
    
    @get:DynamoDbSortKey
    var sk: String = "",
    
    @get:DynamoDbSecondaryPartitionKey(indexNames = ["GSI1"])
    var gsi1Pk: String = "",
    
    @get:DynamoDbSecondarySortKey(indexNames = ["GSI1"])
    var gsi1Sk: String = "",
    
    @get:DynamoDbSecondaryPartitionKey(indexNames = ["GSI2"])
    var gsi2Pk: String? = null,
    
    @get:DynamoDbSecondarySortKey(indexNames = ["GSI2"])
    var gsi2Sk: String? = null,
    
    var prescriptionId: String = "",
    var patientId: String = "",
    var medicationName: String = "",
    var dosage: Double = 0.0,
    var unit: String = "",
    var frequencyHours: Int = 0,
    var startTime: String = "",
    var startDate: String = "",
    var endDate: String = "",
    var status: String = "",
    var prescribedBy: String = "",
    var createdAt: String = "",
    var updatedAt: String = ""
)

@DynamoDbBean
data class ScheduleDynamoItem(
    @get:DynamoDbPartitionKey
    var pk: String = "",
    
    @get:DynamoDbSortKey
    var sk: String = "",
    
    @get:DynamoDbSecondaryPartitionKey(indexNames = ["GSI1"])
    var gsi1Pk: String = "",
    
    @get:DynamoDbSecondarySortKey(indexNames = ["GSI1"])
    var gsi1Sk: String = "",
    
    var prescriptionId: String = "",
    var patientId: String = "",
    var scheduledDate: String = "",
    var scheduledTime: String = "",
    var dosage: Double = 0.0,
    var unit: String = "",
    var status: String = ""
)