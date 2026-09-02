package com.sucharu.sucharupro.customerfinancial

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.service.customerfinancial.CustomerFinancialAccountServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MODULE 14 STEP 01: Service Layer Integration Tests.
 */
class CustomerFinancialAccountServiceTest {

    private lateinit var customerDs: FakeCustomerDataSource
    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountDs: FakeCustomerFinancialAccountDataSource
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var service: CustomerFinancialAccountServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-TEST-001"

    @Before
    fun setup() {
        customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)
        accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        service = CustomerFinancialAccountServiceImpl(accountRepo, customerRepo)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-001001",
                    displayName = "Test Enterprise Customer",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801700000000",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
        }
    }

    @Test
    fun testCreateFinancialAccountSuccess() = runBlocking {
        val res = service.createFinancialAccount(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            currency = "BDT",
            notes = "Standard commercial account",
            actorId = "staff_user",
            actorRole = "STAFF"
        )
        assertTrue(res is DomainResult.Success)
        val account = (res as DomainResult.Success).data
        assertEquals(customerId, account.customerId)
        assertEquals("BDT", account.currency)
        assertEquals(CustomerFinancialAccountStatus.ACTIVE, account.status)

        // Verify audit event was automatically recorded
        val auditRes = service.getAuditHistory(tenantId, projectId, account.financialAccountId)
        assertTrue(auditRes is DomainResult.Success)
        val audits = (auditRes as DomainResult.Success).data
        assertEquals(1, audits.size)
        assertEquals("ACCOUNT_CREATED", audits[0].action)
    }

    @Test
    fun testDuplicateAccountCreationRejected() = runBlocking {
        service.createFinancialAccount(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            currency = "BDT",
            actorId = "staff_user",
            actorRole = "STAFF"
        )

        // Attempt second account for same customer -> MUST fail
        val dupRes = service.createFinancialAccount(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            currency = "BDT",
            actorId = "staff_user",
            actorRole = "STAFF"
        )
        assertTrue("Duplicate account creation must be rejected", dupRes is DomainResult.Error)
    }

    @Test
    fun testCreateAccountForNonExistentCustomerFails() = runBlocking {
        val nonExistentRes = service.createFinancialAccount(
            tenantId = tenantId,
            projectId = projectId,
            customerId = "NON-EXISTENT-CUS",
            currency = "BDT",
            actorId = "staff_user",
            actorRole = "STAFF"
        )
        assertTrue("Account creation for missing customer must fail", nonExistentRes is DomainResult.Error)
    }

    @Test
    fun testLifecycleStatusTransitionsAndAudit() = runBlocking {
        val created = (service.createFinancialAccount(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            currency = "BDT",
            actorId = "staff_user",
            actorRole = "STAFF"
        ) as DomainResult.Success).data

        // 1. Suspend Account
        val suspendRes = service.suspendAccount(
            tenantId = tenantId,
            projectId = projectId,
            financialAccountId = created.financialAccountId,
            reason = "Pending KYC renewal",
            actorId = "compliance_lead",
            actorRole = "MANAGER",
            expectedVersion = 1L
        )
        assertTrue(suspendRes is DomainResult.Success)
        val suspended = (suspendRes as DomainResult.Success).data
        assertEquals(CustomerFinancialAccountStatus.SUSPENDED, suspended.status)
        assertEquals("Pending KYC renewal", suspended.suspensionReason)
        assertEquals(2L, suspended.version)

        // 2. Reactivate Account
        val reactivateRes = service.reactivateAccount(
            tenantId = tenantId,
            projectId = projectId,
            financialAccountId = created.financialAccountId,
            reason = "KYC verified",
            actorId = "compliance_lead",
            actorRole = "MANAGER",
            expectedVersion = 2L
        )
        assertTrue(reactivateRes is DomainResult.Success)
        val reactivated = (reactivateRes as DomainResult.Success).data
        assertEquals(CustomerFinancialAccountStatus.ACTIVE, reactivated.status)
        assertEquals(3L, reactivated.version)

        // 3. Close Account
        val closeRes = service.closeAccount(
            tenantId = tenantId,
            projectId = projectId,
            financialAccountId = created.financialAccountId,
            reason = "Customer company merged",
            actorId = "admin_user",
            actorRole = "ADMIN",
            expectedVersion = 3L
        )
        assertTrue(closeRes is DomainResult.Success)
        val closed = (closeRes as DomainResult.Success).data
        assertEquals(CustomerFinancialAccountStatus.CLOSED, closed.status)
        assertEquals(4L, closed.version)

        // Verify total audit history count
        val audits = (service.getAuditHistory(tenantId, projectId, created.financialAccountId) as DomainResult.Success).data
        assertEquals(4, audits.size) // Created -> Suspended -> Reactivated -> Closed
    }
}
