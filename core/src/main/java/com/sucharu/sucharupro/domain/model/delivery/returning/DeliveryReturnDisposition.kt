package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Return item disposition classification (Module 08 Step 07).
 *
 * RESTOCK triggers canonical Module 07 inventory stock-in movement.
 * Non-restock dispositions do not alter sellable stock.
 */
enum class DeliveryReturnDisposition(val defaultLabel: String, val allowsRestock: Boolean) {
    RESTOCK("Return to Stock (Restock)", true),
    QUARANTINE("Quarantine / Quality Hold", false),
    REWORK("Rework / Reprint", false),
    REPLACEMENT("Replacement Pending", false),
    SCRAP("Scrap / Disposal", false),
    CUSTOMER_HOLD("Hold for Customer Decision", false),
    NON_STOCK("Non-Stocked Disposal", false),
    PENDING_DECISION("Pending Decision", false)
}
