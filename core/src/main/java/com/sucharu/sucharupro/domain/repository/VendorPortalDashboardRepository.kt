package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Domain repository contract for Vendor Portal Dashboard & Summaries (Module 13 Step 02).
 */
interface VendorPortalDashboardRepository {

    /**
     * Retrieves the profile summary for a vendor.
     */
    suspend fun getProfileSummary(vendorId: String, tenantId: String, projectId: String): DomainResult<VendorPortalProfileSummary>

    /**
     * Retrieves the operational summary (Work Orders, Purchase Orders, Deliveries).
     */
    suspend fun getOperationalSummary(vendorId: String, tenantId: String, projectId: String): DomainResult<VendorPortalOperationalSummary>

    /**
     * Retrieves the financial summary (Invoices, Settlements, Payables).
     */
    suspend fun getFinancialSummary(vendorId: String, tenantId: String, projectId: String): DomainResult<VendorPortalFinancialSummary>

    /**
     * Retrieves the quality summary (Inspections, Rejections, Disputes).
     */
    suspend fun getQualitySummary(vendorId: String, tenantId: String, projectId: String): DomainResult<VendorPortalQualitySummary>

    /**
     * Retrieves the performance summary (Evaluations, Tier, Scores).
     */
    suspend fun getPerformanceSummary(vendorId: String, tenantId: String, projectId: String): DomainResult<VendorPortalPerformanceSummary>

    /**
     * Retrieves the compliance summary (Certifications, Licenses).
     */
    suspend fun getComplianceSummary(vendorId: String, tenantId: String, projectId: String): DomainResult<VendorPortalComplianceSummary>

    /**
     * Retrieves recent activity logs across portal and operational events.
     */
    suspend fun getRecentActivities(vendorId: String, tenantId: String, limit: Int = 10): DomainResult<List<VendorPortalActivitySummary>>
}
