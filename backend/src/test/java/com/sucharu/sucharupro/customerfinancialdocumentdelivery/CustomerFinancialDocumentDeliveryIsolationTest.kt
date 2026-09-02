package com.sucharu.sucharupro.customerfinancialdocumentdelivery

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CustomerFinancialDocumentDeliveryIsolationTest {

    private lateinit var deliveryRepo: CustomerFinancialDocumentDeliveryRepositoryImpl
    private val tenantA = "TENANT-AAA"
    private val tenantB = "TENANT-BBB"

    @Before
    fun setup() {
        val fakeDs = FakeCustomerFinancialDocumentDeliveryDataSource()
        deliveryRepo = CustomerFinancialDocumentDeliveryRepositoryImpl(fakeDs)

        runBlocking {
            val docA = CustomerFinancialDocumentDelivery(
                deliveryId = "DEL-A1",
                tenantId = tenantA,
                projectId = tenantA,
                customerId = "CUS-A",
                documentId = "DOC-A1",
                documentType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
                documentFormat = CustomerFinancialReportFormat.CSV,
                documentName = "statement_A.csv",
                storageReference = "docstore://TENANT-AAA/CUS-A/DOC-A1.csv",
                checksum = "SHA256:aaa",
                fileSize = 100L,
                createdBy = "admin",
                updatedBy = "admin"
            )
            val docB = CustomerFinancialDocumentDelivery(
                deliveryId = "DEL-B1",
                tenantId = tenantB,
                projectId = tenantB,
                customerId = "CUS-B",
                documentId = "DOC-B1",
                documentType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
                documentFormat = CustomerFinancialReportFormat.CSV,
                documentName = "statement_B.csv",
                storageReference = "docstore://TENANT-BBB/CUS-B/DOC-B1.csv",
                checksum = "SHA256:bbb",
                fileSize = 100L,
                createdBy = "admin",
                updatedBy = "admin"
            )
            deliveryRepo.saveDelivery(docA)
            deliveryRepo.saveDelivery(docB)
        }
    }

    @Test
    fun testTenantIsolationOnDocumentDeliveries() = runBlocking {
        val listA = deliveryRepo.listDeliveries(tenantA, tenantA, null, null, null)
        assertTrue(listA is DomainResult.Success)
        val dataA = (listA as DomainResult.Success).data
        assertEquals(1, dataA.size)
        assertEquals("DEL-A1", dataA[0].deliveryId)

        val listB = deliveryRepo.listDeliveries(tenantB, tenantB, null, null, null)
        assertTrue(listB is DomainResult.Success)
        val dataB = (listB as DomainResult.Success).data
        assertEquals(1, dataB.size)
        assertEquals("DEL-B1", dataB[0].deliveryId)

        val crossRes = deliveryRepo.getDeliveryById(tenantA, tenantA, "DEL-B1")
        assertTrue(crossRes is DomainResult.Success)
        assertNull((crossRes as DomainResult.Success).data)
    }
}
