package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPortalAnalyticsNotificationSearchDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalAnalyticsNotificationSearchRepository

/**
 * Implementation of VendorPortalAnalyticsNotificationSearchRepository (Module 13 Step 10).
 */
class VendorPortalAnalyticsNotificationSearchRepositoryImpl(
    private val dataSource: VendorPortalAnalyticsNotificationSearchDataSource
) : VendorPortalAnalyticsNotificationSearchRepository {

    override suspend fun saveNotification(notification: VendorPortalNotification): DomainResult<VendorPortalNotification> =
        try {
            DomainResult.Success(dataSource.saveNotification(notification))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save notification")
        }

    override suspend fun findNotificationById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String
    ): DomainResult<VendorPortalNotification?> =
        try {
            DomainResult.Success(dataSource.findNotificationById(tenantId, projectId, vendorId, notificationId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find notification")
        }

    override suspend fun listNotifications(
        tenantId: String,
        projectId: String,
        vendorId: String,
        category: VendorPortalNotificationCategory?,
        status: VendorPortalNotificationStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<VendorPortalNotification>> =
        try {
            DomainResult.Success(dataSource.listNotifications(tenantId, projectId, vendorId, category, status, limit, offset))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list notifications")
        }

    override suspend fun countUnreadNotifications(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<Int> =
        try {
            DomainResult.Success(dataSource.countUnreadNotifications(tenantId, projectId, vendorId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to count unread notifications")
        }

    override suspend fun markNotificationAsRead(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String,
        readAt: Long
    ): DomainResult<Boolean> =
        try {
            DomainResult.Success(dataSource.markNotificationAsRead(tenantId, projectId, vendorId, notificationId, readAt))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to mark notification as read")
        }

    override suspend fun markAllNotificationsAsRead(
        tenantId: String,
        projectId: String,
        vendorId: String,
        readAt: Long
    ): DomainResult<Int> =
        try {
            DomainResult.Success(dataSource.markAllNotificationsAsRead(tenantId, projectId, vendorId, readAt))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to mark all notifications as read")
        }

    override suspend fun archiveNotification(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String
    ): DomainResult<Boolean> =
        try {
            DomainResult.Success(dataSource.archiveNotification(tenantId, projectId, vendorId, notificationId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to archive notification")
        }

    override suspend fun savePreferences(preferences: VendorPortalNotificationPreference): DomainResult<VendorPortalNotificationPreference> =
        try {
            DomainResult.Success(dataSource.savePreferences(preferences))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save notification preferences")
        }

    override suspend fun getPreferences(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalNotificationPreference?> =
        try {
            DomainResult.Success(dataSource.getPreferences(tenantId, projectId, vendorId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to get notification preferences")
        }

    override suspend fun saveAnalyticsSnapshot(snapshot: VendorPortalAnalyticsSnapshot): DomainResult<VendorPortalAnalyticsSnapshot> =
        try {
            DomainResult.Success(dataSource.saveAnalyticsSnapshot(snapshot))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save analytics snapshot")
        }

    override suspend fun getLatestAnalyticsSnapshot(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod
    ): DomainResult<VendorPortalAnalyticsSnapshot?> =
        try {
            DomainResult.Success(dataSource.getLatestAnalyticsSnapshot(tenantId, projectId, vendorId, period))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to get latest analytics snapshot")
        }
}
