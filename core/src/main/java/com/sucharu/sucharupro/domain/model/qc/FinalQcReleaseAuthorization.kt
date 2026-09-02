package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable record representing the formal management authorization of a production release (Module 06 Step 07).
 *
 * Preserves the complete traceability chain from pre-production, defects, reworks, and re-QC cycles.
 */
data class FinalQcReleaseAuthorization(
    val releaseAuthorizationId: String,
    val projectId: String,
    val productionJobId: String,
    val productionJobItemId: String? = null,
    val finalQcId: String,
    val finalQcDecision: FinalQcDecision,
    val finalQcStatus: FinalQcStatus,
    val authorizedBy: String,
    val authorizedByName: String? = null,
    val authorizedAt: String,
    val releaseNotes: String? = null,
    val finalQcVersion: Int = 1,
    val preProductionQcId: String? = null,
    val checklistId: String? = null,
    val sourceReQcIds: List<String> = emptyList(),
    val sourceDefectIds: List<String> = emptyList(),
    val sourceReworkIds: List<String> = emptyList(),
    val createdAt: String
) {
    init {
        require(releaseAuthorizationId.isNotBlank()) { "Release Authorization ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(finalQcId.isNotBlank()) { "Final QC ID cannot be blank." }
        require(authorizedBy.isNotBlank()) { "AuthorizedBy cannot be blank." }
        require(authorizedAt.isNotBlank()) { "Authorized timestamp cannot be blank." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
    }
}
