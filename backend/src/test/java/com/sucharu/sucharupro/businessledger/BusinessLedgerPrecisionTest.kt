package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.businessledger.PostBusinessAdjustmentCommand
import com.sucharu.sucharupro.domain.validation.businessledger.BusinessLedgerValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class BusinessLedgerPrecisionTest {

    private lateinit var dataSource: FakeBusinessLedgerDataSource
    private lateinit var repository: BusinessLedgerRepositoryImpl
    private lateinit var service: BusinessLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "manager1",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        dataSource = FakeBusinessLedgerDataSource()
        repository = BusinessLedgerRepositoryImpl(dataSource)
        service = BusinessLedgerServiceImpl(
            repository = repository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testExact4DecimalPrecisionPreserved() = runBlocking {
        val amount = BigDecimal("123456.7891")
        val postRes = service.postBusinessAdjustment(
            managerPrincipal,
            PostBusinessAdjustmentCommand(
                amount = amount,
                isDebit = true,
                accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
                description = "High precision testing"
            )
        )
        assertTrue(postRes is DomainResult.Success)
        val posting = (postRes as DomainResult.Success).data
        assertEquals(amount, posting.debitAmount)

        val balRes = service.getBalanceSummary(managerPrincipal)
        assertEquals(amount, (balRes as DomainResult.Success).data.totalDebit)
    }

    @Test
    fun testNoFloatingPointDriftOverManyPostings() = runBlocking {
        // Add 100 entries of 0.0001
        val itemAmount = BigDecimal("0.0001")
        for (i in 1..100) {
            service.postBusinessAdjustment(
                managerPrincipal,
                PostBusinessAdjustmentCommand(
                    amount = itemAmount,
                    isDebit = true,
                    accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
                    description = "Micro-charge #$i",
                    idempotencyKey = "KEY-PREC-$i"
                )
            )
        }

        val balRes = service.getBalanceSummary(managerPrincipal)
        val expected = BigDecimal("0.0100").setScale(4, RoundingMode.HALF_UP)
        assertEquals(expected, (balRes as DomainResult.Success).data.totalDebit)
    }

    @Test
    fun testScaleGreaterThan4Rejected() {
        val invalid = BigDecimal("100.12345")
        val res = BusinessLedgerValidator.validatePrecision(invalid)
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("precision cannot exceed 4"))
    }
}
