package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.repository.FakeCustomerRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerCreditProfile
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTermType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.InquiryRepository
import com.sucharu.sucharupro.domain.repository.QuotationRepository
import com.sucharu.sucharupro.ui.features.orders.quotation.form.QuotationFormMode
import com.sucharu.sucharupro.ui.features.orders.quotation.form.QuotationFormViewModel
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

class QuotationFormViewModelTest {

    private val sampleCustomer = Customer(
        customerId = "cus-001",
        customerCode = "CUS-1001",
        displayName = "প্রাইম পাবলিকেশন",
        customerType = CustomerType.BUSINESS,
        status = CustomerStatusType.ACTIVE,
        primaryPhone = "01711000111",
        creditProfile = CustomerCreditProfile.DEFAULT_CASH_ONLY,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private fun sampleDraftQuotation(id: String = "quo-101"): Quotation {
        val rev1 = QuotationRevision(
            revisionId = "rev-001",
            quotationId = id,
            revisionNumber = 1,
            items = listOf(
                QuotationItem(
                    itemId = "item-01",
                    description = "বই প্রিন্টিং",
                    specification = "A4, 80 GSM",
                    quantity = 1000,
                    unit = "copies",
                    unitPrice = Money(150.0),
                    discount = Money(5000.0)
                )
            ),
            discount = Money(2000.0),
            paymentTerms = PaymentTerms(PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 50),
            deliveryRequirement = DeliveryRequirement(DeliveryType.BUSINESS_DELIVERY),
            notes = "Initial draft remarks",
            createdAt = "2026-08-16T10:00:00Z",
            createdBy = "Commercial Desk"
        )

        return Quotation(
            quotationId = id,
            quotationNumber = "QUO-2026-0001",
            customerId = "cus-001",
            inquiryId = "inq-101",
            status = QuotationStatusType.DRAFT,
            currentRevisionNumber = 1,
            revisions = listOf(rev1),
            validUntil = "2026-09-16",
            termsAndConditions = "Standard commercial terms apply.",
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
    }

    private fun sampleApprovedQuotation(id: String = "quo-202"): Quotation =
        sampleDraftQuotation(id).copy(
            status = QuotationStatusType.APPROVED,
            approvedRevisionId = "rev-001",
            approvedBy = "Manager"
        )

    private fun createFakeCustomerRepo(): CustomerRepository {
        return FakeCustomerRepository()
    }

    private fun createFakeQuotationRepo(
        quotation: Quotation? = null,
        onSaveCallback: ((Quotation) -> Unit)? = null
    ): QuotationRepository = object : QuotationRepository {
        var savedQuotation: Quotation? = null
        override fun getQuotations(): Flow<List<Quotation>> = flowOf(listOfNotNull(quotation))
        override fun getQuotationById(quotationId: String): Flow<Quotation?> =
            flowOf(if (quotation?.quotationId == quotationId) quotation else null)
        override suspend fun findQuotationById(quotationId: String): DomainResult<Quotation> =
            if (quotation?.quotationId == quotationId) DomainResult.Success(quotation) else DomainResult.Error(message = "Not found")
        override fun getQuotationsForCustomer(customerId: String): Flow<List<Quotation>> = flowOf(listOfNotNull(quotation))
        override fun getQuotationsForInquiry(inquiryId: String): Flow<List<Quotation>> = flowOf(listOfNotNull(quotation))
        override suspend fun createQuotation(quotation: Quotation): DomainResult<Quotation> {
            savedQuotation = quotation
            onSaveCallback?.invoke(quotation)
            return DomainResult.Success(quotation)
        }
        override suspend fun updateQuotation(quotation: Quotation): DomainResult<Quotation> {
            savedQuotation = quotation
            onSaveCallback?.invoke(quotation)
            return DomainResult.Success(quotation)
        }
        override suspend fun updateQuotationStatus(quotationId: String, status: QuotationStatusType): DomainResult<Quotation> =
            quotation?.let { DomainResult.Success(it.copy(status = status)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun deleteQuotation(quotationId: String): DomainResult<Unit> = DomainResult.Success(Unit)
        override fun getQuotationRevisions(quotationId: String): Flow<List<QuotationRevision>> = flowOf(quotation?.revisions ?: emptyList())
        override suspend fun findQuotationRevision(quotationId: String, revisionId: String): DomainResult<QuotationRevision> =
            quotation?.revisions?.find { it.revisionId == revisionId }?.let { DomainResult.Success(it) } ?: DomainResult.Error(message = "Not found")
        override suspend fun createQuotationRevision(quotationId: String, revision: QuotationRevision): DomainResult<QuotationRevision> =
            DomainResult.Success(revision)
        override suspend fun getLatestQuotationRevision(quotationId: String): QuotationRevision? = quotation?.currentRevision
        override suspend fun getApprovedQuotationRevision(quotationId: String): QuotationRevision? = null
        override suspend fun approveQuotationRevision(
            quotationId: String,
            revisionId: String,
            approvedBy: String,
            timestamp: String
        ): DomainResult<Quotation> = DomainResult.Error(message = "Not supported")
    }

    private fun createFakeInquiryRepo(inquiry: Inquiry? = null): InquiryRepository = object : InquiryRepository {
        override fun getInquiries(): Flow<List<Inquiry>> = flowOf(listOfNotNull(inquiry))
        override fun getInquiryById(inquiryId: String): Flow<Inquiry?> = flowOf(if (inquiry?.inquiryId == inquiryId) inquiry else null)
        override suspend fun findInquiryById(inquiryId: String): DomainResult<Inquiry> =
            if (inquiry?.inquiryId == inquiryId) DomainResult.Success(inquiry) else DomainResult.Error(message = "Not found")
        override fun getInquiriesForCustomer(customerId: String): Flow<List<Inquiry>> = flowOf(listOfNotNull(inquiry))
        override suspend fun createInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Success(inquiry)
        override suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Success(inquiry)
        override suspend fun updateInquiryStatus(inquiryId: String, status: InquiryStatusType): DomainResult<Inquiry> =
            DomainResult.Success(inquiry!!)
        override suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit> = DomainResult.Success(Unit)
    }

    @Test
    fun createQuotation_initialState_isCorrect() {
        val vm = QuotationFormViewModel(
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertEquals(QuotationFormMode.CREATE, state.mode)
        assertFalse(state.isEditMode)
        assertEquals("New Quotation", state.screenTitle)
        assertTrue(state.items.isEmpty())
        assertEquals(Money.ZERO, state.subtotal)
        assertEquals(Money.ZERO, state.totalAmount)
        assertFalse(state.isImmutableError)
    }

    @Test
    fun createQuotation_calculations_withFinancialMoneyIntegrity() {
        val vm = QuotationFormViewModel(
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // Item 1: 1000 pcs @ 100 tk, discount 2000 tk -> lineSubtotal = 98,000 tk
        val item1 = QuotationItem(
            itemId = "q-1",
            description = "আইটেম ১",
            quantity = 1000,
            unitPrice = Money(100.0),
            discount = Money(2000.0)
        )
        // Item 2: 500 pcs @ 50 tk, discount 0 tk -> lineSubtotal = 25,000 tk
        val item2 = QuotationItem(
            itemId = "q-2",
            description = "আইটেম ২",
            quantity = 500,
            unitPrice = Money(50.0),
            discount = Money.ZERO
        )

        vm.saveItem(item1)
        vm.saveItem(item2)

        val state = vm.uiState.value
        assertEquals(Money(123000.0), state.subtotal) // 98,000 + 25,000 = 123,000

        // Apply quotation-level discount of 3,000 tk
        vm.onQuotationDiscountChange("3000")
        assertEquals(Money(3000.0), vm.uiState.value.discountMoney)
        assertEquals(Money(120000.0), vm.uiState.value.totalAmount) // 123,000 - 3,000 = 120,000
    }

    @Test
    fun createQuotation_prefillFromInquiry_populatesCustomerAndItems() {
        val inq = Inquiry(
            inquiryId = "inq-505",
            inquiryNumber = "INQ-505",
            customerId = "cus-001",
            source = InquirySource.PHONE_CALL,
            status = InquiryStatusType.IN_PROGRESS,
            items = listOf(
                InquiryRequirement(
                    itemId = "req-1",
                    productName = "ম্যাগাজিন",
                    description = "১২০ পৃষ্ঠা",
                    quantity = 2500,
                    size = "A4"
                )
            ),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )

        val vm = QuotationFormViewModel(
            initialInquiryId = "inq-505",
            inquiryRepository = createFakeInquiryRepo(inq),
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertEquals("inq-505", state.inquiryId)
        assertEquals("cus-001", state.customerId)
        assertEquals(1, state.items.size)
        assertEquals("ম্যাগাজিন", state.items[0].description)
        assertEquals(2500, state.items[0].quantity)
    }

    @Test
    fun createQuotation_success_createsDraftQuotationWithRevision1() {
        var createdQuotation: Quotation? = null
        val quoRepo = createFakeQuotationRepo(onSaveCallback = { createdQuotation = it })

        val vm = QuotationFormViewModel(
            quotationRepository = quoRepo,
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        vm.onCustomerSelected(sampleCustomer)
        vm.onValidUntilChange("2026-09-30")
        vm.saveItem(
            QuotationItem(
                itemId = "q-01",
                description = "বই ছাপানো",
                quantity = 1000,
                unitPrice = Money(200.0)
            )
        )
        vm.onPaymentTermsChange(PaymentTerms(PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 40))
        vm.onDeliveryRequirementChange(
            DeliveryRequirement(
                deliveryType = DeliveryType.BUSINESS_DELIVERY,
                address = "ঢাকা প্রেস ক্লাস্টার"
            )
        )

        var savedId: String? = null
        vm.saveQuotation { savedId = it }

        assertNotNull(savedId)
        assertNotNull(createdQuotation)
        assertEquals(QuotationStatusType.DRAFT, createdQuotation?.status)
        assertEquals(1, createdQuotation?.currentRevisionNumber)
        assertEquals(1, createdQuotation?.revisions?.size)
        assertEquals(Money(200000.0), createdQuotation?.totalAmount)
        assertEquals("2026-09-30", createdQuotation?.validUntil)
    }

    @Test
    fun editDraftQuotation_loadsAndUpdatesDraftPreservingIdentity() {
        val existingDraft = sampleDraftQuotation("quo-101")
        var updatedQuotation: Quotation? = null
        val quoRepo = createFakeQuotationRepo(quotation = existingDraft, onSaveCallback = { updatedQuotation = it })

        val vm = QuotationFormViewModel(
            quotationId = "quo-101",
            quotationRepository = quoRepo,
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertEquals(QuotationFormMode.EDIT_DRAFT, state.mode)
        assertTrue(state.isEditMode)
        assertFalse(state.isImmutableError)
        assertEquals("quo-101", state.quotationId)
        assertEquals("QUO-2026-0001", state.quotationNumber)
        assertEquals(1, state.items.size)

        // Modify quotation discount
        vm.onQuotationDiscountChange("5000")
        var savedId: String? = null
        vm.saveQuotation { savedId = it }

        assertEquals("quo-101", savedId)
        assertNotNull(updatedQuotation)
        assertEquals("quo-101", updatedQuotation?.quotationId)
        assertEquals("QUO-2026-0001", updatedQuotation?.quotationNumber)
        assertEquals(QuotationStatusType.DRAFT, updatedQuotation?.status)
        assertEquals(existingDraft.createdAt, updatedQuotation?.createdAt)
    }

    @Test
    fun editQuotation_nonDraftStatus_blocksMutationWithImmutableWarning() {
        val approvedQuotation = sampleApprovedQuotation("quo-202")
        val quoRepo = createFakeQuotationRepo(quotation = approvedQuotation)

        val vm = QuotationFormViewModel(
            quotationId = "quo-202",
            quotationRepository = quoRepo,
            customerRepository = createFakeCustomerRepo(),
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue(state.isImmutableError)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Only DRAFT quotations can be edited"))

        // Attempt to save should do nothing
        var savedId: String? = null
        vm.saveQuotation { savedId = it }
        assertNull(savedId)
    }
}
