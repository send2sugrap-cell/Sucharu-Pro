package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorSettlementDataSource
import com.sucharu.sucharupro.data.repository.VendorSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.SettlementMethod
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlement
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorSettlementTenantIsolationTest {

    private lateinit var settlementDs: FakeVendorSettlementDataSource
    private lateinit var settlementRepo: VendorSettlementRepositoryImpl

    @Before
    fun setUp() {
        settlementDs = FakeVendorSettlementDataSource()
        settlementRepo = VendorSettlementRepositoryImpl(settlementDs)
    }

    @Test
    fun testTenantIsolationForSettlements() = runBlocking {
        val settlementA = VendorSettlement(
            settlementId = "VSET-TENANT-A",
            projectId = "PRJ-A",
            tenantId = "TENANT-A",
            vendorId = "VND-A",
            settlementNumber = "SET-A-001",
            totalAmount = Money(BigDecimal("100.00")),
            status = VendorSettlementStatus.DRAFT,
            settlementMethod = SettlementMethod.BANK_TRANSFER
        )
        settlementRepo.createSettlement(settlementA)

        // Tenant B cannot retrieve Tenant A settlement
        val findFromB = settlementRepo.getSettlementById("VSET-TENANT-A", "TENANT-B")
        assertTrue(findFromB is DomainResult.Success)
        assertNull((findFromB as DomainResult.Success).data)

        // Tenant B list is empty
        val listB = settlementRepo.listSettlements(null, null, null, "TENANT-B")
        assertTrue(listB is DomainResult.Success)
        assertEquals(0, (listB as DomainResult.Success).data.size)
    }
}
