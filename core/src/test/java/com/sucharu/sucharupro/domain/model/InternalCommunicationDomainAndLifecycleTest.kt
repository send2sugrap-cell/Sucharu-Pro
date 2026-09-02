package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.communication.internal.InternalCommunicationAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.communication.internal.InternalCommunicationLifecycleValidator
import com.sucharu.sucharupro.domain.validation.communication.internal.InternalCommunicationValidator
import org.junit.Assert.*
import org.junit.Test

class InternalCommunicationDomainAndLifecycleTest {

    private val validCommunication = InternalCommunication(
        communicationId = "icm-01",
        communicationNo = "ICM-2026-00001",
        projectId = "PRJ-01",
        senderUserId = "STAFF-001",
        senderRole = UserRole.STAFF,
        recipientType = InternalCommunicationRecipientType.USER,
        recipientUserIds = setOf("STAFF-002"),
        communicationType = InternalCommunicationType.DIRECT_MESSAGE,
        subject = "Shift handover notes",
        message = "Print job #102 is 80% complete on Heidelberg press.",
        createdBy = "STAFF-001",
        updatedBy = "STAFF-001"
    )

    @Test
    fun `InternalCommunication passes validation for valid fields`() {
        val res = InternalCommunicationValidator.validate(validCommunication)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `InternalCommunication constructor rejects missing required fields`() {
        assertThrows(IllegalArgumentException::class.java) {
            validCommunication.copy(subject = "   ")
        }
    }

    @Test
    fun `InternalCommunication constructor rejects security secrets in metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            validCommunication.copy(metadata = mapOf("api_key" to "unauthorized_token_value"))
        }
    }

    @Test
    fun `InternalCommunicationLifecycleValidator permits allowed transitions`() {
        assertTrue(InternalCommunicationLifecycleValidator.validateTransition(InternalCommunicationStatus.DRAFT, InternalCommunicationStatus.QUEUED) is DomainResult.Success)
        assertTrue(InternalCommunicationLifecycleValidator.validateTransition(InternalCommunicationStatus.QUEUED, InternalCommunicationStatus.SENT) is DomainResult.Success)
        assertTrue(InternalCommunicationLifecycleValidator.validateTransition(InternalCommunicationStatus.SENT, InternalCommunicationStatus.DELIVERED) is DomainResult.Success)
        assertTrue(InternalCommunicationLifecycleValidator.validateTransition(InternalCommunicationStatus.DELIVERED, InternalCommunicationStatus.READ) is DomainResult.Success)
        assertTrue(InternalCommunicationLifecycleValidator.validateTransition(InternalCommunicationStatus.READ, InternalCommunicationStatus.ACKNOWLEDGED) is DomainResult.Success)
    }

    @Test
    fun `InternalCommunicationLifecycleValidator blocks invalid transitions`() {
        assertTrue(InternalCommunicationLifecycleValidator.validateTransition(InternalCommunicationStatus.ARCHIVED, InternalCommunicationStatus.DRAFT) is DomainResult.Error)
        assertTrue(InternalCommunicationLifecycleValidator.validateTransition(InternalCommunicationStatus.CANCELLED, InternalCommunicationStatus.QUEUED) is DomainResult.Error)
    }

    @Test
    fun `InternalCommunicationAuthorizationValidator blocks Customer and Vendor roles`() {
        val custResult = InternalCommunicationAuthorizationValidator.validateInternalUser(UserRole.CUSTOMER)
        assertTrue(custResult is DomainResult.Error)

        val vendorResult = InternalCommunicationAuthorizationValidator.validateInternalUser(UserRole.VENDOR)
        assertTrue(vendorResult is DomainResult.Error)

        val staffResult = InternalCommunicationAuthorizationValidator.validateInternalUser(UserRole.STAFF)
        assertTrue(staffResult is DomainResult.Success)
    }

    @Test
    fun `InternalCommunicationAuthorizationValidator blocks unauthorized broadcast from staff`() {
        val staffBroad = InternalCommunicationAuthorizationValidator.validateBroadcast(
            InternalCommunicationRecipientType.ALL_INTERNAL_USERS,
            UserRole.STAFF
        )
        assertTrue(staffBroad is DomainResult.Error)

        val adminBroad = InternalCommunicationAuthorizationValidator.validateBroadcast(
            InternalCommunicationRecipientType.ALL_INTERNAL_USERS,
            UserRole.ADMIN
        )
        assertTrue(adminBroad is DomainResult.Success)
    }
}
