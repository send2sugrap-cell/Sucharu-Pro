package com.sucharu.sucharupro.domain.validation.communication.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.VendorCommunicationType
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorCommunicationValidatorTest {

    @Test
    fun `validate returns success for valid inputs`() {
        val result = VendorCommunicationValidator.validate(
            projectId = "proj-1",
            vendorId = "vendor-1",
            communicationType = VendorCommunicationType.PURCHASE_UPDATE.name,
            subject = "Order #123",
            message = "Please process this order.",
            createdBy = "actor-1"
        )
        assertTrue("Expected success", result is DomainResult.Success<*>)
    }

    @Test
    fun `validate returns error for blank vendorId`() {
        val result = VendorCommunicationValidator.validate(
            projectId = "proj-1",
            vendorId = "   ",
            communicationType = VendorCommunicationType.PURCHASE_UPDATE.name,
            subject = "Order #123",
            message = "Please process this order.",
            createdBy = "actor-1"
        )
        assertTrue("Expected error", result is DomainResult.Error)
    }

    @Test
    fun `validate returns error for blank subject`() {
        val result = VendorCommunicationValidator.validate(
            projectId = "proj-1",
            vendorId = "vendor-1",
            communicationType = VendorCommunicationType.PURCHASE_UPDATE.name,
            subject = "",
            message = "Please process this order.",
            createdBy = "actor-1"
        )
        assertTrue("Expected error", result is DomainResult.Error)
    }
}
