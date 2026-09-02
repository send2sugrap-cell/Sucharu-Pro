package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorSettlementDataSource
import com.sucharu.sucharupro.data.repository.VendorSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.SettlementMethod
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlement
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorSettlementConcurrencyTest {

    private lateinit var settlementDs: FakeVendorSettlementDataSource
    private lateinit var settlementRepo: VendorSettlementRepositoryImpl

    @Before
    fun setUp() {
        settlementDs = FakeVendorSettlementDataSource()
        settlementRepo = VendorSettlementRepositoryImpl(settlementDs)
    }

    @Test
    fun testConcurrentSettlementNumberCreationBlocked() = runBlocking {
        val tasks = (1..5).map { i ->
            async(Dispatchers.Default) {
                val settlement = VendorSettlement(
                    settlementId = "VSET-$i",
                    projectId = "PRJ-01",
                    tenantId = "TENANT-001",
                    vendorId = "VND-01",
                    settlementNumber = "SAME-SETTLEMENT-NO",
                    totalAmount = Money(BigDecimal("100.00")),
                    status = VendorSettlementStatus.DRAFT,
                    settlementMethod = SettlementMethod.BANK_TRANSFER
                )
                settlementRepo.createSettlement(settlement)
            }
        }

        val results = tasks.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val failureCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(4, failureCount)
    }
}
