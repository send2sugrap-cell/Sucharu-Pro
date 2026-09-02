package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.service.finance.VendorPayableAgingCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class VendorPayableOverdueTest {

    @Test
    fun `payable with outstanding balance past due date evaluates to OVERDUE`() {
        val now = 1700000000000L
        val pastDue = now - TimeUnit.DAYS.toMillis(5)

        val payable = VendorPayable(
            payableId = "PAY-1",
            payableNo = "PAY-00001",
            projectId = "PRJ-1",
            vendorId = "VEND-1",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-1",
            originalAmount = Money(BigDecimal("10000.00")),
            settledAmount = Money.ZERO,
            currency = "BDT",
            dueDate = pastDue,
            status = VendorPayableStatus.APPROVED,
            description = "Overdue test",
            createdBy = "acct-1"
        )

        assertEquals(VendorPayableStatus.OVERDUE, VendorPayableAgingCalculator.evaluateEffectiveStatus(payable, asOfTimestamp = now))
    }

    @Test
    fun `fully settled or cancelled payable never evaluates to OVERDUE`() {
        val now = 1700000000000L
        val pastDue = now - TimeUnit.DAYS.toMillis(5)

        val settledPayable = VendorPayable(
            payableId = "PAY-2",
            payableNo = "PAY-00002",
            projectId = "PRJ-1",
            vendorId = "VEND-1",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-2",
            originalAmount = Money(BigDecimal("10000.00")),
            settledAmount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            dueDate = pastDue,
            status = VendorPayableStatus.SETTLED,
            description = "Settled test",
            createdBy = "acct-1"
        )

        assertEquals(VendorPayableStatus.SETTLED, VendorPayableAgingCalculator.evaluateEffectiveStatus(settledPayable, asOfTimestamp = now))

        val cancelledPayable = VendorPayable(
            payableId = "PAY-3",
            payableNo = "PAY-00003",
            projectId = "PRJ-1",
            vendorId = "VEND-1",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-3",
            originalAmount = Money(BigDecimal("10000.00")),
            settledAmount = Money.ZERO,
            currency = "BDT",
            dueDate = pastDue,
            status = VendorPayableStatus.CANCELLED,
            description = "Cancelled test",
            createdBy = "acct-1"
        )

        assertEquals(VendorPayableStatus.CANCELLED, VendorPayableAgingCalculator.evaluateEffectiveStatus(cancelledPayable, asOfTimestamp = now))
    }
}
