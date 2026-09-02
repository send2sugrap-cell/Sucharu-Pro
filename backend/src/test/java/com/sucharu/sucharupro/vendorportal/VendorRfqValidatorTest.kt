package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorRfqValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorRfqValidatorTest {

    @Test
    fun testValidatesValidRfqSuccessfully() {
        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Paper Supply",
            requestedBy = "user-1",
            issueDate = 1000L,
            responseDeadline = 5000L,
            createdBy = "user-1"
        )
        VendorRfqValidator.validateRfq(rfq)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRejectsRfqWithDeadlineBeforeIssueDate() {
        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Paper Supply",
            requestedBy = "user-1",
            issueDate = 5000L,
            responseDeadline = 1000L,
            createdBy = "user-1"
        )
        VendorRfqValidator.validateRfq(rfq)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRejectsInvalidRfqItemQuantity() {
        val item = VendorRfqItem(
            rfqItemId = "item-1",
            rfqId = "rfq-1",
            sequenceNumber = 1,
            description = "Paper",
            quantity = BigDecimal.ZERO
        )
        VendorRfqValidator.validateRfqItem(item)
    }
}
