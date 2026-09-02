package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorSettlementDataSource
import com.sucharu.sucharupro.data.repository.VendorSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.SettlementMethod
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlement
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorSettlementIdempotencyTest {

    private lateinit var settlementDs: FakeVendorSettlementDataSource
    private lateinit var settlementRepo: VendorSettlementRepositoryImpl

    @Before
    fun setUp() {
        settlementDs = FakeVendorSettlementDataSource()
        settlementRepo = VendorSettlementRepositoryImpl(settlementDs)
    }

    @Test
    fun testDuplicateSettlementCreationFails() = runBlocking {
        val settlement = VendorSettlement(
            settlementId = "VSET-IDEMPOTENT-01",
            projectId = "PRJ-01",
            tenantId = "TENANT-001",
            vendorId = "VND-01",
            settlementNumber = "SET-IDEMPOTENT-01",
            totalAmount = Money(BigDecimal("500.00")),
            status = VendorSettlementStatus.DRAFT,
            settlementMethod = SettlementMethod.BANK_TRANSFER
        )

        val first = settlementRepo.createSettlement(settlement)
        assertTrue(first is DomainResult.Success)

        val duplicate = settlementRepo.createSettlement(settlement)
        assertTrue(duplicate is DomainResult.Error)
    }
}
