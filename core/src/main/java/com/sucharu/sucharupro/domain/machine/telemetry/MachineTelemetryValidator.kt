package com.sucharu.sucharupro.domain.machine.telemetry

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Validator for Machine Telemetry Records.
 */
object MachineTelemetryValidator {

    fun validateRecord(record: MachineTelemetryRecord): DomainResult<Unit> {
        if (record.telemetryId.isBlank()) {
            return DomainResult.Error(message = "Telemetry ID cannot be blank.")
        }
        if (record.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (record.machineId.isBlank()) {
            return DomainResult.Error(message = "Machine ID cannot be blank.")
        }
        if (record.eventTimestamp <= 0) {
            return DomainResult.Error(message = "Event timestamp must be a valid positive epoch timestamp.")
        }
        val maxFutureMs = System.currentTimeMillis() + (86400 * 1000L) // 24 hours future allowance
        if (record.eventTimestamp > maxFutureMs) {
            return DomainResult.Error(message = "Event timestamp cannot be more than 24 hours in the future.")
        }
        return DomainResult.Success(Unit)
    }
}
