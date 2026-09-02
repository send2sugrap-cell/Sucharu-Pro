package com.sucharu.sucharupro.customerfinancial

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MODULE 14 STEP 01: RBAC & Customer Ownership Authorization Tests.
 */
class CustomerFinancialAccountSecurityTest {

    private lateinit var useCases: BackendUseCases

    private val projectId = "PRJ-SEC-01"
    private val customerId1 = "CUS-SEC-01"
    private val customerId2 = "CUS-SEC-02"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF
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

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "affiliate_01",
        projectId = projectId,
        username = "affiliate_user",
        role = UserRole.AFFILIATE
    )

    private lateinit var account1: CustomerFinancialAccountDto

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        val customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        val accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val service = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required")
            }
        }

        val customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createCustomerRepository(tenantId: String) = customerRepo
            override fun createCustomerFinancialAccountRepository(tenantId: String) = accountRepo
            override fun createCustomerFinancialAccountService(tenantId: String) = service
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId1,
                    customerCode = "CUS-01",
                    displayName = "Customer One",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000001",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId2,
                    customerCode = "CUS-02",
                    displayName = "Customer Two",
                    customerType = CustomerType.INDIVIDUAL,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000002",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            // Staff creates Account for Customer 1
            account1 = useCases.createCustomerFinancialAccount(
                staffPrincipal,
                CreateCustomerFinancialAccountRequest(customerId = customerId1, currency = "BDT")
            )
        }
    }

    @Test
    fun testCustomerCanAccessOwnAccount() = runBlocking {
        val account = useCases.getCustomerFinancialAccountByCustomer(customerPrincipal1, customerId1)
        assertEquals(account1.financialAccountId, account.financialAccountId)
    }

    @Test
    fun testCustomerCannotAccessAnotherCustomerAccount() = runBlocking {
        // Customer 2 attempts to query Customer 1's financial account -> MUST throw ForbiddenException
        try {
            useCases.getCustomerFinancialAccountByCustomer(customerPrincipal2, customerId1)
            fail("Must block cross-customer access attempt")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }

        try {
            useCases.getCustomerFinancialAccount(customerPrincipal2, account1.financialAccountId)
            fail("Must block cross-customer direct ID lookup")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testCustomerCannotCreateFinancialAccount() = runBlocking {
        // Customer role trying to create account must be rejected by RBAC
        try {
            useCases.createCustomerFinancialAccount(
                customerPrincipal1,
                CreateCustomerFinancialAccountRequest(customerId = customerId2)
            )
            fail("Customer must not be allowed to create financial accounts")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testAffiliateCannotAccessCustomerFinancialAccount() = runBlocking {
        try {
            useCases.getCustomerFinancialAccount(affiliatePrincipal, account1.financialAccountId)
            fail("Affiliate role must be blocked from customer financial accounts")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException)
        }
    }
}
