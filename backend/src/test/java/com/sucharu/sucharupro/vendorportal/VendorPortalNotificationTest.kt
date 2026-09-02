package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalAnalyticsNotificationSearchDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalAnalyticsNotificationSearchRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalNotificationTest {

    private lateinit var dataSource: FakeVendorPortalAnalyticsNotificationSearchDataSource
    private lateinit var repository: VendorPortalAnalyticsNotificationSearchRepositoryImpl

    @Before
    fun setup() {
        dataSource = FakeVendorPortalAnalyticsNotificationSearchDataSource()
        repository = VendorPortalAnalyticsNotificationSearchRepositoryImpl(dataSource)
    }

    @Test
    fun testNotificationLifecycleAndStatusTransitions() = runBlocking {
        val notif = VendorPortalNotification(
            notificationId = "NOTIF-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            category = VendorPortalNotificationCategory.PURCHASE_ORDER,
            severity = VendorPortalNotificationSeverity.NORMAL,
            status = VendorPortalNotificationStatus.UNREAD,
            title = "New Purchase Order Received",
            message = "PO #PO-2026-001 has been issued to your account.",
            relatedEntityType = "PURCHASE_ORDER",
            relatedEntityId = "PO-001",
            deepLinkTarget = "/vendor-portal/purchase-orders/PO-001"
        )

        val saveRes = repository.saveNotification(notif)
        assertTrue(saveRes is DomainResult.Success)

        val countRes = repository.countUnreadNotifications("TENANT-001", "PRJ-001", "VND-001")
        assertEquals(1, (countRes as DomainResult.Success).data)

        val markRes = repository.markNotificationAsRead("TENANT-001", "PRJ-001", "VND-001", "NOTIF-001")
        assertTrue((markRes as DomainResult.Success).data)

        val countAfter = repository.countUnreadNotifications("TENANT-001", "PRJ-001", "VND-001")
        assertEquals(0, (countAfter as DomainResult.Success).data)

        val fetched = repository.findNotificationById("TENANT-001", "PRJ-001", "VND-001", "NOTIF-001")
        assertEquals(VendorPortalNotificationStatus.READ, (fetched as DomainResult.Success).data?.status)
    }

    @Test
    fun testPreferencesSaveAndFetch() = runBlocking {
        val pref = VendorPortalNotificationPreference(
            preferenceId = "PREF-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            emailEnabled = true,
            inAppEnabled = true,
            pushEnabled = true,
            importantOnlyMode = false,
            disabledCategories = setOf(VendorPortalNotificationCategory.SYSTEM),
            minSeverity = VendorPortalNotificationSeverity.LOW
        )

        val saveRes = repository.savePreferences(pref)
        assertTrue(saveRes is DomainResult.Success)

        val fetched = repository.getPreferences("TENANT-001", "PRJ-001", "VND-001")
        val fetchedData = (fetched as DomainResult.Success).data
        assertNotNull(fetchedData)
        assertTrue(fetchedData!!.pushEnabled)
        assertTrue(fetchedData.disabledCategories.contains(VendorPortalNotificationCategory.SYSTEM))
    }
}
