package com.sucharu.sucharupro.domain.model.qc

import com.sucharu.sucharupro.domain.model.common.FileReference

/**
 * Supporting evidence reference attached to a [ProductionDefect] (Module 06 Step 04).
 */
data class DefectEvidence(
    val evidenceId: String,
    val defectId: String,
    val fileReferenceId: String? = null,
    val fileReference: FileReference? = null,
    val description: String? = null,
    val createdBy: String,
    val createdAt: String
) {
    init {
        require(evidenceId.isNotBlank()) { "Evidence ID cannot be blank." }
        require(defectId.isNotBlank()) { "Defect ID cannot be blank." }
        require(createdBy.isNotBlank()) { "CreatedBy cannot be blank." }
        require(createdAt.isNotBlank()) { "CreatedAt cannot be blank." }
    }
}
