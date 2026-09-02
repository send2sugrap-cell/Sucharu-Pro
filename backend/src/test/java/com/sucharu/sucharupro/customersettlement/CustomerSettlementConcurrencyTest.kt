package com.sucharu.sucharupro.customersettlement

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerSettlementConcurrencyTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var allocationRepo: CustomerPaymentAllocationRepositoryImpl
    private lateinit var service: CustomerSettlementServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-CONCUR-001"
    private val accountId = "CFA-CONCUR-001"

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
        val allocationDs = FakeCustomerPaymentAllocationDataSource()
        allocationRepo = CustomerPaymentAllocationRepositoryImpl(allocationDs)

        service = CustomerSettlementServiceImpl(
            allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo
        )

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-C-01",
                    displayName = "Concurrency Customer",
                    primaryPhone = "+8801700000000",
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
                    accountNumber = "CFA-C-1001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            // Invoice 1: 10,000 due
            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-CONCUR",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    invoiceNumber = "INV-CONCUR",
                    grandTotal = BigDecimal("10000.0000"),
                    dueAmount = BigDecimal("10000.0000"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )

            // Payment: 5,000
            paymentRepo.createPayment(
                CustomerPayment(
                    paymentId = "PAY-CONCUR",
                    tenantId = tenantId,
                    projectId = projectId,
                    paymentNumber = "PAY-CONCUR",
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    amount = BigDecimal("5000.0000"),
                    status = CustomerPaymentStatus.CONFIRMED
                )
            )
        }
    }

    @Test
    fun testConcurrentAllocationsCannotExceedPaymentAmount() = runBlocking {
        // Launch 10 simultaneous attempts to allocate 1,000 from the 5,000 payment
        val deferred = (1..10).map { i ->
            async {
                service.allocatePayment(
                    tenantId = tenantId,
                    projectId = projectId,
                    paymentId = "PAY-CONCUR",
                    invoiceId = "INV-CONCUR",
                    amount = BigDecimal("1000.0000"),
                    idempotencyKey = "IDEMP-$i"
                )
            }
        }

        val results = deferred.awaitAll()
        val successCount = results.count { it is DomainResult.Success }

        // Total successful allocations must not exceed 5 (5 x 1000 = 5000)
        assertTrue("Success count ($successCount) must be <= 5", successCount <= 5)

        val totalAllocated = (allocationRepo.listAllocations(
            tenantId, projectId, paymentId = "PAY-CONCUR", status = CustomerPaymentAllocationStatus.ALLOCATED
        ) as DomainResult.Success).data.map { it.allocatedAmount }.fold(BigDecimal.ZERO, BigDecimal::add)

        assertTrue(totalAllocated <= BigDecimal("5000.0000"))
    }
}
