package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.data.repository.QuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.PaymentTermType
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
import java.time.Instant

class QuotationLifecycleIntegrationTest {

    private lateinit var dataSource: FakeQuotationDataSource
    private lateinit var repository: QuotationRepositoryImpl

    private val sampleRev1 = QuotationRevision(
        revisionId = "rev-001-v1",
        quotationId = "qt-100",
        revisionNumber = 1,
        items = listOf(
            QuotationItem(
                itemId = "item-01",
                description = "বই প্রিন্টিং (৩০০ জিএসএম আর্ট কার্ড)",
                specification = "A4, 4/4 কালার",
                quantity = 1000,
                unit = "Pcs",
                unitPrice = Money(250.0),
                discount = Money(5000.0)
            )
        ),
        discount = Money(2000.0),
        paymentTerms = PaymentTerms(type = PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 50),
        deliveryRequirement = DeliveryRequirement(deliveryType = DeliveryType.BUSINESS_DELIVERY, address = "ঢাকা প্রেস"),
        revisionReason = "Initial Estimate",
        createdAt = "2026-08-16T10:00:00Z",
        createdBy = "Estimator"
    )

    private val initialDraftQuotation = Quotation(
        quotationId = "qt-100",
        quotationNumber = "QT-2026-000100",
        customerId = "cus-001",
        inquiryId = "inq-001",
        currentRevisionNumber = 1,
        revisions = listOf(sampleRev1),
        status = QuotationStatusType.DRAFT,
        validUntil = "2026-09-16",
        termsAndConditions = "ডেলিভারি অগ্রিম গ্রহণের ৭ দিনের মধ্যে।",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeQuotationDataSource(listOf(initialDraftQuotation))
        repository = QuotationRepositoryImpl(dataSource)
    }

    @Test
    fun sendLifecycle_draftToSent_succeedsAndPreservesHistory() = runBlocking {
        val result = repository.updateQuotationStatus("qt-100", QuotationStatusType.SENT)
        assertTrue(result is DomainResult.Success)

        val updated = repository.getQuotationById("qt-100").first()
        assertNotNull(updated)
        assertEquals(QuotationStatusType.SENT, updated?.status)
        assertEquals("QT-2026-000100", updated?.quotationNumber)
        assertEquals(1, updated?.revisions?.size)
        assertEquals(Money(243000.0), updated?.totalAmount)
    }

    @Test
    fun sendLifecycle_invalidTransition_failsGracefully() = runBlocking {
        // First set to APPROVED
        repository.approveQuotationRevision("qt-100", "rev-001-v1", "Manager", "2026-08-16T12:00:00Z")

        // Attempt transition from APPROVED to SENT is invalid
        val result = repository.updateQuotationStatus("qt-100", QuotationStatusType.SENT)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun negotiationLifecycle_sentToNegotiation_succeeds() = runBlocking {
        repository.updateQuotationStatus("qt-100", QuotationStatusType.SENT)
        val result = repository.updateQuotationStatus("qt-100", QuotationStatusType.NEGOTIATION)
        assertTrue(result is DomainResult.Success)

        val quotation = repository.getQuotationById("qt-100").first()
        assertEquals(QuotationStatusType.NEGOTIATION, quotation?.status)
    }

    @Test
    fun revisionManagement_createV2FromV1_preservesV1ImmutablyAndSetsV2Current() = runBlocking {
        repository.updateQuotationStatus("qt-100", QuotationStatusType.SENT)
        repository.updateQuotationStatus("qt-100", QuotationStatusType.NEGOTIATION)

        val rev2 = QuotationRevision(
            revisionId = "rev-001-v2",
            quotationId = "qt-100",
            revisionNumber = 2,
            items = listOf(
                QuotationItem(
                    itemId = "item-01",
                    description = "বই প্রিন্টিং (৩০০ জিএসএম আর্ট কার্ড + ম্যাট ল্যামিনেশন)",
                    specification = "A4, 4/4 কালার + ম্যাট",
                    quantity = 2000, // Quantity doubled
                    unit = "Pcs",
                    unitPrice = Money(220.0), // Discounted unit price
                    discount = Money(10000.0)
                )
            ),
            discount = Money(5000.0),
            paymentTerms = PaymentTerms(type = PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 40),
            deliveryRequirement = sampleRev1.deliveryRequirement,
            revisionReason = "গ্রাহক ২০০০ কপির জন্য বিশেষ রেট চেয়েছেন।",
            createdAt = "2026-08-16T14:00:00Z",
            createdBy = "Sales Lead",
            previousRevisionId = "rev-001-v1"
        )

        val addResult = repository.createQuotationRevision("qt-100", rev2)
        assertTrue(addResult is DomainResult.Success)

        val quotation = repository.getQuotationById("qt-100").first()
        assertNotNull(quotation)
        assertEquals(2, quotation?.currentRevisionNumber)
        assertEquals(2, quotation?.revisions?.size)

        // Verify V1 remains unchanged (immutable)
        val v1 = quotation?.revisions?.find { it.revisionNumber == 1 }
        assertNotNull(v1)
        assertEquals(1000, v1?.items?.first()?.quantity)
        assertEquals(Money(250.0), v1?.items?.first()?.unitPrice)
        assertEquals(Money(243000.0), v1?.totalAmount)

        // Verify V2 is current active revision
        val currentRev = quotation?.currentRevision
        assertNotNull(currentRev)
        assertEquals(2, currentRev?.revisionNumber)
        assertEquals(2000, currentRev?.items?.first()?.quantity)
        assertEquals(Money(220.0), currentRev?.items?.first()?.unitPrice)
        // Subtotal = 2000 * 220 - 10,000 = 430,000; Total = 430,000 - 5,000 = 425,000
        assertEquals(Money(425000.0), currentRev?.totalAmount)
        assertEquals("গ্রাহক ২০০০ কপির জন্য বিশেষ রেট চেয়েছেন।", currentRev?.revisionReason)
    }

    @Test
    fun approvalLifecycle_approvesRevisionAndRecordsMetadata() = runBlocking {
        repository.updateQuotationStatus("qt-100", QuotationStatusType.SENT)
        repository.updateQuotationStatus("qt-100", QuotationStatusType.NEGOTIATION)

        val timestamp = "2026-08-16T15:30:00Z"
        val result = repository.approveQuotationRevision(
            quotationId = "qt-100",
            revisionId = "rev-001-v1",
            approvedBy = "Md. Rafiq (Commercial Manager)",
            timestamp = timestamp
        )

        assertTrue(result is DomainResult.Success)
        val quotation = (result as DomainResult.Success).data
        assertEquals(QuotationStatusType.APPROVED, quotation.status)
        assertTrue(quotation.isApproved)
        assertEquals("rev-001-v1", quotation.approvedRevisionId)
        assertEquals("Md. Rafiq (Commercial Manager)", quotation.approvedBy)
        assertEquals(timestamp, quotation.approvedAt)
    }

    @Test
    fun approvalLifecycle_cannotApproveNonExistentRevision() = runBlocking {
        val result = repository.approveQuotationRevision(
            quotationId = "qt-100",
            revisionId = "rev-non-existent",
            approvedBy = "Manager",
            timestamp = "2026-08-16T15:00:00Z"
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun rejectionLifecycle_rejectsAndPreservesHistory() = runBlocking {
        repository.updateQuotationStatus("qt-100", QuotationStatusType.SENT)
        val result = repository.updateQuotationStatus("qt-100", QuotationStatusType.REJECTED)
        assertTrue(result is DomainResult.Success)

        val quotation = repository.getQuotationById("qt-100").first()
        assertEquals(QuotationStatusType.REJECTED, quotation?.status)
        assertEquals(1, quotation?.revisions?.size)
        assertFalse(quotation?.isApproved ?: true)
    }

    @Test
    fun cancellationLifecycle_cancelsAndProtectsFromApproval() = runBlocking {
        val result = repository.updateQuotationStatus("qt-100", QuotationStatusType.CANCELLED)
        assertTrue(result is DomainResult.Success)

        val quotation = repository.getQuotationById("qt-100").first()
        assertEquals(QuotationStatusType.CANCELLED, quotation?.status)

        // Cancelled cannot transition to APPROVED
        assertFalse(quotation!!.status.canTransitionTo(QuotationStatusType.APPROVED))
    }
}
