package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.GangRunSpecification
import com.sucharu.sucharupro.domain.model.imposition.GangRunStatus

/**
 * Data source interface for Gang-Run Batch Specifications.
 * Module 18 Step 02.
 */
interface GangRunDataSource {
    suspend fun saveGangRunSpecification(specification: GangRunSpecification): GangRunSpecification
    suspend fun getGangRunSpecification(tenantId: String, gangRunId: String): GangRunSpecification?
    suspend fun listGangRunSpecifications(tenantId: String, limit: Int = 50, offset: Int = 0): List<GangRunSpecification>
    suspend fun updateGangRunStatus(tenantId: String, gangRunId: String, status: GangRunStatus, actor: String, notes: String?): Boolean
}
