package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseAuditEvent
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessExpenseRepositoryTest {

    private lateinit var dataSource: FakeBusinessExpenseDataSource
    private lateinit var repository: BusinessExpenseRepositoryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    @Before
    fun setup() {
        dataSource = FakeBusinessExpenseDataSource()
        repository = BusinessExpenseRepositoryImpl(dataSource)
    }

    @Test
    fun testCreateAndRetrieveExpense() = runBlocking {
        val expense = BusinessExpense(
            expenseId = "EXP-REPO-1",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-20261015-0001",
            expenseCategoryId = "CAT-OFC",
            amount = BigDecimal("2500.50"),
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.CASH,
            status = BusinessExpenseStatus.DRAFT,
            description = "Stationery and printing papers",
            createdBy = "USER-01"
        )

        val createRes = repository.createExpense(expense)
        assertTrue(createRes is DomainResult.Success)

        val getRes = repository.getExpenseById(tenantId, projectId, "EXP-REPO-1")
        assertTrue(getRes is DomainResult.Success)
        val retrieved = (getRes as DomainResult.Success).data
        assertNotNull(retrieved)
        assertEquals("EXP-20261015-0001", retrieved!!.expenseNumber)
        assertEquals(BigDecimal("2500.50"), retrieved.amount)
        assertEquals(BusinessExpenseStatus.DRAFT, retrieved.status)
    }

    @Test
    fun testUpdateExpense() = runBlocking {
        val expense = BusinessExpense(
            expenseId = "EXP-REPO-2",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-20261015-0002",
            expenseCategoryId = "CAT-OFC",
            amount = BigDecimal("1000.00"),
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.CASH,
            status = BusinessExpenseStatus.DRAFT,
            description = "Initial description",
            createdBy = "USER-01"
        )
        repository.createExpense(expense)

        val updatedExpense = expense.copy(
            amount = BigDecimal("1200.00"),
            description = "Updated description"
        )
        val updateRes = repository.updateExpense(updatedExpense)
        assertTrue(updateRes is DomainResult.Success)

        val retrieved = (repository.getExpenseById(tenantId, projectId, "EXP-REPO-2") as DomainResult.Success).data
        assertNotNull(retrieved)
        assertEquals(BigDecimal("1200.00"), retrieved!!.amount)
        assertEquals("Updated description", retrieved.description)
    }

    @Test
    fun testListAndCountWithFilters() = runBlocking {
        for (i in 1..5) {
            val status = if (i % 2 == 0) BusinessExpenseStatus.SUBMITTED else BusinessExpenseStatus.DRAFT
            val category = if (i <= 3) "CAT-TRN" else "CAT-UTL"
            repository.createExpense(
                BusinessExpense(
                    expenseId = "EXP-FILTER-$i",
                    tenantId = tenantId,
                    projectId = projectId,
                    expenseNumber = "EXP-20261015-000$i",
                    expenseCategoryId = category,
                    amount = BigDecimal("${i * 100}.00"),
                    currency = "BDT",
                    expenseDate = System.currentTimeMillis() - (i * 1000L),
                    paymentMethod = BusinessExpensePaymentMethod.CASH,
                    status = status,
                    description = "Expense #$i",
                    createdBy = "USER-01"
                )
            )
        }

        val allRes = repository.listExpenses(tenantId, projectId, limit = 10, offset = 0)
        assertTrue(allRes is DomainResult.Success)
        assertEquals(5, (allRes as DomainResult.Success).data.size)

        val submittedRes = repository.listExpenses(tenantId, projectId, status = BusinessExpenseStatus.SUBMITTED)
        assertEquals(2, (submittedRes as DomainResult.Success).data.size)

        val transportRes = repository.listExpenses(tenantId, projectId, categoryId = "CAT-TRN")
        assertEquals(3, (transportRes as DomainResult.Success).data.size)

        val countRes = repository.countExpenses(tenantId, projectId, categoryId = "CAT-TRN")
        assertEquals(3L, (countRes as DomainResult.Success).data)
    }

    @Test
    fun testAuditTrailPersistence() = runBlocking {
        val audit1 = BusinessExpenseAuditEvent(
            eventId = "EVT-01",
            tenantId = tenantId,
            projectId = projectId,
            expenseId = "EXP-AUD-1",
            eventType = "CREATED",
            actorId = "USER-01",
            actorRole = "STAFF",
            timestamp = 1000L,
            newStatus = BusinessExpenseStatus.DRAFT
        )
        val audit2 = BusinessExpenseAuditEvent(
            eventId = "EVT-02",
            tenantId = tenantId,
            projectId = projectId,
            expenseId = "EXP-AUD-1",
            eventType = "SUBMITTED",
            actorId = "USER-01",
            actorRole = "STAFF",
            timestamp = 2000L,
            previousStatus = BusinessExpenseStatus.DRAFT,
            newStatus = BusinessExpenseStatus.SUBMITTED
        )

        repository.recordAuditEvent(audit1)
        repository.recordAuditEvent(audit2)

        val auditsRes = repository.getAuditEvents(tenantId, projectId, "EXP-AUD-1")
        assertTrue(auditsRes is DomainResult.Success)
        val audits = (auditsRes as DomainResult.Success).data
        assertEquals(2, audits.size)
        assertEquals("CREATED", audits[0].eventType)
        assertEquals("SUBMITTED", audits[1].eventType)
    }

    @Test
    fun testCategoryCreationAndDuplicateRejection() = runBlocking {
        val category = BusinessExpenseCategory(
            categoryId = "CAT-CUSTOM-1",
            tenantId = tenantId,
            projectId = projectId,
            name = "Courier Logistics",
            code = "CAT-LOG",
            description = "Logistics and postage",
            isActive = true,
            sortOrder = 15
        )

        val createRes = repository.createCategory(category)
        assertTrue(createRes is DomainResult.Success)

        // Duplicate code rejection
        val duplicateRes = repository.createCategory(category.copy(categoryId = "CAT-CUSTOM-2"))
        assertTrue(duplicateRes is DomainResult.Error)
    }
}
