package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.vendor.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorQualityInspectionDomainTest {

    @Test
    fun testQualityInspectionCreationAndQuantitySum() {
        val item1 = VendorQualityInspectionItem(
            inspectionItemId = "vqii_01",
            inspectionId = "vqi_01",
            itemDescription = "Paper Board 300gsm",
            receivedQuantity = BigDecimal("100"),
            acceptedQuantity = BigDecimal("80"),
            rejectedQuantity = BigDecimal("15"),
            conditionalQuantity = BigDecimal("5"),
            defectCount = 2,
            inspectionResult = InspectionResult.CONDITIONAL
        )

        val inspection = VendorQualityInspection(
            inspectionId = "vqi_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            inspectionReference = "VQI-2026-0001",
            receivedQuantity = BigDecimal("100"),
            acceptedQuantity = BigDecimal("80"),
            rejectedQuantity = BigDecimal("15"),
            conditionalQuantity = BigDecimal("5"),
            overallResult = InspectionResult.CONDITIONAL,
            items = listOf(item1)
        )

        assertEquals("vqi_01", inspection.inspectionId)
        assertEquals(VendorInspectionStatus.DRAFT, inspection.inspectionStatus)
        assertEquals(BigDecimal("100"), inspection.receivedQuantity)
        assertEquals(BigDecimal("80"), inspection.acceptedQuantity)
        assertEquals(BigDecimal("15"), inspection.rejectedQuantity)
        assertEquals(BigDecimal("5"), inspection.conditionalQuantity)
        assertEquals(1, inspection.items.size)
    }

    @Test
    fun testInspectionStateTransitions() {
        assertTrue(VendorInspectionStatus.DRAFT.canTransitionTo(VendorInspectionStatus.IN_PROGRESS))
        assertTrue(VendorInspectionStatus.DRAFT.canTransitionTo(VendorInspectionStatus.CANCELLED))
        assertTrue(VendorInspectionStatus.IN_PROGRESS.canTransitionTo(VendorInspectionStatus.PASSED))
        assertTrue(VendorInspectionStatus.IN_PROGRESS.canTransitionTo(VendorInspectionStatus.PARTIALLY_PASSED))
        assertTrue(VendorInspectionStatus.IN_PROGRESS.canTransitionTo(VendorInspectionStatus.FAILED))
        assertTrue(VendorInspectionStatus.IN_PROGRESS.canTransitionTo(VendorInspectionStatus.CANCELLED))

        assertFalse(VendorInspectionStatus.PASSED.canTransitionTo(VendorInspectionStatus.IN_PROGRESS))
        assertFalse(VendorInspectionStatus.FAILED.canTransitionTo(VendorInspectionStatus.DRAFT))
        assertFalse(VendorInspectionStatus.CANCELLED.canTransitionTo(VendorInspectionStatus.IN_PROGRESS))
    }
}
