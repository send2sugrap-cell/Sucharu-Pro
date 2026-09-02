package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorQualityDataSource
import com.sucharu.sucharupro.data.repository.VendorQualityRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorQualityServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorRejectionServiceTest {

    private lateinit var qualityService: VendorQualityServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            val vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            val qualityRepo = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())
            qualityService = VendorQualityServiceImpl(
                vendorRepository = vendorRepo,
                qualityRepository = qualityRepo
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-01",
                    projectId = "PRJ-01",
                    vendorCode = "V001",
                    vendorName = "Apex Paper Mills",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testRejectionLifecycle() = runBlocking {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "VRJ-2026-0001",
            rejectionReason = "Thickness variance",
            rejectedQuantity = BigDecimal("20"),
            disposition = VendorRejectionDisposition.REPLACE
        )

        val created = qualityService.createRejection(rejection, "user-1")
        assertTrue(created is DomainResult.Success)

        val submitted = qualityService.submitRejection("PRJ-01", "vrj_01", "user-1")
        assertTrue(submitted is DomainResult.Success)
        assertEquals(VendorRejectionStatus.PENDING_VENDOR_RESPONSE, (submitted as DomainResult.Success).data.status)

        val accepted = qualityService.acceptRejection("PRJ-01", "vrj_01", "We agree to replace the 20 units", "vendor-rep")
        assertTrue(accepted is DomainResult.Success)
        assertEquals(VendorRejectionStatus.ACCEPTED, (accepted as DomainResult.Success).data.status)

        val resolved = qualityService.resolveRejection("PRJ-01", "vrj_01", "Replacement goods delivered", "user-1")
        assertTrue(resolved is DomainResult.Success)
        assertEquals(VendorRejectionStatus.RESOLVED, (resolved as DomainResult.Success).data.status)

        val closed = qualityService.closeRejection("PRJ-01", "vrj_01", "user-1")
        assertTrue(closed is DomainResult.Success)
        assertEquals(VendorRejectionStatus.CLOSED, (closed as DomainResult.Success).data.status)
    }
}
