package com.sucharu.sucharupro.domain.service.businessfinancialreporting

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialReportingSecurityTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var repoFactory: PostgresRepositoryFactory
    private lateinit var mockDb: MockPostgresEventDatabase

    private val projectA = "PROJ-001"
    private val projectB = "PROJ-002"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        projectId = projectA,
        username = "admin",
        role = UserRole.ADMIN,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr-1",
        projectId = projectA,
        username = "manager",
        role = UserRole.MANAGER,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff-1",
        projectId = projectA,
        username = "staff",
        role = UserRole.STAFF,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cust-1",
        projectId = projectA,
        username = "customer",
        role = UserRole.CUSTOMER,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vend-1",
        projectId = projectA,
        username = "vendor",
        role = UserRole.VENDOR,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    @Before
    fun setup() {
        mockDb = MockPostgresEventDatabase()
        val fakeExpenseDsA = com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource()
        val fakeExpenseDsB = com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource()
        val fakeReportingDs = com.sucharu.sucharupro.data.datasource.businessfinancialreporting.FakeBusinessFinancialReportingDataSource()

        repoFactory = object : PostgresRepositoryFactory(mockDb, defaultTenantId = projectA) {
            override fun createBusinessExpenseRepository(
                tenantId: String
            ): com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository {
                val ds = if (tenantId == projectB) fakeExpenseDsB else fakeExpenseDsA
                return com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl(ds)
            }

            override fun createBusinessFinancialReportingRepository(
                tenantId: String
            ): com.sucharu.sucharupro.domain.repository.businessfinancialreporting.BusinessFinancialReportingRepository {
                return com.sucharu.sucharupro.data.repository.businessfinancialreporting.BusinessFinancialReportingRepositoryImpl(fakeReportingDs)
            }
        }
        useCases = BackendUseCases(mockDb, repoFactory)
    }

    @Test
    fun testAdminManagerStaffAuthorizedForReports() = runBlocking {
        val adminSummary = useCases.getExecutiveFinancialSummary(adminPrincipal)
        assertNotNull(adminSummary)

        val managerSummary = useCases.getExecutiveFinancialSummary(managerPrincipal)
        assertNotNull(managerSummary)

        val staffSummary = useCases.getExecutiveFinancialSummary(staffPrincipal)
        assertNotNull(staffSummary)
    }

    @Test
    fun testCustomerAndVendorForbiddenFromReports() = runBlocking {
        try {
            useCases.getExecutiveFinancialSummary(customerPrincipal)
            fail("Customer should be denied access to private financial reports")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Required role") == true || e.message?.contains("Access denied") == true)
        }

        try {
            useCases.getExecutiveFinancialSummary(vendorPrincipal)
            fail("Vendor should be denied access to private financial reports")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Required role") == true || e.message?.contains("Access denied") == true)
        }
    }

    @Test
    fun testTenantIsolationEnforced() = runBlocking {
        val expenseRepoA = repoFactory.createBusinessExpenseRepository(projectA)

        // Seed expense only in Project A
        expenseRepoA.createExpense(
            BusinessExpense(
                expenseId = "EXP-A-1",
                tenantId = projectA,
                projectId = projectA,
                expenseNumber = "EXP-A-001",
                description = "Tenant A Expense",
                expenseCategoryId = "CAT-GEN",
                amount = BigDecimal("9999.0000"),
                currency = "BDT",
                expenseDate = System.currentTimeMillis(),
                status = BusinessExpenseStatus.APPROVED,
                paymentMethod = BusinessExpensePaymentMethod.BANK,
                createdBy = "admin-1"
            )
        )

        val tenantBPrincipal = AuthenticatedPrincipal(
            userId = "admin-2",
            projectId = projectB,
            username = "admin2",
            role = UserRole.ADMIN,
            principalType = PrincipalType.HUMAN,
            permissions = emptySet()
        )

        val reportB = useCases.getExecutiveFinancialSummary(tenantBPrincipal)
        assertEquals(BigDecimal.ZERO.setScale(4), reportB.totalExpenseAmount.setScale(4))
        assertEquals(0, reportB.expenseCount)
    }

    @Test
    fun testReadOnlyInvarianceAgainstCanonicalTransactions() = runBlocking {
        val expenseRepo = repoFactory.createBusinessExpenseRepository(projectA)

        val expense = BusinessExpense(
            expenseId = "EXP-IMMUTABLE-1",
            tenantId = projectA,
            projectId = projectA,
            expenseNumber = "EXP-IMM-001",
            description = "Immutable Expense",
            expenseCategoryId = "CAT-GEN",
            amount = BigDecimal("2500.0000"),
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            status = BusinessExpenseStatus.APPROVED,
            paymentMethod = BusinessExpensePaymentMethod.BANK,
            createdBy = "admin-1"
        )
        expenseRepo.createExpense(expense)

        // Generate reports repeatedly
        useCases.getExecutiveFinancialSummary(adminPrincipal)
        useCases.getBusinessExpenseAnalytics(adminPrincipal)
        useCases.getBusinessLedgerReport(adminPrincipal)

        // Verify that canonical expense was NOT mutated
        val retrieved = expenseRepo.getExpenseById(projectA, projectA, "EXP-IMMUTABLE-1")
        assertTrue(retrieved is DomainResult.Success)
        val expData = (retrieved as DomainResult.Success).data
        assertNotNull(expData)
        assertEquals(BusinessExpenseStatus.APPROVED, expData!!.status)
        assertEquals(BigDecimal("2500.0000"), expData.amount)
    }
}
