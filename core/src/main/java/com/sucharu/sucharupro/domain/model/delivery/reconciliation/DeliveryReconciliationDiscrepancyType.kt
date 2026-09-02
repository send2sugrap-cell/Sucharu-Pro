package com.sucharu.sucharupro.domain.model.delivery.reconciliation

enum class DeliveryReconciliationDiscrepancyType(val defaultLabel: String) {
    DISPATCH_EXCEEDS_ORDER("Dispatch Exceeds Ordered Quantity"),
    DELIVERY_EXCEEDS_DISPATCH("Delivered Exceeds Dispatched Quantity"),
    POD_EXCEEDS_DELIVERY("Accepted POD Exceeds Delivered Quantity"),
    POD_MISSING("Accepted POD Evidence Missing"),
    RETURN_EXCEEDS_DELIVERY("Returned Exceeds Delivered Quantity"),
    QUANTITY_MISMATCH("Item Quantity Mismatch"),
    PROJECT_MISMATCH("Project Boundary Mismatch"),
    ORDER_MISMATCH("Delivery Order Reference Mismatch"),
    REJECTION_UNRESOLVED("Unresolved Item Rejection")
}
