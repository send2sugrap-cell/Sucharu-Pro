package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus

/**
 * Authoritative validator for [DesignProject] lifecycle state transitions,
 * terminal state protection, and cancellation rules.
 */
object DesignLifecycleValidator {

    /** Checks whether a status is in a terminal lifecycle state. */
    fun isTerminal(status: DesignStatus): Boolean = status.isTerminal

    /** Checks whether a project is in a terminal lifecycle state. */
    fun isTerminal(project: DesignProject): Boolean = isTerminal(project.status)

    /** Checks whether a project is operationally mutable. */
    fun isMutable(project: DesignProject): Boolean = !isTerminal(project)

    /**
     * Validates whether [project] can transition to [targetStatus].
     */
    fun validateStatusTransition(
        project: DesignProject,
        targetStatus: DesignStatus
    ): DomainResult<Unit> {
        val currentStatus = project.status

        // 1. Reject self-transitions
        if (currentStatus == targetStatus) {
            return DomainResult.Error(
                message = "Design Project '${project.projectNumber}' is already in ${currentStatus.defaultLabel} state."
            )
        }

        // 2. Reject terminal state mutations
        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Terminal design projects cannot undergo status changes (Current: ${currentStatus.defaultLabel})."
            )
        }

        // 3. Validate transition matrix
        if (!currentStatus.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Cannot transition Design Project '${project.projectNumber}' from ${currentStatus.defaultLabel} to ${targetStatus.defaultLabel}."
            )
        }

        // 4. Starting design requires an assigned designer
        if (targetStatus == DesignStatus.IN_DESIGN && !project.isAssigned) {
            return DomainResult.Error(
                message = "Cannot start design work without an assigned designer."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates cancellation of a design project, enforcing mandatory non-blank reason.
     */
    fun validateCancellation(
        project: DesignProject,
        reason: String?
    ): DomainResult<Unit> {
        if (isTerminal(project)) {
            return DomainResult.Error(
                message = "Cannot cancel Design Project '${project.projectNumber}' because it is already in terminal state ${project.status.defaultLabel}."
            )
        }

        if (reason.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Cancellation reason is required and cannot be blank."
            )
        }

        return DomainResult.Success(Unit)
    }
}
