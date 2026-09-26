package com.sucharu.sucharupro.data.api.model.compliance

import kotlinx.serialization.Serializable

@Serializable
data class TaxRuleConfigurationDto(
    val taxRuleId: String,
    val taxCode: String,
    val taxName: String,
    val taxCategory: String = "VAT_STANDARD",
    val ratePercentage: String = "15.00",
    val isActive: Boolean = true
)

@Serializable
data class AccountingPeriodLockDto(
    val periodId: String,
    val periodName: String,
    val fiscalYear: String = "FY-2026",
    val startDate: String,
    val endDate: String,
    val status: String = "OPEN"
)

@Serializable
data class ImmutableInvoiceTaxSnapshotDto(
    val snapshotId: String,
    val invoiceId: String,
    val subtotalAmount: String,
    val taxAmount: String,
    val applicableTaxCode: String,
    val taxRatePercentage: String,
    val customerBin: String? = null,
    val customerTin: String? = null,
    val isImmutable: Boolean = true
)

@Serializable
data class FinancialComplianceSummaryDto(
    val activeTaxRulesCount: Int,
    val closedAccountingPeriodsCount: Int,
    val complianceStatus: String = "COMPLIANCE_READY",
    val isNoHardcodedTaxRateRuleEnforced: Boolean = true,
    val taxRules: List<TaxRuleConfigurationDto> = emptyList(),
    val accountingPeriods: List<AccountingPeriodLockDto> = emptyList(),
    val generatedAt: String
)
