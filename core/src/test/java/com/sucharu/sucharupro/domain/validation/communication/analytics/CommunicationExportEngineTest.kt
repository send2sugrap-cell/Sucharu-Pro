package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * 2. CommunicationExportEngineTest (Validates Payload generation and filtering)
 *
 * Verifies that the CommunicationExportEngine generates structurally correct
 * export payloads in various formats and handles data mapping securely.
 */
class CommunicationExportEngineTest {

    private val sampleKpi = CommunicationKpiSummary(
        totalCommunications = 100,
        queuedCount = 0,
        sentCount = 0,
        deliveredCount = 95,
        readCount = 80,
        acknowledgedCount = 70,
        failedCount = 5,
        cancelledCount = 0,
        deliveryRate = 95.0,
        readRate = 80.0,
        acknowledgementRate = 70.0,
        failureRate = 5.0,
        averageDeliveryTimeMs = 100,
        averageReadTimeMs = 200,
        averageAcknowledgementTimeMs = 300
    )

    private val sampleData = CommunicationAnalyticsSnapshot(
        snapshotId = "SNAP-1",
        projectId = "PROJ-1",
        fromDate = Instant.now(),
        toDate = Instant.now(),
        generatedAt = Instant.now(),
        kpiSummary = sampleKpi,
        channelAnalytics = emptyList(),
        typeAnalytics = emptyList(),
        customerEngagement = emptyList(),
        internalEngagement = emptyList(),
        vendorEngagement = emptyList(),
        campaignAnalytics = emptyList(),
        automationAnalytics = emptyList(),
        riskIndicators = emptyList(),
        anomalies = emptyList(),
        governanceResult = CommunicationGovernanceResult("gov-1", Instant.now(), emptyList(), emptyList(), emptyList(), 0, 0, GovernanceStatus.HEALTHY),
        sha256Hash = "hash"
    )

    private val baseRequest = CommunicationExportRequest(
        exportId = "EXP-1",
        projectId = "PROJ-1",
        requestedBy = "USR-1",
        exportType = CommunicationExportType.FULL_REPORT,
        correlationId = "corr-1"
    )

    @Test
    fun `buildPayload creates payload correctly`() {
        val payload = CommunicationExportEngine.buildPayload(baseRequest, sampleData)

        assertNotNull(payload)
        assertEquals(CommunicationExportType.FULL_REPORT, payload.exportType)
        assertNotNull(payload.kpiSummary)
        assertEquals(100, payload.kpiSummary?.totalCommunications)
        assertNotNull(payload.governanceSummary)
        assertTrue(payload.payloadHash.isNotEmpty())
    }

    @Test
    fun `buildPayload handles specific export types correctly`() {
        val request = baseRequest.copy(exportType = CommunicationExportType.KPI_SUMMARY)
        val payload = CommunicationExportEngine.buildPayload(request, sampleData)

        assertNotNull(payload)
        assertEquals(CommunicationExportType.KPI_SUMMARY, payload.exportType)
        assertNotNull(payload.kpiSummary)
        assertNull(payload.channelAnalytics)
    }

    @Test
    fun `deriveOperationalHealth derives health correctly`() {
        val health = CommunicationExportEngine.deriveOperationalHealth(
            projectId = "PROJ-1",
            governance = sampleData.governanceResult,
            risks = emptyList(),
            anomalies = emptyList(),
            forecast = null,
            latestVerification = null
        )
        
        assertNotNull(health)
        assertEquals(CommunicationHealth.GOOD, health.communicationHealth)
        assertEquals(CommunicationGovernanceStatus.COMPLIANT, health.governanceStatus)
    }
}
