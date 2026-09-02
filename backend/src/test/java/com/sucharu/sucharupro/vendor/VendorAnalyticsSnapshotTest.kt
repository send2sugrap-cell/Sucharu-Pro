package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorSettlementDataSource
import com.sucharu.sucharupro.data.repository.VendorSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.AnalyticsPeriod
import com.sucharu.sucharupro.domain.model.vendor.VendorAnalyticsSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VendorAnalyticsSnapshotTest {

    private lateinit var settlementDs: FakeVendorSettlementDataSource
    private lateinit var settlementRepo: VendorSettlementRepositoryImpl

    @Before
    fun setUp() {
        settlementDs = FakeVendorSettlementDataSource()
        settlementRepo = VendorSettlementRepositoryImpl(settlementDs)
    }

    @Test
    fun testSnapshotImmutabilityAndRetrieval() = runBlocking {
        val now = System.currentTimeMillis()
        val snapshot = VendorAnalyticsSnapshot(
            snapshotId = "VSNAP-01",
            vendorId = "VND-01",
            projectId = "PRJ-01",
            tenantId = "TENANT-001",
            period = AnalyticsPeriod.MONTHLY,
            startDate = now - 30L * 86400000L,
            endDate = now,
            generatedAt = now,
            generatedBy = "user_admin",
            calculationVersion = "1.0.0",
            metricsJson = "{\"score\": 95.5, \"totalPaid\": 150000.0}"
        )

        val saveRes = settlementRepo.saveAnalyticsSnapshot(snapshot)
        assertTrue(saveRes is DomainResult.Success)

        val listRes = settlementRepo.listAnalyticsSnapshots("VND-01", AnalyticsPeriod.MONTHLY, "TENANT-001")
        assertTrue(listRes is DomainResult.Success)
        val list = (listRes as DomainResult.Success).data
        assertEquals(1, list.size)
        assertEquals("VSNAP-01", list.first().snapshotId)
        assertEquals("{\"score\": 95.5, \"totalPaid\": 150000.0}", list.first().metricsJson)
    }
}
