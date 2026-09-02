package com.sucharu.sucharupro.domain.service.customerfinancialdashboard

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercollection.CollectionPriority
import com.sucharu.sucharupro.domain.model.customercollection.PaymentPromiseStatus
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditRiskStatus
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerPaymentTermsType
import com.sucharu.sucharupro.domain.model.customercreditcontrol.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.model.customerfinancialdashboard.*
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customercollection.CustomerCollectionRepository
import com.sucharu.sucharupro.domain.repository.customercredit.CustomerCreditRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.repository.customerpayment.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionService
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlService
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerService
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementService
import java.math.BigDecimal

class CustomerFinancialDashboardServiceImpl(
    private val customerRepository: CustomerRepository,
    private val accountRepository: CustomerFinancialAccountRepository,
    private val invoiceRepository: CustomerInvoiceRepository,
    private val paymentRepository: CustomerPaymentRepository,
    private val creditRepository: CustomerCreditRepository,
    private val collectionRepository: CustomerCollectionRepository,
    private val settlementService: CustomerSettlementService,
    private val creditControlService: CustomerCreditControlService,
    private val collectionService: CustomerCollectionService,
    private val ledgerService: CustomerLedgerService
) : CustomerFinancialDashboardService {

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

    override suspend fun getCustomerFinancialDashboard(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long
    ): DomainResult<CustomerFinancialDashboard> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = (custRes as DomainResult.Success).data

        val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, customerId)
        val account = (accountRes as DomainResult.Success).data

        // 1. Authoritative Settlement Data (Steps 01-06)
        val settlementRes = settlementService.getCustomerSettlementSummary(tenantId, projectId, customerId)
        val settlement = if (settlementRes is DomainResult.Success) settlementRes.data else null

        // 2. Authoritative Credit & Risk Data (Step 07)
        val riskRes = creditControlService.getReceivableRiskSummary(tenantId, projectId, customerId)
        val risk = if (riskRes is DomainResult.Success) riskRes.data else null

        // 3. Authoritative Aging Report (Step 07)
        val agingRes = getReceivableAgingSummary(tenantId, projectId, customerId, asOfDate)
        val agingSummary = if (agingRes is DomainResult.Success) agingRes.data else CustomerReceivableAgingSummary(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        )

        // 4. Authoritative Collection & Due Schedule Data (Step 08)
        val collRes = collectionService.getCustomerCollectionSummary(tenantId, projectId, customerId, asOfDate)
        val collSummary = if (collRes is DomainResult.Success) collRes.data else null

        val dueSchedule = CustomerDueScheduleSummary(
            upcomingDueAmount = collSummary?.upcomingDueAmount ?: BigDecimal.ZERO,
            dueTodayAmount = collSummary?.dueTodayAmount ?: BigDecimal.ZERO,
            overdueAmount = collSummary?.overdueAmount ?: BigDecimal.ZERO,
            criticalOverdueAmount = collSummary?.criticalOverdueAmount ?: BigDecimal.ZERO,
            overdueInvoiceCount = collSummary?.overdueInvoiceCount ?: 0
        )

        val collectionStatus = CustomerCollectionStatusSummary(
            priority = collSummary?.priority ?: CollectionPriority.LOW,
            pendingActionCount = collSummary?.pendingActionCount ?: 0,
            completedActionCount = collSummary?.completedActionCount ?: 0,
            activePromiseCount = collSummary?.activePromiseCount ?: 0,
            activePromisedAmount = collSummary?.activePromisedAmount ?: BigDecimal.ZERO,
            nextFollowUpAt = collSummary?.nextFollowUpAt,
            latestOutcome = collSummary?.latestOutcome
        )

        // 5. Authoritative Reconciliation Data (Step 05)
        val reconRes = ledgerService.reconcileCustomerReceivable(tenantId, projectId, customerId)
        val reconSummary = if (reconRes is DomainResult.Success) {
            val recon = reconRes.data
            CustomerReconciliationStatusSummary(
                isReconciled = recon.isConsistent,
                discrepancyCount = recon.discrepancyCount,
                lastReconciledAt = recon.reconciledAt,
                varianceAmount = recon.difference
            )
        } else {
            CustomerReconciliationStatusSummary(isReconciled = true, discrepancyCount = 0, varianceAmount = BigDecimal.ZERO)
        }

        // 6. Warnings & Recommended Actions
        val warningsRes = getFinancialWarnings(tenantId, projectId, customerId, asOfDate)
        val warnings = if (warningsRes is DomainResult.Success) warningsRes.data else emptyList()

        val actionsRes = getRecommendedFinancialActions(tenantId, projectId, customerId, asOfDate)
        val recommendedActions = if (actionsRes is DomainResult.Success) actionsRes.data else emptyList()

        // 7. Recent Financial Activity
        val activityRes = getRecentFinancialActivity(tenantId, projectId, customerId, limit = 20)
        val recentActivity = if (activityRes is DomainResult.Success) activityRes.data else emptyList()

        val dashboard = CustomerFinancialDashboard(
            customerId = customerId,
            tenantId = tenantId,
            projectId = projectId,
            customerCode = customer.customerCode,
            customerDisplayName = customer.displayName,
            accountNumber = account.accountNumber,
            accountStatus = account.status,
            totalInvoiced = settlement?.totalInvoiced ?: BigDecimal.ZERO,
            totalPaid = settlement?.totalPaid ?: BigDecimal.ZERO,
            totalAllocated = settlement?.totalAllocated ?: BigDecimal.ZERO,
            totalUnallocated = settlement?.totalUnallocated ?: BigDecimal.ZERO,
            availableCreditBalance = settlement?.totalAvailableCredit ?: BigDecimal.ZERO,
            outstandingReceivable = settlement?.totalOutstanding ?: BigDecimal.ZERO,
            creditLimit = risk?.creditLimit ?: BigDecimal.ZERO,
            currentCreditExposure = risk?.netReceivableExposure ?: BigDecimal.ZERO,
            availableCreditCapacity = risk?.availableCreditLimit ?: BigDecimal.ZERO,
            paymentTerms = risk?.paymentTermsType ?: CustomerPaymentTermsType.DUE_ON_RECEIPT,
            creditDays = risk?.creditDays ?: 0,
            requiresAdvance = risk?.requiresAdvance ?: false,
            riskStatus = risk?.riskStatus ?: CustomerCreditRiskStatus.NORMAL,
            financialHold = risk?.financialHold ?: false,
            holdReason = risk?.holdReason,
            agingSummary = agingSummary,
            dueSchedule = dueSchedule,
            collectionStatus = collectionStatus,
            reconciliationSummary = reconSummary,
            warnings = warnings,
            recommendedActions = recommendedActions,
            recentActivity = recentActivity,
            generatedAt = asOfDate
        )

        return DomainResult.Success(dashboard)
    }

    override suspend fun getFinancialWarnings(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long
    ): DomainResult<List<CustomerFinancialWarning>> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val warnings = mutableListOf<CustomerFinancialWarning>()

        // 1. Credit Risk & Hold Warnings
        val riskRes = creditControlService.getReceivableRiskSummary(tenantId, projectId, customerId)
        if (riskRes is DomainResult.Success) {
            val risk = riskRes.data
            if (risk.financialHold) {
                warnings.add(
                    CustomerFinancialWarning(
                        warningType = FinancialWarningType.FINANCIAL_HOLD,
                        severity = CollectionPriority.CRITICAL,
                        title = "Account on Financial Hold",
                        message = "Financial transactions are restricted: ${risk.holdReason ?: "Manual hold"}",
                        relatedEntityId = customerId,
                        relatedEntityType = "CUSTOMER"
                    )
                )
            }
            if (risk.creditLimit > BigDecimal.ZERO && risk.netReceivableExposure > risk.creditLimit) {
                warnings.add(
                    CustomerFinancialWarning(
                        warningType = FinancialWarningType.CREDIT_LIMIT_EXCEEDED,
                        severity = CollectionPriority.HIGH,
                        title = "Credit Limit Exceeded",
                        message = "Net exposure (৳ ${risk.netReceivableExposure}) exceeds approved limit (৳ ${risk.creditLimit}).",
                        relatedEntityId = customerId,
                        relatedEntityType = "CREDIT_PROFILE"
                    )
                )
            }
            if (risk.requiresAdvance) {
                warnings.add(
                    CustomerFinancialWarning(
                        warningType = FinancialWarningType.ADVANCE_REQUIRED,
                        severity = CollectionPriority.NORMAL,
                        title = "Advance Payment Required",
                        message = "Customer requires 100% advance prepayment before trade dispatch.",
                        relatedEntityId = customerId,
                        relatedEntityType = "CREDIT_PROFILE"
                    )
                )
            }
        }

        // 2. Overdue & Due Today Warnings
        val dueRes = collectionService.getReceivableDueSchedule(tenantId, projectId, customerId, asOfDate)
        if (dueRes is DomainResult.Success) {
            val dueList = dueRes.data
            val overdueItems = dueList.filter { it.daysOverdue > 0 }
            if (overdueItems.isNotEmpty()) {
                val totalOverdue = overdueItems.map { it.dueAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
                val severity = if (overdueItems.any { it.daysOverdue > 60 }) CollectionPriority.CRITICAL else CollectionPriority.HIGH
                warnings.add(
                    CustomerFinancialWarning(
                        warningType = FinancialWarningType.OVERDUE_RECEIVABLE,
                        severity = severity,
                        title = "Overdue Invoices (${overdueItems.size})",
                        message = "Total overdue amount is ৳ $totalOverdue. Immediate follow-up required.",
                        relatedEntityId = overdueItems.first().invoiceId,
                        relatedEntityType = "INVOICE"
                    )
                )
            }

            val dueTodayItems = dueList.filter { it.daysOverdue == 0 }
            if (dueTodayItems.isNotEmpty()) {
                val totalDueToday = dueTodayItems.map { it.dueAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
                warnings.add(
                    CustomerFinancialWarning(
                        warningType = FinancialWarningType.DUE_TODAY,
                        severity = CollectionPriority.NORMAL,
                        title = "Invoices Due Today (${dueTodayItems.size})",
                        message = "৳ $totalDueToday is scheduled for payment today.",
                        relatedEntityId = dueTodayItems.first().invoiceId,
                        relatedEntityType = "INVOICE"
                    )
                )
            }
        }

        // 3. Payment Promises Due
        val promiseRes = collectionRepository.listPaymentPromises(tenantId, projectId, customerId = customerId, status = PaymentPromiseStatus.PENDING, limit = 50)
        if (promiseRes is DomainResult.Success) {
            val promises = promiseRes.data
            val duePromises = promises.filter { it.promisedDate <= asOfDate }
            if (duePromises.isNotEmpty()) {
                val totalPromised = duePromises.map { it.promisedAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
                warnings.add(
                    CustomerFinancialWarning(
                        warningType = FinancialWarningType.PAYMENT_PROMISE_DUE,
                        severity = CollectionPriority.HIGH,
                        title = "Payment Promises Mature (${duePromises.size})",
                        message = "Customer promised payment of ৳ $totalPromised on or before today.",
                        relatedEntityId = duePromises.first().promiseId,
                        relatedEntityType = "PAYMENT_PROMISE"
                    )
                )
            }
        }

        // 4. Unallocated Payment Warning
        val settlementRes = settlementService.getCustomerSettlementSummary(tenantId, projectId, customerId)
        if (settlementRes is DomainResult.Success) {
            val settlement = settlementRes.data
            if (settlement.totalUnallocated > BigDecimal.ZERO && settlement.totalOutstanding > BigDecimal.ZERO) {
                warnings.add(
                    CustomerFinancialWarning(
                        warningType = FinancialWarningType.UNALLOCATED_PAYMENT,
                        severity = CollectionPriority.NORMAL,
                        title = "Unallocated Payments Available",
                        message = "৳ ${settlement.totalUnallocated} unallocated funds available against open receivables of ৳ ${settlement.totalOutstanding}.",
                        relatedEntityId = customerId,
                        relatedEntityType = "SETTLEMENT"
                    )
                )
            }
        }

        // 5. Reconciliation Discrepancy Warning
        val reconRes = ledgerService.reconcileCustomerReceivable(tenantId, projectId, customerId)
        if (reconRes is DomainResult.Success && !reconRes.data.isConsistent) {
            warnings.add(
                CustomerFinancialWarning(
                    warningType = FinancialWarningType.RECONCILIATION_DISCREPANCY,
                    severity = CollectionPriority.HIGH,
                    title = "Ledger Reconciliation Discrepancy",
                    message = "Found ${reconRes.data.discrepancyCount} ledger variance discrepancies (variance: ৳ ${reconRes.data.difference}).",
                    relatedEntityId = customerId,
                    relatedEntityType = "RECONCILIATION"
                )
            )
        }

        return DomainResult.Success(warnings)
    }

    override suspend fun getRecommendedFinancialActions(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long
    ): DomainResult<List<CustomerFinancialAction>> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val actions = mutableListOf<CustomerFinancialAction>()

        val warningsRes = getFinancialWarnings(tenantId, projectId, customerId, asOfDate)
        val warnings = if (warningsRes is DomainResult.Success) warningsRes.data else emptyList()

        for (w in warnings) {
            when (w.warningType) {
                FinancialWarningType.FINANCIAL_HOLD -> {
                    actions.add(
                        CustomerFinancialAction(
                            actionType = FinancialActionType.REVIEW_HOLD,
                            priority = CollectionPriority.CRITICAL,
                            title = "Review Financial Hold",
                            description = "Inspect hold cause and evaluate credit release conditions.",
                            targetRoute = "/customers/$customerId/credit-profile",
                            relatedEntityId = customerId
                        )
                    )
                }
                FinancialWarningType.OVERDUE_RECEIVABLE -> {
                    actions.add(
                        CustomerFinancialAction(
                            actionType = FinancialActionType.REVIEW_COLLECTION,
                            priority = w.severity,
                            title = "Initiate Collection Follow-up",
                            description = "Dispatch reminder or call customer regarding overdue balances.",
                            targetRoute = "/customers/$customerId/collection",
                            relatedEntityId = w.relatedEntityId
                        )
                    )
                }
                FinancialWarningType.UNALLOCATED_PAYMENT -> {
                    actions.add(
                        CustomerFinancialAction(
                            actionType = FinancialActionType.ALLOCATE_PAYMENT,
                            priority = CollectionPriority.NORMAL,
                            title = "Allocate Unapplied Payments",
                            description = "Apply unallocated payments to open overdue/current invoices.",
                            targetRoute = "/customers/$customerId/settlement",
                            relatedEntityId = customerId
                        )
                    )
                }
                FinancialWarningType.CREDIT_LIMIT_EXCEEDED -> {
                    actions.add(
                        CustomerFinancialAction(
                            actionType = FinancialActionType.REVIEW_CREDIT,
                            priority = CollectionPriority.HIGH,
                            title = "Review Credit Limit",
                            description = "Evaluate credit capacity expansion or demand partial clearance.",
                            targetRoute = "/customers/$customerId/credit-profile",
                            relatedEntityId = customerId
                        )
                    )
                }
                FinancialWarningType.RECONCILIATION_DISCREPANCY -> {
                    actions.add(
                        CustomerFinancialAction(
                            actionType = FinancialActionType.REVIEW_RECONCILIATION,
                            priority = CollectionPriority.HIGH,
                            title = "Investigate Ledger Discrepancy",
                            description = "Analyze variance discrepancies between invoice items and ledger credits.",
                            targetRoute = "/customers/$customerId/ledger",
                            relatedEntityId = customerId
                        )
                    )
                }
                else -> {}
            }
        }

        // Always provide statement inspection
        actions.add(
            CustomerFinancialAction(
                actionType = FinancialActionType.VIEW_STATEMENT,
                priority = CollectionPriority.LOW,
                title = "View Account Statement",
                description = "Inspect full chronological ledger statement and balance timeline.",
                targetRoute = "/customers/$customerId/statement",
                relatedEntityId = customerId
            )
        )

        return DomainResult.Success(actions.distinctBy { it.actionType })
    }

    override suspend fun getReceivableAgingSummary(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long
    ): DomainResult<CustomerReceivableAgingSummary> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val reportRes = creditControlService.getReceivableAgingReport(tenantId, projectId, customerId, asOfDate)
        if (reportRes is DomainResult.Error) return DomainResult.Error(reportRes.exception, reportRes.message)
        val report = (reportRes as DomainResult.Success).data

        val bucketMap = report.buckets.associateBy { it.bucket }

        val summary = CustomerReceivableAgingSummary(
            currentAmount = bucketMap[ReceivableAgingBucket.CURRENT]?.outstandingAmount ?: BigDecimal.ZERO,
            days1To7Amount = bucketMap[ReceivableAgingBucket.DAYS_1_7]?.outstandingAmount ?: BigDecimal.ZERO,
            days8To30Amount = bucketMap[ReceivableAgingBucket.DAYS_8_30]?.outstandingAmount ?: BigDecimal.ZERO,
            days31To60Amount = bucketMap[ReceivableAgingBucket.DAYS_31_60]?.outstandingAmount ?: BigDecimal.ZERO,
            days61To90Amount = bucketMap[ReceivableAgingBucket.DAYS_61_90]?.outstandingAmount ?: BigDecimal.ZERO,
            days90PlusAmount = bucketMap[ReceivableAgingBucket.DAYS_90_PLUS]?.outstandingAmount ?: BigDecimal.ZERO,
            totalAgingOutstanding = report.totalOutstanding,
            oldestOverdueDate = report.oldestOverdueDate,
            maxDaysOverdue = report.maxDaysOverdue
        )

        return DomainResult.Success(summary)
    }

    override suspend fun getRecentFinancialActivity(
        tenantId: String,
        projectId: String,
        customerId: String,
        limit: Int
    ): DomainResult<List<CustomerFinancialActivityItem>> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val activity = mutableListOf<CustomerFinancialActivityItem>()

        // 1. Invoices
        val invRes = invoiceRepository.listInvoices(tenantId, projectId, customerId = customerId, limit = limit)
        if (invRes is DomainResult.Success) {
            for (inv in invRes.data) {
                activity.add(
                    CustomerFinancialActivityItem(
                        activityId = inv.invoiceId,
                        type = FinancialActivityType.INVOICE,
                        amount = inv.grandTotal,
                        referenceNumber = inv.invoiceNumber,
                        status = inv.status.name,
                        description = "Invoice issued: Due ৳ ${inv.dueAmount}",
                        occurredAt = inv.createdAt
                    )
                )
            }
        }

        // 2. Payments
        val payRes = paymentRepository.listPayments(tenantId, projectId, customerId = customerId, limit = limit)
        if (payRes is DomainResult.Success) {
            for (pay in payRes.data) {
                activity.add(
                    CustomerFinancialActivityItem(
                        activityId = pay.paymentId,
                        type = FinancialActivityType.PAYMENT,
                        amount = pay.amount,
                        referenceNumber = pay.paymentNumber,
                        status = pay.status.name,
                        description = "Payment via ${pay.paymentMethod}: ৳ ${pay.amount}",
                        occurredAt = pay.paymentDate
                    )
                )
            }
        }

        // 3. Advances
        val advRes = creditRepository.listAdvances(tenantId, projectId, customerId = customerId, limit = limit)
        if (advRes is DomainResult.Success) {
            for (adv in advRes.data) {
                activity.add(
                    CustomerFinancialActivityItem(
                        activityId = adv.advanceId,
                        type = FinancialActivityType.ADVANCE,
                        amount = adv.amount,
                        referenceNumber = adv.advanceNumber,
                        status = adv.status.name,
                        description = "Advance: Available ৳ ${adv.availableAmount}",
                        occurredAt = adv.createdAt
                    )
                )
            }
        }

        // 4. Collection Actions
        val collRes = collectionRepository.listActions(tenantId, projectId, customerId = customerId, limit = limit)
        if (collRes is DomainResult.Success) {
            for (action in collRes.data) {
                activity.add(
                    CustomerFinancialActivityItem(
                        activityId = action.actionId,
                        type = FinancialActivityType.COLLECTION_ACTION,
                        amount = null,
                        referenceNumber = null,
                        status = action.status.name,
                        description = "Collection follow-up (${action.actionType}): ${action.outcome ?: action.notes ?: ""}",
                        occurredAt = action.performedAt ?: action.scheduledAt
                    )
                )
            }
        }

        // Sort chronologically descending
        val sorted = activity.sortedByDescending { it.occurredAt }.take(limit)
        return DomainResult.Success(sorted)
    }
}
