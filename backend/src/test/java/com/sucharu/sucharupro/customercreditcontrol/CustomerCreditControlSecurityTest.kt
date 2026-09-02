package com.sucharu.sucharupro.customercreditcontrol

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditControlSecurityTest {

    private lateinit var useCases: BackendUseCases

    private val projectId = "PRJ-SEC-01"
    private val customerId1 = "CUS-SEC-01"
    private val customerId2 = "CUS-SEC-02"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF,
        permissions = setOf(UserPermission.MANAGE_CUSTOMERS, UserPermission.MANAGE_FINANCE)
    )

    private val customerPrincipal1 = AuthenticatedPrincipal(
        userId = "client_01",
        projectId = projectId,
        username = "client_user_1",
        role = UserRole.CUSTOMER,
        customerId = customerId1
    )

    private val customerPrincipal2 = AuthenticatedPrincipal(
        userId = "client_02",
        projectId = projectId,
        username = "client_user_2",
        role = UserRole.CUSTOMER,
        customerId = customerId2
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor_01",
        projectId = projectId,
        username = "vendor_user",
        role = UserRole.VENDOR,
        vendorId = "VND-001"
    )

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

        val settlementService = CustomerSettlementServiceImpl(allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)
        val creditControlService = CustomerCreditControlServiceImpl(creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo)

        val mockDb = MockIntegrationDb()
        val txManager = DefaultPostgresTransactionManager(mockDb)

        val customFactory = object : PostgresRepositoryFactory(txManager) {
            override fun createCustomerRepository(tenantId: String) = customerRepo
            override fun createCustomerFinancialAccountRepository(tenantId: String) = accountRepo
            override fun createCustomerInvoiceRepository(tenantId: String) = invoiceRepo
            override fun createCustomerPaymentRepository(tenantId: String) = paymentRepo
            override fun createCustomerCreditRepository(tenantId: String) = creditRepo
            override fun createCustomerPaymentAllocationRepository(tenantId: String) = allocationRepo
            override fun createCustomerSettlementService(tenantId: String) = settlementService
            override fun createCustomerCreditControlRepository(tenantId: String) = creditControlRepo
            override fun createCustomerCreditControlService(tenantId: String) = creditControlService
        }

        useCases = BackendUseCases(txManager, customFactory)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId1,
                    customerCode = "CUS-01",
                    displayName = "Customer 1",
                    primaryPhone = "+8801700000001",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = "CFA-01",
                    tenantId = projectId,
                    projectId = projectId,
                    customerId = customerId1,
                    accountNumber = "ACC-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testCustomerCannotUpdateCreditLimit() = runBlocking {
        try {
            useCases.updateCustomerCreditProfile(
                customerPrincipal1,
                customerId1,
                UpdateCustomerCreditProfileRequest(
                    creditLimit = BigDecimal("500000.0000"),
                    paymentTermsType = "NET_30",
                    creditDays = 30
                )
            )
            fail("Customer must not be allowed to update credit limits")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testCustomerCannotPlaceFinancialHold() = runBlocking {
        try {
            useCases.placeCustomerFinancialHold(
                customerPrincipal1,
                customerId1,
                CustomerFinancialHoldRequest(reason = "Self hold")
            )
            fail("Customer must not be allowed to place financial holds")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testVendorCannotAccessCustomerCreditControl() = runBlocking {
        try {
            useCases.getCustomerReceivableRiskSummary(vendorPrincipal, customerId1)
            fail("Vendor must not be allowed to access customer credit risk")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testCustomerCanViewOwnCreditProfile() = runBlocking {
        val profile = useCases.getCustomerCreditProfile(customerPrincipal1, customerId1)
        assertTrue(profile.customerId == customerId1)
    }

    @Test
    fun testCustomerCannotViewAnotherCustomerCreditProfile() = runBlocking {
        try {
            useCases.getCustomerCreditProfile(customerPrincipal2, customerId1)
            fail("Customer 2 must not be allowed to view Customer 1's credit profile")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }
}
