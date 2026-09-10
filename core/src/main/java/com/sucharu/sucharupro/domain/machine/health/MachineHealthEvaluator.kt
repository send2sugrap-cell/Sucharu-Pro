package com.sucharu.sucharupro.domain.machine.health

import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import java.math.BigDecimal

/**
 * Deterministic Machine Operational State & Health Evaluator.
 */
object MachineHealthEvaluator {

    fun evaluate(
        machine: MachineEquipment,
        telemetryRecords: List<MachineTelemetryRecord>,
        policy: MachineHealthPolicy = MachineHealthPolicy(),
        currentTime: Long = System.currentTimeMillis()
    ): MachineHealthSnapshot {
        // 1. Master Status overrides
        if (machine.status == MachineStatus.DECOMMISSIONED) {
            return MachineHealthSnapshot(
                machineId = machine.machineId,
                tenantId = machine.tenantId,
                assetCode = machine.assetCode,
                machineName = machine.name,
                machineType = machine.type,
                masterStatus = machine.status,
                operationalState = MachineOperationalState.OFFLINE,
                healthCondition = MachineHealthCondition.CRITICAL,
                activeMessage = "Machine is decommissioned",
                evaluatedAt = currentTime
            )
        }

        if (machine.status == MachineStatus.MAINTENANCE) {
            return MachineHealthSnapshot(
                machineId = machine.machineId,
                tenantId = machine.tenantId,
                assetCode = machine.assetCode,
                machineName = machine.name,
                machineType = machine.type,
                masterStatus = machine.status,
                operationalState = MachineOperationalState.WARNING,
                healthCondition = MachineHealthCondition.DEGRADED,
                activeMessage = "Machine is under maintenance",
                evaluatedAt = currentTime
            )
        }

        if (machine.status == MachineStatus.OFFLINE) {
            return MachineHealthSnapshot(
                machineId = machine.machineId,
                tenantId = machine.tenantId,
                assetCode = machine.assetCode,
                machineName = machine.name,
                machineType = machine.type,
                masterStatus = machine.status,
                operationalState = MachineOperationalState.OFFLINE,
                healthCondition = MachineHealthCondition.UNKNOWN,
                activeMessage = "Machine master status is offline",
                evaluatedAt = currentTime
            )
        }

        // 2. Evaluate Latest Telemetry
        val latest = telemetryRecords.maxByOrNull { it.eventTimestamp }
            ?: return MachineHealthSnapshot(
                machineId = machine.machineId,
                tenantId = machine.tenantId,
                assetCode = machine.assetCode,
                machineName = machine.name,
                machineType = machine.type,
                masterStatus = machine.status,
                operationalState = MachineOperationalState.UNKNOWN,
                healthCondition = MachineHealthCondition.UNKNOWN,
                activeMessage = "No telemetry received for machine",
                evaluatedAt = currentTime
            )

        val freshnessMs = currentTime - latest.eventTimestamp
        val isFresh = freshnessMs in 0..policy.freshnessThresholdMs

        if (!isFresh) {
            return MachineHealthSnapshot(
                machineId = machine.machineId,
                tenantId = machine.tenantId,
                assetCode = machine.assetCode,
                machineName = machine.name,
                machineType = machine.type,
                masterStatus = machine.status,
                operationalState = MachineOperationalState.OFFLINE,
                healthCondition = MachineHealthCondition.UNKNOWN,
                lastEventTimestamp = latest.eventTimestamp,
                lastIngestedAt = latest.ingestedAt,
                telemetryFreshnessMs = freshnessMs,
                isFresh = false,
                latestMetricValue = latest.metricValue,
                latestMetricType = latest.metricType,
                activeMessage = "Telemetry stale (last reading ${freshnessMs / 1000}s ago)",
                evaluatedAt = currentTime
            )
        }

        // 3. Evaluate Fresh Metric Values
        var opState = MachineOperationalState.UNKNOWN
        var healthCond = MachineHealthCondition.HEALTHY
        var msg: String? = null

        when (latest.metricType) {
            TelemetryMetricType.OPERATIONAL_STATE -> {
                when {
                    latest.metricValue >= BigDecimal("50") -> {
                        opState = MachineOperationalState.FAULT
                        healthCond = MachineHealthCondition.CRITICAL
                        msg = "Machine reported fault state (code ${latest.metricValue})"
                    }
                    latest.metricValue >= BigDecimal("30") -> {
                        opState = MachineOperationalState.WARNING
                        healthCond = MachineHealthCondition.DEGRADED
                        msg = "Machine reported warning state (code ${latest.metricValue})"
                    }
                    latest.metricValue > BigDecimal.ZERO -> {
                        opState = MachineOperationalState.RUNNING
                        healthCond = MachineHealthCondition.HEALTHY
                    }
                    else -> {
                        opState = MachineOperationalState.IDLE
                        healthCond = MachineHealthCondition.HEALTHY
                    }
                }
            }
            TelemetryMetricType.SPEED, TelemetryMetricType.RPM, TelemetryMetricType.OUTPUT_COUNTER -> {
                if (latest.metricValue > BigDecimal.ZERO) {
                    opState = MachineOperationalState.RUNNING
                    healthCond = MachineHealthCondition.HEALTHY
                } else {
                    opState = MachineOperationalState.IDLE
                    healthCond = MachineHealthCondition.HEALTHY
                }
            }
            TelemetryMetricType.TEMPERATURE -> {
                if (latest.metricValue > BigDecimal("90.0")) {
                    opState = MachineOperationalState.WARNING
                    healthCond = MachineHealthCondition.DEGRADED
                    msg = "Elevated temperature reading: ${latest.metricValue}°C"
                } else {
                    opState = if (machine.status == MachineStatus.IN_USE) MachineOperationalState.RUNNING else MachineOperationalState.IDLE
                    healthCond = MachineHealthCondition.HEALTHY
                }
            }
            else -> {
                opState = if (machine.status == MachineStatus.IN_USE) MachineOperationalState.RUNNING else MachineOperationalState.IDLE
                healthCond = MachineHealthCondition.HEALTHY
            }
        }

        return MachineHealthSnapshot(
            machineId = machine.machineId,
            tenantId = machine.tenantId,
            assetCode = machine.assetCode,
            machineName = machine.name,
            machineType = machine.type,
            masterStatus = machine.status,
            operationalState = opState,
            healthCondition = healthCond,
            lastEventTimestamp = latest.eventTimestamp,
            lastIngestedAt = latest.ingestedAt,
            telemetryFreshnessMs = freshnessMs,
            isFresh = true,
            latestMetricValue = latest.metricValue,
            latestMetricType = latest.metricType,
            activeMessage = msg,
            evaluatedAt = currentTime
        )
    }
}
