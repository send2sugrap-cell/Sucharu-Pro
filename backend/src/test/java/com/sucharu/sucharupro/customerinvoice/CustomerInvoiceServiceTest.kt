package com.sucharu.sucharupro.customerinvoice

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customerinvoice.CustomerInvoiceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 02: Customer Invoice Service Lifecycle & Receivable Establishment Tests.
 */
class CustomerInvoiceServiceTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var service: CustomerInvoiceServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-INV-001"
    private val accountId = "CFA-INV-001"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)

        service = CustomerInvoiceServiceImpl(invoiceRepo, customerRepo, accountRepo)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-INV-01",
                    displayName = "Printing Client",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000001",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountId,
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    accountNumber = "ACC-INV-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testCompleteInvoiceLifecycleAndReceivable() = runBlocking {
        val line1 = CustomerInvoiceLine(
            lineId = "", invoiceId = "", tenantId = "", projectId = "",
            description = "Letterhead Offset Printing", quantity = BigDecimal("1000"), unitPrice = BigDecimal("2.50")
        )

        // 1. Create Draft Invoice
        val draftRes = service.createDraftInvoice(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            sourceOrderId = "ORD-101",
            sourceJobId = "JOB-202",
            dueDate = System.currentTimeMillis() + 86400000L,
            currency = "BDT",
            lines = listOf(line1),
            discount = BigDecimal("100.00"),
            tax = BigDecimal("50.00"),
            notes = "Urgent delivery required",
            actorId = "sales_exec",
            actorRole = "STAFF"
        )
        assertTrue(draftRes is DomainResult.Success)
        val draft = (draftRes as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.DRAFT, draft.status)
        // 1000 * 2.50 = 2500 - 100 = 2400 + 50 = 2450
        assertEquals(0, BigDecimal("2450.0000").compareTo(draft.grandTotal))
        assertEquals(0, BigDecimal("2450.0000").compareTo(draft.dueAmount))
        assertEquals(1L, draft.version)

        // 2. Update Draft Invoice (Add another line)
        val line2 = CustomerInvoiceLine(
            lineId = "", invoiceId = "", tenantId = "", projectId = "",
            description = "Envelopes", quantity = BigDecimal("500"), unitPrice = BigDecimal("1.00")
        )
        val updateRes = service.updateDraftInvoice(
            tenantId = tenantId,
            projectId = projectId,
            invoiceId = draft.invoiceId,
            lines = listOf(line1, line2),
            discount = BigDecimal("100.00"),
            tax = BigDecimal("50.00"),
            adjustment = BigDecimal.ZERO,
            notes = "Updated with envelopes",
            actorId = "sales_exec",
            actorRole = "STAFF",
            expectedVersion = 1L
        )
        assertTrue(updateRes is DomainResult.Success)
        val updatedDraft = (updateRes as DomainResult.Success).data
        // (2500 + 500) = 3000 - 100 = 2900 + 50 = 2950
        assertEquals(0, BigDecimal("2950.0000").compareTo(updatedDraft.grandTotal))
        assertEquals(2, updatedDraft.lines.size)
        assertEquals(2L, updatedDraft.version)

        // 3. Issue Invoice -> Establishes authoritative receivable
        val issueRes = service.issueInvoice(
            tenantId = tenantId,
            projectId = projectId,
            invoiceId = draft.invoiceId,
            actorId = "billing_lead",
            actorRole = "MANAGER",
            expectedVersion = 2L
        )
        assertTrue(issueRes is DomainResult.Success)
        val issued = (issueRes as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.ISSUED, issued.status)
        assertNotNull(issued.issueDate)
        assertEquals(3L, issued.version)

        // Verify audit history count
        val audits = (service.getAuditHistory(tenantId, projectId, draft.invoiceId) as DomainResult.Success).data
        assertEquals(3, audits.size) // Created -> Updated -> Issued
    }

    @Test
    fun testCannotEditIssuedInvoice() = runBlocking {
        val line = CustomerInvoiceLine(
            lineId = "", invoiceId = "", tenantId = "", projectId = "",
            description = "Digital Printing", quantity = BigDecimal("10"), unitPrice = BigDecimal("50")
        )
        val draft = (service.createDraftInvoice(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            currency = "BDT",
            lines = listOf(line),
            actorId = "sales_exec",
            actorRole = "STAFF"
        ) as DomainResult.Success).data

        // Issue it
        val issued = (service.issueInvoice(
            tenantId = tenantId,
            projectId = projectId,
            invoiceId = draft.invoiceId,
            actorId = "billing_lead",
            actorRole = "MANAGER",
            expectedVersion = 1L
        ) as DomainResult.Success).data

        // Attempting to update lines on an ISSUED invoice must be rejected
        val editRes = service.updateDraftInvoice(
            tenantId = tenantId,
            projectId = projectId,
            invoiceId = issued.invoiceId,
            lines = listOf(line),
            actorId = "sales_exec",
            actorRole = "STAFF",
            expectedVersion = 2L
        )
        assertTrue("Editing issued invoice must fail", editRes is DomainResult.Error)
    }

    @Test
    fun testCancelInvoiceRequiresReason() = runBlocking {
        val line = CustomerInvoiceLine(
            lineId = "", invoiceId = "", tenantId = "", projectId = "",
            description = "Flyer", quantity = BigDecimal("10"), unitPrice = BigDecimal("10")
        )
        val draft = (service.createDraftInvoice(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            currency = "BDT",
            lines = listOf(line),
            actorId = "sales_exec",
            actorRole = "STAFF"
        ) as DomainResult.Success).data

        val cancelNoReason = service.cancelInvoice(
            tenantId = tenantId,
            projectId = projectId,
            invoiceId = draft.invoiceId,
            reason = "",
            actorId = "admin",
            actorRole = "ADMIN",
            expectedVersion = 1L
        )
        assertTrue("Cancellation without reason must fail", cancelNoReason is DomainResult.Error)

        val cancelSuccess = service.cancelInvoice(
            tenantId = tenantId,
            projectId = projectId,
            invoiceId = draft.invoiceId,
            reason = "Customer cancelled order",
            actorId = "admin",
            actorRole = "ADMIN",
            expectedVersion = 1L
        )
        assertTrue(cancelSuccess is DomainResult.Success)
        val cancelled = (cancelSuccess as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.CANCELLED, cancelled.status)
        assertEquals("Customer cancelled order", cancelled.cancellationReason)
    }
}
