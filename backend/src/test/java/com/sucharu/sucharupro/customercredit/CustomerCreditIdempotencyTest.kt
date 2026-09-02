package com.sucharu.sucharupro.customercredit

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.service.customercredit.CustomerCreditServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditIdempotencyTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var service: CustomerCreditServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-IDEM-001"
    private val accountId = "CFA-IDEM-001"

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
        val creditDs = FakeCustomerCreditDataSource()
        creditRepo = CustomerCreditRepositoryImpl(creditDs)

        service = CustomerCreditServiceImpl(creditRepo, accountRepo, invoiceRepo, customerRepo, paymentRepo)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-ID-01",
                    displayName = "Idempotency Customer",
                    primaryPhone = "01700000000",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
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
                    accountNumber = "CFA-ID-1001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testRecordAdvance_IdempotencyKeyReturnsSameEntity() = runBlocking {
        val key = "IDEM-ADVANCE-UNIQUE-KEY-001"
        val res1 = service.recordAdvance(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("1500.0000"),
            idempotencyKey = key,
            actorId = "staff1",
            actorRole = "STAFF"
        )
        assertTrue(res1 is DomainResult.Success)
        val advance1 = (res1 as DomainResult.Success).data

        val res2 = service.recordAdvance(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("1500.0000"),
            idempotencyKey = key,
            actorId = "staff1",
            actorRole = "STAFF"
        )
        assertTrue(res2 is DomainResult.Success)
        val advance2 = (res2 as DomainResult.Success).data

        assertEquals(advance1.advanceId, advance2.advanceId)
        assertEquals(advance1.advanceNumber, advance2.advanceNumber)
    }
}
