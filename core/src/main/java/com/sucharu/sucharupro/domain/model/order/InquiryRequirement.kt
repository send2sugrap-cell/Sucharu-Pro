package com.sucharu.sucharupro.domain.model.order

/**
 * Generic specification requirement line item within a customer inquiry.
 *
 * Represents diverse printing, packaging, book, and promotional requirements:
 * e.g., Visiting Cards, Flyers, Brochures, Books, Posters, Packaging, Banners, or Design Services.
 */
data class InquiryRequirement(
    val itemId: String,
    val productName: String,
    val description: String,
    val quantity: Int,
    val unit: String = "Pcs",
    val size: String? = null,
    val paperMaterial: String? = null,
    val gsm: Int? = null,
    val colorSpecification: String? = null,
    val printingMethod: String? = null,
    val finishing: String? = null,
    val isDesignRequired: Boolean = false,
    val notes: String? = null
) {
    init {
        require(itemId.isNotBlank()) { "Requirement itemId cannot be blank." }
        require(productName.isNotBlank()) { "Product name cannot be blank." }
        require(description.isNotBlank()) { "Requirement description cannot be blank." }
        require(quantity > 0) { "Quantity must be greater than zero (was $quantity)." }
        require(unit.isNotBlank()) { "Unit cannot be blank." }
        if (gsm != null) {
            require(gsm > 0) { "GSM must be positive when specified." }
        }
    }
}
