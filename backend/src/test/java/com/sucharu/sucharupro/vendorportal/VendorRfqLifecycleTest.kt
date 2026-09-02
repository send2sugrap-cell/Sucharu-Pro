package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.VendorRfqStatus
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorRfqValidator
import org.junit.Assert.*
import org.junit.Test

class VendorRfqLifecycleTest {

    @Test
    fun testLegalRfqTransitions() {
        assertTrue(VendorRfqStatus.DRAFT.canTransitionTo(VendorRfqStatus.PUBLISHED))
        assertTrue(VendorRfqStatus.PUBLISHED.canTransitionTo(VendorRfqStatus.OPEN))
        assertTrue(VendorRfqStatus.OPEN.canTransitionTo(VendorRfqStatus.CLOSED))
        assertTrue(VendorRfqStatus.CLOSED.canTransitionTo(VendorRfqStatus.EVALUATION))
        assertTrue(VendorRfqStatus.EVALUATION.canTransitionTo(VendorRfqStatus.AWARDED))
    }

    @Test
    fun testIllegalRfqTransitions() {
        assertFalse(VendorRfqStatus.DRAFT.canTransitionTo(VendorRfqStatus.AWARDED))
        assertFalse(VendorRfqStatus.AWARDED.canTransitionTo(VendorRfqStatus.OPEN))
        assertFalse(VendorRfqStatus.CANCELLED.canTransitionTo(VendorRfqStatus.DRAFT))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRejectsIllegalRfqTransition() {
        VendorRfqValidator.validateRfqTransition(VendorRfqStatus.DRAFT, VendorRfqStatus.AWARDED)
    }
}
