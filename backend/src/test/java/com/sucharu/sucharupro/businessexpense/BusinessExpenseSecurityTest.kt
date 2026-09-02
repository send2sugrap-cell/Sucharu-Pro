package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseServiceImpl
import com.sucharu.sucharupro.domain.service.businessexpense.CreateBusinessExpenseCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessExpenseSecurityTest {

    private lateinit var dataSource: FakeBusinessExpenseDataSource
    private lateinit var repository: BusinessExpenseRepositoryImpl
    private lateinit var service: BusinessExpenseServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "USER-CUS-1",
        projectId = projectId,
        username = "customer1",
        role = UserRole.CUSTOMER
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "USER-VEN-1",
        projectId = projectId,
        username = "vendor1",
        role = UserRole.VENDOR
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "USER-AFF-1",
        projectId = projectId,
        username = "affiliate1",
        role = UserRole.AFFILIATE
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "USER-STAFF-1",
        projectId = projectId,
        username = "staff1",
        role = UserRole.STAFF
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "manager1",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        dataSource = FakeBusinessExpenseDataSource()
        repository = BusinessExpenseRepositoryImpl(dataSource)
        service = BusinessExpenseServiceImpl(repository, tenantId)
    }

    @Test
    fun testExternalPrincipalsDeniedAccess() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-OFC"
        val cmd = CreateBusinessExpenseCommand(
            categoryId = catId,
            amount = BigDecimal("500.00"),
            description = "Unauthorized Attempt"
        )

        // Customer denied
        val customerRes = service.createExpense(customerPrincipal, cmd)
        assertTrue(customerRes is DomainResult.Error)

        // Vendor denied
        val vendorRes = service.createExpense(vendorPrincipal, cmd)
        assertTrue(vendorRes is DomainResult.Error)

        // Affiliate denied
        val affiliateRes = service.createExpense(affiliatePrincipal, cmd)
        assertTrue(affiliateRes is DomainResult.Error)
    }

    @Test
    fun testStaffCannotApproveOrRejectExpenses() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-OFC"
        val createRes = service.createExpense(
            staffPrincipal,
            CreateBusinessExpenseCommand(
                categoryId = catId,
                amount = BigDecimal("500.00"),
                description = "Office Supplies",
                autoSubmit = true
            )
        )
        val expense = (createRes as DomainResult.Success).data

        // Staff tries to approve
        val staffApproveRes = service.approveExpense(staffPrincipal, expense.expenseId)
        assertTrue(staffApproveRes is DomainResult.Error)

        // Staff tries to reject
        val staffRejectRes = service.rejectExpense(staffPrincipal, expense.expenseId, "Reason")
        assertTrue(staffRejectRes is DomainResult.Error)
    }

    @Test
    fun testSelfApprovalDeniedForManagers() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-OFC"
        // Manager creates expense
        val createRes = service.createExpense(
            managerPrincipal,
            CreateBusinessExpenseCommand(
                categoryId = catId,
                amount = BigDecimal("8000.00"),
                description = "Manager Travel Expense",
                autoSubmit = true
            )
        )
        val expense = (createRes as DomainResult.Success).data

        // Same manager tries to approve their own expense -> Denied
        val selfApproveRes = service.approveExpense(managerPrincipal, expense.expenseId)
        assertTrue(selfApproveRes is DomainResult.Error)
        assertTrue((selfApproveRes as DomainResult.Error).message.contains("Separation of duties"))
    }
}
