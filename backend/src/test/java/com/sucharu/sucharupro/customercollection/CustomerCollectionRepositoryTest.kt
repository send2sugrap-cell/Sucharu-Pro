package com.sucharu.sucharupro.customercollection

import com.sucharu.sucharupro.data.datasource.customercollection.FakeCustomerCollectionDataSource
import com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercollection.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCollectionRepositoryTest {

    private lateinit var repository: CustomerCollectionRepositoryImpl
    private val tenantId = "TENANT-REP-01"
    private val projectId = "PRJ-REP-01"
    private val customerId = "CUS-REP-01"

    @Before
    fun setup() {
        val ds = FakeCustomerCollectionDataSource()
        repository = CustomerCollectionRepositoryImpl(ds)
    }

    @Test
    fun testSaveAndRetrieveCollectionAction() = runBlocking {
        val action = CustomerCollectionAction(
            actionId = "ACT-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            actionType = CollectionActionType.PHONE_FOLLOW_UP,
            priority = CollectionPriority.HIGH,
            status = CollectionActionStatus.SCHEDULED,
            scheduledAt = System.currentTimeMillis() + 86400000,
            assignedUserId = "agent_01",
            notes = "Call customer regarding overdue invoice",
            idempotencyKey = "IDEM-001",
            createdAt = System.currentTimeMillis(),
            createdBy = "admin",
            updatedAt = System.currentTimeMillis(),
            updatedBy = "admin"
        )

        val saveRes = repository.saveAction(action)
        assertTrue(saveRes is DomainResult.Success)

        val getRes = repository.getActionById(tenantId, projectId, "ACT-001")
        assertTrue(getRes is DomainResult.Success)
        val fetched = (getRes as DomainResult.Success).data
        assertNotNull(fetched)
        assertEquals(CollectionActionType.PHONE_FOLLOW_UP, fetched?.actionType)
        assertEquals(CollectionPriority.HIGH, fetched?.priority)

        val listRes = repository.listActions(tenantId, projectId, customerId)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)
    }

    @Test
    fun testSaveAndRetrievePaymentPromise() = runBlocking {
        val promise = CustomerPaymentPromise(
            promiseId = "PROM-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            promisedAmount = BigDecimal("25000.0000"),
            promisedDate = System.currentTimeMillis() + 172800000,
            status = PaymentPromiseStatus.PENDING,
            notes = "Customer promised check payment",
            createdAt = System.currentTimeMillis(),
            createdBy = "agent_01",
            updatedAt = System.currentTimeMillis(),
            updatedBy = "agent_01"
        )

        val saveRes = repository.savePaymentPromise(promise)
        assertTrue(saveRes is DomainResult.Success)

        val getRes = repository.getPaymentPromiseById(tenantId, projectId, "PROM-001")
        assertTrue(getRes is DomainResult.Success)
        val fetched = (getRes as DomainResult.Success).data
        assertNotNull(fetched)
        assertEquals(BigDecimal("25000.0000"), fetched?.promisedAmount)
    }

    @Test
    fun testRecordAndRetrieveAuditEvents() = runBlocking {
        val event = CustomerCollectionAuditEvent(
            auditId = "AUD-COL-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            actionId = "ACT-001",
            actorId = "agent_01",
            actorRole = "STAFF",
            action = "COMPLETE_ACTION",
            reason = "Customer confirmed payment schedule",
            occurredAt = System.currentTimeMillis()
        )

        val recRes = repository.recordAuditEvent(event)
        assertTrue(recRes is DomainResult.Success)

        val auditRes = repository.getAuditEvents(tenantId, projectId, customerId)
        assertTrue(auditRes is DomainResult.Success)
        assertEquals(1, (auditRes as DomainResult.Success).data.size)
    }
}
