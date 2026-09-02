package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoiceMatchStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoiceStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalInvoiceUiTest {

    @Test
    fun testMappingInvoiceDomainModelToUiDto() {
        val summary = VendorPortalInvoiceSummary(
            invoiceId = "INV-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            purchaseOrderId = "PO-01",
            orderNumber = "PO-2026-001",
            invoiceNumber = "INV-2026-001",
            vendorInvoiceNumber = "VINV-100",
            invoiceDate = 1700000000000L,
            receivedDate = 1700000000000L,
            currency = "BDT",
            subtotal = Money(BigDecimal("1000.00")),
            taxAmount = Money(BigDecimal("100.00")),
            discountAmount = Money.ZERO,
            shippingAmount = Money.ZERO,
            otherCharges = Money.ZERO,
            totalAmount = Money(BigDecimal("1100.00")),
            approvedAmount = Money(BigDecimal("1100.00")),
            paidAmount = Money(BigDecimal("500.00")),
            outstandingAmount = Money(BigDecimal("600.00")),
            status = VendorInvoiceStatus.APPROVED,
            matchStatus = VendorInvoiceMatchStatus.MATCHED,
            paymentStatus = VendorPortalPaymentStatus.PARTIALLY_PAID,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )

        val dto = summary.toDto()
        assertEquals("INV-01", dto.invoiceId)
        assertEquals("APPROVED", dto.status)
        assertEquals("MATCHED", dto.matchStatus)
        assertEquals("PARTIALLY_PAID", dto.paymentStatus)
        assertEquals(1100.0, dto.totalAmount, 0.001)
        assertEquals(500.0, dto.paidAmount, 0.001)
        assertEquals(600.0, dto.outstandingAmount, 0.001)
    }

    @Test
    fun testMappingPaymentAndFinancialKpiModelsToUiDtos() {
        val payment = VendorPortalPaymentSummary(
            settlementId = "SETTLE-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            settlementNumber = "SETTLE-2026-001",
            settlementDate = 1700000000000L,
            currency = "BDT",
            totalAmount = Money(BigDecimal("5000.00")),
            paymentStatus = VendorPortalPaymentStatus.PAID,
            paymentMethod = "BANK TRANSFER",
            referenceNumber = "****4321"
        )

        val paymentDto = payment.toDto()
        assertEquals("SETTLE-01", paymentDto.settlementId)
        assertEquals(5000.0, paymentDto.totalAmount, 0.001)
        assertEquals("PAID", paymentDto.paymentStatus)

        val kpi = VendorPortalFinancialKpiSummary(
            vendorId = "VND-001",
            currency = "BDT",
            totalInvoiced = Money(BigDecimal("10000.00")),
            totalApproved = Money(BigDecimal("8000.00")),
            totalPaid = Money(BigDecimal("5000.00")),
            totalOutstanding = Money(BigDecimal("3000.00")),
            totalDisputed = Money(BigDecimal("2000.00")),
            totalOnHold = Money.ZERO,
            invoiceCount = 4,
            outstandingInvoiceCount = 2,
            paidInvoiceCount = 2
        )

        val kpiDto = kpi.toDto()
        assertEquals(10000.0, kpiDto.totalInvoiced, 0.001)
        assertEquals(4, kpiDto.invoiceCount)
    }
}
