package com.sucharu.sucharupro.domain.service.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.*
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepository
import com.sucharu.sucharupro.domain.repository.businesscost.BusinessCostManagementRepository
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerService
import com.sucharu.sucharupro.domain.service.businessledger.PostBusinessAdjustmentCommand
import com.sucharu.sucharupro.domain.validation.businesscostcontrol.BusinessCostControlValidators
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class BusinessCostControlServiceImpl(
    private val repository: BusinessCostControlRepository,
    private val costManagementRepository: BusinessCostManagementRepository? = null,
    private val ledgerService: BusinessLedgerService? = null,
    private val payableRepository: VendorPayableRepository? = null,
    private val expenseRepository: BusinessExpenseRepository? = null,
    private val defaultTenantId: String = "TENANT-001"
) : BusinessCostControlService {

    private val commitmentLock = Mutex()

    // --- RBAC & SoD Helpers ---

    private fun checkInternalAccess(principal: AuthenticatedPrincipal): DomainResult<Unit> {
        val allowedRoles = setOf(
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.STAFF
        )
        if (principal.role !in allowedRoles) {
            return DomainResult.Error(
                message = "Access denied: Role '${principal.role}' is not authorized to access cost control operations."
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

    // --- Financial Periods Governance ---

    override suspend fun createFinancialPeriod(
        principal: AuthenticatedPrincipal,
        command: CreateFinancialPeriodCommand
    ): DomainResult<BusinessFinancialPeriod> {
        val access = checkManagerOrAdmin(principal, "create financial periods")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val valRes = BusinessCostControlValidators.validateFinancialPeriod(
            periodCode = command.periodCode,
            periodName = command.periodName,
            startDate = command.startDate,
            endDate = command.endDate,
            tenantId = tenantId,
            projectId = projectId
        )
        if (valRes is DomainResult.Error) return valRes

        val existing = repository.findFinancialPeriodByCode(command.periodCode, tenantId, projectId)
        if (existing != null) {
            return DomainResult.Error(message = "Financial period with code '${command.periodCode}' already exists.")
        }

        val period = BusinessFinancialPeriod(
            id = "PER-" + UUID.randomUUID().toString().take(12).uppercase(),
            tenantId = tenantId,
            projectId = projectId,
            periodCode = command.periodCode.trim().uppercase(),
            periodName = command.periodName.trim(),
            startDate = command.startDate,
            endDate = command.endDate,
            status = BusinessFinancialPeriodStatus.OPEN,
            createdBy = principal.userId,
            updatedBy = principal.userId
        )

        val saved = try {
            repository.createFinancialPeriod(period)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to create financial period.")
        }

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "FINANCIAL_PERIOD",
                entityId = saved.id,
                eventType = "PERIOD_CREATED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                newState = "code=${saved.periodCode}, status=${saved.status}",
                reason = "Created financial period ${saved.periodCode}"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun softCloseFinancialPeriod(
        principal: AuthenticatedPrincipal,
        periodId: String,
        reason: String?
    ): DomainResult<BusinessFinancialPeriod> {
        val access = checkManagerOrAdmin(principal, "soft-close financial periods")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val period = repository.findFinancialPeriodById(periodId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Financial period '$periodId' not found.")

        if (period.status.isClosed) {
            return DomainResult.Error(message = "Period '${period.periodCode}' is already CLOSED.")
        }

        val updated = period.copy(
            status = BusinessFinancialPeriodStatus.SOFT_CLOSED,
            closeReason = reason,
            updatedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateFinancialPeriod(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "FINANCIAL_PERIOD",
                entityId = saved.id,
                eventType = "PERIOD_SOFT_CLOSED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=${period.status}",
                newState = "status=${saved.status}",
                reason = reason ?: "Soft closed period"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun closeFinancialPeriod(
        principal: AuthenticatedPrincipal,
        periodId: String,
        reason: String
    ): DomainResult<BusinessFinancialPeriod> {
        val access = checkManagerOrAdmin(principal, "close financial periods")
        if (access is DomainResult.Error) return access

        if (reason.trim().length < 3) {
            return DomainResult.Error(message = "A mandatory closing reason (at least 3 characters) is required.")
        }

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val period = repository.findFinancialPeriodById(periodId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Financial period '$periodId' not found.")

        if (period.status.isClosed) {
            return DomainResult.Success(period)
        }

        val updated = period.copy(
            status = BusinessFinancialPeriodStatus.CLOSED,
            closedBy = principal.userId,
            closedAt = System.currentTimeMillis(),
            closeReason = reason.trim(),
            updatedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateFinancialPeriod(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "FINANCIAL_PERIOD",
                entityId = saved.id,
                eventType = "PERIOD_CLOSED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=${period.status}",
                newState = "status=${saved.status}, closedBy=${principal.userId}",
                reason = reason.trim()
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun reopenFinancialPeriod(
        principal: AuthenticatedPrincipal,
        periodId: String,
        reason: String
    ): DomainResult<BusinessFinancialPeriod> {
        val access = checkManagerOrAdmin(principal, "reopen financial periods")
        if (access is DomainResult.Error) return access

        if (principal.role != UserRole.ADMIN) {
            return DomainResult.Error(message = "Strict Governance: Only ADMIN can reopen a closed financial period.")
        }
        if (reason.trim().length < 5) {
            return DomainResult.Error(message = "A detailed administrative reason (at least 5 characters) is required to reopen a closed period.")
        }

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val period = repository.findFinancialPeriodById(periodId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Financial period '$periodId' not found.")

        val updated = period.copy(
            status = BusinessFinancialPeriodStatus.OPEN,
            closedBy = null,
            closedAt = null,
            closeReason = "Reopened by ${principal.userId}: $reason",
            updatedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateFinancialPeriod(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "FINANCIAL_PERIOD",
                entityId = saved.id,
                eventType = "PERIOD_REOPENED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=${period.status}",
                newState = "status=OPEN",
                reason = reason
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun getFinancialPeriodById(
        principal: AuthenticatedPrincipal,
        periodId: String
    ): DomainResult<BusinessFinancialPeriod> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val period = repository.findFinancialPeriodById(periodId, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Financial period '$periodId' not found.")
        return DomainResult.Success(period)
    }

    override suspend fun listFinancialPeriods(
        principal: AuthenticatedPrincipal,
        filter: BusinessFinancialPeriodFilter
    ): DomainResult<List<BusinessFinancialPeriod>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listFinancialPeriods(defaultTenantId, principal.projectId, filter)
        return DomainResult.Success(list)
    }

    // --- Business Cost Commitments ---

    override suspend fun createCommitment(
        principal: AuthenticatedPrincipal,
        command: CreateCostCommitmentCommand
    ): DomainResult<BusinessCostCommitment> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // Idempotency Check
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existingAudit = repository.listAuditEvents(tenantId, projectId, null, "COMMITMENT")
                .find { it.idempotencyKey == command.idempotencyKey }
            if (existingAudit != null) {
                val existing = repository.findCommitmentById(existingAudit.entityId, tenantId, projectId)
                if (existing != null) return DomainResult.Success(existing)
            }
        }

        val commitmentNumber = command.commitmentNumber?.trim()
            ?: ("CMT-" + UUID.randomUUID().toString().take(8).uppercase())

        val scaledAmount = command.committedAmount.setScale(4, RoundingMode.HALF_UP)
        val valRes = BusinessCostControlValidators.validateCommitment(
            commitmentNumber = commitmentNumber,
            committedAmount = scaledAmount,
            currency = command.currency,
            costCategoryId = command.costCategoryId,
            description = command.description,
            tenantId = tenantId,
            projectId = projectId,
            createdBy = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        // Validate Period if specified
        if (!command.periodId.isNullOrBlank()) {
            val period = repository.findFinancialPeriodById(command.periodId, tenantId, projectId)
            if (period != null && period.status.isClosed) {
                return DomainResult.Error(message = "Cannot assign commitment to closed financial period '${period.periodCode}'.")
            }
        }

        val id = "CMT-" + UUID.randomUUID().toString().take(12).uppercase()
        val commitment = BusinessCostCommitment(
            id = id,
            tenantId = tenantId,
            projectId = projectId,
            commitmentNumber = commitmentNumber,
            vendorId = command.vendorId,
            jobId = command.jobId,
            costCenterId = command.costCenterId,
            costCategoryId = command.costCategoryId,
            description = command.description.trim(),
            committedAmount = scaledAmount,
            consumedAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            remainingAmount = scaledAmount,
            currency = command.currency.trim().uppercase(),
            commitmentDate = command.commitmentDate,
            expectedDate = command.expectedDate,
            periodId = command.periodId,
            status = BusinessCostCommitmentStatus.DRAFT,
            sourceType = command.sourceType,
            sourceId = command.sourceId ?: id,
            createdBy = principal.userId
        )

        val saved = try {
            repository.createCommitment(commitment)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to create cost commitment.")
        }

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "COMMITMENT",
                entityId = saved.id,
                eventType = "COMMITMENT_CREATED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                amount = saved.committedAmount,
                currency = saved.currency,
                newState = "number=${saved.commitmentNumber}, status=${saved.status}, amount=${saved.committedAmount}",
                reason = "Created cost commitment ${saved.commitmentNumber}",
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun updateCommitment(
        principal: AuthenticatedPrincipal,
        command: UpdateCostCommitmentCommand
    ): DomainResult<BusinessCostCommitment> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCommitmentById(command.commitmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Commitment '${command.commitmentId}' not found.")

        if (existing.status != BusinessCostCommitmentStatus.DRAFT) {
            return DomainResult.Error(message = "Only DRAFT commitments can be edited. Current status is '${existing.status}'.")
        }

        val newCommittedAmount = command.committedAmount?.setScale(4, RoundingMode.HALF_UP) ?: existing.committedAmount
        val newCurrency = command.currency?.trim()?.uppercase() ?: existing.currency

        val valRes = BusinessCostControlValidators.validateCommitment(
            commitmentNumber = existing.commitmentNumber,
            committedAmount = newCommittedAmount,
            currency = newCurrency,
            costCategoryId = command.costCategoryId ?: existing.costCategoryId,
            description = command.description ?: existing.description,
            tenantId = tenantId,
            projectId = projectId,
            createdBy = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = existing.copy(
            vendorId = command.vendorId ?: existing.vendorId,
            jobId = command.jobId ?: existing.jobId,
            costCenterId = command.costCenterId ?: existing.costCenterId,
            costCategoryId = command.costCategoryId ?: existing.costCategoryId,
            description = command.description?.trim() ?: existing.description,
            committedAmount = newCommittedAmount,
            remainingAmount = (newCommittedAmount - existing.consumedAmount).setScale(4, RoundingMode.HALF_UP),
            currency = newCurrency,
            expectedDate = command.expectedDate ?: existing.expectedDate,
            periodId = command.periodId ?: existing.periodId,
            updatedAt = System.currentTimeMillis()
        )

        val saved = repository.updateCommitment(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "COMMITMENT",
                entityId = saved.id,
                eventType = "COMMITMENT_UPDATED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                amount = saved.committedAmount,
                currency = saved.currency,
                reason = "Updated commitment metadata"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun submitCommitment(
        principal: AuthenticatedPrincipal,
        commitmentId: String
    ): DomainResult<BusinessCostCommitment> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCommitmentById(commitmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Commitment '$commitmentId' not found.")

        if (existing.status != BusinessCostCommitmentStatus.DRAFT) {
            return DomainResult.Error(message = "Only DRAFT commitments can be submitted. Current status is '${existing.status}'.")
        }

        val updated = existing.copy(
            status = BusinessCostCommitmentStatus.SUBMITTED,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateCommitment(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "COMMITMENT",
                entityId = saved.id,
                eventType = "COMMITMENT_SUBMITTED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=DRAFT",
                newState = "status=SUBMITTED",
                reason = "Submitted commitment for approval"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun approveCommitment(
        principal: AuthenticatedPrincipal,
        commitmentId: String
    ): DomainResult<BusinessCostCommitment> {
        val access = checkManagerOrAdmin(principal, "approve commitments")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCommitmentById(commitmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Commitment '$commitmentId' not found.")

        if (existing.status != BusinessCostCommitmentStatus.SUBMITTED && existing.status != BusinessCostCommitmentStatus.DRAFT) {
            return DomainResult.Error(message = "Cannot approve commitment in status '${existing.status}'.")
        }

        // Separation of Duties: Creator cannot approve their own commitment
        if (existing.createdBy == principal.userId && principal.role != UserRole.ADMIN) {
            return DomainResult.Error(message = "Separation of Duties violation: The creator cannot approve their own commitment.")
        }

        val updated = existing.copy(
            status = BusinessCostCommitmentStatus.APPROVED,
            approvedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateCommitment(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "COMMITMENT",
                entityId = saved.id,
                eventType = "COMMITMENT_APPROVED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=${existing.status}",
                newState = "status=APPROVED, approvedBy=${principal.userId}",
                reason = "Approved cost commitment"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun activateCommitment(
        principal: AuthenticatedPrincipal,
        commitmentId: String
    ): DomainResult<BusinessCostCommitment> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCommitmentById(commitmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Commitment '$commitmentId' not found.")

        if (existing.status != BusinessCostCommitmentStatus.APPROVED) {
            return DomainResult.Error(message = "Only APPROVED commitments can be activated. Current status is '${existing.status}'.")
        }

        val updated = existing.copy(
            status = BusinessCostCommitmentStatus.ACTIVE,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateCommitment(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "COMMITMENT",
                entityId = saved.id,
                eventType = "COMMITMENT_ACTIVATED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=APPROVED",
                newState = "status=ACTIVE",
                reason = "Activated cost commitment"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun consumeCommitment(
        principal: AuthenticatedPrincipal,
        command: ConsumeCostCommitmentCommand
    ): DomainResult<BusinessCostCommitmentConsumption> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // Idempotency check
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existingAudit = repository.listAuditEvents(tenantId, projectId, command.commitmentId, "COMMITMENT")
                .find { it.idempotencyKey == command.idempotencyKey && it.eventType == "COMMITMENT_CONSUMED" }
            if (existingAudit != null) {
                val consumptions = repository.listConsumptions(tenantId, projectId, command.commitmentId)
                val match = consumptions.find { it.idempotencyKey == command.idempotencyKey }
                if (match != null) return DomainResult.Success(match)
            }
        }

        return commitmentLock.withLock {
            val commitment = repository.findCommitmentById(command.commitmentId, tenantId, projectId)
                ?: return@withLock DomainResult.Error(message = "Commitment '${command.commitmentId}' not found.")

            val scaledAmount = command.amount.setScale(4, RoundingMode.HALF_UP)
            val valRes = BusinessCostControlValidators.validateConsumption(
                commitment = commitment,
                consumptionAmount = scaledAmount,
                currency = command.currency,
                actorId = principal.userId
            )
            if (valRes is DomainResult.Error) return@withLock valRes

            val newConsumed = (commitment.consumedAmount + scaledAmount).setScale(4, RoundingMode.HALF_UP)
            val newRemaining = (commitment.committedAmount - newConsumed).setScale(4, RoundingMode.HALF_UP)
            val newStatus = if (newRemaining <= BigDecimal.ZERO) {
                BusinessCostCommitmentStatus.FULLY_CONSUMED
            } else {
                BusinessCostCommitmentStatus.PARTIALLY_CONSUMED
            }

            val updatedCommitment = commitment.copy(
                consumedAmount = newConsumed,
                remainingAmount = newRemaining,
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateCommitment(updatedCommitment)

            val consumption = BusinessCostCommitmentConsumption(
                id = "CON-" + UUID.randomUUID().toString().take(12).uppercase(),
                commitmentId = commitment.id,
                tenantId = tenantId,
                projectId = projectId,
                sourceType = command.sourceType,
                sourceId = command.sourceId,
                amount = scaledAmount,
                currency = command.currency.trim().uppercase(),
                consumedAt = System.currentTimeMillis(),
                createdBy = principal.userId,
                idempotencyKey = command.idempotencyKey,
                notes = command.notes
            )
            val savedConsumption = repository.recordConsumption(consumption)

            repository.recordAuditEvent(
                BusinessCostControlAuditEvent(
                    id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                    tenantId = tenantId,
                    projectId = projectId,
                    entityType = "COMMITMENT",
                    entityId = commitment.id,
                    eventType = "COMMITMENT_CONSUMED",
                    actorUserId = principal.userId,
                    actorRole = principal.role.name,
                    amount = scaledAmount,
                    currency = consumption.currency,
                    previousState = "consumed=${commitment.consumedAmount}, remaining=${commitment.remainingAmount}",
                    newState = "consumed=$newConsumed, remaining=$newRemaining, status=$newStatus",
                    reason = "Consumed commitment via ${command.sourceType} ${command.sourceId}",
                    correlationId = command.correlationId,
                    idempotencyKey = command.idempotencyKey
                )
            )

            DomainResult.Success(savedConsumption)
        }
    }

    override suspend fun cancelCommitment(
        principal: AuthenticatedPrincipal,
        commitmentId: String,
        reason: String
    ): DomainResult<BusinessCostCommitment> {
        val access = checkManagerOrAdmin(principal, "cancel commitments")
        if (access is DomainResult.Error) return access

        if (reason.trim().length < 3) {
            return DomainResult.Error(message = "A mandatory cancellation reason (at least 3 characters) is required.")
        }

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCommitmentById(commitmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Commitment '$commitmentId' not found.")

        if (existing.status.isTerminal) {
            return DomainResult.Error(message = "Cannot cancel commitment in terminal status '${existing.status}'.")
        }

        if (existing.consumedAmount > BigDecimal.ZERO) {
            return DomainResult.Error(message = "Cannot cancel partially consumed commitment. Please use closeCommitment instead.")
        }

        val updated = existing.copy(
            status = BusinessCostCommitmentStatus.CANCELLED,
            remainingAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateCommitment(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "COMMITMENT",
                entityId = saved.id,
                eventType = "COMMITMENT_CANCELLED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=${existing.status}",
                newState = "status=CANCELLED",
                reason = reason.trim()
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun closeCommitment(
        principal: AuthenticatedPrincipal,
        commitmentId: String,
        reason: String
    ): DomainResult<BusinessCostCommitment> {
        val access = checkManagerOrAdmin(principal, "close commitments")
        if (access is DomainResult.Error) return access

        if (reason.trim().length < 3) {
            return DomainResult.Error(message = "A mandatory closure reason (at least 3 characters) is required.")
        }

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCommitmentById(commitmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Commitment '$commitmentId' not found.")

        if (existing.status.isTerminal) {
            return DomainResult.Success(existing)
        }

        val updated = existing.copy(
            status = BusinessCostCommitmentStatus.CLOSED,
            remainingAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateCommitment(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "COMMITMENT",
                entityId = saved.id,
                eventType = "COMMITMENT_CLOSED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=${existing.status}",
                newState = "status=CLOSED",
                reason = reason.trim()
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun getCommitmentById(
        principal: AuthenticatedPrincipal,
        commitmentId: String
    ): DomainResult<BusinessCostCommitment> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val commitment = repository.findCommitmentById(commitmentId, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Commitment '$commitmentId' not found.")
        return DomainResult.Success(commitment)
    }

    override suspend fun listCommitments(
        principal: AuthenticatedPrincipal,
        filter: BusinessCostCommitmentFilter
    ): DomainResult<List<BusinessCostCommitment>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listCommitments(defaultTenantId, principal.projectId, filter)
        return DomainResult.Success(list)
    }

    override suspend fun listConsumptions(
        principal: AuthenticatedPrincipal,
        commitmentId: String
    ): DomainResult<List<BusinessCostCommitmentConsumption>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listConsumptions(defaultTenantId, principal.projectId, commitmentId)
        return DomainResult.Success(list)
    }

    // --- Business Cost Accruals ---

    override suspend fun createAccrual(
        principal: AuthenticatedPrincipal,
        command: CreateCostAccrualCommand
    ): DomainResult<BusinessCostAccrual> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // Idempotency Check
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existingAudit = repository.listAuditEvents(tenantId, projectId, null, "ACCRUAL")
                .find { it.idempotencyKey == command.idempotencyKey }
            if (existingAudit != null) {
                val existing = repository.findAccrualById(existingAudit.entityId, tenantId, projectId)
                if (existing != null) return DomainResult.Success(existing)
            }
        }

        val accountingPeriod = repository.findFinancialPeriodById(command.accountingPeriodId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Accounting period '${command.accountingPeriodId}' not found.")

        val scaledAmount = command.accrualAmount.setScale(4, RoundingMode.HALF_UP)
        val accrualNumber = command.accrualNumber?.trim()
            ?: ("ACR-" + UUID.randomUUID().toString().take(8).uppercase())

        val valRes = BusinessCostControlValidators.validateAccrual(
            accrualNumber = accrualNumber,
            accrualAmount = scaledAmount,
            currency = command.currency,
            accountingPeriod = accountingPeriod,
            costCategoryId = command.costCategoryId,
            description = command.description,
            tenantId = tenantId,
            projectId = projectId,
            createdBy = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        val id = "ACR-" + UUID.randomUUID().toString().take(12).uppercase()
        val accrual = BusinessCostAccrual(
            id = id,
            tenantId = tenantId,
            projectId = projectId,
            accrualNumber = accrualNumber,
            vendorId = command.vendorId,
            jobId = command.jobId,
            costCenterId = command.costCenterId,
            costCategoryId = command.costCategoryId,
            description = command.description.trim(),
            accrualAmount = scaledAmount,
            reversedAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            currency = command.currency.trim().uppercase(),
            accountingPeriodId = command.accountingPeriodId,
            accrualDate = command.accrualDate,
            sourceCommitmentId = command.sourceCommitmentId,
            sourceType = command.sourceType,
            sourceId = command.sourceId ?: id,
            status = BusinessCostAccrualStatus.DRAFT,
            createdBy = principal.userId
        )

        val saved = try {
            repository.createAccrual(accrual)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to create cost accrual.")
        }

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ACCRUAL",
                entityId = saved.id,
                eventType = "ACCRUAL_CREATED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                amount = saved.accrualAmount,
                currency = saved.currency,
                newState = "number=${saved.accrualNumber}, amount=${saved.accrualAmount}, period=${saved.accountingPeriodId}",
                reason = "Created cost accrual ${saved.accrualNumber}",
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun reviewAccrual(
        principal: AuthenticatedPrincipal,
        accrualId: String
    ): DomainResult<BusinessCostAccrual> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findAccrualById(accrualId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Accrual '$accrualId' not found.")

        if (existing.status != BusinessCostAccrualStatus.DRAFT) {
            return DomainResult.Error(message = "Only DRAFT accruals can be reviewed. Current status is '${existing.status}'.")
        }

        val updated = existing.copy(
            status = BusinessCostAccrualStatus.REVIEWED,
            reviewedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateAccrual(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ACCRUAL",
                entityId = saved.id,
                eventType = "ACCRUAL_REVIEWED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=DRAFT",
                newState = "status=REVIEWED, reviewedBy=${principal.userId}",
                reason = "Reviewed cost accrual"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun approveAccrual(
        principal: AuthenticatedPrincipal,
        accrualId: String
    ): DomainResult<BusinessCostAccrual> {
        val access = checkManagerOrAdmin(principal, "approve accruals")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findAccrualById(accrualId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Accrual '$accrualId' not found.")

        if (existing.status != BusinessCostAccrualStatus.REVIEWED && existing.status != BusinessCostAccrualStatus.DRAFT) {
            return DomainResult.Error(message = "Cannot approve accrual in status '${existing.status}'.")
        }

        // SoD
        if (existing.createdBy == principal.userId && principal.role != UserRole.ADMIN) {
            return DomainResult.Error(message = "Separation of Duties violation: The creator cannot approve their own accrual.")
        }

        val updated = existing.copy(
            status = BusinessCostAccrualStatus.APPROVED,
            approvedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateAccrual(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ACCRUAL",
                entityId = saved.id,
                eventType = "ACCRUAL_APPROVED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=${existing.status}",
                newState = "status=APPROVED, approvedBy=${principal.userId}",
                reason = "Approved cost accrual"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun postAccrual(
        principal: AuthenticatedPrincipal,
        accrualId: String,
        correlationId: String?,
        idempotencyKey: String?
    ): DomainResult<BusinessCostAccrual> {
        val access = checkManagerOrAdmin(principal, "post accruals to business ledger")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findAccrualById(accrualId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Accrual '$accrualId' not found.")

        if (existing.status == BusinessCostAccrualStatus.POSTED) {
            return DomainResult.Success(existing)
        }

        if (!existing.status.canBePosted) {
            return DomainResult.Error(message = "Only APPROVED accruals can be posted to the business ledger. Current status is '${existing.status}'.")
        }

        val period = repository.findFinancialPeriodById(existing.accountingPeriodId, tenantId, projectId)
        if (period != null && period.status.isClosed) {
            return DomainResult.Error(message = "Cannot post accrual into closed accounting period '${period.periodCode}'.")
        }

        // Post to canonical BusinessLedgerService
        var ledgerPostingId = existing.ledgerPostingId
        if (ledgerPostingId.isNullOrBlank() && ledgerService != null) {
            val postCmd = PostBusinessAdjustmentCommand(
                amount = existing.accrualAmount,
                isDebit = true,
                accountCategory = BusinessLedgerAccountCategory.PRODUCTION_COST,
                description = "Period Cost Accrual: ${existing.description}",
                reference = existing.accrualNumber,
                jobId = existing.jobId,
                vendorId = existing.vendorId,
                currency = existing.currency,
                idempotencyKey = idempotencyKey ?: ("IDEM-ACR-POST-" + existing.id),
                correlationId = correlationId
            )
            when (val ledgerRes = ledgerService.postBusinessAdjustment(principal, postCmd)) {
                is DomainResult.Success -> {
                    ledgerPostingId = ledgerRes.data.id
                }
                is DomainResult.Error -> {
                    return DomainResult.Error(message = "Canonical Ledger posting failed: ${ledgerRes.message}")
                }
                DomainResult.Loading -> {}
            }
        } else if (ledgerPostingId.isNullOrBlank()) {
            ledgerPostingId = "BLP-ACR-" + UUID.randomUUID().toString().take(10).uppercase()
        }

        val updated = existing.copy(
            status = BusinessCostAccrualStatus.POSTED,
            ledgerPostingId = ledgerPostingId,
            postedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateAccrual(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ACCRUAL",
                entityId = saved.id,
                eventType = "ACCRUAL_POSTED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                amount = saved.accrualAmount,
                currency = saved.currency,
                previousState = "status=APPROVED",
                newState = "status=POSTED, ledgerPostingId=$ledgerPostingId, postedBy=${principal.userId}",
                reason = "Posted accrual to canonical business ledger",
                correlationId = correlationId,
                idempotencyKey = idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun reverseAccrual(
        principal: AuthenticatedPrincipal,
        command: ReverseCostAccrualCommand
    ): DomainResult<BusinessCostAccrualReversal> {
        val access = checkManagerOrAdmin(principal, "reverse accruals")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val accrual = repository.findAccrualById(command.accrualId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Accrual '${command.accrualId}' not found.")

        val targetPeriodId = command.accountingPeriodId ?: accrual.accountingPeriodId
        val period = repository.findFinancialPeriodById(targetPeriodId, tenantId, projectId)

        val scaledReversal = command.reversalAmount.setScale(4, RoundingMode.HALF_UP)
        val valRes = BusinessCostControlValidators.validateReversal(
            accrual = accrual,
            reversalAmount = scaledReversal,
            currency = command.currency,
            reason = command.reason,
            accountingPeriod = period,
            actorId = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        // Post compensating reversal entry to BusinessLedgerService
        var reversalPostingId: String? = null
        if (ledgerService != null) {
            val revCmd = PostBusinessAdjustmentCommand(
                amount = scaledReversal,
                isDebit = false, // Compensating credit
                accountCategory = BusinessLedgerAccountCategory.PRODUCTION_COST,
                description = "Accrual Reversal: ${command.reason}",
                reference = "REV-" + accrual.accrualNumber,
                jobId = accrual.jobId,
                vendorId = accrual.vendorId,
                currency = command.currency,
                idempotencyKey = command.idempotencyKey ?: ("IDEM-ACR-REV-" + accrual.id + "-" + UUID.randomUUID().toString().take(8)),
                correlationId = command.correlationId
            )
            when (val revRes = ledgerService.postBusinessAdjustment(principal, revCmd)) {
                is DomainResult.Success -> {
                    reversalPostingId = revRes.data.id
                }
                is DomainResult.Error -> {
                    return DomainResult.Error(message = "Compensating Ledger reversal failed: ${revRes.message}")
                }
                DomainResult.Loading -> {}
            }
        }
        if (reversalPostingId.isNullOrBlank()) {
            reversalPostingId = "BLP-REV-" + UUID.randomUUID().toString().take(10).uppercase()
        }

        val newReversedAmount = (accrual.reversedAmount + scaledReversal).setScale(4, RoundingMode.HALF_UP)
        val isFullyReversed = newReversedAmount >= accrual.accrualAmount
        val newStatus = if (isFullyReversed) BusinessCostAccrualStatus.REVERSED else BusinessCostAccrualStatus.POSTED

        val updatedAccrual = accrual.copy(
            reversedAmount = newReversedAmount,
            status = newStatus,
            reversalPostingId = reversalPostingId,
            reversedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateAccrual(updatedAccrual)

        val reversal = BusinessCostAccrualReversal(
            id = "REV-" + UUID.randomUUID().toString().take(12).uppercase(),
            tenantId = tenantId,
            projectId = projectId,
            accrualId = accrual.id,
            reversalAmount = scaledReversal,
            currency = command.currency.trim().uppercase(),
            reversalDate = System.currentTimeMillis(),
            accountingPeriodId = targetPeriodId,
            reason = command.reason.trim(),
            ledgerPostingId = reversalPostingId,
            createdBy = principal.userId,
            idempotencyKey = command.idempotencyKey
        )
        val savedReversal = repository.recordReversal(reversal)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ACCRUAL",
                entityId = accrual.id,
                eventType = "ACCRUAL_REVERSED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                amount = scaledReversal,
                currency = reversal.currency,
                previousState = "reversed=${accrual.reversedAmount}",
                newState = "reversed=$newReversedAmount, status=$newStatus, reversalPostingId=$reversalPostingId",
                reason = command.reason.trim(),
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(savedReversal)
    }

    override suspend fun cancelAccrual(
        principal: AuthenticatedPrincipal,
        accrualId: String,
        reason: String
    ): DomainResult<BusinessCostAccrual> {
        val access = checkManagerOrAdmin(principal, "cancel accruals")
        if (access is DomainResult.Error) return access

        if (reason.trim().length < 3) {
            return DomainResult.Error(message = "A mandatory cancellation reason (at least 3 characters) is required.")
        }

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findAccrualById(accrualId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Accrual '$accrualId' not found.")

        if (existing.status == BusinessCostAccrualStatus.POSTED) {
            return DomainResult.Error(message = "Posted accruals cannot be cancelled directly. Please use reverseAccrual.")
        }

        val updated = existing.copy(
            status = BusinessCostAccrualStatus.CANCELLED,
            updatedAt = System.currentTimeMillis()
        )
        val saved = repository.updateAccrual(updated)

        repository.recordAuditEvent(
            BusinessCostControlAuditEvent(
                id = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ACCRUAL",
                entityId = saved.id,
                eventType = "ACCRUAL_CANCELLED",
                actorUserId = principal.userId,
                actorRole = principal.role.name,
                previousState = "status=${existing.status}",
                newState = "status=CANCELLED",
                reason = reason.trim()
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun getAccrualById(
        principal: AuthenticatedPrincipal,
        accrualId: String
    ): DomainResult<BusinessCostAccrual> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val accrual = repository.findAccrualById(accrualId, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Accrual '$accrualId' not found.")
        return DomainResult.Success(accrual)
    }

    override suspend fun listAccruals(
        principal: AuthenticatedPrincipal,
        filter: BusinessCostAccrualFilter
    ): DomainResult<List<BusinessCostAccrual>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listAccruals(defaultTenantId, principal.projectId, filter)
        return DomainResult.Success(list)
    }

    override suspend fun listReversals(
        principal: AuthenticatedPrincipal,
        accrualId: String
    ): DomainResult<List<BusinessCostAccrualReversal>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listReversals(defaultTenantId, principal.projectId, accrualId)
        return DomainResult.Success(list)
    }

    // --- Control Analytics, Dashboards & Reconciliation ---

    override suspend fun getControlDashboard(principal: AuthenticatedPrincipal): DomainResult<BusinessCostControlDashboard> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val commitments = repository.listCommitments(tenantId, projectId, BusinessCostCommitmentFilter())
        val accruals = repository.listAccruals(tenantId, projectId, BusinessCostAccrualFilter())

        var totalCommitments = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var activeCommitments = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var consumedCommitments = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var remainingCommitments = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var activeCount = 0

        commitments.forEach { c ->
            totalCommitments = totalCommitments.add(c.committedAmount)
            consumedCommitments = consumedCommitments.add(c.consumedAmount)
            if (c.status == BusinessCostCommitmentStatus.ACTIVE || c.status == BusinessCostCommitmentStatus.PARTIALLY_CONSUMED || c.status == BusinessCostCommitmentStatus.APPROVED) {
                activeCommitments = activeCommitments.add(c.committedAmount)
                remainingCommitments = remainingCommitments.add(c.remainingAmount)
                activeCount++
            }
        }

        var accruedCosts = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var pendingAccrualCount = 0
        accruals.forEach { a ->
            if (a.status == BusinessCostAccrualStatus.POSTED) {
                accruedCosts = accruedCosts.add(a.netAccrualAmount)
            } else if (a.status == BusinessCostAccrualStatus.DRAFT || a.status == BusinessCostAccrualStatus.REVIEWED || a.status == BusinessCostAccrualStatus.APPROVED) {
                pendingAccrualCount++
            }
        }

        val exceptionsRes = listExceptions(principal)
        val exceptions = if (exceptionsRes is DomainResult.Success) exceptionsRes.data else emptyList()

        val dashboard = BusinessCostControlDashboard(
            totalCommitments = totalCommitments,
            activeCommitments = activeCommitments,
            consumedCommitments = consumedCommitments,
            remainingCommitments = remainingCommitments,
            accruedCosts = accruedCosts,
            unbilledLiabilities = accruedCosts, // Incurred unbilled liability
            vendorPayables = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            unreconciledAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            totalCommitmentCount = commitments.size,
            activeCommitmentCount = activeCount,
            pendingAccrualCount = pendingAccrualCount,
            exceptionCount = exceptions.size,
            currency = "BDT"
        )

        return DomainResult.Success(dashboard)
    }

    override suspend fun getReconciliationSummary(
        principal: AuthenticatedPrincipal,
        vendorId: String?,
        jobId: String?
    ): DomainResult<BusinessCostReconciliationSummary> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val commitFilter = BusinessCostCommitmentFilter(vendorId = vendorId, jobId = jobId)
        val commitments = repository.listCommitments(tenantId, projectId, commitFilter)

        val accrualFilter = BusinessCostAccrualFilter(vendorId = vendorId, jobId = jobId)
        val accruals = repository.listAccruals(tenantId, projectId, accrualFilter)

        var commitmentAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var consumedAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var remainingCommitment = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        commitments.forEach { c ->
            commitmentAmount = commitmentAmount.add(c.committedAmount)
            consumedAmount = consumedAmount.add(c.consumedAmount)
            remainingCommitment = remainingCommitment.add(c.remainingAmount)
        }

        var accruedAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        accruals.forEach { a ->
            if (a.status == BusinessCostAccrualStatus.POSTED) {
                accruedAmount = accruedAmount.add(a.netAccrualAmount)
            }
        }

        val unbilledAmount = accruedAmount
        val unreconciledAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        val exceptionsRes = listExceptions(principal)
        val allExceptions = if (exceptionsRes is DomainResult.Success) exceptionsRes.data else emptyList()

        val summary = BusinessCostReconciliationSummary(
            commitmentAmount = commitmentAmount,
            consumedAmount = consumedAmount,
            accruedAmount = accruedAmount,
            payableAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            paidAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            remainingCommitment = remainingCommitment,
            unbilledAmount = unbilledAmount,
            unreconciledAmount = unreconciledAmount,
            exceptions = allExceptions,
            currency = "BDT"
        )

        return DomainResult.Success(summary)
    }

    override suspend fun listExceptions(principal: AuthenticatedPrincipal): DomainResult<List<BusinessCostControlException>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val exceptions = mutableListOf<BusinessCostControlException>()

        val commitments = repository.listCommitments(tenantId, projectId, BusinessCostCommitmentFilter())
        val accruals = repository.listAccruals(tenantId, projectId, BusinessCostAccrualFilter())

        // 1. Check for Over-committed or expired commitments
        commitments.forEach { c ->
            if (c.consumedAmount > c.committedAmount) {
                exceptions.add(
                    BusinessCostControlException(
                        exceptionType = BusinessCostControlExceptionType.OVER_COMMITTED,
                        description = "Commitment ${c.commitmentNumber} has consumed amount (${c.consumedAmount}) exceeding committed amount (${c.committedAmount}).",
                        severity = BusinessCostControlSeverity.CRITICAL,
                        sourceEntityId = c.id,
                        amount = c.consumedAmount - c.committedAmount,
                        currency = c.currency
                    )
                )
            }
        }

        // 2. Check for Accruals without source commitments or unbilled liabilities
        accruals.forEach { a ->
            if (a.status == BusinessCostAccrualStatus.POSTED && a.sourceCommitmentId.isNullOrBlank()) {
                exceptions.add(
                    BusinessCostControlException(
                        exceptionType = BusinessCostControlExceptionType.ACCRUAL_WITHOUT_PAYABLE,
                        description = "Posted accrual ${a.accrualNumber} for ${a.accrualAmount} ${a.currency} has no linked purchase commitment.",
                        severity = BusinessCostControlSeverity.INFO,
                        sourceEntityId = a.id,
                        amount = a.netAccrualAmount,
                        currency = a.currency
                    )
                )
            }
        }

        return DomainResult.Success(exceptions)
    }

    override suspend fun getPeriodEndReport(
        principal: AuthenticatedPrincipal,
        periodId: String
    ): DomainResult<BusinessCostPeriodEndReport> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val period = repository.findFinancialPeriodById(periodId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Financial period '$periodId' not found.")

        val commitments = repository.listCommitments(tenantId, projectId, BusinessCostCommitmentFilter(periodId = period.id))
        val accruals = repository.listAccruals(tenantId, projectId, BusinessCostAccrualFilter(accountingPeriodId = period.id))

        var openCommitmentsAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        commitments.forEach { c ->
            if (!c.status.isTerminal) {
                openCommitmentsAmount = openCommitmentsAmount.add(c.remainingAmount)
            }
        }

        var pendingAccrualsCount = 0
        var pendingAccrualsAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var postedAccrualsAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        accruals.forEach { a ->
            if (a.status == BusinessCostAccrualStatus.POSTED) {
                postedAccrualsAmount = postedAccrualsAmount.add(a.netAccrualAmount)
            } else if (!a.status.isReversed && a.status != BusinessCostAccrualStatus.CANCELLED) {
                pendingAccrualsCount++
                pendingAccrualsAmount = pendingAccrualsAmount.add(a.accrualAmount)
            }
        }

        val exceptionsRes = listExceptions(principal)
        val exceptions = if (exceptionsRes is DomainResult.Success) exceptionsRes.data else emptyList()

        val warnings = mutableListOf<String>()
        if (pendingAccrualsCount > 0) {
            warnings.add("$pendingAccrualsCount unposted/pending accruals totaling $pendingAccrualsAmount remain in this period.")
        }
        if (openCommitmentsAmount > BigDecimal.ZERO) {
            warnings.add("Active open commitments totaling $openCommitmentsAmount are linked to this period.")
        }

        val isReadyForClosure = pendingAccrualsCount == 0

        val report = BusinessCostPeriodEndReport(
            period = period,
            openCommitmentsAmount = openCommitmentsAmount,
            pendingAccrualsCount = pendingAccrualsCount,
            pendingAccrualsAmount = pendingAccrualsAmount,
            postedAccrualsAmount = postedAccrualsAmount,
            unbilledLiabilitiesAmount = postedAccrualsAmount,
            unreconciledPayablesAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            exceptionsCount = exceptions.size,
            isReadyForClosure = isReadyForClosure,
            warnings = warnings
        )

        return DomainResult.Success(report)
    }

    override suspend fun listAuditEvents(
        principal: AuthenticatedPrincipal,
        entityId: String?,
        entityType: String?
    ): DomainResult<List<BusinessCostControlAuditEvent>> {
        val access = checkInternalAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listAuditEvents(defaultTenantId, principal.projectId, entityId, entityType)
        return DomainResult.Success(list)
    }
}
