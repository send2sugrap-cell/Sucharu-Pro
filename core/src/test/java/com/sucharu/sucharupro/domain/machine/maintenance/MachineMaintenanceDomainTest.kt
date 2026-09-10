package com.sucharu.sucharupro.domain.machine.maintenance

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.maintenance.FakeMachineMaintenanceDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.maintenance.MachineMaintenanceRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.service.machine.maintenance.MachineMaintenanceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MachineMaintenanceDomainTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var machineRepo: MachineRegistryRepositoryImpl
    private lateinit var maintenanceDataSource: FakeMachineMaintenanceDataSource
    private lateinit var maintenanceRepo: MachineMaintenanceRepositoryImpl
    private lateinit var service: MachineMaintenanceServiceImpl

    private val tenantId = "TENANT-001"
    private val machineId = "MAC-PRESS-001"

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        maintenanceDataSource = FakeMachineMaintenanceDataSource()
        maintenanceRepo = MachineMaintenanceRepositoryImpl(maintenanceDataSource)
        service = MachineMaintenanceServiceImpl(maintenanceRepo, machineRepo)

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
    fun test01_createSchedule_success() = runBlocking {
        val now = System.currentTimeMillis()
        val schedule = MaintenanceSchedule(
            scheduleId = "SCHED-001",
            tenantId = tenantId,
            machineId = machineId,
            title = "Quarterly Roller Inspection & Oil Change",
            maintenanceType = MaintenanceType.PREVENTIVE,
            plannedDate = now + 86400000L,
            recurrenceIntervalDays = 90
        )

        val result = service.createSchedule(schedule)
        assertTrue(result is DomainResult.Success)
        val created = (result as DomainResult.Success).data
        assertEquals("SCHED-001", created.scheduleId)
        assertEquals(MaintenanceStatus.PLANNED, created.status)
        assertEquals(90, created.recurrenceIntervalDays)
    }

    @Test
    fun test02_createRecord_decommissionedMachine_fails() = runBlocking {
        val decommMachine = MachineEquipment(
            machineId = "MAC-OLD-002",
            tenantId = tenantId,
            assetCode = "EQ-OLD-02",
            name = "Decommissioned Machine",
            type = MachineType.FOLDING,
            status = MachineStatus.DECOMMISSIONED
        )
        machineRepo.saveMachine(decommMachine)

        val record = MaintenanceRecord(
            recordId = "REC-001",
            tenantId = tenantId,
            machineId = "MAC-OLD-002",
            title = "Emergency Motor Check",
            maintenanceType = MaintenanceType.CORRECTIVE
        )

        val result = service.createRecord(record)
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertTrue(err.message.contains("decommissioned"))
    }

    @Test
    fun test03_maintenanceLifecycle_startAndComplete_updatesMachineStatus() = runBlocking {
        // Create record
        val record = MaintenanceRecord(
            recordId = "REC-002",
            tenantId = tenantId,
            machineId = machineId,
            title = "Gear Box Lubrication",
            maintenanceType = MaintenanceType.PREVENTIVE
        )
        val createRes = service.createRecord(record)
        assertTrue(createRes is DomainResult.Success)

        // Start Maintenance -> Machine Status becomes MAINTENANCE
        val startRes = service.startMaintenance(tenantId, machineId, "REC-002", "TECH-01", "Kamal Hossain")
        assertTrue(startRes is DomainResult.Success)
        val startedRec = (startRes as DomainResult.Success).data
        assertEquals(MaintenanceStatus.IN_PROGRESS, startedRec.status)

        val machineInMaint = (machineRepo.getMachineById(tenantId, machineId) as DomainResult.Success).data!!
        assertEquals(MachineStatus.MAINTENANCE, machineInMaint.status)

        // Complete Maintenance -> Machine Status returns to AVAILABLE
        val completeRes = service.completeMaintenance(
            tenantId = tenantId,
            machineId = machineId,
            recordId = "REC-002",
            resolutionSummary = "Gear box lubricated and alignment tested",
            workPerformed = "Replaced oil seal and filled fresh synthetic lubricant",
            completedBy = "TECH-01"
        )
        assertTrue(completeRes is DomainResult.Success)
        val completedRec = (completeRes as DomainResult.Success).data
        assertEquals(MaintenanceStatus.COMPLETED, completedRec.status)
        assertNotNull(completedRec.completedAt)

        val machineRestored = (machineRepo.getMachineById(tenantId, machineId) as DomainResult.Success).data!!
        assertEquals(MachineStatus.AVAILABLE, machineRestored.status)

        // Verify Service History Log was created
        val historyRes = service.listServiceHistory(tenantId, machineId)
        assertTrue(historyRes is DomainResult.Success)
        val logs = (historyRes as DomainResult.Success).data
        assertTrue(logs.any { it.recordId == "REC-002" && it.actionType == "COMPLETED" })
    }

    @Test
    fun test04_cancelMaintenance_restoresMachineStatus() = runBlocking {
        val record = MaintenanceRecord(
            recordId = "REC-003",
            tenantId = tenantId,
            machineId = machineId,
            title = "Optional Sensor Calibration",
            maintenanceType = MaintenanceType.PREVENTIVE
        )
        service.createRecord(record)
        service.startMaintenance(tenantId, machineId, "REC-003", "TECH-02", "Tanvir Ahmed")

        // Machine is in MAINTENANCE
        assertEquals(MachineStatus.MAINTENANCE, (machineRepo.getMachineById(tenantId, machineId) as DomainResult.Success).data!!.status)

        // Cancel maintenance
        val cancelRes = service.cancelMaintenance(tenantId, machineId, "REC-003", "Postponed for client rush job", "TECH-02")
        assertTrue(cancelRes is DomainResult.Success)
        val cancelledRec = (cancelRes as DomainResult.Success).data
        assertEquals(MaintenanceStatus.CANCELLED, cancelledRec.status)

        // Machine status restored to AVAILABLE
        assertEquals(MachineStatus.AVAILABLE, (machineRepo.getMachineById(tenantId, machineId) as DomainResult.Success).data!!.status)
    }

    @Test
    fun test05_completedMaintenance_cannotBeModified_enforcesTerminalState() = runBlocking {
        val record = MaintenanceRecord(
            recordId = "REC-004",
            tenantId = tenantId,
            machineId = machineId,
            title = "Belt Replacement",
            maintenanceType = MaintenanceType.CORRECTIVE
        )
        service.createRecord(record)
        service.startMaintenance(tenantId, machineId, "REC-004", "TECH-01", "Kamal")
        service.completeMaintenance(tenantId, machineId, "REC-004", "Belt replaced", "Replaced timing belt", "TECH-01")

        // Try restarting completed record -> Should fail
        val restartRes = service.startMaintenance(tenantId, machineId, "REC-004", "TECH-01", "Kamal")
        assertTrue(restartRes is DomainResult.Error)
        val err = restartRes as DomainResult.Error
        assertTrue(err.message.contains("Terminal maintenance status"))
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
