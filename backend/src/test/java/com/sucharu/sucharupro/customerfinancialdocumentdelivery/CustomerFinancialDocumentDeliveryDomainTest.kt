package com.sucharu.sucharupro.customerfinancialdocumentdelivery

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import org.junit.Assert.*
import org.junit.Test

class CustomerFinancialDocumentDeliveryDomainTest {

    @Test
    fun testDeliveryModelPropertiesAndDefaults() {
        val delivery = CustomerFinancialDocumentDelivery(
            deliveryId = "DEL-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            customerId = "CUS-001",
            documentId = "DOC-001",
            documentType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
            documentFormat = CustomerFinancialReportFormat.CSV,
            documentName = "statement_CUS-001.csv",
            storageReference = "docstore://PRJ-001/CUS-001/DOC-001.csv",
            checksum = "SHA256:abcdef123456",
            fileSize = 2048L,
            mimeType = "text/csv",
            deliveryStatus = CustomerFinancialDeliveryStatus.READY,
            createdBy = "admin",
            updatedBy = "admin"
        )

        assertEquals("DEL-001", delivery.deliveryId)
        assertEquals("TENANT-001", delivery.tenantId)
        assertEquals("PRJ-001", delivery.projectId)
        assertEquals("CUS-001", delivery.customerId)
        assertEquals(CustomerFinancialDeliveryStatus.READY, delivery.deliveryStatus)
        assertEquals(0, delivery.accessCount)
        assertFalse(delivery.isRevoked)
        assertFalse(delivery.isExpired)
        assertTrue(delivery.isDownloadable)
    }

    @Test
    fun testExpirationAndRevocationFlags() {
        val pastTime = System.currentTimeMillis() - 10000L
        val expiredDelivery = CustomerFinancialDocumentDelivery(
            deliveryId = "DEL-EXP",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            customerId = "CUS-001",
            documentId = "DOC-EXP",
            documentType = CustomerFinancialReportType.INVOICE_REPORT,
            documentFormat = CustomerFinancialReportFormat.PDF,
            documentName = "invoices.pdf",
            storageReference = "docstore://PRJ-001/CUS-001/DOC-EXP.pdf",
            checksum = "SHA256:exp123",
            fileSize = 1024L,
            expiresAt = pastTime,
            createdBy = "admin",
            updatedBy = "admin"
        )
        assertTrue(expiredDelivery.isExpired)
        assertFalse(expiredDelivery.isDownloadable)

        val revokedDelivery = expiredDelivery.copy(
            expiresAt = null,
            isRevoked = true,
            revocationReason = "Data correction required"
        )
        assertTrue(revokedDelivery.isRevoked)
        assertFalse(revokedDelivery.isDownloadable)
    }
}
