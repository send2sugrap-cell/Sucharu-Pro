package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import kotlinx.coroutines.flow.Flow

/**
 * Data source contract for Production Job persistence.
 */
interface ProductionJobDataSource {
    fun observeJobs(): Flow<List<ProductionJob>>
    suspend fun fetchJobById(jobId: String): DomainResult<ProductionJob>
    suspend fun insertJob(job: ProductionJob): DomainResult<ProductionJob>
    suspend fun updateJob(job: ProductionJob): DomainResult<ProductionJob>

    fun observeAssignments(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment>>
    suspend fun insertAssignment(assignment: com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment>
    suspend fun updateAssignment(assignment: com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment>

    fun observeExecutions(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageExecution>>
    suspend fun insertExecution(execution: com.sucharu.sucharupro.domain.model.job.ProductionStageExecution): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionStageExecution>
    suspend fun updateExecution(execution: com.sucharu.sucharupro.domain.model.job.ProductionStageExecution): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionStageExecution>

    fun observeActivityEvents(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent>>
    suspend fun insertActivityEvent(event: com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent>

    fun observeOutputs(): Flow<List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>>
    suspend fun insertOutput(output: com.sucharu.sucharupro.domain.model.job.ProductionStageOutput): DomainResult<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput>
}
