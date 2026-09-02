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
 * MODULE 14 STEP 03: Multi-Tenant & Multi-Project Isolation Tests for Payments.
 */
class CustomerPaymentIsolationTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var service: CustomerPaymentServiceImpl

    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val projectA = "PRJ-A"
    private val projectB = "PRJ-B"

    private val customerA = "CUS-A-1"
    private val accountA = "CFA-A-1"

    private lateinit var paymentAId: String

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
                    customerId = customerA,
                    customerCode = "CUS-A",
                    displayName = "Customer A",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000001",
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
                    accountNumber = "ACC-A-1",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            val p = (service.recordPayment(
                tenantId = tenantA,
                projectId = projectA,
                customerId = customerA,
                customerFinancialAccountId = accountA,
                invoiceId = null,
                amount = BigDecimal("1000.00"),
                currency = "BDT",
                paymentMethod = CustomerPaymentMethod.CASH,
                actorId = "staff_a",
                actorRole = "STAFF"
            ) as DomainResult.Success).data
            paymentAId = p.paymentId
        }
    }

    @Test
    fun testTenantIsolationOnPaymentLookup() = runBlocking {
        val crossTenantLookup = service.getPaymentById(tenantB, projectA, paymentAId)
        assertTrue("Cross-tenant lookup must fail", crossTenantLookup is DomainResult.Error)

        val tenantBList = service.listPayments(tenantB, projectA)
        assertTrue(tenantBList is DomainResult.Success)
        assertTrue((tenantBList as DomainResult.Success).data.isEmpty())
    }

    @Test
    fun testProjectIsolationOnPaymentLookup() = runBlocking {
        val crossProjectLookup = service.getPaymentById(tenantA, projectB, paymentAId)
        assertTrue("Cross-project lookup must fail", crossProjectLookup is DomainResult.Error)
    }
}
