package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorQuotationValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorQuotationValidatorTest {

    @Test(expected = IllegalArgumentException::class)
    fun testValidatesQuotationSubmissionAndPreventsMissingItems() {
        val rfqItems = listOf(
            VendorRfqItem(
                rfqItemId = "rfq-item-1",
                rfqId = "rfq-1",
                sequenceNumber = 1,
                description = "Paper",
                quantity = BigDecimal("10.00")
            ),
            VendorRfqItem(
                rfqItemId = "rfq-item-2",
                rfqId = "rfq-1",
                sequenceNumber = 2,
                description = "Ink",
                quantity = BigDecimal("5.00")
            )
        )

        val quotation = VendorQuotation(
            quotationId = "q-1",
            rfqId = "rfq-1",
            invitationId = "inv-1",
            vendorId = "vnd-1",
            projectId = "proj-1",
            tenantId = "proj-1",
            quotationNumber = "QTN-001",
            items = listOf(
                VendorQuotationItem(
                    quotationItemId = "qi-1",
                    quotationId = "q-1",
                    rfqItemId = "rfq-item-1",
                    quantity = BigDecimal("10.00"),
                    unitPrice = Money("100.00"),
                    lineTotal = Money("1000.00")
                )
            ),
            subtotal = Money("1000.00"),
            grandTotal = Money("1000.00"),
            createdBy = "user-1"
        )

        // Missing rfq-item-2
        VendorQuotationValidator.validateQuotationSubmission(quotation, rfqItems)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEnforcesSeparationOfDutiesDuringRfqAward() {
        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "proj-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Paper Supply",
            requestedBy = "staff-1",
            status = VendorRfqStatus.EVALUATION,
            responseDeadline = System.currentTimeMillis() + 86400000L,
            createdBy = "staff-1"
        )

        val quotation = VendorQuotation(
            quotationId = "q-1",
            rfqId = "rfq-1",
            invitationId = "inv-1",
            vendorId = "vnd-1",
            projectId = "proj-1",
            tenantId = "proj-1",
            quotationNumber = "QTN-001",
            status = VendorQuotationStatus.SUBMITTED,
            submittedBy = "vendor-user-1",
            createdBy = "vendor-user-1"
        )

        VendorQuotationValidator.validateAwardDecision(rfq, quotation, "vendor-user-1", "Lowest Price")
    }
}
