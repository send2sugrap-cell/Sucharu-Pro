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
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerSettlementIsolationTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var allocationRepo: CustomerPaymentAllocationRepositoryImpl
    private lateinit var service: CustomerSettlementServiceImpl

    private val tenantA = "TENANT-A"
    private val projectA = "PRJ-A"
    private val customerA = "CUS-A"
    private val accountA = "CFA-A"

    private val tenantB = "TENANT-B"
    private val projectB = "PRJ-B"
    private val customerB = "CUS-B"
    private val accountB = "CFA-B"

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
            // Setup Tenant A Customer, Account, Invoice & Payment
            customerRepo.addCustomer(
                Customer(
                    customerId = customerA,
                    customerCode = "CUS-A",
                    displayName = "Customer A",
                    primaryPhone = "+8801700000001",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountA,
                    tenantId = tenantA,
                    projectId = projectA,
                    customerId = customerA,
                    accountNumber = "CFA-A",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-A",
                    tenantId = tenantA,
                    projectId = projectA,
                    customerId = customerA,
                    customerFinancialAccountId = accountA,
                    invoiceNumber = "INV-A",
                    grandTotal = BigDecimal("5000.0000"),
                    dueAmount = BigDecimal("5000.0000"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )
            paymentRepo.createPayment(
                CustomerPayment(
                    paymentId = "PAY-A",
                    tenantId = tenantA,
                    projectId = projectA,
                    paymentNumber = "PAY-A",
                    customerId = customerA,
                    customerFinancialAccountId = accountA,
                    amount = BigDecimal("5000.0000"),
                    status = CustomerPaymentStatus.CONFIRMED
                )
            )

            // Setup Tenant B Customer, Account, Invoice & Payment
            customerRepo.addCustomer(
                Customer(
                    customerId = customerB,
                    customerCode = "CUS-B",
                    displayName = "Customer B",
                    primaryPhone = "+8801700000002",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountB,
                    tenantId = tenantB,
                    projectId = projectB,
                    customerId = customerB,
                    accountNumber = "CFA-B",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-B",
                    tenantId = tenantB,
                    projectId = projectB,
                    customerId = customerB,
                    customerFinancialAccountId = accountB,
                    invoiceNumber = "INV-B",
                    grandTotal = BigDecimal("3000.0000"),
                    dueAmount = BigDecimal("3000.0000"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )
        }
    }

    @Test
    fun testRejectCrossTenantAllocation() = runBlocking {
        // Attempt to allocate Payment from Tenant A to Invoice in Tenant B
        val res = service.allocatePayment(
            tenantId = tenantA,
            projectId = projectA,
            paymentId = "PAY-A",
            invoiceId = "INV-B", // Belongs to Tenant B
            amount = BigDecimal("1000.0000")
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testRejectCrossCustomerAllocation() = runBlocking {
        // Create an invoice for another customer in Tenant A
        accountRepo.createAccount(
            CustomerFinancialAccount(
                financialAccountId = "CFA-A2",
                tenantId = tenantA,
                projectId = projectA,
                customerId = "CUS-OTHER",
                accountNumber = "CFA-A2",
                status = CustomerFinancialAccountStatus.ACTIVE
            )
        )
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-OTHER",
                tenantId = tenantA,
                projectId = projectA,
                customerId = "CUS-OTHER",
                customerFinancialAccountId = "CFA-A2",
                invoiceNumber = "INV-OTHER",
                grandTotal = BigDecimal("2000.0000"),
                dueAmount = BigDecimal("2000.0000"),
                status = CustomerInvoiceStatus.ISSUED
            )
        )

        // Allocate Payment A to Invoice Other (both in Tenant A, but different customers)
        val res = service.allocatePayment(
            tenantId = tenantA,
            projectId = projectA,
            paymentId = "PAY-A",
            invoiceId = "INV-OTHER",
            amount = BigDecimal("1000.0000")
        )
        assertTrue(res is DomainResult.Error)
    }
}
