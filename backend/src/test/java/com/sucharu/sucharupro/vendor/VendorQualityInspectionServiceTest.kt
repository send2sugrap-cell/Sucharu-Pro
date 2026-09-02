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

class VendorQualityInspectionServiceTest {

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
    fun testCreateInspectionAgainstInactiveVendorFails() = runBlocking {
        vendorRepo.createVendor(
            Vendor(
                vendorId = "VND-INACTIVE",
                projectId = "PRJ-01",
                vendorCode = "V999",
                vendorName = "Suspended Vendor",
                status = VendorStatus.SUSPENDED
            )
        )

        val inspection = VendorQualityInspection(
            inspectionId = "vqi_01",
            projectId = "PRJ-01",
            vendorId = "VND-INACTIVE",
            inspectionReference = "VQI-2026-0001",
            receivedQuantity = BigDecimal("100")
        )

        val result = qualityService.createInspection(inspection, "user-1")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun testStartAndCompleteInspectionLifecycle() = runBlocking {
        val inspection = VendorQualityInspection(
            inspectionId = "vqi_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            inspectionReference = "VQI-2026-0001",
            receivedQuantity = BigDecimal("100")
        )

        val created = qualityService.createInspection(inspection, "user-1")
        assertTrue(created is DomainResult.Success)

        val started = qualityService.startInspection("PRJ-01", "vqi_01", "inspector-1")
        assertTrue(started is DomainResult.Success)
        assertEquals(VendorInspectionStatus.IN_PROGRESS, (started as DomainResult.Success).data.inspectionStatus)

        val completed = qualityService.completeInspection(
            projectId = "PRJ-01",
            inspectionId = "vqi_01",
            status = VendorInspectionStatus.PASSED,
            overallResult = InspectionResult.ACCEPTED,
            acceptedQty = BigDecimal("95"),
            rejectedQty = BigDecimal("5"),
            conditionalQty = BigDecimal.ZERO,
            actorId = "inspector-1"
        )
        assertTrue(completed is DomainResult.Success)
        assertEquals(VendorInspectionStatus.PASSED, (completed as DomainResult.Success).data.inspectionStatus)
        assertEquals(InspectionResult.ACCEPTED, (completed as DomainResult.Success).data.overallResult)
    }
}
