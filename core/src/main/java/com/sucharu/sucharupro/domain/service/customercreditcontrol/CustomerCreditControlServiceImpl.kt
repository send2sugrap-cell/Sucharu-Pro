package com.sucharu.sucharupro.domain.service.customercreditcontrol

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customercreditcontrol.*
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.repository.customercreditcontrol.CustomerCreditControlRepository
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementService
import com.sucharu.sucharupro.domain.validation.customercreditcontrol.CustomerCreditControlValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Production implementation of Customer Credit Limits, Payment Terms & Receivable Risk Control (Module 14 Step 07).
 */
class CustomerCreditControlServiceImpl(
    private val creditControlRepository: CustomerCreditControlRepository,
    private val customerRepository: CustomerRepository,
    private val accountRepository: CustomerFinancialAccountRepository,
    private val settlementService: CustomerSettlementService,
    private val invoiceRepository: CustomerInvoiceRepository
) : CustomerCreditControlService {

    private suspend fun validateTenantCustomer(tenantId: String, projectId: String, customerId: String): DomainResult<Unit> {
        val custRes = customerRepository.findCustomerById(customerId)
        if (custRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' not found."))
        }
        val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, customerId)
        if (accountRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' does not belong to tenant '$tenantId' / project '$projectId'."))
        }
        return DomainResult.Success(Unit)
    }

    override suspend fun getOrCreateCreditProfile(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditProfileEntity> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val existing = creditControlRepository.getProfileByCustomerId(tenantId, projectId, customerId)
        if (existing is DomainResult.Success && existing.data != null) {
            return DomainResult.Success(existing.data)
        }

        val now = System.currentTimeMillis()
        val defaultProfile = CustomerCreditProfileEntity(
            profileId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            creditLimit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            currency = "BDT",
            paymentTermsType = CustomerPaymentTermsType.DUE_ON_RECEIPT,
            creditDays = 0,
            requiresAdvance = true,
            financialHold = false,
            createdAt = now,
            createdBy = "system",
            updatedAt = now,
            updatedBy = "system",
            version = 1
        )
        return creditControlRepository.saveProfile(defaultProfile)
    }

    override suspend fun updateCreditProfile(
        tenantId: String,
        projectId: String,
        customerId: String,
        creditLimit: BigDecimal,
        currency: String,
        paymentTermsType: CustomerPaymentTermsType,
        creditDays: Int,
        requiresAdvance: Boolean,
        notes: String?,
        actorId: String,
        actorRole: String,
        reason: String
    ): DomainResult<CustomerCreditProfileEntity> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = if (custRes is DomainResult.Success) custRes.data else null
        val valRes = CustomerCreditControlValidator.validateProfileCreationOrUpdate(
            customer, creditLimit, paymentTermsType, creditDays
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(valRes.exception, valRes.message)
        }

        val existingRes = getOrCreateCreditProfile(tenantId, projectId, customerId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val now = System.currentTimeMillis()
        val calculatedCreditDays = if (paymentTermsType != CustomerPaymentTermsType.CUSTOM) {
            paymentTermsType.defaultDays
        } else creditDays

        val updated = existing.copy(
            creditLimit = creditLimit.setScale(4, RoundingMode.HALF_UP),
            currency = currency,
            paymentTermsType = paymentTermsType,
            creditDays = calculatedCreditDays,
            requiresAdvance = requiresAdvance,
            notes = notes,
            updatedAt = now,
            updatedBy = actorId,
            version = existing.version + 1
        )

        val saveRes = creditControlRepository.saveProfile(updated)
        if (saveRes is DomainResult.Success) {
            val auditEvent = CustomerCreditControlAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                actorId = actorId,
                actorRole = actorRole,
                action = "UPDATE_CREDIT_PROFILE",
                previousValueJson = "{\"creditLimit\": ${existing.creditLimit}, \"terms\": \"${existing.paymentTermsType}\", \"days\": ${existing.creditDays}, \"requiresAdvance\": ${existing.requiresAdvance}}",
                newValueJson = "{\"creditLimit\": ${updated.creditLimit}, \"terms\": \"${updated.paymentTermsType}\", \"days\": ${updated.creditDays}, \"requiresAdvance\": ${updated.requiresAdvance}}",
                reason = reason,
                occurredAt = now
            )
            creditControlRepository.recordAuditEvent(auditEvent)
        }
        return saveRes
    }

    override suspend fun placeFinancialHold(
        tenantId: String,
        projectId: String,
        customerId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCreditProfileEntity> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = if (custRes is DomainResult.Success) custRes.data else null
        val valRes = CustomerCreditControlValidator.validateFinancialHold(customer, reason, actorId)
        if (valRes is DomainResult.Error) return DomainResult.Error(valRes.exception, valRes.message)

        val profileRes = getOrCreateCreditProfile(tenantId, projectId, customerId)
        if (profileRes is DomainResult.Error) return profileRes
        val profile = (profileRes as DomainResult.Success).data

        val now = System.currentTimeMillis()
        val updated = profile.copy(
            financialHold = true,
            holdReason = reason,
            holdPlacedAt = now,
            holdPlacedBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = profile.version + 1
        )

        val saveRes = creditControlRepository.saveProfile(updated)
        if (saveRes is DomainResult.Success) {
            val auditEvent = CustomerCreditControlAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                actorId = actorId,
                actorRole = actorRole,
                action = "PLACE_FINANCIAL_HOLD",
                previousValueJson = "{\"financialHold\": false}",
                newValueJson = "{\"financialHold\": true, \"reason\": \"$reason\"}",
                reason = reason,
                occurredAt = now
            )
            creditControlRepository.recordAuditEvent(auditEvent)
        }
        return saveRes
    }

    override suspend fun releaseFinancialHold(
        tenantId: String,
        projectId: String,
        customerId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCreditProfileEntity> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = if (custRes is DomainResult.Success) custRes.data else null
        val profileRes = getOrCreateCreditProfile(tenantId, projectId, customerId)
        if (profileRes is DomainResult.Error) return profileRes
        val profile = (profileRes as DomainResult.Success).data

        val valRes = CustomerCreditControlValidator.validateHoldRelease(customer, profile.financialHold, reason, actorId)
        if (valRes is DomainResult.Error) return DomainResult.Error(valRes.exception, valRes.message)

        val now = System.currentTimeMillis()
        val updated = profile.copy(
            financialHold = false,
            holdReason = null,
            holdPlacedAt = null,
            holdPlacedBy = null,
            updatedAt = now,
            updatedBy = actorId,
            version = profile.version + 1
        )

        val saveRes = creditControlRepository.saveProfile(updated)
        if (saveRes is DomainResult.Success) {
            val auditEvent = CustomerCreditControlAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                actorId = actorId,
                actorRole = actorRole,
                action = "RELEASE_FINANCIAL_HOLD",
                previousValueJson = "{\"financialHold\": true, \"previousReason\": \"${profile.holdReason}\"}",
                newValueJson = "{\"financialHold\": false}",
                reason = reason,
                occurredAt = now
            )
            creditControlRepository.recordAuditEvent(auditEvent)
        }
        return saveRes
    }

    override suspend fun evaluateCredit(
        tenantId: String,
        projectId: String,
        request: CustomerCreditCheckRequest
    ): DomainResult<CustomerCreditCheckResult> {
        val check = validateTenantCustomer(tenantId, projectId, request.customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(request.customerId)
        val customer = if (custRes is DomainResult.Success) custRes.data else null
        val valRes = CustomerCreditControlValidator.validateCreditCheck(customer, request.requestedExposure)
        if (valRes is DomainResult.Error) return DomainResult.Error(valRes.exception, valRes.message)

        val profileRes = getOrCreateCreditProfile(tenantId, projectId, request.customerId)
        if (profileRes is DomainResult.Error) return DomainResult.Error(profileRes.exception, profileRes.message)
        val profile = (profileRes as DomainResult.Success).data

        // Fetch authoritative settlement summary
        val summaryRes = settlementService.getCustomerSettlementSummary(tenantId, projectId, request.customerId)
        val summary = if (summaryRes is DomainResult.Success) summaryRes.data else null

        val currentOutstanding = summary?.totalOutstanding ?: BigDecimal.ZERO
        val unallocated = summary?.totalUnallocated ?: BigDecimal.ZERO
        val availableCreditBalance = summary?.totalAvailableCredit ?: BigDecimal.ZERO

        // Net Receivable Exposure = totalOutstanding - unallocated - availableCredit
        val netExposure = currentOutstanding.subtract(unallocated).subtract(availableCreditBalance).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val creditLimit = profile.creditLimit.setScale(4, RoundingMode.HALF_UP)
        val availableCredit = creditLimit.subtract(netExposure).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)

        val requested = request.requestedExposure.setScale(4, RoundingMode.HALF_UP)
        val projectedExposure = netExposure.add(requested).setScale(4, RoundingMode.HALF_UP)

        // Determine Risk Status
        val riskStatus = calculateRiskStatus(profile, netExposure, creditLimit, summary?.overdueInvoiceCount ?: 0)

        // 1. Financial Hold check
        if (profile.financialHold) {
            return DomainResult.Success(
                CustomerCreditCheckResult(
                    customerId = request.customerId,
                    approved = false,
                    creditLimit = creditLimit,
                    currentExposure = netExposure,
                    availableCredit = availableCredit,
                    requestedExposure = requested,
                    projectedExposure = projectedExposure,
                    riskStatus = CustomerCreditRiskStatus.FINANCIAL_HOLD,
                    reason = "Customer is on financial hold: ${profile.holdReason ?: "Account restricted"}",
                    failureCode = "FINANCIAL_HOLD"
                )
            )
        }

        // 2. Advance Required check
        if (profile.requiresAdvance && requested > BigDecimal.ZERO) {
            return DomainResult.Success(
                CustomerCreditCheckResult(
                    customerId = request.customerId,
                    approved = false,
                    creditLimit = creditLimit,
                    currentExposure = netExposure,
                    availableCredit = availableCredit,
                    requestedExposure = requested,
                    projectedExposure = projectedExposure,
                    riskStatus = CustomerCreditRiskStatus.ADVANCE_REQUIRED,
                    reason = "Customer is marked as Advance Required. Credit cannot be extended without advance payment.",
                    failureCode = "ADVANCE_REQUIRED"
                )
            )
        }

        // 3. Zero Credit Limit check
        if (creditLimit == BigDecimal.ZERO && requested > BigDecimal.ZERO) {
            return DomainResult.Success(
                CustomerCreditCheckResult(
                    customerId = request.customerId,
                    approved = false,
                    creditLimit = creditLimit,
                    currentExposure = netExposure,
                    availableCredit = availableCredit,
                    requestedExposure = requested,
                    projectedExposure = projectedExposure,
                    riskStatus = CustomerCreditRiskStatus.LIMIT_REACHED,
                    reason = "Customer has a zero credit limit. Advance/prepaid payment required.",
                    failureCode = "ZERO_CREDIT_LIMIT"
                )
            )
        }

        // 4. Over Limit Check
        if (projectedExposure > creditLimit) {
            return DomainResult.Success(
                CustomerCreditCheckResult(
                    customerId = request.customerId,
                    approved = false,
                    creditLimit = creditLimit,
                    currentExposure = netExposure,
                    availableCredit = availableCredit,
                    requestedExposure = requested,
                    projectedExposure = projectedExposure,
                    riskStatus = CustomerCreditRiskStatus.OVER_LIMIT,
                    reason = "Requested exposure ($requested) exceeds available credit limit ($availableCredit). Projected exposure: $projectedExposure, Limit: $creditLimit.",
                    failureCode = "CREDIT_LIMIT_EXCEEDED"
                )
            )
        }

        return DomainResult.Success(
            CustomerCreditCheckResult(
                customerId = request.customerId,
                approved = true,
                creditLimit = creditLimit,
                currentExposure = netExposure,
                availableCredit = availableCredit,
                requestedExposure = requested,
                projectedExposure = projectedExposure,
                riskStatus = riskStatus,
                reason = "Credit check approved. Available credit after transaction: ${creditLimit.subtract(projectedExposure)}."
            )
        )
    }

    override suspend fun getReceivableRiskSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerReceivableRiskSummary> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val profileRes = getOrCreateCreditProfile(tenantId, projectId, customerId)
        if (profileRes is DomainResult.Error) return DomainResult.Error(profileRes.exception, profileRes.message)
        val profile = (profileRes as DomainResult.Success).data

        val summaryRes = settlementService.getCustomerSettlementSummary(tenantId, projectId, customerId)
        if (summaryRes is DomainResult.Error) return DomainResult.Error(summaryRes.exception, summaryRes.message)
        val summary = (summaryRes as DomainResult.Success).data

        // Invoices for overdue calculation
        val invoicesRes = invoiceRepository.listInvoices(tenantId, projectId, customerId = customerId, limit = 5000)
        val invoices = if (invoicesRes is DomainResult.Success) invoicesRes.data else emptyList()

        val now = System.currentTimeMillis()
        val overdueInvoices = invoices.filter {
            it.status in setOf(CustomerInvoiceStatus.ISSUED, CustomerInvoiceStatus.PARTIALLY_PAID) &&
                    it.dueDate != null && it.dueDate < now && it.dueAmount > BigDecimal.ZERO
        }
        val overdueAmount = overdueInvoices.map { it.dueAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)
        val oldestDue = overdueInvoices.minByOrNull { it.dueDate ?: Long.MAX_VALUE }?.dueDate

        val netExposure = summary.totalOutstanding
            .subtract(summary.totalUnallocated)
            .subtract(summary.totalAvailableCredit)
            .max(BigDecimal.ZERO)
            .setScale(4, RoundingMode.HALF_UP)

        val availableCredit = profile.creditLimit
            .subtract(netExposure)
            .max(BigDecimal.ZERO)
            .setScale(4, RoundingMode.HALF_UP)

        val riskStatus = calculateRiskStatus(profile, netExposure, profile.creditLimit, overdueInvoices.size)

        val riskSummary = CustomerReceivableRiskSummary(
            customerId = customerId,
            creditLimit = profile.creditLimit,
            totalInvoiced = summary.totalInvoiced,
            totalPaid = summary.totalPaid,
            currentOutstanding = summary.totalOutstanding,
            totalUnallocatedPayment = summary.totalUnallocated,
            totalAvailableCredit = summary.totalAvailableCredit,
            netReceivableExposure = netExposure,
            availableCreditLimit = availableCredit,
            overdueAmount = overdueAmount,
            overdueInvoiceCount = overdueInvoices.size,
            oldestDueInvoiceDate = oldestDue,
            paymentTermsType = profile.paymentTermsType,
            creditDays = profile.creditDays,
            requiresAdvance = profile.requiresAdvance,
            financialHold = profile.financialHold,
            holdReason = profile.holdReason,
            riskStatus = riskStatus
        )
        return DomainResult.Success(riskSummary)
    }

    override suspend fun getReceivableAgingReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long
    ): DomainResult<CustomerReceivableAgingReport> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val invoicesRes = invoiceRepository.listInvoices(tenantId, projectId, customerId = customerId, limit = 5000)
        if (invoicesRes is DomainResult.Error) return DomainResult.Error(invoicesRes.exception, invoicesRes.message)
        val invoices = (invoicesRes as DomainResult.Success).data
            .filter { it.status in setOf(CustomerInvoiceStatus.ISSUED, CustomerInvoiceStatus.PARTIALLY_PAID) && it.dueAmount > BigDecimal.ZERO }

        val bucketAmounts = mutableMapOf<ReceivableAgingBucket, BigDecimal>()
        val bucketCounts = mutableMapOf<ReceivableAgingBucket, Int>()
        ReceivableAgingBucket.entries.forEach {
            bucketAmounts[it] = BigDecimal.ZERO
            bucketCounts[it] = 0
        }

        val millisInDay = 86_400_000L
        var oldestOverdueDate: Long? = null
        var maxDaysOverdue = 0

        for (inv in invoices) {
            val due = inv.dueDate ?: asOfDate
            val daysOverdue = if (asOfDate > due) {
                ((asOfDate - due) / millisInDay).toInt()
            } else 0

            if (daysOverdue > 0) {
                if (oldestOverdueDate == null || due < oldestOverdueDate) {
                    oldestOverdueDate = due
                }
                if (daysOverdue > maxDaysOverdue) {
                    maxDaysOverdue = daysOverdue
                }
            }

            val bucket = when {
                daysOverdue <= 0 -> ReceivableAgingBucket.CURRENT
                daysOverdue in 1..7 -> ReceivableAgingBucket.DAYS_1_7
                daysOverdue in 8..30 -> ReceivableAgingBucket.DAYS_8_30
                daysOverdue in 31..60 -> ReceivableAgingBucket.DAYS_31_60
                daysOverdue in 61..90 -> ReceivableAgingBucket.DAYS_61_90
                else -> ReceivableAgingBucket.DAYS_90_PLUS
            }

            bucketAmounts[bucket] = bucketAmounts[bucket]!!.add(inv.dueAmount).setScale(4, RoundingMode.HALF_UP)
            bucketCounts[bucket] = bucketCounts[bucket]!! + 1
        }

        val bucketsList = ReceivableAgingBucket.entries.map { bucket ->
            AgingBucketSummary(
                bucket = bucket,
                invoiceCount = bucketCounts[bucket] ?: 0,
                outstandingAmount = bucketAmounts[bucket] ?: BigDecimal.ZERO
            )
        }

        val totalOutstanding = bucketsList.map { it.outstandingAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

        val report = CustomerReceivableAgingReport(
            customerId = customerId,
            asOfDate = asOfDate,
            totalOutstanding = totalOutstanding,
            buckets = bucketsList,
            oldestOverdueDate = oldestOverdueDate,
            maxDaysOverdue = maxDaysOverdue
        )
        return DomainResult.Success(report)
    }

    override suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<List<CustomerCreditControlAuditEvent>> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        return creditControlRepository.getAuditEvents(tenantId, projectId, customerId)
    }

    private fun calculateRiskStatus(
        profile: CustomerCreditProfileEntity,
        netExposure: BigDecimal,
        creditLimit: BigDecimal,
        overdueCount: Int
    ): CustomerCreditRiskStatus {
        return when {
            profile.financialHold -> CustomerCreditRiskStatus.FINANCIAL_HOLD
            overdueCount > 0 -> CustomerCreditRiskStatus.OVERDUE
            creditLimit > BigDecimal.ZERO && netExposure > creditLimit -> CustomerCreditRiskStatus.OVER_LIMIT
            creditLimit > BigDecimal.ZERO && netExposure.compareTo(creditLimit) == 0 -> CustomerCreditRiskStatus.LIMIT_REACHED
            profile.requiresAdvance || creditLimit == BigDecimal.ZERO -> CustomerCreditRiskStatus.ADVANCE_REQUIRED
            creditLimit > BigDecimal.ZERO && netExposure >= creditLimit.multiply(BigDecimal("0.80")) -> CustomerCreditRiskStatus.WATCH
            else -> CustomerCreditRiskStatus.NORMAL
        }
    }
}
