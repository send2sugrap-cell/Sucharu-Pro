package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorQuotationDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorRfqDataSource
import com.sucharu.sucharupro.data.repository.VendorQuotationRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRfqRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.VendorQuotation
import com.sucharu.sucharupro.domain.model.vendorportal.VendorRfq
import com.sucharu.sucharupro.domain.service.vendorportal.VendorQuotationServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorRfqServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorRfqTenantIsolationTest {

    private lateinit var rfqService: VendorRfqServiceImpl
    private lateinit var quotationService: VendorQuotationServiceImpl
    private lateinit var rfqRepo: VendorRfqRepositoryImpl
    private lateinit var quoteRepo: VendorQuotationRepositoryImpl

    @Before
    fun setup() {
        rfqRepo = VendorRfqRepositoryImpl(FakeVendorRfqDataSource())
        quoteRepo = VendorQuotationRepositoryImpl(FakeVendorQuotationDataSource())
        val vendorRepo = com.sucharu.sucharupro.data.repository.VendorRepositoryImpl(com.sucharu.sucharupro.data.datasource.FakeVendorDataSource())
        rfqService = VendorRfqServiceImpl(rfqRepo, vendorRepo)
        quotationService = VendorQuotationServiceImpl(quoteRepo, rfqRepo)
    }

    @Test
    fun testPreventsCrossTenantAccessToRfqsAndQuotations() = runBlocking {
        val rfq = VendorRfq(
            rfqId = "rfq-tenant-A",
            tenantId = "tenant-A",
            projectId = "proj-A",
            rfqNumber = "RFQ-A-001",
            title = "Tenant A RFQ",
            requestedBy = "user-A",
            responseDeadline = System.currentTimeMillis() + 86400000L,
            createdBy = "user-A"
        )
        rfqService.createRfq(rfq, "tenant-A", "user-A")

        // Tenant B cannot retrieve Tenant A's RFQ
        val resTenantB = rfqService.getRfqById("rfq-tenant-A", "tenant-B")
        assertTrue(resTenantB is DomainResult.Error)

        // Vendor B cannot retrieve Vendor A's quotation
        val quote = VendorQuotation(
            quotationId = "q-vnd-A",
            rfqId = "rfq-tenant-A",
            invitationId = "inv-A",
            vendorId = "vnd-A",
            projectId = "proj-A",
            tenantId = "tenant-A",
            quotationNumber = "QTN-A-001",
            grandTotal = Money("5000.00"),
            createdBy = "user-A"
        )
        quoteRepo.createQuotation(quote)

        val qResVndB = quotationService.getQuotationForVendor("q-vnd-A", "vnd-B", "tenant-A")
        assertTrue(qResVndB is DomainResult.Error)
    }
}
