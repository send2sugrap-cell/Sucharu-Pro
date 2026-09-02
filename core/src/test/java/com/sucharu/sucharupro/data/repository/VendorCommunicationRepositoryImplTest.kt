package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeVendorCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorCommunicationRepositoryImplTest {

    private val dataSource = FakeVendorCommunicationDataSource()
    private val notificationDataSource = FakeNotificationDataSource()
    private val notificationRepo = NotificationRepositoryImpl(notificationDataSource)
    private val repository = VendorCommunicationRepositoryImpl(dataSource, notificationRepo)

    @Test
    fun `createCommunication orchestrates correctly`() = runBlocking {
        val result = repository.createCommunication(
            projectId = "proj",
            vendorId = "vendor-1",
            communicationType = VendorCommunicationType.PURCHASE_UPDATE,
            subject = "Order 1",
            message = "Please process",
            actorId = "admin-1",
            callerRole = UserRole.ADMIN
        )

        assertTrue("Expected creation to succeed", result is DomainResult.Success<*>)

        if (result is DomainResult.Success) {
            val commId = result.data.communicationId
            val history = dataSource.getHistory("proj", commId)
            assertTrue("Expected history to be written", history.isNotEmpty())
        }
    }

    @Test
    fun `acknowledge fails if wrong vendor`() = runBlocking {
        val createResult = repository.createCommunication(
            projectId = "proj",
            vendorId = "vendor-1",
            communicationType = VendorCommunicationType.PURCHASE_UPDATE,
            subject = "Order 1",
            message = "Please process",
            requiresAcknowledgement = true,
            actorId = "admin-1",
            callerRole = UserRole.ADMIN
        )
        val commId = (createResult as DomainResult.Success).data.communicationId

        val ackResult = repository.acknowledge(
            projectId = "proj",
            communicationId = commId,
            acknowledgeMessage = "Ok",
            actorId = "vendor-2",
            callerRole = UserRole.VENDOR,
            callerVendorId = "vendor-2"
        )
        assertTrue("Expected failure due to isolation", ackResult is DomainResult.Error)
    }
}
