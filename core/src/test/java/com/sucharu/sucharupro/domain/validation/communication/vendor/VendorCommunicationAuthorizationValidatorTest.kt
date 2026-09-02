package com.sucharu.sucharupro.domain.validation.communication.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorCommunicationAuthorizationValidatorTest {

    @Test
    fun `validateCreate allows ADMIN`() {
        val result = VendorCommunicationAuthorizationValidator.validateCreate(
            UserRole.ADMIN,
            VendorCommunicationType.GENERAL_MESSAGE
        )
        assertTrue("Admin can access", result is DomainResult.Success<*>)
    }

    @Test
    fun `validateRead allows VENDOR with matching vendorId`() {
        val result = VendorCommunicationAuthorizationValidator.validateRead(
            UserRole.VENDOR,
            targetVendorId = "vendor-123",
            callerVendorId = "vendor-123"
        )
        assertTrue("Matching vendor can access", result is DomainResult.Success<*>)
    }

    @Test
    fun `validateRead rejects VENDOR with wrong vendorId`() {
        val result = VendorCommunicationAuthorizationValidator.validateRead(
            UserRole.VENDOR,
            targetVendorId = "vendor-123",
            callerVendorId = "vendor-456"
        )
        assertTrue("Wrong vendor rejected", result is DomainResult.Error)
    }
}
