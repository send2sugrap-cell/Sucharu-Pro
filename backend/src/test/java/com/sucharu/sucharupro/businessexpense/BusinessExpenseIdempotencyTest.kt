package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseFilter
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseServiceImpl
import com.sucharu.sucharupro.domain.service.businessexpense.CreateBusinessExpenseCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessExpenseIdempotencyTest {

    private lateinit var dataSource: FakeBusinessExpenseDataSource
    private lateinit var repository: BusinessExpenseRepositoryImpl
    private lateinit var service: BusinessExpenseServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "USER-STAFF-1",
        projectId = projectId,
        username = "staff1",
        role = UserRole.STAFF
    )

    @Before
    fun setup() {
        dataSource = FakeBusinessExpenseDataSource()
        repository = BusinessExpenseRepositoryImpl(dataSource)
        service = BusinessExpenseServiceImpl(repository, tenantId)
    }

    @Test
    fun testIdempotentExpenseCreationReturnsExistingRecord() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-TRN"
        val idempotencyKey = "IDEM-EXP-KEY-999"

        val cmd = CreateBusinessExpenseCommand(
            categoryId = catId,
            amount = BigDecimal("150.00"),
            description = "Toll Fare",
            idempotencyKey = idempotencyKey
        )

        // First Request
        val firstRes = service.createExpense(staffPrincipal, cmd)
        assertTrue(firstRes is DomainResult.Success)
        val firstExp = (firstRes as DomainResult.Success).data

        // Second Request with identical key
        val secondRes = service.createExpense(staffPrincipal, cmd)
        assertTrue(secondRes is DomainResult.Success)
        val secondExp = (secondRes as DomainResult.Success).data

        // Must return identical expense ID and number
        assertEquals(firstExp.expenseId, secondExp.expenseId)
        assertEquals(firstExp.expenseNumber, secondExp.expenseNumber)

        // Total count in database must be exactly 1
        val listRes = service.listExpenses(staffPrincipal, BusinessExpenseFilter())
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)
    }
}
