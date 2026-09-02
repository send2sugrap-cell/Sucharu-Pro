package com.sucharu.sucharupro.domain.model.inventory.analytics

/**
 * Pre-defined time periods for inventory analytics filtering (Module 07 Step 10).
 */
enum class InventoryAnalyticsPeriod {
    TODAY,
    YESTERDAY,
    LAST_7_DAYS,
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    CURRENT_QUARTER,
    CURRENT_YEAR,
    CUSTOM
}
