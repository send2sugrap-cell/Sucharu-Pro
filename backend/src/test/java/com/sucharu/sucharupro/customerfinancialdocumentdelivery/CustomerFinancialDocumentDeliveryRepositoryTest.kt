package com.sucharu.sucharupro.customerfinancialdocumentdelivery

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CustomerFinancialDocumentDeliveryRepositoryTest {

    private lateinit var repository: CustomerFinancialDocumentDeliveryRepositoryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-001"

    @Before
    fun setup() {
        val fakeDs = FakeCustomerFinancialDocumentDeliveryDataSource()
        repository = CustomerFinancialDocumentDeliveryRepositoryImpl(fakeDs)
    }

    @Test
    fun testSaveAndRetrieveDelivery() = runBlocking {
        val delivery = CustomerFinancialDocumentDelivery(
            deliveryId = "DEL-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            documentId = "DOC-001",
            documentType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
            documentFormat = CustomerFinancialReportFormat.CSV,
            documentName = "statement.csv",
            storageReference = "docstore://PRJ-001/CUS-001/DOC-001.csv",
            checksum = "SHA256:123",
            fileSize = 512L,
            createdBy = "tester",
            updatedBy = "tester"
        )

        val saveRes = repository.saveDelivery(delivery)
        assertTrue(saveRes is DomainResult.Success)

        val getRes = repository.getDeliveryById(tenantId, projectId, "DEL-001")
        assertTrue(getRes is DomainResult.Success)
        val fetched = (getRes as DomainResult.Success).data
        assertNotNull(fetched)
        assertEquals("DEL-001", fetched?.deliveryId)
        assertEquals("statement.csv", fetched?.documentName)
    }

    @Test
    fun testRecordAndListAuditEvents() = runBlocking {
        val event = CustomerFinancialDocumentDeliveryAuditEvent(
            auditId = "AUD-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            deliveryId = "DEL-001",
            documentId = "DOC-001",
            eventType = CustomerFinancialDeliveryEventType.DOCUMENT_CREATED,
            actorId = "user-1",
            actorRole = "STAFF",
            checksum = "SHA256:123"
        )

        repository.recordAuditEvent(event)
        val listRes = repository.listAuditEvents(tenantId, projectId, "DEL-001")
        assertTrue(listRes is DomainResult.Success)
        val list = (listRes as DomainResult.Success).data
        assertEquals(1, list.size)
        assertEquals(CustomerFinancialDeliveryEventType.DOCUMENT_CREATED, list[0].eventType)
    }
}
