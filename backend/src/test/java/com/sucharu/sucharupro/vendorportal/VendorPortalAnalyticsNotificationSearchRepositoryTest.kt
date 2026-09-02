package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalAnalyticsNotificationSearchDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalAnalyticsNotificationSearchRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalAnalyticsNotificationSearchRepositoryTest {

    private lateinit var dataSource: FakeVendorPortalAnalyticsNotificationSearchDataSource
    private lateinit var repository: VendorPortalAnalyticsNotificationSearchRepositoryImpl

    @Before
    fun setup() {
        dataSource = FakeVendorPortalAnalyticsNotificationSearchDataSource()
        repository = VendorPortalAnalyticsNotificationSearchRepositoryImpl(dataSource)
    }

    @Test
    fun testNotificationCRUD() = runBlocking {
        val notif = VendorPortalNotification(
            notificationId = "NOTIF-999",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            category = VendorPortalNotificationCategory.SETTLEMENT,
            severity = VendorPortalNotificationSeverity.HIGH,
            status = VendorPortalNotificationStatus.UNREAD,
            title = "Settlement Ready for Acknowledgement",
            message = "Statement #SETTL-2026-001 has been posted.",
            relatedEntityType = "SETTLEMENT",
            relatedEntityId = "SETTL-001"
        )

        val saveRes = repository.saveNotification(notif)
        assertTrue(saveRes is DomainResult.Success)

        val listRes = repository.listNotifications("TENANT-001", "PRJ-001", "VND-001")
        assertEquals(1, (listRes as DomainResult.Success).data.size)

        val archiveRes = repository.archiveNotification("TENANT-001", "PRJ-001", "VND-001", "NOTIF-999")
        assertTrue((archiveRes as DomainResult.Success).data)

        val fetched = repository.findNotificationById("TENANT-001", "PRJ-001", "VND-001", "NOTIF-999")
        assertEquals(VendorPortalNotificationStatus.ARCHIVED, (fetched as DomainResult.Success).data?.status)
    }

    @Test
    fun testAnalyticsSnapshotSaveAndRetrieve() = runBlocking {
        val snapshot = VendorPortalAnalyticsSnapshot(
            snapshotId = "SNAP-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            period = VendorPortalPeriod.LAST_30_DAYS,
            calculationVersion = 1,
            metricsJson = "{\"score\": 95.0}"
        )

        val saveRes = repository.saveAnalyticsSnapshot(snapshot)
        assertTrue(saveRes is DomainResult.Success)

        val fetched = repository.getLatestAnalyticsSnapshot("TENANT-001", "PRJ-001", "VND-001", VendorPortalPeriod.LAST_30_DAYS)
        assertEquals("SNAP-001", (fetched as DomainResult.Success).data?.snapshotId)
    }
}
