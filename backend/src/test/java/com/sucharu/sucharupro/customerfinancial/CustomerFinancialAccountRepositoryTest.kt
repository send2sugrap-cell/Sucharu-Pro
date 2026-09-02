package com.sucharu.sucharupro.customerfinancial

import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MODULE 14 STEP 01: Repository & Optimistic Locking Test.
 */
class CustomerFinancialAccountRepositoryTest {

    private lateinit var dataSource: FakeCustomerFinancialAccountDataSource
    private lateinit var repository: CustomerFinancialAccountRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-001"

    @Before
    fun setup() {
        dataSource = FakeCustomerFinancialAccountDataSource()
        repository = CustomerFinancialAccountRepositoryImpl(dataSource)
    }

    @Test
    fun testCreateAndRetrieveAccount() = runBlocking {
        val account = CustomerFinancialAccount(
            financialAccountId = "CFA-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            accountNumber = "ACC-001"
        )
        val createRes = repository.createAccount(account)
        assertTrue(createRes is DomainResult.Success)

        val getRes = repository.getAccountById(tenantId, projectId, "CFA-001")
        assertTrue(getRes is DomainResult.Success)
        assertEquals("ACC-001", (getRes as DomainResult.Success).data.accountNumber)

        val getByCustRes = repository.getAccountByCustomerId(tenantId, projectId, customerId)
        assertTrue(getByCustRes is DomainResult.Success)
        assertEquals("CFA-001", (getByCustRes as DomainResult.Success).data.financialAccountId)
    }

    @Test
    fun testUpdateStatusAndOptimisticLocking() = runBlocking {
        val account = CustomerFinancialAccount(
            financialAccountId = "CFA-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            accountNumber = "ACC-001",
            version = 1L
        )
        repository.createAccount(account)

        // Version conflict attempt
        val conflictRes = repository.updateAccountStatus(
            tenantId, projectId, "CFA-001",
            CustomerFinancialAccountStatus.SUSPENDED, "Review", "admin",
            expectedVersion = 99L // Wrong version
        )
        assertTrue("Must fail on version mismatch", conflictRes is DomainResult.Error)

        // Correct version
        val successRes = repository.updateAccountStatus(
            tenantId, projectId, "CFA-001",
            CustomerFinancialAccountStatus.SUSPENDED, "Review", "admin",
            expectedVersion = 1L
        )
        assertTrue(successRes is DomainResult.Success)
        val updated = (successRes as DomainResult.Success).data
        assertEquals(CustomerFinancialAccountStatus.SUSPENDED, updated.status)
        assertEquals(2L, updated.version)
    }

    @Test
    fun testAuditEventLoggingAndRetrieval() = runBlocking {
        val event = CustomerFinancialAccountAuditEvent(
            auditId = "AUD-001",
            financialAccountId = "CFA-001",
            customerId = customerId,
            tenantId = tenantId,
            projectId = projectId,
            actorId = "user_01",
            actorRole = "ADMIN",
            action = "ACCOUNT_CREATED",
            newStatus = CustomerFinancialAccountStatus.ACTIVE
        )
        val auditRes = repository.recordAuditEvent(event)
        assertTrue(auditRes is DomainResult.Success)

        val historyRes = repository.getAuditEvents(tenantId, projectId, "CFA-001")
        assertTrue(historyRes is DomainResult.Success)
        val history = (historyRes as DomainResult.Success).data
        assertEquals(1, history.size)
        assertEquals("ACCOUNT_CREATED", history[0].action)
    }
}
