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

class VendorDisputeWorkflowTest {

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
    fun testEscalatedDisputeWorkflow() = runBlocking {
        val dispute = VendorDispute(
            disputeId = "vds_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            disputeReference = "VDS-2026-0001",
            priority = VendorDisputePriority.CRITICAL,
            subject = "Contract breach on delivery SLAs",
            description = "Vendor refused SLA penalty clause",
            raisedBy = "user-1"
        )

        qualityService.createDispute(dispute, "user-1")
        val escalated = qualityService.escalateDispute("PRJ-01", "vds_01", "Vendor unresponsive for 14 days", "procurement-head")
        assertTrue(escalated is DomainResult.Success)
        assertEquals(VendorDisputeStatus.ESCALATED, (escalated as DomainResult.Success).data.status)
        assertEquals(VendorDisputePriority.CRITICAL, escalated.data.priority)
    }
}
