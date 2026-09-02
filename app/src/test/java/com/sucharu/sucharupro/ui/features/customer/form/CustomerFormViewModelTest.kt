package com.sucharu.sucharupro.ui.features.customer.form

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerFormViewModelTest {

    @Test
    fun test01_createMode_initializesCleanForm() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)

        val state = viewModel.formState.value
        assertEquals(CustomerFormMode.CREATE, state.mode)
        assertFalse(state.isEditMode)
        assertEquals("", state.displayName)
        assertEquals(CustomerType.INDIVIDUAL, state.customerType)
        assertEquals(CustomerStatusType.ACTIVE, state.status)
        assertEquals("", state.primaryPhone)
        assertNull(state.displayNameError)
        assertNull(state.primaryPhoneError)
    }

    @Test
    fun test02_editMode_loadsExistingCustomerData() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = "cus-001", repository = repository, externalScope = testScope)

        val state = viewModel.formState.value
        assertEquals(CustomerFormMode.EDIT, state.mode)
        assertTrue(state.isEditMode)
        assertEquals("Md. Abdullah Rahman", state.displayName)
        assertEquals("CUS-000001", state.customerCode)
        assertEquals("+880 1711-234567", state.primaryPhone)
        assertEquals("abdullah.rahman@gmail.com", state.email)
        assertEquals("Uttara", state.area)
        assertEquals("Dhaka", state.city)
    }

    @Test
    fun test03_blankDisplayName_triggersValidationError() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)

        viewModel.onDisplayNameChange("   ")
        var successCalled = false
        viewModel.saveCustomer { successCalled = true }

        val state = viewModel.formState.value
        assertFalse(successCalled)
        assertNotNull(state.displayNameError)
        assertEquals("Customer name is required.", state.displayNameError)
    }

    @Test
    fun test04_blankPrimaryPhone_triggersValidationError() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)

        viewModel.onDisplayNameChange("Test Customer")
        viewModel.onPrimaryPhoneChange("  ")
        var successCalled = false
        viewModel.saveCustomer { successCalled = true }

        val state = viewModel.formState.value
        assertFalse(successCalled)
        assertNotNull(state.primaryPhoneError)
        assertEquals("Primary phone is required.", state.primaryPhoneError)
    }

    @Test
    fun test05_invalidEmail_triggersValidationError() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)

        viewModel.onDisplayNameChange("Test Customer")
        viewModel.onPrimaryPhoneChange("01711000000")
        viewModel.onEmailChange("invalid-email-address")

        var successCalled = false
        viewModel.saveCustomer { successCalled = true }

        val state = viewModel.formState.value
        assertFalse(successCalled)
        assertNotNull(state.emailError)
        assertEquals("Enter a valid email address (e.g., info@domain.com).", state.emailError)
    }

    @Test
    fun test05b_invalidAlternatePhone_triggersValidationError() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)

        viewModel.onDisplayNameChange("Test Customer")
        viewModel.onPrimaryPhoneChange("01711000000")
        viewModel.onAlternatePhoneChange("123") // Too short

        var successCalled = false
        viewModel.saveCustomer { successCalled = true }

        val state = viewModel.formState.value
        assertFalse(successCalled)
        assertNotNull(state.alternatePhoneError)
    }

    @Test
    fun test06_optionalEmail_allowsBlank() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)

        viewModel.onDisplayNameChange("মদিনা প্রিন্টিং প্রেস")
        viewModel.onPrimaryPhoneChange("01811223344")
        viewModel.onEmailChange("")

        var savedId: String? = null
        viewModel.saveCustomer { savedId = it }

        val state = viewModel.formState.value
        assertNull(state.emailError)
        assertTrue(state.saveSuccess)
        assertNotNull(savedId)
    }

    @Test
    fun test07_validCreate_savesToRepository() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)

        viewModel.onDisplayNameChange("New Horizon Press")
        viewModel.onCustomerTypeChange(CustomerType.BUSINESS)
        viewModel.onPrimaryPhoneChange("+880 1911-998877")
        viewModel.onEmailChange("info@horizonpress.com")
        viewModel.onContactPersonChange("Rahim Uddin")
        viewModel.onAddressLineChange("Plot 4, Road 2")
        viewModel.onAreaChange("Arambagh")
        viewModel.onCityChange("Dhaka")

        var createdId: String? = null
        viewModel.saveCustomer { createdId = it }

        assertTrue(viewModel.formState.value.saveSuccess)
        assertNotNull(createdId)

        // Verify in repository
        val customer = repository.findCustomerById(createdId!!).getOrNull()
        assertNotNull(customer)
        assertEquals("New Horizon Press", customer?.displayName)
        assertEquals(CustomerType.BUSINESS, customer?.customerType)
        assertEquals("Rahim Uddin", customer?.contactPersonName)
    }

    @Test
    fun test08_validEdit_updatesCustomerInRepository() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = "cus-001", repository = repository, externalScope = testScope)

        viewModel.onDisplayNameChange("Md. Abdullah Rahman (Updated)")
        viewModel.onStatusChange(CustomerStatusType.INACTIVE)
        viewModel.onPrimaryPhoneChange("+880 1711-999999")

        var updatedId: String? = null
        viewModel.saveCustomer { updatedId = it }

        assertEquals("cus-001", updatedId)
        assertTrue(viewModel.formState.value.saveSuccess)

        // Verify in repository
        val customer = repository.findCustomerById("cus-001").getOrNull()
        assertNotNull(customer)
        assertEquals("Md. Abdullah Rahman (Updated)", customer?.displayName)
        assertEquals(CustomerStatusType.INACTIVE, customer?.status)
        assertEquals("+880 1711-999999", customer?.primaryPhone)
    }

    @Test
    fun test09_banglaUnicodeInput_savesSuccessfully() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)

        viewModel.onDisplayNameChange("বাংলা একাডেমি প্রকাশনী")
        viewModel.onCustomerTypeChange(CustomerType.INSTITUTION)
        viewModel.onPrimaryPhoneChange("+880 1552-123456")
        viewModel.onAddressLineChange("ধানমন্ডি, ঢাকা")
        viewModel.onNotesChange("বিশেষ ছাড় প্রযোজ্য")

        var savedId: String? = null
        viewModel.saveCustomer { savedId = it }

        assertTrue(viewModel.formState.value.saveSuccess)
        assertNotNull(savedId)

        val customer = repository.findCustomerById(savedId!!).getOrNull()
        assertNotNull(customer)
        assertEquals("বাংলা একাডেমি প্রকাশনী", customer?.displayName)
        assertEquals("বিশেষ ছাড় প্রযোজ্য", customer?.notes)
    }

    @Test
    fun test10_duplicatePhone_triggersWarningAndAllowsOverride() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = null, repository = repository, externalScope = testScope)

        // Attempt to create customer with same phone as cus-001 ("01711234567")
        viewModel.onDisplayNameChange("Duplicate Candidate Author")
        viewModel.onPrimaryPhoneChange("01711234567")

        var savedId: String? = null
        viewModel.saveCustomer { savedId = it }

        val state = viewModel.formState.value
        assertNull(savedId)
        assertNotNull(state.duplicateWarning)
        assertTrue(state.duplicateWarning!!.contains("Md. Abdullah Rahman"))

        // Now override and proceed
        viewModel.acknowledgeDuplicateAndSave { savedId = it }
        assertNotNull(savedId)
        assertTrue(viewModel.formState.value.saveSuccess)
    }

    @Test
    fun test11_editCustomer_preservesExactCustomerIdAndDoesNotCreateDuplicate() = runBlocking {
        val repository = CustomerRepositoryImpl(FakeCustomerDataSource())
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = CustomerFormViewModel(customerId = "cus-002", repository = repository, externalScope = testScope)

        viewModel.onDisplayNameChange("Bengal Publications Ltd. (Main Office)")
        var savedId: String? = null
        viewModel.saveCustomer { savedId = it }

        assertEquals("cus-002", savedId)

        // Verify total customer count remains 10 (no second entity created)
        val count = repository.getCustomers().first().size
        assertEquals(10, count)
    }
}
