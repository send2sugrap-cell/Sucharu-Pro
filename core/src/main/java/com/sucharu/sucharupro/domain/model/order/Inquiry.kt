package com.sucharu.sucharupro.domain.model.order

/**
 * Customer business inquiry representing the initial requirement capture in Sucharu Pro.
 *
 * References the customer via [customerId] without embedding the full customer entity.
 */
data class Inquiry(
    val inquiryId: String,
    val inquiryNumber: String,
    val customerId: String,
    val status: InquiryStatusType = InquiryStatusType.NEW,
    val source: InquirySource = InquirySource.DIRECT_VISIT,
    val items: List<InquiryRequirement> = emptyList(),
    val contactPhone: String? = null,
    val contactPerson: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(inquiryId.isNotBlank()) { "Inquiry ID cannot be blank." }
        require(inquiryNumber.isNotBlank()) { "Inquiry Number cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Updated timestamp cannot be blank." }
    }

    val totalItemsCount: Int get() = items.size
    val totalQuantity: Int get() = items.sumOf { it.quantity }
}
