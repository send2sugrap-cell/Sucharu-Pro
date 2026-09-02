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

class VendorQualityInspectionWorkflowTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var qualityRepo: VendorQualityRepositoryImpl
    private lateinit var qualityService: VendorQualityServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
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
        }
    }

    @Test
    fun testFailedInspectionWorkflowAndAudit() = runBlocking {
        val inspection = VendorQualityInspection(
            inspectionId = "vqi_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            inspectionReference = "VQI-2026-0001",
            receivedQuantity = BigDecimal("100")
        )

        qualityService.createInspection(inspection, "creator")
        qualityService.startInspection("PRJ-01", "vqi_01", "inspector")
        val completed = qualityService.completeInspection(
            projectId = "PRJ-01",
            inspectionId = "vqi_01",
            status = VendorInspectionStatus.FAILED,
            overallResult = InspectionResult.REJECTED,
            acceptedQty = BigDecimal.ZERO,
            rejectedQty = BigDecimal("100"),
            conditionalQty = BigDecimal.ZERO,
            actorId = "inspector"
        )
        assertTrue(completed is DomainResult.Success)

        val audits = qualityRepo.listQualityAudits("PRJ-01", "vqi_01")
        assertTrue(audits is DomainResult.Success)
        assertEquals(3, (audits as DomainResult.Success).data.size)
    }
}
