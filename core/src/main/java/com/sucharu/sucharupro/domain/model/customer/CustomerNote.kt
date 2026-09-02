package com.sucharu.sucharupro.domain.model.customer

/**
 * Domain entity representing an internal customer-level note / remark.
 *
 * Scoped strictly to Customer Management (internal observations, preferences, remarks).
 *
 * @param id Unique note identifier (UUID).
 * @param customerId Reference to the customer entity.
 * @param text Content of the note (supports English, Bengali, and mixed Unicode).
 * @param isImportant Flag indicating high-priority or critical internal instruction.
 * @param authorName Optional author / staff identifier.
 * @param createdAt ISO 8601 creation timestamp.
 * @param updatedAt ISO 8601 last update timestamp.
 */
data class CustomerNote(
    val id: String,
    val customerId: String,
    val text: String,
    val isImportant: Boolean = false,
    val authorName: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(id.isNotBlank()) { "Note ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(text.isNotBlank()) { "Note text cannot be blank." }
    }
}
