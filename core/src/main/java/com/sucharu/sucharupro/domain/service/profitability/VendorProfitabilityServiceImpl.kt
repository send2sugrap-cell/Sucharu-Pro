package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.VendorProfitabilityRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.VendorProfitabilityValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Production implementation of VendorProfitabilityService.
 * Module 16 Step 05.
 */
class VendorProfitabilityServiceImpl(
    private val repository: VendorProfitabilityRepository,
    private val sourceCollector: VendorProfitabilitySourceCollector = VendorProfitabilitySourceCollectorImpl(),
    private val reconciliationService: VendorProfitabilityReconciliationService = VendorProfitabilityReconciliationServiceImpl(),
    private val rankingService: VendorProfitabilityRankingService = VendorProfitabilityRankingServiceImpl(),
    private val baselineProvider: VendorCostEstimationBaselineProvider? = null
) : VendorProfitabilityService {

    private val locks = ConcurrentHashMap<String, Mutex>()
    private val idempotencyCache = ConcurrentHashMap<String, VendorProfitabilitySnapshot>()

    private fun getLock(tenantId: String, vendorId: String): Mutex {
        return locks.computeIfAbsent("$tenantId:$vendorId") { Mutex() }
    }

    override suspend fun calculateVendorProfitability(
        tenantId: String,
        projectId: String,
        vendorId: String,
        vendorName: String?,
        vendorCode: String?,
        serviceCategory: String?,
        periodId: String?,
        periodStart: Long?,
        periodEnd: Long?,
        customCosts: List<VendorCostAttribution>?,
        customRevenueContext: List<VendorRevenueContextAttribution>?,
        customBaselineCost: BigDecimal?,
        idempotencyKey: String?
    ): DomainResult<VendorProfitabilitySnapshot> {
        val valResult = VendorProfitabilityValidator.validateCalculateRequest(tenantId, projectId, vendorId, customBaselineCost)
        if (valResult is DomainResult.Error) return valResult

        if (idempotencyKey != null) {
            val cached = idempotencyCache["$tenantId:$vendorId:$idempotencyKey"]
            if (cached != null) return DomainResult.Success(cached)
        }

        return getLock(tenantId, vendorId).withLock {
            if (idempotencyKey != null) {
                val cached = idempotencyCache["$tenantId:$vendorId:$idempotencyKey"]
                if (cached != null) return@withLock DomainResult.Success(cached)
            }

            val sourceRes = sourceCollector.collectVendorData(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                customCosts = customCosts,
                customRevenueContext = customRevenueContext,
                periodStart = periodStart,
                periodEnd = periodEnd
            )

            if (sourceRes is DomainResult.Error) return@withLock sourceRes
            val sourceData = (sourceRes as DomainResult.Success).data

            // Baseline Cost Resolution (Custom -> Future Module 17 Provider)
            val baseline = customBaselineCost ?: baselineProvider?.getEstimatedVendorBaselineCost(tenantId, projectId, vendorId, periodId)
            val (costVarAmount, costVarPct) = VendorProfitabilityMathUtils.calculateCostVariance(sourceData.totalVendorCost, baseline)

            // Operational & Unit Metrics
            val costPerJob = VendorProfitabilityMathUtils.calculateCostPerJob(sourceData.totalVendorCost, sourceData.jobSummaries.size)
            val costPerUnit = VendorProfitabilityMathUtils.calculateCostPerUnit(sourceData.totalVendorCost, sourceData.totalQuantity)
            val costSharePct = VendorProfitabilityMathUtils.calculateCostSharePercentage(sourceData.totalVendorCost, sourceData.attributedTotalJobCost)
            val costToRevPct = VendorProfitabilityMathUtils.calculateCostToRevenueContextPercentage(sourceData.totalVendorCost, sourceData.attributedRevenueContext)
            val fulfillmentProfitImpact = VendorProfitabilityMathUtils.calculateFulfillmentProfitabilityImpact(sourceData.attributedRevenueContext, sourceData.attributedTotalJobCost)

            val reworkRate = VendorProfitabilityMathUtils.calculateReworkRate(sourceData.reworkCount, sourceData.jobSummaries.size)
            val qualityFailureRate = VendorProfitabilityMathUtils.calculateQualityFailureRate(sourceData.qualityFailureCount, sourceData.jobSummaries.size)

            // Efficiency Scoring & Risk Analysis
            val efficiencyScoreBreakdown = VendorProfitabilityMathUtils.calculateEfficiencyScoreBreakdown(
                costVariancePercentage = costVarPct,
                reworkRate = reworkRate,
                qualityFailureRate = qualityFailureRate,
                disputeCount = sourceData.disputeCount,
                outstandingExposure = sourceData.outstandingExposure,
                totalVendorCost = sourceData.totalVendorCost
            )

            val (riskClass, riskReasons) = VendorProfitabilityMathUtils.classifyRisk(
                efficiencyScore = efficiencyScoreBreakdown.totalScore,
                costVariancePercentage = costVarPct,
                reworkCount = sourceData.reworkCount,
                disputeCount = sourceData.disputeCount,
                qualityFailureCount = sourceData.qualityFailureCount
            )

            // Previous Snapshot & Trend
            val prevSnapshot = repository.findLatestSnapshotByVendorId(tenantId, vendorId)
            val trend = VendorProfitabilityMathUtils.determineTrend(sourceData.totalVendorCost, prevSnapshot?.totalVendorCost)

            // Dependency Classification vs Total Known Spend
            val allSnaps = repository.listSnapshots(tenantId, VendorProfitabilityFilter(limit = 100))
            val totalSystemSpend = allSnaps.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.totalVendorCost) }.add(sourceData.totalVendorCost)
            val (depClass, depShare) = VendorProfitabilityMathUtils.classifyDependency(sourceData.totalVendorCost, totalSystemSpend)

            val snapshotId = UUID.randomUUID().toString()

            val integrityHash = VendorProfitabilityMathUtils.calculateSnapshotIntegrityHash(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                periodId = periodId,
                totalVendorCost = sourceData.totalVendorCost,
                paidVendorCost = sourceData.paidVendorCost,
                outstandingExposure = sourceData.outstandingExposure,
                attributedRevenueContext = sourceData.attributedRevenueContext,
                efficiencyScore = efficiencyScoreBreakdown.totalScore,
                fingerprints = sourceData.provenanceFingerprints
            )

            val snapshot = VendorProfitabilitySnapshot(
                snapshotId = snapshotId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                vendorName = vendorName ?: "Vendor $vendorId",
                vendorCode = vendorCode,
                serviceCategory = serviceCategory ?: "GENERAL_OUTSOURCE",
                vendorStatus = "ACTIVE",
                periodId = periodId,
                periodStart = periodStart,
                periodEnd = periodEnd,
                currency = "BDT",
                generatedAt = System.currentTimeMillis(),
                totalVendorCost = sourceData.totalVendorCost,
                directVendorCost = sourceData.directVendorCost,
                paidVendorCost = sourceData.paidVendorCost,
                outstandingExposure = sourceData.outstandingExposure,
                unbilledEstimateCost = sourceData.unbilledEstimateCost,
                reworkCost = sourceData.reworkCost,
                baselineCost = baseline,
                costVariance = costVarAmount,
                costVariancePercentage = costVarPct,
                attributedRevenueContext = sourceData.attributedRevenueContext,
                attributedTotalJobCost = sourceData.attributedTotalJobCost,
                fulfillmentProfitabilityImpact = fulfillmentProfitImpact,
                costToRevenueContextPercentage = costToRevPct,
                vendorCostSharePercentage = costSharePct,
                attributedWorkOrderCount = sourceData.workOrderSummaries.size,
                attributedJobCount = sourceData.jobSummaries.size,
                attributedProductCount = sourceData.productSummaries.size,
                attributedCustomerCount = sourceData.customerSummaries.size,
                totalAttributedQuantity = sourceData.totalQuantity,
                costPerJob = costPerJob,
                costPerUnit = costPerUnit,
                qualityFailureCount = sourceData.qualityFailureCount,
                reworkCount = sourceData.reworkCount,
                rejectionCount = sourceData.rejectionCount,
                disputeCount = sourceData.disputeCount,
                qualityFailureRate = qualityFailureRate,
                reworkRate = reworkRate,
                efficiencyScore = efficiencyScoreBreakdown.totalScore,
                efficiencyFactors = efficiencyScoreBreakdown.explanations,
                riskClassification = riskClass,
                riskReasons = riskReasons,
                dependencyClassification = depClass,
                dependencySharePercentage = depShare,
                trendDirection = trend,
                costBreakdown = sourceData.costBreakdown,
                dataReadiness = sourceData.sourceReadiness,
                provenanceFingerprints = sourceData.provenanceFingerprints,
                integrityHash = integrityHash,
                warnings = sourceData.warnings
            )

            // Save Snapshot, Attributions, Unattributed items and Audit
            repository.saveSnapshot(snapshot)
            if (sourceData.costAttributions.isNotEmpty()) {
                repository.saveCostAttributions(sourceData.costAttributions)
            }
            if (sourceData.revenueContextAttributions.isNotEmpty()) {
                repository.saveRevenueContextAttributions(sourceData.revenueContextAttributions)
            }
            if (sourceData.unattributedItems.isNotEmpty()) {
                repository.saveUnattributedItems(sourceData.unattributedItems)
            }

            repository.saveAuditEvent(
                VendorProfitabilityAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    action = "CALCULATE_VENDOR_PROFITABILITY",
                    actorId = "SYSTEM",
                    actorRole = "SYSTEM",
                    details = "Generated vendor economics snapshot $snapshotId. Total Cost: ${snapshot.totalVendorCost}, Efficiency: ${snapshot.efficiencyScore}",
                    integrityHash = integrityHash
                )
            )

            if (idempotencyKey != null) {
                idempotencyCache["$tenantId:$vendorId:$idempotencyKey"] = snapshot
            }

            DomainResult.Success(snapshot)
        }
    }

    override suspend fun getLatestSnapshot(tenantId: String, vendorId: String): DomainResult<VendorProfitabilitySnapshot?> {
        return DomainResult.Success(repository.findLatestSnapshotByVendorId(tenantId, vendorId))
    }

    override suspend fun getSnapshotById(tenantId: String, snapshotId: String): DomainResult<VendorProfitabilitySnapshot?> {
        return DomainResult.Success(repository.findSnapshotById(tenantId, snapshotId))
    }

    override suspend fun listSnapshots(tenantId: String, filter: VendorProfitabilityFilter): DomainResult<List<VendorProfitabilitySnapshot>> {
        return DomainResult.Success(repository.listSnapshots(tenantId, filter))
    }

    override suspend fun getCostBreakdown(tenantId: String, vendorId: String): DomainResult<List<VendorCostBreakdownItem>> {
        val snap = repository.findLatestSnapshotByVendorId(tenantId, vendorId)
            ?: return DomainResult.Error(message = "No snapshot found for vendor $vendorId")
        return DomainResult.Success(snap.costBreakdown)
    }

    override suspend fun getCostAttributions(tenantId: String, vendorId: String): DomainResult<List<VendorCostAttribution>> {
        return DomainResult.Success(repository.listCostAttributionsByVendorId(tenantId, vendorId))
    }

    override suspend fun getRevenueContextAttributions(tenantId: String, vendorId: String): DomainResult<List<VendorRevenueContextAttribution>> {
        return DomainResult.Success(repository.listRevenueContextByVendorId(tenantId, vendorId))
    }

    override suspend fun getProvenance(tenantId: String, vendorId: String): DomainResult<Pair<List<VendorCostAttribution>, List<VendorRevenueContextAttribution>>> {
        val costs = repository.listCostAttributionsByVendorId(tenantId, vendorId)
        val revs = repository.listRevenueContextByVendorId(tenantId, vendorId)
        return DomainResult.Success(Pair(costs, revs))
    }

    override suspend fun reconcile(tenantId: String, projectId: String, vendorId: String, snapshotId: String?): DomainResult<VendorProfitabilityReconciliationEvent> {
        val snap = if (snapshotId != null) {
            repository.findSnapshotById(tenantId, snapshotId)
        } else {
            repository.findLatestSnapshotByVendorId(tenantId, vendorId)
        } ?: return DomainResult.Error(message = "Snapshot not found for reconciliation")

        val costAttrs = repository.listCostAttributionsByVendorId(tenantId, vendorId)
        val revAttrs = repository.listRevenueContextByVendorId(tenantId, vendorId)

        val sourceRes = sourceCollector.collectVendorData(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            customCosts = costAttrs,
            customRevenueContext = revAttrs
        )
        val sourceData = (sourceRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Failed to retrieve source data for reconciliation")

        val reconEvent = reconciliationService.reconcile(snap, sourceData)
        repository.saveReconciliationEvent(reconEvent)

        repository.saveAuditEvent(
            VendorProfitabilityAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                action = "RECONCILE_VENDOR_PROFITABILITY",
                actorId = "SYSTEM",
                actorRole = "SYSTEM",
                details = "Executed non-mutating reconciliation for snapshot ${snap.snapshotId}. Balanced: ${reconEvent.isBalanced}",
                integrityHash = snap.integrityHash
            )
        )

        return DomainResult.Success(reconEvent)
    }

    override suspend fun listAuditEvents(tenantId: String, vendorId: String): DomainResult<List<VendorProfitabilityAuditEvent>> {
        return DomainResult.Success(repository.listAuditEventsByVendorId(tenantId, vendorId))
    }

    override suspend fun listUnattributedItems(tenantId: String, vendorId: String?): DomainResult<List<VendorUnattributedItem>> {
        return DomainResult.Success(repository.listUnattributedItems(tenantId, vendorId))
    }

    override suspend fun rankVendors(tenantId: String, criteria: VendorRankingCriteria, ascending: Boolean, limit: Int): DomainResult<List<VendorRankingItem>> {
        val snapshots = repository.listSnapshots(tenantId, VendorProfitabilityFilter(limit = 100))
        val ranked = rankingService.rankVendors(snapshots, criteria, ascending, limit)
        return DomainResult.Success(ranked)
    }

    override suspend fun analyzeConcentration(tenantId: String, projectId: String, periodId: String?): DomainResult<VendorConcentrationAnalysis> {
        val snapshots = repository.listSnapshots(tenantId, VendorProfitabilityFilter(periodId = periodId, limit = 100))
        val analysis = rankingService.analyzeConcentration(tenantId, projectId, snapshots, periodId)
        return DomainResult.Success(analysis)
    }

    override suspend fun compareVendors(tenantId: String, vendorIds: List<String>): DomainResult<List<VendorComparisonItem>> {
        val snapshots = repository.listSnapshots(tenantId, VendorProfitabilityFilter(limit = 100))
        val compared = rankingService.compareVendors(snapshots, vendorIds)
        return DomainResult.Success(compared)
    }
}
