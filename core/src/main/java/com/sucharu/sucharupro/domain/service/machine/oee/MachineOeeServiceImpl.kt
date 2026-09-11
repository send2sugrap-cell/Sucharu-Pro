package com.sucharu.sucharupro.domain.service.machine.oee

import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.events.DowntimeStatus
import com.sucharu.sucharupro.domain.machine.oee.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.productionexecution.WorkOrderStatus
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.events.MachineEventRepository
import com.sucharu.sucharupro.domain.repository.machine.oee.MachineOeeRepository
import com.sucharu.sucharupro.domain.repository.productionexecution.ProductionExecutionRepository
import java.math.BigDecimal
import java.util.UUID

/**
 * Domain Service implementation for Machine Performance & OEE calculations.
 */
class MachineOeeServiceImpl(
    private val oeeRepository: MachineOeeRepository,
    private val machineRegistryRepository: MachineRegistryRepository,
    private val eventRepository: MachineEventRepository? = null,
    private val productionExecutionRepository: ProductionExecutionRepository? = null
) : MachineOeeService {

    override suspend fun calculateAndSaveOee(
        request: MachineOeeCalculationRequest,
        actorId: String?
    ): DomainResult<MachineOeeMetrics> {
        val tenantId = request.tenantId
        val machineId = request.machineId

        if (request.periodEnd <= request.periodStart) {
            return DomainResult.Error(message = "Period end must be strictly after period start.")
        }

        val machineCheck = machineRegistryRepository.getMachineById(tenantId, machineId)
        if (machineCheck is DomainResult.Error) return machineCheck
        val machine = (machineCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Machine '$machineId' not found in registry.")

        if (machine.status == MachineStatus.DECOMMISSIONED) {
            return DomainResult.Error(message = "Cannot calculate OEE for decommissioned machine '$machineId'.")
        }

        // 1. Calculate Planned Production Time (Seconds)
        val defaultPeriodSec = (request.periodEnd - request.periodStart) / 1000L
        val plannedProductionSeconds = request.customPlannedProductionSeconds ?: defaultPeriodSec

        // 2. Query Qualifying Downtime (Seconds)
        var downtimeSeconds = 0L
        if (eventRepository != null) {
            val downtimeRes = eventRepository.listDowntimeEventsByMachine(tenantId, machineId, status = DowntimeStatus.ENDED, limit = 500)
            if (downtimeRes is DomainResult.Success) {
                downtimeSeconds = downtimeRes.data.filter { d ->
                    d.startedAt >= request.periodStart && (d.endedAt ?: d.startedAt) <= request.periodEnd
                }.sumOf { it.durationSeconds }
            }
        }

        // 3. Query Production Actuals (Good, Rejected, Total Output)
        var goodOutput = BigDecimal.ZERO
        var rejectedOutput = BigDecimal.ZERO

        val prodRepo = productionExecutionRepository
        if (prodRepo != null) {
            val jobs = runCatching { prodRepo.listJobExecutions(tenantId) }.getOrDefault(emptyList())
            for (job in jobs) {
                for (wo in job.workOrders) {
                    if (wo.assignedMachineId == machineId && wo.status == WorkOrderStatus.COMPLETED) {
                        goodOutput = goodOutput.add(wo.completedQuantity)
                        rejectedOutput = rejectedOutput.add(wo.rejectedQuantity)
                    }
                }
            }
        }
        val actualOutput = goodOutput.add(rejectedOutput)

        // 4. Theoretical Ideal Rate Units Per Hour
        val idealRateUnitsPerHour = request.customIdealRateUnitsPerHour
            ?: BigDecimal("1000.00") // Default standard shop floor rate

        // 5. Run Pure Deterministic OEE Calculator
        val calcResult = MachineOeeCalculator.calculate(
            plannedProductionSeconds = plannedProductionSeconds,
            downtimeSeconds = downtimeSeconds,
            idealRateUnitsPerHour = idealRateUnitsPerHour,
            actualOutputUnits = actualOutput,
            goodOutputUnits = goodOutput,
            rejectedOutputUnits = rejectedOutput
        )

        val metrics = MachineOeeMetrics(
            metricId = "OEE-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = tenantId,
            machineId = machineId,
            periodStart = request.periodStart,
            periodEnd = request.periodEnd,
            plannedProductionSeconds = plannedProductionSeconds,
            runTimeSeconds = calcResult.runTimeSeconds,
            downtimeSeconds = downtimeSeconds,
            idealRateUnitsPerHour = idealRateUnitsPerHour,
            actualOutputUnits = actualOutput,
            goodOutputUnits = goodOutput,
            rejectedOutputUnits = rejectedOutput,
            availabilityRatio = calcResult.availabilityRatio,
            performanceRatio = calcResult.performanceRatio,
            qualityRatio = calcResult.qualityRatio,
            oeeRatio = calcResult.oeeRatio,
            createdAt = System.currentTimeMillis(),
            createdBy = actorId
        )

        return oeeRepository.saveOeeMetrics(metrics)
    }

    override suspend fun getLatestOeeSummary(
        tenantId: String,
        machineId: String
    ): DomainResult<MachineOeeMetrics?> {
        return oeeRepository.getLatestOeeMetricsByMachine(tenantId, machineId)
    }

    override suspend fun listOeeHistory(
        tenantId: String,
        machineId: String,
        limit: Int
    ): DomainResult<List<MachineOeeMetrics>> {
        return oeeRepository.listOeeMetricsByMachine(tenantId, machineId, limit)
    }
}
