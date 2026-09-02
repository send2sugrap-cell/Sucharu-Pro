package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.ProductProfitabilityRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.ProductProfitabilityValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Production implementation of ProductProfitabilityService.
 *
 * Implements single-flight mutex protection, idempotency caching, non-mutating reconciliation,
 * deterministic SHA-256 hashing, and append-only audit tracking.
 */
class ProductProfitabilityServiceImpl(
    private val repository: ProductProfitabilityRepository,
    private val sourceCollector: ProductProfitabilitySourceCollector,
    private val reconciliationService: ProductProfitabilityReconciliationService
) : ProductProfitabilityService {

    private val mutex = Mutex()
    private val idempotencyCache = ConcurrentHashMap<String, String>() // idempotencyKey -> snapshotId

    override suspend fun calculateProductProfitability(
        tenantId: String,
        projectId: String,
        productId: String,
        sku: String?,
        productName: String?,
        editionId: String?,
        versionId: String?,
        periodId: String?,
        customerId: String?,
        customRevenue: List<ProductRevenueAttribution>?,
        customCosts: List<ProductCostAttribution>?,
        customBaselineCost: BigDecimal?,
        idempotencyKey: String?,
        actor: String
    ): DomainResult<ProductProfitabilitySnapshot> = mutex.withLock {
        val validation = ProductProfitabilityValidator.validateCalculationRequest(tenantId, projectId, productId)
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

        // 1. Collect canonical revenue and cost attributions
        val collectionRes = sourceCollector.collectProductData(
            tenantId = tenantId,
            projectId = projectId,
            productId = productId,
            customRevenue = customRevenue,
            customCosts = customCosts
        )

        if (collectionRes is DomainResult.Error) {
            return DomainResult.Error(message = collectionRes.message)
        }

        val collected = (collectionRes as DomainResult.Success).data

        // 2. Compute Gross Profit & Gross Margin %
        val grossProfit = ProductProfitabilityMathUtils.calculateGrossProfit(
            revenue = collected.totalRecognizedRevenue,
            cost = collected.totalActualCost
        )

        val grossMarginPercentage = ProductProfitabilityMathUtils.calculateGrossMarginPercentage(
            revenue = collected.totalRecognizedRevenue,
            cost = collected.totalActualCost
        )

        // 3. Compute Unit Economics
        val unitEconomics = ProductProfitabilityMathUtils.calculateUnitEconomics(
            quantity = collected.totalQuantity,
            recognizedRevenue = collected.totalRecognizedRevenue,
            totalActualCost = collected.totalActualCost,
            components = collected.components
        )

        // 4. Compute Variance against baseline
        val (varianceClass, varianceValues) = ProductProfitabilityMathUtils.calculateVariance(
            actualCost = collected.totalActualCost,
            baselineCost = customBaselineCost
        )

        // 5. Determine Classification
        val classification = ProductProfitabilityMathUtils.classifyProfitability(
            recognizedRevenue = collected.totalRecognizedRevenue,
            totalActualCost = collected.totalActualCost,
            grossMarginPercentage = grossMarginPercentage,
            sourceIntegrity = collected.sourceIntegrity
        )

        // 6. Generate SHA-256 Integrity Hash
        val snapshotId = "SNAP-PROD-${UUID.randomUUID()}"
        val calculationVersion = "PRODUCT_PROFITABILITY_V1"
        val integrityHash = ProductProfitabilityMathUtils.generateIntegrityHash(
            tenantId = tenantId,
            projectId = projectId,
            productId = productId,
            calculationVersion = calculationVersion,
            quantity = collected.totalQuantity,
            recognizedRevenue = collected.totalRecognizedRevenue,
            totalActualCost = collected.totalActualCost,
            grossProfit = grossProfit,
            components = collected.components,
            provenanceFingerprints = collected.provenanceFingerprints
        )

        val snapshot = ProductProfitabilitySnapshot(
            snapshotId = snapshotId,
            tenantId = tenantId,
            projectId = projectId,
            productId = productId,
            sku = sku,
            productName = productName ?: "Product $productId",
            editionId = editionId,
            versionId = versionId,
            periodId = periodId,
            customerId = customerId,
            totalQuantity = collected.totalQuantity,
            recognizedRevenue = collected.totalRecognizedRevenue,
            totalActualCost = collected.totalActualCost,
            grossProfit = grossProfit,
            grossMarginPercentage = grossMarginPercentage,
            unitEconomics = unitEconomics,
            costBreakdown = collected.components,
            profitabilityClassification = classification,
            varianceClassification = varianceClass,
            baselineCost = customBaselineCost,
            costVariance = varianceValues.first,
            costVariancePercentage = varianceValues.second,
            sourceIntegrityStatus = collected.sourceIntegrity,
            isReconciled = true,
            reconciliationDiscrepancy = BigDecimal.ZERO.setScale(4, ProductProfitabilityMathUtils.ROUNDING_MODE),
            calculationVersion = calculationVersion,
            generatedAt = System.currentTimeMillis(),
            generatedBy = actor,
            integrityHash = integrityHash
        )

        // 7. Perform Non-mutating Reconciliation
        val reconRes = reconciliationService.reconcileSnapshot(
            snapshot = snapshot,
            revenueSources = collected.revenueAttributions,
            costSources = collected.costAttributions,
            actor = actor
        )

        val finalSnapshot = if (reconRes is DomainResult.Success) {
            val event = reconRes.data
            snapshot.copy(
                isReconciled = event.isReconciled,
                reconciliationDiscrepancy = event.grossProfitDiscrepancy
            )
        } else {
            snapshot.copy(isReconciled = false)
        }

        // 8. Persist Snapshot, Attributions, Reconciliation Event & Audit Event
        repository.saveSnapshot(finalSnapshot)
        if (collected.revenueAttributions.isNotEmpty()) {
            repository.saveRevenueAttributions(collected.revenueAttributions)
        }
        if (collected.costAttributions.isNotEmpty()) {
            repository.saveCostAttributions(collected.costAttributions)
        }
        if (reconRes is DomainResult.Success) {
            repository.saveReconciliationEvent(reconRes.data)
        }

        repository.recordAuditEvent(
            ProductProfitabilityAuditEvent(
                eventId = "AUDIT-${UUID.randomUUID()}",
                tenantId = tenantId,
                projectId = projectId,
                productId = productId,
                snapshotId = finalSnapshot.snapshotId,
                action = "CALCULATE_PRODUCT_PROFITABILITY",
                actor = actor,
                actorRole = "STAFF",
                outcome = "SUCCESS",
                details = "Calculated profitability: Revenue=${finalSnapshot.recognizedRevenue}, Cost=${finalSnapshot.totalActualCost}, Profit=${finalSnapshot.grossProfit}, Margin=${finalSnapshot.grossMarginPercentage}%",
                timestamp = System.currentTimeMillis()
            )
        )

        if (!idempotencyKey.isNullOrBlank()) {
            idempotencyCache[idempotencyKey] = finalSnapshot.snapshotId
        }

        return DomainResult.Success(finalSnapshot)
    }

    override suspend fun getLatestSnapshot(tenantId: String, projectId: String, productId: String): DomainResult<ProductProfitabilitySnapshot?> {
        return repository.getLatestSnapshotByProduct(tenantId, projectId, productId)
    }

    override suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): DomainResult<ProductProfitabilitySnapshot> {
        return repository.getSnapshotById(tenantId, projectId, snapshotId)
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, filter: ProductProfitabilityFilter): DomainResult<List<ProductProfitabilitySnapshot>> {
        return repository.listSnapshots(tenantId, projectId, filter)
    }

    override suspend fun getCostBreakdown(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductCostBreakdownItem>> {
        val snapRes = repository.getLatestSnapshotByProduct(tenantId, projectId, productId)
        if (snapRes is DomainResult.Success && snapRes.data != null) {
            return DomainResult.Success(snapRes.data!!.costBreakdown)
        }
        return DomainResult.Error(message = "No profitability snapshot found for product $productId")
    }

    override suspend fun getProvenance(tenantId: String, projectId: String, productId: String): DomainResult<Pair<List<ProductRevenueAttribution>, List<ProductCostAttribution>>> {
        val revRes = repository.getRevenueAttributions(tenantId, projectId, productId)
        val costRes = repository.getCostAttributions(tenantId, projectId, productId)
        val rev = if (revRes is DomainResult.Success) revRes.data else emptyList()
        val cost = if (costRes is DomainResult.Success) costRes.data else emptyList()
        return DomainResult.Success(Pair(rev, cost))
    }

    override suspend fun getUnitEconomics(tenantId: String, projectId: String, productId: String): DomainResult<ProductUnitEconomics> {
        val snapRes = repository.getLatestSnapshotByProduct(tenantId, projectId, productId)
        if (snapRes is DomainResult.Success && snapRes.data != null) {
            return DomainResult.Success(snapRes.data!!.unitEconomics)
        }
        return DomainResult.Error(message = "No profitability snapshot found for product $productId")
    }

    override suspend fun getVariance(tenantId: String, projectId: String, productId: String): DomainResult<Pair<ProductVarianceClassification, Pair<BigDecimal?, BigDecimal?>>> {
        val snapRes = repository.getLatestSnapshotByProduct(tenantId, projectId, productId)
        if (snapRes is DomainResult.Success && snapRes.data != null) {
            val snap = snapRes.data!!
            return DomainResult.Success(Pair(snap.varianceClassification, Pair(snap.costVariance, snap.costVariancePercentage)))
        }
        return DomainResult.Error(message = "No profitability snapshot found for product $productId")
    }

    override suspend fun reconcileProductProfitability(
        tenantId: String,
        projectId: String,
        productId: String,
        snapshotId: String?,
        actor: String
    ): DomainResult<ProductProfitabilityReconciliationEvent> {
        val snap = if (snapshotId != null) {
            val res = repository.getSnapshotById(tenantId, projectId, snapshotId)
            if (res is DomainResult.Success) res.data else null
        } else {
            val res = repository.getLatestSnapshotByProduct(tenantId, projectId, productId)
            if (res is DomainResult.Success) res.data else null
        }

        if (snap == null) {
            return DomainResult.Error(message = "Snapshot not found for reconciliation.")
        }

        val revList = when (val r = repository.getRevenueAttributions(tenantId, projectId, productId)) {
            is DomainResult.Success -> r.data
            else -> emptyList()
        }
        val costList = when (val c = repository.getCostAttributions(tenantId, projectId, productId)) {
            is DomainResult.Success -> c.data
            else -> emptyList()
        }

        val reconRes = reconciliationService.reconcileSnapshot(snap, revList, costList, actor)
        if (reconRes is DomainResult.Success) {
            repository.saveReconciliationEvent(reconRes.data)
            repository.recordAuditEvent(
                ProductProfitabilityAuditEvent(
                    eventId = "AUDIT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    productId = productId,
                    snapshotId = snap.snapshotId,
                    action = "RECONCILE_PRODUCT_PROFITABILITY",
                    actor = actor,
                    actorRole = "STAFF",
                    outcome = if (reconRes.data.isReconciled) "SUCCESS" else "DISCREPANCY_DETECTED",
                    details = "Reconciliation completed: isReconciled=${reconRes.data.isReconciled}, discrepancies=${reconRes.data.discrepancies.size}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return reconRes
    }

    override suspend fun getAuditHistory(tenantId: String, projectId: String, productId: String): DomainResult<List<ProductProfitabilityAuditEvent>> {
        return repository.getAuditEvents(tenantId, projectId, productId)
    }

    override suspend fun compareProducts(tenantId: String, projectId: String, productIds: List<String>): DomainResult<List<ProductProfitabilityComparisonItem>> {
        val items = mutableListOf<ProductProfitabilityComparisonItem>()
        for (pid in productIds) {
            val snapRes = repository.getLatestSnapshotByProduct(tenantId, projectId, pid)
            if (snapRes is DomainResult.Success && snapRes.data != null) {
                val s = snapRes.data!!
                val vendorCost = s.costBreakdown.firstOrNull { it.componentType == JobCostComponentType.VENDOR_OUTSOURCE_COST }?.amount ?: BigDecimal.ZERO
                val reworkCost = s.costBreakdown.firstOrNull { it.componentType == JobCostComponentType.REWORK_COST }?.amount ?: BigDecimal.ZERO
                val wastageCost = s.costBreakdown.firstOrNull { it.componentType == JobCostComponentType.WASTAGE_COST }?.amount ?: BigDecimal.ZERO

                items.add(
                    ProductProfitabilityComparisonItem(
                        productId = s.productId,
                        sku = s.sku,
                        productName = s.productName,
                        quantity = s.totalQuantity,
                        recognizedRevenue = s.recognizedRevenue,
                        totalActualCost = s.totalActualCost,
                        grossProfit = s.grossProfit,
                        grossMarginPercentage = s.grossMarginPercentage,
                        unitRevenue = s.unitEconomics.unitRevenue,
                        unitActualCost = s.unitEconomics.unitActualCost,
                        unitGrossProfit = s.unitEconomics.unitGrossProfit,
                        vendorOutsourceCost = vendorCost,
                        reworkCost = reworkCost,
                        wastageCost = wastageCost,
                        classification = s.profitabilityClassification,
                        varianceClassification = s.varianceClassification
                    )
                )
            }
        }
        return DomainResult.Success(items)
    }
}
