package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorQuotationDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorRfqDataSource
import com.sucharu.sucharupro.data.repository.VendorQuotationRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRfqRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorQuotationServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorQuotationServiceTest {

    private lateinit var quotationService: VendorQuotationServiceImpl
    private lateinit var quotationRepository: VendorQuotationRepositoryImpl
    private lateinit var rfqRepository: VendorRfqRepositoryImpl

    @Before
    fun setup() {
        quotationRepository = VendorQuotationRepositoryImpl(FakeVendorQuotationDataSource())
        rfqRepository = VendorRfqRepositoryImpl(FakeVendorRfqDataSource())
        quotationService = VendorQuotationServiceImpl(quotationRepository, rfqRepository)
    }

    @Test
    fun testCreatesDraftQuotationAndSubmitsSuccessfully() = runBlocking {
        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "proj-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Paper Supply",
            requestedBy = "user-1",
            status = VendorRfqStatus.OPEN,
            responseDeadline = System.currentTimeMillis() + 86400000L,
            items = listOf(
                VendorRfqItem(
                    rfqItemId = "item-1",
                    rfqId = "rfq-1",
                    sequenceNumber = 1,
                    description = "Paper A4",
                    quantity = BigDecimal("100.00")
                )
            ),
            createdBy = "user-1"
        )
        rfqRepository.createRfq(rfq)
        rfqRepository.createInvitation(
            VendorRfqInvitation(
                invitationId = "inv-1",
                rfqId = "rfq-1",
                vendorId = "vnd-1",
                projectId = "proj-1",
                tenantId = "proj-1"
            )
        )

        val quotation = VendorQuotation(
            quotationId = "q-1",
            rfqId = "rfq-1",
            invitationId = "inv-1",
            vendorId = "vnd-1",
            projectId = "proj-1",
            tenantId = "proj-1",
            quotationNumber = "QTN-001",
            items = listOf(
                VendorQuotationItem(
                    quotationItemId = "qi-1",
                    quotationId = "q-1",
                    rfqItemId = "item-1",
                    quantity = BigDecimal("100.00"),
                    unitPrice = Money("10.00"),
                    lineTotal = Money.ZERO
                )
            ),
            createdBy = "vnd-user"
        )

        val draftRes = quotationService.createQuotationDraft(quotation, "proj-1", "vnd-user")
        assertTrue(draftRes is DomainResult.Success)
        assertEquals(Money("1000.00"), (draftRes as DomainResult.Success).data.grandTotal)

        val submitRes = quotationService.submitQuotation("q-1", "proj-1", "vnd-user")
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(VendorQuotationStatus.SUBMITTED, (submitRes as DomainResult.Success).data.status)
    }

    @Test
    fun testRevisesSubmittedQuotationWithVersionBumpAndSnapshotRetention() = runBlocking {
        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "proj-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Paper Supply",
            requestedBy = "user-1",
            status = VendorRfqStatus.OPEN,
            responseDeadline = System.currentTimeMillis() + 86400000L,
            items = listOf(
                VendorRfqItem(
                    rfqItemId = "item-1",
                    rfqId = "rfq-1",
                    sequenceNumber = 1,
                    description = "Paper A4",
                    quantity = BigDecimal("100.00")
                )
            ),
            createdBy = "user-1"
        )
        rfqRepository.createRfq(rfq)
        rfqRepository.createInvitation(
            VendorRfqInvitation(
                invitationId = "inv-1",
                rfqId = "rfq-1",
                vendorId = "vnd-1",
                projectId = "proj-1",
                tenantId = "proj-1"
            )
        )

        val quotation = VendorQuotation(
            quotationId = "q-1",
            rfqId = "rfq-1",
            invitationId = "inv-1",
            vendorId = "vnd-1",
            projectId = "proj-1",
            tenantId = "proj-1",
            quotationNumber = "QTN-001",
            items = listOf(
                VendorQuotationItem(
                    quotationItemId = "qi-1",
                    quotationId = "q-1",
                    rfqItemId = "item-1",
                    quantity = BigDecimal("100.00"),
                    unitPrice = Money("10.00"),
                    lineTotal = Money.ZERO
                )
            ),
            createdBy = "vnd-user"
        )
        quotationService.createQuotationDraft(quotation, "proj-1", "vnd-user")
        quotationService.submitQuotation("q-1", "proj-1", "vnd-user")

        val revisedQuote = quotation.copy(
            items = listOf(
                VendorQuotationItem(
                    quotationItemId = "qi-1",
                    quotationId = "q-1",
                    rfqItemId = "item-1",
                    quantity = BigDecimal("100.00"),
                    unitPrice = Money("9.50"),
                    lineTotal = Money.ZERO
                )
            )
        )

        val revRes = quotationService.submitRevision("q-1", revisedQuote, "Special volume discount", "proj-1", "vnd-user")
        assertTrue(revRes is DomainResult.Success)
        val data = (revRes as DomainResult.Success).data
        assertEquals(2, data.revisionNumber)
        assertEquals(Money("950.00"), data.grandTotal)
        assertEquals(VendorQuotationStatus.REVISED, data.status)

        val revList = (quotationService.listRevisions("q-1", "proj-1") as DomainResult.Success).data
        assertEquals(1, revList.size)
        assertEquals(1, revList[0].revisionNumber)
        assertEquals(Money("1000.00"), revList[0].snapshotGrandTotal)
    }
}
