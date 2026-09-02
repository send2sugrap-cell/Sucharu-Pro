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

class VendorDefectTest {

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

            qualityRepo.createInspection(
                VendorQualityInspection(
                    inspectionId = "vqi_01",
                    projectId = "PRJ-01",
                    vendorId = "VND-01",
                    inspectionReference = "VQI-2026-0001",
                    receivedQuantity = BigDecimal("100")
                )
            )
        }
    }

    @Test
    fun testAddAndListDefects() = runBlocking {
        val defect = VendorDefect(
            defectId = "vdf_01",
            projectId = "PRJ-01",
            inspectionId = "vqi_01",
            vendorId = "VND-01",
            defectType = VendorDefectType.COLOR_VARIANCE,
            severity = VendorDefectSeverity.HIGH,
            description = "Color density variance exceeds delta-E tolerance of 2.0",
            quantityAffected = BigDecimal("20")
        )

        val added = qualityService.addDefect(defect, "inspector")
        assertTrue(added is DomainResult.Success)

        val list = qualityService.listDefects("PRJ-01", "vqi_01")
        assertTrue(list is DomainResult.Success)
        assertEquals(1, (list as DomainResult.Success).data.size)
        assertEquals(VendorDefectSeverity.HIGH, list.data[0].severity)
    }
}
