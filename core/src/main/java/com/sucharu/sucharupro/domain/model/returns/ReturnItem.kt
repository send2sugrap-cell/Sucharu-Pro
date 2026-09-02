package com.sucharu.sucharupro.domain.model.returns

/**
 * Child entity representing a specific line item in a Return Request (Module 11 Step 01).
 */
data class ReturnItem(
    val returnItemId: String,
    val returnId: String,
    val productId: String,
    val originalChallanItemId: String?,
    val requestedQuantity: Int,
    val acceptedQuantity: Int = 0,
    val rejectedQuantity: Int = 0,
    val unit: String? = null,
    val condition: String? = null,
    val notes: String? = null
) {
    init {
        require(returnItemId.isNotBlank()) { "Return Item ID cannot be blank." }
        require(returnId.isNotBlank()) { "Return ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(requestedQuantity > 0) { "Requested quantity must be strictly positive." }
        require(acceptedQuantity >= 0) { "Accepted quantity cannot be negative." }
        require(rejectedQuantity >= 0) { "Rejected quantity cannot be negative." }
        require(acceptedQuantity + rejectedQuantity <= requestedQuantity) { 
            "Sum of accepted and rejected quantity ($acceptedQuantity + $rejectedQuantity) cannot exceed requested quantity ($requestedQuantity)." 
        }
    }
}
