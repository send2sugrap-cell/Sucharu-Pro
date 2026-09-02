package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorSettlementValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorSettlementDomainTest {

    @Test
    fun testSettlementModelAndAllocationIntegrity() {
        val alloc = VendorSettlementAllocation(
            allocationId = "VSA-01",
            settlementId = "VSET-01",
            payableId = "PAY-01",
            invoiceId = "INV-01",
            allocatedAmount = Money(BigDecimal("5000.00")),
            currency = "BDT"
        )
        val settlement = VendorSettlement(
            settlementId = "VSET-01",
            projectId = "PRJ-01",
            tenantId = "TENANT-001",
            vendorId = "VND-01",
            settlementNumber = "SET-2026-001",
            totalAmount = Money(BigDecimal("5000.00")),
            status = VendorSettlementStatus.DRAFT,
            settlementMethod = SettlementMethod.BANK_TRANSFER,
            allocations = listOf(alloc)
        )

        assertEquals("VSET-01", settlement.settlementId)
        assertEquals(VendorSettlementStatus.DRAFT, settlement.status)
        assertEquals(1, settlement.allocations.size)
        assertEquals(Money(BigDecimal("5000.00")), settlement.totalAmount)
    }

    @Test
    fun testValidationFailsOnBlankOrNegativeAmounts() {
        val resBlank = VendorSettlementValidator.validateSettlementCreation(
            vendorId = "",
            settlementNumber = "SET-01",
            totalAmount = Money(BigDecimal("100.00")),
            allocations = listOf(
                VendorSettlementAllocation(
                    allocationId = "A1",
                    settlementId = "S1",
                    payableId = "P1",
                    allocatedAmount = Money(BigDecimal("100.00"))
                )
            )
        )
        assertTrue(resBlank is DomainResult.Error)

        val resZero = VendorSettlementValidator.validateSettlementCreation(
            vendorId = "VND-01",
            settlementNumber = "SET-01",
            totalAmount = Money.ZERO,
            allocations = emptyList()
        )
        assertTrue(resZero is DomainResult.Error)
    }

    @Test
    fun testValidationFailsWhenAllocationsMismatchTotal() {
        val alloc = VendorSettlementAllocation(
            allocationId = "VSA-01",
            settlementId = "VSET-01",
            payableId = "PAY-01",
            allocatedAmount = Money(BigDecimal("3000.00")),
            currency = "BDT"
        )
        val res = VendorSettlementValidator.validateSettlementCreation(
            vendorId = "VND-01",
            settlementNumber = "SET-01",
            totalAmount = Money(BigDecimal("5000.00")),
            allocations = listOf(alloc)
        )
        assertTrue(res is DomainResult.Error)
    }
}
