package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*

/**
 * Repository interface for aggregated Vendor Analytics & Vendor 360 view (Module 12 Step 10).
 */
interface VendorAnalyticsRepository {

    suspend fun getFinancialSummary(vendorId: String, tenantId: String, projectId: String? = null): DomainResult<VendorFinancialSummary>
    suspend fun getOperationalSummary(vendorId: String, tenantId: String, projectId: String? = null): DomainResult<VendorOperationalSummary>
    suspend fun getQualitySummary(vendorId: String, tenantId: String): DomainResult<VendorQualitySummary>
    suspend fun getDeliverySummary(vendorId: String, tenantId: String): DomainResult<VendorDeliverySummary>
    suspend fun getInvoiceSummary(vendorId: String, tenantId: String): DomainResult<VendorInvoiceSummary>
    suspend fun getPerformanceSummary(vendorId: String, tenantId: String): DomainResult<VendorPerformanceSummary>
    suspend fun getComplianceSummary(vendorId: String, tenantId: String): DomainResult<VendorComplianceSummary>
    suspend fun getRiskSummary(vendorId: String, tenantId: String): DomainResult<VendorRiskSummary>
    suspend fun getVendor360Summary(vendorId: String, tenantId: String): DomainResult<Vendor360Summary>
    suspend fun getAnalyticsTrends(vendorId: String, period: AnalyticsPeriod, tenantId: String): DomainResult<List<VendorAnalyticsTrendPoint>>
}
