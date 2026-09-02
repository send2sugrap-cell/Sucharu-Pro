package com.sucharu.sucharupro.customerfinancial

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MODULE 14 STEP 01: Multi-Tenant, Multi-Project & Customer Isolation Tests.
 */
class CustomerFinancialAccountIsolationTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var service: CustomerFinancialAccountServiceImpl

    private val tenantA = "TENANT-ALPHA"
    private val tenantB = "TENANT-BETA"

    private val projectA = "PRJ-ALPHA"
    private val projectB = "PRJ-BETA"

    private val customerA = "CUS-ALPHA-01"
    private val customerB = "CUS-BETA-01"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        service = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerA,
                    customerCode = "CUS-A-01",
                    displayName = "Customer Alpha",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801711111111",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            customerRepo.addCustomer(
                Customer(
                    customerId = customerB,
                    customerCode = "CUS-B-01",
                    displayName = "Customer Beta",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801722222222",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            // Seed Account for Customer A in Tenant Alpha / Project Alpha
            service.createFinancialAccount(
                tenantId = tenantA,
                projectId = projectA,
                customerId = customerA,
                currency = "BDT",
                actorId = "staff_a",
                actorRole = "STAFF"
            )
        }
    }

    @Test
    fun testTenantIsolation() = runBlocking {
        // Tenant B attempting to access Customer A's account in Tenant A must fail
        val accountA = (service.getFinancialAccountByCustomerId(tenantA, projectA, customerA) as DomainResult.Success).data

        val crossTenantLookup = service.getFinancialAccountById(tenantB, projectA, accountA.financialAccountId)
        assertTrue("Cross-tenant lookup must fail", crossTenantLookup is DomainResult.Error)

        val crossTenantByCust = service.getFinancialAccountByCustomerId(tenantB, projectA, customerA)
        assertTrue("Cross-tenant customer lookup must fail", crossTenantByCust is DomainResult.Error)
    }

    @Test
    fun testProjectIsolation() = runBlocking {
        // Project B in Tenant A attempting to access Project A's account must fail
        val accountA = (service.getFinancialAccountByCustomerId(tenantA, projectA, customerA) as DomainResult.Success).data

        val crossProjectLookup = service.getFinancialAccountById(tenantA, projectB, accountA.financialAccountId)
        assertTrue("Cross-project lookup must fail", crossProjectLookup is DomainResult.Error)
    }
}
