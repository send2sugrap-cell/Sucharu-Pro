package com.sucharu.sucharupro.domain.machine.oee

import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.datasource.machine.events.FakeMachineEventDataSource
import com.sucharu.sucharupro.data.datasource.machine.oee.FakeMachineOeeDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.events.MachineEventRepositoryImpl
import com.sucharu.sucharupro.data.repository.machine.oee.MachineOeeRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionexecution.ProductionExecutionRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.events.DowntimeReasonCategory
import com.sucharu.sucharupro.domain.machine.events.MachineDowntimeEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.service.machine.oee.MachineOeeServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class MachineOeeDomainTest {

    private lateinit var machineDataSource: FakeMachineRegistryDataSource
    private lateinit var machineRepo: MachineRegistryRepositoryImpl
    private lateinit var oeeDataSource: FakeMachineOeeDataSource
    private lateinit var oeeRepo: MachineOeeRepositoryImpl
    private lateinit var eventDataSource: FakeMachineEventDataSource
    private lateinit var eventRepo: MachineEventRepositoryImpl
    private lateinit var prodDataSource: FakeProductionExecutionDataSource
    private lateinit var prodRepo: ProductionExecutionRepositoryImpl
    private lateinit var oeeService: MachineOeeServiceImpl

    private val tenantId = "TENANT-001"
    private val machineId = "MAC-PRESS-001"

    @Before
    fun setUp() = runBlocking {
        machineDataSource = FakeMachineRegistryDataSource()
        machineRepo = MachineRegistryRepositoryImpl(machineDataSource)
        oeeDataSource = FakeMachineOeeDataSource()
        oeeRepo = MachineOeeRepositoryImpl(oeeDataSource)
        eventDataSource = FakeMachineEventDataSource()
        eventRepo = MachineEventRepositoryImpl(eventDataSource)
        prodDataSource = FakeProductionExecutionDataSource()
        prodRepo = ProductionExecutionRepositoryImpl(prodDataSource)

        oeeService = MachineOeeServiceImpl(oeeRepo, machineRepo, eventRepo, prodRepo)

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
    fun test01_pureCalculator_correctFormulaAndZeroDivisionSafety() {
        val res1 = MachineOeeCalculator.calculate(
            plannedProductionSeconds = 28800L,
            downtimeSeconds = 3600L,
            idealRateUnitsPerHour = BigDecimal("1000"),
            actualOutputUnits = BigDecimal("6300"),
            goodOutputUnits = BigDecimal("6000"),
            rejectedOutputUnits = BigDecimal("300")
        )

        assertEquals(25200L, res1.runTimeSeconds)
        assertEquals(BigDecimal("0.8750"), res1.availabilityRatio)
        assertEquals(BigDecimal("0.9000"), res1.performanceRatio)
        assertEquals(BigDecimal("0.9524"), res1.qualityRatio)

        val expectedOee = res1.availabilityRatio.multiply(res1.performanceRatio).multiply(res1.qualityRatio).setScale(4, RoundingMode.HALF_UP)
        assertEquals(expectedOee, res1.oeeRatio)

        // Zero Planned Production Time (No NaN or Infinity)
        val resZero = MachineOeeCalculator.calculate(
            plannedProductionSeconds = 0L,
            downtimeSeconds = 0L,
            idealRateUnitsPerHour = BigDecimal("1000"),
            actualOutputUnits = BigDecimal.ZERO,
            goodOutputUnits = BigDecimal.ZERO,
            rejectedOutputUnits = BigDecimal.ZERO
        )

        assertEquals(0L, resZero.runTimeSeconds)
        assertEquals(BigDecimal("0.0000"), resZero.availabilityRatio)
        assertEquals(BigDecimal("0.0000"), resZero.performanceRatio)
        assertEquals(BigDecimal("0.0000"), resZero.qualityRatio)
        assertEquals(BigDecimal("0.0000"), resZero.oeeRatio)
    }

    @Test
    fun test02_calculateAndSaveOee_integrationWithDowntime() = runBlocking {
        val now = System.currentTimeMillis()
        val start = now - 28800000L // 8 hours ago

        val downtime = MachineDowntimeEvent(
            downtimeId = "DT-OEE-01",
            tenantId = tenantId,
            machineId = machineId,
            reasonCategory = DowntimeReasonCategory.MAINTENANCE,
            status = com.sucharu.sucharupro.domain.machine.events.DowntimeStatus.ENDED,
            startedAt = start + 3600000L,
            endedAt = start + 7200000L,
            durationSeconds = 3600L
        )
        eventRepo.saveDowntimeEvent(downtime)

        val request = MachineOeeCalculationRequest(
            tenantId = tenantId,
            machineId = machineId,
            periodStart = start,
            periodEnd = now,
            customPlannedProductionSeconds = 28800L,
            customIdealRateUnitsPerHour = BigDecimal("1000")
        )

        val calcRes = oeeService.calculateAndSaveOee(request, "USR-ANALYST")
        assertTrue(calcRes is DomainResult.Success)
        val metrics = (calcRes as DomainResult.Success).data

        assertEquals(28800L, metrics.plannedProductionSeconds)
        assertEquals(3600L, metrics.downtimeSeconds)
        assertEquals(25200L, metrics.runTimeSeconds)
        assertEquals(BigDecimal("0.8750"), metrics.availabilityRatio)
        assertEquals(BigDecimal("87.50"), metrics.availabilityPercentage)
    }

    @Test
    fun test03_decommissionedMachine_oeeCalculation_fails() = runBlocking {
        val decommMachine = MachineEquipment(
            machineId = "MAC-DECOMM-01",
            tenantId = tenantId,
            assetCode = "EQ-OLD-01",
            name = "Decommissioned Machine",
            type = MachineType.CUTTING,
            status = MachineStatus.DECOMMISSIONED
        )
        machineRepo.saveMachine(decommMachine)

        val request = MachineOeeCalculationRequest(
            tenantId = tenantId,
            machineId = "MAC-DECOMM-01",
            periodStart = System.currentTimeMillis() - 3600000L,
            periodEnd = System.currentTimeMillis()
        )

        val res = oeeService.calculateAndSaveOee(request)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("decommissioned"))
    }

    @Test
    fun test04_canonicalProductionStageWorkflow_regressionCheck() {
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
