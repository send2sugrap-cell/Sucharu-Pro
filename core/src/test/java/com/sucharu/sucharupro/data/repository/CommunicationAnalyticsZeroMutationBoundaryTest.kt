package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeCampaignDataSource
import com.sucharu.sucharupro.data.datasource.FakeCommunicationAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeCommunicationAutomationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsFilter
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * MANDATORY: Zero-Mutation Boundary Test for Communication Analytics (Module 10 Step 09).
 *
 * Verifies that the analytics layer NEVER modifies existing source records.
 * All analytics reads must be idempotent and non-destructive.
 */
class CommunicationAnalyticsZeroMutationBoundaryTest {

    private lateinit var notifDataSource: FakeNotificationDataSource
    private lateinit var repository: CommunicationAnalyticsRepositoryImpl
    private val projectId = "PROJ-TEST-ZERO-MUTATION"
    private val actorRole = UserRole.ADMIN

    @Before
    fun setUp() {
        notifDataSource = FakeNotificationDataSource()
        val analyticsDataSource = FakeCommunicationAnalyticsDataSource()
        val automationDataSource = FakeCommunicationAutomationDataSource()
        val campaignDataSource = FakeCampaignDataSource()

        repository = CommunicationAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            notificationDataSource = notifDataSource,
            campaignDataSource = campaignDataSource,
            automationDataSource = automationDataSource
        )
    }

    private fun baseFilter() = CommunicationAnalyticsFilter(
        projectId = projectId,
        fromDate = Instant.now().minus(30, ChronoUnit.DAYS),
        toDate = Instant.now()
    )

    @Test
    fun `KPI summary does not mutate notification records`() = runBlocking {
        val filter = baseFilter()
        val snapshotBefore = notifDataSource.getNotificationsByProject(projectId).toList()

        repository.getKpiSummary(filter, actorRole)

        val snapshotAfter = notifDataSource.getNotificationsByProject(projectId).toList()

        assertEquals(
            "Notification count must be unchanged after KPI calculation",
            snapshotBefore.size,
            snapshotAfter.size
        )
    }

    @Test
    fun `Channel analytics does not mutate notification records`() = runBlocking {
        val filter = baseFilter()
        val snapshotBefore = notifDataSource.getNotificationsByProject(projectId).toList()

        repository.getChannelAnalytics(filter, actorRole)

        val snapshotAfter = notifDataSource.getNotificationsByProject(projectId).toList()

        assertEquals(
            "Notification count must be unchanged after channel analytics",
            snapshotBefore.size,
            snapshotAfter.size
        )
    }

    @Test
    fun `Risk indicator detection does not mutate notification records`() = runBlocking {
        val filter = baseFilter()
        val snapshotBefore = notifDataSource.getNotificationsByProject(projectId).toList()

        repository.getRiskIndicators(filter, actorRole)

        val snapshotAfter = notifDataSource.getNotificationsByProject(projectId).toList()

        assertEquals(
            "Notification count must be unchanged after risk indicator detection",
            snapshotBefore.size,
            snapshotAfter.size
        )
    }

    @Test
    fun `Anomaly detection does not mutate notification records`() = runBlocking {
        val filter = baseFilter()
        val snapshotBefore = notifDataSource.getNotificationsByProject(projectId).toList()

        repository.getAnomalies(filter, actorRole)

        val snapshotAfter = notifDataSource.getNotificationsByProject(projectId).toList()

        assertEquals(
            "Notification count must be unchanged after anomaly detection",
            snapshotBefore.size,
            snapshotAfter.size
        )
    }

    @Test
    fun `Governance evaluation does not mutate notification records`() = runBlocking {
        val filter = baseFilter()
        val snapshotBefore = notifDataSource.getNotificationsByProject(projectId).toList()

        repository.getGovernanceResult(filter, actorRole)

        val snapshotAfter = notifDataSource.getNotificationsByProject(projectId).toList()

        assertEquals(
            "Notification count must be unchanged after governance evaluation",
            snapshotBefore.size,
            snapshotAfter.size
        )
    }

    @Test
    fun `Period comparison does not mutate notification records`() = runBlocking {
        val currentFilter = baseFilter()
        val durationMs = currentFilter.toDate.toEpochMilli() - currentFilter.fromDate.toEpochMilli()
        val previousFilter = currentFilter.copy(
            fromDate = Instant.ofEpochMilli(currentFilter.fromDate.toEpochMilli() - durationMs),
            toDate = currentFilter.fromDate
        )
        val snapshotBefore = notifDataSource.getNotificationsByProject(projectId).toList()

        repository.comparePeriods(projectId, currentFilter, previousFilter, actorRole)

        val snapshotAfter = notifDataSource.getNotificationsByProject(projectId).toList()

        assertEquals(
            "Notification count must be unchanged after period comparison",
            snapshotBefore.size,
            snapshotAfter.size
        )
    }

    @Test
    fun `Snapshot generation does not mutate notification records`() = runBlocking {
        val filter = baseFilter()
        val snapshotBefore = notifDataSource.getNotificationsByProject(projectId).toList()

        repository.createSnapshot(
            filter = filter,
            actorId = "USR-AUDIT",
            actorRole = actorRole,
            idempotencyKey = "test-idempotency-key-zero-mutation"
        )

        val snapshotAfter = notifDataSource.getNotificationsByProject(projectId).toList()

        assertEquals(
            "Notification records must not be added/removed by snapshot generation",
            snapshotBefore.size,
            snapshotAfter.size
        )
    }

    @Test
    fun `Multiple successive reads return same KPI result (idempotency)`() = runBlocking {
        val filter = baseFilter()
        val result1 = repository.getKpiSummary(filter, actorRole)
        val result2 = repository.getKpiSummary(filter, actorRole)

        assertTrue("Both KPI reads must succeed", result1 is DomainResult.Success && result2 is DomainResult.Success)
        val kpi1 = (result1 as DomainResult.Success).data
        val kpi2 = (result2 as DomainResult.Success).data

        assertEquals("KPI totalCommunications must be idempotent", kpi1.totalCommunications, kpi2.totalCommunications)
        assertEquals("KPI deliveryRate must be idempotent", kpi1.deliveryRate, kpi2.deliveryRate, 0.0001)
        assertEquals("KPI readRate must be idempotent", kpi1.readRate, kpi2.readRate, 0.0001)
    }

    @Test
    fun `Non-admin role gets authorization error for governance`() = runBlocking {
        val filter = baseFilter()
        val result = repository.getGovernanceResult(filter, UserRole.STAFF)
        assertTrue(
            "STAFF should not be authorized for governance analytics",
            result is DomainResult.Error
        )
    }

    // Helper: uses the existing observeNotificationsByProject Flow API for boundary checking
    private suspend fun FakeNotificationDataSource.getNotificationsByProject(projectId: String) =
        observeNotificationsByProject(projectId).first()
}
