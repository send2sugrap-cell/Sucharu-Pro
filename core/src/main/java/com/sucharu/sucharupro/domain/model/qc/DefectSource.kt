package com.sucharu.sucharupro.domain.model.qc

/**
 * Origin and detection source for a QC defect (Module 06 Step 04).
 */
enum class DefectSource(
    val defaultLabel: String
) {
    PRE_PRODUCTION_QC("Pre-Production QC"),
    PRODUCTION_STAGE("Production Stage In-Line"),
    FINAL_QC("Final QC Inspection"),
    CHECKLIST_INSPECTION("Checklist Inspection"),
    OPERATOR_REPORTED("Operator Reported"),
    SUPERVISOR_REPORTED("Supervisor Reported"),
    OTHER("Other Source");

    companion object {
        fun fromString(value: String?): DefectSource? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
