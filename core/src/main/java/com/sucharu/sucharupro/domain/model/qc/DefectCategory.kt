package com.sucharu.sucharupro.domain.model.qc

/**
 * Domain-specific printing industry defect categories (Module 06 Step 04).
 */
enum class DefectCategory(
    val defaultLabel: String,
    val description: String
) {
    CONTENT_ERROR("Content Error", "Typographical, copy, barcode, or content mistake."),
    COLOR_MISMATCH("Color Mismatch", "Color deviation, delta-E out of tolerance, or density mismatch."),
    PRINT_QUALITY("Print Quality", "Hickeys, streaks, scumming, banding, or ghosting."),
    REGISTRATION_ERROR("Registration Error", "Misaligned color plates, traps, or print-to-cut alignment."),
    PAPER_OR_MATERIAL("Paper / Material", "Substrate defect, wrong stock, grain direction, or moisture issue."),
    SIZE_ERROR("Size Error", "Incorrect trim dimensions, margin, or bleed."),
    CUTTING_ERROR("Cutting Error", "Guillotine drift, ragged edges, or incorrect cut lines."),
    FOLDING_ERROR("Folding Error", "Crooked fold, buckled sheet, or cracking on spine."),
    BINDING_ERROR("Binding Error", "Defective stitching, glue failure, missing signatures, or loose pages."),
    LAMINATION_ERROR("Lamination Error", "Delamination, bubbles, silvering, or wrinkles."),
    DIE_CUT_ERROR("Die-Cut Error", "Improper crease depth, blunt blade tears, or nick placement."),
    FOIL_ERROR("Foil Error", "Incomplete foil transfer, flaking, or foil dusting."),
    SPOT_UV_ERROR("Spot UV Error", "Misregistered UV coat, pinholes, or improper curing."),
    FINISHING_ERROR("Finishing Error", "General post-press, embossing, or debossing defects."),
    ARTWORK_ERROR("Artwork Error", "Low resolution images, missing fonts, or wrong color space."),
    MACHINE_ERROR("Machine Error", "Press jam, feeder stoppage, or blanket breakdown damage."),
    PROCESS_ERROR("Process Error", "Operator error, missed procedural step, or wrong job setup."),
    QUANTITY_VARIANCE("Quantity Variance", "Shortage or overage beyond acceptable tolerance."),
    PACKAGING_ERROR("Packaging Error", "Damaged cartons, incorrect labeling, or improper strapping."),
    OTHER("Other", "Miscellaneous or unclassified defect.");

    companion object {
        fun fromString(value: String?): DefectCategory? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
