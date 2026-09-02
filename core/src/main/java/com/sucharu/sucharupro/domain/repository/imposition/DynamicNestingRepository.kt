package com.sucharu.sucharupro.domain.repository.imposition

import com.sucharu.sucharupro.domain.model.imposition.DynamicNestingSpecification
import com.sucharu.sucharupro.domain.model.imposition.NestingStatus

/**
 * Domain Repository interface for Dynamic 2D Nesting Specifications.
 * Module 18 Step 03.
 */
interface DynamicNestingRepository {
    suspend fun save(specification: DynamicNestingSpecification): DynamicNestingSpecification
    suspend fun findById(tenantId: String, nestingId: String): DynamicNestingSpecification?
    suspend fun listAll(tenantId: String, limit: Int = 50, offset: Int = 0): List<DynamicNestingSpecification>
    suspend fun updateStatus(tenantId: String, nestingId: String, status: NestingStatus, actor: String, notes: String? = null): Boolean
}
