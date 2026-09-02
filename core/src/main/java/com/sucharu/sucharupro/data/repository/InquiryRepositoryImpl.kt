package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.InquiryDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.repository.InquiryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Production-ready implementation of [InquiryRepository] delegating to [InquiryDataSource].
 */
class InquiryRepositoryImpl(
    private val dataSource: InquiryDataSource
) : InquiryRepository {

    override fun getInquiries(): Flow<List<Inquiry>> = dataSource.observeInquiries()

    override fun getInquiryById(inquiryId: String): Flow<Inquiry?> {
        return dataSource.observeInquiries().map { inquiries ->
            inquiries.find { it.inquiryId == inquiryId }
        }
    }

    override suspend fun findInquiryById(inquiryId: String): DomainResult<Inquiry> {
        return dataSource.fetchInquiryById(inquiryId)
    }

    override fun getInquiriesForCustomer(customerId: String): Flow<List<Inquiry>> {
        return dataSource.observeInquiries().map { inquiries ->
            inquiries.filter { it.customerId == customerId }
        }
    }

    override suspend fun createInquiry(inquiry: Inquiry): DomainResult<Inquiry> {
        if (inquiry.inquiryId.isBlank()) {
            return DomainResult.Error(message = "Inquiry ID cannot be blank.")
        }
        if (inquiry.inquiryNumber.isBlank()) {
            return DomainResult.Error(message = "Inquiry Number cannot be blank.")
        }
        if (inquiry.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }
        return dataSource.insertInquiry(inquiry)
    }

    override suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry> {
        if (inquiry.inquiryId.isBlank()) {
            return DomainResult.Error(message = "Inquiry ID cannot be blank.")
        }
        return dataSource.updateInquiry(inquiry)
    }

    override suspend fun updateInquiryStatus(
        inquiryId: String,
        status: InquiryStatusType
    ): DomainResult<Inquiry> {
        if (inquiryId.isBlank()) {
            return DomainResult.Error(message = "Inquiry ID cannot be blank.")
        }
        return dataSource.updateInquiryStatus(inquiryId, status)
    }

    override suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit> {
        if (inquiryId.isBlank()) {
            return DomainResult.Error(message = "Inquiry ID cannot be blank.")
        }
        return dataSource.deleteInquiry(inquiryId)
    }
}
