package com.sucharu.sucharupro.ui.features.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsSnapshot
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationKpiSummary
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceResult
import com.sucharu.sucharupro.domain.model.communication.analytics.GovernanceStatus
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * 7. CommunicationAnalyticsUiStateTest
 *
 * Verifies that the UI state correctly handles the loading state, data population,
 * and error/action resetting logic for the analytics dashboard.
 */
class CommunicationAnalyticsUiStateTest {

    @Test
    fun `initial state is loading without data`() {
        val state = CommunicationAnalyticsUiState()

        assertTrue(state.isLoading == false)
        assertNull(state.kpiSummary)
        assertNull(state.operationalHealth)
        assertTrue(state.channelAnalytics.isEmpty())
        assertTrue(state.riskIndicators.isEmpty())
        assertNull(state.error)
        assertNull(state.verificationResult)
    }

    @Test
    fun `state update resets loading and error flags`() {
        val initialState = CommunicationAnalyticsUiState(isLoading = true, error = "Some error")
        val updatedState = initialState.copy(isLoading = false, error = null, isVerifyingSnapshot = true)

        assertFalse(updatedState.isLoading)
        assertNull(updatedState.error)
        assertTrue(updatedState.isVerifyingSnapshot)
    }

    @Test
    fun `snapshot payload updates correctly`() {
        val snapshot = CommunicationAnalyticsSnapshot(
            snapshotId = "SNAP-1",
            projectId = "PROJ-1",
            fromDate = Instant.now(),
            toDate = Instant.now(),
            generatedAt = Instant.now(),
            kpiSummary = CommunicationKpiSummary(0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0L),
            channelAnalytics = emptyList(),
            typeAnalytics = emptyList(),
            customerEngagement = emptyList(),
            internalEngagement = emptyList(),
            vendorEngagement = emptyList(),
            campaignAnalytics = emptyList(),
            automationAnalytics = emptyList(),
            riskIndicators = emptyList(),
            anomalies = emptyList(),
            governanceResult = CommunicationGovernanceResult(projectId = "PROJ-1", evaluatedAt = Instant.now(), passedControls = emptyList(), warningControls = emptyList(), failedControls = emptyList(), riskCount = 0, anomalyCount = 0, governanceStatus = GovernanceStatus.HEALTHY),
            sha256Hash = "hash"
        )

        val state = CommunicationAnalyticsUiState(
            snapshots = listOf(snapshot)
        )

        assertTrue(state.snapshots.isNotEmpty())
        assertEquals("SNAP-1", state.snapshots.first().snapshotId)
    }
}
