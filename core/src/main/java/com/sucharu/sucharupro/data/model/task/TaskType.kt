package com.sucharu.sucharupro.data.model.task

/**
 * Extensible task classification categories for Sucharu Pro ERP.
 */
enum class TaskType(val defaultLabel: String) {
    GENERAL("General Task"),
    ORDER("Order Processing"),
    DESIGN("Design & Prepress"),
    APPROVAL("Approval Request"),
    PRODUCTION("Production Job"),
    QC("Quality Control"),
    INVENTORY("Inventory & Stock"),
    DELIVERY("Delivery & Dispatch"),
    FINANCE("Financial & Billing"),
    CUSTOMER_SERVICE("Customer Service"),
    ADMINISTRATIVE("Administrative"),
    URGENT("Urgent Task"),
    FOLLOW_UP("Follow Up");

    companion object {
        fun fromString(type: String?): TaskType {
            if (type.isNull_or_blank()) return GENERAL
            return entries.firstOrNull { it.name.equals(type, ignoreCase = true) } ?: GENERAL
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
