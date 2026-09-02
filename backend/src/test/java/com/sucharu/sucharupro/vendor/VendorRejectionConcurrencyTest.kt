package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorQualityDataSource
import com.sucharu.sucharupro.data.repository.VendorQualityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorRejection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorRejectionConcurrencyTest {

    private lateinit var repository: VendorQualityRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())
    }

    @Test
    fun testOptimisticConcurrencyOnRejectionUpdate() = runBlocking {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "VRJ-2026-0001",
            rejectionReason = "Initial reason",
            rejectedQuantity = BigDecimal("10")
        )
        repository.createRejection(rejection)

        // Read snapshot v1
        val snap1 = (repository.findRejectionById("PRJ-01", "vrj_01") as DomainResult.Success).data
        val snap2 = (repository.findRejectionById("PRJ-01", "vrj_01") as DomainResult.Success).data

        // Concurrent update 1 succeeds
        val update1 = repository.updateRejection(snap1.copy(rejectionReason = "Updated reason 1"))
        assertTrue(update1 is DomainResult.Success)

        // Concurrent update 2 on stale version fails
        val update2 = repository.updateRejection(snap2.copy(rejectionReason = "Updated reason 2"))
        assertTrue(update2 is DomainResult.Error)
    }

    @Test
    fun testConcurrentRejectionCreationWithSameReferenceIsBlocked() = runBlocking {
        val jobs = (1..5).map { idx ->
            async(Dispatchers.Default) {
                repository.createRejection(
                    VendorRejection(
                        rejectionId = "vrj_0$idx",
                        projectId = "PRJ-01",
                        vendorId = "VND-01",
                        rejectionReference = "VRJ-DUPLICATE-REF",
                        rejectionReason = "Reason $idx",
                        rejectedQuantity = BigDecimal("10")
                    )
                )
            }
        }

        val results = jobs.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(4, errorCount)
    }
}
