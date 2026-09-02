package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customerfinancialdashboard.*
import java.math.BigDecimal

data class CustomerFinancialWarningDto(
    val warningType: String,
    val severity: String,
    val title: String,
    val message: String,
    val relatedEntityId: String? = null,
    val relatedEntityType: String? = null
)

data class CustomerFinancialActionDto(
    val actionType: String,
    val priority: String,
    val title: String,
    val description: String,
    val targetRoute: String,
    val relatedEntityId: String? = null
)

data class CustomerFinancialActivityItemDto(
    val activityId: String,
    val type: String,
    val amount: BigDecimal?,
    val referenceNumber: String?,
    val status: String,
    val description: String,
    val occurredAt: Long
)

data class CustomerReceivableAgingSummaryDto(
    val currentAmount: BigDecimal,
    val days1To7Amount: BigDecimal,
    val days8To30Amount: BigDecimal,
    val days31To60Amount: BigDecimal,
    val days61To90Amount: BigDecimal,
    val days90PlusAmount: BigDecimal,
    val totalAgingOutstanding: BigDecimal,
    val oldestOverdueDate: Long? = null,
    val maxDaysOverdue: Int = 0
)

data class CustomerDueScheduleSummaryDto(
    val upcomingDueAmount: BigDecimal,
    val dueTodayAmount: BigDecimal,
    val overdueAmount: BigDecimal,
    val criticalOverdueAmount: BigDecimal,
    val overdueInvoiceCount: Int
)

data class CustomerCollectionStatusSummaryDto(
    val priority: String,
    val pendingActionCount: Int,
    val completedActionCount: Int,
    val activePromiseCount: Int,
    val activePromisedAmount: BigDecimal,
    val nextFollowUpAt: Long? = null,
    val latestOutcome: String? = null
)

data class CustomerReconciliationStatusSummaryDto(
    val isReconciled: Boolean,
    val discrepancyCount: Int,
    val lastReconciledAt: Long? = null,
    val varianceAmount: BigDecimal
)

data class CustomerFinancialDashboardDto(
    val customerId: String,
    val tenantId: String,
    val projectId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val accountNumber: String,
    val accountStatus: String,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalUnallocated: BigDecimal,
    val availableCreditBalance: BigDecimal,
    val outstandingReceivable: BigDecimal,
    val creditLimit: BigDecimal,
    val currentCreditExposure: BigDecimal,
    val availableCreditCapacity: BigDecimal,
    val paymentTerms: String,
    val creditDays: Int,
    val requiresAdvance: Boolean,
    val riskStatus: String,
    val financialHold: Boolean,
    val holdReason: String?,
    val agingSummary: CustomerReceivableAgingSummaryDto,
    val dueSchedule: CustomerDueScheduleSummaryDto,
    val collectionStatus: CustomerCollectionStatusSummaryDto,
    val reconciliationSummary: CustomerReconciliationStatusSummaryDto,
    val warnings: List<CustomerFinancialWarningDto>,
    val recommendedActions: List<CustomerFinancialActionDto>,
    val recentActivity: List<CustomerFinancialActivityItemDto>,
    val generatedAt: Long
)

fun CustomerFinancialWarning.toDto(): CustomerFinancialWarningDto {
    return CustomerFinancialWarningDto(
        warningType = warningType.name,
        severity = severity.name,
        title = title,
        message = message,
        relatedEntityId = relatedEntityId,
        relatedEntityType = relatedEntityType
    )
}

fun CustomerFinancialAction.toDto(): CustomerFinancialActionDto {
    return CustomerFinancialActionDto(
        actionType = actionType.name,
        priority = priority.name,
        title = title,
        description = description,
        targetRoute = targetRoute,
        relatedEntityId = relatedEntityId
    )
}

fun CustomerFinancialActivityItem.toDto(): CustomerFinancialActivityItemDto {
    return CustomerFinancialActivityItemDto(
        activityId = activityId,
        type = type.name,
        amount = amount,
        referenceNumber = referenceNumber,
        status = status,
        description = description,
        occurredAt = occurredAt
    )
}

fun CustomerReceivableAgingSummary.toDto(): CustomerReceivableAgingSummaryDto {
    return CustomerReceivableAgingSummaryDto(
        currentAmount = currentAmount,
        days1To7Amount = days1To7Amount,
        days8To30Amount = days8To30Amount,
        days31To60Amount = days31To60Amount,
        days61To90Amount = days61To90Amount,
        days90PlusAmount = days90PlusAmount,
        totalAgingOutstanding = totalAgingOutstanding,
        oldestOverdueDate = oldestOverdueDate,
        maxDaysOverdue = maxDaysOverdue
    )
}

fun CustomerDueScheduleSummary.toDto(): CustomerDueScheduleSummaryDto {
    return CustomerDueScheduleSummaryDto(
        upcomingDueAmount = upcomingDueAmount,
        dueTodayAmount = dueTodayAmount,
        overdueAmount = overdueAmount,
        criticalOverdueAmount = criticalOverdueAmount,
        overdueInvoiceCount = overdueInvoiceCount
    )
}

fun CustomerCollectionStatusSummary.toDto(): CustomerCollectionStatusSummaryDto {
    return CustomerCollectionStatusSummaryDto(
        priority = priority.name,
        pendingActionCount = pendingActionCount,
        completedActionCount = completedActionCount,
        activePromiseCount = activePromiseCount,
        activePromisedAmount = activePromisedAmount,
        nextFollowUpAt = nextFollowUpAt,
        latestOutcome = latestOutcome?.name
    )
}

fun CustomerReconciliationStatusSummary.toDto(): CustomerReconciliationStatusSummaryDto {
    return CustomerReconciliationStatusSummaryDto(
        isReconciled = isReconciled,
        discrepancyCount = discrepancyCount,
        lastReconciledAt = lastReconciledAt,
        varianceAmount = varianceAmount
    )
}

fun CustomerFinancialDashboard.toDto(): CustomerFinancialDashboardDto {
    return CustomerFinancialDashboardDto(
        customerId = customerId,
        tenantId = tenantId,
        projectId = projectId,
        customerCode = customerCode,
        customerDisplayName = customerDisplayName,
        accountNumber = accountNumber,
        accountStatus = accountStatus.name,
        totalInvoiced = totalInvoiced,
        totalPaid = totalPaid,
        totalAllocated = totalAllocated,
        totalUnallocated = totalUnallocated,
        availableCreditBalance = availableCreditBalance,
        outstandingReceivable = outstandingReceivable,
        creditLimit = creditLimit,
        currentCreditExposure = currentCreditExposure,
        availableCreditCapacity = availableCreditCapacity,
        paymentTerms = paymentTerms.name,
        creditDays = creditDays,
        requiresAdvance = requiresAdvance,
        riskStatus = riskStatus.name,
        financialHold = financialHold,
        holdReason = holdReason,
        agingSummary = agingSummary.toDto(),
        dueSchedule = dueSchedule.toDto(),
        collectionStatus = collectionStatus.toDto(),
        reconciliationSummary = reconciliationSummary.toDto(),
        warnings = warnings.map { it.toDto() },
        recommendedActions = recommendedActions.map { it.toDto() },
        recentActivity = recentActivity.map { it.toDto() },
        generatedAt = generatedAt
    )
}
