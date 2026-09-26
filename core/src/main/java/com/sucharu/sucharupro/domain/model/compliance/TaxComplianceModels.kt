package com.sucharu.sucharupro.domain.model.compliance

import java.math.BigDecimal

/**
 * BI-07 Tax/VAT Categories.
 */
enum class TaxRuleCategory {
    VAT_STANDARD,
    VAT_REDUCED,
    VAT_EXEMPT,
    WITHHOLDING_TAX_AIT,
    SUPPLEMENTARY_DUTY,
    ZERO_RATED
}

/**
 * BI-07 Accounting Period Status.
 */
enum class AccountingPeriodStatus {
    OPEN,
    CLOSED,
    LOCKED_PERIOD
}

/**
 * Configurable Tax/VAT Rule Entity.
 */
data class TaxRuleConfiguration(
    val taxRuleId: String,
    val taxCode: String,
    val taxName: String,
    val taxCategory: TaxRuleCategory = TaxRuleCategory.VAT_STANDARD,
    val ratePercentage: BigDecimal = BigDecimal("15.00"),
    val isActive: Boolean = true,
    val effectiveFrom: String? = null,
    val effectiveUntil: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(taxRuleId.isNotBlank()) { "Tax Rule ID cannot be blank." }
        require(taxCode.isNotBlank()) { "Tax Code cannot be blank." }
        require(taxName.isNotBlank()) { "Tax Name cannot be blank." }
        require(ratePercentage >= BigDecimal.ZERO) { "Tax Rate Percentage cannot be negative." }
    }
}

/**
 * Accounting Period Lock Entity for Period Closing Governance.
 */
data class AccountingPeriodLock(
    val periodId: String,
    val periodName: String,
    val fiscalYear: String = "FY-2026",
    val startDate: String,
    val endDate: String,
    val status: AccountingPeriodStatus = AccountingPeriodStatus.OPEN,
    val closedAt: String? = null,
    val closedBy: String? = null,
    val createdAt: String,
    val createdBy: String
) {
    init {
        require(periodId.isNotBlank()) { "Period ID cannot be blank." }
        require(periodName.isNotBlank()) { "Period Name cannot be blank." }
    }

    val isPostingAllowed: Boolean
        get() = status == AccountingPeriodStatus.OPEN
}

/**
 * Immutable Invoice Tax Snapshot Entity.
 */
data class ImmutableInvoiceTaxSnapshot(
    val snapshotId: String,
    val invoiceId: String,
    val subtotalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val applicableTaxCode: String,
    val taxRatePercentage: BigDecimal,
    val customerBin: String? = null,
    val customerTin: String? = null,
    val isImmutable: Boolean = true, // Critical Invariant: Always true!
    val createdAt: String
)

/**
 * BI-07 Master Financial Compliance Summary Model.
 */
data class FinancialComplianceSummary(
    val activeTaxRulesCount: Int,
    val closedAccountingPeriodsCount: Int,
    val complianceStatus: String = "COMPLIANCE_READY",
    val isNoHardcodedTaxRateRuleEnforced: Boolean = true, // Critical Invariant: Always true!
    val taxRules: List<TaxRuleConfiguration> = emptyList(),
    val accountingPeriods: List<AccountingPeriodLock> = emptyList(),
    val generatedAt: String
)
