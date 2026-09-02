package com.sucharu.sucharupro.businesscost

import com.sucharu.sucharupro.domain.model.businesscost.BusinessCostCategory
import com.sucharu.sucharupro.domain.model.businesscost.StandardCostCategoryType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businesscost.BusinessCostValidators
import org.junit.Assert.*
import org.junit.Test

class BusinessCostCategoryDomainTest {

    @Test
    fun testValidCostCategoryCreation() {
        val cat = BusinessCostCategory(
            id = "CAT-PAPER",
            code = "CAT-PAPER",
            name = "Paper & Substrates",
            description = "Art paper, duplex board, offset stock",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            isSystemDefined = true
        )
        val res = BusinessCostValidators.validateCostCategory(
            code = cat.code,
            name = cat.name,
            tenantId = cat.tenantId,
            projectId = cat.projectId
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testBlankCategoryCodeFails() {
        val res = BusinessCostValidators.validateCostCategory(
            code = "A",
            name = "Paper",
            tenantId = "TENANT-001",
            projectId = "PRJ-001"
        )
        assertTrue(res is DomainResult.Error)
        assertEquals("Cost category code must be at least 2 characters.", (res as DomainResult.Error).message)
    }

    @Test
    fun testBlankCategoryNameFails() {
        val res = BusinessCostValidators.validateCostCategory(
            code = "CAT-PAPER",
            name = "",
            tenantId = "TENANT-001",
            projectId = "PRJ-001"
        )
        assertTrue(res is DomainResult.Error)
        assertEquals("Cost category name must be at least 2 characters.", (res as DomainResult.Error).message)
    }

    @Test
    fun testCategorySelfParentingFails() {
        val res = BusinessCostValidators.validateCostCategory(
            code = "CAT-PAPER",
            name = "Paper",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            categoryId = "CAT-01",
            parentCategoryId = "CAT-01"
        )
        assertTrue(res is DomainResult.Error)
        assertEquals("A cost category cannot be its own parent.", (res as DomainResult.Error).message)
    }

    @Test
    fun testStandardCostCategoryEnumTypes() {
        val types = StandardCostCategoryType.values()
        assertTrue(types.isNotEmpty())
        assertTrue(types.any { it.name == "PAPER" && it.isDirectProduction })
        assertTrue(types.any { it.name == "TRANSPORT" && !it.isDirectProduction })
        assertTrue(types.any { it.name == "OUTSOURCE" && it.isDirectProduction })
    }
}
