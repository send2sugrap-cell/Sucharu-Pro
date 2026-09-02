package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorQualityDataSource
import com.sucharu.sucharupro.data.repository.VendorQualityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorDispute
import com.sucharu.sucharupro.domain.model.vendor.VendorQualityInspection
import com.sucharu.sucharupro.domain.model.vendor.VendorRejection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorQualityTenantIsolationTest {

    private lateinit var repository: VendorQualityRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())
    }

    @Test
    fun testTenantIsolationForQualityEntities() = runBlocking {
        // Tenant A
        repository.createInspection(
            VendorQualityInspection(
                inspectionId = "vqi_A",
                projectId = "TENANT-A",
                vendorId = "VND-01",
                inspectionReference = "VQI-A",
                receivedQuantity = BigDecimal("100")
            )
        )
        repository.createRejection(
            VendorRejection(
                rejectionId = "vrj_A",
                projectId = "TENANT-A",
                vendorId = "VND-01",
                rejectionReference = "VRJ-A",
                rejectionReason = "Defect",
                rejectedQuantity = BigDecimal("10")
            )
        )
        repository.createDispute(
            VendorDispute(
                disputeId = "vds_A",
                projectId = "TENANT-A",
                vendorId = "VND-01",
                disputeReference = "VDS-A",
                subject = "Subj A",
                description = "Desc A",
                raisedBy = "user-A"
            )
        )

        // Tenant B queries Tenant A's ID
        val inspB = repository.findInspectionById("TENANT-B", "vqi_A")
        assertTrue(inspB is DomainResult.Error)

        val rejB = repository.findRejectionById("TENANT-B", "vrj_A")
        assertTrue(rejB is DomainResult.Error)

        val dispB = repository.findDisputeById("TENANT-B", "vds_A")
        assertTrue(dispB is DomainResult.Error)

        val listInspB = repository.listInspections("TENANT-B")
        assertTrue(listInspB is DomainResult.Success && (listInspB as DomainResult.Success).data.isEmpty())
    }
}
