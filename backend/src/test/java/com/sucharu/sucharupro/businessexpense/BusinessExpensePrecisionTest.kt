package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseServiceImpl
import com.sucharu.sucharupro.domain.service.businessexpense.CreateBusinessExpenseCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessExpensePrecisionTest {

    private lateinit var dataSource: FakeBusinessExpenseDataSource
    private lateinit var repository: BusinessExpenseRepositoryImpl
    private lateinit var service: BusinessExpenseServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USER-ADMIN",
        projectId = projectId,
        username = "admin",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() {
        dataSource = FakeBusinessExpenseDataSource()
        repository = BusinessExpenseRepositoryImpl(dataSource)
        service = BusinessExpenseServiceImpl(repository, tenantId)
    }

    @Test
    fun testFourDecimalPrecisionPreservedExactly() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-PRN"
        val exactAmount = BigDecimal("123456.7891")

        val createRes = service.createExpense(
            adminPrincipal,
            CreateBusinessExpenseCommand(
                categoryId = catId,
                amount = exactAmount,
                currency = "BDT",
                description = "Bulk Printing Paper Import"
            )
        )
        assertTrue(createRes is DomainResult.Success)
        val expense = (createRes as DomainResult.Success).data

        assertEquals(exactAmount, expense.amount)
        assertEquals("123456.7891", expense.amount.toPlainString())

        // Verify retrieval preserves exact BigDecimal scale
        val getRes = service.getExpenseById(adminPrincipal, expense.expenseId)
        assertTrue(getRes is DomainResult.Success)
        val retrieved = (getRes as DomainResult.Success).data
        assertEquals(exactAmount, retrieved.amount)
    }

    @Test
    fun testLargeFinancialAmountSupported() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-MNT"
        val largeAmount = BigDecimal("98765432101234.5678")

        val createRes = service.createExpense(
            adminPrincipal,
            CreateBusinessExpenseCommand(
                categoryId = catId,
                amount = largeAmount,
                currency = "BDT",
                description = "Offset Printing Press Heavy Maintenance Overhaul"
            )
        )
        assertTrue(createRes is DomainResult.Success)
        val expense = (createRes as DomainResult.Success).data
        assertEquals(largeAmount, expense.amount)
    }
}
