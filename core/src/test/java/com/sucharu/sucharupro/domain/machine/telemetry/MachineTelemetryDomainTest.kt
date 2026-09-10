package com.sucharu.sucharupro.domain.machine.telemetry

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.service.machine.telemetry.MachineTelemetryIngestionServiceImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class MachineTelemetryDomainTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var machineRepo: MachineRegistryRepositoryImpl
    private lateinit var telemetryDataSource: FakeMachineTelemetryDataSource
    private lateinit var telemetryRepo: MachineTelemetryRepositoryImpl
    private lateinit var service: MachineTelemetryIngestionServiceImpl

    private val tenantId = "TENANT-001"
    private val machineId = "MAC-PRESS-001"

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        telemetryDataSource = FakeMachineTelemetryDataSource()
        telemetryRepo = MachineTelemetryRepositoryImpl(telemetryDataSource)
        service = MachineTelemetryIngestionServiceImpl(telemetryRepo, machineRepo)

        val activeMachine = MachineEquipment(
            machineId = machineId,
            tenantId = tenantId,
            assetCode = "EQ-PRESS-01",
            name = "Heidelberg Speedmaster",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.AVAILABLE
        )
        machineRepo.saveMachine(activeMachine)
        Unit
    }

    @Test
    fun test01_ingestTelemetry_success() = runBlocking {
        val now = System.currentTimeMillis()
        val record = MachineTelemetryRecord(
            telemetryId = "TEL-001",
            tenantId = tenantId,
            machineId = machineId,
            sourceDeviceId = "SENSOR-SPD-01",
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("15000"),
            unit = "IPH",
            eventTimestamp = now
        )

        val result = service.ingestTelemetry(record)
        assertTrue(result is DomainResult.Success)
        val ingested = (result as DomainResult.Success).data
        assertEquals("TEL-001", ingested.telemetryId)
        assertEquals(BigDecimal("15000"), ingested.metricValue)
        assertEquals(now, ingested.eventTimestamp)
    }

    @Test
    fun test02_ingestTelemetry_unknownMachine_fails() = runBlocking {
        val record = MachineTelemetryRecord(
            telemetryId = "TEL-002",
            tenantId = tenantId,
            machineId = "MAC-NONEXISTENT",
            metricType = TelemetryMetricType.TEMPERATURE,
            metricValue = BigDecimal("65.5"),
            eventTimestamp = System.currentTimeMillis()
        )

        val result = service.ingestTelemetry(record)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("not found in registry"))
    }

    @Test
    fun test03_ingestTelemetry_idempotency_returnsExisting() = runBlocking {
        val now = System.currentTimeMillis()
        val record1 = MachineTelemetryRecord(
            telemetryId = "TEL-003",
            tenantId = tenantId,
            machineId = machineId,
            metricType = TelemetryMetricType.RPM,
            metricValue = BigDecimal("3000"),
            eventTimestamp = now,
            idempotencyKey = "IDEM-KEY-001"
        )

        val res1 = service.ingestTelemetry(record1)
        assertTrue(res1 is DomainResult.Success)

        val record2 = record1.copy(telemetryId = "TEL-004", metricValue = BigDecimal("9999"))
        val res2 = service.ingestTelemetry(record2)
        assertTrue(res2 is DomainResult.Success)
        val returned = (res2 as DomainResult.Success).data
        assertEquals("TEL-003", returned.telemetryId)
        assertEquals(BigDecimal("3000"), returned.metricValue)
    }

    @Test
    fun test04_validateRecord_invalidTimestamp_fails() = runBlocking {
        val invalidRecord = MachineTelemetryRecord(
            telemetryId = "TEL-005",
            tenantId = tenantId,
            machineId = machineId,
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("100"),
            eventTimestamp = -1L
        )

        val res = service.ingestTelemetry(invalidRecord)
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("positive epoch timestamp"))
    }
}
