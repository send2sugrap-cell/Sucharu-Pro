package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.DynamicNestingSpecification
import com.sucharu.sucharupro.domain.model.imposition.NestingStatus

/**
 * Data source interface for Dynamic 2D Nesting Specifications.
 * Module 18 Step 03.
 */
interface DynamicNestingDataSource {
    suspend fun saveNestingSpecification(specification: DynamicNestingSpecification): DynamicNestingSpecification
    suspend fun getNestingSpecification(tenantId: String, nestingId: String): DynamicNestingSpecification?
    suspend fun listNestingSpecifications(tenantId: String, limit: Int = 50, offset: Int = 0): List<DynamicNestingSpecification>
    suspend fun updateNestingStatus(tenantId: String, nestingId: String, status: NestingStatus, actor: String, notes: String?): Boolean
}
