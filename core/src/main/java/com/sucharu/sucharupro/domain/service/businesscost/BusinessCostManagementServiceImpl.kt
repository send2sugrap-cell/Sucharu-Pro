package com.sucharu.sucharupro.domain.service.businesscost

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostTrackingFilter
import com.sucharu.sucharupro.domain.model.businesscost.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import com.sucharu.sucharupro.domain.repository.businesscost.BusinessCostManagementRepository
import com.sucharu.sucharupro.domain.repository.businessledger.BusinessLedgerRepository
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import com.sucharu.sucharupro.domain.validation.businesscost.BusinessCostValidators
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Production implementation of BusinessCostManagementService.
 * Coordinates operational cost tracking, cost center/category governance, and tamper-evident reclassifications.
 */
class BusinessCostManagementServiceImpl(
    private val repository: BusinessCostManagementRepository,
    private val expenseRepository: BusinessExpenseRepository? = null,
    private val payableRepository: VendorPayableRepository? = null,
    private val ledgerRepository: BusinessLedgerRepository? = null,
    private val defaultTenantId: String = "TENANT-001"
) : BusinessCostManagementService {

    // --- RBAC Helpers ---

    private fun checkInternalStaffAccess(principal: AuthenticatedPrincipal): DomainResult<Unit> {
        val allowedRoles = setOf(
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.STAFF
        )
        if (principal.role !in allowedRoles) {
            return DomainResult.Error(
                message = "Access denied: Role '${principal.role}' is not authorized to access internal business cost management."
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

    // --- Cost Center Governance ---

    override suspend fun createCostCenter(
        principal: AuthenticatedPrincipal,
        command: CreateCostCenterCommand
    ): DomainResult<BusinessCostCenter> {
        val access = checkManagerOrAdmin(principal, "create cost centers")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val valRes = BusinessCostValidators.validateCostCenter(
            code = command.code,
            name = command.name,
            tenantId = tenantId,
            projectId = projectId,
            parentCostCenterId = command.parentCostCenterId
        )
        if (valRes is DomainResult.Error) return valRes

        val existing = repository.findCostCenterByCode(command.code, tenantId, projectId)
        if (existing != null) {
            return DomainResult.Error(message = "Cost center with code '${command.code}' already exists.")
        }

        if (command.parentCostCenterId != null) {
            val parent = repository.findCostCenterById(command.parentCostCenterId, tenantId, projectId)
                ?: return DomainResult.Error(message = "Parent cost center '${command.parentCostCenterId}' not found.")
            if (!parent.isActive) {
                return DomainResult.Error(message = "Cannot set inactive cost center '${parent.name}' as parent.")
            }
        }

        val id = "CC-" + UUID.randomUUID().toString().take(12).uppercase()
        val center = BusinessCostCenter(
            id = id,
            tenantId = tenantId,
            projectId = projectId,
            code = command.code.trim().uppercase(),
            name = command.name.trim(),
            description = command.description?.trim(),
            parentCostCenterId = command.parentCostCenterId,
            isActive = true,
            createdBy = principal.userId,
            updatedBy = principal.userId
        )

        val saved = try {
            repository.createCostCenter(center)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to create cost center.")
        }

        repository.recordAuditEvent(
            BusinessCostClassificationAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                trackingId = saved.id,
                action = "CREATE_COST_CENTER",
                actorId = principal.userId,
                actorRole = principal.role.name,
                newStateJson = "code=${saved.code}, name=${saved.name}",
                reason = "Created cost center ${saved.code}"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun getCostCenterById(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessCostCenter> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val center = repository.findCostCenterById(id, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Cost center '$id' not found.")
        return DomainResult.Success(center)
    }

    override suspend fun updateCostCenter(
        principal: AuthenticatedPrincipal,
        id: String,
        command: UpdateCostCenterCommand
    ): DomainResult<BusinessCostCenter> {
        val access = checkManagerOrAdmin(principal, "update cost centers")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCostCenterById(id, tenantId, projectId)
            ?: return DomainResult.Error(message = "Cost center '$id' not found.")

        val valRes = BusinessCostValidators.validateCostCenter(
            code = existing.code,
            name = command.name,
            tenantId = tenantId,
            projectId = projectId,
            costCenterId = id,
            parentCostCenterId = command.parentCostCenterId
        )
        if (valRes is DomainResult.Error) return valRes

        if (command.parentCostCenterId != null) {
            val parent = repository.findCostCenterById(command.parentCostCenterId, tenantId, projectId)
                ?: return DomainResult.Error(message = "Parent cost center '${command.parentCostCenterId}' not found.")
            if (parent.id == id) {
                return DomainResult.Error(message = "A cost center cannot be its own parent.")
            }
        }

        val updated = existing.copy(
            name = command.name.trim(),
            description = command.description?.trim(),
            parentCostCenterId = command.parentCostCenterId,
            isActive = command.isActive,
            updatedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )

        val saved = try {
            repository.updateCostCenter(updated)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to update cost center.")
        }

        repository.recordAuditEvent(
            BusinessCostClassificationAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                trackingId = saved.id,
                action = "UPDATE_COST_CENTER",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStateJson = "name=${existing.name}, active=${existing.isActive}",
                newStateJson = "name=${saved.name}, active=${saved.isActive}",
                reason = "Updated cost center metadata"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun activateCostCenter(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessCostCenter> {
        val access = checkManagerOrAdmin(principal, "activate cost centers")
        if (access is DomainResult.Error) return access

        val existing = repository.findCostCenterById(id, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Cost center '$id' not found.")
        if (existing.isActive) return DomainResult.Success(existing)

        val updated = repository.updateCostCenter(existing.copy(isActive = true, updatedBy = principal.userId))
        return DomainResult.Success(updated)
    }

    override suspend fun deactivateCostCenter(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessCostCenter> {
        val access = checkManagerOrAdmin(principal, "deactivate cost centers")
        if (access is DomainResult.Error) return access

        val existing = repository.findCostCenterById(id, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Cost center '$id' not found.")
        if (!existing.isActive) return DomainResult.Success(existing)

        val updated = repository.updateCostCenter(existing.copy(isActive = false, updatedBy = principal.userId))
        return DomainResult.Success(updated)
    }

    override suspend fun listCostCenters(
        principal: AuthenticatedPrincipal,
        activeOnly: Boolean?
    ): DomainResult<List<BusinessCostCenter>> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listCostCenters(defaultTenantId, principal.projectId, activeOnly)
        return DomainResult.Success(list)
    }

    override suspend fun getCostCenterHierarchy(
        principal: AuthenticatedPrincipal,
        parentCostCenterId: String
    ): DomainResult<List<BusinessCostCenter>> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.getCostCenterChildren(parentCostCenterId, defaultTenantId, principal.projectId)
        return DomainResult.Success(list)
    }

    // --- Cost Category Governance ---

    override suspend fun createCostCategory(
        principal: AuthenticatedPrincipal,
        command: CreateCostCategoryCommand
    ): DomainResult<BusinessCostCategory> {
        val access = checkManagerOrAdmin(principal, "create cost categories")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val valRes = BusinessCostValidators.validateCostCategory(
            code = command.code,
            name = command.name,
            tenantId = tenantId,
            projectId = projectId,
            parentCategoryId = command.parentCategoryId
        )
        if (valRes is DomainResult.Error) return valRes

        val existing = repository.findCostCategoryByCode(command.code, tenantId, projectId)
        if (existing != null) {
            return DomainResult.Error(message = "Cost category with code '${command.code}' already exists.")
        }

        if (command.parentCategoryId != null) {
            val parent = repository.findCostCategoryById(command.parentCategoryId, tenantId, projectId)
                ?: return DomainResult.Error(message = "Parent cost category '${command.parentCategoryId}' not found.")
            if (!parent.isActive) {
                return DomainResult.Error(message = "Cannot set inactive cost category '${parent.name}' as parent.")
            }
        }

        val id = "CAT-" + UUID.randomUUID().toString().take(12).uppercase()
        val category = BusinessCostCategory(
            id = id,
            tenantId = tenantId,
            projectId = projectId,
            code = command.code.trim().uppercase(),
            name = command.name.trim(),
            description = command.description?.trim(),
            parentCategoryId = command.parentCategoryId,
            isActive = true,
            isSystemDefined = command.isSystemDefined,
            createdBy = principal.userId,
            updatedBy = principal.userId
        )

        val saved = try {
            repository.createCostCategory(category)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to create cost category.")
        }

        repository.recordAuditEvent(
            BusinessCostClassificationAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                trackingId = saved.id,
                action = "CREATE_COST_CATEGORY",
                actorId = principal.userId,
                actorRole = principal.role.name,
                newStateJson = "code=${saved.code}, name=${saved.name}",
                reason = "Created cost category ${saved.code}"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun getCostCategoryById(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessCostCategory> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val category = repository.findCostCategoryById(id, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Cost category '$id' not found.")
        return DomainResult.Success(category)
    }

    override suspend fun updateCostCategory(
        principal: AuthenticatedPrincipal,
        id: String,
        command: UpdateCostCategoryCommand
    ): DomainResult<BusinessCostCategory> {
        val access = checkManagerOrAdmin(principal, "update cost categories")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCostCategoryById(id, tenantId, projectId)
            ?: return DomainResult.Error(message = "Cost category '$id' not found.")

        val valRes = BusinessCostValidators.validateCostCategory(
            code = existing.code,
            name = command.name,
            tenantId = tenantId,
            projectId = projectId,
            categoryId = id,
            parentCategoryId = command.parentCategoryId
        )
        if (valRes is DomainResult.Error) return valRes

        if (command.parentCategoryId != null) {
            val parent = repository.findCostCategoryById(command.parentCategoryId, tenantId, projectId)
                ?: return DomainResult.Error(message = "Parent cost category '${command.parentCategoryId}' not found.")
            if (parent.id == id) {
                return DomainResult.Error(message = "A cost category cannot be its own parent.")
            }
        }

        val updated = existing.copy(
            name = command.name.trim(),
            description = command.description?.trim(),
            parentCategoryId = command.parentCategoryId,
            isActive = command.isActive,
            updatedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )

        val saved = try {
            repository.updateCostCategory(updated)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to update cost category.")
        }

        repository.recordAuditEvent(
            BusinessCostClassificationAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                trackingId = saved.id,
                action = "UPDATE_COST_CATEGORY",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStateJson = "name=${existing.name}, active=${existing.isActive}",
                newStateJson = "name=${saved.name}, active=${saved.isActive}",
                reason = "Updated cost category metadata"
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun activateCostCategory(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessCostCategory> {
        val access = checkManagerOrAdmin(principal, "activate cost categories")
        if (access is DomainResult.Error) return access

        val existing = repository.findCostCategoryById(id, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Cost category '$id' not found.")
        if (existing.isActive) return DomainResult.Success(existing)

        val updated = repository.updateCostCategory(existing.copy(isActive = true, updatedBy = principal.userId))
        return DomainResult.Success(updated)
    }

    override suspend fun deactivateCostCategory(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessCostCategory> {
        val access = checkManagerOrAdmin(principal, "deactivate cost categories")
        if (access is DomainResult.Error) return access

        val existing = repository.findCostCategoryById(id, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Cost category '$id' not found.")
        if (!existing.isActive) return DomainResult.Success(existing)

        val updated = repository.updateCostCategory(existing.copy(isActive = false, updatedBy = principal.userId))
        return DomainResult.Success(updated)
    }

    override suspend fun listCostCategories(
        principal: AuthenticatedPrincipal,
        activeOnly: Boolean?
    ): DomainResult<List<BusinessCostCategory>> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listCostCategories(defaultTenantId, principal.projectId, activeOnly)
        return DomainResult.Success(list)
    }

    override suspend fun getCostCategoryHierarchy(
        principal: AuthenticatedPrincipal,
        parentCategoryId: String
    ): DomainResult<List<BusinessCostCategory>> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.getCostCategoryChildren(parentCategoryId, defaultTenantId, principal.projectId)
        return DomainResult.Success(list)
    }

    // --- Operational Cost Tracking & Ingestion ---

    override suspend fun trackOperationalCost(
        principal: AuthenticatedPrincipal,
        command: TrackOperationalCostCommand
    ): DomainResult<BusinessCostTracking> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // Check Idempotency Key
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existingAudit = repository.listAuditEvents(tenantId, projectId)
                .find { it.idempotencyKey == command.idempotencyKey }
            if (existingAudit != null) {
                val existingTracking = repository.findCostTrackingById(existingAudit.trackingId, tenantId, projectId)
                if (existingTracking != null) {
                    return DomainResult.Success(existingTracking)
                }
            }
        }

        // Validate canonical source and resolve exact canonical amount
        val canonicalAmount = when (command.sourceType) {
            BusinessCostTrackingSourceType.BUSINESS_EXPENSE -> {
                if (expenseRepository != null) {
                    when (val r = expenseRepository.getExpenseById(tenantId, projectId, command.sourceId)) {
                        is DomainResult.Success -> {
                            val exp = r.data ?: return DomainResult.Error(message = "Canonical source expense '${command.sourceId}' not found.")
                            exp.amount
                        }
                        is DomainResult.Error -> return DomainResult.Error(message = "Canonical source expense '${command.sourceId}' not found.")
                        DomainResult.Loading -> null
                    }
                } else command.amount ?: return DomainResult.Error(message = "Missing amount for expense tracking.")
            }
            BusinessCostTrackingSourceType.VENDOR_PAYABLE -> {
                if (payableRepository != null) {
                    when (val r = payableRepository.getPayableById(tenantId, projectId, command.sourceId)) {
                        is DomainResult.Success -> {
                            val pay = r.data ?: return DomainResult.Error(message = "Canonical source payable '${command.sourceId}' not found.")
                            pay.originalAmount
                        }
                        is DomainResult.Error -> return DomainResult.Error(message = "Canonical source payable '${command.sourceId}' not found.")
                        DomainResult.Loading -> null
                    }
                } else command.amount ?: return DomainResult.Error(message = "Missing amount for payable tracking.")
            }
            BusinessCostTrackingSourceType.BUSINESS_LEDGER_POSTING -> {
                if (ledgerRepository != null) {
                    val posting = ledgerRepository.findPostingById(command.sourceId, tenantId, projectId)
                        ?: return DomainResult.Error(message = "Canonical source posting '${command.sourceId}' not found.")
                    if (posting.debitAmount > BigDecimal.ZERO) posting.debitAmount else posting.creditAmount
                } else command.amount ?: return DomainResult.Error(message = "Missing amount for posting tracking.")
            }
            BusinessCostTrackingSourceType.MANUAL_OPERATIONAL_REFERENCE -> {
                command.amount ?: return DomainResult.Error(message = "Amount is mandatory for manual operational cost reference.")
            }
        }?.setScale(4, RoundingMode.HALF_UP) ?: return DomainResult.Error(message = "Unable to resolve canonical amount.")

        if (command.amount != null && command.amount.setScale(4, RoundingMode.HALF_UP) != canonicalAmount) {
            return DomainResult.Error(
                message = "Amount ${command.amount} does not match canonical source amount $canonicalAmount."
            )
        }

        val costCenter = repository.findCostCenterById(command.costCenterId, tenantId, projectId)
        val costCategory = repository.findCostCategoryById(command.costCategoryId, tenantId, projectId)

        val valRes = BusinessCostValidators.validateCostTracking(
            tenantId = tenantId,
            projectId = projectId,
            sourceId = command.sourceId,
            costCenter = costCenter,
            costCategory = costCategory,
            amount = canonicalAmount,
            currency = command.currency,
            createdBy = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        val trackingId = "BCT-" + UUID.randomUUID().toString().take(12).uppercase()
        val allocationStatus = if (command.jobId.isNullOrBlank()) {
            BusinessCostAllocationStatus.UNALLOCATED
        } else {
            BusinessCostAllocationStatus.FULLY_ALLOCATED
        }

        val tracking = BusinessCostTracking(
            id = trackingId,
            tenantId = tenantId,
            projectId = projectId,
            sourceType = command.sourceType,
            sourceId = command.sourceId,
            ledgerPostingId = command.ledgerPostingId,
            costCenterId = command.costCenterId,
            costCategoryId = command.costCategoryId,
            jobId = command.jobId,
            amount = canonicalAmount,
            currency = command.currency,
            allocationStatus = allocationStatus,
            classificationStatus = BusinessCostClassificationStatus.CLASSIFIED,
            notes = command.notes,
            createdBy = principal.userId,
            updatedBy = principal.userId
        )

        val saved = try {
            repository.createCostTracking(tracking)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to record cost tracking.")
        }

        repository.recordAuditEvent(
            BusinessCostClassificationAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                trackingId = saved.id,
                action = "TRACK_OPERATIONAL_COST",
                actorId = principal.userId,
                actorRole = principal.role.name,
                newStateJson = "center=${costCenter?.code}, category=${costCategory?.code}, job=${command.jobId}, amount=$canonicalAmount",
                reason = "Tracked operational cost from ${command.sourceType} ${command.sourceId}",
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun getCostTrackingById(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessCostTracking> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val tracking = repository.findCostTrackingById(id, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Cost tracking record '$id' not found.")
        return DomainResult.Success(tracking)
    }

    override suspend fun listCostTracking(
        principal: AuthenticatedPrincipal,
        filter: BusinessCostTrackingFilter
    ): DomainResult<List<BusinessCostTracking>> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listCostTracking(defaultTenantId, principal.projectId, filter)
        return DomainResult.Success(list)
    }

    override suspend fun getCostTrackingBySource(
        principal: AuthenticatedPrincipal,
        sourceType: BusinessCostTrackingSourceType,
        sourceId: String
    ): DomainResult<List<BusinessCostTracking>> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.findCostTrackingBySource(sourceType, sourceId, defaultTenantId, principal.projectId)
        return DomainResult.Success(list)
    }

    // --- Classification & Reclassification ---

    override suspend fun classifyCost(
        principal: AuthenticatedPrincipal,
        command: ClassifyCostCommand
    ): DomainResult<BusinessCostTracking> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCostTrackingById(command.trackingId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Cost tracking record '${command.trackingId}' not found.")

        val costCenter = repository.findCostCenterById(command.costCenterId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Cost center '${command.costCenterId}' not found.")
        if (!costCenter.isActive) {
            return DomainResult.Error(message = "Target cost center '${costCenter.name}' is inactive.")
        }

        val costCategory = repository.findCostCategoryById(command.costCategoryId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Cost category '${command.costCategoryId}' not found.")
        if (!costCategory.isActive) {
            return DomainResult.Error(message = "Target cost category '${costCategory.name}' is inactive.")
        }

        val allocationStatus = if (command.jobId.isNullOrBlank()) {
            BusinessCostAllocationStatus.UNALLOCATED
        } else {
            BusinessCostAllocationStatus.FULLY_ALLOCATED
        }

        val updated = existing.copy(
            costCenterId = command.costCenterId,
            costCategoryId = command.costCategoryId,
            jobId = command.jobId,
            notes = command.notes ?: existing.notes,
            allocationStatus = allocationStatus,
            classificationStatus = BusinessCostClassificationStatus.CLASSIFIED,
            updatedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )

        val saved = try {
            repository.updateCostTracking(updated)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to update cost classification.")
        }

        repository.recordAuditEvent(
            BusinessCostClassificationAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                trackingId = saved.id,
                action = "CLASSIFY_COST",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStateJson = "center=${existing.costCenterId}, category=${existing.costCategoryId}, job=${existing.jobId}",
                newStateJson = "center=${saved.costCenterId}, category=${saved.costCategoryId}, job=${saved.jobId}",
                reason = "Classified operational cost record",
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun reclassifyCost(
        principal: AuthenticatedPrincipal,
        command: ReclassifyCostCommand
    ): DomainResult<BusinessCostTracking> {
        val access = checkManagerOrAdmin(principal, "reclassify operational costs")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val existing = repository.findCostTrackingById(command.trackingId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Cost tracking record '${command.trackingId}' not found.")

        val targetCostCenter = repository.findCostCenterById(command.newCostCenterId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Cost center '${command.newCostCenterId}' not found.")
        val targetCostCategory = repository.findCostCategoryById(command.newCostCategoryId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Cost category '${command.newCostCategoryId}' not found.")

        val valRes = BusinessCostValidators.validateReclassification(
            existingTracking = existing,
            newCostCenter = targetCostCenter,
            newCostCategory = targetCostCategory,
            reason = command.reason,
            actorId = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        val previousCostCenter = repository.findCostCenterById(existing.costCenterId, tenantId, projectId)
        val previousCostCategory = repository.findCostCategoryById(existing.costCategoryId, tenantId, projectId)

        val newAllocationStatus = if (command.newJobId.isNullOrBlank()) {
            BusinessCostAllocationStatus.UNALLOCATED
        } else {
            BusinessCostAllocationStatus.RECLASSIFIED
        }

        val updated = existing.copy(
            costCenterId = command.newCostCenterId,
            costCategoryId = command.newCostCategoryId,
            jobId = command.newJobId,
            allocationStatus = newAllocationStatus,
            classificationStatus = BusinessCostClassificationStatus.RECLASSIFIED,
            updatedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )

        val saved = try {
            repository.updateCostTracking(updated)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to reclassify cost.")
        }

        repository.recordAuditEvent(
            BusinessCostClassificationAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                trackingId = saved.id,
                action = "RECLASSIFY_COST",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStateJson = "center=${previousCostCenter?.code}, category=${previousCostCategory?.code}, job=${existing.jobId}",
                newStateJson = "center=${targetCostCenter.code}, category=${targetCostCategory.code}, job=${command.newJobId}",
                reason = command.reason,
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    // --- Summaries & Analytical Projections ---

    override suspend fun getCostCenterSummary(
        principal: AuthenticatedPrincipal,
        costCenterId: String
    ): DomainResult<BusinessCostCenterSummary> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val summary = try {
            repository.calculateCostCenterSummary(costCenterId, defaultTenantId, principal.projectId)
        } catch (e: NoSuchElementException) {
            return DomainResult.Error(message = e.message ?: "Cost center '$costCenterId' not found.")
        }
        return DomainResult.Success(summary)
    }

    override suspend fun getCostCategorySummary(
        principal: AuthenticatedPrincipal,
        categoryId: String
    ): DomainResult<BusinessCostCategorySummary> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val summary = try {
            repository.calculateCostCategorySummary(categoryId, defaultTenantId, principal.projectId)
        } catch (e: NoSuchElementException) {
            return DomainResult.Error(message = e.message ?: "Cost category '$categoryId' not found.")
        }
        return DomainResult.Success(summary)
    }

    override suspend fun getJobCostDetail(
        principal: AuthenticatedPrincipal,
        jobId: String
    ): DomainResult<BusinessJobCostDetailSummary> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val summary = repository.calculateJobCostDetail(jobId, defaultTenantId, principal.projectId)
        return DomainResult.Success(summary)
    }

    override suspend fun getTrackingSummary(principal: AuthenticatedPrincipal): DomainResult<BusinessCostTrackingSummary> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val summary = repository.calculateTrackingSummary(defaultTenantId, principal.projectId)
        return DomainResult.Success(summary)
    }

    override suspend fun getAuditTrail(
        principal: AuthenticatedPrincipal,
        trackingId: String?
    ): DomainResult<List<BusinessCostClassificationAuditEvent>> {
        val access = checkInternalStaffAccess(principal)
        if (access is DomainResult.Error) return access

        val audits = repository.listAuditEvents(defaultTenantId, principal.projectId, trackingId)
        return DomainResult.Success(audits)
    }
}
