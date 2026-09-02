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
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryGovernanceProjectIsolationTest {

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
    fun `alerts are strictly isolated by projectId`() = runBlocking {
        governanceDataSource.insertAlert(
            DeliveryGovernanceAlert(
                alertId = "ALT-A",
                projectId = "PRJ-A",
                category = DeliveryGovernanceAlertCategory.MISSING_POD,
                severity = DeliveryGovernanceAlertSeverity.WARNING,
                referenceType = "SHIPMENT",
                referenceId = "SH-A",
                title = "Alert A",
                description = "Desc A",
                detectedAt = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        governanceDataSource.insertAlert(
            DeliveryGovernanceAlert(
                alertId = "ALT-B",
                projectId = "PRJ-B",
                category = DeliveryGovernanceAlertCategory.MISSING_POD,
                severity = DeliveryGovernanceAlertSeverity.WARNING,
                referenceType = "SHIPMENT",
                referenceId = "SH-B",
                title = "Alert B",
                description = "Desc B",
                detectedAt = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val alertsA = repository.getAlerts("PRJ-A", UserRole.ADMIN)
        val alertsB = repository.getAlerts("PRJ-B", UserRole.ADMIN)

        assertTrue(alertsA is DomainResult.Success)
        assertTrue(alertsB is DomainResult.Success)
        assertEquals(1, (alertsA as DomainResult.Success).data.size)
        assertEquals(1, (alertsB as DomainResult.Success).data.size)
        assertEquals("ALT-A", alertsA.data.first().alertId)
        assertEquals("ALT-B", alertsB.data.first().alertId)
    }
}
