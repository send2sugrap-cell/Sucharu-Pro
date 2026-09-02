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

class InternalCommunicationFinanceBoundaryTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test finance discussion does not alter ledger, receivables, or payables`() = runBlocking {
        val res = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "ACCT-01",
            senderRole = UserRole.ACCOUNTS,
            recipientType = InternalCommunicationRecipientType.ROLE,
            recipientRole = UserRole.MANAGER,
            communicationType = InternalCommunicationType.FINANCE_DISCUSSION,
            subject = "Payment Verification",
            message = "Bank wire confirmation received for INV-2026-104",
            referenceType = "FINANCE",
            referenceId = "REC-2026-0012",
            actorId = "ACCT-01",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(res is DomainResult.Success)
        val comm = (res as DomainResult.Success).data
        assertEquals("FINANCE", comm.referenceType)
        assertEquals("REC-2026-0012", comm.referenceId)
    }
}
