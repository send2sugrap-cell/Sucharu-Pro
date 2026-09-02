package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businesscostcontrol.BusinessCostControlValidators
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BusinessCostAccrualDomainTest {

    private val openPeriod = BusinessFinancialPeriod(
        id = "PER-2026-08",
        tenantId = "TENANT-001",
        projectId = "PRJ-001",
        periodCode = "2026-08",
        periodName = "August 2026",
        startDate = 1754092800000L,
        endDate = 1756684799000L,
        status = BusinessFinancialPeriodStatus.OPEN,
        createdBy = "USR-01"
    )

    private val closedPeriod = openPeriod.copy(
        id = "PER-2026-07",
        periodCode = "2026-07",
        periodName = "July 2026",
        status = BusinessFinancialPeriodStatus.CLOSED
    )

    @Test
    fun testValidAccrualValidation() {
        val res = BusinessCostControlValidators.validateAccrual(
            accrualNumber = "ACR-2026-001",
            accrualAmount = BigDecimal("45000.0000"),
            currency = "BDT",
            accountingPeriod = openPeriod,
            costCategoryId = "CAT-LABOR",
            description = "Overtime printing labor accrual for August",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            createdBy = "USR-01"
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testAccrualInClosedPeriodFails() {
        val res = BusinessCostControlValidators.validateAccrual(
            accrualNumber = "ACR-2026-002",
            accrualAmount = BigDecimal("45000.0000"),
            currency = "BDT",
            accountingPeriod = closedPeriod,
            costCategoryId = "CAT-LABOR",
            description = "Backdated labor",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            createdBy = "USR-01"
        )
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("closed accounting period"))
    }

    @Test
    fun testNetAccrualCalculation() {
        val accrual = BusinessCostAccrual(
            id = "ACR-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            accrualNumber = "ACR-01",
            costCategoryId = "CAT-UTIL",
            description = "Electricity accrual",
            accrualAmount = BigDecimal("60000.0000"),
            reversedAmount = BigDecimal("20000.0000"),
            currency = "BDT",
            accountingPeriodId = "PER-2026-08",
            status = BusinessCostAccrualStatus.POSTED,
            sourceType = BusinessCostCommitmentSourceType.MANUAL,
            sourceId = "ACR-01",
            createdBy = "USR-01"
        )
        assertEquals(BigDecimal("40000.0000"), accrual.netAccrualAmount)
    }
}
