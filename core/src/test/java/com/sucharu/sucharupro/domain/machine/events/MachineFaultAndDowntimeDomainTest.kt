package com.sucharu.sucharupro.domain.machine.events

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.events.FakeMachineEventDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.service.machine.events.MachineDowntimeServiceImpl
import com.sucharu.sucharupro.domain.service.machine.events.MachineFaultEventServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MachineFaultAndDowntimeDomainTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var machineRepo: MachineRegistryRepositoryImpl
    private lateinit var eventDataSource: FakeMachineEventDataSource
    private lateinit var eventRepo: MachineEventRepositoryImpl
    private lateinit var faultService: MachineFaultEventServiceImpl
    private lateinit var downtimeService: MachineDowntimeServiceImpl

    private val tenantId = "TENANT-001"
    private val machineId = "MAC-PRESS-001"

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        eventDataSource = FakeMachineEventDataSource()
        eventRepo = MachineEventRepositoryImpl(eventDataSource)
        faultService = MachineFaultEventServiceImpl(eventRepo, machineRepo)
        downtimeService = MachineDowntimeServiceImpl(eventRepo, machineRepo)

        val activeMachine = MachineEquipment(
            machineId = machineId,
            tenantId = tenantId,
            assetCode = "EQ-PRESS-01",
            name = "Heidelberg Speedmaster XL 106",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.AVAILABLE
        )
        machineRepo.saveMachine(activeMachine)
        Unit
    }

    @Test
    fun test01_recordFaultEvent_success() = runBlocking {
        val now = System.currentTimeMillis()
        val fault = MachineFaultEvent(
            faultEventId = "FLT-001",
            tenantId = tenantId,
            machineId = machineId,
            faultCode = "ERR-402",
            faultType = "ELECTRICAL",
            severity = FaultSeverity.CRITICAL,
            description = "Main drive motor inverter fault",
            occurredAt = now
        )

        val result = faultService.recordFault(fault)
        assertTrue(result is DomainResult.Success)
        val recorded = (result as DomainResult.Success).data
        assertEquals("FLT-001", recorded.faultEventId)
        assertEquals(FaultStatus.OPEN, recorded.status)
        assertEquals(FaultSeverity.CRITICAL, recorded.severity)
    }

    @Test
    fun test02_faultLifecycle_acknowledgeAndResolve_success() = runBlocking {
        val now = System.currentTimeMillis()
        val fault = MachineFaultEvent(
            faultEventId = "FLT-002",
            tenantId = tenantId,
            machineId = machineId,
            faultType = "MECHANICAL",
            description = "Feeder belt misaligned",
            occurredAt = now
        )
        faultService.recordFault(fault)

        // Acknowledge
        val ackRes = faultService.acknowledgeFault(tenantId, machineId, "FLT-002", "USR-TECH")
        assertTrue(ackRes is DomainResult.Success)
        val acked = (ackRes as DomainResult.Success).data
        assertEquals(FaultStatus.ACKNOWLEDGED, acked.status)
        assertEquals("USR-TECH", acked.acknowledgedBy)

        // Resolve
        val resRes = faultService.resolveFault(
            tenantId = tenantId,
            machineId = machineId,
            faultEventId = "FLT-002",
            resolutionNotes = "Belt tension re-calibrated and test run completed",
            maintenanceRecordId = "REC-MAINT-101",
            actorId = "USR-TECH"
        )
        assertTrue(resRes is DomainResult.Success)
        val resolved = (resRes as DomainResult.Success).data
        assertEquals(FaultStatus.RESOLVED, resolved.status)
        assertEquals("USR-TECH", resolved.resolvedBy)
        assertEquals("REC-MAINT-101", resolved.maintenanceRecordId)
    }

    @Test
    fun test03_downtimeEvent_startAndEnd_calculatesDuration() = runBlocking {
        val startTime = System.currentTimeMillis() - 3600000L // 1 hour ago
        val endTime = System.currentTimeMillis()

        val downtime = MachineDowntimeEvent(
            downtimeId = "DOWNTIME-001",
            tenantId = tenantId,
            machineId = machineId,
            reasonCategory = DowntimeReasonCategory.MACHINE_FAULT,
            reasonDetails = "Waiting for motor inverter replacement",
            startedAt = startTime
        )

        val startRes = downtimeService.startDowntime(downtime)
        assertTrue(startRes is DomainResult.Success)
        val started = (startRes as DomainResult.Success).data
        assertEquals(DowntimeStatus.STARTED, started.status)

        // End Downtime
        val endRes = downtimeService.endDowntime(tenantId, machineId, "DOWNTIME-001", endedAt = endTime, actorId = "USR-STAFF")
        assertTrue(endRes is DomainResult.Success)
        val ended = (endRes as DomainResult.Success).data
        assertEquals(DowntimeStatus.ENDED, ended.status)
        assertEquals(3600L, ended.durationSeconds)
    }

    @Test
    fun test04_downtimeEnd_beforeStart_failsValidation() = runBlocking {
        val startTime = System.currentTimeMillis()
        val invalidEndTime = startTime - 1000L

        val downtime = MachineDowntimeEvent(
            downtimeId = "DOWNTIME-002",
            tenantId = tenantId,
            machineId = machineId,
            reasonCategory = DowntimeReasonCategory.POWER,
            startedAt = startTime
        )
        downtimeService.startDowntime(downtime)

        val endRes = downtimeService.endDowntime(tenantId, machineId, "DOWNTIME-002", endedAt = invalidEndTime, actorId = "USR-STAFF")
        assertTrue(endRes is DomainResult.Error)
        val err = endRes as DomainResult.Error
        assertTrue(err.message.contains("earlier than start timestamp"))
    }

    @Test
    fun test05_decommissionedMachine_eventCreation_fails() = runBlocking {
        val decommMachine = MachineEquipment(
            machineId = "MAC-DECOMM-01",
            tenantId = tenantId,
            assetCode = "EQ-OLD-01",
            name = "Decommissioned Unit",
            type = MachineType.CUTTING,
            status = MachineStatus.DECOMMISSIONED
        )
        machineRepo.saveMachine(decommMachine)

        val fault = MachineFaultEvent(
            faultEventId = "FLT-OLD-01",
            tenantId = tenantId,
            machineId = "MAC-DECOMM-01",
            faultType = "POWER",
            description = "Attempting fault record on decommissioned unit"
        )

        val res = faultService.recordFault(fault)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("decommissioned"))
    }

    @Test
    fun test06_canonicalProductionStageWorkflow_regressionCheck() {
        val expectedSequence = listOf(
            ProductionStageType.DESIGN,
            ProductionStageType.APPROVAL,
            ProductionStageType.QC,
            ProductionStageType.ITEM_APPROVAL,
            ProductionStageType.CTP,
            ProductionStageType.PRINTING,
            ProductionStageType.LAMINATION,
            ProductionStageType.FOLDING,
            ProductionStageType.BINDING,
            ProductionStageType.FINAL_QC,
            ProductionStageType.PACKAGING,
            ProductionStageType.READY,
            ProductionStageType.DELIVERED
        )
        assertEquals(13, ProductionStageType.entries.size)
        assertEquals(expectedSequence, ProductionStageType.entries)
    }
}
