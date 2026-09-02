package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccountStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalMembershipStatus
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalValidator
import org.junit.Assert.*
import org.junit.Test

class VendorPortalValidatorTest {

    @Test
    fun testValidAccountStatusTransitions() {
        assertTrue(VendorPortalValidator.isValidAccountStatusTransition(VendorPortalAccountStatus.INVITED, VendorPortalAccountStatus.ACTIVE))
        assertTrue(VendorPortalValidator.isValidAccountStatusTransition(VendorPortalAccountStatus.ACTIVE, VendorPortalAccountStatus.SUSPENDED))
        assertTrue(VendorPortalValidator.isValidAccountStatusTransition(VendorPortalAccountStatus.SUSPENDED, VendorPortalAccountStatus.ACTIVE))
        assertTrue(VendorPortalValidator.isValidAccountStatusTransition(VendorPortalAccountStatus.ACTIVE, VendorPortalAccountStatus.DISABLED))
        assertTrue(VendorPortalValidator.isValidAccountStatusTransition(VendorPortalAccountStatus.DISABLED, VendorPortalAccountStatus.ACTIVE))
        assertTrue(VendorPortalValidator.isValidAccountStatusTransition(VendorPortalAccountStatus.ACTIVE, VendorPortalAccountStatus.REVOKED))

        // Disallowed: Revoked is terminal
        assertFalse(VendorPortalValidator.isValidAccountStatusTransition(VendorPortalAccountStatus.REVOKED, VendorPortalAccountStatus.ACTIVE))
    }

    @Test
    fun testValidMembershipStatusTransitions() {
        assertTrue(VendorPortalValidator.isValidMembershipStatusTransition(VendorPortalMembershipStatus.INVITED, VendorPortalMembershipStatus.PENDING_ACTIVATION))
        assertTrue(VendorPortalValidator.isValidMembershipStatusTransition(VendorPortalMembershipStatus.PENDING_ACTIVATION, VendorPortalMembershipStatus.ACTIVE))
        assertTrue(VendorPortalValidator.isValidMembershipStatusTransition(VendorPortalMembershipStatus.ACTIVE, VendorPortalMembershipStatus.SUSPENDED))
        assertTrue(VendorPortalValidator.isValidMembershipStatusTransition(VendorPortalMembershipStatus.SUSPENDED, VendorPortalMembershipStatus.ACTIVE))
        assertTrue(VendorPortalValidator.isValidMembershipStatusTransition(VendorPortalMembershipStatus.ACTIVE, VendorPortalMembershipStatus.REVOKED))

        // Disallowed: Revoked is terminal
        assertFalse(VendorPortalValidator.isValidMembershipStatusTransition(VendorPortalMembershipStatus.REVOKED, VendorPortalMembershipStatus.ACTIVE))
    }

    @Test
    fun testSeparationOfDutiesOnActivation() {
        // Self-activation by regular user fails
        try {
            VendorPortalValidator.enforceSeparationOfDutiesOnActivation(
                memberUserId = "usr_001",
                actorUserId = "usr_001",
                isInternalAdmin = false
            )
            fail("Expected IllegalStateException on self-activation")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Separation of Duties") == true)
        }

        // Activation by different user succeeds
        VendorPortalValidator.enforceSeparationOfDutiesOnActivation(
            memberUserId = "usr_001",
            actorUserId = "admin_002",
            isInternalAdmin = false
        )

        // Internal admin self-activation is permitted
        VendorPortalValidator.enforceSeparationOfDutiesOnActivation(
            memberUserId = "admin_001",
            actorUserId = "admin_001",
            isInternalAdmin = true
        )
    }

    @Test
    fun testIpWhitelistValidation() {
        // No whitelist permits all
        assertTrue(VendorPortalValidator.isIpAllowed("192.168.1.1", null))
        assertTrue(VendorPortalValidator.isIpAllowed("192.168.1.1", ""))

        // Matching IP in whitelist
        assertTrue(VendorPortalValidator.isIpAllowed("10.0.0.1", "10.0.0.1, 10.0.0.2, 192.168.1.5"))

        // Non-matching IP blocked
        assertFalse(VendorPortalValidator.isIpAllowed("203.0.113.1", "10.0.0.1, 10.0.0.2"))

        // Blank client IP with active whitelist blocked
        assertFalse(VendorPortalValidator.isIpAllowed(null, "10.0.0.1"))
    }
}
