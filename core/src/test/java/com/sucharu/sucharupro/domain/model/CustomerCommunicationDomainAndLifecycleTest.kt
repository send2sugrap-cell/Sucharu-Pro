package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.*
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.communication.customer.CustomerCommunicationAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.communication.customer.CustomerCommunicationLifecycleValidator
import com.sucharu.sucharupro.domain.validation.communication.customer.CustomerCommunicationValidator
import org.junit.Assert.*
import org.junit.Test

class CustomerCommunicationDomainAndLifecycleTest {

    private val validCommunication = CustomerCommunication(
        communicationId = "comm-101",
        communicationNo = "CCM-2026-00001",
        projectId = "PRJ-01",
        customerId = "CUST-001",
        recipientUserId = "USER-001",
        communicationType = CustomerCommunicationType.ORDER_UPDATE,
        notificationId = "NTF-101",
        title = "Order Processing Update",
        message = "Your print order #100 is in production.",
        createdBy = "ADMIN-01"
    )

    @Test
    fun `CustomerCommunication passes structural validation`() {
        val result = CustomerCommunicationValidator.validate(validCommunication)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `CustomerCommunication constructor rejects missing required fields`() {
        assertThrows(IllegalArgumentException::class.java) {
            validCommunication.copy(title = "   ")
        }
    }

    @Test
    fun `CustomerCommunication constructor rejects sensitive security keys in metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            validCommunication.copy(
                metadata = mapOf("user_secret_token" to "abc123xyz")
            )
        }
    }

    @Test
    fun `CustomerCommunicationLifecycleValidator allows canonical lifecycle transitions`() {
        assertTrue(CustomerCommunicationLifecycleValidator.validateTransition(CustomerCommunicationStatus.DRAFT, CustomerCommunicationStatus.QUEUED) is DomainResult.Success)
        assertTrue(CustomerCommunicationLifecycleValidator.validateTransition(CustomerCommunicationStatus.QUEUED, CustomerCommunicationStatus.SENT) is DomainResult.Success)
        assertTrue(CustomerCommunicationLifecycleValidator.validateTransition(CustomerCommunicationStatus.SENT, CustomerCommunicationStatus.DELIVERED) is DomainResult.Success)
        assertTrue(CustomerCommunicationLifecycleValidator.validateTransition(CustomerCommunicationStatus.DELIVERED, CustomerCommunicationStatus.READ) is DomainResult.Success)
        assertTrue(CustomerCommunicationLifecycleValidator.validateTransition(CustomerCommunicationStatus.READ, CustomerCommunicationStatus.ACKNOWLEDGED) is DomainResult.Success)
    }

    @Test
    fun `CustomerCommunicationLifecycleValidator blocks invalid backwards transition from terminal state`() {
        assertTrue(CustomerCommunicationLifecycleValidator.validateTransition(CustomerCommunicationStatus.ACKNOWLEDGED, CustomerCommunicationStatus.DRAFT) is DomainResult.Error)
        assertTrue(CustomerCommunicationLifecycleValidator.validateTransition(CustomerCommunicationStatus.CANCELLED, CustomerCommunicationStatus.QUEUED) is DomainResult.Error)
    }

    @Test
    fun `CustomerCommunicationPolicy properly maps business events`() {
        val orderDecision = CustomerCommunicationPolicy.evaluateEvent("ORDER_CREATED")
        assertTrue(orderDecision.shouldCommunicate)
        assertEquals(CustomerCommunicationType.ORDER_UPDATE, orderDecision.communicationType)
        assertFalse(orderDecision.requiresAcknowledgement)

        val approvalDecision = CustomerCommunicationPolicy.evaluateEvent("DESIGN_APPROVAL_REQUIRED")
        assertTrue(approvalDecision.shouldCommunicate)
        assertEquals(CustomerCommunicationType.APPROVAL_REQUEST, approvalDecision.communicationType)
        assertTrue(approvalDecision.requiresAcknowledgement)
        assertEquals(NotificationPriority.HIGH, approvalDecision.defaultPriority)
    }

    @Test
    fun `CustomerCommunicationRecipientResolver resolves and validates correctly`() {
        val res = CustomerCommunicationRecipientResolver.resolve("PRJ-01", "CUST-100", null)
        assertTrue(res is DomainResult.Success)
        val data = (res as DomainResult.Success).data
        assertEquals("CUST-100", data.customerId)
        assertEquals("CUST-100", data.recipientUserId)
    }

    @Test
    fun `CustomerCommunicationAuthorizationValidator protects cross-customer and vendor isolation`() {
        // Customer viewing own communication -> Success
        val ownResult = CustomerCommunicationAuthorizationValidator.validateView(
            communication = validCommunication.copy(customerId = "CUST-001"),
            requestProjectId = "PRJ-01",
            actorId = "CUST-001",
            callerRole = UserRole.CUSTOMER
        )
        assertTrue(ownResult is DomainResult.Success)

        // Customer viewing other customer's communication -> Error
        val otherResult = CustomerCommunicationAuthorizationValidator.validateView(
            communication = validCommunication.copy(customerId = "CUST-001"),
            requestProjectId = "PRJ-01",
            actorId = "CUST-002",
            callerRole = UserRole.CUSTOMER
        )
        assertTrue(otherResult is DomainResult.Error)

        // Vendor accessing customer communication -> Error
        val vendorResult = CustomerCommunicationAuthorizationValidator.validateView(
            communication = validCommunication,
            requestProjectId = "PRJ-01",
            actorId = "VENDOR-01",
            callerRole = UserRole.VENDOR
        )
        assertTrue(vendorResult is DomainResult.Error)
    }
}
