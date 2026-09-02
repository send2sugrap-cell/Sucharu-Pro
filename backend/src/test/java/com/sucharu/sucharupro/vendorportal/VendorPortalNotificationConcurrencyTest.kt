package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalAnalyticsNotificationSearchDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalAnalyticsNotificationSearchRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalNotification
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalNotificationCategory
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalNotificationSeverity
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalNotificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorPortalNotificationConcurrencyTest {

    @Test
    fun testConcurrentNotificationSavesAndReadMarks() = runBlocking {
        val dataSource = FakeVendorPortalAnalyticsNotificationSearchDataSource()
        val repository = VendorPortalAnalyticsNotificationSearchRepositoryImpl(dataSource)

        val total = 50
        val deferredSaves = (1..total).map { i ->
            async(Dispatchers.Default) {
                repository.saveNotification(
                    VendorPortalNotification(
                        notificationId = "NOTIF-CONCUR-$i",
                        tenantId = "TENANT-001",
                        projectId = "PRJ-001",
                        vendorId = "VND-001",
                        category = VendorPortalNotificationCategory.OPERATIONS,
                        severity = VendorPortalNotificationSeverity.NORMAL,
                        status = VendorPortalNotificationStatus.UNREAD,
                        title = "Concurrent Notification #$i",
                        message = "Testing concurrent notification persistence."
                    )
                )
            }
        }
        val saveResults = deferredSaves.awaitAll()
        assertEquals(total, saveResults.filterIsInstance<DomainResult.Success<*>>().size)

        val countRes = repository.countUnreadNotifications("TENANT-001", "PRJ-001", "VND-001")
        assertEquals(total, (countRes as DomainResult.Success).data)

        // Concurrently mark all as read
        val deferredMarks = (1..total).map { i ->
            async(Dispatchers.Default) {
                repository.markNotificationAsRead("TENANT-001", "PRJ-001", "VND-001", "NOTIF-CONCUR-$i")
            }
        }
        val markResults = deferredMarks.awaitAll()
        assertTrue(markResults.all { it is DomainResult.Success && it.data })

        val countAfter = repository.countUnreadNotifications("TENANT-001", "PRJ-001", "VND-001")
        assertEquals(0, (countAfter as DomainResult.Success).data)
    }
}
