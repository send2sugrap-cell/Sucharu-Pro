package com.sucharu.sucharupro.domain.model.inventory

/**
 * Standard unit of measure for counting finished products in inventory (Module 07 Step 01).
 */
enum class InventoryUnit(val defaultLabel: String) {
    PCS("Pcs"),
    SET("Set"),
    BOX("Box"),
    PACK("Pack"),
    UNIT("Unit")
}
