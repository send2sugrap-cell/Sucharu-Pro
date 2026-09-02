package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeVendorCommunicationDataSourceTest {

    private val dataSource = FakeVendorCommunicationDataSource()

    @Test
    fun `saveCommunication creates and retrieves successfully`() = runBlocking {
        val comm = VendorCommunication(
            communicationId = "test-1",
            communicationNo = "VCM-2026-00001",
            projectId = "proj",
            vendorId = "vendor-1",
            communicationType = VendorCommunicationType.GENERAL_MESSAGE,
            status = VendorCommunicationStatus.SENT,
            priority = com.sucharu.sucharupro.domain.model.notification.NotificationPriority.NORMAL,
            subject = "Subject",
            message = "Message",
            requiresAcknowledgement = true,
            createdBy = "admin",
            createdAt = System.currentTimeMillis()
        )

        dataSource.saveCommunication(comm)

        val retrieved = dataSource.getCommunicationById("proj", "test-1")
        assertNotNull(retrieved)
        assertEquals("test-1", retrieved?.communicationId)
    }

    @Test
    fun `recordHistory records properly`() = runBlocking {
        val history = VendorCommunicationHistory(
            historyId = "h-1",
            communicationId = "test-1",
            projectId = "proj",
            vendorId = "vendor-1",
            action = "SENT",
            previousStatus = null,
            newStatus = VendorCommunicationStatus.SENT,
            performedBy = "admin",
            performedAt = System.currentTimeMillis()
        )

        dataSource.recordHistory(history)
        val list = dataSource.getHistory("proj", "test-1")
        assertEquals(1, list.size)
        assertEquals("h-1", list[0].historyId)
    }
}
