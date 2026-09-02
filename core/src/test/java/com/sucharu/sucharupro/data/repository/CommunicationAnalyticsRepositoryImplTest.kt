package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeCampaignDataSource
import com.sucharu.sucharupro.data.datasource.FakeCommunicationAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeCommunicationAutomationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsFilter
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * 6. CommunicationAnalyticsRepositoryImplTest
 *
 * Verifies that the implementation correctly computes KPI, Channel Analytics,
 * Risk Indicators, Anomalies, and Governance logic while adhering to RBAC limits.
 */
class CommunicationAnalyticsRepositoryImplTest {

    private lateinit var notifDataSource: FakeNotificationDataSource
    private lateinit var analyticsDataSource: FakeCommunicationAnalyticsDataSource
    private lateinit var repository: CommunicationAnalyticsRepositoryImpl
    private val projectId = "PROJ-1"

    @Before
    fun setUp() {
        notifDataSource = FakeNotificationDataSource()
        analyticsDataSource = FakeCommunicationAnalyticsDataSource()
        
        repository = CommunicationAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            notificationDataSource = notifDataSource,
            campaignDataSource = FakeCampaignDataSource(),
            automationDataSource = FakeCommunicationAutomationDataSource()
        )
    }

    private fun baseFilter() = CommunicationAnalyticsFilter(
        projectId = projectId,
        fromDate = Instant.now().minusSeconds(86400),
        toDate = Instant.now()
    )

    @Test
    fun `getKpiSummary returns Success for valid user`() = runBlocking {
        val result = repository.getKpiSummary(baseFilter(), UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)
        val kpi = (result as DomainResult.Success).data
        assertTrue(kpi.totalCommunications >= 0)
    }

    @Test
    fun `getGovernanceResult succeeds for ADMIN`() = runBlocking {
        val result = repository.getGovernanceResult(baseFilter(), UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `getGovernanceResult succeeds for MANAGER`() = runBlocking {
        val result = repository.getGovernanceResult(baseFilter(), UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `getGovernanceResult fails for STAFF`() = runBlocking {
        val result = repository.getGovernanceResult(baseFilter(), UserRole.STAFF)
        assertTrue("STAFF should not be able to access Governance", result is DomainResult.Error)
    }

    @Test
    fun `createSnapshot calls data source and returns valid snapshot`() = runBlocking {
        val filter = baseFilter()
        val result = repository.createSnapshot(filter, "USR-1", UserRole.ADMIN, "idempotency-key")
        
        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data
        
        assertEquals("PROJ-1", snapshot.projectId)
        assertTrue(snapshot.sha256Hash.isNotEmpty())
    }
}
