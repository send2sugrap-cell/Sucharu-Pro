package com.sucharu.sucharupro.domain.model.qc

/**
 * Strongly typed failure reasons for Re-QC inspection rejections (Module 06 Step 06).
 */
enum class ReQcFailureReason(
    val defaultLabel: String
) {
    DEFECT_REMAINS("Defect Remains"),
    NEW_DEFECT_FOUND("New Defect Found"),
    REWORK_INCOMPLETE("Rework Incomplete"),
    SPECIFICATION_MISMATCH("Specification Mismatch"),
    QUALITY_STANDARD_NOT_MET("Quality Standard Not Met"),
    QUANTITY_REJECTED("Quantity Rejected"),
    CHECKLIST_FAILURE("Checklist Failure"),
    OTHER("Other");

    companion object {
        fun fromString(value: String?): ReQcFailureReason? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
