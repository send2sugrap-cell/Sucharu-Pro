package com.sucharu.sucharupro.ui.features.customer

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomerListViewModelTest {

    private lateinit var viewModel: CustomerListViewModel

    @Before
    fun setUp() {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        viewModel = CustomerListViewModel(repository, testScope)
    }

    @Test
    fun test01_loadsSampleCustomersInitially() = runBlocking {
        val state = viewModel.uiState.value
        assertTrue(state is CustomerListUiState.Success)
        val success = state as CustomerListUiState.Success
        assertEquals(10, success.totalCount)
        assertEquals(10, success.visibleCount)
    }

    @Test
    fun test02_searchFiltersByDisplayName() = runBlocking {
        viewModel.onSearchQueryChange("Abdullah")
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Md. Abdullah Rahman", state.visibleCustomers[0].displayName)
    }

    @Test
    fun test03_searchFiltersByPhone() = runBlocking {
        viewModel.onSearchQueryChange("1912-345678")
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Bengal Publications Ltd.", state.visibleCustomers[0].displayName)
    }

    @Test
    fun test04_searchFiltersByCustomerCode() = runBlocking {
        viewModel.onSearchQueryChange("CUS-000003")
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Al-Noor Printing & Book Agency", state.visibleCustomers[0].displayName)
    }

    @Test
    fun test05_customerTypeFilterWorks() = runBlocking {
        viewModel.onTypeFilterChange(CustomerType.DEALER)
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(2, state.visibleCount)
        assertTrue(state.visibleCustomers.all { it.customerType == CustomerType.DEALER })
    }

    @Test
    fun test06_customerStatusFilterWorks() = runBlocking {
        viewModel.onStatusFilterChange(CustomerStatusType.BLOCKED)
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Prime Media & Design Studio", state.visibleCustomers[0].displayName)
    }

    @Test
    fun test07_searchAndTypeFilterCombinationWorks() = runBlocking {
        viewModel.onTypeFilterChange(CustomerType.DEALER)
        viewModel.onSearchQueryChange("Bangla")
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Al-Noor Printing & Book Agency", state.visibleCustomers[0].displayName)
    }

    @Test
    fun test08_searchAndStatusFilterCombinationWorks() = runBlocking {
        viewModel.onStatusFilterChange(CustomerStatusType.ACTIVE)
        viewModel.onSearchQueryChange("Madrasa")
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Darul Uloom Madrasa & Orphanage", state.visibleCustomers[0].displayName)
    }

    @Test
    fun test09_clearFiltersRestoresAllCustomers() = runBlocking {
        viewModel.onTypeFilterChange(CustomerType.INSTITUTION)
        viewModel.onSearchQueryChange("Nazrul")
        var state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)

        viewModel.clearFilters()
        state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(10, state.visibleCount)
        assertEquals("", state.searchQuery)
        assertNull(state.selectedType)
        assertNull(state.selectedStatus)
    }

    @Test
    fun test10_emptySearchResultDisplaysZeroVisibleCount() = runBlocking {
        viewModel.onSearchQueryChange("NonExistentCustomerNameXYZ")
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(0, state.visibleCount)
        assertEquals(10, state.totalCount)
    }

    @Test
    fun test11_refreshWorks() = runBlocking {
        viewModel.refresh()
        val state = viewModel.uiState.value
        assertTrue(state is CustomerListUiState.Success)
    }

    @Test
    fun test12_vipAndGovernmentTypeFiltersWork() = runBlocking {
        viewModel.onTypeFilterChange(CustomerType.VIP)
        var state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("City Creative Pack & Print (VIP)", state.visibleCustomers[0].displayName)

        viewModel.onTypeFilterChange(CustomerType.GOVERNMENT)
        state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("জাতীয় শিক্ষাক্রম ও পাঠ্যপুস্তক বোর্ড (NCTB)", state.visibleCustomers[0].displayName)
    }

    @Test
    fun test13_combinedTypeAndStatusFilter_producesIntersection() = runBlocking {
        // Dealer + Inactive -> only cus-007
        viewModel.onTypeFilterChange(CustomerType.DEALER)
        viewModel.onStatusFilterChange(CustomerStatusType.INACTIVE)
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Meghna Paper & Stationery Mart", state.visibleCustomers[0].displayName)
    }

    @Test
    fun test14_searchPlusTypePlusStatus_filterCombinationWorks() = runBlocking {
        viewModel.onTypeFilterChange(CustomerType.DEALER)
        viewModel.onStatusFilterChange(CustomerStatusType.ACTIVE)
        viewModel.onSearchQueryChange("Al-Noor")

        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Al-Noor Printing & Book Agency", state.visibleCustomers[0].displayName)
    }

    @Test
    fun test15_emptyFilterCombination_showsZeroResults() = runBlocking {
        // Government + Inactive -> No matches in initial sample dataset
        viewModel.onTypeFilterChange(CustomerType.GOVERNMENT)
        viewModel.onStatusFilterChange(CustomerStatusType.INACTIVE)

        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(0, state.visibleCount)
        assertEquals(10, state.totalCount)
    }

    @Test
    fun test16_retailAliasWorksAsIndividual() = runBlocking {
        viewModel.onTypeFilterChange(CustomerType.RETAIL)
        val state = viewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Md. Abdullah Rahman", state.visibleCustomers[0].displayName)
        assertEquals(CustomerType.INDIVIDUAL, state.visibleCustomers[0].customerType)
    }
}
