package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionscheduling.*
import com.sucharu.sucharupro.domain.service.productionscheduling.ProductionSchedulingMathUtils.p4
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

object ProductionCapacityPlanner {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Default canonical factory machine directory.
     */
    fun getCanonicalMachines(): List<ProductionMachineAvailability> {
        return listOf(
            ProductionMachineAvailability(
                machineId = "PREPRESS-DESK-01",
                machineName = "Imposition & CTP Imaging Workstation",
                workCenter = "PREPRESS_DESK",
                isOnline = true,
                shiftHoursPerDay = BigDecimal("16.0000"),
                hourlyOutputRate = BigDecimal("20.0000"),
                supportedStageTypes = listOf(ProductionStageType.DESIGN, ProductionStageType.APPROVAL, ProductionStageType.CTP)
            ),
            ProductionMachineAvailability(
                machineId = "PRESS-OFFSET-4C-01",
                machineName = "Heidelberg Speedmaster 4-Color Press",
                workCenter = "OFFSET_PRESS_ROOM",
                isOnline = true,
                shiftHoursPerDay = BigDecimal("16.0000"),
                hourlyOutputRate = BigDecimal("8000.0000"),
                supportedStageTypes = listOf(ProductionStageType.PRINTING)
            ),
            ProductionMachineAvailability(
                machineId = "PRESS-DIGITAL-01",
                machineName = "Konica Minolta AccurioPress Digital",
                workCenter = "DIGITAL_PRESS_ROOM",
                isOnline = true,
                shiftHoursPerDay = BigDecimal("16.0000"),
                hourlyOutputRate = BigDecimal("3000.0000"),
                supportedStageTypes = listOf(ProductionStageType.PRINTING)
            ),
            ProductionMachineAvailability(
                machineId = "POSTPRESS-LAM-01",
                machineName = "Thermal Film Laminator Auto-500",
                workCenter = "LAMINATION_ROOM",
                isOnline = true,
                shiftHoursPerDay = BigDecimal("16.0000"),
                hourlyOutputRate = BigDecimal("2500.0000"),
                supportedStageTypes = listOf(ProductionStageType.LAMINATION)
            ),
            ProductionMachineAvailability(
                machineId = "POSTPRESS-DIE-01",
                machineName = "Heidelberg Cylinder Die-Cutter & Folder",
                workCenter = "DIE_CUTTING_ROOM",
                isOnline = true,
                shiftHoursPerDay = BigDecimal("16.0000"),
                hourlyOutputRate = BigDecimal("3500.0000"),
                supportedStageTypes = listOf(ProductionStageType.FOLDING)
            ),
            ProductionMachineAvailability(
                machineId = "POSTPRESS-BIND-01",
                machineName = "Horizon Stitchliner Binding Machine",
                workCenter = "BINDERY_ROOM",
                isOnline = true,
                shiftHoursPerDay = BigDecimal("16.0000"),
                hourlyOutputRate = BigDecimal("2000.0000"),
                supportedStageTypes = listOf(ProductionStageType.BINDING)
            ),
            ProductionMachineAvailability(
                machineId = "PACKAGING-LINE-01",
                machineName = "Automated Shrinkwrap & Carton Packaging Station",
                workCenter = "PACKAGING_ROOM",
                isOnline = true,
                shiftHoursPerDay = BigDecimal("16.0000"),
                hourlyOutputRate = BigDecimal("5000.0000"),
                supportedStageTypes = listOf(ProductionStageType.FINAL_QC, ProductionStageType.PACKAGING, ProductionStageType.READY)
            )
        )
    }

    /**
     * Default canonical operators.
     */
    fun getCanonicalOperators(): List<ProductionOperatorAvailability> {
        return listOf(
            ProductionOperatorAvailability(
                operatorId = "OP-PREPRESS-01",
                operatorName = "Rahim Ahmed (Prepress Specialist)",
                isActive = true,
                qualifiedStages = listOf(ProductionStageType.DESIGN, ProductionStageType.APPROVAL, ProductionStageType.CTP),
                shiftType = ShiftType.MORNING_SHIFT
            ),
            ProductionOperatorAvailability(
                operatorId = "OP-OFFSET-01",
                operatorName = "Karim Hossain (Senior Pressman)",
                isActive = true,
                qualifiedStages = listOf(ProductionStageType.PRINTING),
                shiftType = ShiftType.MORNING_SHIFT
            ),
            ProductionOperatorAvailability(
                operatorId = "OP-DIGITAL-01",
                operatorName = "Tanvir Islam (Digital Press Operator)",
                isActive = true,
                qualifiedStages = listOf(ProductionStageType.PRINTING),
                shiftType = ShiftType.MORNING_SHIFT
            ),
            ProductionOperatorAvailability(
                operatorId = "OP-FINISH-01",
                operatorName = "Milon Mia (Finishing & Bindery Lead)",
                isActive = true,
                qualifiedStages = listOf(ProductionStageType.LAMINATION, ProductionStageType.FOLDING, ProductionStageType.BINDING),
                shiftType = ShiftType.MORNING_SHIFT
            ),
            ProductionOperatorAvailability(
                operatorId = "OP-QC-PACK-01",
                operatorName = "Fatema Khatun (QC & Packaging Lead)",
                isActive = true,
                qualifiedStages = listOf(ProductionStageType.QC, ProductionStageType.ITEM_APPROVAL, ProductionStageType.FINAL_QC, ProductionStageType.PACKAGING, ProductionStageType.READY),
                shiftType = ShiftType.MORNING_SHIFT
            )
        )
    }

    /**
     * Resolves compatible machine for a specific stage type and printing method.
     */
    fun resolveCompatibleMachine(
        stageType: ProductionStageType,
        printingMethod: String,
        machines: List<ProductionMachineAvailability> = getCanonicalMachines()
    ): ProductionMachineAvailability {
        if (stageType == ProductionStageType.PRINTING) {
            if (printingMethod.equals("DIGITAL", ignoreCase = true)) {
                val digital = machines.firstOrNull { it.machineId == "PRESS-DIGITAL-01" }
                if (digital != null) return digital
            } else {
                val offset = machines.firstOrNull { it.machineId == "PRESS-OFFSET-4C-01" }
                if (offset != null) return offset
            }
        }

        val directMatch = machines.firstOrNull { m -> m.supportedStageTypes.contains(stageType) && m.isOnline }
        if (directMatch != null) return directMatch

        return machines.firstOrNull { it.isOnline } ?: machines.first()
    }

    /**
     * Resolves qualified operator for a specific stage type.
     */
    fun resolveQualifiedOperator(
        stageType: ProductionStageType,
        operators: List<ProductionOperatorAvailability> = getCanonicalOperators()
    ): ProductionOperatorAvailability? {
        return operators.firstOrNull { op -> op.isActive && op.qualifiedStages.contains(stageType) }
    }

    /**
     * Generates capacity windows for a date and updates allocations based on planned slots.
     */
    fun computeCapacityWindows(
        tenantId: String,
        startTime: Long,
        slots: List<ProductionScheduleSlot>,
        machines: List<ProductionMachineAvailability> = getCanonicalMachines()
    ): List<ProductionCapacityWindow> {
        val dateStr = synchronized(DATE_FORMAT) { DATE_FORMAT.format(Date(startTime)) }
        val windows = mutableListOf<ProductionCapacityWindow>()

        machines.forEach { machine ->
            val totalCapMinutes = machine.shiftHoursPerDay.multiply(BigDecimal("60.0000")).p4()

            val allocatedForMachine = slots.filter { it.machineId == machine.machineId }
                .sumOf { it.totalEstimatedMinutes.toLong() }
            val allocatedMinutes = BigDecimal.valueOf(allocatedForMachine).p4()

            val availableMinutes = totalCapMinutes.subtract(allocatedMinutes).max(BigDecimal.ZERO).p4()
            val utilizationRate = ProductionSchedulingMathUtils.calculateUtilizationRate(allocatedMinutes, totalCapMinutes)

            windows.add(
                ProductionCapacityWindow(
                    windowId = "CAP-${machine.machineId}-$dateStr",
                    tenantId = tenantId,
                    machineId = machine.machineId,
                    machineName = machine.machineName,
                    shiftDate = dateStr,
                    shiftType = ShiftType.MORNING_SHIFT,
                    startTimestamp = startTime,
                    endTimestamp = startTime + 16L * 3600L * 1000L,
                    totalCapacityMinutes = totalCapMinutes,
                    allocatedMinutes = allocatedMinutes,
                    availableMinutes = availableMinutes,
                    utilizationRate = utilizationRate
                )
            )
        }

        return windows
    }
}
