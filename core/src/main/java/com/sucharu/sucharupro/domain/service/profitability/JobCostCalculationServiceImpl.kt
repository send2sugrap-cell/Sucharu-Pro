package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.JobCostRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.JobCostValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Production implementation of JobCostCalculationService.
 */
class JobCostCalculationServiceImpl(
    private val repository: JobCostRepository,
    private val sourceCollector: JobCostSourceCollector,
    private val reconciliationService: JobCostReconciliationService,
    private val baselineProvider: JobCostEstimationBaselineProvider = DefaultJobCostEstimationBaselineProvider()
) : JobCostCalculationService {

    private val mutex = Mutex()
    private val idempotencyCache = ConcurrentHashMap<String, String>() // idempotencyKey -> snapshotId

    override suspend fun calculateJobActualCost(
        tenantId: String,
        projectId: String,
        jobId: String,
        jobNumber: String?,
        customerId: String?,
        productId: String?,
        jobQuantity: Int,
        customDirectCosts: List<JobCostComponent>?,
        customIndirectCosts: List<JobCostAllocationDetail>?,
        customEstimatedCost: BigDecimal?,
        idempotencyKey: String?,
        actor: String
    ): DomainResult<JobCostSnapshot> = mutex.withLock {
        val validation = JobCostValidator.validateJobCostCalculationRequest(tenantId, projectId, jobId, "BDT")
        if (validation is DomainResult.Error) {
            return DomainResult.Error(message = validation.message)
        }

        // Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existingSnapshotId = idempotencyCache[idempotencyKey]
            if (existingSnapshotId != null) {
                val existing = repository.getSnapshotById(tenantId, projectId, existingSnapshotId)
                if (existing is DomainResult.Success) {
                    return DomainResult.Success(existing.data)
                }
            }
        }

        // 1. Collect canonical cost items
        val collectionRes = sourceCollector.collectJobCosts(
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            customDirectCosts = customDirectCosts,
            customIndirectCosts = customIndirectCosts
        )

        if (collectionRes is DomainResult.Error) {
            return DomainResult.Error(message = collectionRes.message)
        }

        val collected = (collectionRes as DomainResult.Success).data

        // 2. Resolve estimated baseline cost
        var resolvedEstimatedCost = customEstimatedCost
        if (resolvedEstimatedCost == null) {
            val baselineRes = baselineProvider.getEstimatedCostBaseline(tenantId, projectId, jobId)
            if (baselineRes is DomainResult.Success) {
                resolvedEstimatedCost = baselineRes.data
            }
        }

        // 3. Compute Totals
        val totalDirectCost = JobCostMathUtils.calculateTotalDirectCost(collected.components)
        val totalIndirectCost = JobCostMathUtils.calculateTotalIndirectCost(collected.components)
        val totalActualCost = JobCostMathUtils.calculateTotalActualCost(totalDirectCost, totalIndirectCost)

        // 4. Compute Component Percentages
        val finalizedComponents = collected.components.map { comp ->
            val percentage = if (totalActualCost.compareTo(BigDecimal.ZERO) > 0) {
                comp.attributedAmount.multiply(BigDecimal("100")).divide(totalActualCost, JobCostMathUtils.SCALE, JobCostMathUtils.ROUNDING_MODE)
            } else {
                BigDecimal.ZERO.setScale(JobCostMathUtils.SCALE, JobCostMathUtils.ROUNDING_MODE)
            }
            comp.copy(percentageOfTotalCost = percentage)
        }

        // 5. Compute Variance
        val varianceResult = JobCostMathUtils.calculateVariance(
            actualCost = totalActualCost,
            estimatedCost = resolvedEstimatedCost
        )

        // 6. Generate Integrity Hash
        val componentHashes = finalizedComponents.map { "${it.componentType.name}:${it.attributedAmount}" }
        val integrityHash = JobCostMathUtils.generateIntegrityHash(
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            calculationVersion = "JOB_COST_ENGINE_V1",
            totalActualCost = totalActualCost,
            totalDirectCost = totalDirectCost,
            totalIndirectCost = totalIndirectCost,
            componentHashes = componentHashes
        )

        val snapshotId = "JOB-SNAP-${UUID.randomUUID()}"
        val snapshot = JobCostSnapshot(
            snapshotId = snapshotId,
            tenantId = tenantId,
            projectId = projectId,
            jobId = jobId,
            jobNumber = jobNumber,
            customerId = customerId,
            productId = productId,
            jobQuantity = jobQuantity,
            calculationVersion = "JOB_COST_ENGINE_V1",
            calculationTimestamp = System.currentTimeMillis(),
            currency = "BDT",
            totalActualCost = totalActualCost,
            totalDirectCost = totalDirectCost,
            totalIndirectCost = totalIndirectCost,
            estimatedCost = varianceResult.estimatedCost,
            costVariance = varianceResult.costVariance,
            costVariancePercentage = varianceResult.costVariancePercentage,
            varianceClassification = varianceResult.classification,
            readinessStatus = collected.readinessStatus,
            isReconciled = true,
            sourceCount = collected.provenances.size,
            duplicateSourceCount = collected.duplicateCount,
            unresolvedSourceCount = collected.unresolvedCount,
            costComponents = finalizedComponents,
            provenances = collected.provenances,
            allocations = collected.allocations,
            warnings = collected.warnings,
            integrityHash = integrityHash,
            generatedBy = actor
        )

        // 7. Save Snapshot
        val saveRes = repository.saveSnapshot(snapshot)
        return when (saveRes) {
            is DomainResult.Success -> {
                if (!idempotencyKey.isNullOrBlank()) {
                    idempotencyCache[idempotencyKey] = snapshotId
                }

                // 8. Reconcile Snapshot
                val reconRes = reconciliationService.reconcileJobCostSnapshot(snapshot, actor)
                if (reconRes is DomainResult.Success) {
                    repository.recordReconciliationEvent(reconRes.data)
                }

                // 9. Record Audit Event
                repository.recordAuditEvent(
                    JobCostAuditEvent(
                        eventId = "AUD-JOB-${UUID.randomUUID()}",
                        tenantId = tenantId,
                        projectId = projectId,
                        jobId = jobId,
                        snapshotId = snapshotId,
                        action = "CALCULATED",
                        actor = actor,
                        outcome = "SUCCESS",
                        details = "Job cost calculated. totalActual: $totalActualCost, direct: $totalDirectCost, indirect: $totalIndirectCost, variance: ${varianceResult.costVariance}",
                        timestamp = System.currentTimeMillis()
                    )
                )

                DomainResult.Success(saveRes.data)
            }
            is DomainResult.Error -> DomainResult.Error(message = saveRes.message)
            DomainResult.Loading -> DomainResult.Error(message = "Job cost calculation is loading")
        }
    }

    override suspend fun getJobActualCostSnapshot(
        tenantId: String,
        projectId: String,
        jobId: String
    ): DomainResult<JobCostSnapshot> {
        return repository.getLatestSnapshotByJobId(tenantId, projectId, jobId)
    }

    override suspend fun getJobCostSnapshotById(
        tenantId: String,
        projectId: String,
        snapshotId: String
    ): DomainResult<JobCostSnapshot> {
        return repository.getSnapshotById(tenantId, projectId, snapshotId)
    }

    override suspend fun listJobCostSnapshots(
        tenantId: String,
        projectId: String,
        jobId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<JobCostSnapshot>> {
        return repository.listSnapshots(tenantId, projectId, jobId, limit, offset)
    }

    override suspend fun reconcileJobCost(
        tenantId: String,
        projectId: String,
        snapshotId: String,
        actor: String
    ): DomainResult<JobCostReconciliationEvent> {
        val snapshotRes = repository.getSnapshotById(tenantId, projectId, snapshotId)
        if (snapshotRes is DomainResult.Error) {
            return DomainResult.Error(message = snapshotRes.message)
        }

        val snapshot = (snapshotRes as DomainResult.Success).data
        val reconRes = reconciliationService.reconcileJobCostSnapshot(snapshot, actor)
        if (reconRes is DomainResult.Success) {
            repository.recordReconciliationEvent(reconRes.data)
            repository.recordAuditEvent(
                JobCostAuditEvent(
                    eventId = "AUD-JOB-REC-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    jobId = snapshot.jobId,
                    snapshotId = snapshotId,
                    action = "RECONCILED",
                    actor = actor,
                    outcome = if (reconRes.data.isReconciled) "SUCCESS" else "DISCREPANCY_DETECTED",
                    details = "Reconciliation completed. isReconciled: ${reconRes.data.isReconciled}, discrepancies: ${reconRes.data.discrepancies.size}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return reconRes
    }

    override suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        jobId: String?,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<JobCostReconciliationEvent>> {
        return repository.listReconciliationEvents(tenantId, projectId, jobId, snapshotId, limit, offset)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        jobId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<JobCostAuditEvent>> {
        return repository.listAuditEvents(tenantId, projectId, jobId, limit, offset)
    }
}
