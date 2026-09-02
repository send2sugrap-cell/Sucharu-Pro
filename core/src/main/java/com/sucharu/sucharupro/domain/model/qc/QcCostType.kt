package com.sucharu.sucharupro.domain.model.qc

/**
 * Operational categorization of QC costs (Module 06 Step 08).
 */
enum class QcCostType(val defaultLabel: String) {
    /** Inspection labor and consumables for standard inspection. */
    INSPECTION("Standard QC Inspection"),

    /** Effort and resources spent on investigating defects and root causes. */
    DEFECT_INVESTIGATION("Defect Investigation"),

    /** Immediate containment measures applied to prevent defect propagation. */
    DEFECT_CONTAINMENT("Defect Containment"),

    /** Corrective quality control effort associated with rework execution. */
    REWORK_QC("Rework Quality Control"),

    /** Inspection labor and overhead incurred during repeat Re-QC cycles. */
    RE_QC("Re-QC Cycle Inspection"),

    /** Comprehensive final quality verification and release inspection. */
    FINAL_QC("Final QC Inspection"),

    /** General quality administration, compliance auditing, and reporting. */
    ADMINISTRATIVE_QC("QC Administration"),

    /** Miscellaneous operational QC expenses. */
    OTHER("Other QC Cost");

    companion object {
        fun fromString(value: String?): QcCostType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
