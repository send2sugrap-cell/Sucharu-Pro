package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseServiceImpl
import com.sucharu.sucharupro.domain.service.businessexpense.CreateBusinessExpenseCommand
import com.sucharu.sucharupro.domain.service.businessexpense.UpdateBusinessExpenseCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessExpenseServiceTest {

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

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "manager1",
        role = UserRole.MANAGER
    )

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USER-ADMIN-1",
        projectId = projectId,
        username = "admin1",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() {
        dataSource = FakeBusinessExpenseDataSource()
        repository = BusinessExpenseRepositoryImpl(dataSource)
        service = BusinessExpenseServiceImpl(repository, tenantId)
    }

    @Test
    fun testFullExpenseLifecycle_DraftSubmitApprove() = runBlocking {
        // 1. Create Draft
        val catId = "CAT-$tenantId-$projectId-CAT-OFC"
        val createCmd = CreateBusinessExpenseCommand(
            categoryId = catId,
            amount = BigDecimal("3500.00"),
            currency = "BDT",
            expenseDate = System.currentTimeMillis(),
            paymentMethod = BusinessExpensePaymentMethod.CASH,
            description = "Office Printer Cartridge",
            notes = "Purchased from local market"
        )
        val createRes = service.createExpense(staffPrincipal, createCmd)
        assertTrue(createRes is DomainResult.Success)
        val expense = (createRes as DomainResult.Success).data
        assertEquals(BusinessExpenseStatus.DRAFT, expense.status)
        assertEquals("USER-STAFF-1", expense.createdBy)

        // 2. Edit Draft
        val updateCmd = UpdateBusinessExpenseCommand(
            amount = BigDecimal("3700.00"),
            description = "Office Printer Cartridge & Cables"
        )
        val updateRes = service.updateExpenseDraft(staffPrincipal, expense.expenseId, updateCmd)
        assertTrue(updateRes is DomainResult.Success)
        val updated = (updateRes as DomainResult.Success).data
        assertEquals(BigDecimal("3700.00"), updated.amount)

        // 3. Submit
        val submitRes = service.submitExpense(staffPrincipal, expense.expenseId)
        assertTrue(submitRes is DomainResult.Success)
        val submitted = (submitRes as DomainResult.Success).data
        assertEquals(BusinessExpenseStatus.SUBMITTED, submitted.status)
        assertNotNull(submitted.submittedAt)

        // 4. Approve (by Manager, not Creator)
        val approveRes = service.approveExpense(managerPrincipal, expense.expenseId, "Approved for operational necessity")
        assertTrue(approveRes is DomainResult.Success)
        val approved = (approveRes as DomainResult.Success).data
        assertEquals(BusinessExpenseStatus.APPROVED, approved.status)
        assertEquals("USER-MGR-1", approved.approvedBy)
        assertNotNull(approved.approvedAt)

        // 5. Verify Audit Trail
        val auditsRes = service.getExpenseAuditTrail(managerPrincipal, expense.expenseId)
        assertTrue(auditsRes is DomainResult.Success)
        val audits = (auditsRes as DomainResult.Success).data
        assertEquals(4, audits.size) // CREATED, UPDATED, SUBMITTED, APPROVED
    }

    @Test
    fun testRejectAndResubmitWorkflow() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-TRN"
        val createRes = service.createExpense(
            staffPrincipal,
            CreateBusinessExpenseCommand(
                categoryId = catId,
                amount = BigDecimal("500.00"),
                currency = "BDT",
                expenseDate = System.currentTimeMillis(),
                paymentMethod = BusinessExpensePaymentMethod.CASH,
                description = "Taxi fare for client meeting",
                autoSubmit = true
            )
        )
        val expense = (createRes as DomainResult.Success).data
        assertEquals(BusinessExpenseStatus.SUBMITTED, expense.status)

        // Reject by Manager
        val rejectRes = service.rejectExpense(managerPrincipal, expense.expenseId, "Missing client meeting receipt")
        assertTrue(rejectRes is DomainResult.Success)
        val rejected = (rejectRes as DomainResult.Success).data
        assertEquals(BusinessExpenseStatus.REJECTED, rejected.status)
        assertEquals("Missing client meeting receipt", rejected.rejectionReason)

        // Staff edits and resubmits
        val editRes = service.updateExpenseDraft(
            staffPrincipal,
            expense.expenseId,
            UpdateBusinessExpenseCommand(notes = "Receipt attached via link")
        )
        assertTrue(editRes is DomainResult.Success)

        val resubmitRes = service.submitExpense(staffPrincipal, expense.expenseId)
        assertTrue(resubmitRes is DomainResult.Success)
        val resubmitted = (resubmitRes as DomainResult.Success).data
        assertEquals(BusinessExpenseStatus.SUBMITTED, resubmitted.status)
    }

    @Test
    fun testCancelExpenseWorkflow() = runBlocking {
        val catId = "CAT-$tenantId-$projectId-CAT-UTL"
        val createRes = service.createExpense(
            staffPrincipal,
            CreateBusinessExpenseCommand(
                categoryId = catId,
                amount = BigDecimal("1200.00"),
                currency = "BDT",
                expenseDate = System.currentTimeMillis(),
                paymentMethod = BusinessExpensePaymentMethod.CASH,
                description = "Electricity bill estimation"
            )
        )
        val expense = (createRes as DomainResult.Success).data

        // Cancel
        val cancelRes = service.cancelExpense(staffPrincipal, expense.expenseId, "Duplicate entry")
        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(BusinessExpenseStatus.CANCELLED, cancelled.status)

        // Attempting to edit or submit cancelled expense fails
        val editCancelled = service.updateExpenseDraft(staffPrincipal, expense.expenseId, UpdateBusinessExpenseCommand(amount = BigDecimal("100.00")))
        assertTrue(editCancelled is DomainResult.Error)

        val submitCancelled = service.submitExpense(staffPrincipal, expense.expenseId)
        assertTrue(submitCancelled is DomainResult.Error)
    }
}
