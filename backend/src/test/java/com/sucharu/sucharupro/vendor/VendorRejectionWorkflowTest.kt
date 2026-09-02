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

class VendorRejectionWorkflowTest {

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
    fun testDisputedRejectionWorkflow() = runBlocking {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "VRJ-2026-0001",
            rejectionReason = "Minor shade difference",
            rejectedQuantity = BigDecimal("15")
        )

        qualityService.createRejection(rejection, "user-1")
        qualityService.submitRejection("PRJ-01", "vrj_01", "user-1")
        val disputed = qualityService.disputeRejection("PRJ-01", "vrj_01", "Shade is within standard delta-E tolerance", "vendor-rep")
        assertTrue(disputed is DomainResult.Success)
        assertEquals(VendorRejectionStatus.DISPUTED, (disputed as DomainResult.Success).data.status)
        assertEquals("Shade is within standard delta-E tolerance", disputed.data.vendorResponse)
    }
}
