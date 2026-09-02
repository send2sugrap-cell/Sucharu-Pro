package com.sucharu.sucharupro.domain.model.design

/**
 * Immutable domain entity representing a controlled authorization event for handing off an approved design
 * to Module 04 [com.sucharu.sucharupro.domain.model.job.ProductionJob] (Module 05 Step 05).
 */
data class DesignProductionHandoff(
    val handoffId: String,
    val projectId: String,
    val productionJobId: String,
    val artworkId: String,
    val artworkVersionId: String,
    val proofId: String,
    val proofVersionId: String,
    val approvalId: String,
    val authorizedBy: String,
    val authorizedByName: String? = null,
    val authorizedAt: String,
    val notes: String? = null
) {
    init {
        require(handoffId.isNotBlank()) { "Handoff ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(artworkId.isNotBlank()) { "Artwork ID cannot be blank." }
        require(artworkVersionId.isNotBlank()) { "Artwork Version ID cannot be blank." }
        require(proofId.isNotBlank()) { "Proof ID cannot be blank." }
        require(proofVersionId.isNotBlank()) { "Proof Version ID cannot be blank." }
        require(approvalId.isNotBlank()) { "Approval ID cannot be blank." }
        require(authorizedBy.isNotBlank()) { "Authorized By cannot be blank." }
        require(authorizedAt.isNotBlank()) { "Authorized timestamp cannot be blank." }
    }
}
