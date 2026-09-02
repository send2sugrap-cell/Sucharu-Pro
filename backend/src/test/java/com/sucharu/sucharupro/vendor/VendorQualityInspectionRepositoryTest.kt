package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorQualityDataSource
import com.sucharu.sucharupro.data.repository.VendorQualityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorQualityInspectionRepositoryTest {

    private lateinit var repository: VendorQualityRepositoryImpl

    @Before
    fun setUp() {
        repository = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())
    }

    @Test
    fun testCreateAndFindInspection() = runBlocking {
        val inspection = VendorQualityInspection(
            inspectionId = "vqi_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            inspectionReference = "VQI-2026-0001",
            receivedQuantity = BigDecimal("100"),
            inspectionStatus = VendorInspectionStatus.DRAFT
        )

        val created = repository.createInspection(inspection)
        assertTrue(created is DomainResult.Success)

        val fetched = repository.findInspectionById("PRJ-01", "vqi_01")
        assertTrue(fetched is DomainResult.Success)
        assertEquals("VQI-2026-0001", (fetched as DomainResult.Success).data.inspectionReference)
    }

    @Test
    fun testListInspectionsFiltering() = runBlocking {
        val insp1 = VendorQualityInspection(
            inspectionId = "vqi_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            inspectionReference = "VQI-01",
            receivedQuantity = BigDecimal("100"),
            inspectionStatus = VendorInspectionStatus.DRAFT
        )
        val insp2 = VendorQualityInspection(
            inspectionId = "vqi_02",
            projectId = "PRJ-01",
            vendorId = "VND-02",
            inspectionReference = "VQI-02",
            receivedQuantity = BigDecimal("200"),
            inspectionStatus = VendorInspectionStatus.PASSED
        )

        repository.createInspection(insp1)
        repository.createInspection(insp2)

        val listVendor1 = repository.listInspections("PRJ-01", vendorId = "VND-01")
        assertTrue(listVendor1 is DomainResult.Success)
        assertEquals(1, (listVendor1 as DomainResult.Success).data.size)

        val listPassed = repository.listInspections("PRJ-01", status = VendorInspectionStatus.PASSED)
        assertTrue(listPassed is DomainResult.Success)
        assertEquals(1, (listPassed as DomainResult.Success).data.size)
    }
}
