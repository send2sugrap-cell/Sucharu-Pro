package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Customer Inquiries in Sucharu Pro.
 */
interface InquiryDataSource {

    /** Continuous reactive stream of all inquiries. */
    fun observeInquiries(): Flow<List<Inquiry>>

    /** One-shot fetch of an inquiry by ID. */
    suspend fun fetchInquiryById(inquiryId: String): DomainResult<Inquiry>

    /** Inserts a new inquiry. */
    suspend fun insertInquiry(inquiry: Inquiry): DomainResult<Inquiry>

    /** Updates an existing inquiry. */
    suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry>

    /** Updates the status of an inquiry. */
    suspend fun updateInquiryStatus(inquiryId: String, status: InquiryStatusType): DomainResult<Inquiry>

    /** Deletes an inquiry by ID. */
    suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit>
}
