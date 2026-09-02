package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPerformanceKpiTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var repo: VendorPerformanceRepositoryImpl
    private lateinit var service: VendorPerformanceServiceImpl

    @Before
    fun setUp() {
        vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
        val ds = FakeVendorPerformanceDataSource()
        repo = VendorPerformanceRepositoryImpl(ds)
        service = VendorPerformanceServiceImpl(
            performanceRepository = repo,
            vendorRepository = vendorRepo
        )
    }

    @Test
    fun testCreateAndGetKpi() = runBlocking {
        val kpi = VendorPerformanceKpi(
            kpiId = "KPI-001",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            code = "ON_TIME_DELIVERY",
            name = "On Time Delivery",
            description = "On time delivery percentage",
            kpiType = KpiType.OPERATIONAL,
            measurementMethod = KpiMeasurementMethod.AUTOMATED,
            targetValue = 95.0,
            minimumAcceptableValue = 85.0,
            unit = "%",
            direction = KpiDirection.HIGHER_IS_BETTER,
            weight = 2.0,
            createdBy = "admin"
        )

        val createRes = service.createKpi(kpi)
        assertTrue(createRes is DomainResult.Success)

        val getRes = service.getKpiById("PRJ-01", "KPI-001")
        assertTrue(getRes is DomainResult.Success)
        val fetched = (getRes as DomainResult.Success).data
        assertEquals("ON_TIME_DELIVERY", fetched.code)
        assertEquals(95.0, fetched.targetValue, 0.001)
    }

    @Test
    fun testUpdateKpi() = runBlocking {
        val kpi = VendorPerformanceKpi(
            kpiId = "KPI-002",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            code = "DEFECT_RATE",
            name = "Defect Rate",
            description = "Defect percentage",
            kpiType = KpiType.QUALITY,
            measurementMethod = KpiMeasurementMethod.AUTOMATED,
            targetValue = 1.0,
            maximumAcceptableValue = 3.0,
            unit = "%",
            direction = KpiDirection.LOWER_IS_BETTER,
            weight = 3.0,
            createdBy = "admin"
        )
        service.createKpi(kpi)

        val updated = kpi.copy(targetValue = 0.5, name = "Strict Defect Rate", version = kpi.version + 1)
        val updateRes = service.updateKpi(updated)
        assertTrue(updateRes is DomainResult.Success)
        val fetched = (service.getKpiById("PRJ-01", "KPI-002") as DomainResult.Success).data
        assertEquals(0.5, fetched.targetValue, 0.001)
        assertEquals("Strict Defect Rate", fetched.name)
    }

    @Test
    fun testListKpisWithFilter() = runBlocking {
        val kpi1 = VendorPerformanceKpi(
            kpiId = "KPI-01",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            code = "K1",
            name = "K1",
            description = "Desc 1",
            kpiType = KpiType.OPERATIONAL,
            measurementMethod = KpiMeasurementMethod.AUTOMATED,
            targetValue = 100.0,
            unit = "%",
            direction = KpiDirection.HIGHER_IS_BETTER,
            weight = 1.0,
            status = KpiStatus.ACTIVE,
            createdBy = "admin"
        )
        val kpi2 = VendorPerformanceKpi(
            kpiId = "KPI-02",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            code = "K2",
            name = "K2",
            description = "Desc 2",
            kpiType = KpiType.COMPLIANCE,
            measurementMethod = KpiMeasurementMethod.MANUAL,
            targetValue = 100.0,
            unit = "%",
            direction = KpiDirection.HIGHER_IS_BETTER,
            weight = 1.0,
            status = KpiStatus.INACTIVE,
            createdBy = "admin"
        )
        service.createKpi(kpi1)
        service.createKpi(kpi2)

        val activeList = (service.listKpis("PRJ-01", status = KpiStatus.ACTIVE) as DomainResult.Success).data
        assertEquals(1, activeList.size)
        assertEquals("KPI-01", activeList[0].kpiId)

        val complianceList = (service.listKpis("PRJ-01", kpiType = KpiType.COMPLIANCE) as DomainResult.Success).data
        assertEquals(1, complianceList.size)
        assertEquals("KPI-02", complianceList[0].kpiId)
    }
}
