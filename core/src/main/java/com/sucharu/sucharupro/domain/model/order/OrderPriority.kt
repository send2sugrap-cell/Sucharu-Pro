package com.sucharu.sucharupro.domain.model.order

/**
 * Commercial priority classification for customer orders in Sucharu Pro.
 */
enum class OrderPriority(val defaultLabel: String) {
    /** Standard turnaround priority. */
    NORMAL("Normal"),

    /** Priority handling with accelerated schedule. */
    HIGH("High"),

    /** Critical / express emergency turnaround. */
    URGENT("Urgent")
}
