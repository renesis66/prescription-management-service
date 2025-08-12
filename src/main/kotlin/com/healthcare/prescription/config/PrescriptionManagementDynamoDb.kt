package com.healthcare.prescription.config

import jakarta.inject.Qualifier
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
annotation class PrescriptionManagementDynamoDb