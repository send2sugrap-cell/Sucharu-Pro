package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorQualityDataSource
import com.sucharu.sucharupro.data.repository.VendorQualityRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorQualityServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorDisputeServiceTest {

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
    fun testDisputeFullLifecycle() = runBlocking {
        val dispute = VendorDispute(
            disputeId = "vds_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            disputeReference = "VDS-2026-0001",
            subject = "Damaged binding glue",
            description = "Glue expired upon delivery",
            disputedQuantity = BigDecimal("100"),
            disputedAmount = Money(5000.0),
            raisedBy = "user-1"
        )

        val created = qualityService.createDispute(dispute, "user-1")
        assertTrue(created is DomainResult.Success)

        val assigned = qualityService.assignDispute("PRJ-01", "vds_01", "procurement-mgr", "admin")
        assertTrue(assigned is DomainResult.Success)
        assertEquals(VendorDisputeStatus.UNDER_REVIEW, (assigned as DomainResult.Success).data.status)
        assertEquals("procurement-mgr", assigned.data.assignedTo)

        val responded = qualityService.submitVendorResponse("PRJ-01", "vds_01", "We can issue full credit note", "vendor-rep")
        assertTrue(responded is DomainResult.Success)
        assertEquals(VendorDisputeStatus.UNDER_REVIEW, (responded as DomainResult.Success).data.status)

        val proposed = qualityService.proposeDisputeResolution("PRJ-01", "vds_01", "Accept full credit note of 5000", "procurement-mgr")
        assertTrue(proposed is DomainResult.Success)
        assertEquals(VendorDisputeStatus.RESOLUTION_PROPOSED, (proposed as DomainResult.Success).data.status)

        val resolved = qualityService.resolveDispute("PRJ-01", "vds_01", "Credit note CN-9901 applied", "procurement-mgr")
        assertTrue(resolved is DomainResult.Success)
        assertEquals(VendorDisputeStatus.RESOLVED, (resolved as DomainResult.Success).data.status)

        val closed = qualityService.closeDispute("PRJ-01", "vds_01", "procurement-mgr")
        assertTrue(closed is DomainResult.Success)
        assertEquals(VendorDisputeStatus.CLOSED, (closed as DomainResult.Success).data.status)
    }
}
