package com.sucharu.sucharupro.domain.machine.alerts

import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.alerts.FakeMachineAlertDataSource
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.alerts.MachineAlertRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.events.FaultSeverity
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.service.machine.alerts.MachineAlertServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MachineAlertDomainTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var machineRepo: MachineRegistryRepositoryImpl
    private lateinit var alertDataSource: FakeMachineAlertDataSource
    private lateinit var alertRepo: MachineAlertRepositoryImpl
    private lateinit var notificationDataSource: FakeNotificationDataSource
    private lateinit var notificationRepo: NotificationRepositoryImpl
    private lateinit var alertService: MachineAlertServiceImpl

    private val tenantId = "TENANT-001"
    private val machineId = "MAC-PRESS-001"

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        alertDataSource = FakeMachineAlertDataSource()
        alertRepo = MachineAlertRepositoryImpl(alertDataSource)
        notificationDataSource = FakeNotificationDataSource()
        notificationRepo = NotificationRepositoryImpl(notificationDataSource)
        alertService = MachineAlertServiceImpl(alertRepo, machineRepo, notificationRepo)

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
    fun test01_raiseAlert_dispatchesModule10Notification() = runBlocking {
        val alert = MachineOperationalAlert(
            alertId = "ALT-001",
            tenantId = tenantId,
            machineId = machineId,
            alertType = MachineAlertType.TELEMETRY_ABNORMAL,
            severity = FaultSeverity.CRITICAL,
            title = "High Motor Temperature",
            description = "Main motor temperature exceeded 95°C threshold",
            correlationKey = "$machineId:TEMP:OVERHEAT",
            createdBy = "STAFF-01"
        )

        val result = alertService.raiseAlert(alert)
        assertTrue(result is DomainResult.Success)
        val raised = (result as DomainResult.Success).data
        assertEquals("ALT-001", raised.alertId)
        assertEquals(MachineAlertStatus.ACTIVE, raised.status)
        assertNotNull(raised.notificationId)

        // Verify Module 10 Notification was created in NotificationRepository
        val notifRes = notificationRepo.getNotification(tenantId, raised.notificationId!!, "STAFF-01", UserRole.STAFF)
        assertTrue(notifRes is DomainResult.Success)
        val notif = (notifRes as DomainResult.Success).data
        assertEquals(NotificationType.MACHINE_TELEMETRY_ALERT, notif.notificationType)
        assertTrue(notif.title.contains("Heidelberg Speedmaster XL 106"))
    }

    @Test
    fun test02_raiseAlert_deduplicationPreventsSpam() = runBlocking {
        val alert1 = MachineOperationalAlert(
            alertId = "ALT-101",
            tenantId = tenantId,
            machineId = machineId,
            alertType = MachineAlertType.HEALTH_CRITICAL,
            severity = FaultSeverity.CRITICAL,
            title = "Machine In Fault State",
            description = "Continuous fault signal active",
            correlationKey = "$machineId:FAULT:ACTIVE",
            createdBy = "STAFF-01"
        )

        val res1 = alertService.raiseAlert(alert1)
        assertTrue(res1 is DomainResult.Success)
        val raised1 = (res1 as DomainResult.Success).data

        // Raise duplicate alert with same correlation key
        val alert2 = alert1.copy(alertId = "ALT-102", description = "Repeated fault sample")
        val res2 = alertService.raiseAlert(alert2)
        assertTrue(res2 is DomainResult.Success)
        val raised2 = (res2 as DomainResult.Success).data

        // Expect existing active alert returned without duplicate notification
        assertEquals(raised1.alertId, raised2.alertId)
    }

    @Test
    fun test03_alertLifecycle_acknowledgeAndResolve() = runBlocking {
        val alert = MachineOperationalAlert(
            alertId = "ALT-201",
            tenantId = tenantId,
            machineId = machineId,
            alertType = MachineAlertType.MAINTENANCE_OVERDUE,
            severity = FaultSeverity.WARNING,
            title = "Oil Filter Change Overdue",
            description = "Maintenance schedule SCHED-101 is 2 days overdue"
        )
        alertService.raiseAlert(alert)

        // Acknowledge
        val ackRes = alertService.acknowledgeAlert(tenantId, machineId, "ALT-201", "USR-STAFF")
        assertTrue(ackRes is DomainResult.Success)
        val acked = (ackRes as DomainResult.Success).data
        assertEquals(MachineAlertStatus.ACKNOWLEDGED, acked.status)
        assertEquals("USR-STAFF", acked.acknowledgedBy)

        // Resolve
        val resRes = alertService.resolveAlert(tenantId, machineId, "ALT-201", "Filter replaced by maintenance crew", "USR-STAFF")
        assertTrue(resRes is DomainResult.Success)
        val resolved = (resRes as DomainResult.Success).data
        assertEquals(MachineAlertStatus.RESOLVED, resolved.status)
        assertEquals("Filter replaced by maintenance crew", resolved.resolutionNotes)
    }

    @Test
    fun test04_decommissionedMachine_alert_fails() = runBlocking {
        val decommMachine = MachineEquipment(
            machineId = "MAC-DECOMM-01",
            tenantId = tenantId,
            assetCode = "EQ-OLD-01",
            name = "Old Printing Press",
            type = MachineType.PRINTING_PRESS,
            status = MachineStatus.DECOMMISSIONED
        )
        machineRepo.saveMachine(decommMachine)

        val alert = MachineOperationalAlert(
            alertId = "ALT-DECOMM-01",
            tenantId = tenantId,
            machineId = "MAC-DECOMM-01",
            alertType = MachineAlertType.WARNING,
            title = "Spurious Warning",
            description = "Decommissioned machine signal"
        )

        val res = alertService.raiseAlert(alert)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("decommissioned"))
    }

    @Test
    fun test05_canonicalProductionStageWorkflow_regressionCheck() {
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
