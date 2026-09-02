package com.sucharu.sucharupro.domain.model.communication.analytics

import java.time.Instant

data class CommunicationPeriodComparison(
    val previousPeriodStart: Instant,
    val previousPeriodEnd: Instant,
    val currentPeriodStart: Instant,
    val currentPeriodEnd: Instant,
    
    val previousTotalCommunications: Int,
    val currentTotalCommunications: Int,
    val totalCommunicationsVariance: Double,
    
    val previousDeliveryRate: Double,
    val currentDeliveryRate: Double,
    val deliveryRateVariance: Double,
    
    val previousFailureRate: Double,
    val currentFailureRate: Double,
    val failureRateVariance: Double,
    
    val previousReadRate: Double,
    val currentReadRate: Double,
    val readRateVariance: Double,
    
    val previousAcknowledgementRate: Double,
    val currentAcknowledgementRate: Double,
    val acknowledgementRateVariance: Double,
    
    val previousAverageEngagementScore: Double,
    val currentAverageEngagementScore: Double,
    val engagementScoreVariance: Double
)

data class CommunicationForecastSummary(
    val forecastedPeriodStart: Instant,
    val forecastedPeriodEnd: Instant,
    
    val forecastedCommunicationVolume: Int,
    val forecastedDeliveryVolume: Int,
    val forecastedUnreadBacklog: Int,
    
    val forecastedCampaignEngagementRate: Double,
    val forecastedAutomationExecutionVolume: Int,
    
    val confidenceIntervalPercentage: Double
)
