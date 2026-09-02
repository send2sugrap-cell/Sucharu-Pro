package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Categorization of Continuous Quality Improvement actions (Module 06 Step 10).
 */
enum class QcImprovementActionType(
    val defaultLabel: String
) {
    CORRECTIVE_ACTION("Corrective Action (CAPA)"),
    PREVENTIVE_ACTION("Preventive Action"),
    PROCESS_IMPROVEMENT("Process Improvement"),
    TRAINING("Operator / Staff Training"),
    CHECKLIST_UPDATE("Checklist Update"),
    QUALITY_STANDARD_UPDATE("Quality Standard Update"),
    WORKFLOW_CHANGE("Workflow Change"),
    EQUIPMENT_REVIEW("Equipment Review / Calibration"),
    SUPPLIER_REVIEW("Supplier / Material Review"),
    OTHER("Other Improvement");

    companion object {
        fun fromString(value: String?): QcImprovementActionType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
