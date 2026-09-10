package com.sucharu.sucharupro.domain.machine.health

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.telemetry.MachineTelemetryRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.machine.health.MachineStatusMonitoringServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class MachineStatusMonitoringDomainTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var machineRepo: MachineRegistryRepositoryImpl
    private lateinit var telemetryDataSource: FakeMachineTelemetryDataSource
    private lateinit var telemetryRepo: MachineTelemetryRepositoryImpl
    private lateinit var service: MachineStatusMonitoringServiceImpl

    private val tenantId = "TENANT-001"
    private val machineId = "MAC-PRESS-001"

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        telemetryDataSource = FakeMachineTelemetryDataSource()
        telemetryRepo = MachineTelemetryRepositoryImpl(telemetryDataSource)
        service = MachineStatusMonitoringServiceImpl(machineRepo, telemetryRepo)

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
    fun test01_evaluateHealth_runningState_success() = runBlocking {
        val now = System.currentTimeMillis()
        val record = MachineTelemetryRecord(
            telemetryId = "TEL-001",
            tenantId = tenantId,
            machineId = machineId,
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("15000"),
            unit = "IPH",
            eventTimestamp = now
        )
        telemetryRepo.saveTelemetryRecord(record)

        val result = service.evaluateMachineHealth(tenantId, machineId)
        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data
        assertEquals(MachineOperationalState.RUNNING, snapshot.operationalState)
        assertEquals(MachineHealthCondition.HEALTHY, snapshot.healthCondition)
        assertTrue(snapshot.isFresh)
    }

    @Test
    fun test02_evaluateHealth_idleState_success() = runBlocking {
        val now = System.currentTimeMillis()
        val record = MachineTelemetryRecord(
            telemetryId = "TEL-002",
            tenantId = tenantId,
            machineId = machineId,
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal.ZERO,
            unit = "IPH",
            eventTimestamp = now
        )
        telemetryRepo.saveTelemetryRecord(record)

        val result = service.evaluateMachineHealth(tenantId, machineId)
        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data
        assertEquals(MachineOperationalState.IDLE, snapshot.operationalState)
        assertEquals(MachineHealthCondition.HEALTHY, snapshot.healthCondition)
    }

    @Test
    fun test03_evaluateHealth_staleTelemetry_returnsOffline() = runBlocking {
        val staleTime = System.currentTimeMillis() - (600 * 1000L) // 10 minutes ago
        val record = MachineTelemetryRecord(
            telemetryId = "TEL-003",
            tenantId = tenantId,
            machineId = machineId,
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("12000"),
            eventTimestamp = staleTime
        )
        telemetryRepo.saveTelemetryRecord(record)

        val result = service.evaluateMachineHealth(tenantId, machineId)
        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data
        assertEquals(MachineOperationalState.OFFLINE, snapshot.operationalState)
        assertFalse(snapshot.isFresh)
        assertTrue(snapshot.activeMessage?.contains("Telemetry stale") == true)
    }

    @Test
    fun test04_evaluateHealth_warningState_temperature() = runBlocking {
        val now = System.currentTimeMillis()
        val record = MachineTelemetryRecord(
            telemetryId = "TEL-004",
            tenantId = tenantId,
            machineId = machineId,
            metricType = TelemetryMetricType.TEMPERATURE,
            metricValue = BigDecimal("95.5"),
            unit = "C",
            eventTimestamp = now
        )
        telemetryRepo.saveTelemetryRecord(record)

        val result = service.evaluateMachineHealth(tenantId, machineId)
        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data
        assertEquals(MachineOperationalState.WARNING, snapshot.operationalState)
        assertEquals(MachineHealthCondition.DEGRADED, snapshot.healthCondition)
        assertTrue(snapshot.activeMessage?.contains("Elevated temperature") == true)
    }

    @Test
    fun test05_evaluateHealth_faultState_code() = runBlocking {
        val now = System.currentTimeMillis()
        val record = MachineTelemetryRecord(
            telemetryId = "TEL-005",
            tenantId = tenantId,
            machineId = machineId,
            metricType = TelemetryMetricType.OPERATIONAL_STATE,
            metricValue = BigDecimal("55"), // Code 55 = FAULT
            eventTimestamp = now
        )
        telemetryRepo.saveTelemetryRecord(record)

        val result = service.evaluateMachineHealth(tenantId, machineId)
        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data
        assertEquals(MachineOperationalState.FAULT, snapshot.operationalState)
        assertEquals(MachineHealthCondition.CRITICAL, snapshot.healthCondition)
        assertTrue(snapshot.activeMessage?.contains("fault state") == true)
    }

    @Test
    fun test06_evaluateHealth_noTelemetry_returnsUnknown() = runBlocking {
        val result = service.evaluateMachineHealth(tenantId, machineId)
        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data
        assertEquals(MachineOperationalState.UNKNOWN, snapshot.operationalState)
        assertEquals(MachineHealthCondition.UNKNOWN, snapshot.healthCondition)
        assertFalse(snapshot.isFresh)
    }

    @Test
    fun test07_eventTime_vs_ingestionTime_preservation() = runBlocking {
        val eventTime = System.currentTimeMillis() - (30 * 1000L) // 30s ago
        val ingestionTime = System.currentTimeMillis()
        val record = MachineTelemetryRecord(
            telemetryId = "TEL-007",
            tenantId = tenantId,
            machineId = machineId,
            metricType = TelemetryMetricType.SPEED,
            metricValue = BigDecimal("10000"),
            eventTimestamp = eventTime,
            ingestedAt = ingestionTime
        )
        telemetryRepo.saveTelemetryRecord(record)

        val result = service.evaluateMachineHealth(tenantId, machineId)
        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data
        assertEquals(eventTime, snapshot.lastEventTimestamp)
        assertEquals(ingestionTime, snapshot.lastIngestedAt)
    }
}
