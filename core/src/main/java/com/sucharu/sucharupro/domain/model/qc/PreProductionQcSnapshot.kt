package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable historical specification snapshot recorded during Pre-Production QC inspection (Module 06 Step 02).
 */
data class PreProductionQcSnapshot(
    val snapshotId: String,
    val qcId: String,
    val productionJobId: String,
    val jobTitle: String? = null,
    val quantity: Int? = null,
    val width: Double? = null,
    val height: Double? = null,
    val unit: String? = null,
    val colorSpecification: String? = null,
    val materialSpecification: String? = null,
    val finishingSpecification: String? = null,
    val bleed: String? = null,
    val trim: String? = null,
    val safeArea: String? = null,
    val resolution: String? = null,
    val artworkId: String? = null,
    val artworkVersionId: String? = null,
    val proofId: String? = null,
    val proofVersionId: String? = null,
    val approvalId: String? = null,
    val inspectedAt: String,
    val inspectedBy: String,
    val inspectedByName: String? = null
) {
    init {
        require(snapshotId.isNotBlank()) { "Snapshot ID cannot be blank." }
        require(qcId.isNotBlank()) { "QC ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(inspectedAt.isNotBlank()) { "Inspected timestamp cannot be blank." }
        require(inspectedBy.isNotBlank()) { "InspectedBy cannot be blank." }
    }
}
