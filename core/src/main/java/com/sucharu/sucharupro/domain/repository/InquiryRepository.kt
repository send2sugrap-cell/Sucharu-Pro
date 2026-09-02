package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Customer Inquiries in Sucharu Pro.
 */
interface InquiryRepository {

    /** Reactive stream of all customer inquiries. */
    fun getInquiries(): Flow<List<Inquiry>>

    /** Reactive stream observing a single inquiry by [inquiryId]. */
    fun getInquiryById(inquiryId: String): Flow<Inquiry?>

    /** Direct lookup of an inquiry by [inquiryId]. */
    suspend fun findInquiryById(inquiryId: String): DomainResult<Inquiry>

    /** Reactive stream of inquiries belonging to a specific customer. */
    fun getInquiriesForCustomer(customerId: String): Flow<List<Inquiry>>

    /** Creates a new customer inquiry. */
    suspend fun createInquiry(inquiry: Inquiry): DomainResult<Inquiry>

    /** Updates an existing inquiry while preserving its ID and creation timestamp. */
    suspend fun updateInquiry(inquiry: Inquiry): DomainResult<Inquiry>

    /** Updates the lifecycle status of an inquiry following domain transition rules. */
    suspend fun updateInquiryStatus(inquiryId: String, status: InquiryStatusType): DomainResult<Inquiry>

    /** Deletes an inquiry if no linked quotations or dependencies exist. */
    suspend fun deleteInquiry(inquiryId: String): DomainResult<Unit>
}
