package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus

/**
 * Domain validator for individual QC time tracking entries (Module 06 Step 08).
 */
object QcTimeEntryValidator {

    /**
     * Validates input parameters required to record a QC time entry.
     */
    fun validateCreation(
        projectId: String,
        productionJobId: String,
        entryType: QcTimeEntryType?,
        actorId: String,
        startedAt: String,
        endedAt: String?,
        durationMinutes: Long
    ): DomainResult<Unit> {
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (entryType == null) {
            return DomainResult.Error(message = "QC Time Entry Type must be specified.")
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank.")
        }
        if (startedAt.isBlank()) {
            return DomainResult.Error(message = "StartedAt timestamp cannot be blank.")
        }
        if (durationMinutes < 0L) {
            return DomainResult.Error(message = "Duration in minutes cannot be negative. Provided: $durationMinutes")
        }
        if (!endedAt.isNullOrBlank()) {
            if (endedAt < startedAt) {
                return DomainResult.Error(message = "EndedAt timestamp ($endedAt) cannot be earlier than startedAt ($startedAt).")
            }
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates internal model integrity of a [QcTimeEntry].
     */
    fun validateModel(entry: QcTimeEntry): DomainResult<Unit> {
        if (entry.id.isBlank()) {
            return DomainResult.Error(message = "Time entry ID cannot be blank.")
        }
        if (entry.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (entry.productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (entry.actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank.")
        }
        if (entry.durationMinutes < 0L) {
            return DomainResult.Error(message = "Duration in minutes cannot be negative.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Enforces status transition rules.
     */
    fun validateStatusTransition(current: QcTimeStatus, target: QcTimeStatus): DomainResult<Unit> {
        if (!current.canTransitionTo(target)) {
            return DomainResult.Error(message = "Illegal QC Time status transition from $current to $target.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Enforces immutability on locked or terminal time entries.
     */
    fun validateImmutability(entry: QcTimeEntry): DomainResult<Unit> {
        if (entry.isLocked) {
            return DomainResult.Error(message = "QC Time entry '${entry.id}' is LOCKED and cannot be modified.")
        }
        if (entry.isTerminal) {
            return DomainResult.Error(message = "QC Time entry '${entry.id}' is in terminal state '${entry.status}' and cannot be modified.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Rejects cross-job reference mismatches.
     */
    fun validateCrossJobIsolation(expectedJobId: String, actualJobId: String): DomainResult<Unit> {
        if (expectedJobId != actualJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: Time entry belongs to Job '$actualJobId' but target Job is '$expectedJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Rejects cross-project reference mismatches.
     */
    fun validateCrossProjectIsolation(expectedProjectId: String, actualProjectId: String): DomainResult<Unit> {
        if (expectedProjectId != actualProjectId) {
            return DomainResult.Error(
                message = "Cross-project reference violation: Expected Project '$expectedProjectId' but received '$actualProjectId'."
            )
        }
        return DomainResult.Success(Unit)
    }
}
