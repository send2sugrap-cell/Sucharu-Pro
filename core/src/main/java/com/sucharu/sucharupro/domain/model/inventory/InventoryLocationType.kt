package com.sucharu.sucharupro.domain.model.inventory

/**
 * Storage location classification within a warehouse (Module 07 Step 02).
 */
enum class InventoryLocationType(val defaultLabel: String) {
    AREA("Storage Area"),
    ROOM("Storage Room"),
    ZONE("Zone / Bay"),
    RACK("Storage Rack"),
    SHELF("Shelf Tier"),
    BIN("Storage Bin"),
    CABINET("Enclosed Cabinet"),
    COUNTER("Picking Counter"),
    FLOOR("Floor Location"),
    OTHER("Other Location")
}
