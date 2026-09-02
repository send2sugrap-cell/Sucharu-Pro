package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, in-memory fake data source for Vendor Portal Analytics, Notifications & Search tests (Module 13 Step 10).
 */
class FakeVendorPortalAnalyticsNotificationSearchDataSource : VendorPortalAnalyticsNotificationSearchDataSource {

    private val notifications = ConcurrentHashMap<String, VendorPortalNotification>()
    private val preferences = ConcurrentHashMap<String, VendorPortalNotificationPreference>()
    private val snapshots = ConcurrentHashMap<String, VendorPortalAnalyticsSnapshot>()

    override suspend fun saveNotification(notification: VendorPortalNotification): VendorPortalNotification {
        val key = "${notification.tenantId}:${notification.notificationId}"
        notifications[key] = notification
        return notification
    }

    override suspend fun findNotificationById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String
    ): VendorPortalNotification? {
        val notif = notifications["$tenantId:$notificationId"] ?: return null
        return if (notif.projectId == projectId && notif.vendorId == vendorId) notif else null
    }

    override suspend fun listNotifications(
        tenantId: String,
        projectId: String,
        vendorId: String,
        category: VendorPortalNotificationCategory?,
        status: VendorPortalNotificationStatus?,
        limit: Int,
        offset: Int
    ): List<VendorPortalNotification> {
        return notifications.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { category == null || it.category == category }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
    }

    override suspend fun countUnreadNotifications(tenantId: String, projectId: String, vendorId: String): Int {
        return notifications.values.count {
            it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId && it.status == VendorPortalNotificationStatus.UNREAD
        }
    }

    override suspend fun markNotificationAsRead(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String,
        readAt: Long
    ): Boolean {
        val key = "$tenantId:$notificationId"
        val existing = notifications[key] ?: return false
        if (existing.projectId != projectId || existing.vendorId != vendorId) return false
        val updated = existing.copy(
            status = VendorPortalNotificationStatus.READ,
            readAt = readAt
        )
        notifications[key] = updated
        return true
    }

    override suspend fun markAllNotificationsAsRead(
        tenantId: String,
        projectId: String,
        vendorId: String,
        readAt: Long
    ): Int {
        var count = 0
        notifications.forEach { (key, existing) ->
            if (existing.tenantId == tenantId && existing.projectId == projectId && existing.vendorId == vendorId && existing.status == VendorPortalNotificationStatus.UNREAD) {
                notifications[key] = existing.copy(status = VendorPortalNotificationStatus.READ, readAt = readAt)
                count++
            }
        }
        return count
    }

    override suspend fun archiveNotification(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String
    ): Boolean {
        val key = "$tenantId:$notificationId"
        val existing = notifications[key] ?: return false
        if (existing.projectId != projectId || existing.vendorId != vendorId) return false
        val updated = existing.copy(status = VendorPortalNotificationStatus.ARCHIVED)
        notifications[key] = updated
        return true
    }

    override suspend fun savePreferences(preferences: VendorPortalNotificationPreference): VendorPortalNotificationPreference {
        val key = "${preferences.tenantId}:${preferences.projectId}:${preferences.vendorId}"
        this.preferences[key] = preferences
        return preferences
    }

    override suspend fun getPreferences(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): VendorPortalNotificationPreference? {
        val key = "$tenantId:$projectId:$vendorId"
        return preferences[key]
    }

    override suspend fun saveAnalyticsSnapshot(snapshot: VendorPortalAnalyticsSnapshot): VendorPortalAnalyticsSnapshot {
        val key = "${snapshot.tenantId}:${snapshot.projectId}:${snapshot.vendorId}:${snapshot.period.name}"
        snapshots[key] = snapshot
        return snapshot
    }

    override suspend fun getLatestAnalyticsSnapshot(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod
    ): VendorPortalAnalyticsSnapshot? {
        val key = "$tenantId:$projectId:$vendorId:${period.name}"
        return snapshots[key]
    }
}
