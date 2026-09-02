package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Repository interface for Vendor Portal Analytics, Notifications & Search (Module 13 Step 10).
 */
interface VendorPortalAnalyticsNotificationSearchRepository {

    // Notifications
    suspend fun saveNotification(notification: VendorPortalNotification): DomainResult<VendorPortalNotification>
    suspend fun findNotificationById(tenantId: String, projectId: String, vendorId: String, notificationId: String): DomainResult<VendorPortalNotification?>
    suspend fun listNotifications(
        tenantId: String,
        projectId: String,
        vendorId: String,
        category: VendorPortalNotificationCategory? = null,
        status: VendorPortalNotificationStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<VendorPortalNotification>>
    suspend fun countUnreadNotifications(tenantId: String, projectId: String, vendorId: String): DomainResult<Int>
    suspend fun markNotificationAsRead(tenantId: String, projectId: String, vendorId: String, notificationId: String, readAt: Long = System.currentTimeMillis()): DomainResult<Boolean>
    suspend fun markAllNotificationsAsRead(tenantId: String, projectId: String, vendorId: String, readAt: Long = System.currentTimeMillis()): DomainResult<Int>
    suspend fun archiveNotification(tenantId: String, projectId: String, vendorId: String, notificationId: String): DomainResult<Boolean>

    // Preferences
    suspend fun savePreferences(preferences: VendorPortalNotificationPreference): DomainResult<VendorPortalNotificationPreference>
    suspend fun getPreferences(tenantId: String, projectId: String, vendorId: String): DomainResult<VendorPortalNotificationPreference?>

    // Snapshots
    suspend fun saveAnalyticsSnapshot(snapshot: VendorPortalAnalyticsSnapshot): DomainResult<VendorPortalAnalyticsSnapshot>
    suspend fun getLatestAnalyticsSnapshot(tenantId: String, projectId: String, vendorId: String, period: VendorPortalPeriod): DomainResult<VendorPortalAnalyticsSnapshot?>
}
