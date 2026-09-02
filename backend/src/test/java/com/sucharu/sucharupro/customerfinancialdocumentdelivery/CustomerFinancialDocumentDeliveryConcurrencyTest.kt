package com.sucharu.sucharupro.customerfinancialdocumentdelivery

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CustomerFinancialDocumentDeliveryConcurrencyTest {

    @Test
    fun testConcurrentAuditRecordingAndDeliveryReads() = runBlocking {
        val fakeDs = FakeCustomerFinancialDocumentDeliveryDataSource()
        val repo = CustomerFinancialDocumentDeliveryRepositoryImpl(fakeDs)
        val tenantId = "TENANT-CONC"
        val deliveryId = "DEL-CONC-1"

        val doc = CustomerFinancialDocumentDelivery(
            deliveryId = deliveryId,
            tenantId = tenantId,
            projectId = tenantId,
            customerId = "CUS-CONC",
            documentId = "DOC-CONC",
            documentType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
            documentFormat = CustomerFinancialReportFormat.CSV,
            documentName = "statement.csv",
            storageReference = "docstore://TENANT-CONC/CUS-CONC/DOC-CONC.csv",
            checksum = "SHA256:conc",
            fileSize = 100L,
            createdBy = "admin",
            updatedBy = "admin"
        )
        repo.saveDelivery(doc)

        val jobs = (1..50).map { i ->
            async(Dispatchers.Default) {
                val event = CustomerFinancialDocumentDeliveryAuditEvent(
                    auditId = "AUD-$i-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = tenantId,
                    customerId = "CUS-CONC",
                    deliveryId = deliveryId,
                    documentId = "DOC-CONC",
                    eventType = CustomerFinancialDeliveryEventType.DOCUMENT_DOWNLOADED,
                    actorId = "user-$i",
                    actorRole = "CUSTOMER",
                    timestamp = System.currentTimeMillis()
                )
                repo.recordAuditEvent(event)
            }
        }
        jobs.awaitAll()

        val listRes = repo.listAuditEvents(tenantId, tenantId, deliveryId)
        assertTrue(listRes is DomainResult.Success)
        val audits = (listRes as DomainResult.Success).data
        assertEquals(50, audits.size)
    }
}
