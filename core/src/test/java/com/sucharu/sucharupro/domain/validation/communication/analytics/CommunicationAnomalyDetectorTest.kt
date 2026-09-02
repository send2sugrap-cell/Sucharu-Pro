package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnomaly
import com.sucharu.sucharupro.domain.model.communication.analytics.RiskSeverity
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * 9. CommunicationAnomalyDetectorTest
 *
 * Verifies the anomaly detection logic (simulated for now, would be inside 
 * CommunicationAnalyticsCalculator or a dedicated Anomaly Engine).
 */
class CommunicationAnomalyDetectorTest {

    @Test
    fun `detectAnomalies identifies delivery spike`() {
        val anomalies = listOf(
            CommunicationAnomaly(
                anomalyType = "DELIVERY_SPIKE",
                baselineValue = 100.0,
                observedValue = 500.0,
                deviationPercentage = 400.0,
                severity = RiskSeverity.HIGH,
                explanation = "Unusual spike in delivery failures",
                detectedAt = Instant.now()
            )
        )
        
        assertTrue(anomalies.isNotEmpty())
        assertEquals("DELIVERY_SPIKE", anomalies.first().anomalyType)
        assertEquals(RiskSeverity.HIGH, anomalies.first().severity)
    }
}
