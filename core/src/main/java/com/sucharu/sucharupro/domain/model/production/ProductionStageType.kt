package com.sucharu.sucharupro.domain.model.production

/**
 * Canonical 13-stage production workflow for Sucharu Pro.
 *
 * This is the SINGLE authoritative definition of the production pipeline.
 * Do NOT define production stages anywhere else in the project.
 *
 * Stages flow in displayOrder sequence:
 * DESIGN → APPROVAL → QC → ITEM_APPROVAL → CTP → PRINTING →
 * LAMINATION → FOLDING → BINDING → FINAL_QC → PACKAGING → READY → DELIVERED
 *
 * Some stages can be skipped (e.g. LAMINATION for uncoated jobs, FOLDING for flat items).
 * QC stages (QC, FINAL_QC) support rework cycles: FAILED → REWORK → return to appropriate stage.
 */
enum class ProductionStageType(
    /** Human-readable label for display in UI. Do NOT hard-code stage names in Composables. */
    val defaultLabel: String,
    /** Canonical display order (1-indexed). Used for progress calculation. */
    val displayOrder: Int,
    /** Short code for compact display (e.g. pipeline tiles). */
    val shortCode: String,
    /** Whether this stage is a quality check point (supports rework cycles). */
    val isQcStage: Boolean = false,
    /** Whether this stage can be skipped for certain job types. */
    val canBeSkipped: Boolean = false
) {
    /** 1. Design & artwork preparation by the design team. */
    DESIGN(
        defaultLabel = "Design",
        displayOrder = 1,
        shortCode = "DSN"
    ),

    /** 2. Customer approval of design proof. */
    APPROVAL(
        defaultLabel = "Approval",
        displayOrder = 2,
        shortCode = "APR"
    ),

    /** 3. Internal quality check of approved design before plate-making. */
    QC(
        defaultLabel = "QC",
        displayOrder = 3,
        shortCode = "QC",
        isQcStage = true
    ),

    /** 4. Final item/specification confirmation before production commit. */
    ITEM_APPROVAL(
        defaultLabel = "Item Approval",
        displayOrder = 4,
        shortCode = "IA"
    ),

    /** 5. Computer-To-Plate (CTP) plate-making for offset printing jobs. Skipped for digital-only jobs. */
    CTP(
        defaultLabel = "CTP",
        displayOrder = 5,
        shortCode = "CTP",
        canBeSkipped = true
    ),

    /** 6. Actual printing process (offset, digital, large format, screen, etc.). */
    PRINTING(
        defaultLabel = "Printing",
        displayOrder = 6,
        shortCode = "PRT"
    ),

    /** 7. Lamination (matte, gloss, soft-touch). Skipped for uncoated or plain paper jobs. */
    LAMINATION(
        defaultLabel = "Lamination",
        displayOrder = 7,
        shortCode = "LAM",
        canBeSkipped = true
    ),

    /** 8. Folding/creasing (brochures, leaflets, etc.). Skipped for flat items. */
    FOLDING(
        defaultLabel = "Folding",
        displayOrder = 8,
        shortCode = "FLD",
        canBeSkipped = true
    ),

    /** 9. Binding (saddle stitch, perfect bind, wire-o, hardcover). Skipped for single-sheet/flat jobs. */
    BINDING(
        defaultLabel = "Binding",
        displayOrder = 9,
        shortCode = "BND",
        canBeSkipped = true
    ),

    /** 10. Final quality control check before packaging. Supports rework cycles. */
    FINAL_QC(
        defaultLabel = "Final QC",
        displayOrder = 10,
        shortCode = "FQC",
        isQcStage = true
    ),

    /** 11. Packaging and wrapping for delivery. */
    PACKAGING(
        defaultLabel = "Packaging",
        displayOrder = 11,
        shortCode = "PKG"
    ),

    /** 12. Job completed, ready for dispatch/pickup. */
    READY(
        defaultLabel = "Ready",
        displayOrder = 12,
        shortCode = "RDY"
    ),

    /** 13. Delivered to customer or dispatched via challan. */
    DELIVERED(
        defaultLabel = "Delivered",
        displayOrder = 13,
        shortCode = "DLV"
    );

    companion object {
        /** Total number of stages in the production pipeline. */
        val TOTAL_STAGES: Int = entries.size

        /** All stages ordered by display sequence. */
        val orderedStages: List<ProductionStageType> = entries.sortedBy { it.displayOrder }

        /** Active production stages (excludes terminal states). */
        val activeProductionStages: List<ProductionStageType> = entries.filter {
            it != READY && it != DELIVERED
        }

        /** QC checkpoint stages. */
        val qcStages: List<ProductionStageType> = entries.filter { it.isQcStage }

        /**
         * Calculates progress as a fraction (0.0 to 1.0) for a given stage.
         * Returns 0.0 for null (not started), 1.0 for DELIVERED.
         */
        fun progressFraction(currentStage: ProductionStageType?): Float {
            if (currentStage == null) return 0f
            return currentStage.displayOrder.toFloat() / TOTAL_STAGES.toFloat()
        }
    }
}
