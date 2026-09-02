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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MODULE 14 STEP 01: Concurrency & Idempotency Tests.
 */
class CustomerFinancialAccountConcurrencyTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var service: CustomerFinancialAccountServiceImpl

    private val tenantId = "TENANT-CONCURRENT"
    private val projectId = "PRJ-CONCURRENT"
    private val customerId = "CUS-CONCURRENT-01"

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
                    customerId = customerId,
                    customerCode = "CUS-CONC-01",
                    displayName = "Concurrent Test Customer",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801733333333",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
        }
    }

    @Test
    fun testConcurrentSimultaneousAccountCreationProducesExactlyOneAccount() = runBlocking {
        val attempts = 10
        val results = coroutineScope {
            (1..attempts).map { index ->
                async {
                    service.createFinancialAccount(
                        tenantId = tenantId,
                        projectId = projectId,
                        customerId = customerId,
                        currency = "BDT",
                        notes = "Concurrent attempt #$index",
                        actorId = "thread_$index",
                        actorRole = "STAFF"
                    )
                }
            }.awaitAll()
        }

        val successes = results.filterIsInstance<DomainResult.Success<*>>()
        val errors = results.filterIsInstance<DomainResult.Error>()

        assertEquals("Exactly one concurrent creation must succeed", 1, successes.size)
        assertEquals("All other concurrent creation attempts must fail", attempts - 1, errors.size)

        // Verify account retrieval returns the single canonical account
        val finalAccountRes = service.getFinancialAccountByCustomerId(tenantId, projectId, customerId)
        assertTrue(finalAccountRes is DomainResult.Success)
    }
}
