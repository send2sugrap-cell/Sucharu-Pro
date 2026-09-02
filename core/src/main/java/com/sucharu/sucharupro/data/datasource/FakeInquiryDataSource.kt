package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [InquiryDataSource] for development and testing.
 */
class FakeInquiryDataSource(
    initialInquiries: List<Inquiry> = defaultSampleInquiries()
) : InquiryDataSource {

    private val mutex = Mutex()
    private val _inquiries = MutableStateFlow<List<Inquiry>>(initialInquiries)

    override fun observeInquiries(): Flow<List<Inquiry>> = _inquiries.asStateFlow()

    override suspend fun fetchInquiryById(inquiryId: String): DomainResult<Inquiry> = mutex.withLock {
        val inquiry = _inquiries.value.find { it.inquiryId == inquiryId }
        return if (inquiry != null) {
            DomainResult.Success(inquiry)
        } else {
            DomainResult.Error(message = "Inquiry not found with ID: $inquiryId")
        }
    }

    override suspend fun insertInquiry(inquiry: Inquiry): DomainResult<Inquiry> = mutex.withLock {
        if (_inquiries.value.any { it.inquiryId == inquiry.inquiryId }) {
            return DomainResult.Error(message = "Inquiry with ID '${inquiry.inquiryId}' already exists.")
        }
        if (_inquiries.value.any { it.inquiryNumber.equals(inquiry.inquiryNumber, ignoreCase = true) }) {
            return DomainResult.Error(message = "Inquiry with Number '${inquiry.inquiryNumber}' already exists.")
        }

        _inquiries.value = _inquiries.value + inquiry
        DomainResult.Success(inquiry)
    }

    override suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry> = mutex.withLock {
        val index = _inquiries.value.indexOfFirst { it.inquiryId == inquiry.inquiryId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent inquiry: ${inquiry.inquiryId}")
        }

        val existing = _inquiries.value[index]
        val updated = inquiry.copy(
            inquiryId = existing.inquiryId,
            createdAt = existing.createdAt
        )

        val currentList = _inquiries.value.toMutableList()
        currentList[index] = updated
        _inquiries.value = currentList.toList()
        DomainResult.Success(updated)
    }

    override suspend fun updateInquiryStatus(
        inquiryId: String,
        status: InquiryStatusType
    ): DomainResult<Inquiry> = mutex.withLock {
        val index = _inquiries.value.indexOfFirst { it.inquiryId == inquiryId }
        if (index == -1) {
            return DomainResult.Error(message = "Inquiry not found: $inquiryId")
        }

        val existing = _inquiries.value[index]
        if (!existing.status.canTransitionTo(status)) {
            return DomainResult.Error(
                message = "Invalid status transition from '${existing.status.defaultLabel}' to '${status.defaultLabel}'."
            )
        }

        val updated = existing.copy(status = status)
        val currentList = _inquiries.value.toMutableList()
        currentList[index] = updated
        _inquiries.value = currentList.toList()
        DomainResult.Success(updated)
    }

    override suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit> = mutex.withLock {
        val exists = _inquiries.value.any { it.inquiryId == inquiryId }
        if (!exists) {
            return DomainResult.Error(message = "Inquiry not found: $inquiryId")
        }

        _inquiries.value = _inquiries.value.filterNot { it.inquiryId == inquiryId }
        DomainResult.Success(Unit)
    }

    companion object {
        fun defaultSampleInquiries(): List<Inquiry> = listOf(
            Inquiry(
                inquiryId = "inq-001",
                inquiryNumber = "INQ-000001",
                customerId = "cus-001",
                status = InquiryStatusType.QUOTED,
                source = InquirySource.DIRECT_VISIT,
                items = listOf(
                    InquiryRequirement(
                        itemId = "inq-item-01",
                        productName = "Corporate Visiting Cards",
                        description = "300 GSM Art Card, Matte Lamination, Both Side 4 Color Print",
                        quantity = 1000,
                        unit = "Pcs",
                        size = "3.25 x 2.0 inch",
                        paperMaterial = "Art Card",
                        gsm = 300,
                        colorSpecification = "4/4 CMYK",
                        printingMethod = "Offset",
                        finishing = "Matte Lamination",
                        isDesignRequired = false
                    )
                ),
                contactPhone = "+880 1711-234567",
                contactPerson = "Md. Abdullah Rahman",
                notes = "Sample card provided for color matching.",
                createdAt = "2026-08-10T09:30:00Z",
                updatedAt = "2026-08-10T11:00:00Z"
            ),
            Inquiry(
                inquiryId = "inq-002",
                inquiryNumber = "INQ-000002",
                customerId = "cus-002",
                status = InquiryStatusType.NEW,
                source = InquirySource.EMAIL,
                items = listOf(
                    InquiryRequirement(
                        itemId = "inq-item-02",
                        productName = "Annual Report Book",
                        description = "120 Pages, Inner 100 GSM Art Paper, Cover 300 GSM Hard Cover with Gold Foil",
                        quantity = 500,
                        unit = "Copies",
                        size = "A4 (8.27 x 11.69 in)",
                        paperMaterial = "Art Paper / Board",
                        gsm = 100,
                        colorSpecification = "Inner 4 Color, Cover Gold Foil + Spot UV",
                        printingMethod = "Offset",
                        finishing = "Hardcover Thread Sewing",
                        isDesignRequired = true,
                        notes = "Artwork files will be provided via Google Drive."
                    )
                ),
                contactPhone = "+880 1819-876543",
                contactPerson = "Engr. Rafiqul Islam",
                notes = "Annual General Meeting is on Sept 15, needs urgent estimate.",
                createdAt = "2026-08-14T14:15:00Z",
                updatedAt = "2026-08-14T14:15:00Z"
            )
        )
    }
}
