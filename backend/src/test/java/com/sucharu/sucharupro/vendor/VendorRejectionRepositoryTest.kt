package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorQualityDataSource
import com.sucharu.sucharupro.data.repository.VendorQualityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorRejection
import com.sucharu.sucharupro.domain.model.vendor.VendorRejectionStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorRejectionRepositoryTest {

    private lateinit var repository: VendorQualityRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())
    }

    @Test
    fun testCreateAndFindRejection() = runBlocking {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "VRJ-2026-0001",
            rejectionReason = "Specification mismatch",
            rejectedQuantity = BigDecimal("30")
        )

        val created = repository.createRejection(rejection)
        assertTrue(created is DomainResult.Success)

        val fetched = repository.findRejectionById("PRJ-01", "vrj_01")
        assertTrue(fetched is DomainResult.Success)
        assertEquals("VRJ-2026-0001", (fetched as DomainResult.Success).data.rejectionReference)
    }

    @Test
    fun testUpdateStatusAndVersion() = runBlocking {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "VRJ-2026-0001",
            rejectionReason = "Defect",
            rejectedQuantity = BigDecimal("10")
        )
        repository.createRejection(rejection)

        val updated = repository.updateRejectionStatus(
            projectId = "PRJ-01",
            rejectionId = "vrj_01",
            status = VendorRejectionStatus.PENDING_VENDOR_RESPONSE,
            updatedBy = "user-1"
        )
        assertTrue(updated is DomainResult.Success)
        assertEquals(VendorRejectionStatus.PENDING_VENDOR_RESPONSE, (updated as DomainResult.Success).data.status)
        assertEquals(2L, updated.data.version)
    }
}
