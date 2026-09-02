package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignRevisionRequest
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Proof & Revision storage in Sucharu Pro ERP.
 */
interface DesignProofDataSource {
    fun observeProofs(): Flow<List<DesignProof>>
    suspend fun fetchProofById(proofId: String): DomainResult<DesignProof>
    suspend fun insertProof(proof: DesignProof): DomainResult<DesignProof>
    suspend fun updateProof(proof: DesignProof): DomainResult<DesignProof>

    fun observeVersions(): Flow<List<DesignProofVersion>>
    suspend fun insertVersion(version: DesignProofVersion): DomainResult<DesignProofVersion>
    suspend fun updateVersion(version: DesignProofVersion): DomainResult<DesignProofVersion>

    fun observeRevisions(): Flow<List<DesignRevisionRequest>>
    suspend fun insertRevision(revision: DesignRevisionRequest): DomainResult<DesignRevisionRequest>
    suspend fun updateRevision(revision: DesignRevisionRequest): DomainResult<DesignRevisionRequest>
}
