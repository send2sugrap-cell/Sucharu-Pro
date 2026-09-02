package com.sucharu.sucharupro.domain.model.inventory

/**
 * Product classification enum for inventory management in Sucharu Pro (Module 07 Step 01).
 */
enum class InventoryProductType(val defaultLabel: String) {
    FINISHED_PRODUCT("Finished Product"),
    PRINTED_PRODUCT("Printed Product"),
    GIFT_PRODUCT("Gift Product"),
    BOOK("Book"),
    CUSTOM_PRODUCT("Custom Product"),
    OTHER("Other")
}
