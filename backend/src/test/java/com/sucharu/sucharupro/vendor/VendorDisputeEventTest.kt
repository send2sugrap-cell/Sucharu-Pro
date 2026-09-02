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

class VendorDisputeEventTest {

    private lateinit var qualityRepo: VendorQualityRepositoryImpl
    private lateinit var qualityService: VendorQualityServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            val vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            qualityRepo = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())
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

            qualityService.createDispute(
                VendorDispute(
                    disputeId = "vds_01",
                    projectId = "PRJ-01",
                    vendorId = "VND-01",
                    disputeReference = "VDS-2026-0001",
                    subject = "Damaged shipment",
                    description = "Description",
                    raisedBy = "user-1"
                ),
                "user-1"
            )
        }
    }

    @Test
    fun testAppendAndListDisputeEvents() = runBlocking {
        qualityService.assignDispute("PRJ-01", "vds_01", "assignee-1", "admin")
        qualityService.submitVendorResponse("PRJ-01", "vds_01", "Response received", "vendor-rep")

        val events = qualityService.listDisputeEvents("PRJ-01", "vds_01")
        assertTrue(events is DomainResult.Success)
        assertEquals(3, (events as DomainResult.Success).data.size)
        assertEquals(VendorDisputeEventType.CREATED, events.data[0].eventType)
        assertEquals(VendorDisputeEventType.ASSIGNED, events.data[1].eventType)
        assertEquals(VendorDisputeEventType.VENDOR_RESPONDED, events.data[2].eventType)
    }
}
