package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalInvoiceDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalInvoiceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalInvoiceRepositoryTest {

    private val dataSource = FakeVendorPortalInvoiceDataSource()
    private val repository = VendorPortalInvoiceRepositoryImpl(dataSource)

    @Test
    fun testSaveAndFindInvoiceSubmission() = runBlocking {
        val submission = VendorPortalInvoiceSubmission(
            submissionId = "SUB-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            purchaseOrderId = "PO-001",
            orderNumber = "PO-2026-001",
            vendorInvoiceNumber = "VINV-001",
            subtotalAmount = Money(BigDecimal("100.00")),
            totalAmount = Money(BigDecimal("100.00")),
            createdBy = "USER-01"
        )

        val saveRes = repository.saveSubmission(submission)
        assertTrue(saveRes is DomainResult.Success)

        val findRes = repository.findSubmissionById("TENANT-001", "PRJ-001", "VND-001", "SUB-01")
        assertTrue(findRes is DomainResult.Success)
        val found = (findRes as DomainResult.Success).data
        assertNotNull(found)
        assertEquals("VINV-001", found?.vendorInvoiceNumber)
    }

    @Test
    fun testSaveAndListResponsesAndEvidence() = runBlocking {
        val response = VendorPortalInvoiceResponse(
            responseId = "RESP-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            invoiceId = "INV-01",
            responseType = VendorPortalInvoiceResponseType.CLARIFY_EXCEPTION,
            comment = "Clarification notes",
            respondedBy = "USER-01"
        )

        val saveResp = repository.saveResponse(response)
        assertTrue(saveResp is DomainResult.Success)

        val listResp = repository.listResponses("TENANT-001", "PRJ-001", "VND-001", "INV-01")
        assertTrue(listResp is DomainResult.Success)
        assertEquals(1, (listResp as DomainResult.Success).data.size)

        val evidence = VendorPortalFinancialEvidence(
            evidenceId = "EV-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            entityType = "INVOICE",
            entityId = "INV-01",
            evidenceType = VendorPortalFinancialEvidenceType.INVOICE_DOCUMENT,
            filename = "tax_inv.pdf",
            fileReference = "s3://invoices/tax_inv.pdf",
            sizeBytes = 20480,
            uploadedBy = "USER-01"
        )

        val saveEv = repository.saveEvidence(evidence)
        assertTrue(saveEv is DomainResult.Success)

        val listEv = repository.listEvidence("TENANT-001", "PRJ-001", "VND-001", "INVOICE", "INV-01")
        assertTrue(listEv is DomainResult.Success)
        assertEquals(1, (listEv as DomainResult.Success).data.size)
    }
}
