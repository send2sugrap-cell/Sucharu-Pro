package com.sucharu.sucharupro.customerpayment

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 03: Idempotency & Duplicate Payment Prevention Tests.
 */
class CustomerPaymentIdempotencyTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var service: CustomerPaymentServiceImpl

    private val tenantId = "TENANT-IDEMP"
    private val projectId = "PRJ-IDEMP"
    private val customerId = "CUS-IDEMP-01"
    private val accountId = "CFA-IDEMP-01"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)
        val paymentDs = FakeCustomerPaymentDataSource()
        paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)

        service = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-ID-01",
                    displayName = "Idempotency Client",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801755555555",
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
                    accountNumber = "ACC-ID-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testExactSameIdempotencyKeyReturnsOriginalPayment() = runBlocking {
        val idempotencyKey = "PAY-IDEMP-KEY-999"

        // First attempt
        val res1 = service.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = null,
            amount = BigDecimal("2500.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BKASH,
            referenceNumber = "TRX-112233",
            idempotencyKey = idempotencyKey,
            actorId = "cashier_1",
            actorRole = "STAFF"
        )
        assertTrue(res1 is DomainResult.Success)
        val payment1 = (res1 as DomainResult.Success).data

        // Retry with same key and same parameters
        val res2 = service.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = null,
            amount = BigDecimal("2500.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BKASH,
            referenceNumber = "TRX-112233",
            idempotencyKey = idempotencyKey,
            actorId = "cashier_1",
            actorRole = "STAFF"
        )
        assertTrue(res2 is DomainResult.Success)
        val payment2 = (res2 as DomainResult.Success).data

        // Both must return the identical payment ID
        assertEquals(payment1.paymentId, payment2.paymentId)

        // Only one record in repository
        val list = (service.listPayments(tenantId, projectId) as DomainResult.Success).data
        assertEquals(1, list.size)
    }

    @Test
    fun testConflictingParametersWithSameIdempotencyKeyFails() = runBlocking {
        val idempotencyKey = "PAY-IDEMP-KEY-CONFLICT"

        // First attempt: 1,000 BDT
        service.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = null,
            amount = BigDecimal("1000.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            idempotencyKey = idempotencyKey,
            actorId = "cashier_1",
            actorRole = "STAFF"
        )

        // Conflicting attempt with same key: 2,000 BDT
        val conflictRes = service.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = null,
            amount = BigDecimal("2000.00"), // Different amount!
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            idempotencyKey = idempotencyKey,
            actorId = "cashier_1",
            actorRole = "STAFF"
        )
        assertTrue("Conflicting payload on same key must be rejected", conflictRes is DomainResult.Error)
    }
}
