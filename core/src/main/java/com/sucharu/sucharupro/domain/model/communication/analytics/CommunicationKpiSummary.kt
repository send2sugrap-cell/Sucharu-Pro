package com.sucharu.sucharupro.domain.model.communication.analytics

data class CommunicationKpiSummary(
    val totalCommunications: Int,
    val queuedCount: Int,
    val sentCount: Int,
    val deliveredCount: Int,
    val readCount: Int,
    val acknowledgedCount: Int,
    val failedCount: Int,
    val cancelledCount: Int,
    
    val deliveryRate: Double,
    val readRate: Double,
    val acknowledgementRate: Double,
    val failureRate: Double,
    
    val averageDeliveryTimeMs: Long,
    val averageReadTimeMs: Long,
    val averageAcknowledgementTimeMs: Long
)
