package com.sucharu.sucharupro.ui.features.customer.details

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerDetailsViewModelTest {

    @Test
    fun test01_validCustomerId_loadsCustomerSuccessfully() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-001", repository, testScope)

        val state = viewModel.uiState.value
        assertTrue(state is CustomerDetailsUiState.Success)
        val success = state as CustomerDetailsUiState.Success
        val customer = success.customer
        assertEquals("cus-001", customer.customerId)
        assertEquals("CUS-000001", customer.customerCode)
        assertEquals("Md. Abdullah Rahman", customer.displayName)
        assertEquals(CustomerType.INDIVIDUAL, customer.customerType)
        assertEquals(CustomerStatusType.ACTIVE, customer.status)
        assertEquals("+880 1711-234567", customer.primaryPhone)
        assertEquals("abdullah.rahman@gmail.com", customer.email)
        assertNotNull(customer.primaryAddress)
        assertEquals("Uttara", customer.primaryAddress?.area)
    }

    @Test
    fun test02_corporateCustomer_loadsContactPersonAndDeliveryAddress() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-002", repository, testScope)

        val state = viewModel.uiState.value as CustomerDetailsUiState.Success
        val customer = state.customer
        assertEquals("Bengal Publications Ltd.", customer.displayName)
        assertEquals(CustomerType.BUSINESS, customer.customerType)
        assertEquals("Tanvir Ahmed (Production Manager)", customer.contactPersonName)
        assertEquals(2, customer.addresses.size)
    }

    @Test
    fun test03_nonExistentCustomerId_producesNotFoundState() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("non-existent-999", repository, testScope)

        val state = viewModel.uiState.value
        assertTrue(state is CustomerDetailsUiState.NotFound)
        val notFound = state as CustomerDetailsUiState.NotFound
        assertEquals("non-existent-999", notFound.customerId)
    }

    @Test
    fun test04_retry_reloadsCustomer() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-003", repository, testScope)

        viewModel.retry()
        val state = viewModel.uiState.value
        assertTrue(state is CustomerDetailsUiState.Success)
        val customer = (state as CustomerDetailsUiState.Success).customer
        assertEquals("Al-Noor Printing & Book Agency", customer.displayName)
    }

    @Test
    fun test05_notesAndActivities_loadedInSuccessState() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-001", repository, testScope)

        val state = viewModel.uiState.value as CustomerDetailsUiState.Success
        assertEquals(2, state.notes.size)
        assertTrue(state.activities.isNotEmpty())
    }

    @Test
    fun test06_addNoteFlow_validationAndSave() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-001", repository, testScope)

        viewModel.onOpenAddNoteDialog()
        var state = viewModel.uiState.value as CustomerDetailsUiState.Success
        assertTrue(state.isNoteDialogVisible)

        // Blank text validation error
        viewModel.onNoteTextChanged("   ")
        viewModel.saveNote()
        state = viewModel.uiState.value as CustomerDetailsUiState.Success
        assertEquals("Note text cannot be empty.", state.noteErrorMessage)

        // Valid note text save
        viewModel.onNoteTextChanged("Customer requested 10 extra sample copies.")
        viewModel.onNoteImportanceChanged(true)
        viewModel.saveNote()

        state = viewModel.uiState.value as CustomerDetailsUiState.Success
        org.junit.Assert.assertFalse(state.isNoteDialogVisible)
        assertEquals(3, state.notes.size)
        assertTrue(state.notes.any { it.text == "Customer requested 10 extra sample copies." && it.isImportant })
    }

    @Test
    fun test07_editNoteFlow_updatesNoteText() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-001", repository, testScope)

        val stateBefore = viewModel.uiState.value as CustomerDetailsUiState.Success
        val noteToEdit = stateBefore.notes.first { it.id == "note-002" }

        viewModel.onOpenEditNoteDialog(noteToEdit)
        var state = viewModel.uiState.value as CustomerDetailsUiState.Success
        assertTrue(state.isNoteDialogVisible)
        assertEquals("note-002", state.editingNoteId)

        viewModel.onNoteTextChanged("Updated delivery preference.")
        viewModel.saveNote()

        state = viewModel.uiState.value as CustomerDetailsUiState.Success
        val updatedNote = state.notes.first { it.id == "note-002" }
        assertEquals("Updated delivery preference.", updatedNote.text)
    }

    @Test
    fun test08_deleteNoteFlow_removesNoteFromState() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-001", repository, testScope)

        viewModel.deleteNote("note-002")
        val state = viewModel.uiState.value as CustomerDetailsUiState.Success
        assertEquals(1, state.notes.size)
        org.junit.Assert.assertFalse(state.notes.any { it.id == "note-002" })
    }

    @Test
    fun test09_toggleNoteImportance_updatesState() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-001", repository, testScope)

        viewModel.toggleNoteImportance("note-001")
        val state = viewModel.uiState.value as CustomerDetailsUiState.Success
        val note = state.notes.first { it.id == "note-001" }
        org.junit.Assert.assertFalse(note.isImportant)
    }

    @Test
    fun test10_followUpFlow_setsAndClearsFollowUp() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-001", repository, testScope)

        viewModel.onOpenFollowUpDialog()
        var state = viewModel.uiState.value as CustomerDetailsUiState.Success
        assertTrue(state.isFollowUpDialogVisible)

        viewModel.onFollowUpInputChanged("2026-08-30")
        viewModel.saveFollowUp()

        state = viewModel.uiState.value as CustomerDetailsUiState.Success
        org.junit.Assert.assertFalse(state.isFollowUpDialogVisible)
        assertEquals("2026-08-30", state.customer.nextFollowUpAt)

        viewModel.clearFollowUp()
        state = viewModel.uiState.value as CustomerDetailsUiState.Success
        org.junit.Assert.assertNull(state.customer.nextFollowUpAt)
    }

    @Test
    fun test11_deactivateCustomer_flow() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-001", repository, testScope)

        viewModel.deactivateCustomer()
        var state = viewModel.uiState.value as CustomerDetailsUiState.Success
        assertTrue(state.isStatusConfirmDialogVisible)
        assertEquals(CustomerStatusType.INACTIVE, state.pendingStatus)

        viewModel.confirmStatusChange()
        state = viewModel.uiState.value as CustomerDetailsUiState.Success
        org.junit.Assert.assertFalse(state.isStatusConfirmDialogVisible)
        assertEquals(CustomerStatusType.INACTIVE, state.customer.status)
    }

    @Test
    fun test12_reactivateCustomer_flow() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        // cus-008 is BLOCKED
        val viewModel = CustomerDetailsViewModel("cus-008", repository, testScope)

        viewModel.reactivateCustomer()
        var state = viewModel.uiState.value as CustomerDetailsUiState.Success
        assertTrue(state.isStatusConfirmDialogVisible)
        assertEquals(CustomerStatusType.ACTIVE, state.pendingStatus)

        viewModel.confirmStatusChange()
        state = viewModel.uiState.value as CustomerDetailsUiState.Success
        assertEquals(CustomerStatusType.ACTIVE, state.customer.status)
    }

    @Test
    fun test13_dismissStatusDialog_cancelsTransition() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerDetailsViewModel("cus-001", repository, testScope)

        viewModel.deactivateCustomer()
        viewModel.dismissStatusConfirmDialog()

        val state = viewModel.uiState.value as CustomerDetailsUiState.Success
        org.junit.Assert.assertFalse(state.isStatusConfirmDialogVisible)
        assertEquals(CustomerStatusType.ACTIVE, state.customer.status)
    }
}
