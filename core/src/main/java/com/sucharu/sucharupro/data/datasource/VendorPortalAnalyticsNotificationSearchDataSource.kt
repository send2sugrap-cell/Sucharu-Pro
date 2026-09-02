package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Data source interface for Vendor Portal Analytics, Notifications & Search persistence (Module 13 Step 10).
 */
interface VendorPortalAnalyticsNotificationSearchDataSource {

    // Notifications
    suspend fun saveNotification(notification: VendorPortalNotification): VendorPortalNotification
    suspend fun findNotificationById(tenantId: String, projectId: String, vendorId: String, notificationId: String): VendorPortalNotification?
    suspend fun listNotifications(
        tenantId: String,
        projectId: String,
        vendorId: String,
        category: VendorPortalNotificationCategory? = null,
        status: VendorPortalNotificationStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<VendorPortalNotification>
    suspend fun countUnreadNotifications(tenantId: String, projectId: String, vendorId: String): Int
    suspend fun markNotificationAsRead(tenantId: String, projectId: String, vendorId: String, notificationId: String, readAt: Long): Boolean
    suspend fun markAllNotificationsAsRead(tenantId: String, projectId: String, vendorId: String, readAt: Long): Int
    suspend fun archiveNotification(tenantId: String, projectId: String, vendorId: String, notificationId: String): Boolean

    // Preferences
    suspend fun savePreferences(preferences: VendorPortalNotificationPreference): VendorPortalNotificationPreference
    suspend fun getPreferences(tenantId: String, projectId: String, vendorId: String): VendorPortalNotificationPreference?

    // Snapshots
    suspend fun saveAnalyticsSnapshot(snapshot: VendorPortalAnalyticsSnapshot): VendorPortalAnalyticsSnapshot
    suspend fun getLatestAnalyticsSnapshot(tenantId: String, projectId: String, vendorId: String, period: VendorPortalPeriod): VendorPortalAnalyticsSnapshot?
}
