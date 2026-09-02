package com.sucharu.sucharupro.domain.model.finance

/**
 * Standard aging classification categories for unpaid customer receivables (Module 09 Step 02).
 */
enum class ReceivableAgingBucket(val defaultLabel: String) {
    CURRENT("Current / Not Due"),
    DAYS_1_TO_30("1–30 Days Overdue"),
    DAYS_31_TO_60("31–60 Days Overdue"),
    DAYS_61_TO_90("61–90 Days Overdue"),
    DAYS_OVER_90("90+ Days Overdue")
}
