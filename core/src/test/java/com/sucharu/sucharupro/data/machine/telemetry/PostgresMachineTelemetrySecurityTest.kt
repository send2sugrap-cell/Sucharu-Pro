package com.sucharu.sucharupro.data.machine.telemetry

import com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class PostgresMachineTelemetrySecurityTest {

    private lateinit var dataSource: FakeMachineTelemetryDataSource

    @Before
    fun setUp() {
        dataSource = FakeMachineTelemetryDataSource()
    }

    @Test
    fun test01_tenantIsolation_tenantA_cannotAccess_tenantB_telemetry() = runBlocking {
        val recordA = MachineTelemetryRecord(
            telemetryId = "TEL-A-01",
            tenantId = "TENANT-A",
            machineId = "MAC-A1",
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("12000"),
            eventTimestamp = System.currentTimeMillis()
        )
        val recordB = MachineTelemetryRecord(
            telemetryId = "TEL-B-01",
            tenantId = "TENANT-B",
            machineId = "MAC-B1",
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("14000"),
            eventTimestamp = System.currentTimeMillis()
        )

        dataSource.saveTelemetryRecord(recordA)
        dataSource.saveTelemetryRecord(recordB)

        // Query Tenant A machine telemetry
        val listA = dataSource.listTelemetryByMachine("TENANT-A", "MAC-A1")
        assertTrue(listA is DomainResult.Success)
        val dataA = (listA as DomainResult.Success).data
        assertEquals(1, dataA.size)
        assertEquals("TEL-A-01", dataA[0].telemetryId)

        // Query Tenant B telemetry with Tenant A ID -> should return empty
        val listCross = dataSource.listTelemetryByMachine("TENANT-A", "MAC-B1")
        assertTrue(listCross is DomainResult.Success)
        assertTrue((listCross as DomainResult.Success).data.isEmpty())
    }
}
