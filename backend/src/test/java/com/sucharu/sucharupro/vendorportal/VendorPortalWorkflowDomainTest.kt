package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalWorkflowDomainTest {

    @Test
    fun testWorkflowSlaProjectionCalculation() {
        val now = 1756291200000L

        // Milestone due in 5 days -> ON_TRACK
        val onTrackDeadline = now + (86400000L * 5)
        val onTrackProj = VendorWorkflowSlaProjection.calculate("WF-01", "Dispatch Delivery", onTrackDeadline, now)
        assertEquals(VendorWorkflowSlaStatus.ON_TRACK, onTrackProj.slaStatus)
        assertFalse(onTrackProj.isBreached)
        assertEquals(86400000L * 5, onTrackProj.timeRemainingMs)

        // Milestone due in 24 hours -> DUE_SOON
        val dueSoonDeadline = now + 86400000L
        val dueSoonProj = VendorWorkflowSlaProjection.calculate("WF-02", "Acknowledge PO", dueSoonDeadline, now)
        assertEquals(VendorWorkflowSlaStatus.DUE_SOON, dueSoonProj.slaStatus)
        assertFalse(dueSoonProj.isBreached)

        // Milestone due in past -> OVERDUE / BREACHED
        val overdueDeadline = now - 10000L
        val overdueProj = VendorWorkflowSlaProjection.calculate("WF-03", "Submit Invoice", overdueDeadline, now)
        assertEquals(VendorWorkflowSlaStatus.OVERDUE, overdueProj.slaStatus)
        assertTrue(overdueProj.isBreached)
    }

    @Test
    fun testWorkflowStageAndStatusEnums() {
        assertEquals(27, VendorWorkflowStage.values().size)
        assertTrue(VendorWorkflowStage.values().contains(VendorWorkflowStage.RFQ_RECEIVED))
        assertTrue(VendorWorkflowStage.values().contains(VendorWorkflowStage.COMPLETED))

        val item = VendorWorkflowItem(
            workflowId = "WF-TEST-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            correlationId = "PO-101",
            workflowTitle = "Commercial Order #PO-101",
            currentStage = VendorWorkflowStage.PRODUCTION_IN_PROGRESS,
            status = VendorWorkflowStatus.ACTIVE,
            slaStatus = VendorWorkflowSlaStatus.ON_TRACK
        )

        assertEquals("WF-TEST-01", item.workflowId)
        assertEquals(VendorWorkflowStage.PRODUCTION_IN_PROGRESS, item.currentStage)
        assertEquals(1L, item.version)
    }
}
