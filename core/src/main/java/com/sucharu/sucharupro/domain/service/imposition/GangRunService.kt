package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.repository.imposition.GangRunRepository

/**
 * Service Layer Interface for Gang-Run Batching & Clustering.
 * Module 18 Step 02.
 */
interface GangRunService {
    fun formClusters(
        candidates: List<GangRunCandidateItem>,
        policy: GangRunClusteringPolicy = GangRunClusteringPolicy.STRICT_IDENTICAL_SUBSTRATE
    ): List<GangRunCluster>

    suspend fun clusterAndOptimize(
        tenantId: String,
        batchName: String,
        candidates: List<GangRunCandidateItem>,
        parentSheetDimension: PrintingDimension,
        margins: ImpositionMarginSpec = ImpositionMarginSpec(),
        spacing: ImpositionSpacingSpec = ImpositionSpacingSpec(),
        policy: GangRunClusteringPolicy = GangRunClusteringPolicy.STRICT_IDENTICAL_SUBSTRATE,
        saveSpecification: Boolean = true,
        actor: String = "prepress_operator"
    ): List<GangRunSpecification>

    suspend fun getGangRunSpecification(tenantId: String, gangRunId: String): GangRunSpecification?
    suspend fun listGangRuns(tenantId: String, limit: Int = 50, offset: Int = 0): List<GangRunSpecification>
    suspend fun updateGangRunStatus(tenantId: String, gangRunId: String, status: GangRunStatus, actor: String, notes: String? = null): Boolean
    suspend fun exportHandoffContract(tenantId: String, gangRunId: String): Module18Step02GangRunHandoffContract?
}

/**
 * Production Implementation of GangRunService.
 * Module 18 Step 02.
 */
class GangRunServiceImpl(
    private val repository: GangRunRepository,
    private val engine: GangRunClusteringEngine = GangRunClusteringEngine()
) : GangRunService {

    override fun formClusters(
        candidates: List<GangRunCandidateItem>,
        policy: GangRunClusteringPolicy
    ): List<GangRunCluster> {
        return engine.formClusters(candidates, policy)
    }

    override suspend fun clusterAndOptimize(
        tenantId: String,
        batchName: String,
        candidates: List<GangRunCandidateItem>,
        parentSheetDimension: PrintingDimension,
        margins: ImpositionMarginSpec,
        spacing: ImpositionSpacingSpec,
        policy: GangRunClusteringPolicy,
        saveSpecification: Boolean,
        actor: String
    ): List<GangRunSpecification> {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(candidates.isNotEmpty()) { "Candidate list cannot be empty." }

        val clusters = engine.formClusters(candidates, policy)
        val specs = mutableListOf<GangRunSpecification>()

        for ((index, cluster) in clusters.withIndex()) {
            val clusterBatchName = if (clusters.size > 1) "$batchName - Form #${index + 1}" else batchName
            val spec = engine.optimizeGangRun(
                tenantId = tenantId,
                batchName = clusterBatchName,
                cluster = cluster,
                parentSheetDimension = parentSheetDimension,
                margins = margins,
                spacing = spacing,
                actor = actor
            )

            if (saveSpecification) {
                repository.save(spec)
            }
            specs.add(spec)
        }

        return specs
    }

    override suspend fun getGangRunSpecification(tenantId: String, gangRunId: String): GangRunSpecification? {
        return repository.findById(tenantId, gangRunId)
    }

    override suspend fun listGangRuns(tenantId: String, limit: Int, offset: Int): List<GangRunSpecification> {
        return repository.listAll(tenantId, limit, offset)
    }

    override suspend fun updateGangRunStatus(
        tenantId: String,
        gangRunId: String,
        status: GangRunStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return repository.updateStatus(tenantId, gangRunId, status, actor, notes)
    }

    override suspend fun exportHandoffContract(tenantId: String, gangRunId: String): Module18Step02GangRunHandoffContract? {
        val spec = repository.findById(tenantId, gangRunId) ?: return null
        return Module18Step02GangRunHandoffContract(
            contractVersion = "1.0.0",
            gangRunId = spec.gangRunId,
            tenantId = spec.tenantId,
            paperStockType = spec.paperStockType.name,
            gsm = spec.gsm,
            parentSheetWidthMm = spec.parentSheetDimension.width,
            parentSheetHeightMm = spec.parentSheetDimension.height,
            totalParentSheetsRequired = spec.commonRequiredSheets,
            totalAllocatedJobs = spec.allocations.size,
            jobIds = spec.allocations.map { it.jobId },
            orderItemIds = spec.allocations.map { it.orderItemId },
            totalProducedItems = spec.totalProducedItems,
            totalOverageItems = spec.totalOverageItems,
            sheetYieldPercentage = spec.sheetYieldPercentage,
            integrityHash = spec.integrityHash,
            generatedAt = System.currentTimeMillis()
        )
    }
}
