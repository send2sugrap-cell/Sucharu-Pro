package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.data.repository.QuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.domain.repository.QuotationRepository
import com.sucharu.sucharupro.ui.features.orders.quotation.QuotationListUiState
import com.sucharu.sucharupro.ui.features.orders.quotation.QuotationListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuotationListViewModelTest {

    private lateinit var dataSource: FakeQuotationDataSource
    private lateinit var repository: QuotationRepository
    private lateinit var viewModel: QuotationListViewModel

    @Before
    fun setUp() {
        dataSource = FakeQuotationDataSource()
        repository = QuotationRepositoryImpl(dataSource)
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        viewModel = QuotationListViewModel(repository, testScope)
    }

    @Test
    fun test01_initialLoadingAndSuccessfulSampleQuotations() = runBlocking {
        val state = viewModel.uiState.value
        assertTrue(state is QuotationListUiState.Success)
        val success = state as QuotationListUiState.Success
        assertEquals(1, success.totalCount)
        assertEquals(1, success.visibleCount)
    }

    @Test
    fun test02_searchByQuotationNumber() = runBlocking {
        viewModel.onSearchQueryChange("QT-000001")
        val state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("QT-000001", state.visibleQuotations[0].quotationNumber)
    }

    @Test
    fun test03_searchByCustomerId() = runBlocking {
        viewModel.onSearchQueryChange("cus-001")
        val state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("cus-001", state.visibleQuotations[0].customerId)
    }

    @Test
    fun test04_searchByInquiryId() = runBlocking {
        viewModel.onSearchQueryChange("inq-001")
        val state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("inq-001", state.visibleQuotations[0].inquiryId)
    }

    @Test
    fun test05_searchByItemDescription() = runBlocking {
        viewModel.onSearchQueryChange("Spot UV")
        val state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("QT-000001", state.visibleQuotations[0].quotationNumber)
    }

    @Test
    fun test06_statusFilteringWorks() = runBlocking {
        viewModel.onStatusFilterChange(QuotationStatusType.APPROVED)
        var state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(1, state.visibleCount)

        viewModel.onStatusFilterChange(QuotationStatusType.DRAFT)
        state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(0, state.visibleCount)
    }

    @Test
    fun test07_searchPlusStatusFilterCombination() = runBlocking {
        viewModel.onStatusFilterChange(QuotationStatusType.APPROVED)
        viewModel.onSearchQueryChange("Visiting Card")
        val state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("QT-000001", state.visibleQuotations[0].quotationNumber)
    }

    @Test
    fun test08_clearFiltersRestoresAllQuotations() = runBlocking {
        viewModel.onStatusFilterChange(QuotationStatusType.DRAFT)
        viewModel.onSearchQueryChange("NonExistent")
        var state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(0, state.visibleCount)

        viewModel.clearFilters()
        state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("", state.searchQuery)
        assertNull(state.selectedStatus)
    }

    @Test
    fun test09_emptySearchResultDisplaysZeroVisibleCount() = runBlocking {
        viewModel.onSearchQueryChange("XYZ999Unknown")
        val state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(0, state.visibleCount)
        assertEquals(1, state.totalCount)
    }

    @Test
    fun test10_emptyDataSourceShowsEmptyState() = runBlocking {
        val emptyDataSource = FakeQuotationDataSource(initialQuotations = emptyList())
        val emptyRepo = QuotationRepositoryImpl(emptyDataSource)
        val vm = QuotationListViewModel(emptyRepo, CoroutineScope(Dispatchers.Unconfined))

        val state = vm.uiState.value
        assertTrue(state is QuotationListUiState.Empty)
    }

    @Test
    fun test11_repositoryErrorShowsErrorState() = runBlocking {
        val failingRepo = object : QuotationRepository {
            override fun getQuotations(): Flow<List<Quotation>> = flow {
                throw RuntimeException("Network request failed")
            }
            override fun getQuotationById(quotationId: String): Flow<Quotation?> = flow { emit(null) }
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

        val vm = QuotationListViewModel(failingRepo, CoroutineScope(Dispatchers.Unconfined))
        val state = vm.uiState.value
        assertTrue(state is QuotationListUiState.Error)
        assertEquals("Network request failed", (state as QuotationListUiState.Error).errorMessage)
    }

    @Test
    fun test12_revisionAndMoneyPresentationIntegrity() = runBlocking {
        val state = viewModel.uiState.value as QuotationListUiState.Success
        val q = state.visibleQuotations[0]
        assertEquals(2, q.currentRevisionNumber)
        assertEquals(2, q.revisionCount)
        assertEquals(1100.toMoney(), q.totalAmount)
        assertEquals("৳ 1,100", q.totalAmount.formatted())
    }

    @Test
    fun test13_banglaUnicodeSearchInQuotations() = runBlocking {
        val banglaRev = QuotationRevision(
            revisionId = "rev-bn-01",
            quotationId = "qt-bn-01",
            revisionNumber = 1,
            items = listOf(
                QuotationItem(
                    itemId = "item-bn-01",
                    description = "বই প্রিন্টিং (চার কালার)",
                    specification = "ডিমাই সাইজ, ১২০ পৃষ্ঠা",
                    quantity = 1000,
                    unit = "কপি",
                    unitPrice = 150.toMoney(),
                    discount = Money.ZERO
                )
            ),
            discount = Money.ZERO,
            paymentTerms = PaymentTerms.DEFAULT,
            deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
            revisionReason = "বাংলা বই মুদ্রণ প্রাক্কলন",
            createdAt = "2026-08-15T12:00:00Z"
        )
        val banglaQuotation = Quotation(
            quotationId = "qt-bn-01",
            quotationNumber = "QT-বাংলা-০১",
            customerId = "cus-003",
            currentRevisionNumber = 1,
            revisions = listOf(banglaRev),
            status = QuotationStatusType.DRAFT,
            createdAt = "2026-08-15T12:00:00Z",
            updatedAt = "2026-08-15T12:00:00Z"
        )
        dataSource.insertQuotation(banglaQuotation)

        viewModel.onSearchQueryChange("মুদ্রণ")
        val state = viewModel.uiState.value as QuotationListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("QT-বাংলা-০১", state.visibleQuotations[0].quotationNumber)
    }
}
