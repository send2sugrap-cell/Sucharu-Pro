package com.sucharu.sucharupro.ui.features.orders

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
import com.sucharu.sucharupro.domain.repository.QuotationRepository
import com.sucharu.sucharupro.ui.features.orders.quotation.details.QuotationDetailsUiState
import com.sucharu.sucharupro.ui.features.orders.quotation.details.QuotationDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotationDetailsViewModelTest {

    private fun sampleQuotation(id: String = "quo-101"): Quotation {
        val rev1Items = listOf(
            QuotationItem(
                itemId = "item-01",
                description = "বই প্রিন্টিং (Initial)",
                specification = "A4, 80 GSM offset paper, 4 Color cover",
                quantity = 1000,
                unit = "copies",
                unitPrice = Money(120.0),
                discount = Money.ZERO
            )
        )
        val rev2Items = listOf(
            QuotationItem(
                itemId = "item-01",
                description = "বই প্রিন্টিং (Revised)",
                specification = "A4, 100 GSM Art paper, Matt Lamination",
                quantity = 1000,
                unit = "copies",
                unitPrice = Money(150.0),
                discount = Money(5000.0)
            )
        )

        val rev1 = QuotationRevision(
            revisionId = "rev-001",
            quotationId = id,
            revisionNumber = 1,
            items = rev1Items,
            discount = Money.ZERO,
            paymentTerms = PaymentTerms(PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 50),
            deliveryRequirement = DeliveryRequirement(DeliveryType.BUSINESS_DELIVERY),
            createdAt = "2026-08-16T10:00:00Z",
            createdBy = "Commercial Desk",
            revisionReason = "Initial quotation"
        )
        val rev2 = QuotationRevision(
            revisionId = "rev-002",
            quotationId = id,
            revisionNumber = 2,
            items = rev2Items,
            discount = Money.ZERO,
            paymentTerms = PaymentTerms(PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 50),
            deliveryRequirement = DeliveryRequirement(DeliveryType.BUSINESS_DELIVERY),
            createdAt = "2026-08-16T11:00:00Z",
            createdBy = "Sales Manager",
            revisionReason = "Customer requested heavier 100 GSM paper"
        )

        return Quotation(
            quotationId = id,
            quotationNumber = "QUO-2026-0001",
            customerId = "cus-001",
            inquiryId = "inq-101",
            status = QuotationStatusType.APPROVED,
            currentRevisionNumber = 2,
            revisions = listOf(rev1, rev2),
            approvedRevisionId = "rev-002",
            approvedBy = "Customer Auth Signatory",
            approvedAt = "2026-08-16T12:00:00Z",
            validUntil = "2026-09-16T12:00:00Z",
            termsAndConditions = "Standard commercial terms apply.",
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T12:00:00Z"
        )
    }

    private fun createFakeRepo(quotation: Quotation?): QuotationRepository = object : QuotationRepository {
        override fun getQuotations(): Flow<List<Quotation>> = flowOf(listOfNotNull(quotation))
        override fun getQuotationById(quotationId: String): Flow<Quotation?> = flowOf(if (quotation?.quotationId == quotationId) quotation else null)
        override suspend fun findQuotationById(quotationId: String): DomainResult<Quotation> =
            if (quotation?.quotationId == quotationId) DomainResult.Success(quotation) else DomainResult.Error(message = "Not found")
        override fun getQuotationsForCustomer(customerId: String): Flow<List<Quotation>> = flowOf(listOfNotNull(quotation))
        override fun getQuotationsForInquiry(inquiryId: String): Flow<List<Quotation>> = flowOf(listOfNotNull(quotation))
        override suspend fun createQuotation(quotation: Quotation): DomainResult<Quotation> = DomainResult.Success(quotation)
        override suspend fun updateQuotation(quotation: Quotation): DomainResult<Quotation> = DomainResult.Success(quotation)
        override suspend fun updateQuotationStatus(quotationId: String, status: QuotationStatusType): DomainResult<Quotation> =
            quotation?.let { DomainResult.Success(it.copy(status = status)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun deleteQuotation(quotationId: String): DomainResult<Unit> = DomainResult.Success(Unit)
        override fun getQuotationRevisions(quotationId: String): Flow<List<QuotationRevision>> = flowOf(quotation?.revisions ?: emptyList())
        override suspend fun findQuotationRevision(quotationId: String, revisionId: String): DomainResult<QuotationRevision> =
            quotation?.revisions?.find { it.revisionId == revisionId }?.let { DomainResult.Success(it) } ?: DomainResult.Error(message = "Not found")
        override suspend fun createQuotationRevision(quotationId: String, revision: QuotationRevision): DomainResult<QuotationRevision> =
            DomainResult.Success(revision)
        override suspend fun getLatestQuotationRevision(quotationId: String): QuotationRevision? = quotation?.currentRevision
        override suspend fun getApprovedQuotationRevision(quotationId: String): QuotationRevision? =
            quotation?.revisions?.find { it.revisionId == quotation.approvedRevisionId }
        override suspend fun approveQuotationRevision(
            quotationId: String,
            revisionId: String,
            approvedBy: String,
            timestamp: String
        ): DomainResult<Quotation> = quotation?.let { DomainResult.Success(it) } ?: DomainResult.Error(message = "Not found")
    }

    @Test
    fun loadQuotation_successfulLoad_emitsSuccessWithFullData() {
        val quotation = sampleQuotation("quo-101")
        val repo = createFakeRepo(quotation)
        val vm = QuotationDetailsViewModel(
            quotationId = "quo-101",
            repository = repo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue("State should be Success, got $state", state is QuotationDetailsUiState.Success)
        val successState = state as QuotationDetailsUiState.Success
        assertEquals("quo-101", successState.quotation.quotationId)
        assertEquals("QUO-2026-0001", successState.quotation.quotationNumber)
        assertEquals("cus-001", successState.quotation.customerId)
        assertEquals("inq-101", successState.quotation.inquiryId)
        assertEquals(2, successState.revisions.size)
        assertEquals(Money(145000.0), successState.quotation.totalAmount)
        assertTrue(successState.quotation.isApproved)
        assertEquals("Customer Auth Signatory", successState.quotation.approvedBy)
    }

    @Test
    fun loadQuotation_selectHistoricalRevision_updatesActiveRevisionSnapshot() {
        val quotation = sampleQuotation("quo-101")
        val repo = createFakeRepo(quotation)
        val vm = QuotationDetailsViewModel(
            quotationId = "quo-101",
            repository = repo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val initialState = vm.uiState.value as QuotationDetailsUiState.Success
        assertEquals(2, initialState.activeRevision?.revisionNumber)
        assertEquals(Money(145000.0), initialState.activeRevision?.totalAmount)
        assertFalse(initialState.isViewingHistoricalRevision)

        // Select historical revision #1
        vm.selectRevision("rev-001")
        val rev1State = vm.uiState.value as QuotationDetailsUiState.Success
        assertEquals(1, rev1State.activeRevision?.revisionNumber)
        assertEquals(Money(120000.0), rev1State.activeRevision?.totalAmount)
        assertTrue(rev1State.isViewingHistoricalRevision)
    }

    @Test
    fun loadQuotation_notFound_emitsNotFoundState() {
        val repo = createFakeRepo(null)
        val vm = QuotationDetailsViewModel(
            quotationId = "quo-unknown",
            repository = repo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue("State should be NotFound, got $state", state is QuotationDetailsUiState.NotFound)
        assertEquals("quo-unknown", (state as QuotationDetailsUiState.NotFound).quotationId)
    }

    @Test
    fun loadQuotation_error_emitsErrorState() {
        val failingRepo = object : QuotationRepository {
            override fun getQuotations(): Flow<List<Quotation>> = flow { emit(emptyList()) }
            override fun getQuotationById(quotationId: String): Flow<Quotation?> = flow {
                throw RuntimeException("Network error")
            }
            override suspend fun findQuotationById(quotationId: String): DomainResult<Quotation> = DomainResult.Error(message = "Error")
            override fun getQuotationsForCustomer(customerId: String): Flow<List<Quotation>> = flow { emit(emptyList()) }
            override fun getQuotationsForInquiry(inquiryId: String): Flow<List<Quotation>> = flow { emit(emptyList()) }
            override suspend fun createQuotation(quotation: Quotation): DomainResult<Quotation> = DomainResult.Error(message = "Error")
            override suspend fun updateQuotation(quotation: Quotation): DomainResult<Quotation> = DomainResult.Error(message = "Error")
            override suspend fun updateQuotationStatus(quotationId: String, status: QuotationStatusType): DomainResult<Quotation> =
                DomainResult.Error(message = "Error")
            override suspend fun deleteQuotation(quotationId: String): DomainResult<Unit> = DomainResult.Error(message = "Error")
            override fun getQuotationRevisions(quotationId: String): Flow<List<QuotationRevision>> = flow { emit(emptyList()) }
            override suspend fun findQuotationRevision(quotationId: String, revisionId: String): DomainResult<QuotationRevision> =
                DomainResult.Error(message = "Error")
            override suspend fun createQuotationRevision(quotationId: String, revision: QuotationRevision): DomainResult<QuotationRevision> =
                DomainResult.Error(message = "Error")
            override suspend fun getLatestQuotationRevision(quotationId: String): QuotationRevision? = null
            override suspend fun getApprovedQuotationRevision(quotationId: String): QuotationRevision? = null
            override suspend fun approveQuotationRevision(
                quotationId: String,
                revisionId: String,
                approvedBy: String,
                timestamp: String
            ): DomainResult<Quotation> = DomainResult.Error(message = "Error")
        }

        val vm = QuotationDetailsViewModel(
            quotationId = "quo-101",
            repository = failingRepo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue("State should be Error, got $state", state is QuotationDetailsUiState.Error)
        assertEquals("Network error", (state as QuotationDetailsUiState.Error).errorMessage)
    }
}
