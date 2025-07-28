package com.healthcare.prescription.config

import com.healthcare.prescription.domain.PrescriptionDynamoItem
import com.healthcare.prescription.domain.ScheduleDynamoItem
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

@Factory
class DynamoDBConfig {

    @Bean
    @Singleton
    fun dynamoDbClient(@Value("\${aws.region}") region: String): DynamoDbClient {
        return DynamoDbClient.builder()
            .region(Region.of(region))
            .build()
    }

    @Bean
    @Singleton
    fun dynamoDbEnhancedClient(dynamoDbClient: DynamoDbClient): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build()
    }

    @Bean
    @Singleton
    fun prescriptionTable(
        enhancedClient: DynamoDbEnhancedClient,
        @Value("\${aws.dynamodb.prescriptions-table}") tableName: String
    ): DynamoDbTable<PrescriptionDynamoItem> {
        return enhancedClient.table(tableName, TableSchema.fromBean(PrescriptionDynamoItem::class.java))
    }

    @Bean
    @Singleton
    fun scheduleTable(
        enhancedClient: DynamoDbEnhancedClient,
        @Value("\${aws.dynamodb.prescription-schedules-table}") tableName: String
    ): DynamoDbTable<ScheduleDynamoItem> {
        return enhancedClient.table(tableName, TableSchema.fromBean(ScheduleDynamoItem::class.java))
    }
}