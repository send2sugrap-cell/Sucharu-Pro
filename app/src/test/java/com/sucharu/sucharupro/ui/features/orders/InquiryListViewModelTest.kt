package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeInquiryDataSource
import com.sucharu.sucharupro.data.repository.InquiryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.repository.InquiryRepository
import com.sucharu.sucharupro.ui.features.orders.inquiry.InquiryListUiState
import com.sucharu.sucharupro.ui.features.orders.inquiry.InquiryListViewModel
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

class InquiryListViewModelTest {

    private lateinit var dataSource: FakeInquiryDataSource
    private lateinit var repository: InquiryRepository
    private lateinit var viewModel: InquiryListViewModel

    @Before
    fun setUp() {
        dataSource = FakeInquiryDataSource()
        repository = InquiryRepositoryImpl(dataSource)
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        viewModel = InquiryListViewModel(repository, testScope)
    }

    @Test
    fun test01_initialLoadingAndSuccessfulSampleData() = runBlocking {
        val state = viewModel.uiState.value
        assertTrue(state is InquiryListUiState.Success)
        val success = state as InquiryListUiState.Success
        assertEquals(2, success.totalCount)
        assertEquals(2, success.visibleCount)
    }

    @Test
    fun test02_searchByInquiryNumber() = runBlocking {
        viewModel.onSearchQueryChange("INQ-000001")
        val state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("INQ-000001", state.visibleInquiries[0].inquiryNumber)
    }

    @Test
    fun test03_searchByCustomerId() = runBlocking {
        viewModel.onSearchQueryChange("cus-002")
        val state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("cus-002", state.visibleInquiries[0].customerId)
    }

    @Test
    fun test04_searchByRequirementDescription() = runBlocking {
        viewModel.onSearchQueryChange("Annual Report")
        val state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Annual Report Book", state.visibleInquiries[0].items[0].productName)
    }

    @Test
    fun test05_statusFilteringWorks() = runBlocking {
        viewModel.onStatusFilterChange(InquiryStatusType.QUOTED)
        var state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals(InquiryStatusType.QUOTED, state.visibleInquiries[0].status)

        viewModel.onStatusFilterChange(InquiryStatusType.NEW)
        state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals(InquiryStatusType.NEW, state.visibleInquiries[0].status)
    }

    @Test
    fun test06_searchPlusStatusFilterCombination() = runBlocking {
        viewModel.onStatusFilterChange(InquiryStatusType.QUOTED)
        viewModel.onSearchQueryChange("Visiting Card")
        val state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("INQ-000001", state.visibleInquiries[0].inquiryNumber)
    }

    @Test
    fun test07_clearFiltersRestoresAllInquiries() = runBlocking {
        viewModel.onStatusFilterChange(InquiryStatusType.NEW)
        viewModel.onSearchQueryChange("Report")
        var state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(1, state.visibleCount)

        viewModel.clearFilters()
        state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(2, state.visibleCount)
        assertEquals("", state.searchQuery)
        assertNull(state.selectedStatus)
    }

    @Test
    fun test08_emptySearchResultDisplaysZeroVisibleCount() = runBlocking {
        viewModel.onSearchQueryChange("NonExistentInquiryXYZ")
        val state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(0, state.visibleCount)
        assertEquals(2, state.totalCount)
    }

    @Test
    fun test09_emptyDataSourceShowsEmptyState() = runBlocking {
        val emptyDataSource = FakeInquiryDataSource(initialInquiries = emptyList())
        val emptyRepo = InquiryRepositoryImpl(emptyDataSource)
        val vm = InquiryListViewModel(emptyRepo, CoroutineScope(Dispatchers.Unconfined))

        val state = vm.uiState.value
        assertTrue(state is InquiryListUiState.Empty)
    }

    @Test
    fun test10_repositoryErrorShowsErrorState() = runBlocking {
        val failingRepo = object : InquiryRepository {
            override fun getInquiries(): Flow<List<Inquiry>> = flow {
                throw RuntimeException("Database connection timeout")
            }
            override fun getInquiryById(inquiryId: String): Flow<Inquiry?> = flow { emit(null) }
            override suspend fun findInquiryById(inquiryId: String): DomainResult<Inquiry> =
                DomainResult.Error(message = "Error")
            override fun getInquiriesForCustomer(customerId: String): Flow<List<Inquiry>> = flow { emit(emptyList()) }
            override suspend fun createInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Error(message = "Error")
            override suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Error(message = "Error")
            override suspend fun updateInquiryStatus(inquiryId: String, status: InquiryStatusType): DomainResult<Inquiry> =
                DomainResult.Error(message = "Error")
            override suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit> = DomainResult.Error(message = "Error")
        }

        val vm = InquiryListViewModel(failingRepo, CoroutineScope(Dispatchers.Unconfined))
        val state = vm.uiState.value
        assertTrue(state is InquiryListUiState.Error)
        assertEquals("Database connection timeout", (state as InquiryListUiState.Error).errorMessage)
    }

    @Test
    fun test11_banglaAndUnicodeSearchSafety() = runBlocking {
        val banglaInquiry = Inquiry(
            inquiryId = "inq-bn-01",
            inquiryNumber = "INQ-বাংলা-০০১",
            customerId = "cus-003",
            status = InquiryStatusType.NEW,
            source = InquirySource.DIRECT_VISIT,
            items = listOf(
                InquiryRequirement(
                    itemId = "item-bn-1",
                    productName = "বাংলা ক্যালেন্ডার ২০২৬",
                    description = "১২ পাতার ওয়াল ক্যালেন্ডার, ১৭০ জিএসএম আর্ট পেপার",
                    quantity = 2000,
                    unit = "কপি"
                )
            ),
            notes = "জরুরি ডেলিভারি প্রয়োজন",
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )
        dataSource.insertInquiry(banglaInquiry)

        viewModel.onSearchQueryChange("ক্যালেন্ডার")
        val state = viewModel.uiState.value as InquiryListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("INQ-বাংলা-০০১", state.visibleInquiries[0].inquiryNumber)
    }
}
