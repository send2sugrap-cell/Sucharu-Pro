package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorCollaborationValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalCollaborationValidatorTest {

    @Test
    fun testValidatePoAcknowledgementSuccessForValidAck() {
        val ack = VendorPoAcknowledgement(
            acknowledgementId = "ack-1",
            purchaseOrderId = "po-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            actorId = "user-1",
            acknowledgementType = VendorPoAcknowledgementType.ACKNOWLEDGED,
            promisedDeliveryDate = 1750000000L,
            acknowledgedAt = 1740000000L
        )

        // Should not throw
        VendorCollaborationValidator.validatePoAcknowledgement(ack)
    }

    @Test
    fun testValidatePoAcknowledgementFailsWhenMissingDeclineReason() {
        val ack = VendorPoAcknowledgement(
            acknowledgementId = "ack-1",
            purchaseOrderId = "po-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            actorId = "user-1",
            acknowledgementType = VendorPoAcknowledgementType.DECLINED,
            declineReason = "",
            acknowledgedAt = 1740000000L
        )

        assertThrows(IllegalArgumentException::class.java) {
            VendorCollaborationValidator.validatePoAcknowledgement(ack)
        }
    }

    @Test
    fun testValidateProgressUpdateSuccessAndFailureOnInvalidQuantities() {
        val validUpdate = VendorProgressUpdate(
            progressUpdateId = "prog-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            workOrderId = "wo-1",
            authorizedQuantity = BigDecimal("100"),
            completedQuantity = BigDecimal("40"),
            remainingQuantity = BigDecimal("60"),
            progressPercentage = 40.0,
            statusSummary = "On track",
            submittedBy = "user-1",
            submittedAt = 1740000000L
        )
        // Should not throw
        VendorCollaborationValidator.validateProgressUpdate(validUpdate)

        val invalidUpdate = validUpdate.copy(completedQuantity = BigDecimal("-5"))
        assertThrows(IllegalArgumentException::class.java) {
            VendorCollaborationValidator.validateProgressUpdate(invalidUpdate)
        }
    }

    @Test
    fun testValidateBlockerTitleAndDescriptionRequired() {
        val blocker = VendorBlocker(
            blockerId = "blk-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            workOrderId = "wo-1",
            vendorId = "vendor-1",
            category = VendorBlockerCategory.SPECIFICATION_UNCLEAR,
            severity = VendorBlockerSeverity.MEDIUM,
            title = "",
            description = "Details missing",
            reportedBy = "user-1",
            reportedAt = 1740000000L
        )
        assertThrows(IllegalArgumentException::class.java) {
            VendorCollaborationValidator.validateBlocker(blocker)
        }
    }
}
