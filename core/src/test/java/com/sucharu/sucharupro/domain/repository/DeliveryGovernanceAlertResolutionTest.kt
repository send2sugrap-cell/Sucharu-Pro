package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertCategory
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertSeverity
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryGovernanceAlertResolutionTest {

    private lateinit var governanceDataSource: FakeDeliveryGovernanceDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var repository: DeliveryAnalyticsRepository

    @Before
    fun setUp() {
        governanceDataSource = FakeDeliveryGovernanceDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        repository = DeliveryAnalyticsRepositoryImpl(
            governanceDataSource = governanceDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource
        )
    }

    @Test
    fun `alert resolution lifecycle transitions and updates audit`() = runBlocking {
        governanceDataSource.insertAlert(
            DeliveryGovernanceAlert(
                alertId = "ALT-1",
                projectId = "PRJ-01",
                category = DeliveryGovernanceAlertCategory.OVERDUE_DELIVERY,
                severity = DeliveryGovernanceAlertSeverity.CRITICAL,
                referenceType = "SHIPMENT",
                referenceId = "SH-1",
                title = "Overdue Shipment",
                description = "Shipment is overdue",
                detectedAt = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val ackRes = repository.acknowledgeAlert("ALT-1", "user-admin", UserRole.ADMIN)
        assertTrue(ackRes is DomainResult.Success)
        assertEquals(DeliveryGovernanceAlertStatus.ACKNOWLEDGED, (ackRes as DomainResult.Success).data.status)

        val resolveRes = repository.resolveAlert("ALT-1", "user-admin", "Delivered manually and confirmed with client.", UserRole.ADMIN)
        assertTrue(resolveRes is DomainResult.Success)
        assertEquals(DeliveryGovernanceAlertStatus.RESOLVED, (resolveRes as DomainResult.Success).data.status)

        val eventsRes = repository.getActivityEvents("ALT-1", UserRole.ADMIN)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data
        assertEquals(2, events.size)
    }
}
