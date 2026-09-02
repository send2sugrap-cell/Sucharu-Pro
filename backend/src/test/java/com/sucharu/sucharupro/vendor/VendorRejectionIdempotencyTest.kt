package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorQualityDataSource
import com.sucharu.sucharupro.data.repository.VendorQualityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorRejection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorRejectionIdempotencyTest {

    private lateinit var repository: VendorQualityRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())
    }

    @Test
    fun testDuplicateRejectionIdRejected() = runBlocking {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "VRJ-01",
            rejectionReason = "Defect",
            rejectedQuantity = BigDecimal("10")
        )

        val first = repository.createRejection(rejection)
        assertTrue(first is DomainResult.Success)

        val second = repository.createRejection(rejection)
        assertTrue(second is DomainResult.Error)
    }
}
