package com.sucharu.sucharupro.domain.service.sla

import com.sucharu.sucharupro.domain.model.sla.DelayReasonCategory
import com.sucharu.sucharupro.domain.model.sla.SlaStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SlaDelayManagementServiceTest {

    private lateinit var service: SlaDelayManagementService

    @Before
    fun setUp() {
        service = SlaDelayManagementService()
    }

    @Test
    fun `buildSlaManagementSummary_computesSlaPerformanceAndExceptionQueue`() = runBlocking {
        val summary = service.buildSlaManagementSummary()

        assertNotNull(summary)
        assertEquals(2, summary.totalMonitoredOrdersCount)
        assertEquals(1, summary.onTrackOrdersCount)
        assertEquals(1, summary.overdueOrdersCount)
        assertEquals(BigDecimal("50.00"), summary.slaOnTimePerformancePercentage)

        // Verify Exception Queue
        assertEquals(1, summary.exceptionQueue.size)
        val overdueCommitment = summary.exceptionQueue.first()
        assertEquals("SLA-2026-001", overdueCommitment.commitmentId)
        assertEquals(SlaStatus.OVERDUE, overdueCommitment.slaStatus)
        assertEquals(1, overdueCommitment.delayDays)
    }

    @Test
    fun `recordDelayReason_updatesCommitmentStatusAndRecordsDelayEvent`() = runBlocking {
        val delayRecord = service.recordDelayReason(
            commitmentId = "SLA-2026-002",
            orderId = "ORD-1002",
            category = DelayReasonCategory.CUSTOMER_APPROVAL_DELAY,
            responsibleStage = "APPROVAL",
            delayDurationDays = 2,
            rootCauseDescription = "Customer proof approval pending",
            actorId = "STAFF-001"
        )

        assertNotNull(delayRecord)
        assertEquals("ORD-1002", delayRecord.orderId)
        assertEquals(2, delayRecord.delayDurationDays)
        assertEquals(DelayReasonCategory.CUSTOMER_APPROVAL_DELAY, delayRecord.delayCategory)

        // Verify updated commitment in summary
        val updatedCommitments = service.filterCommitmentsByStatus(SlaStatus.OVERDUE)
        assertEquals(2, updatedCommitments.size)
    }
}
