package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*

/**
 * Service Layer Interface for Prepress Orchestration, Cross-Step Reconciliation & AI Handoff.
 * Module 18 Step 06.
 */
interface PrepressOrchestrationService {

    /**
     * Synthesizes upstream specifications (Steps 01 to 05), executes cross-step reconciliation,
     * readiness scoring, recommendation analysis, SHA-256 master sealing, and persists the master plan.
     */
    suspend fun orchestrateAndSavePlan(
        tenantId: String,
        planName: String? = null,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        productName: String,
        requiredQuantity: Long,
        step01ImpositionId: String? = null,
        step02GangRunBatchId: String? = null,
        step03NestingId: String? = null,
        step04SignatureId: String? = null,
        step05CtpOutputId: String? = null,
        actor: String = "prepress_orchestrator"
    ): PrepressOrchestrationPlan

    /**
     * Retrieves an existing prepress orchestration plan by ID.
     */
    suspend fun getPlan(tenantId: String, planId: String): PrepressOrchestrationPlan?

    /**
     * Lists prepress orchestration plans by Job ID.
     */
    suspend fun listPlansByJob(tenantId: String, jobId: String): List<PrepressOrchestrationPlan>

    /**
     * Lists prepress orchestration plans by Order ID.
     */
    suspend fun listPlansByOrder(tenantId: String, orderId: String): List<PrepressOrchestrationPlan>

    /**
     * Lists all prepress orchestration plans for a tenant.
     */
    suspend fun listAllPlans(tenantId: String, limit: Int = 50): List<PrepressOrchestrationPlan>

    /**
     * Updates the lifecycle status of a prepress plan with audit trail.
     */
    suspend fun updatePlanStatus(
        tenantId: String,
        planId: String,
        newStatus: PrepressPlanStatus,
        actor: String,
        reason: String? = null
    ): PrepressOrchestrationPlan

    /**
     * Emits the structured downstream handoff contract for Module 19 / 17 / AI Agent.
     */
    suspend fun getHandoffContract(
        tenantId: String,
        planId: String
    ): Module18Step06PrepressOrchestrationHandoffContract
}

/**
 * Default Implementation of [PrepressOrchestrationService].
 */
class PrepressOrchestrationServiceImpl(
    private val orchestrationRepository: com.sucharu.sucharupro.domain.repository.imposition.PrepressOrchestrationRepository,
    private val impositionRepository: com.sucharu.sucharupro.domain.repository.imposition.ImpositionRepository? = null,
    private val gangRunRepository: com.sucharu.sucharupro.domain.repository.imposition.GangRunRepository? = null,
    private val nestingRepository: com.sucharu.sucharupro.domain.repository.imposition.DynamicNestingRepository? = null,
    private val signatureRepository: com.sucharu.sucharupro.domain.repository.imposition.SignatureImpositionRepository? = null,
    private val ctpOutputRepository: com.sucharu.sucharupro.domain.repository.imposition.CtpOutputRepository? = null
) : PrepressOrchestrationService {

    override suspend fun orchestrateAndSavePlan(
        tenantId: String,
        planName: String?,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        productName: String,
        requiredQuantity: Long,
        step01ImpositionId: String?,
        step02GangRunBatchId: String?,
        step03NestingId: String?,
        step04SignatureId: String?,
        step05CtpOutputId: String?,
        actor: String
    ): PrepressOrchestrationPlan {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(orderId.isNotBlank()) { "Order ID must not be blank." }
        require(orderItemId.isNotBlank()) { "Order Item ID must not be blank." }
        require(requiredQuantity > 0L) { "Required quantity must be positive." }
        require(actor.isNotBlank()) { "Actor must not be blank." }

        // Fetch upstream specs if referenced
        val step01 = step01ImpositionId?.let { impositionRepository?.getSpecificationById(tenantId, it) }
        val step02 = step02GangRunBatchId?.let { gangRunRepository?.findById(tenantId, it) }
        val step03 = step03NestingId?.let { nestingRepository?.findById(tenantId, it) }
        val step04 = step04SignatureId?.let { signatureRepository?.getSpecificationById(tenantId, it) }
        val step05 = step05CtpOutputId?.let { ctpOutputRepository?.findById(tenantId, it) }

        val plan = PrepressOrchestrationEngine.orchestratePlan(
            tenantId = tenantId,
            planName = planName,
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            productName = productName,
            requiredQuantity = requiredQuantity,
            step01Imposition = step01,
            step02GangRun = step02,
            step03Nesting = step03,
            step04Signature = step04,
            step05CtpOutput = step05,
            planVersion = 1,
            actor = actor
        )

        return orchestrationRepository.savePlan(plan)
    }

    override suspend fun getPlan(tenantId: String, planId: String): PrepressOrchestrationPlan? {
        if (tenantId.isBlank() || planId.isBlank()) return null
        return orchestrationRepository.getPlanById(tenantId, planId)
    }

    override suspend fun listPlansByJob(tenantId: String, jobId: String): List<PrepressOrchestrationPlan> {
        if (tenantId.isBlank() || jobId.isBlank()) return emptyList()
        return orchestrationRepository.listPlansByJob(tenantId, jobId)
    }

    override suspend fun listPlansByOrder(tenantId: String, orderId: String): List<PrepressOrchestrationPlan> {
        if (tenantId.isBlank() || orderId.isBlank()) return emptyList()
        return orchestrationRepository.listPlansByOrder(tenantId, orderId)
    }

    override suspend fun listAllPlans(tenantId: String, limit: Int): List<PrepressOrchestrationPlan> {
        if (tenantId.isBlank()) return emptyList()
        return orchestrationRepository.listAllPlans(tenantId, limit)
    }

    override suspend fun updatePlanStatus(
        tenantId: String,
        planId: String,
        newStatus: PrepressPlanStatus,
        actor: String,
        reason: String?
    ): PrepressOrchestrationPlan {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(planId.isNotBlank()) { "Plan ID must not be blank." }

        val existing = getPlan(tenantId, planId)
            ?: throw IllegalArgumentException("Prepress orchestration plan not found: $planId for tenant: $tenantId")

        val success = orchestrationRepository.updatePlanStatus(tenantId, planId, newStatus, actor, reason)
        if (!success) {
            throw IllegalStateException("Failed to update status for plan: $planId")
        }

        return getPlan(tenantId, planId) ?: existing.copy(status = newStatus)
    }

    override suspend fun getHandoffContract(
        tenantId: String,
        planId: String
    ): Module18Step06PrepressOrchestrationHandoffContract {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(planId.isNotBlank()) { "Plan ID must not be blank." }

        val plan = getPlan(tenantId, planId)
            ?: throw IllegalArgumentException("Prepress orchestration plan not found: $planId for tenant: $tenantId")

        return Module18Step06PrepressOrchestrationHandoffContract(
            contractVersion = "1.0.0",
            planId = plan.planId,
            tenantId = plan.tenantId,
            jobId = plan.jobId,
            orderId = plan.orderId,
            orderItemId = plan.orderItemId,
            productName = plan.productName,
            planVersion = plan.version,
            planStatus = plan.status.name,
            requiredSheets = plan.requiredSheets,
            totalProducedQuantity = plan.totalProducedQuantity,
            totalPlatesCount = plan.totalPlatesCount,
            totalSignaturesCount = plan.totalSignaturesCount,
            sheetUtilizationPercentage = plan.sheetUtilizationPercentage,
            wastePercentage = plan.wastePercentage,
            pressSheetWidthMm = plan.pressSheetWidthMm,
            pressSheetHeightMm = plan.pressSheetHeightMm,
            plateWidthMm = plan.plateWidthMm,
            plateHeightMm = plan.plateHeightMm,
            readinessScore = plan.readinessScore.overallScore,
            isFullyReconciled = plan.reconciliationResult.isReconciled,
            blockingErrorsCount = plan.reconciliationResult.blockingErrorsCount,
            warningsCount = plan.reconciliationResult.warningsCount,
            masterIntegrityHash = plan.masterIntegrityHash,
            step05CtpOutputId = plan.step05CtpOutputId,
            step04SignatureId = plan.step04SignatureId,
            step03NestingId = plan.step03NestingId,
            step02GangRunBatchId = plan.step02GangRunBatchId,
            step01ImpositionId = plan.step01ImpositionId,
            generatedAt = System.currentTimeMillis()
        )
    }
}
