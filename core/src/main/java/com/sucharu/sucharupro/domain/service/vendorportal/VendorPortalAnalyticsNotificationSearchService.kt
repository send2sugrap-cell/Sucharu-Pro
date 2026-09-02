package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Service interface for Vendor Portal Analytics, Notifications & Search Hub (Module 13 Step 10).
 */
interface VendorPortalAnalyticsNotificationSearchService {

    // --- Unified Analytics Hub & Breakdown ---
    suspend fun getUnifiedAnalyticsHub(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod = VendorPortalPeriod.LAST_30_DAYS
    ): DomainResult<VendorPortalUnifiedAnalyticsHub>

    suspend fun getOperationalAnalytics(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod = VendorPortalPeriod.LAST_30_DAYS
    ): DomainResult<VendorPortalOperationalAnalytics>

    suspend fun getFinancialAnalytics(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod = VendorPortalPeriod.LAST_30_DAYS
    ): DomainResult<VendorPortalFinancialAnalytics>

    suspend fun getQualityAnalytics(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod = VendorPortalPeriod.LAST_30_DAYS
    ): DomainResult<VendorPortalQualityAnalytics>

    suspend fun getPerformanceAnalytics(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod = VendorPortalPeriod.LAST_30_DAYS
    ): DomainResult<VendorPortalPerformanceAnalytics>

    suspend fun getComplianceAnalytics(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod = VendorPortalPeriod.LAST_30_DAYS
    ): DomainResult<VendorPortalComplianceAnalytics>

    suspend fun getCollaborationAnalytics(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod = VendorPortalPeriod.LAST_30_DAYS
    ): DomainResult<VendorPortalCollaborationAnalytics>

    suspend fun getTrends(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod = VendorPortalPeriod.LAST_30_DAYS
    ): DomainResult<List<VendorPortalTrendMetric>>

    // --- Notifications & Lifecycle ---
    suspend fun listNotifications(
        tenantId: String,
        projectId: String,
        vendorId: String,
        category: VendorPortalNotificationCategory? = null,
        status: VendorPortalNotificationStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<VendorPortalNotification>>

    suspend fun getUnreadCount(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalNotificationUnreadCount>

    suspend fun markNotificationAsRead(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String,
        actorId: String
    ): DomainResult<Boolean>

    suspend fun markAllNotificationsAsRead(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actorId: String
    ): DomainResult<Int>

    suspend fun archiveNotification(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String,
        actorId: String
    ): DomainResult<Boolean>

    suspend fun emitNotification(
        tenantId: String,
        projectId: String,
        vendorId: String,
        category: VendorPortalNotificationCategory,
        severity: VendorPortalNotificationSeverity = VendorPortalNotificationSeverity.NORMAL,
        title: String,
        message: String,
        relatedEntityType: String? = null,
        relatedEntityId: String? = null,
        deepLinkTarget: String? = null,
        metadata: Map<String, String> = emptyMap(),
        idempotencyKey: String? = null
    ): DomainResult<VendorPortalNotification>

    // --- Preferences ---
    suspend fun getNotificationPreferences(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalNotificationPreference>

    suspend fun updateNotificationPreferences(
        tenantId: String,
        projectId: String,
        vendorId: String,
        emailEnabled: Boolean,
        inAppEnabled: Boolean,
        pushEnabled: Boolean,
        importantOnlyMode: Boolean,
        disabledCategories: Set<VendorPortalNotificationCategory>,
        minSeverity: VendorPortalNotificationSeverity,
        actorId: String
    ): DomainResult<VendorPortalNotificationPreference>

    // --- Portal-Wide Search ---
    suspend fun search(
        tenantId: String,
        projectId: String,
        vendorId: String,
        query: String,
        types: Set<VendorPortalSearchResultType> = emptySet(),
        page: Int = 1,
        pageSize: Int = 20
    ): DomainResult<VendorPortalSearchResult>

    // --- Activity Timeline ---
    suspend fun getActivityTimeline(
        tenantId: String,
        projectId: String,
        vendorId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): DomainResult<VendorPortalActivityTimeline>

    // --- Unified Workspace Summary ---
    suspend fun getUnifiedWorkspaceSummary(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalUnifiedWorkspaceSummary>
}
