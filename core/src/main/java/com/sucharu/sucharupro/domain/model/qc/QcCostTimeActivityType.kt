package com.sucharu.sucharupro.domain.model.qc

/**
 * Audit activity types for QC cost and time tracking lifecycle (Module 06 Step 08).
 */
enum class QcCostTimeActivityType(val defaultLabel: String) {
    /** A new QC cost entry was created. */
    QC_COST_ENTRY_CREATED("QC Cost Entry Created"),

    /** An existing QC cost entry was updated. */
    QC_COST_ENTRY_UPDATED("QC Cost Entry Updated"),

    /** A QC cost entry was recorded/confirmed. */
    QC_COST_ENTRY_RECORDED("QC Cost Entry Recorded"),

    /** A new QC time tracking entry was created. */
    QC_TIME_ENTRY_CREATED("QC Time Entry Created"),

    /** A QC time tracking entry was recorded/completed. */
    QC_TIME_ENTRY_RECORDED("QC Time Entry Recorded"),

    /** Planned vs. actual reconciliation was calculated. */
    QC_RECONCILIATION_CALCULATED("QC Reconciliation Calculated"),

    /** Reconciliation was completed. */
    QC_RECONCILIATION_COMPLETED("QC Reconciliation Completed"),

    /** Reconciliation values were adjusted with reason. */
    QC_RECONCILIATION_ADJUSTED("QC Reconciliation Adjusted"),

    /** Reconciliation was permanently locked and sealed. */
    QC_RECONCILIATION_LOCKED("QC Reconciliation Locked"),

    /** Reconciliation was cancelled. */
    QC_RECONCILIATION_CANCELLED("QC Reconciliation Cancelled"),

    /** Immutable snapshot was generated. */
    QC_COST_TIME_SNAPSHOT_CREATED("QC Cost & Time Snapshot Created");

    companion object {
        fun fromString(value: String?): QcCostTimeActivityType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
