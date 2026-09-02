package com.sucharu.sucharupro.domain.model.qc

/**
 * Structured reasons justifying a rework request (Module 06 Step 05).
 */
enum class ReworkReason(
    val defaultLabel: String
) {
    DEFECT_CORRECTION(
        defaultLabel = "Defect Correction"
    ),
    FAILED_QC(
        defaultLabel = "Failed QC Inspection"
    ),
    PRINT_ERROR(
        defaultLabel = "Print Error"
    ),
    MATERIAL_ERROR(
        defaultLabel = "Material Error"
    ),
    FINISHING_ERROR(
        defaultLabel = "Finishing Error"
    ),
    MACHINE_PROCESS_ERROR(
        defaultLabel = "Machine/Process Error"
    ),
    HUMAN_ERROR(
        defaultLabel = "Human Error"
    ),
    SPECIFICATION_MISMATCH(
        defaultLabel = "Specification Mismatch"
    ),
    CUSTOMER_CORRECTION(
        defaultLabel = "Customer Correction"
    ),
    OTHER(
        defaultLabel = "Other"
    );

    companion object {
        fun fromString(value: String?): ReworkReason? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
