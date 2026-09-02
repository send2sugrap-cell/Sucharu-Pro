package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorQualityValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorQualityInspectionValidatorTest {

    @Test
    fun testValidInspectionPasses() {
        val item = VendorQualityInspectionItem(
            inspectionItemId = "vqii_01",
            inspectionId = "vqi_01",
            itemDescription = "Offset Paper 100gsm",
            receivedQuantity = BigDecimal("500"),
            acceptedQuantity = BigDecimal("450"),
            rejectedQuantity = BigDecimal("50"),
            conditionalQuantity = BigDecimal.ZERO
        )

        val inspection = VendorQualityInspection(
            inspectionId = "vqi_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            inspectionReference = "VQI-2026-0001",
            receivedQuantity = BigDecimal("500"),
            acceptedQuantity = BigDecimal("450"),
            rejectedQuantity = BigDecimal("50"),
            items = listOf(item)
        )

        val result = VendorQualityValidator.validateInspection(inspection)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun testExcessiveInspectedQuantityFails() {
        val inspection = VendorQualityInspection(
            inspectionId = "vqi_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            inspectionReference = "VQI-2026-0001",
            receivedQuantity = BigDecimal("100"),
            acceptedQuantity = BigDecimal("80"),
            rejectedQuantity = BigDecimal("30") // 80 + 30 = 110 > 100
        )

        val result = VendorQualityValidator.validateInspection(inspection)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun testBlankIdentifiersFail() {
        val blankId = VendorQualityInspection(
            inspectionId = "",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            inspectionReference = "VQI-01",
            receivedQuantity = BigDecimal("100")
        )
        assertTrue(VendorQualityValidator.validateInspection(blankId) is DomainResult.Error)

        val blankRef = VendorQualityInspection(
            inspectionId = "vqi_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            inspectionReference = "",
            receivedQuantity = BigDecimal("100")
        )
        assertTrue(VendorQualityValidator.validateInspection(blankRef) is DomainResult.Error)
    }

    @Test
    fun testDefectRateCalculations() {
        val rate = VendorQualityValidator.computeDefectRate(BigDecimal("5"), BigDecimal("100"))
        assertEquals(BigDecimal("0.0500"), rate)

        val zeroInspected = VendorQualityValidator.computeDefectRate(BigDecimal("5"), BigDecimal.ZERO)
        assertEquals(BigDecimal.ZERO, zeroInspected)
    }
}
