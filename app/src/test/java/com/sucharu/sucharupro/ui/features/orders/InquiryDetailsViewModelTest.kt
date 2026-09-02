package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.repository.InquiryRepository
import com.sucharu.sucharupro.ui.features.orders.inquiry.details.InquiryDetailsUiState
import com.sucharu.sucharupro.ui.features.orders.inquiry.details.InquiryDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InquiryDetailsViewModelTest {

    private fun sampleInquiry(id: String = "inq-101") = Inquiry(
        inquiryId = id,
        inquiryNumber = "INQ-2026-0001",
        customerId = "cus-001",
        status = InquiryStatusType.NEW,
        source = InquirySource.DIRECT_VISIT,
        items = listOf(
            InquiryRequirement(
                itemId = "req-01",
                productName = "প্যাকেজিং বক্স",
                description = "Custom printed box",
                quantity = 5000,
                unit = "pcs",
                size = "8x6x3 in",
                paperMaterial = "Duplex Board",
                gsm = 350,
                colorSpecification = "4 Color",
                printingMethod = "Offset",
                finishing = "Gloss Lamination + Die Cut",
                isDesignRequired = true,
                notes = "Urgent delivery required"
            )
        ),
        contactPerson = "রাহিম আহমেদ",
        contactPhone = "+8801711000001",
        notes = "Bangla inquiry notes: ডেলিভারি দ্রুত করতে হবে।",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private fun createFakeRepo(inquiry: Inquiry?): InquiryRepository = object : InquiryRepository {
        override fun getInquiries(): Flow<List<Inquiry>> = flowOf(listOfNotNull(inquiry))
        override fun getInquiryById(inquiryId: String): Flow<Inquiry?> = flowOf(if (inquiry?.inquiryId == inquiryId) inquiry else null)
        override suspend fun findInquiryById(inquiryId: String): DomainResult<Inquiry> =
            if (inquiry?.inquiryId == inquiryId) DomainResult.Success(inquiry) else DomainResult.Error(message = "Not found")
        override fun getInquiriesForCustomer(customerId: String): Flow<List<Inquiry>> = flowOf(listOfNotNull(inquiry))
        override suspend fun createInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Success(inquiry)
        override suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Success(inquiry)
        override suspend fun updateInquiryStatus(inquiryId: String, status: InquiryStatusType): DomainResult<Inquiry> =
            inquiry?.let { DomainResult.Success(it.copy(status = status)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit> = DomainResult.Success(Unit)
    }

    @Test
    fun loadInquiry_successfulLoad_emitsSuccessWithCorrectData() {
        val inquiry = sampleInquiry("inq-101")
        val repo = createFakeRepo(inquiry)
        val vm = InquiryDetailsViewModel(
            inquiryId = "inq-101",
            repository = repo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue("State should be Success, got $state", state is InquiryDetailsUiState.Success)
        val successState = state as InquiryDetailsUiState.Success
        assertEquals("inq-101", successState.inquiry.inquiryId)
        assertEquals("INQ-2026-0001", successState.inquiry.inquiryNumber)
        assertEquals("cus-001", successState.inquiry.customerId)
        assertEquals("রাহিম আহমেদ", successState.inquiry.contactPerson)
        assertEquals(1, successState.inquiry.items.size)
        assertEquals("প্যাকেজিং বক্স", successState.inquiry.items[0].productName)
        assertEquals(5000, successState.inquiry.items[0].quantity)
        assertTrue(successState.inquiry.items[0].isDesignRequired)
    }

    @Test
    fun loadInquiry_recordNotFound_emitsNotFoundState() {
        val repo = createFakeRepo(null)
        val vm = InquiryDetailsViewModel(
            inquiryId = "inq-non-existent",
            repository = repo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue("State should be NotFound, got $state", state is InquiryDetailsUiState.NotFound)
        assertEquals("inq-non-existent", (state as InquiryDetailsUiState.NotFound).inquiryId)
    }

    @Test
    fun loadInquiry_repositoryThrows_emitsErrorState() {
        val failingRepo = object : InquiryRepository {
            override fun getInquiries(): Flow<List<Inquiry>> = flow { emit(emptyList()) }
            override fun getInquiryById(inquiryId: String): Flow<Inquiry?> = flow {
                throw RuntimeException("Database connection timeout")
            }
            override suspend fun findInquiryById(inquiryId: String): DomainResult<Inquiry> =
                DomainResult.Error(message = "Database error")
            override fun getInquiriesForCustomer(customerId: String): Flow<List<Inquiry>> = flow { emit(emptyList()) }
            override suspend fun createInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Error(message = "Error")
            override suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Error(message = "Error")
            override suspend fun updateInquiryStatus(inquiryId: String, status: InquiryStatusType): DomainResult<Inquiry> =
                DomainResult.Error(message = "Error")
            override suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit> = DomainResult.Error(message = "Error")
        }

        val vm = InquiryDetailsViewModel(
            inquiryId = "inq-101",
            repository = failingRepo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue("State should be Error, got $state", state is InquiryDetailsUiState.Error)
        assertEquals("Database connection timeout", (state as InquiryDetailsUiState.Error).errorMessage)
    }

    @Test
    fun retry_reloadsInquiryState() {
        var shouldFail = true
        val retryRepo = object : InquiryRepository {
            override fun getInquiries(): Flow<List<Inquiry>> = flow { emit(emptyList()) }
            override fun getInquiryById(inquiryId: String): Flow<Inquiry?> = flow {
                if (shouldFail) {
                    throw RuntimeException("Temporary failure")
                } else {
                    emit(sampleInquiry(inquiryId))
                }
            }
            override suspend fun findInquiryById(inquiryId: String): DomainResult<Inquiry> = DomainResult.Error(message = "Error")
            override fun getInquiriesForCustomer(customerId: String): Flow<List<Inquiry>> = flow { emit(emptyList()) }
            override suspend fun createInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Error(message = "Error")
            override suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry> = DomainResult.Error(message = "Error")
            override suspend fun updateInquiryStatus(inquiryId: String, status: InquiryStatusType): DomainResult<Inquiry> =
                DomainResult.Error(message = "Error")
            override suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit> = DomainResult.Error(message = "Error")
        }

        val vm = InquiryDetailsViewModel(
            inquiryId = "inq-101",
            repository = retryRepo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        assertTrue(vm.uiState.value is InquiryDetailsUiState.Error)

        shouldFail = false
        vm.retry()

        assertTrue(vm.uiState.value is InquiryDetailsUiState.Success)
    }
}
