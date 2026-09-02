package com.sucharu.sucharupro.domain.model.inventory

/**
 * Stock availability status for finished and saleable products in Sucharu Pro.
 *
 * This applies to finished/saleable products maintained in inventory, such as:
 * Quran Sharif, Qaida, Ampara, Tajwid, Calendars, Diaries, Gift Items,
 * Promotional Items, Corporate Gifts, and other stocked finished products.
 *
 * Note: Sucharu Pro does NOT track raw material inventory (paper, ink, plates, chemicals).
 * This enum is exclusively for finished product stock levels.
 */
enum class StockStatusType(val defaultLabel: String) {
    /** Product is in stock and available for dispatch. */
    IN_STOCK("In Stock"),

    /** Product stock is below minimum threshold — replenishment needed soon. */
    LOW_STOCK("Low Stock"),

    /** Product is completely out of stock — cannot be dispatched. */
    OUT_OF_STOCK("Out of Stock")
}
