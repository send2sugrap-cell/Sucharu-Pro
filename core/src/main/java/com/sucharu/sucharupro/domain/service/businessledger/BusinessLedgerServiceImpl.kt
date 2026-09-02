package com.sucharu.sucharupro.domain.service.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessCostAllocationFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayableStatus
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import com.sucharu.sucharupro.domain.repository.businessledger.BusinessLedgerRepository
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseService
import com.sucharu.sucharupro.domain.service.vendorpayable.VendorPayableService
import com.sucharu.sucharupro.domain.validation.businessledger.BusinessLedgerValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.*

/**
 * Production implementation of [BusinessLedgerService] (Module 15 Step 03).
 */
class BusinessLedgerServiceImpl(
    private val repository: BusinessLedgerRepository,
    private val expenseService: BusinessExpenseService? = null,
    private val expenseRepository: BusinessExpenseRepository? = null,
    private val payableService: VendorPayableService? = null,
    private val payableRepository: VendorPayableRepository? = null,
    private val defaultTenantId: String = "TENANT-001"
) : BusinessLedgerService {

    private fun checkAccess(principal: AuthenticatedPrincipal): DomainResult<Unit> {
        return when (principal.role) {
            UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Access denied: Principal does not have permission to access the business financial ledger.")
        }
    }

    private fun checkManagerOrAdmin(principal: AuthenticatedPrincipal, action: String): DomainResult<Unit> {
        return when (principal.role) {
            UserRole.ADMIN, UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Access denied: Only Manager or Admin can perform $action.")
        }
    }

    private fun generateChecksum(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    override suspend fun postApprovedExpense(
        principal: AuthenticatedPrincipal,
        command: PostApprovedExpenseCommand
    ): DomainResult<BusinessLedgerPosting> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // Idempotency check 1: By key
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existing = repository.findPostingByIdempotencyKey(command.idempotencyKey, tenantId, projectId)
            if (existing != null) return DomainResult.Success(existing)
        }

        // Idempotency check 2: By source and posting type
        val existingSourcePosting = repository.findPostingBySourceAndType(
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = command.expenseId,
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            tenantId = tenantId,
            projectId = projectId
        )
        if (existingSourcePosting != null) {
            return DomainResult.Success(existingSourcePosting)
        }

        // Retrieve canonical expense
        val expense = when {
            expenseRepository != null -> {
                when (val r = expenseRepository.getExpenseById(tenantId, projectId, command.expenseId)) {
                    is DomainResult.Success -> r.data
                    is DomainResult.Error -> return DomainResult.Error(message = r.message)
                    DomainResult.Loading -> null
                }
            }
            expenseService != null -> {
                when (val r = expenseService.getExpenseById(principal, command.expenseId)) {
                    is DomainResult.Success -> r.data
                    is DomainResult.Error -> return DomainResult.Error(message = r.message)
                    DomainResult.Loading -> null
                }
            }
            else -> null
        } ?: return DomainResult.Error(message = "Business expense '${command.expenseId}' not found.")

        // Verify eligibility
        if (expense.status != BusinessExpenseStatus.APPROVED && expense.status != BusinessExpenseStatus.POSTABLE) {
            return DomainResult.Error(
                message = "Only approved or postable business expenses can be recognized in the ledger (current status: ${expense.status})."
            )
        }

        val accountCat = command.accountCategory ?: BusinessLedgerAccountCategory.OPERATING_EXPENSE
        val debitAmount = expense.amount.setScale(4, RoundingMode.HALF_UP)
        val creditAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        val valRes = BusinessLedgerValidator.validatePosting(
            tenantId = tenantId,
            projectId = projectId,
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = expense.expenseId,
            accountCategory = accountCat,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            currency = expense.currency,
            createdBy = principal.userId,
            description = command.description ?: "Expense Recognition: ${expense.description}"
        )
        if (valRes is DomainResult.Error) return valRes

        val postingId = "BLP-" + UUID.randomUUID().toString().take(12).uppercase()
        val postingNumber = "POST-EXP-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val now = System.currentTimeMillis()
        val checksum = generateChecksum("$postingId:$tenantId:$projectId:$debitAmount:$creditAmount:$now")

        val posting = BusinessLedgerPosting(
            id = postingId,
            tenantId = tenantId,
            projectId = projectId,
            postingNumber = postingNumber,
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = expense.expenseId,
            accountCategory = accountCat,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            currency = expense.currency,
            postingDate = now,
            effectiveDate = expense.expenseDate,
            description = command.description ?: "Expense Recognition: ${expense.description}",
            reference = command.reference ?: expense.expenseNumber,
            jobId = command.jobId ?: expense.jobId,
            vendorId = expense.vendorId,
            expenseId = expense.expenseId,
            correlationId = command.correlationId ?: "CORR-BLP-${System.currentTimeMillis()}",
            idempotencyKey = command.idempotencyKey,
            checksum = checksum,
            createdBy = principal.userId,
            createdAt = now
        )

        val saved = try {
            repository.createPosting(posting)
        } catch (e: Exception) {
            val retryExisting = repository.findPostingBySourceAndType(
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = command.expenseId,
                postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
                tenantId = tenantId,
                projectId = projectId
            )
            if (retryExisting != null) return DomainResult.Success(retryExisting)
            return DomainResult.Error(message = e.message ?: "Failed to create ledger posting.")
        }

        repository.recordAuditEvent(
            BusinessLedgerAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                eventType = "EXPENSE_RECOGNITION_POSTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = expense.expenseId,
                postingId = saved.id,
                action = "POST_EXPENSE",
                amount = debitAmount,
                reason = "Approved expense recognized in financial ledger",
                correlationId = saved.correlationId,
                idempotencyKey = command.idempotencyKey,
                checksum = saved.checksum
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun postApprovedPayable(
        principal: AuthenticatedPrincipal,
        command: PostApprovedPayableCommand
    ): DomainResult<BusinessLedgerPosting> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // Idempotency check 1: By key
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existing = repository.findPostingByIdempotencyKey(command.idempotencyKey, tenantId, projectId)
            if (existing != null) return DomainResult.Success(existing)
        }

        // Idempotency check 2: By source and posting type
        val existingSourcePosting = repository.findPostingBySourceAndType(
            sourceType = BusinessLedgerSourceType.VENDOR_PAYABLE,
            sourceId = command.payableId,
            postingType = BusinessLedgerPostingType.VENDOR_LIABILITY_RECOGNITION,
            tenantId = tenantId,
            projectId = projectId
        )
        if (existingSourcePosting != null) {
            return DomainResult.Success(existingSourcePosting)
        }

        // Retrieve canonical vendor payable
        val payable = when {
            payableRepository != null -> {
                when (val r = payableRepository.getPayableById(tenantId, projectId, command.payableId)) {
                    is DomainResult.Success -> r.data
                    is DomainResult.Error -> return DomainResult.Error(message = r.message)
                    DomainResult.Loading -> null
                }
            }
            payableService != null -> {
                when (val r = payableService.getPayableById(principal, command.payableId)) {
                    is DomainResult.Success -> r.data
                    is DomainResult.Error -> return DomainResult.Error(message = r.message)
                    DomainResult.Loading -> null
                }
            }
            else -> null
        } ?: return DomainResult.Error(message = "Vendor payable '${command.payableId}' not found.")

        // Verify eligibility
        if (payable.status !in setOf(VendorPayableStatus.APPROVED, VendorPayableStatus.PARTIALLY_PAID, VendorPayableStatus.PAID)) {
            return DomainResult.Error(
                message = "Only approved vendor payables can be recognized in the financial ledger (current status: ${payable.status})."
            )
        }

        val debitAmount = payable.originalAmount.setScale(4, RoundingMode.HALF_UP)
        val creditAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        val valRes = BusinessLedgerValidator.validatePosting(
            tenantId = tenantId,
            projectId = projectId,
            postingType = BusinessLedgerPostingType.VENDOR_LIABILITY_RECOGNITION,
            sourceType = BusinessLedgerSourceType.VENDOR_PAYABLE,
            sourceId = payable.payableId,
            accountCategory = command.accountCategory,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            currency = payable.currency,
            createdBy = principal.userId,
            description = command.description ?: "Vendor Liability Recognition: ${payable.description}"
        )
        if (valRes is DomainResult.Error) return valRes

        val postingId = "BLP-" + UUID.randomUUID().toString().take(12).uppercase()
        val postingNumber = "POST-PAY-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val now = System.currentTimeMillis()
        val checksum = generateChecksum("$postingId:$tenantId:$projectId:$debitAmount:$creditAmount:$now")

        val posting = BusinessLedgerPosting(
            id = postingId,
            tenantId = tenantId,
            projectId = projectId,
            postingNumber = postingNumber,
            postingType = BusinessLedgerPostingType.VENDOR_LIABILITY_RECOGNITION,
            sourceType = BusinessLedgerSourceType.VENDOR_PAYABLE,
            sourceId = payable.payableId,
            accountCategory = command.accountCategory,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            currency = payable.currency,
            postingDate = now,
            effectiveDate = payable.issueDate,
            description = command.description ?: "Vendor Liability Recognition: ${payable.description}",
            reference = command.reference ?: payable.payableNumber,
            jobId = command.jobId ?: payable.jobId,
            vendorId = payable.vendorId,
            payableId = payable.payableId,
            correlationId = command.correlationId ?: "CORR-BLP-${System.currentTimeMillis()}",
            idempotencyKey = command.idempotencyKey,
            checksum = checksum,
            createdBy = principal.userId,
            createdAt = now
        )

        val saved = try {
            repository.createPosting(posting)
        } catch (e: Exception) {
            val retryExisting = repository.findPostingBySourceAndType(
                sourceType = BusinessLedgerSourceType.VENDOR_PAYABLE,
                sourceId = command.payableId,
                postingType = BusinessLedgerPostingType.VENDOR_LIABILITY_RECOGNITION,
                tenantId = tenantId,
                projectId = projectId
            )
            if (retryExisting != null) return DomainResult.Success(retryExisting)
            return DomainResult.Error(message = e.message ?: "Failed to create ledger posting.")
        }

        repository.recordAuditEvent(
            BusinessLedgerAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                eventType = "VENDOR_LIABILITY_POSTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                sourceType = BusinessLedgerSourceType.VENDOR_PAYABLE,
                sourceId = payable.payableId,
                postingId = saved.id,
                action = "POST_VENDOR_PAYABLE",
                amount = debitAmount,
                reason = "Vendor liability recognized in financial ledger",
                correlationId = saved.correlationId,
                idempotencyKey = command.idempotencyKey,
                checksum = saved.checksum
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun postVendorPayment(
        principal: AuthenticatedPrincipal,
        command: PostVendorPaymentCommand
    ): DomainResult<BusinessLedgerPosting> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // Idempotency check
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existing = repository.findPostingByIdempotencyKey(command.idempotencyKey, tenantId, projectId)
            if (existing != null) return DomainResult.Success(existing)
        }

        val existingAllocationPosting = repository.findPostingBySourceAndType(
            sourceType = BusinessLedgerSourceType.VENDOR_PAYMENT,
            sourceId = command.allocationId,
            postingType = BusinessLedgerPostingType.VENDOR_PAYMENT,
            tenantId = tenantId,
            projectId = projectId
        )
        if (existingAllocationPosting != null) {
            return DomainResult.Success(existingAllocationPosting)
        }

        val payable = when {
            payableRepository != null -> {
                when (val r = payableRepository.getPayableById(tenantId, projectId, command.payableId)) {
                    is DomainResult.Success -> r.data
                    is DomainResult.Error -> return DomainResult.Error(message = r.message)
                    DomainResult.Loading -> null
                }
            }
            payableService != null -> {
                when (val r = payableService.getPayableById(principal, command.payableId)) {
                    is DomainResult.Success -> r.data
                    is DomainResult.Error -> return DomainResult.Error(message = r.message)
                    DomainResult.Loading -> null
                }
            }
            else -> null
        } ?: return DomainResult.Error(message = "Vendor payable '${command.payableId}' not found.")

        val debitAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val creditAmount = command.amount.setScale(4, RoundingMode.HALF_UP)

        val valRes = BusinessLedgerValidator.validatePosting(
            tenantId = tenantId,
            projectId = projectId,
            postingType = BusinessLedgerPostingType.VENDOR_PAYMENT,
            sourceType = BusinessLedgerSourceType.VENDOR_PAYMENT,
            sourceId = command.allocationId,
            accountCategory = command.accountCategory,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            currency = command.currency,
            createdBy = principal.userId,
            description = command.description ?: "Vendor Payment Settlement for ${payable.payableNumber}"
        )
        if (valRes is DomainResult.Error) return valRes

        val postingId = "BLP-" + UUID.randomUUID().toString().take(12).uppercase()
        val postingNumber = "POST-VPM-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val now = System.currentTimeMillis()
        val checksum = generateChecksum("$postingId:$tenantId:$projectId:$debitAmount:$creditAmount:$now")

        val posting = BusinessLedgerPosting(
            id = postingId,
            tenantId = tenantId,
            projectId = projectId,
            postingNumber = postingNumber,
            postingType = BusinessLedgerPostingType.VENDOR_PAYMENT,
            sourceType = BusinessLedgerSourceType.VENDOR_PAYMENT,
            sourceId = command.allocationId,
            accountCategory = command.accountCategory,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            currency = command.currency,
            postingDate = now,
            effectiveDate = command.paymentDate,
            description = command.description ?: "Vendor Payment Settlement for ${payable.payableNumber}",
            reference = command.paymentReference ?: payable.payableNumber,
            jobId = payable.jobId,
            vendorId = payable.vendorId,
            payableId = payable.payableId,
            allocationId = command.allocationId,
            correlationId = command.correlationId ?: "CORR-BLP-${System.currentTimeMillis()}",
            idempotencyKey = command.idempotencyKey,
            checksum = checksum,
            createdBy = principal.userId,
            createdAt = now
        )

        val saved = try {
            repository.createPosting(posting)
        } catch (e: Exception) {
            val retryExisting = repository.findPostingBySourceAndType(
                sourceType = BusinessLedgerSourceType.VENDOR_PAYMENT,
                sourceId = command.allocationId,
                postingType = BusinessLedgerPostingType.VENDOR_PAYMENT,
                tenantId = tenantId,
                projectId = projectId
            )
            if (retryExisting != null) return DomainResult.Success(retryExisting)
            return DomainResult.Error(message = e.message ?: "Failed to create payment posting.")
        }

        repository.recordAuditEvent(
            BusinessLedgerAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                eventType = "VENDOR_PAYMENT_POSTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                sourceType = BusinessLedgerSourceType.VENDOR_PAYMENT,
                sourceId = command.allocationId,
                postingId = saved.id,
                action = "POST_VENDOR_PAYMENT",
                amount = creditAmount,
                reason = "Vendor payment settlement registered in business ledger",
                correlationId = saved.correlationId,
                idempotencyKey = command.idempotencyKey,
                checksum = saved.checksum
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun postBusinessAdjustment(
        principal: AuthenticatedPrincipal,
        command: PostBusinessAdjustmentCommand
    ): DomainResult<BusinessLedgerPosting> {
        val access = checkManagerOrAdmin(principal, "manual business adjustments")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!command.idempotencyKey.isNullOrBlank()) {
            val existing = repository.findPostingByIdempotencyKey(command.idempotencyKey, tenantId, projectId)
            if (existing != null) return DomainResult.Success(existing)
        }

        val debitAmount = if (command.isDebit) command.amount.setScale(4, RoundingMode.HALF_UP) else BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val creditAmount = if (!command.isDebit) command.amount.setScale(4, RoundingMode.HALF_UP) else BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        val adjustmentSourceId = "ADJ-" + UUID.randomUUID().toString().take(10).uppercase()

        val valRes = BusinessLedgerValidator.validatePosting(
            tenantId = tenantId,
            projectId = projectId,
            postingType = BusinessLedgerPostingType.ADJUSTMENT,
            sourceType = BusinessLedgerSourceType.BUSINESS_ADJUSTMENT,
            sourceId = adjustmentSourceId,
            accountCategory = command.accountCategory,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            currency = command.currency,
            createdBy = principal.userId,
            description = command.description
        )
        if (valRes is DomainResult.Error) return valRes

        val postingId = "BLP-" + UUID.randomUUID().toString().take(12).uppercase()
        val postingNumber = "POST-ADJ-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val now = System.currentTimeMillis()
        val checksum = generateChecksum("$postingId:$tenantId:$projectId:$debitAmount:$creditAmount:$now")

        val posting = BusinessLedgerPosting(
            id = postingId,
            tenantId = tenantId,
            projectId = projectId,
            postingNumber = postingNumber,
            postingType = BusinessLedgerPostingType.ADJUSTMENT,
            sourceType = BusinessLedgerSourceType.BUSINESS_ADJUSTMENT,
            sourceId = adjustmentSourceId,
            accountCategory = command.accountCategory,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            currency = command.currency,
            postingDate = now,
            effectiveDate = now,
            description = command.description,
            reference = command.reference,
            jobId = command.jobId,
            vendorId = command.vendorId,
            correlationId = command.correlationId ?: "CORR-BLP-${System.currentTimeMillis()}",
            idempotencyKey = command.idempotencyKey,
            checksum = checksum,
            createdBy = principal.userId,
            createdAt = now
        )

        val saved = try {
            repository.createPosting(posting)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to create adjustment posting.")
        }

        repository.recordAuditEvent(
            BusinessLedgerAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                eventType = "BUSINESS_ADJUSTMENT_POSTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                sourceType = BusinessLedgerSourceType.BUSINESS_ADJUSTMENT,
                sourceId = adjustmentSourceId,
                postingId = saved.id,
                action = "POST_ADJUSTMENT",
                amount = command.amount,
                reason = command.description,
                correlationId = saved.correlationId,
                idempotencyKey = command.idempotencyKey,
                checksum = saved.checksum
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun reversePosting(
        principal: AuthenticatedPrincipal,
        command: ReversePostingCommand
    ): DomainResult<BusinessLedgerPosting> {
        val access = checkManagerOrAdmin(principal, "reversing financial ledger postings")
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val original = repository.findPostingById(command.postingId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Ledger posting '${command.postingId}' not found.")

        val revVal = BusinessLedgerValidator.validateReversal(original, command.reason, principal.userId)
        if (revVal is DomainResult.Error) return revVal

        // Construct compensating reversal entry: swap debit and credit
        val reversalDebit = original.creditAmount
        val reversalCredit = original.debitAmount

        val reversalId = "BLP-" + UUID.randomUUID().toString().take(12).uppercase()
        val reversalNumber = "POST-REV-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val now = System.currentTimeMillis()
        val checksum = generateChecksum("$reversalId:$tenantId:$projectId:$reversalDebit:$reversalCredit:$now")

        val reversalPosting = BusinessLedgerPosting(
            id = reversalId,
            tenantId = tenantId,
            projectId = projectId,
            postingNumber = reversalNumber,
            postingType = BusinessLedgerPostingType.REVERSAL,
            sourceType = original.sourceType,
            sourceId = original.sourceId,
            accountCategory = original.accountCategory,
            debitAmount = reversalDebit,
            creditAmount = reversalCredit,
            currency = original.currency,
            postingDate = now,
            effectiveDate = now,
            description = "Compensating Reversal for ${original.postingNumber}: ${command.reason}",
            reference = original.postingNumber,
            jobId = original.jobId,
            vendorId = original.vendorId,
            expenseId = original.expenseId,
            payableId = original.payableId,
            allocationId = original.allocationId,
            reversalOfPostingId = original.id,
            correlationId = command.correlationId ?: "CORR-REV-${System.currentTimeMillis()}",
            checksum = checksum,
            createdBy = principal.userId,
            createdAt = now
        )

        // Mark original posting as reversed
        val marked = repository.markPostingReversed(
            id = original.id,
            reversalReason = command.reason,
            reversedBy = principal.userId,
            reversedAt = now,
            reversalPostingId = reversalId
        )
        if (!marked) {
            return DomainResult.Error(message = "Posting was already reversed by another transaction.")
        }

        val savedReversal = repository.createPosting(reversalPosting)

        repository.recordAuditEvent(
            BusinessLedgerAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                eventType = "POSTING_REVERSED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                sourceType = original.sourceType,
                sourceId = original.sourceId,
                postingId = original.id,
                action = "REVERSE_POSTING",
                previousState = "POSTED",
                newState = "REVERSED",
                amount = original.debitAmount.max(original.creditAmount),
                reason = command.reason,
                correlationId = command.correlationId,
                checksum = savedReversal.checksum
            )
        )

        return DomainResult.Success(savedReversal)
    }

    override suspend fun allocateCost(
        principal: AuthenticatedPrincipal,
        command: AllocateCostCommand
    ): DomainResult<BusinessCostAllocation> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!command.idempotencyKey.isNullOrBlank()) {
            val existing = repository.findCostAllocationByIdempotencyKey(command.idempotencyKey, tenantId, projectId)
            if (existing != null) return DomainResult.Success(existing)
        }

        // Determine source total amount
        val sourceTotalAmount: BigDecimal = when (command.sourceType) {
            BusinessLedgerSourceType.BUSINESS_EXPENSE -> {
                val exp = when {
                    expenseRepository != null -> {
                        when (val r = expenseRepository.getExpenseById(tenantId, projectId, command.sourceId)) {
                            is DomainResult.Success -> r.data
                            is DomainResult.Error -> return DomainResult.Error(message = r.message)
                            DomainResult.Loading -> null
                        }
                    }
                    expenseService != null -> (expenseService.getExpenseById(principal, command.sourceId) as? DomainResult.Success)?.data
                    else -> null
                } ?: return DomainResult.Error(message = "Source expense '${command.sourceId}' not found.")
                exp.amount
            }
            BusinessLedgerSourceType.VENDOR_PAYABLE -> {
                val pay = when {
                    payableRepository != null -> {
                        when (val r = payableRepository.getPayableById(tenantId, projectId, command.sourceId)) {
                            is DomainResult.Success -> r.data
                            is DomainResult.Error -> return DomainResult.Error(message = r.message)
                            DomainResult.Loading -> null
                        }
                    }
                    payableService != null -> (payableService.getPayableById(principal, command.sourceId) as? DomainResult.Success)?.data
                    else -> null
                } ?: return DomainResult.Error(message = "Source payable '${command.sourceId}' not found.")
                pay.originalAmount
            }
            else -> {
                return DomainResult.Error(message = "Cost allocation is only supported for Business Expenses and Vendor Payables.")
            }
        }.setScale(4, RoundingMode.HALF_UP)

        // Calculate existing active allocations
        val existingAllocations = repository.listCostAllocations(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessCostAllocationFilter(
                sourceType = command.sourceType,
                sourceId = command.sourceId,
                isReversed = false,
                limit = 1000
            )
        )
        val existingAllocatedSum = existingAllocations.fold(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)) { acc, a ->
            acc.add(a.allocatedAmount)
        }

        val valRes = BusinessLedgerValidator.validateCostAllocation(
            tenantId = tenantId,
            projectId = projectId,
            sourceType = command.sourceType,
            sourceId = command.sourceId,
            jobId = command.jobId,
            allocatedAmount = command.allocatedAmount.setScale(4, RoundingMode.HALF_UP),
            sourceTotalAmount = sourceTotalAmount,
            existingAllocatedAmount = existingAllocatedSum,
            currency = command.currency,
            createdBy = principal.userId
        )
        if (valRes is DomainResult.Error) return valRes

        val allocationId = "BCA-" + UUID.randomUUID().toString().take(12).uppercase()
        val allocationNumber = "ALLOC-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val now = System.currentTimeMillis()

        val allocation = BusinessCostAllocation(
            id = allocationId,
            tenantId = tenantId,
            projectId = projectId,
            allocationNumber = allocationNumber,
            sourceType = command.sourceType,
            sourceId = command.sourceId,
            ledgerPostingId = command.ledgerPostingId,
            jobId = command.jobId,
            vendorId = command.vendorId,
            costCategory = command.costCategory,
            allocatedAmount = command.allocatedAmount.setScale(4, RoundingMode.HALF_UP),
            currency = command.currency,
            allocationDate = now,
            reason = command.reason,
            correlationId = command.correlationId ?: "CORR-BCA-${System.currentTimeMillis()}",
            idempotencyKey = command.idempotencyKey,
            createdBy = principal.userId,
            createdAt = now
        )

        val saved = try {
            repository.createCostAllocation(allocation)
        } catch (e: Exception) {
            return DomainResult.Error(message = e.message ?: "Failed to record cost allocation.")
        }

        repository.recordAuditEvent(
            BusinessLedgerAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                eventType = "COST_ALLOCATED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                sourceType = command.sourceType,
                sourceId = command.sourceId,
                allocationId = saved.id,
                action = "ALLOCATE_COST",
                amount = command.allocatedAmount,
                reason = command.reason ?: "Attributed cost to Job ${command.jobId}",
                correlationId = saved.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun reverseCostAllocation(
        principal: AuthenticatedPrincipal,
        command: ReverseCostAllocationCommand
    ): DomainResult<BusinessCostAllocation> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val allocation = repository.findCostAllocationById(command.allocationId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Cost allocation '${command.allocationId}' not found.")

        val valRes = BusinessLedgerValidator.validateAllocationReversal(allocation, command.reason, principal.userId)
        if (valRes is DomainResult.Error) return valRes

        val now = System.currentTimeMillis()
        val success = repository.markCostAllocationReversed(
            id = allocation.id,
            reversalReason = command.reason,
            reversedBy = principal.userId,
            reversedAt = now
        )
        if (!success) {
            return DomainResult.Error(message = "Allocation was already reversed by another transaction.")
        }

        val updated = allocation.copy(
            isReversed = true,
            reversalReason = command.reason,
            reversedBy = principal.userId,
            reversedAt = now,
            version = allocation.version + 1
        )

        repository.recordAuditEvent(
            BusinessLedgerAuditEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = tenantId,
                projectId = projectId,
                eventType = "COST_ALLOCATION_REVERSED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                sourceType = allocation.sourceType,
                sourceId = allocation.sourceId,
                allocationId = allocation.id,
                action = "REVERSE_ALLOCATION",
                previousState = "ACTIVE",
                newState = "REVERSED",
                amount = allocation.allocatedAmount,
                reason = command.reason,
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getPostingById(
        principal: AuthenticatedPrincipal,
        postingId: String
    ): DomainResult<BusinessLedgerPosting> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val posting = repository.findPostingById(postingId, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Ledger posting '$postingId' not found.")
        return DomainResult.Success(posting)
    }

    override suspend fun getPostingByNumber(
        principal: AuthenticatedPrincipal,
        postingNumber: String
    ): DomainResult<BusinessLedgerPosting> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val posting = repository.findPostingByNumber(postingNumber, defaultTenantId, principal.projectId)
            ?: return DomainResult.Error(message = "Ledger posting number '$postingNumber' not found.")
        return DomainResult.Success(posting)
    }

    override suspend fun listPostings(
        principal: AuthenticatedPrincipal,
        filter: BusinessLedgerPostingFilter
    ): DomainResult<List<BusinessLedgerPosting>> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listPostings(defaultTenantId, principal.projectId, filter)
        return DomainResult.Success(list)
    }

    override suspend fun getPostingsBySource(
        principal: AuthenticatedPrincipal,
        sourceType: BusinessLedgerSourceType,
        sourceId: String
    ): DomainResult<List<BusinessLedgerPosting>> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.findPostingsBySource(sourceType, sourceId, defaultTenantId, principal.projectId)
        return DomainResult.Success(list)
    }

    override suspend fun getBalanceSummary(
        principal: AuthenticatedPrincipal,
        asOfTimestamp: Long
    ): DomainResult<BusinessLedgerBalanceSummary> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val summary = repository.calculateBalanceSummary(defaultTenantId, principal.projectId, asOfTimestamp)
        return DomainResult.Success(summary)
    }

    override suspend fun getPeriodSummary(
        principal: AuthenticatedPrincipal,
        fromDate: Long,
        toDate: Long
    ): DomainResult<BusinessLedgerPeriodSummary> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        if (fromDate > toDate) {
            return DomainResult.Error(message = "fromDate ($fromDate) cannot be greater than toDate ($toDate).")
        }

        val summary = repository.calculatePeriodSummary(defaultTenantId, principal.projectId, fromDate, toDate)
        return DomainResult.Success(summary)
    }

    override suspend fun listCostAllocations(
        principal: AuthenticatedPrincipal,
        filter: BusinessCostAllocationFilter
    ): DomainResult<List<BusinessCostAllocation>> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val list = repository.listCostAllocations(defaultTenantId, principal.projectId, filter)
        return DomainResult.Success(list)
    }

    override suspend fun getJobCostSummary(
        principal: AuthenticatedPrincipal,
        jobId: String
    ): DomainResult<BusinessJobCostSummary> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val allocations = repository.listCostAllocations(
            tenantId = defaultTenantId,
            projectId = principal.projectId,
            filter = BusinessCostAllocationFilter(jobId = jobId, isReversed = false, limit = 1000)
        )

        var total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val breakdown = mutableMapOf<String, BigDecimal>()

        for (a in allocations) {
            total = total.add(a.allocatedAmount)
            val cat = a.costCategory.name
            breakdown[cat] = breakdown.getOrDefault(cat, BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)).add(a.allocatedAmount)
        }

        return DomainResult.Success(
            BusinessJobCostSummary(
                jobId = jobId,
                totalAllocatedCost = total,
                allocationCount = allocations.size,
                currency = "BDT",
                breakdownByCategory = breakdown,
                allocations = allocations
            )
        )
    }

    override suspend fun getUnallocatedCostSummary(
        principal: AuthenticatedPrincipal,
        sourceType: BusinessLedgerSourceType,
        sourceId: String
    ): DomainResult<BusinessUnallocatedCostSummary> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val totalSourceAmount = when (sourceType) {
            BusinessLedgerSourceType.BUSINESS_EXPENSE -> {
                val exp = when {
                    expenseRepository != null -> {
                        when (val r = expenseRepository.getExpenseById(tenantId, projectId, sourceId)) {
                            is DomainResult.Success -> r.data
                            is DomainResult.Error -> return DomainResult.Error(message = r.message)
                            DomainResult.Loading -> null
                        }
                    }
                    expenseService != null -> (expenseService.getExpenseById(principal, sourceId) as? DomainResult.Success)?.data
                    else -> null
                } ?: return DomainResult.Error(message = "Source expense '$sourceId' not found.")
                exp.amount
            }
            BusinessLedgerSourceType.VENDOR_PAYABLE -> {
                val pay = when {
                    payableRepository != null -> {
                        when (val r = payableRepository.getPayableById(tenantId, projectId, sourceId)) {
                            is DomainResult.Success -> r.data
                            is DomainResult.Error -> return DomainResult.Error(message = r.message)
                            DomainResult.Loading -> null
                        }
                    }
                    payableService != null -> (payableService.getPayableById(principal, sourceId) as? DomainResult.Success)?.data
                    else -> null
                } ?: return DomainResult.Error(message = "Source payable '$sourceId' not found.")
                pay.originalAmount
            }
            else -> return DomainResult.Error(message = "Unallocated summary only supports Expense or Payable.")
        }.setScale(4, RoundingMode.HALF_UP)

        val allocations = repository.listCostAllocations(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessCostAllocationFilter(sourceType = sourceType, sourceId = sourceId, isReversed = false, limit = 1000)
        )

        val allocatedAmount = allocations.fold(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)) { acc, a ->
            acc.add(a.allocatedAmount)
        }

        val unallocated = totalSourceAmount.subtract(allocatedAmount).max(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
        val percentage = if (totalSourceAmount > BigDecimal.ZERO) {
            allocatedAmount.multiply(BigDecimal("100")).divide(totalSourceAmount, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        return DomainResult.Success(
            BusinessUnallocatedCostSummary(
                sourceType = sourceType,
                sourceId = sourceId,
                totalSourceAmount = totalSourceAmount,
                allocatedAmount = allocatedAmount,
                unallocatedAmount = unallocated,
                allocationPercentage = percentage,
                currency = "BDT"
            )
        )
    }

    override suspend fun getCostAllocationSummary(
        principal: AuthenticatedPrincipal
    ): DomainResult<BusinessCostAllocationSummary> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val allAllocations = repository.listCostAllocations(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessCostAllocationFilter(isReversed = false, limit = 5000)
        )

        val jobGroups = allAllocations.groupBy { it.jobId }
        var grandTotalAllocated = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val jobSummaries = mutableListOf<BusinessJobCostSummary>()

        for ((jobId, jobAllocs) in jobGroups) {
            var jobTotal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
            val breakdown = mutableMapOf<String, BigDecimal>()
            for (a in jobAllocs) {
                jobTotal = jobTotal.add(a.allocatedAmount)
                val cat = a.costCategory.name
                breakdown[cat] = breakdown.getOrDefault(cat, BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)).add(a.allocatedAmount)
            }
            grandTotalAllocated = grandTotalAllocated.add(jobTotal)
            jobSummaries.add(
                BusinessJobCostSummary(
                    jobId = jobId,
                    totalAllocatedCost = jobTotal,
                    allocationCount = jobAllocs.size,
                    currency = "BDT",
                    breakdownByCategory = breakdown,
                    allocations = jobAllocs
                )
            )
        }

        return DomainResult.Success(
            BusinessCostAllocationSummary(
                totalAllocated = grandTotalAllocated,
                totalUnallocated = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                jobCount = jobSummaries.size,
                currency = "BDT",
                jobSummaries = jobSummaries
            )
        )
    }

    override suspend fun getAuditTrail(
        principal: AuthenticatedPrincipal,
        sourceId: String?,
        postingId: String?,
        allocationId: String?
    ): DomainResult<List<BusinessLedgerAuditEvent>> {
        val access = checkAccess(principal)
        if (access is DomainResult.Error) return access

        val audits = repository.listAuditEvents(
            tenantId = defaultTenantId,
            projectId = principal.projectId,
            sourceId = sourceId,
            postingId = postingId,
            allocationId = allocationId
        )
        return DomainResult.Success(audits)
    }
}
