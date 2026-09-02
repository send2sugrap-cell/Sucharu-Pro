package com.sucharu.sucharupro.domain.model.order

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Commercial item line in a Quotation in Sucharu Pro.
 *
 * Encapsulates pricing arithmetic: (unitPrice × quantity) - discount = lineSubtotal.
 */
data class QuotationItem(
    val itemId: String,
    val description: String,
    val specification: String? = null,
    val quantity: Int,
    val unit: String = "Pcs",
    val unitPrice: Money,
    val discount: Money = Money.ZERO,
    val notes: String? = null
) {
    init {
        require(itemId.isNotBlank()) { "Item ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(quantity > 0) { "Quantity must be positive (was $quantity)." }
        require(unit.isNotBlank()) { "Unit cannot be blank." }
        require(!unitPrice.isNegative()) { "Unit price cannot be negative (was ${unitPrice.formatted()})." }
        require(!discount.isNegative()) { "Discount cannot be negative (was ${discount.formatted()})." }
    }

    /** Total before line discount. */
    val lineGrossTotal: Money get() = unitPrice * quantity

    /** Net total for this line after discount. */
    val lineSubtotal: Money
        get() {
            val gross = lineGrossTotal
            return if (discount >= gross) Money.ZERO else gross - discount
        }
}
