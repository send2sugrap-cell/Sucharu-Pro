package com.sucharu.sucharupro.domain.model.qc

/**
 * Canonical check categories for Pre-Production Quality Control (Module 06 Step 02).
 */
enum class PreProductionQcCategory(val defaultLabel: String) {
    JOB_SPECIFICATION("Job & Specification"),
    ARTWORK("Artwork Verification"),
    APPROVED_PROOF("Approved Proof Verification"),
    FINAL_APPROVAL("Final Approval State"),
    SIZE("Dimensions & Size"),
    CONTENT("Content & Text"),
    COLOR("Color & Modes"),
    BLEED_TRIM_SAFE_AREA("Bleed, Trim & Safe Area"),
    RESOLUTION("Resolution & Quality"),
    MATERIAL_SPECIFICATION("Material & Paper Specification"),
    FINISHING("Finishing Requirements")
}
