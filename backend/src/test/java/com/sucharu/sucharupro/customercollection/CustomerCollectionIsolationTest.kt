package com.sucharu.sucharupro.customercollection

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercollection.FakeCustomerCollectionDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customercollection.CollectionActionType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCollectionIsolationTest {

    private lateinit var service: CustomerCollectionServiceImpl

    private val tenantA = "TENANT-A"
    private val projectA = "PRJ-A"
    private val customerA = "CUS-A"

    private val tenantB = "TENANT-B"
    private val projectB = "PRJ-B"
    private val customerB = "CUS-B"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        val customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        val accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        val invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)
        val paymentDs = FakeCustomerPaymentDataSource()
        val paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)
        val creditDs = FakeCustomerCreditDataSource()
        val creditRepo = CustomerCreditRepositoryImpl(creditDs)
        val allocationDs = FakeCustomerPaymentAllocationDataSource()
        val allocationRepo = CustomerPaymentAllocationRepositoryImpl(allocationDs)
        val creditControlDs = FakeCustomerCreditControlDataSource()
        val creditControlRepo = CustomerCreditControlRepositoryImpl(creditControlDs)
        val collectionDs = FakeCustomerCollectionDataSource()
        val collectionRepo = CustomerCollectionRepositoryImpl(collectionDs)

        val settlementService = CustomerSettlementServiceImpl(
            allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo
        )
        val creditControlService = CustomerCreditControlServiceImpl(
            creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo
        )

        service = CustomerCollectionServiceImpl(
            collectionRepository = collectionRepo,
            customerRepository = customerRepo,
            accountRepository = accountRepo,
            invoiceRepository = invoiceRepo,
            settlementService = settlementService,
            creditControlService = creditControlService
        )

        runBlocking {
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
                    financialAccountId = "CFA-A",
                    tenantId = tenantA,
                    projectId = projectA,
                    customerId = customerA,
                    accountNumber = "CFA-A",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

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
                    financialAccountId = "CFA-B",
                    tenantId = tenantB,
                    projectId = projectB,
                    customerId = customerB,
                    accountNumber = "CFA-B",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testRejectCrossTenantActionCreation() = runBlocking {
        // Attempt to create collection action for Customer B under Tenant A context
        val res = service.createCollectionAction(
            tenantId = tenantA,
            projectId = projectA,
            customerId = customerB,
            actionType = CollectionActionType.EMAIL,
            scheduledAt = System.currentTimeMillis() + 86400000,
            actorId = "staff_a",
            actorRole = "STAFF"
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testRejectCrossTenantPromiseCreation() = runBlocking {
        // Attempt to create payment promise for Customer B under Tenant A context
        val res = service.createPaymentPromise(
            tenantId = tenantA,
            projectId = projectA,
            customerId = customerB,
            promisedAmount = BigDecimal("5000.0000"),
            promisedDate = System.currentTimeMillis() + 86400000,
            actorId = "staff_a",
            actorRole = "STAFF"
        )
        assertTrue(res is DomainResult.Error)
    }
}
