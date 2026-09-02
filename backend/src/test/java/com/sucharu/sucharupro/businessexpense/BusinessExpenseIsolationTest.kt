package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseFilter
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseServiceImpl
import com.sucharu.sucharupro.domain.service.businessexpense.CreateBusinessExpenseCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessExpenseIsolationTest {

    private lateinit var dataSource: FakeBusinessExpenseDataSource
    private lateinit var repository: BusinessExpenseRepositoryImpl

    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val projectA = "PRJ-A"
    private val projectB = "PRJ-B"

    private lateinit var serviceA: BusinessExpenseServiceImpl
    private lateinit var serviceB: BusinessExpenseServiceImpl

    private val principalA = AuthenticatedPrincipal(
        userId = "USER-A",
        projectId = projectA,
        username = "usera",
        role = UserRole.ADMIN
    )

    private val principalB = AuthenticatedPrincipal(
        userId = "USER-B",
        projectId = projectB,
        username = "userb",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() {
        dataSource = FakeBusinessExpenseDataSource()
        repository = BusinessExpenseRepositoryImpl(dataSource)
        serviceA = BusinessExpenseServiceImpl(repository, tenantA)
        serviceB = BusinessExpenseServiceImpl(repository, tenantB)

        dataSource.seedDefaultCategories(tenantA, projectA)
        dataSource.seedDefaultCategories(tenantB, projectB)
    }

    @Test
    fun testTenantAndProjectIsolation() = runBlocking {
        // Tenant A creates an expense
        val catA = "CAT-$tenantA-$projectA-CAT-OFC"
        val expARes = serviceA.createExpense(
            principalA,
            CreateBusinessExpenseCommand(
                categoryId = catA,
                amount = BigDecimal("1000.00"),
                description = "Tenant A Office Supplies"
            )
        )
        assertTrue(expARes is DomainResult.Success)
        val expA = (expARes as DomainResult.Success).data

        // Tenant B creates an expense
        val catB = "CAT-$tenantB-$projectB-CAT-OFC"
        val expBRes = serviceB.createExpense(
            principalB,
            CreateBusinessExpenseCommand(
                categoryId = catB,
                amount = BigDecimal("2000.00"),
                description = "Tenant B Office Supplies"
            )
        )
        assertTrue(expBRes is DomainResult.Success)
        val expB = (expBRes as DomainResult.Success).data

        // 1. Tenant B cannot get Tenant A's expense by ID
        val crossGetRes = serviceB.getExpenseById(principalB, expA.expenseId)
        assertTrue(crossGetRes is DomainResult.Error)

        // 2. Tenant A list only contains Tenant A expenses
        val listARes = serviceA.listExpenses(principalA, BusinessExpenseFilter())
        assertTrue(listARes is DomainResult.Success)
        val listA = (listARes as DomainResult.Success).data
        assertEquals(1, listA.size)
        assertEquals("Tenant A Office Supplies", listA[0].description)

        // 3. Tenant B list only contains Tenant B expenses
        val listBRes = serviceB.listExpenses(principalB, BusinessExpenseFilter())
        assertTrue(listBRes is DomainResult.Success)
        val listB = (listBRes as DomainResult.Success).data
        assertEquals(1, listB.size)
        assertEquals("Tenant B Office Supplies", listB[0].description)

        // 4. Cross-tenant category lookup fails
        val crossCatCheck = serviceA.createExpense(
            principalA,
            CreateBusinessExpenseCommand(
                categoryId = catB, // Category from Tenant B
                amount = BigDecimal("500.00"),
                description = "Invalid cross-category expense"
            )
        )
        assertTrue(crossCatCheck is DomainResult.Error)
    }
}
