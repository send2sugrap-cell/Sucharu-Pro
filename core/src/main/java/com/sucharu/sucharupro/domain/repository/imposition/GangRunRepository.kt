package com.sucharu.sucharupro.domain.repository.imposition

import com.sucharu.sucharupro.domain.model.imposition.GangRunSpecification
import com.sucharu.sucharupro.domain.model.imposition.GangRunStatus

/**
 * Domain Repository Interface for Gang-Run Specifications.
 * Module 18 Step 02.
 */
interface GangRunRepository {
    suspend fun save(specification: GangRunSpecification): GangRunSpecification
    suspend fun findById(tenantId: String, gangRunId: String): GangRunSpecification?
    suspend fun listAll(tenantId: String, limit: Int = 50, offset: Int = 0): List<GangRunSpecification>
    suspend fun updateStatus(tenantId: String, gangRunId: String, status: GangRunStatus, actor: String, notes: String? = null): Boolean
}
