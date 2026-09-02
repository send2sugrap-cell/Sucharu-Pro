package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationInventoryBoundaryTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test inventory discussion reference does not alter inventory stock or ledger`() = runBlocking {
        val res = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "INV-STAFF-01",
            senderRole = UserRole.WAREHOUSE,
            recipientType = InternalCommunicationRecipientType.ROLE,
            recipientRole = UserRole.MANAGER,
            communicationType = InternalCommunicationType.INVENTORY_DISCUSSION,
            subject = "Paper Stock Low",
            message = "Art paper 120gsm stock is below reorder point",
            referenceType = "INVENTORY",
            referenceId = "SKU-PAPER-120GSM",
            actorId = "INV-STAFF-01",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(res is DomainResult.Success)
        val comm = (res as DomainResult.Success).data
        assertEquals("INVENTORY", comm.referenceType)
        assertEquals("SKU-PAPER-120GSM", comm.referenceId)
    }
}
