package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoiceMatchStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoiceStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalInvoiceDomainTest {

    @Test
    fun testVendorInvoiceSubmissionCalculationsAndModelDefaults() {
        val item = VendorPortalInvoiceSubmissionItem(
            itemId = "ITEM-01",
            submissionId = "SUB-01",
            tenantId = "TENANT-001",
            purchaseOrderItemId = "PO-ITEM-01",
            deliveryReceiptItemId = "DR-ITEM-01",
            itemName = "High Tensile Steel Rods",
            itemCode = "HT-ROD-01",
            invoicedQuantity = BigDecimal("100"),
            unitOfMeasure = "PIECE",
            unitPrice = Money(BigDecimal("50.00")),
            taxAmount = Money(BigDecimal("500.00")),
            lineTotal = Money(BigDecimal("5500.00"))
        )

        val submission = VendorPortalInvoiceSubmission(
            submissionId = "SUB-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            purchaseOrderId = "PO-001",
            orderNumber = "PO-2026-001",
            vendorInvoiceNumber = "VINV-9988",
            subtotalAmount = Money(BigDecimal("5000.00")),
            taxAmount = Money(BigDecimal("500.00")),
            totalAmount = Money(BigDecimal("5500.00")),
            items = listOf(item),
            createdBy = "VENDOR_USER_01"
        )

        assertEquals("SUB-01", submission.submissionId)
        assertEquals(VendorPortalInvoiceSubmissionStatus.DRAFT, submission.status)
        assertEquals(Money(BigDecimal("5500.00")), submission.totalAmount)
        assertEquals(1, submission.items.size)
        assertEquals(BigDecimal("100"), submission.items[0].invoicedQuantity)
    }

    @Test
    fun testVendorInvoiceMatchProjectionModel() {
        val line = VendorPortalInvoiceMatchLineSummary(
            matchLineId = "ML-01",
            invoiceItemId = "INV-ITEM-01",
            purchaseOrderItemId = "PO-ITEM-01",
            deliveryReceiptItemId = "DR-ITEM-01",
            description = "Structural Beams",
            orderedQuantity = BigDecimal("50"),
            receivedQuantity = BigDecimal("50"),
            acceptedQuantity = BigDecimal("50"),
            invoicedQuantity = BigDecimal("50"),
            orderedUnitPrice = Money(BigDecimal("100.00")),
            invoicedUnitPrice = Money(BigDecimal("100.00")),
            quantityVariance = BigDecimal.ZERO,
            priceVariance = Money.ZERO,
            amountVariance = Money.ZERO,
            matchStatus = VendorInvoiceMatchStatus.MATCHED
        )

        val match = VendorPortalInvoiceMatchSummary(
            matchId = "MATCH-01",
            invoiceId = "INV-01",
            purchaseOrderId = "PO-01",
            matchStatus = VendorInvoiceMatchStatus.MATCHED,
            matchedAt = System.currentTimeMillis(),
            subtotalVariance = Money.ZERO,
            quantityVariance = BigDecimal.ZERO,
            priceVariance = Money.ZERO,
            taxVariance = Money.ZERO,
            totalVariance = Money.ZERO,
            currencyMismatch = false,
            vendorMismatch = false,
            exceptionCount = 0,
            lines = listOf(line)
        )

        assertEquals(VendorInvoiceMatchStatus.MATCHED, match.matchStatus)
        assertEquals(0, match.exceptionCount)
        assertFalse(match.vendorMismatch)
    }

    @Test
    fun testVendorFinancialKpiModelDeterministicZeroSafety() {
        val kpi = VendorPortalFinancialKpiSummary(
            vendorId = "VND-001",
            currency = "BDT",
            totalInvoiced = Money(BigDecimal("10000.00")),
            totalApproved = Money(BigDecimal("8000.00")),
            totalPaid = Money(BigDecimal("5000.00")),
            totalOutstanding = Money(BigDecimal("3000.00")),
            totalDisputed = Money(BigDecimal("2000.00")),
            totalOnHold = Money.ZERO,
            invoiceCount = 5,
            outstandingInvoiceCount = 2,
            paidInvoiceCount = 3
        )

        assertEquals(5, kpi.invoiceCount)
        assertEquals(Money(BigDecimal("3000.00")), kpi.totalOutstanding)
        assertEquals(Money(BigDecimal("2000.00")), kpi.totalDisputed)
    }
}
