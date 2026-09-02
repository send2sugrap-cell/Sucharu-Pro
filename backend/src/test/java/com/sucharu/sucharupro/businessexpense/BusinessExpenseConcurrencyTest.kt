package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseServiceImpl
import com.sucharu.sucharupro.domain.service.businessexpense.CreateBusinessExpenseCommand
import com.sucharu.sucharupro.domain.service.businessexpense.UpdateBusinessExpenseCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

class BusinessExpenseConcurrencyTest {

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

    private val managerPrincipal1 = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "manager1",
        role = UserRole.MANAGER
    )

    private val managerPrincipal2 = AuthenticatedPrincipal(
        userId = "USER-MGR-2",
        projectId = projectId,
        username = "manager2",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        dataSource = FakeBusinessExpenseDataSource()
        repository = BusinessExpenseRepositoryImpl(dataSource)
        service = BusinessExpenseServiceImpl(repository, tenantId)
    }

    @Test
    fun testConcurrentApprovalAndCancellation() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-OFC"
        val createRes = service.createExpense(
            staffPrincipal,
            CreateBusinessExpenseCommand(
                categoryId = catId,
                amount = BigDecimal("1500.00"),
                description = "Office Hardware",
                autoSubmit = true
            )
        )
        val expense = (createRes as DomainResult.Success).data

        // Launch concurrent approval and cancellation
        val approveDeferred = async(Dispatchers.IO) {
            service.approveExpense(managerPrincipal1, expense.expenseId, "Approved concurrently")
        }
        val cancelDeferred = async(Dispatchers.IO) {
            service.cancelExpense(managerPrincipal2, expense.expenseId, "Cancelled concurrently")
        }

        val results = awaitAll(approveDeferred, cancelDeferred)

        // Exactly one transition must succeed and the other might succeed or fail depending on ordering,
        // but the final state must be a valid deterministic status (APPROVED or CANCELLED)
        val finalExpRes = service.getExpenseById(managerPrincipal1, expense.expenseId)
        assertTrue(finalExpRes is DomainResult.Success)
        val finalStatus = (finalExpRes as DomainResult.Success).data.status
        assertTrue(finalStatus in setOf(BusinessExpenseStatus.APPROVED, BusinessExpenseStatus.CANCELLED))
    }

    @Test
    fun testConcurrentExpenseCreation() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-OFC"
        val count = 20
        val successCount = AtomicInteger(0)

        val deferreds = (1..count).map { idx ->
            async(Dispatchers.IO) {
                val res = service.createExpense(
                    staffPrincipal,
                    CreateBusinessExpenseCommand(
                        categoryId = catId,
                        amount = BigDecimal("${idx * 10}.00"),
                        description = "Concurrent Expense #$idx"
                    )
                )
                if (res is DomainResult.Success) {
                    successCount.incrementAndGet()
                }
            }
        }

        deferreds.awaitAll()
        assertEquals(count, successCount.get())

        val listRes = service.listExpenses(staffPrincipal, limit = 50)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(count, (listRes as DomainResult.Success).data.size)
    }
}
