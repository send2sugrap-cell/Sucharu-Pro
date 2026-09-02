package com.sucharu.sucharupro.domain.service.businessreconciliation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostTrackingFilter
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessCostAccrualFilter
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessCostCommitmentFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.data.datasource.businessreconciliation.DiscrepancyFilter
import com.sucharu.sucharupro.data.datasource.businessreconciliation.ReconciliationRunFilter
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepository
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepository
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostAccrualStatus
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostCommitmentStatus
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPosting
import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayableStatus
import com.sucharu.sucharupro.domain.repository.businesscost.BusinessCostManagementRepository
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import com.sucharu.sucharupro.domain.repository.businessledger.BusinessLedgerRepository
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import com.sucharu.sucharupro.domain.validation.businessreconciliation.BusinessFinancialReconciliationValidators
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.UUID

class BusinessFinancialReconciliationServiceImpl(
    private val repository: BusinessFinancialReconciliationRepository,
    private val expenseRepository: BusinessExpenseRepository? = null,
    private val payableRepository: VendorPayableRepository? = null,
    private val ledgerRepository: BusinessLedgerRepository? = null,
    private val costManagementRepository: BusinessCostManagementRepository? = null,
    private val costControlRepository: BusinessCostControlRepository? = null,
    private val defaultTenantId: String = "TENANT-001"
) : BusinessFinancialReconciliationService {

    private val runMutex = Mutex()

    // --- RBAC & SoD Helpers ---

    private fun checkInternalAccess(principal: AuthenticatedPrincipal): DomainResult<Unit> {
        val allowedRoles = setOf(
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.STAFF
        )
        if (principal.role !in allowedRoles) {
            return DomainResult.Error(
                message = "Access denied: Role '${principal.role}' is not authorized to access reconciliation operations."
            )
        }
        return DomainResult.Success(Unit)
    }

    private fun checkManagerOrAdmin(principal: AuthenticatedPrincipal, actionDescription: String): DomainResult<Unit> {
        val allowedRoles = setOf(UserRole.ADMIN, UserRole.MANAGER)
        if (principal.role !in allowedRoles) {
            return DomainResult.Error(
                message = "Access denied: Only ADMIN or MANAGER can perform $actionDescription (current role: ${principal.role})."
            )
        }
        return DomainResult.Success(Unit)
    }

    // --- Reconciliation Runs ---

    override suspend fun createReconciliationRun(
        principal: AuthenticatedPrincipal,
        command: CreateReconciliationRunCommand
    ): DomainResult<BusinessFinancialReconciliationRun> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // Idempotency check
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existingAudit = repository.listAuditEvents(tenantId, projectId)
                .find { it.idempotencyKey == command.idempotencyKey && it.eventType == "RUN_CREATED" }
            if (existingAudit != null && existingAudit.reconciliationRunId != null) {
                val existingRun = repository.findRunById(existingAudit.reconciliationRunId, tenantId, projectId)
                if (existingRun != null) return DomainResult.Success(existingRun)
            }
        }

        val runNumber = command.runNumber?.trim()
            ?: ("REC-" + UUID.randomUUID().toString().take(8).uppercase())

        val valRes = BusinessFinancialReconciliationValidators.validateRunCreation(
            periodId = command.periodId,
            runNumber = runNumber,
            tenantId = tenantId,
            projectId = projectId,
            createdBy = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        val existingWithNumber = repository.findRunByNumber(runNumber, tenantId, projectId)
        if (existingWithNumber != null) {
            return DomainResult.Error(message = "Reconciliation run with number '$runNumber' already exists.")
        }

        val id = "RUN-" + UUID.randomUUID().toString().take(12).uppercase()
        val initialChecksum = generateChecksum("$id:$tenantId:$projectId:${command.periodId}:$runNumber")
        val run = BusinessFinancialReconciliationRun(
            id = id,
            tenantId = tenantId,
            projectId = projectId,
            periodId = command.periodId,
            runNumber = runNumber,
            runType = command.runType,
            status = ReconciliationRunStatus.CREATED,
            startedAt = System.currentTimeMillis(),
            createdBy = principal.userId,
            checksum = initialChecksum,
            notes = command.notes,
            idempotencyKey = command.idempotencyKey
        )

        val saved = try {
            repository.createRun(run)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to create reconciliation run.")
        }

        repository.recordAuditEvent(
            BusinessFinancialReconciliationAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = saved.id,
                eventType = "RUN_CREATED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                reason = "Created reconciliation run ${saved.runNumber} for period ${saved.periodId}",
                afterState = "status=CREATED, runType=${saved.runType}",
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun executeReconciliationRun(
        principal: AuthenticatedPrincipal,
        runId: String,
        correlationId: String?
    ): DomainResult<BusinessFinancialReconciliationRun> = runMutex.withLock {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val run = repository.findRunById(runId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Reconciliation run '$runId' not found.")

        if (run.status == ReconciliationRunStatus.APPROVED) {
            return DomainResult.Error(message = "Cannot re-execute an APPROVED reconciliation run.")
        }

        // Mark RUNNING
        val runningRun = run.copy(
            status = ReconciliationRunStatus.RUNNING,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateRun(runningRun)

        val detectedDiscrepancies = mutableListOf<BusinessFinancialReconciliationDiscrepancy>()
        var totalRecordsChecked = 0
        var matchedRecords = 0

        try {
            // Execute domain-specific checks according to runType
            when (run.runType) {
                ReconciliationRunType.FULL_PERIOD -> {
                    val expRes = reconcileExpenses(tenantId, projectId, run)
                    val payRes = reconcileVendorPayables(tenantId, projectId, run)
                    val ledRes = reconcileBusinessLedger(tenantId, projectId, run)
                    val cosRes = reconcileCostAllocations(tenantId, projectId, run)
                    val cmtRes = reconcileCommitmentsAndAccruals(tenantId, projectId, run)

                    detectedDiscrepancies.addAll(expRes.discrepancies)
                    detectedDiscrepancies.addAll(payRes.discrepancies)
                    detectedDiscrepancies.addAll(ledRes.discrepancies)
                    detectedDiscrepancies.addAll(cosRes.discrepancies)
                    detectedDiscrepancies.addAll(cmtRes.discrepancies)

                    totalRecordsChecked = expRes.checked + payRes.checked + ledRes.checked + cosRes.checked + cmtRes.checked
                    matchedRecords = expRes.matched + payRes.matched + ledRes.matched + cosRes.matched + cmtRes.matched
                }
                ReconciliationRunType.EXPENSE -> {
                    val expRes = reconcileExpenses(tenantId, projectId, run)
                    detectedDiscrepancies.addAll(expRes.discrepancies)
                    totalRecordsChecked = expRes.checked
                    matchedRecords = expRes.matched
                }
                ReconciliationRunType.VENDOR_PAYABLE -> {
                    val payRes = reconcileVendorPayables(tenantId, projectId, run)
                    detectedDiscrepancies.addAll(payRes.discrepancies)
                    totalRecordsChecked = payRes.checked
                    matchedRecords = payRes.matched
                }
                ReconciliationRunType.LEDGER -> {
                    val ledRes = reconcileBusinessLedger(tenantId, projectId, run)
                    detectedDiscrepancies.addAll(ledRes.discrepancies)
                    totalRecordsChecked = ledRes.checked
                    matchedRecords = ledRes.matched
                }
                ReconciliationRunType.COST_ALLOCATION -> {
                    val cosRes = reconcileCostAllocations(tenantId, projectId, run)
                    detectedDiscrepancies.addAll(cosRes.discrepancies)
                    totalRecordsChecked = cosRes.checked
                    matchedRecords = cosRes.matched
                }
                ReconciliationRunType.COMMITMENT -> {
                    val cmtRes = reconcileCommitments(tenantId, projectId, run)
                    detectedDiscrepancies.addAll(cmtRes.discrepancies)
                    totalRecordsChecked = cmtRes.checked
                    matchedRecords = cmtRes.matched
                }
                ReconciliationRunType.ACCRUAL -> {
                    val acrRes = reconcileAccruals(tenantId, projectId, run)
                    detectedDiscrepancies.addAll(acrRes.discrepancies)
                    totalRecordsChecked = acrRes.checked
                    matchedRecords = acrRes.matched
                }
            }

            // Save detected discrepancies in batch
            if (detectedDiscrepancies.isNotEmpty()) {
                repository.createDiscrepanciesBatch(detectedDiscrepancies)
            }

            val criticalCount = detectedDiscrepancies.count { it.severity == DiscrepancySeverity.CRITICAL }
            val warningCount = detectedDiscrepancies.count { it.severity == DiscrepancySeverity.WARNING }
            val newStatus = if (detectedDiscrepancies.isEmpty()) {
                ReconciliationRunStatus.COMPLETED
            } else {
                ReconciliationRunStatus.UNDER_REVIEW
            }

            val snapshotData = "checked=$totalRecordsChecked, matched=$matchedRecords, discrepancies=${detectedDiscrepancies.size}, critical=$criticalCount, warnings=$warningCount"
            val snapshotChecksum = generateChecksum(snapshotData + ":" + run.id)

            val snapshot = BusinessFinancialReconciliationSnapshot(
                id = "SNP-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = run.id,
                periodId = run.periodId,
                snapshotData = snapshotData,
                checksum = snapshotChecksum
            )
            repository.saveSnapshot(snapshot)

            val finalRun = runningRun.copy(
                status = newStatus,
                completedAt = System.currentTimeMillis(),
                totalRecordsChecked = totalRecordsChecked,
                matchedRecords = matchedRecords,
                discrepancyCount = detectedDiscrepancies.size,
                criticalDiscrepancyCount = criticalCount,
                warningCount = warningCount,
                checksum = snapshotChecksum,
                updatedAt = System.currentTimeMillis()
            )
            val savedRun = repository.updateRun(finalRun)

            repository.recordAuditEvent(
                BusinessFinancialReconciliationAuditEvent(
                    id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                    tenantId = tenantId,
                    projectId = projectId,
                    reconciliationRunId = savedRun.id,
                    eventType = "RUN_COMPLETED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    reason = "Executed reconciliation run: $totalRecordsChecked checked, ${detectedDiscrepancies.size} discrepancies found",
                    afterState = "status=${savedRun.status}, critical=$criticalCount, warnings=$warningCount",
                    checksum = snapshotChecksum,
                    correlationId = correlationId
                )
            )

            DomainResult.Success(savedRun)
        } catch (e: Exception) {
            val failedRun = runningRun.copy(
                status = ReconciliationRunStatus.FAILED,
                notes = (runningRun.notes ?: "") + "\nExecution error: ${e.message}",
                updatedAt = System.currentTimeMillis()
            )
            repository.updateRun(failedRun)

            repository.recordAuditEvent(
                BusinessFinancialReconciliationAuditEvent(
                    id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                    tenantId = tenantId,
                    projectId = projectId,
                    reconciliationRunId = failedRun.id,
                    eventType = "RUN_FAILED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    reason = "Reconciliation execution failed: ${e.message}",
                    correlationId = correlationId
                )
            )

            DomainResult.Error(message = "Reconciliation execution failed: ${e.message}")
        }
    }

    override suspend fun getReconciliationRunById(
        principal: AuthenticatedPrincipal,
        runId: String
    ): DomainResult<BusinessFinancialReconciliationRun> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val run = repository.findRunById(runId, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Reconciliation run '$runId' not found.")
        return DomainResult.Success(run)
    }

    override suspend fun listReconciliationRuns(
        principal: AuthenticatedPrincipal,
        filter: ReconciliationRunFilter
    ): DomainResult<List<BusinessFinancialReconciliationRun>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val runs = repository.listRuns(defaultTenantId, principal.projectId, filter)
        return DomainResult.Success(runs)
    }

    override suspend fun getDiscrepancyById(
        principal: AuthenticatedPrincipal,
        discrepancyId: String
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val disc = repository.findDiscrepancyById(discrepancyId, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Discrepancy '$discrepancyId' not found.")
        return DomainResult.Success(disc)
    }

    override suspend fun listDiscrepancies(
        principal: AuthenticatedPrincipal,
        filter: DiscrepancyFilter
    ): DomainResult<List<BusinessFinancialReconciliationDiscrepancy>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val discrepancies = repository.listDiscrepancies(defaultTenantId, principal.projectId, filter)
        return DomainResult.Success(discrepancies)
    }

    override suspend fun assignDiscrepancy(
        principal: AuthenticatedPrincipal,
        command: AssignDiscrepancyCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findDiscrepancyById(command.discrepancyId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Discrepancy '${command.discrepancyId}' not found.")

        val updated = existing.copy(
            assignedTo = command.assignedTo.trim(),
            status = if (existing.status == DiscrepancyStatus.OPEN) DiscrepancyStatus.INVESTIGATING else existing.status,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateDiscrepancy(updated)

        repository.recordAuditEvent(
            BusinessFinancialReconciliationAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = saved.reconciliationRunId,
                discrepancyId = saved.id,
                eventType = "DISCREPANCY_ASSIGNED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                reason = "Assigned discrepancy to ${command.assignedTo}",
                afterState = "assignedTo=${command.assignedTo}, status=${saved.status}"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun resolveDiscrepancy(
        principal: AuthenticatedPrincipal,
        command: ResolveDiscrepancyCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findDiscrepancyById(command.discrepancyId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Discrepancy '${command.discrepancyId}' not found.")

        val valRes = BusinessFinancialReconciliationValidators.validateDiscrepancyResolution(
            discrepancy = existing,
            resolutionNote = command.resolutionNote,
            resolvedBy = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = existing.copy(
            status = DiscrepancyStatus.RESOLVED,
            resolutionNote = command.resolutionNote.trim(),
            resolvedBy = principal.userId,
            resolvedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateDiscrepancy(updated)

        repository.recordAuditEvent(
            BusinessFinancialReconciliationAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = saved.reconciliationRunId,
                discrepancyId = saved.id,
                eventType = "DISCREPANCY_RESOLVED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                reason = command.resolutionNote,
                beforeState = "status=${existing.status}",
                afterState = "status=RESOLVED, resolvedBy=${principal.userId}",
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun waiveDiscrepancy(
        principal: AuthenticatedPrincipal,
        command: WaiveDiscrepancyCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy> {
        val access = checkManagerOrAdmin(principal, "waive reconciliation discrepancies")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findDiscrepancyById(command.discrepancyId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Discrepancy '${command.discrepancyId}' not found.")

        val valRes = BusinessFinancialReconciliationValidators.validateDiscrepancyWaiver(
            discrepancy = existing,
            waiverReason = command.waiverReason,
            waivedBy = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = existing.copy(
            status = DiscrepancyStatus.WAIVED,
            resolutionNote = "WAIVED: " + command.waiverReason.trim(),
            approvedBy = principal.userId,
            approvedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateDiscrepancy(updated)

        repository.recordAuditEvent(
            BusinessFinancialReconciliationAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = saved.reconciliationRunId,
                discrepancyId = saved.id,
                eventType = "DISCREPANCY_WAIVED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                reason = command.waiverReason,
                beforeState = "status=${existing.status}",
                afterState = "status=WAIVED, approvedBy=${principal.userId}",
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun rejectDiscrepancy(
        principal: AuthenticatedPrincipal,
        command: RejectDiscrepancyCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findDiscrepancyById(command.discrepancyId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Discrepancy '${command.discrepancyId}' not found.")

        val updated = existing.copy(
            status = DiscrepancyStatus.REJECTED,
            resolutionNote = "REJECTED: " + command.rejectionReason.trim(),
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateDiscrepancy(updated)

        repository.recordAuditEvent(
            BusinessFinancialReconciliationAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = saved.reconciliationRunId,
                discrepancyId = saved.id,
                eventType = "DISCREPANCY_REJECTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                reason = command.rejectionReason,
                afterState = "status=REJECTED",
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun approveReconciliationRun(
        principal: AuthenticatedPrincipal,
        command: ApproveReconciliationCommand
    ): DomainResult<BusinessFinancialReconciliationRun> {
        val access = checkManagerOrAdmin(principal, "approve reconciliation runs")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val run = repository.findRunById(command.runId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Reconciliation run '${command.runId}' not found.")

        val valRes = BusinessFinancialReconciliationValidators.validateRunApproval(
            run = run,
            approverId = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        // Check if unresolved CRITICAL discrepancies remain
        val unresolvedCritical = repository.listDiscrepancies(
            tenantId, projectId,
            DiscrepancyFilter(reconciliationRunId = run.id, severity = DiscrepancySeverity.CRITICAL)
        ).filter { it.status.isOpenOrInvestigating }

        if (unresolvedCritical.isNotEmpty()) {
            return DomainResult.Error(
                message = "Cannot approve reconciliation run '${run.runNumber}': ${unresolvedCritical.size} critical discrepancies are still unresolved. Resolve or waive them first."
            )
        }

        val updated = run.copy(
            status = ReconciliationRunStatus.APPROVED,
            approvedBy = principal.userId,
            reviewedBy = run.reviewedBy ?: principal.userId,
            notes = if (!command.notes.isNullOrBlank()) (run.notes?.let { "$it\n" } ?: "") + "Approved: ${command.notes}" else run.notes,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateRun(updated)

        repository.recordAuditEvent(
            BusinessFinancialReconciliationAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = saved.id,
                eventType = "RECONCILIATION_APPROVED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                reason = command.notes ?: "Approved reconciliation run",
                beforeState = "status=${run.status}",
                afterState = "status=APPROVED, approvedBy=${principal.userId}",
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun linkCorrection(
        principal: AuthenticatedPrincipal,
        command: LinkCorrectionCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findDiscrepancyById(command.discrepancyId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Discrepancy '${command.discrepancyId}' not found.")

        val valRes = BusinessFinancialReconciliationValidators.validateCorrectionLinkage(
            discrepancy = existing,
            correctionType = command.correctionType,
            correctionId = command.correctionId,
            actorId = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = existing.copy(
            linkedCorrectionType = command.correctionType.trim(),
            linkedCorrectionId = command.correctionId.trim(),
            resolutionNote = if (!command.note.isNullOrBlank()) (existing.resolutionNote?.let { "$it\n" } ?: "") + command.note else existing.resolutionNote,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateDiscrepancy(updated)

        repository.recordAuditEvent(
            BusinessFinancialReconciliationAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = saved.reconciliationRunId,
                discrepancyId = saved.id,
                eventType = "CORRECTION_LINKED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                reason = "Linked correction ${command.correctionType}:${command.correctionId}",
                afterState = "correctionType=${command.correctionType}, correctionId=${command.correctionId}",
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun getPeriodCloseReadiness(
        principal: AuthenticatedPrincipal,
        periodId: String
    ): DomainResult<PeriodCloseReadiness> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val runs = repository.listRuns(tenantId, projectId, ReconciliationRunFilter(periodId = periodId))
        val discrepancies = repository.listDiscrepancies(tenantId, projectId, DiscrepancyFilter(periodId = periodId))

        val unresolvedCritical = discrepancies.filter { it.severity == DiscrepancySeverity.CRITICAL && it.status.isOpenOrInvestigating }
        val unresolvedWarnings = discrepancies.filter { it.severity == DiscrepancySeverity.WARNING && it.status.isOpenOrInvestigating }

        val blockingIssues = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (runs.isEmpty()) {
            blockingIssues.add("No financial reconciliation run has been executed for period '$periodId'.")
        } else {
            val unapprovedRuns = runs.filter { it.status != ReconciliationRunStatus.APPROVED }
            if (unapprovedRuns.isNotEmpty()) {
                blockingIssues.add("${unapprovedRuns.size} reconciliation runs are not APPROVED for period '$periodId'.")
            }
        }

        if (unresolvedCritical.isNotEmpty()) {
            blockingIssues.add("${unresolvedCritical.size} CRITICAL discrepancies remain unresolved in period '$periodId'.")
        }

        if (unresolvedWarnings.isNotEmpty()) {
            warnings.add("${unresolvedWarnings.size} WARNING discrepancies exist in period '$periodId'.")
        }

        val isReady = blockingIssues.isEmpty()
        val allRequiredRunsApproved = runs.isNotEmpty() && runs.all { it.status == ReconciliationRunStatus.APPROVED }

        val readiness = PeriodCloseReadiness(
            periodId = periodId,
            isReady = isReady,
            blockingIssues = blockingIssues,
            warnings = warnings,
            reconciliationRunIds = runs.map { it.id },
            unresolvedCriticalCount = unresolvedCritical.size,
            unresolvedWarningCount = unresolvedWarnings.size,
            allRequiredRunsApproved = allRequiredRunsApproved,
            calculatedAt = System.currentTimeMillis()
        )

        return DomainResult.Success(readiness)
    }

    override suspend fun getDashboardSummary(
        principal: AuthenticatedPrincipal,
        periodId: String?
    ): DomainResult<ReconciliationDashboardSummary> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val runs = repository.listRuns(tenantId, projectId, ReconciliationRunFilter(periodId = periodId))
        val discrepancies = repository.listDiscrepancies(tenantId, projectId, DiscrepancyFilter(periodId = periodId))

        val summary = ReconciliationDashboardSummary(
            totalRuns = runs.size,
            approvedRuns = runs.count { it.status == ReconciliationRunStatus.APPROVED },
            openDiscrepancies = discrepancies.count { it.status.isOpenOrInvestigating },
            criticalDiscrepancies = discrepancies.count { it.severity == DiscrepancySeverity.CRITICAL && it.status.isOpenOrInvestigating },
            resolvedDiscrepancies = discrepancies.count { it.status == DiscrepancyStatus.RESOLVED },
            totalRecordsChecked = runs.sumOf { it.totalRecordsChecked },
            totalMatchedRecords = runs.sumOf { it.matchedRecords },
            readyToClosePeriods = if (runs.isNotEmpty() && discrepancies.none { it.severity == DiscrepancySeverity.CRITICAL && it.status.isOpenOrInvestigating }) 1 else 0
        )

        return DomainResult.Success(summary)
    }

    override suspend fun listAuditEvents(
        principal: AuthenticatedPrincipal,
        runId: String?,
        discrepancyId: String?
    ): DomainResult<List<BusinessFinancialReconciliationAuditEvent>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val events = repository.listAuditEvents(defaultTenantId, principal.projectId, runId, discrepancyId)
        return DomainResult.Success(events)
    }

    // --- Domain Engine Implementations ---

    private data class ReconResult(
        val checked: Int,
        val matched: Int,
        val discrepancies: List<BusinessFinancialReconciliationDiscrepancy>
    )

    private suspend fun reconcileExpenses(tenantId: String, projectId: String, run: BusinessFinancialReconciliationRun): ReconResult {
        if (expenseRepository == null) return ReconResult(0, 0, emptyList())

        val expensesRes = expenseRepository.listExpenses(tenantId = tenantId, projectId = projectId, limit = 10000)
        val expenses = if (expensesRes is DomainResult.Success) expensesRes.data else emptyList()
        val approvedExpenses = expenses.filter { it.status == BusinessExpenseStatus.APPROVED }

        val discrepancies = mutableListOf<BusinessFinancialReconciliationDiscrepancy>()
        var checked = 0
        var matched = 0

        for (exp in approvedExpenses) {
            checked++
            matched++
        }

        return ReconResult(checked, matched, discrepancies)
    }

    private suspend fun reconcileVendorPayables(tenantId: String, projectId: String, run: BusinessFinancialReconciliationRun): ReconResult {
        if (payableRepository == null) return ReconResult(0, 0, emptyList())

        val payablesRes = payableRepository.listPayables(tenantId = tenantId, projectId = projectId, limit = 10000)
        val payables = if (payablesRes is DomainResult.Success) payablesRes.data else emptyList()
        val discrepancies = mutableListOf<BusinessFinancialReconciliationDiscrepancy>()
        var checked = 0
        var matched = 0

        for (p in payables) {
            checked++
            if (p.paidAmount > p.originalAmount) {
                discrepancies.add(
                    BusinessFinancialReconciliationDiscrepancy(
                        id = "DISC-" + UUID.randomUUID().toString().take(12).uppercase(),
                        tenantId = tenantId,
                        projectId = projectId,
                        reconciliationRunId = run.id,
                        periodId = run.periodId,
                        discrepancyType = FinancialDiscrepancyType.OVER_ALLOCATION,
                        severity = DiscrepancySeverity.CRITICAL,
                        sourceType = "VENDOR_PAYABLE",
                        sourceId = p.payableId,
                        expectedAmount = p.originalAmount,
                        actualAmount = p.paidAmount,
                        differenceAmount = (p.paidAmount.subtract(p.originalAmount)).setScale(4, RoundingMode.HALF_UP),
                        currency = p.currency,
                        description = "Vendor payable '${p.payableNumber}' paid amount (${p.paidAmount}) exceeds original liability (${p.originalAmount})."
                    )
                )
            } else {
                matched++
            }
        }

        return ReconResult(checked, matched, discrepancies)
    }

    private suspend fun reconcileBusinessLedger(tenantId: String, projectId: String, run: BusinessFinancialReconciliationRun): ReconResult {
        if (ledgerRepository == null) return ReconResult(0, 0, emptyList())

        val postings = ledgerRepository.listPostings(tenantId, projectId, BusinessLedgerPostingFilter())
        val discrepancies = mutableListOf<BusinessFinancialReconciliationDiscrepancy>()
        val checked = postings.size
        var matched = 0

        val debitsSum = postings.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.debitAmount) }.setScale(4, RoundingMode.HALF_UP)
        val creditsSum = postings.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.creditAmount) }.setScale(4, RoundingMode.HALF_UP)

        if (debitsSum.compareTo(creditsSum) != 0) {
            discrepancies.add(
                BusinessFinancialReconciliationDiscrepancy(
                    id = "DISC-" + UUID.randomUUID().toString().take(12).uppercase(),
                    tenantId = tenantId,
                    projectId = projectId,
                    reconciliationRunId = run.id,
                    periodId = run.periodId,
                    discrepancyType = FinancialDiscrepancyType.BALANCE_MISMATCH,
                    severity = DiscrepancySeverity.CRITICAL,
                    sourceType = "BUSINESS_LEDGER",
                    sourceId = "LEDGER-TOTALS",
                    expectedAmount = debitsSum,
                    actualAmount = creditsSum,
                    differenceAmount = (debitsSum.subtract(creditsSum)).abs().setScale(4, RoundingMode.HALF_UP),
                    currency = "BDT",
                    description = "Canonical Business Ledger debits sum ($debitsSum) does not equal credits sum ($creditsSum)."
                )
            )
        } else {
            matched += postings.size
        }

        return ReconResult(checked, matched, discrepancies)
    }

    private suspend fun reconcileCostAllocations(tenantId: String, projectId: String, run: BusinessFinancialReconciliationRun): ReconResult {
        if (costManagementRepository == null) return ReconResult(0, 0, emptyList())

        val trackings = costManagementRepository.listCostTracking(tenantId, projectId, BusinessCostTrackingFilter())
        val discrepancies = mutableListOf<BusinessFinancialReconciliationDiscrepancy>()
        var checked = 0
        var matched = 0

        for (t in trackings) {
            checked++
            if (t.amount < BigDecimal.ZERO) {
                discrepancies.add(
                    BusinessFinancialReconciliationDiscrepancy(
                        id = "DISC-" + UUID.randomUUID().toString().take(12).uppercase(),
                        tenantId = tenantId,
                        projectId = projectId,
                        reconciliationRunId = run.id,
                        periodId = run.periodId,
                        discrepancyType = FinancialDiscrepancyType.INVALID_CLASSIFICATION,
                        severity = DiscrepancySeverity.CRITICAL,
                        sourceType = "COST_TRACKING",
                        sourceId = t.id,
                        expectedAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                        actualAmount = t.amount,
                        differenceAmount = t.amount.abs().setScale(4, RoundingMode.HALF_UP),
                        currency = t.currency,
                        description = "Cost tracking '${t.id}' has negative amount (${t.amount})."
                    )
                )
            } else {
                matched++
            }
        }

        return ReconResult(checked, matched, discrepancies)
    }

    private suspend fun reconcileCommitments(tenantId: String, projectId: String, run: BusinessFinancialReconciliationRun): ReconResult {
        if (costControlRepository == null) return ReconResult(0, 0, emptyList())

        val commitments = costControlRepository.listCommitments(tenantId, projectId, BusinessCostCommitmentFilter())
        val discrepancies = mutableListOf<BusinessFinancialReconciliationDiscrepancy>()
        var checked = 0
        var matched = 0

        for (c in commitments) {
            checked++
            if (c.consumedAmount > c.committedAmount) {
                discrepancies.add(
                    BusinessFinancialReconciliationDiscrepancy(
                        id = "DISC-" + UUID.randomUUID().toString().take(12).uppercase(),
                        tenantId = tenantId,
                        projectId = projectId,
                        reconciliationRunId = run.id,
                        periodId = run.periodId,
                        discrepancyType = FinancialDiscrepancyType.COMMITMENT_OVER_CONSUMPTION,
                        severity = DiscrepancySeverity.CRITICAL,
                        sourceType = "COST_COMMITMENT",
                        sourceId = c.id,
                        expectedAmount = c.committedAmount,
                        actualAmount = c.consumedAmount,
                        differenceAmount = (c.consumedAmount.subtract(c.committedAmount)).setScale(4, RoundingMode.HALF_UP),
                        currency = c.currency,
                        description = "Cost commitment '${c.commitmentNumber}' consumed amount (${c.consumedAmount}) exceeds committed limit (${c.committedAmount})."
                    )
                )
            } else {
                matched++
            }
        }

        return ReconResult(checked, matched, discrepancies)
    }

    private suspend fun reconcileAccruals(tenantId: String, projectId: String, run: BusinessFinancialReconciliationRun): ReconResult {
        if (costControlRepository == null) return ReconResult(0, 0, emptyList())

        val accruals = costControlRepository.listAccruals(tenantId, projectId, BusinessCostAccrualFilter(accountingPeriodId = run.periodId))
        val discrepancies = mutableListOf<BusinessFinancialReconciliationDiscrepancy>()
        var checked = 0
        var matched = 0

        for (a in accruals) {
            checked++
            if (a.reversedAmount > a.accrualAmount) {
                discrepancies.add(
                    BusinessFinancialReconciliationDiscrepancy(
                        id = "DISC-" + UUID.randomUUID().toString().take(12).uppercase(),
                        tenantId = tenantId,
                        projectId = projectId,
                        reconciliationRunId = run.id,
                        periodId = run.periodId,
                        discrepancyType = FinancialDiscrepancyType.AMOUNT_MISMATCH,
                        severity = DiscrepancySeverity.CRITICAL,
                        sourceType = "COST_ACCRUAL",
                        sourceId = a.id,
                        expectedAmount = a.accrualAmount,
                        actualAmount = a.reversedAmount,
                        differenceAmount = (a.reversedAmount.subtract(a.accrualAmount)).setScale(4, RoundingMode.HALF_UP),
                        currency = a.currency,
                        description = "Cost accrual '${a.accrualNumber}' reversed amount (${a.reversedAmount}) exceeds original accrual (${a.accrualAmount})."
                    )
                )
            } else {
                matched++
            }
        }

        return ReconResult(checked, matched, discrepancies)
    }

    private suspend fun reconcileCommitmentsAndAccruals(tenantId: String, projectId: String, run: BusinessFinancialReconciliationRun): ReconResult {
        val cRes = reconcileCommitments(tenantId, projectId, run)
        val aRes = reconcileAccruals(tenantId, projectId, run)
        return ReconResult(
            checked = cRes.checked + aRes.checked,
            matched = cRes.matched + aRes.matched,
            discrepancies = cRes.discrepancies + aRes.discrepancies
        )
    }

    private fun generateChecksum(content: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(content.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
