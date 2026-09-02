package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorQualityDataSource
import com.sucharu.sucharupro.data.repository.VendorQualityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorDispute
import com.sucharu.sucharupro.domain.model.vendor.VendorDisputeStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorDisputeRepositoryTest {

    private lateinit var repository: VendorQualityRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())
    }

    @Test
    fun testCreateAndFindDispute() = runBlocking {
        val dispute = VendorDispute(
            disputeId = "vds_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            disputeReference = "VDS-2026-0001",
            subject = "Defective shipment",
            description = "Major tears",
            raisedBy = "user-1"
        )

        val created = repository.createDispute(dispute)
        assertTrue(created is DomainResult.Success)

        val fetched = repository.findDisputeById("PRJ-01", "vds_01")
        assertTrue(fetched is DomainResult.Success)
        assertEquals("VDS-2026-0001", (fetched as DomainResult.Success).data.disputeReference)
    }

    @Test
    fun testUpdateDisputeStatus() = runBlocking {
        val dispute = VendorDispute(
            disputeId = "vds_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            disputeReference = "VDS-2026-0001",
            subject = "Defective shipment",
            description = "Major tears",
            raisedBy = "user-1"
        )
        repository.createDispute(dispute)

        val updated = repository.updateDisputeStatus(
            projectId = "PRJ-01",
            disputeId = "vds_01",
            status = VendorDisputeStatus.UNDER_REVIEW,
            updatedBy = "reviewer-1"
        )
        assertTrue(updated is DomainResult.Success)
        assertEquals(VendorDisputeStatus.UNDER_REVIEW, (updated as DomainResult.Success).data.status)
        assertEquals(2L, updated.data.version)
    }
}
