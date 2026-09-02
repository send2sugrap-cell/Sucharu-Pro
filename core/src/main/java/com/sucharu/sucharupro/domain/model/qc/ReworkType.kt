package com.sucharu.sucharupro.domain.model.qc

/**
 * Corrective rework action classification (Module 06 Step 05).
 *
 * Describes the physical or technical work required to correct a detected defect.
 */
enum class ReworkType(
    val defaultLabel: String
) {
    PRINT_CORRECTION(
        defaultLabel = "Print Correction"
    ),
    COLOR_CORRECTION(
        defaultLabel = "Color Correction"
    ),
    CONTENT_CORRECTION(
        defaultLabel = "Content Correction"
    ),
    REGISTRATION_CORRECTION(
        defaultLabel = "Registration Correction"
    ),
    CUTTING_CORRECTION(
        defaultLabel = "Cutting Correction"
    ),
    FOLDING_CORRECTION(
        defaultLabel = "Folding Correction"
    ),
    BINDING_CORRECTION(
        defaultLabel = "Binding Correction"
    ),
    LAMINATION_CORRECTION(
        defaultLabel = "Lamination Correction"
    ),
    SPOT_UV_CORRECTION(
        defaultLabel = "Spot UV Correction"
    ),
    FINISHING_CORRECTION(
        defaultLabel = "Finishing Correction"
    ),
    REPRINT(
        defaultLabel = "Reprint"
    ),
    OTHER(
        defaultLabel = "Other"
    );

    companion object {
        fun fromString(value: String?): ReworkType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
