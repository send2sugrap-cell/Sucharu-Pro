package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.repository.FakeCustomerRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerCreditProfile
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.InquiryRepository
import com.sucharu.sucharupro.ui.features.orders.inquiry.form.InquiryFormMode
import com.sucharu.sucharupro.ui.features.orders.inquiry.form.InquiryFormViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InquiryFormViewModelTest {

    private val sampleCustomer = Customer(
        customerId = "cus-001",
        customerCode = "CUS-1001",
        displayName = "প্রাইম পাবলিকেশন (Prime Pub)",
        customerType = CustomerType.BUSINESS,
        status = CustomerStatusType.ACTIVE,
        primaryPhone = "01711000111",
        creditProfile = CustomerCreditProfile.DEFAULT_CASH_ONLY,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private fun sampleInquiry(id: String = "inq-101") = Inquiry(
        inquiryId = id,
        inquiryNumber = "INQ-2026-0001",
        customerId = "cus-001",
        source = InquirySource.DIRECT_VISIT,
        status = InquiryStatusType.NEW,
        contactPerson = "জনাব মাসুদ",
        contactPhone = "01711000111",
        items = listOf(
            InquiryRequirement(
                itemId = "req-01",
                productName = "ক্যাটালগ প্রিন্টিং",
                description = "A4 সাইজ, কভার ৪ কালার, ইনার ৮০ জিএসএম অফসেট",
                quantity = 2000,
                unit = "Pcs",
                size = "A4",
                paperMaterial = "Offset & Art Card",
                gsm = 80,
                colorSpecification = "4 Color",
                printingMethod = "Offset",
                finishing = "Gloss Lamination",
                isDesignRequired = true,
                notes = "জরুরি ডেলিভারি লাগবে।"
            )
        ),
        notes = "গ্রাহক স্পেসিফিকেশন চূড়ান্ত করেছেন।",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private fun createFakeCustomerRepo(): CustomerRepository {
        return FakeCustomerRepository()
    }

    private fun createFakeInquiryRepo(
        inquiry: Inquiry? = null,
        onSaveCallback: ((Inquiry) -> Unit)? = null
    ): InquiryRepository = object : InquiryRepository {
        var savedInquiry: Inquiry? = null
        override fun getInquiries(): Flow<List<Inquiry>> = flowOf(listOfNotNull(inquiry))
        override fun getInquiryById(inquiryId: String): Flow<Inquiry?> =
            flowOf(if (inquiry?.inquiryId == inquiryId) inquiry else null)
        override suspend fun findInquiryById(inquiryId: String): DomainResult<Inquiry> =
            if (inquiry?.inquiryId == inquiryId) DomainResult.Success(inquiry) else DomainResult.Error(message = "Not found")
        override fun getInquiriesForCustomer(customerId: String): Flow<List<Inquiry>> = flowOf(listOfNotNull(inquiry))
        override suspend fun createInquiry(inquiry: Inquiry): DomainResult<Inquiry> {
            savedInquiry = inquiry
            onSaveCallback?.invoke(inquiry)
            return DomainResult.Success(inquiry)
        }
        override suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry> {
            savedInquiry = inquiry
            onSaveCallback?.invoke(inquiry)
            return DomainResult.Success(inquiry)
        }
        override suspend fun updateInquiryStatus(inquiryId: String, status: InquiryStatusType): DomainResult<Inquiry> =
            inquiry?.let { DomainResult.Success(it.copy(status = status)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit> = DomainResult.Success(Unit)
    }

    @Test
    fun createInquiry_initialState_isCorrect() {
        val vm = InquiryFormViewModel(
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertEquals(InquiryFormMode.CREATE, state.mode)
        assertFalse(state.isEditMode)
        assertEquals("New Inquiry", state.screenTitle)
        assertTrue(state.availableCustomers.isNotEmpty())
        assertTrue(state.items.isEmpty())
        assertNull(state.customerIdError)
    }

    @Test
    fun createInquiry_validation_failsWhenCustomerOrItemsMissing() {
        val inqRepo = createFakeInquiryRepo()
        val vm = InquiryFormViewModel(
            inquiryRepository = inqRepo,
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        var savedId: String? = null
        vm.saveInquiry { savedId = it }

        val state = vm.uiState.value
        assertNull(savedId)
        assertNotNull(state.customerIdError)
        assertNotNull(state.itemsError)
    }

    @Test
    fun createInquiry_addEditRemoveItem_updatesItemsList() {
        val vm = InquiryFormViewModel(
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val item1 = InquiryRequirement(
            itemId = "req-1",
            productName = "বই",
            description = "১০০ পৃষ্ঠা",
            quantity = 500
        )
        val item2 = InquiryRequirement(
            itemId = "req-2",
            productName = "লিফলেট",
            description = "A5 সাইজ",
            quantity = 1000
        )

        vm.saveItem(item1)
        assertEquals(1, vm.uiState.value.items.size)

        vm.saveItem(item2)
        assertEquals(2, vm.uiState.value.items.size)

        // Edit item 1
        vm.openEditItemDialog(0)
        val updatedItem1 = item1.copy(quantity = 600)
        vm.saveItem(updatedItem1)
        assertEquals(600, vm.uiState.value.items[0].quantity)

        // Remove item 2
        vm.removeItem(1)
        assertEquals(1, vm.uiState.value.items.size)
    }

    @Test
    fun createInquiry_success_createsNewInquiryWithGeneratedId() {
        var createdInquiry: Inquiry? = null
        val inqRepo = createFakeInquiryRepo(onSaveCallback = { createdInquiry = it })
        val vm = InquiryFormViewModel(
            inquiryRepository = inqRepo,
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        vm.onCustomerSelected(sampleCustomer)
        vm.onSourceChange(InquirySource.WHATSAPP)
        vm.onNotesChange("জরুরি বুকিং নোট")
        vm.saveItem(
            InquiryRequirement(
                itemId = "req-01",
                productName = "ক্যালেন্ডার ২০২৬",
                description = "ওয়াল ক্যালেন্ডার ১২ পাতা",
                quantity = 1500
            )
        )

        var resultId: String? = null
        vm.saveInquiry { resultId = it }

        assertNotNull(resultId)
        assertNotNull(createdInquiry)
        assertEquals(sampleCustomer.customerId, createdInquiry?.customerId)
        assertEquals(InquirySource.WHATSAPP, createdInquiry?.source)
        assertEquals(InquiryStatusType.NEW, createdInquiry?.status)
        assertEquals(1, createdInquiry?.items?.size)
        assertEquals("জরুরি বুকিং নোট", createdInquiry?.notes)
    }

    @Test
    fun editInquiry_loadsExistingAndUpdatesPreservingIdentity() {
        val existing = sampleInquiry("inq-999")
        var updatedInquiry: Inquiry? = null
        val inqRepo = createFakeInquiryRepo(inquiry = existing, onSaveCallback = { updatedInquiry = it })

        val vm = InquiryFormViewModel(
            inquiryId = "inq-999",
            inquiryRepository = inqRepo,
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertEquals(InquiryFormMode.EDIT, state.mode)
        assertTrue(state.isEditMode)
        assertEquals("inq-999", state.inquiryId)
        assertEquals("INQ-2026-0001", state.inquiryNumber)
        assertEquals("cus-001", state.customerId)
        assertEquals(1, state.items.size)

        // Modify note
        vm.onNotesChange("সংশোধিত নোট")
        var resultId: String? = null
        vm.saveInquiry { resultId = it }

        assertEquals("inq-999", resultId)
        assertNotNull(updatedInquiry)
        assertEquals("inq-999", updatedInquiry?.inquiryId)
        assertEquals("INQ-2026-0001", updatedInquiry?.inquiryNumber)
        assertEquals(existing.createdAt, updatedInquiry?.createdAt)
        assertEquals(InquiryStatusType.NEW, updatedInquiry?.status)
        assertEquals("সংশোধিত নোট", updatedInquiry?.notes)
    }

    @Test
    fun createInquiry_handlesRepositoryErrorGracefully() {
        val failingRepo = object : InquiryRepository {
            override fun getInquiries(): Flow<List<Inquiry>> = flowOf(emptyList())
            override fun getInquiryById(inquiryId: String): Flow<Inquiry?> = flowOf(null)
            override suspend fun findInquiryById(inquiryId: String): DomainResult<Inquiry> = DomainResult.Error(message = "Error")
            override fun getInquiriesForCustomer(customerId: String): Flow<List<Inquiry>> = flowOf(emptyList())
            override suspend fun createInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Error(message = "Database disk full")
            override suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Error(message = "Database disk full")
            override suspend fun updateInquiryStatus(inquiryId: String, status: InquiryStatusType): DomainResult<Inquiry> =
                DomainResult.Error(message = "Error")
            override suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit> = DomainResult.Error(message = "Error")
        }

        val vm = InquiryFormViewModel(
            inquiryRepository = failingRepo,
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        vm.onCustomerSelected(sampleCustomer)
        vm.saveItem(InquiryRequirement(itemId = "r-1", productName = "Test", description = "Desc", quantity = 100))

        var savedId: String? = null
        vm.saveInquiry { savedId = it }

        assertNull(savedId)
        val state = vm.uiState.value
        assertFalse(state.isSaving)
        assertEquals("Database disk full", state.errorMessage)
    }
}
