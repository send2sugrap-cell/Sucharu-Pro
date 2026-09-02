package com.sucharu.sucharupro.ui.features.customer

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.ui.features.customer.details.CustomerDetailsUiState
import com.sucharu.sucharupro.ui.features.customer.details.CustomerDetailsViewModel
import com.sucharu.sucharupro.ui.features.customer.form.CustomerFormViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end integration and data integrity test suite for Module 02 (Customer Management).
 */
class CustomerIntegrationTest {

    private lateinit var repository: CustomerRepositoryImpl
    private lateinit var testScope: CoroutineScope
    private lateinit var listViewModel: CustomerListViewModel

    @Before
    fun setUp() {
        val dataSource = FakeCustomerDataSource()
        repository = CustomerRepositoryImpl(dataSource)
        testScope = CoroutineScope(Dispatchers.Unconfined)
        listViewModel = CustomerListViewModel(repository, testScope)
    }

    @Test
    fun test01_endToEnd_customerCreationAndListSync() = runBlocking {
        val initialListState = listViewModel.uiState.value as CustomerListUiState.Success
        val initialCount = initialListState.totalCount

        // 1. Create New Customer via FormViewModel
        val formViewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)
        formViewModel.onDisplayNameChange("সোনার বাংলা প্রিন্টার্স")
        formViewModel.onCustomerTypeChange(CustomerType.BUSINESS)
        formViewModel.onPrimaryPhoneChange("+880 1819-998877")
        formViewModel.onAddressLineChange("নয়াপল্টন, ঢাকা")
        formViewModel.onNotesChange("জরুরি বুকলেট মুদ্রণ")

        var createdId: String? = null
        formViewModel.saveCustomer { createdId = it }

        assertNotNull(createdId)
        assertTrue(formViewModel.formState.value.saveSuccess)

        // 2. Verify in Customer List
        val updatedList = repository.getCustomers().first()
        assertEquals(initialCount + 1, updatedList.size)
        val createdInRepo = updatedList.firstOrNull { it.customerId == createdId }
        assertNotNull(createdInRepo)
        assertEquals("সোনার বাংলা প্রিন্টার্স", createdInRepo?.displayName)

        // 3. Verify in Customer Details
        val detailsViewModel = CustomerDetailsViewModel(createdId!!, repository, testScope)
        val detailsState = detailsViewModel.uiState.value as CustomerDetailsUiState.Success
        assertEquals("সোনার বাংলা প্রিন্টার্স", detailsState.customer.displayName)
        assertEquals("+880 1819-998877", detailsState.customer.primaryPhone)
        assertEquals("জরুরি বুকলেট মুদ্রণ", detailsState.customer.notes)
    }

    @Test
    fun test02_endToEnd_customerEditAndStateConsistency() = runBlocking {
        // 1. Load initial customer details
        val detailsViewModel = CustomerDetailsViewModel("cus-001", repository, testScope)
        val initialDetails = (detailsViewModel.uiState.value as CustomerDetailsUiState.Success).customer
        assertEquals("Md. Abdullah Rahman", initialDetails.displayName)

        // 2. Edit Customer via FormViewModel
        val editViewModel = CustomerFormViewModel(customerId = "cus-001", repository = repository, externalScope = testScope)
        editViewModel.onDisplayNameChange("Md. Abdullah Rahman (Corporate Head)")
        editViewModel.onPrimaryPhoneChange("+880 1711-888888")
        editViewModel.onStatusChange(CustomerStatusType.INACTIVE)

        var savedId: String? = null
        editViewModel.saveCustomer { savedId = it }
        assertEquals("cus-001", savedId)

        // 3. Verify Details re-reads updated data from reactive repository
        detailsViewModel.loadCustomer()
        val updatedDetails = (detailsViewModel.uiState.value as CustomerDetailsUiState.Success).customer
        assertEquals("Md. Abdullah Rahman (Corporate Head)", updatedDetails.displayName)
        assertEquals("+880 1711-888888", updatedDetails.primaryPhone)
        assertEquals(CustomerStatusType.INACTIVE, updatedDetails.status)

        // 4. Verify List reflects updated customer
        listViewModel.onSearchQueryChange("Corporate Head")
        val searchState = listViewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, searchState.visibleCount)
        assertEquals("Md. Abdullah Rahman (Corporate Head)", searchState.visibleCustomers[0].displayName)
    }

    @Test
    fun test03_searchAndFilterConsistency() = runBlocking {
        // Filter by Dealer
        listViewModel.onTypeFilterChange(CustomerType.DEALER)
        var state = listViewModel.uiState.value as CustomerListUiState.Success
        assertEquals(2, state.visibleCount)

        // Filter Dealer + Search "Bogura"
        listViewModel.onSearchQueryChange("Bogura")
        state = listViewModel.uiState.value as CustomerListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("Meghna Paper & Stationery Mart", state.visibleCustomers[0].displayName)

        // Clear all filters restores full dataset
        listViewModel.clearFilters()
        state = listViewModel.uiState.value as CustomerListUiState.Success
        assertEquals(10, state.visibleCount)
    }

    @Test
    fun test04_dataIntegrity_preventsDuplicateEntities() = runBlocking {
        val initialCustomers = repository.getCustomers().first()
        val initialSize = initialCustomers.size

        // Edit an existing customer must NOT increase list size
        val editViewModel = CustomerFormViewModel(customerId = "cus-002", repository = repository, externalScope = testScope)
        editViewModel.onDisplayNameChange("Bengal Publications Ltd. (Main Branch)")
        editViewModel.saveCustomer {}

        val updatedCustomers = repository.getCustomers().first()
        assertEquals(initialSize, updatedCustomers.size)
        assertEquals("Bengal Publications Ltd. (Main Branch)", updatedCustomers.first { it.customerId == "cus-002" }.displayName)
    }

    @Test
    fun test05_contactManagement_updatePhoneEmailAddress_reflectsAcrossModules() = runBlocking {
        // Edit cus-003 contact info
        val editViewModel = CustomerFormViewModel(customerId = "cus-003", repository = repository, externalScope = testScope)
        editViewModel.onPrimaryPhoneChange("+880 1819-112233")
        editViewModel.onAlternatePhoneChange("+880 1911-223344")
        editViewModel.onEmailChange("contact@alnoorpress.com.bd")
        editViewModel.onContactPersonChange("হাফেজ মাওলানা নূর হোসেন")
        editViewModel.onAddressLineChange("আন্দারকিল্লা, চট্টগ্রাম")
        editViewModel.onCityChange("চট্টগ্রাম")

        var savedId: String? = null
        editViewModel.saveCustomer { savedId = it }
        assertEquals("cus-003", savedId)

        // Verify in Customer Details
        val detailsViewModel = CustomerDetailsViewModel("cus-003", repository, testScope)
        val customer = (detailsViewModel.uiState.value as CustomerDetailsUiState.Success).customer
        assertEquals("+880 1819-112233", customer.primaryPhone)
        assertEquals("+880 1911-223344", customer.alternatePhone)
        assertEquals("contact@alnoorpress.com.bd", customer.email)
        assertEquals("হাফেজ মাওলানা নূর হোসেন", customer.contactPersonName)
        assertEquals("আন্দারকিল্লা, চট্টগ্রাম", customer.primaryAddress?.addressLine)
        assertEquals("চট্টগ্রাম", customer.primaryAddress?.city)
    }

    @Test
    fun test06_missingContactInfo_handlesGracefully() = runBlocking {
        // Create a minimal individual customer without optional email/alternate phone/address
        val formViewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)
        formViewModel.onDisplayNameChange("আব্দুর রহিম")
        formViewModel.onPrimaryPhoneChange("01711223344")

        var createdId: String? = null
        formViewModel.saveCustomer { createdId = it }
        assertNotNull(createdId)

        val detailsViewModel = CustomerDetailsViewModel(createdId!!, repository, testScope)
        val state = detailsViewModel.uiState.value as CustomerDetailsUiState.Success
        assertEquals("আব্দুর রহিম", state.customer.displayName)
        assertEquals("01711223344", state.customer.primaryPhone)
        assertEquals(null, state.customer.email)
        assertEquals(null, state.customer.alternatePhone)
        assertTrue(state.customer.addresses.isEmpty())
    }

    @Test
    fun test07_customerTypeAndStatusMutation_reflectsInDetailsAndList() = runBlocking {
        // Edit cus-001 from Individual/Active to VIP/Active
        val editViewModel = CustomerFormViewModel(customerId = "cus-001", repository = repository, externalScope = testScope)
        editViewModel.onCustomerTypeChange(CustomerType.VIP)
        editViewModel.onStatusChange(CustomerStatusType.ACTIVE)

        var savedId: String? = null
        editViewModel.saveCustomer { savedId = it }
        assertEquals("cus-001", savedId)

        // Verify in Details Screen
        val detailsViewModel = CustomerDetailsViewModel("cus-001", repository, testScope)
        val detailsCustomer = (detailsViewModel.uiState.value as CustomerDetailsUiState.Success).customer
        assertEquals(CustomerType.VIP, detailsCustomer.customerType)
        assertEquals(CustomerStatusType.ACTIVE, detailsCustomer.status)
        assertEquals("cus-001", detailsCustomer.customerId)

        // Verify in List Screen with VIP filter
        listViewModel.onTypeFilterChange(CustomerType.VIP)
        val listState = listViewModel.uiState.value as CustomerListUiState.Success
        assertTrue(listState.visibleCustomers.any { it.customerId == "cus-001" })
    }

    @Test
    fun test08_customerNoteAddition_updatesTimelineAndLastActivity() = runBlocking {
        val detailsViewModel = CustomerDetailsViewModel("cus-001", repository, testScope)
        detailsViewModel.onOpenAddNoteDialog()
        detailsViewModel.onNoteTextChanged("প্রিন্ট কোয়ালিটি অত্যন্ত নিখুঁত হতে হবে।")
        detailsViewModel.onNoteImportanceChanged(true)
        detailsViewModel.saveNote()

        val state = detailsViewModel.uiState.value as CustomerDetailsUiState.Success
        assertEquals(3, state.notes.size)
        val latestNote = state.notes.first()
        assertEquals("প্রিন্ট কোয়ালিটি অত্যন্ত নিখুঁত হতে হবে।", latestNote.text)
        assertTrue(latestNote.isImportant)

        // Activity timeline contains NOTE_ADDED
        assertTrue(state.activities.any { it.type == com.sucharu.sucharupro.domain.model.customer.CustomerActivityType.NOTE_ADDED })
    }

    @Test
    fun test09_customerDataIsolation_noDataLeakage() = runBlocking {
        val cus1Details = CustomerDetailsViewModel("cus-001", repository, testScope)
        val cus2Details = CustomerDetailsViewModel("cus-002", repository, testScope)

        val cus1State = cus1Details.uiState.value as CustomerDetailsUiState.Success
        val cus2State = cus2Details.uiState.value as CustomerDetailsUiState.Success

        // Verify notes isolation
        assertTrue(cus1State.notes.all { it.customerId == "cus-001" })
        assertTrue(cus2State.notes.all { it.customerId == "cus-002" })

        // Verify activities isolation
        assertTrue(cus1State.activities.all { it.customerId == "cus-001" })
        assertTrue(cus2State.activities.all { it.customerId == "cus-002" })
    }

    @Test
    fun test10_customerLifecycleDeactivationAndReactivation_acrossScreens() = runBlocking {
        // 1. Deactivate cus-001 from Customer Details
        val detailsViewModel = CustomerDetailsViewModel("cus-001", repository, testScope)
        detailsViewModel.deactivateCustomer()
        detailsViewModel.confirmStatusChange()

        var detailsState = detailsViewModel.uiState.value as CustomerDetailsUiState.Success
        assertEquals(CustomerStatusType.INACTIVE, detailsState.customer.status)

        // 2. Verify in Customer List Screen with Inactive filter
        listViewModel.onStatusFilterChange(CustomerStatusType.INACTIVE)
        var listState = listViewModel.uiState.value as CustomerListUiState.Success
        assertTrue(listState.visibleCustomers.any { it.customerId == "cus-001" })

        // 3. Reactivate customer from Details Screen
        detailsViewModel.reactivateCustomer()
        detailsViewModel.confirmStatusChange()

        detailsState = detailsViewModel.uiState.value as CustomerDetailsUiState.Success
        assertEquals(CustomerStatusType.ACTIVE, detailsState.customer.status)

        // 4. Verify in Customer List Screen with Active filter
        listViewModel.onStatusFilterChange(CustomerStatusType.ACTIVE)
        listState = listViewModel.uiState.value as CustomerListUiState.Success
        assertTrue(listState.visibleCustomers.any { it.customerId == "cus-001" })
    }
}
