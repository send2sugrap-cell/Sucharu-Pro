package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationForecastSummary
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationKpiSummary
import java.time.Instant

object CommunicationForecastEngine {

    /**
     * Calculates a simple deterministic forecast based on historical periods.
     * Uses a Weighted Moving Average (WMA) approach where more recent periods carry higher weight.
     * Weights: most recent (0.5), middle (0.3), oldest (0.2).
     */
    fun calculateForecast(
        historicalKpis: List<CommunicationKpiSummary>,
        forecastStart: Instant,
        forecastEnd: Instant
    ): CommunicationForecastSummary {
        
        if (historicalKpis.isEmpty()) {
            return CommunicationForecastSummary(
                forecastedPeriodStart = forecastStart,
                forecastedPeriodEnd = forecastEnd,
                forecastedCommunicationVolume = 0,
                forecastedDeliveryVolume = 0,
                forecastedUnreadBacklog = 0,
                forecastedCampaignEngagementRate = 0.0,
                forecastedAutomationExecutionVolume = 0,
                confidenceIntervalPercentage = 0.0
            )
        }

        // We expect up to 3 historical periods for WMA
        val periods = historicalKpis.takeLast(3)
        
        var volumeSum = 0.0
        var deliverySum = 0.0
        var unreadSum = 0.0
        var readRateSum = 0.0
        var totalWeight = 0.0

        val weights = listOf(0.5, 0.3, 0.2)
        
        // Match weights to the available periods (most recent first)
        for (i in periods.indices.reversed()) {
            val kpi = periods[i]
            val weightIndex = (periods.size - 1) - i
            val weight = weights.getOrElse(weightIndex) { 0.1 }
            
            volumeSum += (kpi.totalCommunications * weight)
            deliverySum += (kpi.deliveredCount * weight)
            unreadSum += ((kpi.deliveredCount - kpi.readCount) * weight)
            readRateSum += (kpi.readRate * weight)
            
            totalWeight += weight
        }

        val forecastedVolume = if (totalWeight > 0) (volumeSum / totalWeight).toInt() else 0
        val forecastedDelivery = if (totalWeight > 0) (deliverySum / totalWeight).toInt() else 0
        val forecastedUnread = if (totalWeight > 0) (unreadSum / totalWeight).toInt() else 0
        val forecastedReadRate = if (totalWeight > 0) (readRateSum / totalWeight) else 0.0

        return CommunicationForecastSummary(
            forecastedPeriodStart = forecastStart,
            forecastedPeriodEnd = forecastEnd,
            forecastedCommunicationVolume = forecastedVolume,
            forecastedDeliveryVolume = forecastedDelivery,
            forecastedUnreadBacklog = forecastedUnread,
            forecastedCampaignEngagementRate = forecastedReadRate, // Simplifying mapping
            forecastedAutomationExecutionVolume = forecastedVolume, // Estimate equal to total comms
            confidenceIntervalPercentage = if (periods.size == 3) 85.0 else 50.0 // Higher confidence with more data points
        )
    }
}
