package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.ProfitabilityRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.ProfitabilityValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Production implementation of ProfitabilityFoundationService.
 */
class ProfitabilityFoundationServiceImpl(
    private val repository: ProfitabilityRepository,
    private val handoffAdapter: Module16FinancialHandoffAdapter,
    private val sourceRegistry: ProfitabilitySourceRegistry,
    private val reconciliationService: ProfitabilityReconciliationService
) : ProfitabilityFoundationService {

    private val mutex = Mutex()
    private val idempotencyCache = ConcurrentHashMap<String, String>() // idempotencyKey -> snapshotId

    override suspend fun generateProfitabilitySnapshot(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope,
        targetEntityId: String?,
        periodId: String?,
        currency: String,
        customRevenue: BigDecimal?,
        customDirectCost: BigDecimal?,
        customIndirectCost: BigDecimal?,
        baselineCost: BigDecimal?,
        baselineRevenue: BigDecimal?,
        revenueProvenances: List<RevenueProvenance>,
        costAttributions: List<CostAttributionReference>,
        idempotencyKey: String?,
        actor: String
    ): DomainResult<ProfitabilitySnapshot> = mutex.withLock {
        val validation = ProfitabilityValidator.validateSnapshotGeneration(
            tenantId = tenantId,
            projectId = projectId,
            scope = scope,
            targetEntityId = targetEntityId,
            periodId = periodId,
            currency = currency
        )
        if (validation is DomainResult.Error) {
            return DomainResult.Error(message = validation.message)
        }

        // Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existingId = idempotencyCache[idempotencyKey]
            if (existingId != null) {
                val existing = repository.getSnapshotById(tenantId, projectId, existingId)
                if (existing is DomainResult.Success) {
                    return DomainResult.Success(existing.data)
                }
            }
        }

        // Check handoff contract if period is present
        var handoffVerified = true
        var handoffChecksum: String? = null
        var sourceIntegrity = SourceIntegrityStatus.VERIFIED
        val integrityNotes = mutableListOf<String>()

        var resolvedRevenue = customRevenue ?: BigDecimal.ZERO
        var resolvedDirectCost = customDirectCost ?: BigDecimal.ZERO
        var resolvedIndirectCost = customIndirectCost ?: BigDecimal.ZERO

        if (!periodId.isNullOrBlank()) {
            when (val handoffRes = handoffAdapter.getVerifiedFinancialHandoff(tenantId, projectId, periodId)) {
                is DomainResult.Success -> {
                    val handoff = handoffRes.data
                    handoffVerified = handoff.isLedgerBalanced
                    handoffChecksum = handoff.contract.closureCertificateChecksum
                    sourceIntegrity = handoff.integrityStatus
                    integrityNotes.addAll(handoff.validationNotes)

                    if (scope == ProfitabilityScope.PERIOD || scope == ProfitabilityScope.BUSINESS) {
                        if (customRevenue == null && handoff.contract.totalRecognizedRevenue.compareTo(BigDecimal.ZERO) > 0) {
                            resolvedRevenue = handoff.contract.totalRecognizedRevenue
                        }
                        if (customDirectCost == null && handoff.contract.totalDirectExpenses.compareTo(BigDecimal.ZERO) > 0) {
                            resolvedDirectCost = handoff.contract.totalDirectExpenses
                        }
                        if (customIndirectCost == null && handoff.contract.totalRecognizedCostAllocations.compareTo(BigDecimal.ZERO) > 0) {
                            resolvedIndirectCost = handoff.contract.totalRecognizedCostAllocations
                        }
                    }
                }
                is DomainResult.Error -> {
                    handoffVerified = false
                    sourceIntegrity = SourceIntegrityStatus.SOURCE_MISSING
                    integrityNotes.add("Financial handoff failed: ${handoffRes.message}")
                }
                DomainResult.Loading -> {
                    handoffVerified = false
                    sourceIntegrity = SourceIntegrityStatus.SOURCE_MISSING
                    integrityNotes.add("Financial handoff is loading")
                }
            }
        }

        // Check if revenue provenances were supplied and compute resolved revenue
        if (revenueProvenances.isNotEmpty() && customRevenue == null) {
            resolvedRevenue = revenueProvenances.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.recognizedAmount) }
        }

        // Check if cost attributions were supplied and compute resolved direct cost
        if (costAttributions.isNotEmpty() && customDirectCost == null) {
            resolvedDirectCost = costAttributions.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.attributableAmount) }
        }

        // Duplicate source check
        val duplicateErrors = sourceRegistry.detectDuplicateSources(revenueProvenances, costAttributions)
        if (duplicateErrors.isNotEmpty()) {
            sourceIntegrity = SourceIntegrityStatus.SOURCE_CONFLICT
            integrityNotes.addAll(duplicateErrors)
        }

        // Compute metrics deterministically
        val metrics = ProfitabilityMathUtils.computeMetrics(
            revenue = resolvedRevenue,
            directCost = resolvedDirectCost,
            indirectCost = resolvedIndirectCost,
            baselineCost = baselineCost,
            baselineRevenue = baselineRevenue
        )

        // Compute cost breakdowns
        val costBreakdowns = ProfitabilityMathUtils.aggregateCostBreakdowns(
            attributions = costAttributions,
            totalCost = metrics.totalCost
        )

        val snapshotId = "SNAP-${UUID.randomUUID()}"
        val snapshot = ProfitabilitySnapshot(
            id = snapshotId,
            tenantId = tenantId,
            projectId = projectId,
            scope = scope,
            targetEntityId = targetEntityId,
            periodId = periodId,
            currency = currency,
            metrics = metrics,
            costBreakdowns = costBreakdowns,
            revenueProvenances = revenueProvenances,
            costAttributions = costAttributions,
            calculationVersion = "1.0.0",
            sourceIntegrityStatus = sourceIntegrity,
            financialHandoffVerified = handoffVerified,
            handoffChecksum = handoffChecksum,
            integrityNotes = integrityNotes,
            generatedBy = actor
        )

        val saveRes = repository.saveSnapshot(snapshot)
        return when (saveRes) {
            is DomainResult.Success -> {
                if (!idempotencyKey.isNullOrBlank()) {
                    idempotencyCache[idempotencyKey] = snapshotId
                }

                // Record analytical audit event
                repository.recordAuditEvent(
                    ProfitabilityAuditEvent(
                        id = "AUD-${UUID.randomUUID()}",
                        tenantId = tenantId,
                        projectId = projectId,
                        snapshotId = snapshotId,
                        action = "SNAPSHOT_GENERATED",
                        scope = scope,
                        targetEntityId = targetEntityId,
                        outcome = "SUCCESS",
                        details = "Generated snapshot with revenue: ${metrics.revenue}, totalCost: ${metrics.totalCost}, grossProfit: ${metrics.grossProfit}, margin: ${metrics.grossMarginPercentage}%",
                        actor = actor
                    )
                )
                DomainResult.Success(saveRes.data)
            }
            is DomainResult.Error -> DomainResult.Error(message = saveRes.message)
            DomainResult.Loading -> DomainResult.Error(message = "Snapshot save is loading")
        }
    }

    override suspend fun getSnapshotById(
        tenantId: String,
        projectId: String,
        id: String
    ): DomainResult<ProfitabilitySnapshot> {
        return repository.getSnapshotById(tenantId, projectId, id)
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope?,
        targetEntityId: String?,
        periodId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<ProfitabilitySnapshot>> {
        return repository.listSnapshots(
            tenantId = tenantId,
            projectId = projectId,
            scope = scope,
            targetEntityId = targetEntityId,
            periodId = periodId,
            limit = limit,
            offset = offset
        )
    }

    override suspend fun reconcileSnapshot(
        tenantId: String,
        projectId: String,
        snapshotId: String,
        actor: String
    ): DomainResult<ProfitabilityReconciliationEvent> {
        val snapshotRes = repository.getSnapshotById(tenantId, projectId, snapshotId)
        if (snapshotRes is DomainResult.Error) {
            return DomainResult.Error(message = snapshotRes.message)
        }

        val snapshot = (snapshotRes as DomainResult.Success).data
        val reconRes = reconciliationService.reconcileSnapshot(snapshot, actor)
        if (reconRes is DomainResult.Success) {
            repository.recordReconciliationEvent(reconRes.data)
            repository.recordAuditEvent(
                ProfitabilityAuditEvent(
                    id = "AUD-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    snapshotId = snapshotId,
                    action = "SNAPSHOT_RECONCILED",
                    scope = snapshot.scope,
                    targetEntityId = snapshot.targetEntityId,
                    outcome = if (reconRes.data.isReconciled) "SUCCESS" else "DISCREPANCY_DETECTED",
                    details = "Reconciliation completed. isReconciled: ${reconRes.data.isReconciled}, discrepancies: ${reconRes.data.discrepancies.size}",
                    actor = actor
                )
            )
        }
        return reconRes
    }

    override suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<ProfitabilityReconciliationEvent>> {
        return repository.listReconciliationEvents(
            tenantId = tenantId,
            projectId = projectId,
            snapshotId = snapshotId,
            limit = limit,
            offset = offset
        )
    }

    override suspend fun getSourceReadiness(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): DomainResult<ProfitabilitySourceReadiness> {
        return sourceRegistry.evaluateSourceReadiness(tenantId, projectId, periodId)
    }

    override suspend fun getFinancialHandoffStatus(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<ValidatedFinancialHandoff> {
        return handoffAdapter.getVerifiedFinancialHandoff(tenantId, projectId, periodId)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<ProfitabilityAuditEvent>> {
        return repository.listAuditEvents(
            tenantId = tenantId,
            projectId = projectId,
            snapshotId = snapshotId,
            limit = limit,
            offset = offset
        )
    }
}
