package com.sucharu.sucharupro.domain.model.inventory

/**
 * Physical warehouse classification in Sucharu Pro (Module 07 Step 02).
 */
enum class InventoryWarehouseType(val defaultLabel: String) {
    MAIN("Main Warehouse"),
    FINISHED_GOODS("Finished Goods Warehouse"),
    BOOK("Book Storage Facility"),
    GIFT("Gift & Merchandise Hub"),
    DISPATCH("Dispatch & Staging Hub"),
    RETURN("Returns & Inspection Center"),
    QUARANTINE("Quarantine Holding Area"),
    OTHER("Other Storage Facility")
}
