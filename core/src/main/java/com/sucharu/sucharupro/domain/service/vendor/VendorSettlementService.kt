package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.model.vendor.*

/**
 * Service orchestrating Vendor Settlement, Reconciliation & Aggregated Analytics (Module 12 Step 10).
 */
interface VendorSettlementService {

    suspend fun evaluateEligibility(
        vendorId: String,
        payableId: String? = null,
        tenantId: String = "TENANT-001"
    ): DomainResult<SettlementEligibilityResult>

    suspend fun createSettlement(
        vendorId: String,
        settlementNumber: String,
        totalAmount: Money,
        settlementMethod: SettlementMethod = SettlementMethod.BANK_TRANSFER,
        referenceNumber: String? = null,
        notes: String? = null,
        allocations: List<VendorSettlementAllocation>,
        tenantId: String = "TENANT-001",
        projectId: String = "PRJ-001",
        actorId: String = "system"
    ): DomainResult<VendorSettlement>

    suspend fun approveSettlement(
        settlementId: String,
        tenantId: String = "TENANT-001",
        actorId: String
    ): DomainResult<VendorSettlement>

    suspend fun processSettlement(
        settlementId: String,
        tenantId: String = "TENANT-001",
        actorId: String,
        callerRole: UserRole = UserRole.ACCOUNTS
    ): DomainResult<VendorSettlement>

    suspend fun reconcileSettlement(
        settlementId: String,
        tenantId: String = "TENANT-001",
        actorId: String = "system"
    ): DomainResult<VendorReconciliationResult>

    suspend fun getSettlementById(
        settlementId: String,
        tenantId: String = "TENANT-001"
    ): DomainResult<VendorSettlement?>

    suspend fun listSettlements(
        vendorId: String? = null,
        status: VendorSettlementStatus? = null,
        projectId: String? = null,
        tenantId: String = "TENANT-001"
    ): DomainResult<List<VendorSettlement>>

    suspend fun getFinancialSummary(
        vendorId: String,
        tenantId: String = "TENANT-001",
        projectId: String? = null
    ): DomainResult<VendorFinancialSummary>

    suspend fun getOperationalSummary(
        vendorId: String,
        tenantId: String = "TENANT-001",
        projectId: String? = null
    ): DomainResult<VendorOperationalSummary>

    suspend fun getQualitySummary(
        vendorId: String,
        tenantId: String = "TENANT-001"
    ): DomainResult<VendorQualitySummary>

    suspend fun getDeliverySummary(
        vendorId: String,
        tenantId: String = "TENANT-001"
    ): DomainResult<VendorDeliverySummary>

    suspend fun getInvoiceSummary(
        vendorId: String,
        tenantId: String = "TENANT-001"
    ): DomainResult<VendorInvoiceSummary>

    suspend fun getPerformanceSummary(
        vendorId: String,
        tenantId: String = "TENANT-001"
    ): DomainResult<VendorPerformanceSummary>

    suspend fun getComplianceSummary(
        vendorId: String,
        tenantId: String = "TENANT-001"
    ): DomainResult<VendorComplianceSummary>

    suspend fun getRiskSummary(
        vendorId: String,
        tenantId: String = "TENANT-001"
    ): DomainResult<VendorRiskSummary>

    suspend fun getVendor360Summary(
        vendorId: String,
        tenantId: String = "TENANT-001"
    ): DomainResult<Vendor360Summary>

    suspend fun getAnalyticsTrends(
        vendorId: String,
        period: AnalyticsPeriod = AnalyticsPeriod.MONTHLY,
        tenantId: String = "TENANT-001"
    ): DomainResult<List<VendorAnalyticsTrendPoint>>

    suspend fun saveAnalyticsSnapshot(
        vendorId: String,
        projectId: String,
        period: AnalyticsPeriod,
        startDate: Long,
        endDate: Long,
        metricsJson: String,
        tenantId: String = "TENANT-001",
        actorId: String = "system"
    ): DomainResult<VendorAnalyticsSnapshot>

    suspend fun listAnalyticsSnapshots(
        vendorId: String,
        period: AnalyticsPeriod? = null,
        tenantId: String = "TENANT-001"
    ): DomainResult<List<VendorAnalyticsSnapshot>>

    suspend fun listAuditEvents(
        settlementId: String? = null,
        vendorId: String? = null,
        tenantId: String = "TENANT-001"
    ): DomainResult<List<VendorSettlementAuditEvent>>
}
