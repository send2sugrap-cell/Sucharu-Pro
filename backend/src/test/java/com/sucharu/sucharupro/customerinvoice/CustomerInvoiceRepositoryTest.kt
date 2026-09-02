package com.sucharu.sucharupro.customerinvoice

import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 02: Repository & Optimistic Locking Test for Invoices.
 */
class CustomerInvoiceRepositoryTest {

    private lateinit var dataSource: FakeCustomerInvoiceDataSource
    private lateinit var repository: CustomerInvoiceRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-001"
    private val accountId = "CFA-001"

    @Before
    fun setup() {
        dataSource = FakeCustomerInvoiceDataSource()
        repository = CustomerInvoiceRepositoryImpl(dataSource)
    }

    @Test
    fun testCreateAndRetrieveInvoiceWithLines() = runBlocking {
        val line1 = CustomerInvoiceLine(
            lineId = "L1", invoiceId = "INV-001", tenantId = tenantId, projectId = projectId,
            description = "Custom Packaging Box", quantity = BigDecimal("100"), unitPrice = BigDecimal("25.00"),
            lineTotal = BigDecimal("2500.00")
        )
        val invoice = CustomerInvoice(
            invoiceId = "INV-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceNumber = "INV-2026-0001",
            subtotal = BigDecimal("2500.00"),
            grandTotal = BigDecimal("2500.00"),
            dueAmount = BigDecimal("2500.00"),
            lines = listOf(line1)
        )

        val createRes = repository.createInvoice(invoice)
        assertTrue(createRes is DomainResult.Success)

        val getRes = repository.getInvoiceById(tenantId, projectId, "INV-001")
        assertTrue(getRes is DomainResult.Success)
        val retrieved = (getRes as DomainResult.Success).data
        assertEquals("INV-2026-0001", retrieved.invoiceNumber)
        assertEquals(1, retrieved.lines.size)
        assertEquals("Custom Packaging Box", retrieved.lines[0].description)

        val getByNumRes = repository.getInvoiceByNumber(tenantId, "INV-2026-0001")
        assertTrue(getByNumRes is DomainResult.Success)
    }

    @Test
    fun testOptimisticLockingOnStatusUpdate() = runBlocking {
        val invoice = CustomerInvoice(
            invoiceId = "INV-002",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceNumber = "INV-2026-0002",
            version = 1L
        )
        repository.createInvoice(invoice)

        // Version mismatch
        val conflictRes = repository.updateInvoiceStatus(
            tenantId, projectId, "INV-002",
            CustomerInvoiceStatus.ISSUED, null, "admin", System.currentTimeMillis(),
            expectedVersion = 99L
        )
        assertTrue("Version conflict must be rejected", conflictRes is DomainResult.Error)

        // Correct version
        val successRes = repository.updateInvoiceStatus(
            tenantId, projectId, "INV-002",
            CustomerInvoiceStatus.ISSUED, null, "admin", System.currentTimeMillis(),
            expectedVersion = 1L
        )
        assertTrue(successRes is DomainResult.Success)
        val updated = (successRes as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.ISSUED, updated.status)
        assertEquals(2L, updated.version)
    }

    @Test
    fun testAuditEventLogging() = runBlocking {
        val event = CustomerInvoiceAuditEvent(
            auditId = "AUD-INV-1",
            invoiceId = "INV-001",
            customerId = customerId,
            tenantId = tenantId,
            projectId = projectId,
            actorId = "staff_1",
            actorRole = "STAFF",
            action = "INVOICE_ISSUED",
            newStatus = CustomerInvoiceStatus.ISSUED
        )
        repository.recordAuditEvent(event)

        val historyRes = repository.getAuditEvents(tenantId, projectId, "INV-001")
        assertTrue(historyRes is DomainResult.Success)
        val history = (historyRes as DomainResult.Success).data
        assertEquals(1, history.size)
        assertEquals("INVOICE_ISSUED", history[0].action)
    }
}
