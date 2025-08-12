package com.healthcare.prescription.config

import com.healthcare.prescription.domain.PrescriptionDynamoItem
import com.healthcare.prescription.domain.ScheduleDynamoItem
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

@Factory
@Requires(notEnv = ["test"])
class DynamoDBConfig {

    @Bean
    @Singleton
    @PrescriptionManagementDynamoDb
    fun dynamoDbClient(): DynamoDbClient {
        return DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://localhost:8000")) // For local DynamoDB
            .build()
    }

    @Bean
    @Singleton
    @PrescriptionManagementDynamoDb
    fun dynamoDbEnhancedClient(@PrescriptionManagementDynamoDb dynamoDbClient: DynamoDbClient): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build()
    }

    @Bean
    @Singleton
    @PrescriptionManagementDynamoDb
    fun prescriptionTable(
        @PrescriptionManagementDynamoDb enhancedClient: DynamoDbEnhancedClient,
        @Value("\${aws.dynamodb.prescriptions-table}") tableName: String
    ): DynamoDbTable<PrescriptionDynamoItem> {
        return enhancedClient.table(tableName, TableSchema.fromBean(PrescriptionDynamoItem::class.java))
    }

    @Bean
    @Singleton
    @PrescriptionManagementDynamoDb
    fun scheduleTable(
        @PrescriptionManagementDynamoDb enhancedClient: DynamoDbEnhancedClient,
        @Value("\${aws.dynamodb.prescription-schedules-table}") tableName: String
    ): DynamoDbTable<ScheduleDynamoItem> {
        return enhancedClient.table(tableName, TableSchema.fromBean(ScheduleDynamoItem::class.java))
    }
}