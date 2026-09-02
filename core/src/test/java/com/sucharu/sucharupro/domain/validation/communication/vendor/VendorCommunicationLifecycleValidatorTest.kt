package com.sucharu.sucharupro.domain.validation.communication.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.VendorCommunicationStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorCommunicationLifecycleValidatorTest {

    @Test
    fun `validate allows READ to ACKNOWLEDGED`() {
        val result = VendorCommunicationLifecycleValidator.validate(
            VendorCommunicationStatus.READ,
            VendorCommunicationStatus.ACKNOWLEDGED
        )
        assertTrue("Expected success", result is DomainResult.Success<*>)
    }

    @Test
    fun `validate rejects from CANCELLED`() {
        val result = VendorCommunicationLifecycleValidator.validate(
            VendorCommunicationStatus.CANCELLED,
            VendorCommunicationStatus.ACKNOWLEDGED
        )
        assertTrue("Expected error from cancelled", result is DomainResult.Error)
    }

    @Test
    fun `validate allows DRAFT to QUEUED`() {
        val result = VendorCommunicationLifecycleValidator.validate(
            VendorCommunicationStatus.DRAFT,
            VendorCommunicationStatus.QUEUED
        )
        assertTrue("Expected success", result is DomainResult.Success<*>)
    }
}
