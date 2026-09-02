package com.sucharu.sucharupro.domain.model.qc

/**
 * Operational categorization of QC time tracking entries (Module 06 Step 08).
 */
enum class QcTimeEntryType(val defaultLabel: String) {
    /** Time spent performing standard visual/dimensional QC inspections. */
    INSPECTION("QC Inspection"),

    /** Time spent on root-cause failure investigation and diagnostic analysis. */
    INVESTIGATION("Defect Investigation"),

    /** Time spent applying immediate physical or digital containment actions. */
    CONTAINMENT("Defect Containment"),

    /** Time spent reviewing and evaluating rework requests and plans. */
    REWORK_REVIEW("Rework Review & Planning"),

    /** Time spent performing repeat Re-QC inspections on completed reworks. */
    RE_QC("Re-QC Inspection"),

    /** Time spent performing comprehensive Final QC verification before release. */
    FINAL_QC("Final QC Inspection"),

    /** Time spent on logging, reporting, checklist configuration, and administration. */
    ADMINISTRATIVE("QC Administration"),

    /** Other operational QC time expenditure. */
    OTHER("Other QC Activity");

    companion object {
        fun fromString(value: String?): QcTimeEntryType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
