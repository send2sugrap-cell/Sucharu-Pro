package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.PeriodProfitabilityRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.PeriodProfitabilityValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID

/**
 * Production implementation of PeriodProfitabilityService.
 * Thread-safe with single-flight Mutex and idempotency support.
 * Module 16 Step 06.
 */
class PeriodProfitabilityServiceImpl(
    private val repository: PeriodProfitabilityRepository,
    private val sourceCollector: PeriodProfitabilitySourceCollector = PeriodProfitabilitySourceCollectorImpl(),
    private val reconciliationService: PeriodProfitabilityReconciliationService = PeriodProfitabilityReconciliationServiceImpl(),
    private val trendService: PeriodProfitabilityTrendService = PeriodProfitabilityTrendServiceImpl(),
    private val rankingService: PeriodProfitabilityRankingService = PeriodProfitabilityRankingServiceImpl()
) : PeriodProfitabilityService {

    private val calculationMutex = Mutex()

    override suspend fun calculatePeriodProfitability(
        tenantId: String,
        projectId: String,
        periodId: String,
        periodType: PeriodType,
        periodStart: Long,
        periodEnd: Long,
        timezone: String,
        periodKey: String,
        fiscalPeriodId: String?,
        customBaselineRevenue: BigDecimal?,
        customBaselineCost: BigDecimal?,
        customRevenueAttributions: List<PeriodRevenueAttributionItem>?,
        customCostBreakdown: List<PeriodCostBreakdownItem>?,
        customProvenance: List<PeriodProfitabilityProvenanceRecord>?,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<PeriodProfitabilitySnapshot> {
        val validation = PeriodProfitabilityValidator.validateCalculateRequest(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            customBaselineRevenue = customBaselineRevenue,
            customBaselineCost = customBaselineCost
        )
        if (validation is DomainResult.Error) return validation

        return calculationMutex.withLock {
            // Check idempotency
            if (!idempotencyKey.isNullOrBlank()) {
                val existingSnapId = repository.getIdempotentSnapshotId(tenantId, idempotencyKey)
                if (existingSnapId != null) {
                    val existingSnap = repository.findSnapshotById(tenantId, existingSnapId)
                    if (existingSnap != null) return@withLock DomainResult.Success(existingSnap)
                }
            }

            // 1. Source Collection
            val sourceRes = sourceCollector.collectPeriodData(
                tenantId = tenantId,
                projectId = projectId,
                periodId = periodId,
                periodType = periodType,
                periodStart = periodStart,
                periodEnd = periodEnd,
                customRevenueAttributions = customRevenueAttributions,
                customCostBreakdown = customCostBreakdown,
                customProvenance = customProvenance
            )
            val sourceData = when (sourceRes) {
                is DomainResult.Success -> sourceRes.data
                is DomainResult.Error -> return@withLock sourceRes
                DomainResult.Loading -> return@withLock DomainResult.Error(message = "Unexpected loading state during source collection")
            }

            // 2. Financial Metrics & Unit Economics
            val revenue = sourceData.recognizedRevenue
            val totalCost = sourceData.totalCost
            val grossProfit = PeriodProfitabilityMathUtils.calculateGrossProfit(revenue, totalCost)
            val grossMarginPct = PeriodProfitabilityMathUtils.calculateGrossMarginPercentage(grossProfit, revenue)
            val costToRevPct = PeriodProfitabilityMathUtils.calculateCostToRevenuePercentage(totalCost, revenue)
            val directCost = sourceData.directCost
            val indirectCost = sourceData.indirectCost
            val contributionAmt = PeriodProfitabilityMathUtils.calculateContributionAmount(revenue, directCost)
            val contribMarginPct = PeriodProfitabilityMathUtils.calculateContributionMarginPercentage(contributionAmt, revenue)

            // Baseline & Variance
            val (revVarAmt, revVarPct) = PeriodProfitabilityMathUtils.calculateVariance(revenue, customBaselineRevenue)
            val (costVarAmt, costVarPct) = PeriodProfitabilityMathUtils.calculateVariance(totalCost, customBaselineCost)
            val customBaselineProfit = if (customBaselineRevenue != null && customBaselineCost != null) {
                customBaselineRevenue - customBaselineCost
            } else null
            val (profitVarAmt, profitVarPct) = PeriodProfitabilityMathUtils.calculateVariance(grossProfit, customBaselineProfit)

            // Unit Economics
            val avgRevJob = PeriodProfitabilityMathUtils.calculateAverageRevenuePerJob(revenue, sourceData.jobCount)
            val avgProfitJob = PeriodProfitabilityMathUtils.calculateAverageProfitPerJob(grossProfit, sourceData.jobCount)
            val avgRevUnit = PeriodProfitabilityMathUtils.calculateAverageRevenuePerUnit(revenue, sourceData.totalUnits)
            val avgCostUnit = PeriodProfitabilityMathUtils.calculateAverageCostPerUnit(totalCost, sourceData.totalUnits)
            val avgProfitUnit = PeriodProfitabilityMathUtils.calculateAverageProfitPerUnit(grossProfit, sourceData.totalUnits)

            // Classifications
            val profClass = PeriodProfitabilityMathUtils.classifyProfitability(revenue, grossProfit, grossMarginPct)

            // Trend
            val previousSnap = repository.findLatestSnapshotByPeriodId(tenantId, periodId)
            val (trendDir, _) = PeriodProfitabilityMathUtils.determineTrend(
                currentProfit = grossProfit,
                previousProfit = previousSnap?.grossProfit,
                currentMargin = grossMarginPct,
                previousMargin = previousSnap?.grossMarginPercentage
            )

            // Provenance Fingerprints & Snapshot Integrity Hash
            val fingerprints = sourceData.provenanceRecords.map { it.fingerprint }
            val snapshotId = UUID.randomUUID().toString()
            val version = (previousSnap?.snapshotVersion ?: 0) + 1
            val calculationVersion = "MODULE16_PERIOD_PROFITABILITY_V1"

            val integrityHash = PeriodProfitabilityMathUtils.generateSnapshotIntegrityHash(
                tenantId = tenantId,
                projectId = projectId,
                periodId = periodId,
                periodType = periodType.name,
                periodStart = periodStart,
                periodEnd = periodEnd,
                revenue = revenue,
                totalCost = totalCost,
                grossProfit = grossProfit,
                calculationVersion = calculationVersion,
                provenanceFingerprints = fingerprints
            )

            val snapshot = PeriodProfitabilitySnapshot(
                snapshotId = snapshotId,
                tenantId = tenantId,
                projectId = projectId,
                periodId = periodId,
                periodType = periodType,
                periodStart = periodStart,
                periodEnd = periodEnd,
                timezone = timezone,
                periodKey = periodKey.ifBlank { "$periodType-$periodId" },
                fiscalPeriodId = fiscalPeriodId,
                periodStatus = PeriodStatus.OPEN,
                currency = "BDT",
                calculationVersion = calculationVersion,
                snapshotVersion = version,
                supersedesSnapshotId = previousSnap?.snapshotId,
                generatedAt = System.currentTimeMillis(),
                generatedBy = actorId,
                sourceAsOf = System.currentTimeMillis(),
                revenue = revenue,
                totalActualCost = totalCost,
                grossProfit = grossProfit,
                grossMarginPercentage = grossMarginPct,
                costToRevenuePercentage = costToRevPct,
                directCost = directCost,
                indirectCost = indirectCost,
                contributionAmount = contributionAmt,
                contributionMarginPercentage = contribMarginPct,
                baselineRevenue = customBaselineRevenue,
                baselineCost = customBaselineCost,
                revenueVariance = revVarAmt,
                revenueVariancePercentage = revVarPct,
                costVariance = costVarAmt,
                costVariancePercentage = costVarPct,
                profitVariance = profitVarAmt,
                profitVariancePercentage = profitVarPct,
                jobCount = sourceData.jobCount,
                completedJobCount = sourceData.completedJobCount,
                productCount = sourceData.productCount,
                customerCount = sourceData.customerCount,
                vendorCount = sourceData.vendorCount,
                totalUnits = sourceData.totalUnits,
                averageRevenuePerJob = avgRevJob,
                averageProfitPerJob = avgProfitJob,
                averageRevenuePerUnit = avgRevUnit,
                averageCostPerUnit = avgCostUnit,
                averageProfitPerUnit = avgProfitUnit,
                profitabilityClassification = profClass,
                trendDirection = trendDir,
                sourceReadiness = sourceData.sourceReadiness,
                costBreakdown = sourceData.costBreakdown,
                revenueAttributions = sourceData.revenueAttributions,
                provenanceFingerprints = fingerprints,
                integrityHash = integrityHash,
                warnings = sourceData.warnings
            )

            // Save Snapshot, Provenance, Unattributed & Audit
            val savedSnapshot = repository.saveSnapshot(snapshot)
            repository.saveProvenanceRecords(sourceData.provenanceRecords)
            if (sourceData.unattributedItems.isNotEmpty()) {
                repository.saveUnattributedItems(sourceData.unattributedItems)
            }

            if (!idempotencyKey.isNullOrBlank()) {
                repository.saveIdempotencyRecord(tenantId, idempotencyKey, snapshotId)
            }

            repository.saveAuditEvent(
                PeriodProfitabilityAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    periodId = periodId,
                    action = if (previousSnap == null) "PERIOD_SNAPSHOT_GENERATED" else "PERIOD_SNAPSHOT_RECALCULATED",
                    actorId = actorId,
                    actorRole = actorRole,
                    snapshotId = snapshotId,
                    calculationVersion = calculationVersion,
                    previousState = previousSnap?.snapshotId,
                    resultingState = snapshotId,
                    details = "Revenue: BDT $revenue | Cost: BDT $totalCost | Gross Profit: BDT $grossProfit",
                    integrityHash = integrityHash
                )
            )

            DomainResult.Success(savedSnapshot)
        }
    }

    override suspend fun getLatestSnapshot(tenantId: String, periodId: String): DomainResult<PeriodProfitabilitySnapshot?> {
        return DomainResult.Success(repository.findLatestSnapshotByPeriodId(tenantId, periodId))
    }

    override suspend fun getSnapshotById(tenantId: String, snapshotId: String): DomainResult<PeriodProfitabilitySnapshot?> {
        return DomainResult.Success(repository.findSnapshotById(tenantId, snapshotId))
    }

    override suspend fun listSnapshots(tenantId: String, filter: PeriodProfitabilityFilter): DomainResult<List<PeriodProfitabilitySnapshot>> {
        return DomainResult.Success(repository.listSnapshots(tenantId, filter))
    }

    override suspend fun getCostBreakdown(tenantId: String, periodId: String): DomainResult<List<PeriodCostBreakdownItem>> {
        val snap = repository.findLatestSnapshotByPeriodId(tenantId, periodId)
            ?: return DomainResult.Error(message = "Snapshot not found for period $periodId")
        return DomainResult.Success(snap.costBreakdown)
    }

    override suspend fun getRevenueBreakdown(tenantId: String, periodId: String): DomainResult<List<PeriodRevenueAttributionItem>> {
        val snap = repository.findLatestSnapshotByPeriodId(tenantId, periodId)
            ?: return DomainResult.Error(message = "Snapshot not found for period $periodId")
        return DomainResult.Success(snap.revenueAttributions)
    }

    override suspend fun getProvenance(tenantId: String, periodId: String): DomainResult<List<PeriodProfitabilityProvenanceRecord>> {
        return DomainResult.Success(repository.listProvenanceByPeriodId(tenantId, periodId))
    }

    override suspend fun reconcile(tenantId: String, projectId: String, periodId: String, snapshotId: String?): DomainResult<PeriodProfitabilityReconciliationEvent> {
        val snap = if (snapshotId != null) {
            repository.findSnapshotById(tenantId, snapshotId)
        } else {
            repository.findLatestSnapshotByPeriodId(tenantId, periodId)
        } ?: return DomainResult.Error(message = "Snapshot not found for reconciliation")

        val provenance = repository.listProvenanceByPeriodId(tenantId, periodId)
        val sourceRes = sourceCollector.collectPeriodData(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            periodType = snap.periodType,
            periodStart = snap.periodStart,
            periodEnd = snap.periodEnd,
            customRevenueAttributions = snap.revenueAttributions,
            customCostBreakdown = snap.costBreakdown,
            customProvenance = provenance
        )
        val sourceData = (sourceRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Failed to retrieve source data for reconciliation")

        val reconEvent = reconciliationService.reconcile(snap, sourceData)
        repository.saveReconciliationEvent(reconEvent)

        repository.saveAuditEvent(
            PeriodProfitabilityAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                periodId = periodId,
                action = "PERIOD_RECONCILIATION_EXECUTED",
                actorId = "SYSTEM",
                actorRole = "SYSTEM",
                snapshotId = snap.snapshotId,
                calculationVersion = snap.calculationVersion,
                details = "Balanced: ${reconEvent.isBalanced} | Discrepancies: ${reconEvent.errorDetails.size}",
                integrityHash = snap.integrityHash
            )
        )

        return DomainResult.Success(reconEvent)
    }

    override suspend fun comparePeriods(tenantId: String, currentPeriodId: String, previousPeriodId: String): DomainResult<PeriodComparisonResult> {
        val currentSnap = repository.findLatestSnapshotByPeriodId(tenantId, currentPeriodId)
            ?: return DomainResult.Error(message = "Current period snapshot not found: $currentPeriodId")
        val prevSnap = repository.findLatestSnapshotByPeriodId(tenantId, previousPeriodId)
            ?: return DomainResult.Error(message = "Previous period snapshot not found: $previousPeriodId")

        return DomainResult.Success(trendService.comparePeriods(currentSnap, prevSnap))
    }

    override suspend fun rankPeriods(tenantId: String, criteria: PeriodRankingCriteria, periodType: PeriodType?): DomainResult<List<PeriodRankingItem>> {
        val filter = PeriodProfitabilityFilter(periodType = periodType, limit = 1000)
        val list = repository.listSnapshots(tenantId, filter)
        return DomainResult.Success(rankingService.rankPeriods(list, criteria))
    }

    override suspend fun analyzeConcentration(tenantId: String, projectId: String, periodType: PeriodType, scopeLabel: String): DomainResult<PeriodConcentrationAnalysis> {
        val filter = PeriodProfitabilityFilter(periodType = periodType, limit = 1000)
        val list = repository.listSnapshots(tenantId, filter)
        return DomainResult.Success(rankingService.analyzeConcentration(tenantId, projectId, periodType, scopeLabel, list))
    }

    override suspend fun listAuditEvents(tenantId: String, periodId: String): DomainResult<List<PeriodProfitabilityAuditEvent>> {
        return DomainResult.Success(repository.listAuditEventsByPeriodId(tenantId, periodId))
    }

    override suspend fun listUnattributedItems(tenantId: String, periodId: String?): DomainResult<List<PeriodUnattributedItem>> {
        return DomainResult.Success(repository.listUnattributedItems(tenantId, periodId))
    }
}
