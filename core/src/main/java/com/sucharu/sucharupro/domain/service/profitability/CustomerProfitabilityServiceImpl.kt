package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.CustomerProfitabilityRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.CustomerProfitabilityValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Production implementation of CustomerProfitabilityService.
 *
 * Implements single-flight mutex protection, idempotency caching, non-mutating reconciliation,
 * deterministic SHA-256 hashing, and append-only audit tracking.
 */
class CustomerProfitabilityServiceImpl(
    private val repository: CustomerProfitabilityRepository,
    private val sourceCollector: CustomerProfitabilitySourceCollector,
    private val reconciliationService: CustomerProfitabilityReconciliationService,
    private val rankingService: CustomerProfitabilityRankingService = CustomerProfitabilityRankingServiceImpl()
) : CustomerProfitabilityService {

    private val mutex = Mutex()
    private val idempotencyCache = ConcurrentHashMap<String, String>() // idempotencyKey -> snapshotId

    override suspend fun calculateCustomerProfitability(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerName: String?,
        customerCode: String?,
        periodType: ProfitabilityPeriodType,
        periodStart: Long?,
        periodEnd: Long?,
        customRevenue: List<CustomerRevenueAttribution>?,
        customCosts: List<CustomerCostAttribution>?,
        previousPeriodMargin: BigDecimal?,
        idempotencyKey: String?,
        actor: String
    ): DomainResult<CustomerProfitabilitySnapshot> = mutex.withLock {
        val validation = CustomerProfitabilityValidator.validateCalculationRequest(tenantId, projectId, customerId, periodStart, periodEnd)
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

        // 1. Collect canonical customer revenue, costs, orders, jobs, and products
        val collectionRes = sourceCollector.collectCustomerData(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customRevenue = customRevenue,
            customCosts = customCosts,
            periodStart = periodStart,
            periodEnd = periodEnd
        )

        if (collectionRes is DomainResult.Error) {
            return DomainResult.Error(message = collectionRes.message)
        }

        val collected = (collectionRes as DomainResult.Success).data

        // 2. Compute Gross Profit & Gross Margin %
        val grossProfit = CustomerProfitabilityMathUtils.calculateGrossProfit(
            revenue = collected.totalRevenue,
            cost = collected.totalCost
        )

        val grossMarginPercentage = CustomerProfitabilityMathUtils.calculateGrossMarginPercentage(
            revenue = collected.totalRevenue,
            cost = collected.totalCost
        )

        // 3. Compute Contribution & Margin
        val contributionAmount = CustomerProfitabilityMathUtils.calculateContributionAmount(
            revenue = collected.totalRevenue,
            variableCost = collected.variableCost
        )

        val contributionMarginPercentage = CustomerProfitabilityMathUtils.calculateContributionMarginPercentage(
            revenue = collected.totalRevenue,
            variableCost = collected.variableCost
        )

        val costToRevenuePercentage = CustomerProfitabilityMathUtils.calculateCostToRevenuePercentage(
            totalCost = collected.totalCost,
            revenue = collected.totalRevenue
        )

        val contributionMetrics = CustomerContributionMetrics(
            attributableVariableCost = collected.variableCost,
            attributableFixedCost = collected.fixedCost,
            contributionAmount = contributionAmount,
            contributionMarginPercentage = contributionMarginPercentage,
            costToRevenuePercentage = costToRevenuePercentage
        )

        // 4. Compute Classification, Trend & Concentration
        val classification = CustomerProfitabilityMathUtils.classifyCustomerProfitability(
            revenue = collected.totalRevenue,
            totalCost = collected.totalCost,
            grossMarginPercentage = grossMarginPercentage
        )

        val trend = CustomerProfitabilityMathUtils.calculateTrend(
            currentMargin = grossMarginPercentage,
            previousMargin = previousPeriodMargin
        )

        val isLoss = grossProfit.compareTo(BigDecimal.ZERO) < 0
        val isLow = grossMarginPercentage != null && grossMarginPercentage > BigDecimal.ZERO && grossMarginPercentage < BigDecimal("15.0000")

        // 5. Generate SHA-256 Integrity Hash
        val snapshotId = "SNAP-CUST-${UUID.randomUUID()}"
        val calculationVersion = "CUSTOMER_PROFITABILITY_V1"
        val integrityHash = CustomerProfitabilityMathUtils.generateIntegrityHash(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            periodType = periodType.name,
            calculationVersion = calculationVersion,
            revenue = collected.totalRevenue,
            cost = collected.totalCost,
            grossProfit = grossProfit,
            contribution = contributionAmount,
            components = collected.costBreakdown,
            provenanceFingerprints = collected.provenanceFingerprints
        )

        val snapshot = CustomerProfitabilitySnapshot(
            snapshotId = snapshotId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerName = customerName ?: "Customer $customerId",
            customerCode = customerCode ?: "CUST-$customerId",
            periodType = periodType,
            periodStart = periodStart,
            periodEnd = periodEnd,
            recognizedRevenue = collected.totalRevenue,
            totalActualCost = collected.totalCost,
            grossProfit = grossProfit,
            grossMarginPercentage = grossMarginPercentage,
            contributionMetrics = contributionMetrics,
            operationalMetrics = collected.operationalMetrics,
            costBreakdown = collected.costBreakdown,
            profitabilityClassification = classification,
            trend = trend,
            concentrationRisk = CustomerConcentrationRisk.CONCENTRATION_LOW,
            isLossMaking = isLoss,
            isLowMargin = isLow,
            sourceIntegrityStatus = collected.sourceIntegrity,
            isReconciled = true,
            reconciliationDiscrepancy = BigDecimal.ZERO.setScale(4, CustomerProfitabilityMathUtils.ROUNDING_MODE),
            calculationVersion = calculationVersion,
            generatedAt = System.currentTimeMillis(),
            generatedBy = actor,
            integrityHash = integrityHash
        )

        // 6. Perform Non-mutating Reconciliation
        val reconRes = reconciliationService.reconcileCustomerSnapshot(
            snapshot = snapshot,
            revenueSources = collected.revenueAttributions,
            costSources = collected.costAttributions,
            actor = actor
        )

        val finalSnapshot = if (reconRes is DomainResult.Success) {
            val event = reconRes.data
            snapshot.copy(
                isReconciled = event.isReconciled,
                reconciliationDiscrepancy = (event.expectedRevenue.subtract(event.actualRevenue)).abs()
            )
        } else {
            snapshot.copy(isReconciled = false)
        }

        // 7. Persist Snapshot, Sources, Reconciliation & Audit Event
        repository.saveSnapshot(finalSnapshot)
        if (collected.revenueAttributions.isNotEmpty()) {
            repository.saveRevenueAttributions(collected.revenueAttributions)
        }
        if (collected.costAttributions.isNotEmpty()) {
            repository.saveCostAttributions(collected.costAttributions)
        }
        if (collected.unattributedItems.isNotEmpty()) {
            repository.saveUnattributedItems(collected.unattributedItems)
        }
        if (reconRes is DomainResult.Success) {
            repository.saveReconciliationEvent(reconRes.data)
        }

        repository.recordAuditEvent(
            CustomerProfitabilityAuditEvent(
                eventId = "AUDIT-${UUID.randomUUID()}",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                snapshotId = finalSnapshot.snapshotId,
                action = "CALCULATE_CUSTOMER_PROFITABILITY",
                actor = actor,
                actorRole = "STAFF",
                outcome = "SUCCESS",
                details = "Calculated customer profitability: Revenue=${finalSnapshot.recognizedRevenue}, Cost=${finalSnapshot.totalActualCost}, Profit=${finalSnapshot.grossProfit}, Margin=${finalSnapshot.grossMarginPercentage}%",
                timestamp = System.currentTimeMillis()
            )
        )

        if (!idempotencyKey.isNullOrBlank()) {
            idempotencyCache[idempotencyKey] = finalSnapshot.snapshotId
        }

        return DomainResult.Success(finalSnapshot)
    }

    override suspend fun getLatestSnapshot(tenantId: String, projectId: String, customerId: String): DomainResult<CustomerProfitabilitySnapshot?> {
        return repository.getLatestSnapshotByCustomer(tenantId, projectId, customerId)
    }

    override suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): DomainResult<CustomerProfitabilitySnapshot> {
        return repository.getSnapshotById(tenantId, projectId, snapshotId)
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, filter: CustomerProfitabilityFilter): DomainResult<List<CustomerProfitabilitySnapshot>> {
        return repository.listSnapshots(tenantId, projectId, filter)
    }

    override suspend fun getCostBreakdown(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerCostBreakdownItem>> {
        val snapRes = repository.getLatestSnapshotByCustomer(tenantId, projectId, customerId)
        if (snapRes is DomainResult.Success && snapRes.data != null) {
            return DomainResult.Success(snapRes.data!!.costBreakdown)
        }
        return DomainResult.Error(message = "No profitability snapshot found for customer $customerId")
    }

    override suspend fun getProvenance(tenantId: String, projectId: String, customerId: String): DomainResult<Pair<List<CustomerRevenueAttribution>, List<CustomerCostAttribution>>> {
        val revRes = repository.getRevenueAttributions(tenantId, projectId, customerId)
        val costRes = repository.getCostAttributions(tenantId, projectId, customerId)
        val rev = if (revRes is DomainResult.Success) revRes.data else emptyList()
        val cost = if (costRes is DomainResult.Success) costRes.data else emptyList()
        return DomainResult.Success(Pair(rev, cost))
    }

    override suspend fun getOrderProfitabilities(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerOrderProfitabilitySummary>> {
        val collectionRes = sourceCollector.collectCustomerData(tenantId, projectId, customerId)
        if (collectionRes is DomainResult.Success) {
            return DomainResult.Success(collectionRes.data.orderSummaries)
        }
        return DomainResult.Error(message = "Failed to retrieve order profitabilities for customer $customerId")
    }

    override suspend fun getJobProfitabilities(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerJobProfitabilitySummary>> {
        val collectionRes = sourceCollector.collectCustomerData(tenantId, projectId, customerId)
        if (collectionRes is DomainResult.Success) {
            return DomainResult.Success(collectionRes.data.jobSummaries)
        }
        return DomainResult.Error(message = "Failed to retrieve job profitabilities for customer $customerId")
    }

    override suspend fun getProductContributions(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerProductContributionSummary>> {
        val collectionRes = sourceCollector.collectCustomerData(tenantId, projectId, customerId)
        if (collectionRes is DomainResult.Success) {
            return DomainResult.Success(collectionRes.data.productSummaries)
        }
        return DomainResult.Error(message = "Failed to retrieve product contributions for customer $customerId")
    }

    override suspend fun getTrend(tenantId: String, projectId: String, customerId: String): DomainResult<CustomerProfitabilityTrend> {
        val snapRes = repository.getLatestSnapshotByCustomer(tenantId, projectId, customerId)
        if (snapRes is DomainResult.Success && snapRes.data != null) {
            return DomainResult.Success(snapRes.data!!.trend)
        }
        return DomainResult.Error(message = "No profitability snapshot found for customer $customerId")
    }

    override suspend fun reconcileCustomerProfitability(
        tenantId: String,
        projectId: String,
        customerId: String,
        snapshotId: String?,
        actor: String
    ): DomainResult<CustomerProfitabilityReconciliationEvent> {
        val snap = if (snapshotId != null) {
            val res = repository.getSnapshotById(tenantId, projectId, snapshotId)
            if (res is DomainResult.Success) res.data else null
        } else {
            val res = repository.getLatestSnapshotByCustomer(tenantId, projectId, customerId)
            if (res is DomainResult.Success) res.data else null
        }

        if (snap == null) {
            return DomainResult.Error(message = "Snapshot not found for customer $customerId")
        }

        val revList = when (val r = repository.getRevenueAttributions(tenantId, projectId, customerId)) {
            is DomainResult.Success -> r.data
            else -> emptyList()
        }
        val costList = when (val c = repository.getCostAttributions(tenantId, projectId, customerId)) {
            is DomainResult.Success -> c.data
            else -> emptyList()
        }

        val reconRes = reconciliationService.reconcileCustomerSnapshot(snap, revList, costList, actor)
        if (reconRes is DomainResult.Success) {
            repository.saveReconciliationEvent(reconRes.data)
            repository.recordAuditEvent(
                CustomerProfitabilityAuditEvent(
                    eventId = "AUDIT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    snapshotId = snap.snapshotId,
                    action = "RECONCILE_CUSTOMER_PROFITABILITY",
                    actor = actor,
                    actorRole = "STAFF",
                    outcome = if (reconRes.data.isReconciled) "SUCCESS" else "DISCREPANCY_DETECTED",
                    details = "Customer reconciliation executed. isReconciled=${reconRes.data.isReconciled}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return reconRes
    }

    override suspend fun getAuditHistory(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerProfitabilityAuditEvent>> {
        return repository.getAuditEvents(tenantId, projectId, customerId)
    }

    override suspend fun rankCustomers(
        tenantId: String,
        projectId: String,
        criteria: CustomerRankingCriteria
    ): DomainResult<List<CustomerProfitabilityRankingItem>> {
        val allSnapshotsRes = repository.listSnapshots(tenantId, projectId, CustomerProfitabilityFilter(limit = 1000))
        if (allSnapshotsRes is DomainResult.Success) {
            val ranked = rankingService.rankCustomers(allSnapshotsRes.data, criteria)
            return DomainResult.Success(ranked)
        }
        return DomainResult.Error(message = "Failed to list snapshots for customer ranking.")
    }

    override suspend fun analyzeConcentration(tenantId: String, projectId: String): DomainResult<CustomerConcentrationAnalysis> {
        val allSnapshotsRes = repository.listSnapshots(tenantId, projectId, CustomerProfitabilityFilter(limit = 1000))
        if (allSnapshotsRes is DomainResult.Success) {
            val analysis = rankingService.analyzeConcentration(allSnapshotsRes.data)
            return DomainResult.Success(analysis)
        }
        return DomainResult.Error(message = "Failed to list snapshots for concentration analysis.")
    }

    override suspend fun compareCustomers(
        tenantId: String,
        projectId: String,
        customerIds: List<String>
    ): DomainResult<List<CustomerProfitabilityComparisonItem>> {
        val allSnapshotsRes = repository.listSnapshots(tenantId, projectId, CustomerProfitabilityFilter(limit = 1000))
        if (allSnapshotsRes is DomainResult.Success) {
            val compared = rankingService.compareCustomers(allSnapshotsRes.data, customerIds)
            return DomainResult.Success(compared)
        }
        return DomainResult.Error(message = "Failed to list snapshots for customer comparison.")
    }

    override suspend fun getUnattributedDiagnostics(tenantId: String, projectId: String): DomainResult<List<UnattributedProfitabilityItem>> {
        return repository.getUnattributedItems(tenantId, projectId)
    }
}
