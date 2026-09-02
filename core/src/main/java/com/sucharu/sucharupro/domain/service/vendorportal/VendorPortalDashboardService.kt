package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorCapability
import com.sucharu.sucharupro.domain.model.vendor.VendorServiceRate
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Domain Service contract for Vendor Portal Dashboard & Workspace management (Module 13 Step 02).
 */
interface VendorPortalDashboardService {

    /**
     * Resolves the complete aggregate dashboard for the authenticated user and vendor.
     */
    suspend fun getDashboard(
        userId: String,
        vendorId: String,
        tenantId: String,
        clientIp: String? = null
    ): DomainResult<VendorPortalDashboard>

    /**
     * Retrieves the profile workspace summary.
     */
    suspend fun getProfile(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalProfileSummary>

    /**
     * Retrieves the vendor capabilities.
     */
    suspend fun getCapabilities(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<List<VendorCapability>>

    /**
     * Retrieves the vendor service rates.
     */
    suspend fun getRates(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<List<VendorServiceRate>>

    /**
     * Retrieves the operational summary.
     */
    suspend fun getOperationalSummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalOperationalSummary>

    /**
     * Retrieves the financial summary.
     */
    suspend fun getFinancialSummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalFinancialSummary>

    /**
     * Retrieves the quality summary.
     */
    suspend fun getQualitySummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalQualitySummary>

    /**
     * Retrieves the performance summary.
     */
    suspend fun getPerformanceSummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalPerformanceSummary>

    /**
     * Retrieves the compliance summary.
     */
    suspend fun getComplianceSummary(
        userId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorPortalComplianceSummary>

    /**
     * Retrieves recent activities.
     */
    suspend fun getRecentActivities(
        userId: String,
        vendorId: String,
        tenantId: String,
        limit: Int = 10
    ): DomainResult<List<VendorPortalActivitySummary>>

    /**
     * Retrieves workspace configuration and role-aware navigation.
     */
    suspend fun getWorkspace(
        userId: String,
        vendorId: String,
        tenantId: String,
        clientIp: String? = null
    ): DomainResult<VendorPortalWorkspace>
}
