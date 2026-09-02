package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorWoCollaborationDomainTest {

    @Test
    fun testWorkOrderAcknowledgementCreation() {
        val ack = VendorWoAcknowledgement(
            acknowledgementId = "wo-ack-1",
            workOrderId = "wo-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            actorId = "user-1",
            acknowledgementType = VendorWoAcknowledgementType.ACKNOWLEDGED,
            promisedStartDate = 1740500000L,
            promisedCompletionDate = 1742000000L,
            comment = "Ready to start job.",
            acknowledgedAt = 1740000000L
        )

        assertEquals("wo-ack-1", ack.acknowledgementId)
        assertEquals(VendorWoAcknowledgementType.ACKNOWLEDGED, ack.acknowledgementType)
        assertNotNull(ack.promisedStartDate)
    }

    @Test
    fun testProgressUpdateModelProperties() {
        val prog = VendorProgressUpdate(
            progressUpdateId = "prog-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            workOrderId = "wo-1",
            authorizedQuantity = BigDecimal("100.00"),
            completedQuantity = BigDecimal("40.00"),
            remainingQuantity = BigDecimal("60.00"),
            progressPercentage = 40.0,
            statusSummary = "Cutting and stitching complete",
            notes = "Ahead of schedule",
            submittedBy = "vendor-rep-1",
            submittedAt = 1740000000L
        )

        assertEquals(BigDecimal("100.00"), prog.authorizedQuantity)
        assertEquals(BigDecimal("40.00"), prog.completedQuantity)
        assertEquals(BigDecimal("60.00"), prog.remainingQuantity)
        assertEquals(40.0, prog.progressPercentage!!, 0.001)
    }

    @Test
    fun testBlockerAndSeverityEnums() {
        val blocker = VendorBlocker(
            blockerId = "blk-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            workOrderId = "wo-1",
            category = VendorBlockerCategory.MATERIAL_UNAVAILABLE,
            severity = VendorBlockerSeverity.CRITICAL,
            title = "Missing zippers",
            description = "Cannot complete assembly without zipper shipment.",
            reportedBy = "vendor-rep-1",
            reportedAt = 1740000000L
        )

        assertEquals(VendorBlockerStatus.OPEN, blocker.status)
        assertEquals(VendorBlockerCategory.MATERIAL_UNAVAILABLE, blocker.category)
        assertEquals(VendorBlockerSeverity.CRITICAL, blocker.severity)
    }

    @Test
    fun testThreadMessageAndVisibilityEnums() {
        val thread = VendorCollaborationThread(
            threadId = "th-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            resourceType = VendorThreadResourceType.WORK_ORDER,
            resourceId = "wo-1",
            title = "WO-001 Collaboration",
            createdBy = "user-1",
            createdAt = 1740000000L
        )

        val msg = VendorCollaborationMessage(
            messageId = "msg-1",
            threadId = "th-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            authorId = "user-internal",
            authorName = "QA Lead",
            isInternalAuthor = true,
            visibility = VendorMessageVisibility.INTERNAL_ONLY,
            message = "Internal review pending check on fabric GSM.",
            createdAt = 1740000100L
        )

        assertEquals(VendorMessageVisibility.INTERNAL_ONLY, msg.visibility)
        assertTrue(msg.isInternalAuthor)
    }
}
