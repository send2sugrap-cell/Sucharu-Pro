package com.sucharu.sucharupro.businesscost

import com.sucharu.sucharupro.domain.model.businesscost.BusinessCostCenter
import com.sucharu.sucharupro.domain.model.businesscost.StandardCostCenterType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businesscost.BusinessCostValidators
import org.junit.Assert.*
import org.junit.Test

class BusinessCostCenterDomainTest {

    @Test
    fun testValidCostCenterCreation() {
        val center = BusinessCostCenter(
            id = "CC-PRINT",
            code = "CC-PRINT",
            name = "Offset & Digital Printing",
            description = "Main printing press room",
            tenantId = "TENANT-001",
            projectId = "PRJ-001"
        )
        val res = BusinessCostValidators.validateCostCenter(
            code = center.code,
            name = center.name,
            tenantId = center.tenantId,
            projectId = center.projectId
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testBlankCodeValidationFails() {
        val res = BusinessCostValidators.validateCostCenter(
            code = "",
            name = "Offset Printing",
            tenantId = "TENANT-001",
            projectId = "PRJ-001"
        )
        assertTrue(res is DomainResult.Error)
        assertEquals("Cost center code must be at least 2 characters.", (res as DomainResult.Error).message)
    }

    @Test
    fun testBlankNameValidationFails() {
        val res = BusinessCostValidators.validateCostCenter(
            code = "CC-PRINT",
            name = " ",
            tenantId = "TENANT-001",
            projectId = "PRJ-001"
        )
        assertTrue(res is DomainResult.Error)
        assertEquals("Cost center name must be at least 2 characters.", (res as DomainResult.Error).message)
    }

    @Test
    fun testSelfParentingValidationFails() {
        val res = BusinessCostValidators.validateCostCenter(
            code = "CC-PRINT",
            name = "Offset Printing",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            costCenterId = "CC-PRINT-01",
            parentCostCenterId = "CC-PRINT-01"
        )
        assertTrue(res is DomainResult.Error)
        assertEquals("A cost center cannot be its own parent.", (res as DomainResult.Error).message)
    }

    @Test
    fun testStandardCostCenterEnumCoverage() {
        val enums = StandardCostCenterType.values()
        assertTrue(enums.isNotEmpty())
        assertTrue(enums.any { it.name == "PRINTING" })
        assertTrue(enums.any { it.name == "DESIGN" })
        assertTrue(enums.any { it.name == "DELIVERY" })
        assertTrue(enums.any { it.name == "ADMINISTRATION" })
    }
}
