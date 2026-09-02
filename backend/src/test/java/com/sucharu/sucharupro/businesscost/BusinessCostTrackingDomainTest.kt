package com.sucharu.sucharupro.businesscost

import com.sucharu.sucharupro.domain.model.businesscost.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businesscost.BusinessCostValidators
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BusinessCostTrackingDomainTest {

    private val activeCostCenter = BusinessCostCenter(
        id = "CC-01",
        code = "CC-01",
        name = "Offset Printing",
        description = null,
        isActive = true,
        parentCostCenterId = null,
        version = 1,
        createdAt = 0,
        updatedAt = 0,
        tenantId = "TENANT-001",
        projectId = "PRJ-001"
    )

    private val activeCostCategory = BusinessCostCategory(
        id = "CAT-01",
        code = "CAT-01",
        name = "Paper Stock",
        description = null,
        isActive = true,
        isSystemDefined = false,
        parentCategoryId = null,
        version = 1,
        createdAt = 0,
        updatedAt = 0,
        tenantId = "TENANT-001",
        projectId = "PRJ-001"
    )

    @Test
    fun testValidCostTrackingValidation() {
        val res = BusinessCostValidators.validateCostTracking(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            sourceId = "EXP-1001",
            costCenter = activeCostCenter,
            costCategory = activeCostCategory,
            amount = BigDecimal("15000.5000"),
            currency = "BDT",
            createdBy = "USER-01"
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testNegativeAmountFails() {
        val res = BusinessCostValidators.validateCostTracking(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            sourceId = "EXP-1001",
            costCenter = activeCostCenter,
            costCategory = activeCostCategory,
            amount = BigDecimal("-50.0000"),
            currency = "BDT",
            createdBy = "USER-01"
        )
        assertTrue(res is DomainResult.Error)
        assertEquals("Tracked cost amount cannot be negative.", (res as DomainResult.Error).message)
    }

    @Test
    fun testExcessPrecisionFails() {
        val res = BusinessCostValidators.validatePrecision(
            amount = BigDecimal("100.12345"),
            fieldName = "Tracked amount"
        )
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("precision cannot exceed 4 decimal places"))
    }

    @Test
    fun testInvalidCurrencyFails() {
        val res = BusinessCostValidators.validateCurrency("XYZ12")
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("Currency code must be a valid 3-letter ISO-4217 code"))
    }

    @Test
    fun testReclassificationValidation() {
        val tracking = BusinessCostTracking(
            id = "TRK-01",
            sourceType = BusinessCostTrackingSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-01",
            costCenterId = "CC-01",
            costCategoryId = "CAT-01",
            jobId = null,
            amount = BigDecimal("100.0000"),
            currency = "BDT",
            tenantId = "TENANT-001",
            projectId = "PRJ-001"
        )

        val validRes = BusinessCostValidators.validateReclassification(
            existingTracking = tracking,
            newCostCenter = activeCostCenter,
            newCostCategory = activeCostCategory,
            reason = "Corrected department assignment",
            actorId = "USER-ADMIN"
        )
        assertTrue(validRes is DomainResult.Success)

        val shortReasonRes = BusinessCostValidators.validateReclassification(
            existingTracking = tracking,
            newCostCenter = activeCostCenter,
            newCostCategory = activeCostCategory,
            reason = "no",
            actorId = "USER-ADMIN"
        )
        assertTrue(shortReasonRes is DomainResult.Error)
        assertTrue((shortReasonRes as DomainResult.Error).message.contains("at least 3 characters"))
    }
}
