package com.sucharu.sucharupro.domain.service.businessexpense

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseAuditEvent
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import com.sucharu.sucharupro.domain.validation.businessexpense.BusinessExpenseValidator
import java.util.UUID

/**
 * Implementation of BusinessExpenseService orchestrating validation, RBAC, separation of duties,
 * lifecycle state machines, and immutable audit trails (Module 15 Step 01).
 */
class BusinessExpenseServiceImpl(
    private val repository: BusinessExpenseRepository,
    private val defaultTenantId: String = "TENANT-001"
) : BusinessExpenseService {

    private fun resolveTenantId(principal: AuthenticatedPrincipal): String {
        return defaultTenantId
    }

    private fun assertInternalStaff(principal: AuthenticatedPrincipal): String? {
        if (principal.role in setOf(UserRole.CUSTOMER, UserRole.AFFILIATE, UserRole.VENDOR, UserRole.GUEST)) {
            return "Access denied: External principals cannot access internal business expense operations."
        }
        return null
    }

    private fun assertApprovalAuthority(principal: AuthenticatedPrincipal): String? {
        if (principal.role !in setOf(UserRole.ADMIN, UserRole.MANAGER)) {
            return "Access denied: User role '${principal.role.name}' does not have expense approval authority."
        }
        return null
    }

    override suspend fun createExpense(
        principal: AuthenticatedPrincipal,
        command: CreateBusinessExpenseCommand
    ): DomainResult<BusinessExpense> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        // Validate create payload
        val valResult = BusinessExpenseValidator.validateCreatePayload(
            tenantId = tenantId,
            projectId = projectId,
            categoryId = command.categoryId,
            amount = command.amount,
            currency = command.currency,
            expenseDate = command.expenseDate,
            paymentMethod = command.paymentMethod,
            paymentReference = command.paymentReference,
            description = command.description,
            createdBy = principal.userId
        )
        if (valResult is DomainResult.Error) return valResult

        // Validate Category
        val catResult = repository.getCategoryById(tenantId, projectId, command.categoryId)
        if (catResult is DomainResult.Error) return catResult
        val category = (catResult as DomainResult.Success).data
        val catCheck = BusinessExpenseValidator.validateCategoryCompatibility(category, tenantId, projectId)
        if (catCheck is DomainResult.Error) return catCheck

        val expenseId = "EXP-" + UUID.randomUUID().toString()
        val expenseNumber = repository.generateNextExpenseNumber(tenantId, projectId)
        val now = System.currentTimeMillis()

        val initialStatus = if (command.autoSubmit) BusinessExpenseStatus.SUBMITTED else BusinessExpenseStatus.DRAFT

        val expense = BusinessExpense(
            expenseId = expenseId,
            tenantId = tenantId,
            projectId = projectId,
            branchId = command.branchId?.trim(),
            locationId = command.locationId?.trim(),
            expenseNumber = expenseNumber,
            expenseCategoryId = command.categoryId.trim(),
            amount = command.amount,
            currency = command.currency.trim(),
            expenseDate = command.expenseDate,
            paymentMethod = command.paymentMethod,
            paymentReference = command.paymentReference?.trim(),
            status = initialStatus,
            vendorId = command.vendorId?.trim(),
            jobId = command.jobId?.trim(),
            description = command.description.trim(),
            notes = command.notes?.trim(),
            attachmentUrl = command.attachmentUrl?.trim(),
            attachmentMetadata = command.attachmentMetadata?.trim(),
            idempotencyKey = command.idempotencyKey?.trim(),
            createdBy = principal.userId,
            createdAt = now,
            submittedBy = if (command.autoSubmit) principal.userId else null,
            submittedAt = if (command.autoSubmit) now else null,
            updatedAt = now,
            updatedBy = principal.userId,
            version = 1L
        )

        val createResult = repository.createExpense(expense)
        if (createResult is DomainResult.Success) {
            repository.recordAuditEvent(
                BusinessExpenseAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    expenseId = expenseId,
                    eventType = if (command.autoSubmit) "CREATED_AND_SUBMITTED" else "CREATED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    newStatus = initialStatus,
                    metadataJson = "{\"amount\":\"${expense.amount}\",\"currency\":\"${expense.currency}\",\"number\":\"$expenseNumber\"}"
                )
            )
        }
        return createResult
    }

    override suspend fun updateExpenseDraft(
        principal: AuthenticatedPrincipal,
        expenseId: String,
        command: UpdateBusinessExpenseCommand
    ): DomainResult<BusinessExpense> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getExpenseById(tenantId, projectId, expenseId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Business expense '$expenseId' not found.")

        val statusCheck = BusinessExpenseValidator.validateUpdateDraft(existing)
        if (statusCheck is DomainResult.Error) return statusCheck

        if (principal.role != UserRole.ADMIN && principal.role != UserRole.MANAGER && existing.createdBy != principal.userId) {
            return DomainResult.Error(message = "Access denied: You can only edit expenses created by yourself.")
        }

        val updatedCategory = command.categoryId?.let { catId ->
            val catRes = repository.getCategoryById(tenantId, projectId, catId)
            if (catRes is DomainResult.Error) return catRes
            val cat = (catRes as DomainResult.Success).data
            val check = BusinessExpenseValidator.validateCategoryCompatibility(cat, tenantId, projectId)
            if (check is DomainResult.Error) return check
            catId
        } ?: existing.expenseCategoryId

        val updatedAmount = command.amount ?: existing.amount
        val updatedCurrency = command.currency ?: existing.currency
        val updatedDate = command.expenseDate ?: existing.expenseDate
        val updatedMethod = command.paymentMethod ?: existing.paymentMethod
        val updatedReference = if (command.paymentReference != null) command.paymentReference else existing.paymentReference
        val updatedDescription = command.description ?: existing.description

        val valResult = BusinessExpenseValidator.validateCreatePayload(
            tenantId = tenantId,
            projectId = projectId,
            categoryId = updatedCategory,
            amount = updatedAmount,
            currency = updatedCurrency,
            expenseDate = updatedDate,
            paymentMethod = updatedMethod,
            paymentReference = updatedReference,
            description = updatedDescription,
            createdBy = existing.createdBy
        )
        if (valResult is DomainResult.Error) return valResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            expenseCategoryId = updatedCategory,
            amount = updatedAmount,
            currency = updatedCurrency,
            expenseDate = updatedDate,
            paymentMethod = updatedMethod,
            paymentReference = updatedReference?.trim(),
            vendorId = command.vendorId?.trim() ?: existing.vendorId,
            jobId = command.jobId?.trim() ?: existing.jobId,
            branchId = command.branchId?.trim() ?: existing.branchId,
            locationId = command.locationId?.trim() ?: existing.locationId,
            description = updatedDescription.trim(),
            notes = command.notes?.trim() ?: existing.notes,
            attachmentUrl = command.attachmentUrl?.trim() ?: existing.attachmentUrl,
            attachmentMetadata = command.attachmentMetadata?.trim() ?: existing.attachmentMetadata,
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updateExpense(updated)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                BusinessExpenseAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    expenseId = expenseId,
                    eventType = "UPDATED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = existing.status,
                    metadataJson = "{\"amount\":\"${updated.amount}\"}"
                )
            )
        }
        return saveRes
    }

    override suspend fun submitExpense(
        principal: AuthenticatedPrincipal,
        expenseId: String
    ): DomainResult<BusinessExpense> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getExpenseById(tenantId, projectId, expenseId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Business expense '$expenseId' not found.")

        val check = BusinessExpenseValidator.validateSubmit(existing)
        if (check is DomainResult.Error) return check

        if (principal.role != UserRole.ADMIN && principal.role != UserRole.MANAGER && existing.createdBy != principal.userId) {
            return DomainResult.Error(message = "Access denied: Only the expense creator or management can submit this expense.")
        }

        val now = System.currentTimeMillis()
        val submitted = existing.copy(
            status = BusinessExpenseStatus.SUBMITTED,
            submittedBy = principal.userId,
            submittedAt = now,
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updateExpense(submitted)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                BusinessExpenseAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    expenseId = expenseId,
                    eventType = "SUBMITTED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = BusinessExpenseStatus.SUBMITTED
                )
            )
        }
        return saveRes
    }

    override suspend fun approveExpense(
        principal: AuthenticatedPrincipal,
        expenseId: String,
        notes: String?
    ): DomainResult<BusinessExpense> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        assertApprovalAuthority(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getExpenseById(tenantId, projectId, expenseId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Business expense '$expenseId' not found.")

        val isSuperAdmin = principal.role == UserRole.ADMIN
        val valResult = BusinessExpenseValidator.validateApprove(existing, principal.userId, isSuperAdmin)
        if (valResult is DomainResult.Error) return valResult

        val now = System.currentTimeMillis()
        val approved = existing.copy(
            status = BusinessExpenseStatus.APPROVED,
            approvedBy = principal.userId,
            approvedAt = now,
            notes = if (!notes.isNullOrBlank()) "${existing.notes ?: ""}\nApproval Note: $notes".trim() else existing.notes,
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updateExpense(approved)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                BusinessExpenseAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    expenseId = expenseId,
                    eventType = "APPROVED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = BusinessExpenseStatus.APPROVED,
                    metadataJson = if (!notes.isNullOrBlank()) "{\"notes\":\"$notes\"}" else null
                )
            )
        }
        return saveRes
    }

    override suspend fun rejectExpense(
        principal: AuthenticatedPrincipal,
        expenseId: String,
        reason: String
    ): DomainResult<BusinessExpense> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        assertApprovalAuthority(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getExpenseById(tenantId, projectId, expenseId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Business expense '$expenseId' not found.")

        val valResult = BusinessExpenseValidator.validateReject(existing, reason)
        if (valResult is DomainResult.Error) return valResult

        val now = System.currentTimeMillis()
        val rejected = existing.copy(
            status = BusinessExpenseStatus.REJECTED,
            rejectedBy = principal.userId,
            rejectedAt = now,
            rejectionReason = reason.trim(),
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updateExpense(rejected)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                BusinessExpenseAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    expenseId = expenseId,
                    eventType = "REJECTED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = BusinessExpenseStatus.REJECTED,
                    reason = reason.trim()
                )
            )
        }
        return saveRes
    }

    override suspend fun cancelExpense(
        principal: AuthenticatedPrincipal,
        expenseId: String,
        reason: String
    ): DomainResult<BusinessExpense> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getExpenseById(tenantId, projectId, expenseId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Business expense '$expenseId' not found.")

        if (principal.role != UserRole.ADMIN && principal.role != UserRole.MANAGER && existing.createdBy != principal.userId) {
            return DomainResult.Error(message = "Access denied: You can only cancel expenses created by yourself or manage them as an admin/manager.")
        }

        val valResult = BusinessExpenseValidator.validateCancel(existing, reason)
        if (valResult is DomainResult.Error) return valResult

        val now = System.currentTimeMillis()
        val cancelled = existing.copy(
            status = BusinessExpenseStatus.CANCELLED,
            cancelledBy = principal.userId,
            cancelledAt = now,
            cancellationReason = reason.trim(),
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updateExpense(cancelled)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                BusinessExpenseAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    expenseId = expenseId,
                    eventType = "CANCELLED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = BusinessExpenseStatus.CANCELLED,
                    reason = reason.trim()
                )
            )
        }
        return saveRes
    }

    override suspend fun getExpenseById(
        principal: AuthenticatedPrincipal,
        expenseId: String
    ): DomainResult<BusinessExpense> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val result = repository.getExpenseById(tenantId, projectId, expenseId)
        if (result is DomainResult.Error) return result
        val expense = (result as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Business expense '$expenseId' not found.")
        return DomainResult.Success(expense)
    }

    override suspend fun getExpenseByNumber(
        principal: AuthenticatedPrincipal,
        expenseNumber: String
    ): DomainResult<BusinessExpense> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val result = repository.getExpenseByNumber(tenantId, projectId, expenseNumber)
        if (result is DomainResult.Error) return result
        val expense = (result as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Business expense number '$expenseNumber' not found.")
        return DomainResult.Success(expense)
    }

    override suspend fun listExpenses(
        principal: AuthenticatedPrincipal,
        filter: BusinessExpenseFilter,
        limit: Int,
        offset: Int
    ): DomainResult<List<BusinessExpense>> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        return repository.listExpenses(
            tenantId = tenantId,
            projectId = projectId,
            status = filter.status,
            categoryId = filter.categoryId,
            vendorId = filter.vendorId,
            jobId = filter.jobId,
            fromDate = filter.fromDate,
            toDate = filter.toDate,
            limit = limit,
            offset = offset
        )
    }

    override suspend fun countExpenses(
        principal: AuthenticatedPrincipal,
        filter: BusinessExpenseFilter
    ): DomainResult<Long> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        return repository.countExpenses(
            tenantId = tenantId,
            projectId = projectId,
            status = filter.status,
            categoryId = filter.categoryId,
            vendorId = filter.vendorId,
            jobId = filter.jobId,
            fromDate = filter.fromDate,
            toDate = filter.toDate
        )
    }

    override suspend fun getExpenseAuditTrail(
        principal: AuthenticatedPrincipal,
        expenseId: String
    ): DomainResult<List<BusinessExpenseAuditEvent>> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        return repository.getAuditEvents(tenantId, projectId, expenseId)
    }

    override suspend fun listCategories(
        principal: AuthenticatedPrincipal,
        activeOnly: Boolean
    ): DomainResult<List<BusinessExpenseCategory>> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        return repository.listCategories(tenantId, projectId, activeOnly)
    }

    override suspend fun createCategory(
        principal: AuthenticatedPrincipal,
        command: CreateBusinessExpenseCategoryCommand
    ): DomainResult<BusinessExpenseCategory> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        if (principal.role != UserRole.ADMIN && principal.role != UserRole.MANAGER) {
            return DomainResult.Error(message = "Access denied: Only administrators and managers can create expense categories.")
        }
        if (command.name.isBlank()) return DomainResult.Error(message = "Category name cannot be blank.")
        if (command.code.isBlank()) return DomainResult.Error(message = "Category code cannot be blank.")

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId
        val categoryId = "CAT-" + UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val category = BusinessExpenseCategory(
            categoryId = categoryId,
            tenantId = tenantId,
            projectId = projectId,
            name = command.name.trim(),
            code = command.code.trim().uppercase(),
            description = command.description?.trim(),
            isActive = true,
            sortOrder = command.sortOrder,
            createdAt = now,
            updatedAt = now,
            version = 1L
        )

        return repository.createCategory(category)
    }
}
