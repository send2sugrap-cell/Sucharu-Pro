package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuotationRepositoryTest {

    private lateinit var dataSource: FakeQuotationDataSource
    private lateinit var repository: QuotationRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeQuotationDataSource()
        repository = QuotationRepositoryImpl(dataSource)
    }

    @Test
    fun test01_createQuotation_success() = runBlocking {
        val rev1 = QuotationRevision(
            revisionId = "rev-new-v1",
            quotationId = "qt-new-01",
            revisionNumber = 1,
            items = listOf(
                QuotationItem(
                    itemId = "item-1",
                    description = "A4 Poster 150 GSM",
                    quantity = 500,
                    unitPrice = 12.0.toMoney()
                )
            ),
            createdAt = "2026-08-15T10:00:00Z"
        )

        val quotation = Quotation(
            quotationId = "qt-new-01",
            quotationNumber = "QT-999001",
            customerId = "cus-002",
            inquiryId = "inq-002",
            currentRevisionNumber = 1,
            revisions = listOf(rev1),
            status = QuotationStatusType.DRAFT,
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )

        val result = repository.createQuotation(quotation)
        assertTrue(result.isSuccess)
        val created = (result as DomainResult.Success).data
        assertEquals("qt-new-01", created.quotationId)
        assertEquals("৳ 6,000", created.totalAmount.formatted())
    }

    @Test
    fun test02_duplicateQuotationId_rejected() = runBlocking {
        val duplicate = Quotation(
            quotationId = "qt-001", // already exists
            quotationNumber = "QT-UNIQUE-999",
            customerId = "cus-001",
            revisions = listOf(
                QuotationRevision(
                    revisionId = "rev-dup",
                    quotationId = "qt-001",
                    revisionNumber = 1,
                    items = listOf(QuotationItem("it-1", "Test", quantity = 10, unitPrice = 10.toMoney())),
                    createdAt = "2026-08-15T10:00:00Z"
                )
            ),
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )

        val result = repository.createQuotation(duplicate)
        assertTrue(result.isError)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun test03_getQuotation_success() = runBlocking {
        val quotation = repository.getQuotationById("qt-001").first()
        assertNotNull(quotation)
        assertEquals("QT-000001", quotation?.quotationNumber)
        assertEquals(2, quotation?.revisionCount)
        assertEquals(QuotationStatusType.APPROVED, quotation?.status)
    }

    @Test
    fun test04_getQuotationsForCustomer_isolated() = runBlocking {
        val cus1Quotations = repository.getQuotationsForCustomer("cus-001").first()
        val cus2Quotations = repository.getQuotationsForCustomer("cus-002").first()

        assertTrue(cus1Quotations.all { it.customerId == "cus-001" })
        assertTrue(cus2Quotations.isEmpty())
    }

    @Test
    fun test05_getQuotationsForInquiry_success() = runBlocking {
        val inquiryQuotations = repository.getQuotationsForInquiry("inq-001").first()
        assertEquals(1, inquiryQuotations.size)
        assertEquals("qt-001", inquiryQuotations.first().quotationId)
    }

    @Test
    fun test06_createRevision_preservesHistoricalIdentity() = runBlocking {
        val rev3 = QuotationRevision(
            revisionId = "rev-001-v3",
            quotationId = "qt-001",
            revisionNumber = 3,
            items = listOf(
                QuotationItem(
                    itemId = "qt-item-01",
                    description = "Visiting Card (300 GSM Art Card, Matte + Gold Foil)",
                    specification = "3.25x2.0 in, 4/4 Color + Gold Foil, 2000 Pcs",
                    quantity = 2000,
                    unit = "Pcs",
                    unitPrice = 1.50.toMoney()
                )
            ),
            revisionReason = "Customer upgraded to Gold Foil and increased quantity to 2000",
            createdAt = "2026-08-15T15:00:00Z",
            previousRevisionId = "rev-001-v2"
        )

        val result = repository.createQuotationRevision("qt-001", rev3)
        assertTrue(result.isSuccess)

        // Verify all 3 revisions exist and are historically identifiable
        val quotation = (repository.findQuotationById("qt-001") as DomainResult.Success).data
        assertEquals(3, quotation.revisionCount)
        assertEquals(3, quotation.currentRevisionNumber)
        assertEquals("rev-001-v3", quotation.currentRevision?.revisionId)
        assertEquals("৳ 3,000", quotation.totalAmount.formatted())

        // Verify V1 and V2 are still intact
        val v1 = quotation.revisions.find { it.revisionNumber == 1 }
        val v2 = quotation.revisions.find { it.revisionNumber == 2 }
        assertNotNull(v1)
        assertNotNull(v2)
        assertEquals("rev-001-v1", v1?.revisionId)
        assertEquals("rev-001-v2", v2?.revisionId)
    }

    @Test
    fun test07_approveRevision_success() = runBlocking {
        // Create a draft quotation
        val draftRev = QuotationRevision(
            revisionId = "rev-draft-v1",
            quotationId = "qt-draft-01",
            revisionNumber = 1,
            items = listOf(
                QuotationItem(
                    itemId = "it-1",
                    description = "Brochures",
                    quantity = 1000,
                    unitPrice = 15.toMoney()
                )
            ),
            createdAt = "2026-08-15T10:00:00Z"
        )

        repository.createQuotation(
            Quotation(
                quotationId = "qt-draft-01",
                quotationNumber = "QT-DRAFT-01",
                customerId = "cus-002",
                revisions = listOf(draftRev),
                status = QuotationStatusType.DRAFT,
                createdAt = "2026-08-15T10:00:00Z",
                updatedAt = "2026-08-15T10:00:00Z"
            )
        )

        // Formally approve revision 1
        val approveResult = repository.approveQuotationRevision(
            quotationId = "qt-draft-01",
            revisionId = "rev-draft-v1",
            approvedBy = "Customer Representative",
            timestamp = "2026-08-15T16:00:00Z"
        )

        assertTrue(approveResult.isSuccess)
        val approvedQuotation = (approveResult as DomainResult.Success).data
        assertEquals(QuotationStatusType.APPROVED, approvedQuotation.status)
        assertEquals("rev-draft-v1", approvedQuotation.approvedRevisionId)
        assertEquals("Customer Representative", approvedQuotation.approvedBy)
        assertEquals("2026-08-15T16:00:00Z", approvedQuotation.approvedAt)
        assertTrue(approvedQuotation.isApproved)
    }

    @Test
    fun test08_approveRevision_wrongQuotation_rejected() = runBlocking {
        val result = repository.approveQuotationRevision(
            quotationId = "qt-001",
            revisionId = "rev-NON-EXISTENT",
            approvedBy = "Manager",
            timestamp = "2026-08-15T16:00:00Z"
        )

        assertTrue(result.isError)
        assertTrue((result as DomainResult.Error).message.contains("does not belong"))
    }

    @Test
    fun test09_deleteQuotation_approvedQuotation_rejected() = runBlocking {
        // qt-001 is APPROVED -> deletion must be rejected
        val result = repository.deleteQuotation("qt-001")
        assertTrue(result.isError)
        assertTrue((result as DomainResult.Error).message.contains("Cannot delete an approved quotation"))
    }
}
