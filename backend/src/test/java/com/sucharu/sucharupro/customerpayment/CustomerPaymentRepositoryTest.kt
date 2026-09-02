package com.sucharu.sucharupro.customerpayment

import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentAuditEvent
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 03: Customer Payment Repository Tests.
 */
class CustomerPaymentRepositoryTest {

    private lateinit var dataSource: FakeCustomerPaymentDataSource
    private lateinit var repository: CustomerPaymentRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-001"
    private val accountId = "CFA-001"

    @Before
    fun setup() {
        dataSource = FakeCustomerPaymentDataSource()
        repository = CustomerPaymentRepositoryImpl(dataSource)
    }

    @Test
    fun testCreateAndRetrievePayment() = runBlocking {
        val payment = CustomerPayment(
            paymentId = "PAY-001",
            tenantId = tenantId,
            projectId = projectId,
            paymentNumber = "PAY-2026-0001",
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = "INV-001",
            amount = BigDecimal("1500.0000"),
            paymentMethod = CustomerPaymentMethod.BKASH,
            referenceNumber = "TRX-777888",
            idempotencyKey = "IDEMP-123"
        )

        val createRes = repository.createPayment(payment)
        assertTrue(createRes is DomainResult.Success)

        val getRes = repository.getPaymentById(tenantId, projectId, "PAY-001")
        assertTrue(getRes is DomainResult.Success)
        val retrieved = (getRes as DomainResult.Success).data
        assertEquals("PAY-2026-0001", retrieved.paymentNumber)
        assertEquals(CustomerPaymentMethod.BKASH, retrieved.paymentMethod)
        assertEquals("TRX-777888", retrieved.referenceNumber)

        val getByNumRes = repository.getPaymentByNumber(tenantId, "PAY-2026-0001")
        assertTrue(getByNumRes is DomainResult.Success)

        val getIdempRes = repository.findByIdempotencyKey(tenantId, projectId, "IDEMP-123")
        assertTrue(getIdempRes is DomainResult.Success)
        assertNotNull((getIdempRes as DomainResult.Success).data)
    }

    @Test
    fun testOptimisticLockingOnStatusUpdate() = runBlocking {
        val payment = CustomerPayment(
            paymentId = "PAY-002",
            tenantId = tenantId,
            projectId = projectId,
            paymentNumber = "PAY-2026-0002",
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("1000.00"),
            version = 1L
        )
        repository.createPayment(payment)

        // Conflict
        val conflictRes = repository.updatePaymentStatus(
            tenantId, projectId, "PAY-002",
            CustomerPaymentStatus.CONFIRMED, null, "admin", expectedVersion = 99L
        )
        assertTrue(conflictRes is DomainResult.Error)

        // Success
        val successRes = repository.updatePaymentStatus(
            tenantId, projectId, "PAY-002",
            CustomerPaymentStatus.CONFIRMED, null, "admin", expectedVersion = 1L
        )
        assertTrue(successRes is DomainResult.Success)
        val updated = (successRes as DomainResult.Success).data
        assertEquals(CustomerPaymentStatus.CONFIRMED, updated.status)
        assertEquals(2L, updated.version)
    }

    @Test
    fun testAuditEventLogging() = runBlocking {
        val event = CustomerPaymentAuditEvent(
            auditId = "AUD-PAY-1",
            paymentId = "PAY-001",
            customerId = customerId,
            tenantId = tenantId,
            projectId = projectId,
            actorId = "staff_1",
            actorRole = "STAFF",
            action = "PAYMENT_RECORDED"
        )
        repository.recordAuditEvent(event)

        val historyRes = repository.getAuditEvents(tenantId, projectId, "PAY-001")
        assertTrue(historyRes is DomainResult.Success)
        assertEquals(1, (historyRes as DomainResult.Success).data.size)
    }
}
