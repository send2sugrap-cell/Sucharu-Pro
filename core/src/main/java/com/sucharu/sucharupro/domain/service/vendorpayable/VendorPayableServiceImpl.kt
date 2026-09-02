package com.sucharu.sucharupro.domain.service.vendorpayable

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import com.sucharu.sucharupro.domain.validation.vendorpayable.VendorPayableValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Implementation of VendorPayableService orchestrating validation, RBAC, separation of duties,
 * atomic payment allocation, and immutable audit logging (Module 15 Step 02).
 */
class VendorPayableServiceImpl(
    private val repository: VendorPayableRepository,
    private val defaultTenantId: String = "TENANT-001"
) : VendorPayableService {

    private fun resolveTenantId(principal: AuthenticatedPrincipal): String {
        return defaultTenantId
    }

    private fun assertInternalStaff(principal: AuthenticatedPrincipal): String? {
        if (principal.role in setOf(UserRole.CUSTOMER, UserRole.AFFILIATE, UserRole.GUEST)) {
            return "Access denied: Role '${principal.role.name}' cannot access vendor payables."
        }
        return null
    }

    private fun assertInternalStaffMutation(principal: AuthenticatedPrincipal): String? {
        assertInternalStaff(principal)?.let { return it }
        if (principal.role == UserRole.VENDOR) {
            return "Access denied: Vendors cannot mutate internal payable records."
        }
        return null
    }

    private fun assertApprovalAuthority(principal: AuthenticatedPrincipal): String? {
        assertInternalStaff(principal)?.let { return it }
        if (principal.role !in setOf(UserRole.ADMIN, UserRole.MANAGER)) {
            return "Access denied: User role '${principal.role.name}' does not have payable approval/void authority."
        }
        return null
    }

    override suspend fun createPayable(
        principal: AuthenticatedPrincipal,
        command: CreateVendorPayableCommand
    ): DomainResult<VendorPayable> {
        assertInternalStaffMutation(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val valResult = VendorPayableValidator.validateCreatePayload(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = command.vendorId,
            originalAmount = command.originalAmount,
            currency = command.currency,
            issueDate = command.issueDate,
            paymentTerms = command.paymentTerms,
            customTermDays = command.customTermDays,
            description = command.description,
            createdBy = principal.userId
        )
        if (valResult is DomainResult.Error) return valResult

        val payableId = "PAYABLE-" + UUID.randomUUID().toString()
        val payableNumber = repository.generateNextPayableNumber(tenantId, projectId)
        val now = System.currentTimeMillis()
        val dueDate = command.paymentTerms.calculateDueDate(command.issueDate, command.customTermDays)
        val initialStatus = if (command.autoSubmit) VendorPayableStatus.SUBMITTED else VendorPayableStatus.DRAFT

        val payable = VendorPayable(
            payableId = payableId,
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = payableNumber,
            vendorId = command.vendorId.trim(),
            jobId = command.jobId?.trim(),
            vendorJobId = command.vendorJobId?.trim(),
            billReference = command.billReference?.trim(),
            description = command.description.trim(),
            notes = command.notes?.trim(),
            originalAmount = command.originalAmount.setScale(4, RoundingMode.HALF_UP),
            paidAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            currency = command.currency.trim().uppercase(),
            issueDate = command.issueDate,
            paymentTerms = command.paymentTerms,
            customTermDays = command.customTermDays,
            dueDate = dueDate,
            status = initialStatus,
            attachmentUrl = command.attachmentUrl?.trim(),
            idempotencyKey = command.idempotencyKey?.trim(),
            createdBy = principal.userId,
            createdAt = now,
            submittedBy = if (command.autoSubmit) principal.userId else null,
            submittedAt = if (command.autoSubmit) now else null,
            updatedAt = now,
            updatedBy = principal.userId,
            version = 1L
        )

        val createResult = repository.createPayable(payable)
        if (createResult is DomainResult.Success) {
            repository.recordAuditEvent(
                VendorPayableAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    payableId = payableId,
                    vendorId = payable.vendorId,
                    eventType = if (command.autoSubmit) "CREATED_AND_SUBMITTED" else "CREATED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    newStatus = initialStatus,
                    amount = payable.originalAmount,
                    metadataJson = "{\"number\":\"$payableNumber\",\"terms\":\"${payable.paymentTerms.name}\"}"
                )
            )
        }
        return createResult
    }

    override suspend fun updatePayableDraft(
        principal: AuthenticatedPrincipal,
        payableId: String,
        command: UpdateVendorPayableCommand
    ): DomainResult<VendorPayable> {
        assertInternalStaffMutation(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getPayableById(tenantId, projectId, payableId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Vendor payable '$payableId' not found.")

        val statusCheck = VendorPayableValidator.validateUpdateDraft(existing)
        if (statusCheck is DomainResult.Error) return statusCheck

        if (principal.role != UserRole.ADMIN && principal.role != UserRole.MANAGER && existing.createdBy != principal.userId) {
            return DomainResult.Error(message = "Access denied: You can only edit payables created by yourself.")
        }

        val updatedVendor = command.vendorId?.trim() ?: existing.vendorId
        val updatedAmount = command.originalAmount?.setScale(4, RoundingMode.HALF_UP) ?: existing.originalAmount
        val updatedCurrency = command.currency?.trim()?.uppercase() ?: existing.currency
        val updatedIssueDate = command.issueDate ?: existing.issueDate
        val updatedTerms = command.paymentTerms ?: existing.paymentTerms
        val updatedCustomDays = if (command.paymentTerms != null) command.customTermDays else (command.customTermDays ?: existing.customTermDays)
        val updatedDueDate = updatedTerms.calculateDueDate(updatedIssueDate, updatedCustomDays)
        val updatedDescription = command.description?.trim() ?: existing.description

        val valResult = VendorPayableValidator.validateCreatePayload(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = updatedVendor,
            originalAmount = updatedAmount,
            currency = updatedCurrency,
            issueDate = updatedIssueDate,
            paymentTerms = updatedTerms,
            customTermDays = updatedCustomDays,
            description = updatedDescription,
            createdBy = existing.createdBy
        )
        if (valResult is DomainResult.Error) return valResult

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            vendorId = updatedVendor,
            jobId = command.jobId?.trim() ?: existing.jobId,
            vendorJobId = command.vendorJobId?.trim() ?: existing.vendorJobId,
            billReference = command.billReference?.trim() ?: existing.billReference,
            description = updatedDescription,
            notes = command.notes?.trim() ?: existing.notes,
            originalAmount = updatedAmount,
            currency = updatedCurrency,
            issueDate = updatedIssueDate,
            paymentTerms = updatedTerms,
            customTermDays = updatedCustomDays,
            dueDate = updatedDueDate,
            attachmentUrl = command.attachmentUrl?.trim() ?: existing.attachmentUrl,
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updatePayable(updated)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                VendorPayableAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    payableId = payableId,
                    vendorId = updated.vendorId,
                    eventType = "UPDATED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = existing.status,
                    amount = updated.originalAmount
                )
            )
        }
        return saveRes
    }

    override suspend fun submitPayable(
        principal: AuthenticatedPrincipal,
        payableId: String
    ): DomainResult<VendorPayable> {
        assertInternalStaffMutation(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getPayableById(tenantId, projectId, payableId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Vendor payable '$payableId' not found.")

        val check = VendorPayableValidator.validateSubmit(existing)
        if (check is DomainResult.Error) return check

        if (principal.role != UserRole.ADMIN && principal.role != UserRole.MANAGER && existing.createdBy != principal.userId) {
            return DomainResult.Error(message = "Access denied: Only the payable creator or management can submit this payable.")
        }

        val now = System.currentTimeMillis()
        val submitted = existing.copy(
            status = VendorPayableStatus.SUBMITTED,
            submittedBy = principal.userId,
            submittedAt = now,
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updatePayable(submitted)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                VendorPayableAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    payableId = payableId,
                    vendorId = existing.vendorId,
                    eventType = "SUBMITTED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = VendorPayableStatus.SUBMITTED
                )
            )
        }
        return saveRes
    }

    override suspend fun approvePayable(
        principal: AuthenticatedPrincipal,
        payableId: String,
        notes: String?
    ): DomainResult<VendorPayable> {
        assertApprovalAuthority(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getPayableById(tenantId, projectId, payableId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Vendor payable '$payableId' not found.")

        val isSuperAdmin = principal.role == UserRole.ADMIN
        val valResult = VendorPayableValidator.validateApprove(existing, principal.userId, isSuperAdmin)
        if (valResult is DomainResult.Error) return valResult

        val now = System.currentTimeMillis()
        val approved = existing.copy(
            status = VendorPayableStatus.APPROVED,
            approvedBy = principal.userId,
            approvedAt = now,
            notes = if (!notes.isNullOrBlank()) "${existing.notes ?: ""}\nApproval Note: $notes".trim() else existing.notes,
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updatePayable(approved)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                VendorPayableAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    payableId = payableId,
                    vendorId = existing.vendorId,
                    eventType = "APPROVED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = VendorPayableStatus.APPROVED,
                    amount = existing.originalAmount,
                    metadataJson = if (!notes.isNullOrBlank()) "{\"notes\":\"$notes\"}" else null
                )
            )
        }
        return saveRes
    }

    override suspend fun rejectPayable(
        principal: AuthenticatedPrincipal,
        payableId: String,
        reason: String
    ): DomainResult<VendorPayable> {
        assertApprovalAuthority(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getPayableById(tenantId, projectId, payableId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Vendor payable '$payableId' not found.")

        val valResult = VendorPayableValidator.validateReject(existing, reason)
        if (valResult is DomainResult.Error) return valResult

        val now = System.currentTimeMillis()
        val rejected = existing.copy(
            status = VendorPayableStatus.REJECTED,
            rejectedBy = principal.userId,
            rejectedAt = now,
            rejectionReason = reason.trim(),
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updatePayable(rejected)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                VendorPayableAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    payableId = payableId,
                    vendorId = existing.vendorId,
                    eventType = "REJECTED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = VendorPayableStatus.REJECTED,
                    reason = reason.trim()
                )
            )
        }
        return saveRes
    }

    override suspend fun cancelPayable(
        principal: AuthenticatedPrincipal,
        payableId: String,
        reason: String
    ): DomainResult<VendorPayable> {
        assertInternalStaffMutation(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getPayableById(tenantId, projectId, payableId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Vendor payable '$payableId' not found.")

        if (principal.role != UserRole.ADMIN && principal.role != UserRole.MANAGER && existing.createdBy != principal.userId) {
            return DomainResult.Error(message = "Access denied: You can only cancel payables created by yourself or manage them as an admin/manager.")
        }

        val valResult = VendorPayableValidator.validateCancel(existing, reason)
        if (valResult is DomainResult.Error) return valResult

        val now = System.currentTimeMillis()
        val cancelled = existing.copy(
            status = VendorPayableStatus.CANCELLED,
            cancelledBy = principal.userId,
            cancelledAt = now,
            cancellationReason = reason.trim(),
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updatePayable(cancelled)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                VendorPayableAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    payableId = payableId,
                    vendorId = existing.vendorId,
                    eventType = "CANCELLED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = VendorPayableStatus.CANCELLED,
                    reason = reason.trim()
                )
            )
        }
        return saveRes
    }

    override suspend fun voidPayable(
        principal: AuthenticatedPrincipal,
        payableId: String,
        reason: String
    ): DomainResult<VendorPayable> {
        assertApprovalAuthority(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getPayableById(tenantId, projectId, payableId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Vendor payable '$payableId' not found.")

        val valResult = VendorPayableValidator.validateVoid(existing, reason)
        if (valResult is DomainResult.Error) return valResult

        val now = System.currentTimeMillis()
        val voided = existing.copy(
            status = VendorPayableStatus.VOIDED,
            voidedBy = principal.userId,
            voidedAt = now,
            voidReason = reason.trim(),
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updatePayable(voided)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                VendorPayableAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    payableId = payableId,
                    vendorId = existing.vendorId,
                    eventType = "VOIDED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = VendorPayableStatus.VOIDED,
                    reason = reason.trim()
                )
            )
        }
        return saveRes
    }

    override suspend fun allocatePayment(
        principal: AuthenticatedPrincipal,
        payableId: String,
        command: AllocateVendorPayablePaymentCommand
    ): DomainResult<VendorPayable> {
        assertInternalStaffMutation(principal)?.let { return DomainResult.Error(message = it) }
        if (principal.role !in setOf(UserRole.ADMIN, UserRole.MANAGER)) {
            return DomainResult.Error(message = "Access denied: Role '${principal.role.name}' does not have payment allocation authority.")
        }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val existingResult = repository.getPayableById(tenantId, projectId, payableId)
        if (existingResult is DomainResult.Error) return existingResult
        val existing = (existingResult as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Vendor payable '$payableId' not found.")

        val allocAmount = command.amount.setScale(4, RoundingMode.HALF_UP)
        val valResult = VendorPayableValidator.validatePaymentAllocation(
            payable = existing,
            amount = allocAmount,
            paymentMethod = command.paymentMethod,
            paymentReference = command.paymentReference,
            paymentDate = command.paymentDate
        )
        if (valResult is DomainResult.Error) return valResult

        val allocationId = "ALLOC-" + UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val allocation = VendorPayablePaymentAllocation(
            allocationId = allocationId,
            tenantId = tenantId,
            projectId = projectId,
            payableId = payableId,
            vendorId = existing.vendorId,
            amount = allocAmount,
            currency = existing.currency,
            paymentMethod = command.paymentMethod,
            paymentReference = command.paymentReference?.trim(),
            paymentDate = command.paymentDate,
            notes = command.notes?.trim(),
            allocatedBy = principal.userId,
            allocatedAt = now,
            idempotencyKey = command.idempotencyKey?.trim(),
            version = 1L
        )

        val allocRecordRes = repository.recordPaymentAllocation(allocation)
        if (allocRecordRes is DomainResult.Error) return DomainResult.Error(message = allocRecordRes.message, exception = allocRecordRes.exception)

        val newPaidAmount = existing.paidAmount.add(allocAmount).setScale(4, RoundingMode.HALF_UP)
        val newStatus = if (newPaidAmount >= existing.originalAmount) VendorPayableStatus.PAID else VendorPayableStatus.PARTIALLY_PAID

        val updatedPayable = existing.copy(
            paidAmount = newPaidAmount,
            status = newStatus,
            updatedAt = now,
            updatedBy = principal.userId
        )

        val saveRes = repository.updatePayable(updatedPayable)
        if (saveRes is DomainResult.Success) {
            repository.recordAuditEvent(
                VendorPayableAuditEvent(
                    eventId = "EVT-" + UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    payableId = payableId,
                    vendorId = existing.vendorId,
                    eventType = "PAYMENT_ALLOCATED",
                    actorId = principal.userId,
                    actorRole = principal.role.name,
                    timestamp = now,
                    previousStatus = existing.status,
                    newStatus = newStatus,
                    amount = allocAmount,
                    idempotencyKey = command.idempotencyKey,
                    metadataJson = "{\"method\":\"${command.paymentMethod.name}\",\"paid\":\"${newPaidAmount.toPlainString()}\",\"outstanding\":\"${updatedPayable.outstandingAmount.toPlainString()}\"}"
                )
            )
        }
        return saveRes
    }

    override suspend fun getPayableById(
        principal: AuthenticatedPrincipal,
        payableId: String
    ): DomainResult<VendorPayable> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val result = repository.getPayableById(tenantId, projectId, payableId)
        if (result is DomainResult.Error) return result
        val payable = (result as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Vendor payable '$payableId' not found.")

        if (principal.role == UserRole.VENDOR && payable.vendorId != principal.userId) {
            return DomainResult.Error(message = "Access denied: Vendors can only access their own payables.")
        }
        return DomainResult.Success(payable)
    }

    override suspend fun getPayableByNumber(
        principal: AuthenticatedPrincipal,
        payableNumber: String
    ): DomainResult<VendorPayable> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val result = repository.getPayableByNumber(tenantId, projectId, payableNumber)
        if (result is DomainResult.Error) return result
        val payable = (result as DomainResult.Success).data
            ?: return DomainResult.Error(message = "Vendor payable number '$payableNumber' not found.")

        if (principal.role == UserRole.VENDOR && payable.vendorId != principal.userId) {
            return DomainResult.Error(message = "Access denied: Vendors can only access their own payables.")
        }
        return DomainResult.Success(payable)
    }

    override suspend fun listPayables(
        principal: AuthenticatedPrincipal,
        filter: VendorPayableFilter,
        limit: Int,
        offset: Int
    ): DomainResult<List<VendorPayable>> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId
        val effectiveVendorId = if (principal.role == UserRole.VENDOR) principal.userId else filter.vendorId

        return repository.listPayables(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = effectiveVendorId,
            status = filter.status,
            jobId = filter.jobId,
            isOverdueOnly = filter.isOverdueOnly,
            fromDate = filter.fromDate,
            toDate = filter.toDate,
            limit = limit,
            offset = offset
        )
    }

    override suspend fun countPayables(
        principal: AuthenticatedPrincipal,
        filter: VendorPayableFilter
    ): DomainResult<Long> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId
        val effectiveVendorId = if (principal.role == UserRole.VENDOR) principal.userId else filter.vendorId

        return repository.countPayables(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = effectiveVendorId,
            status = filter.status,
            jobId = filter.jobId,
            isOverdueOnly = filter.isOverdueOnly,
            fromDate = filter.fromDate,
            toDate = filter.toDate
        )
    }

    override suspend fun getVendorPayableSummary(
        principal: AuthenticatedPrincipal,
        vendorId: String
    ): DomainResult<VendorPayableSummary> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        if (principal.role == UserRole.VENDOR && vendorId != principal.userId) {
            return DomainResult.Error(message = "Access denied: Vendors can only view their own summary.")
        }

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val listRes = repository.listPayables(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            limit = 5000,
            offset = 0
        )
        if (listRes is DomainResult.Error) return DomainResult.Error(message = listRes.message, exception = listRes.exception)
        val list = (listRes as DomainResult.Success).data

        var totalApproved = BigDecimal.ZERO
        var totalPaid = BigDecimal.ZERO
        var totalOutstanding = BigDecimal.ZERO
        var totalOverdue = BigDecimal.ZERO
        var currentDue = BigDecimal.ZERO
        var dueToday = BigDecimal.ZERO
        var upcomingDue = BigDecimal.ZERO

        var draftCount = 0
        var submittedCount = 0
        var approvedCount = 0
        var partiallyPaidCount = 0
        var paidCount = 0
        var rejectedCount = 0
        var cancelledCount = 0
        var voidedCount = 0

        val now = System.currentTimeMillis()
        val oneDayMillis = 24L * 60L * 60L * 1000L

        list.forEach { p ->
            when (p.status) {
                VendorPayableStatus.DRAFT -> draftCount++
                VendorPayableStatus.SUBMITTED -> submittedCount++
                VendorPayableStatus.APPROVED -> {
                    approvedCount++
                    totalApproved = totalApproved.add(p.originalAmount)
                    totalPaid = totalPaid.add(p.paidAmount)
                    totalOutstanding = totalOutstanding.add(p.outstandingAmount)
                    if (p.dueDate < now) {
                        totalOverdue = totalOverdue.add(p.outstandingAmount)
                    } else if (p.dueDate in now..(now + oneDayMillis)) {
                        dueToday = dueToday.add(p.outstandingAmount)
                    } else {
                        upcomingDue = upcomingDue.add(p.outstandingAmount)
                    }
                    if (p.dueDate >= now) currentDue = currentDue.add(p.outstandingAmount)
                }
                VendorPayableStatus.PARTIALLY_PAID -> {
                    partiallyPaidCount++
                    totalApproved = totalApproved.add(p.originalAmount)
                    totalPaid = totalPaid.add(p.paidAmount)
                    totalOutstanding = totalOutstanding.add(p.outstandingAmount)
                    if (p.dueDate < now) {
                        totalOverdue = totalOverdue.add(p.outstandingAmount)
                    } else if (p.dueDate in now..(now + oneDayMillis)) {
                        dueToday = dueToday.add(p.outstandingAmount)
                    } else {
                        upcomingDue = upcomingDue.add(p.outstandingAmount)
                    }
                    if (p.dueDate >= now) currentDue = currentDue.add(p.outstandingAmount)
                }
                VendorPayableStatus.PAID -> {
                    paidCount++
                    totalApproved = totalApproved.add(p.originalAmount)
                    totalPaid = totalPaid.add(p.paidAmount)
                }
                VendorPayableStatus.REJECTED -> rejectedCount++
                VendorPayableStatus.CANCELLED -> cancelledCount++
                VendorPayableStatus.VOIDED -> voidedCount++
            }
        }

        return DomainResult.Success(
            VendorPayableSummary(
                vendorId = vendorId,
                totalApprovedLiability = totalApproved.setScale(4, RoundingMode.HALF_UP),
                totalPaid = totalPaid.setScale(4, RoundingMode.HALF_UP),
                totalOutstanding = totalOutstanding.setScale(4, RoundingMode.HALF_UP),
                totalOverdue = totalOverdue.setScale(4, RoundingMode.HALF_UP),
                currentDue = currentDue.setScale(4, RoundingMode.HALF_UP),
                dueToday = dueToday.setScale(4, RoundingMode.HALF_UP),
                upcomingDue = upcomingDue.setScale(4, RoundingMode.HALF_UP),
                draftCount = draftCount,
                submittedCount = submittedCount,
                approvedCount = approvedCount,
                partiallyPaidCount = partiallyPaidCount,
                paidCount = paidCount,
                rejectedCount = rejectedCount,
                cancelledCount = cancelledCount,
                voidedCount = voidedCount,
                currency = "BDT"
            )
        )
    }

    override suspend fun getVendorPayableAging(
        principal: AuthenticatedPrincipal,
        vendorId: String?,
        asOfDate: Long
    ): DomainResult<VendorPayableAgingReport> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        val effectiveVendorId = if (principal.role == UserRole.VENDOR) principal.userId else vendorId

        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        val listRes = repository.listPayables(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = effectiveVendorId,
            limit = 5000,
            offset = 0
        )
        if (listRes is DomainResult.Error) return DomainResult.Error(message = listRes.message, exception = listRes.exception)
        val list = (listRes as DomainResult.Success).data
            .filter { it.status in setOf(VendorPayableStatus.APPROVED, VendorPayableStatus.PARTIALLY_PAID) }

        val bucketMap = VendorPayableAgingBucket.values().associateWith {
            mutableMapOf(
                "count" to 0,
                "totalAmount" to BigDecimal.ZERO,
                "outstandingAmount" to BigDecimal.ZERO
            )
        }

        var totalOutstanding = BigDecimal.ZERO

        list.forEach { p ->
            val bucket = VendorPayableAgingBucket.calculateBucket(p.dueDate, asOfDate)
            val stats = bucketMap[bucket]!!
            stats["count"] = (stats["count"] as Int) + 1
            stats["totalAmount"] = (stats["totalAmount"] as BigDecimal).add(p.originalAmount)
            stats["outstandingAmount"] = (stats["outstandingAmount"] as BigDecimal).add(p.outstandingAmount)
            totalOutstanding = totalOutstanding.add(p.outstandingAmount)
        }

        val items = VendorPayableAgingBucket.values().map { bucket ->
            val stats = bucketMap[bucket]!!
            VendorPayableAgingItem(
                bucket = bucket,
                count = stats["count"] as Int,
                totalAmount = (stats["totalAmount"] as BigDecimal).setScale(4, RoundingMode.HALF_UP),
                outstandingAmount = (stats["outstandingAmount"] as BigDecimal).setScale(4, RoundingMode.HALF_UP)
            )
        }

        return DomainResult.Success(
            VendorPayableAgingReport(
                vendorId = effectiveVendorId,
                asOfDate = asOfDate,
                buckets = items,
                totalOutstanding = totalOutstanding.setScale(4, RoundingMode.HALF_UP),
                currency = "BDT"
            )
        )
    }

    override suspend fun getPayableAuditTrail(
        principal: AuthenticatedPrincipal,
        payableId: String
    ): DomainResult<List<VendorPayableAuditEvent>> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        return repository.getAuditEvents(tenantId, projectId, payableId)
    }

    override suspend fun getPayablePaymentAllocations(
        principal: AuthenticatedPrincipal,
        payableId: String
    ): DomainResult<List<VendorPayablePaymentAllocation>> {
        assertInternalStaff(principal)?.let { return DomainResult.Error(message = it) }
        val tenantId = resolveTenantId(principal)
        val projectId = principal.projectId

        return repository.getPaymentAllocations(tenantId, projectId, payableId)
    }
}
