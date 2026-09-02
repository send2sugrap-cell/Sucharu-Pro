package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.repository.imposition.ImpositionRepository

interface ImpositionService {

    fun calculateOptimalLayout(
        tenantId: String,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        calculationId: String? = null,
        productName: String,
        finishedItemDimension: PrintingDimension,
        parentSheetDimension: PrintingDimension,
        margins: ImpositionMarginSpec = ImpositionMarginSpec(),
        spacing: ImpositionSpacingSpec = ImpositionSpacingSpec(),
        orientationPolicy: ImpositionOrientationPolicy = ImpositionOrientationPolicy.AUTO_OPTIMAL,
        requiredQuantity: Long,
        notes: String? = null,
        actor: String = "prepress_operator"
    ): ImpositionSpecification

    suspend fun saveImpositionSpecification(spec: ImpositionSpecification): ImpositionSpecification

    suspend fun calculateAndSave(
        tenantId: String,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        calculationId: String? = null,
        productName: String,
        finishedItemDimension: PrintingDimension,
        parentSheetDimension: PrintingDimension,
        margins: ImpositionMarginSpec = ImpositionMarginSpec(),
        spacing: ImpositionSpacingSpec = ImpositionSpacingSpec(),
        orientationPolicy: ImpositionOrientationPolicy = ImpositionOrientationPolicy.AUTO_OPTIMAL,
        requiredQuantity: Long,
        notes: String? = null,
        actor: String = "prepress_operator"
    ): ImpositionSpecification

    suspend fun getImpositionSpecification(tenantId: String, impositionId: String): ImpositionSpecification?

    suspend fun listImpositionsByJob(tenantId: String, jobId: String): List<ImpositionSpecification>

    suspend fun listImpositionsByOrder(tenantId: String, orderId: String): List<ImpositionSpecification>

    suspend fun listAllImpositions(tenantId: String, limit: Int = 50): List<ImpositionSpecification>

    suspend fun updateImpositionStatus(tenantId: String, impositionId: String, status: String, actor: String, notes: String?): Boolean

    suspend fun exportHandoffContract(tenantId: String, impositionId: String): Module18Step01ImpositionHandoffContract
}

class ImpositionServiceImpl(
    private val repository: ImpositionRepository,
    private val engine: SingleJobImpositionEngine = SingleJobImpositionEngine()
) : ImpositionService {

    override fun calculateOptimalLayout(
        tenantId: String,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        calculationId: String?,
        productName: String,
        finishedItemDimension: PrintingDimension,
        parentSheetDimension: PrintingDimension,
        margins: ImpositionMarginSpec,
        spacing: ImpositionSpacingSpec,
        orientationPolicy: ImpositionOrientationPolicy,
        requiredQuantity: Long,
        notes: String?,
        actor: String
    ): ImpositionSpecification {
        return engine.calculateOptimalLayout(
            tenantId = tenantId,
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            calculationId = calculationId,
            productName = productName,
            finishedItemDimension = finishedItemDimension,
            parentSheetDimension = parentSheetDimension,
            margins = margins,
            spacing = spacing,
            orientationPolicy = orientationPolicy,
            requiredQuantity = requiredQuantity,
            notes = notes,
            actor = actor
        )
    }

    override suspend fun saveImpositionSpecification(spec: ImpositionSpecification): ImpositionSpecification {
        return repository.saveSpecification(spec)
    }

    override suspend fun calculateAndSave(
        tenantId: String,
        jobId: String?,
        orderId: String,
        orderItemId: String,
        calculationId: String?,
        productName: String,
        finishedItemDimension: PrintingDimension,
        parentSheetDimension: PrintingDimension,
        margins: ImpositionMarginSpec,
        spacing: ImpositionSpacingSpec,
        orientationPolicy: ImpositionOrientationPolicy,
        requiredQuantity: Long,
        notes: String?,
        actor: String
    ): ImpositionSpecification {
        val spec = calculateOptimalLayout(
            tenantId = tenantId,
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            calculationId = calculationId,
            productName = productName,
            finishedItemDimension = finishedItemDimension,
            parentSheetDimension = parentSheetDimension,
            margins = margins,
            spacing = spacing,
            orientationPolicy = orientationPolicy,
            requiredQuantity = requiredQuantity,
            notes = notes,
            actor = actor
        )
        return repository.saveSpecification(spec)
    }

    override suspend fun getImpositionSpecification(tenantId: String, impositionId: String): ImpositionSpecification? {
        return repository.getSpecificationById(tenantId, impositionId)
    }

    override suspend fun listImpositionsByJob(tenantId: String, jobId: String): List<ImpositionSpecification> {
        return repository.listSpecificationsByJob(tenantId, jobId)
    }

    override suspend fun listImpositionsByOrder(tenantId: String, orderId: String): List<ImpositionSpecification> {
        return repository.listSpecificationsByOrder(tenantId, orderId)
    }

    override suspend fun listAllImpositions(tenantId: String, limit: Int): List<ImpositionSpecification> {
        return repository.listAllSpecifications(tenantId, limit)
    }

    override suspend fun updateImpositionStatus(
        tenantId: String,
        impositionId: String,
        status: String,
        actor: String,
        notes: String?
    ): Boolean {
        return repository.updateStatus(tenantId, impositionId, status, actor, notes)
    }

    override suspend fun exportHandoffContract(tenantId: String, impositionId: String): Module18Step01ImpositionHandoffContract {
        val spec = repository.getSpecificationById(tenantId, impositionId)
            ?: throw IllegalArgumentException("Imposition specification not found: $impositionId for tenant: $tenantId")

        val wastePct = ImpositionMathUtils.ONE_HUNDRED.subtract(spec.yieldPercentage).coerceAtLeast(ImpositionMathUtils.ZERO)

        return Module18Step01ImpositionHandoffContract(
            contractVersion = "1.0.0",
            impositionId = spec.impositionId,
            tenantId = spec.tenantId,
            orderId = spec.orderId,
            orderItemId = spec.orderItemId,
            jobId = spec.jobId,
            parentSheetWidthMm = spec.parentSheetDimension.width,
            parentSheetHeightMm = spec.parentSheetDimension.height,
            finishedItemWidthMm = spec.finishedItemDimension.width,
            finishedItemHeightMm = spec.finishedItemDimension.height,
            orientation = spec.selectedOrientation.name,
            columns = spec.columns,
            rows = spec.rows,
            copiesPerSheet = spec.copiesPerSheet,
            requiredProductiveQuantity = spec.requiredQuantity,
            requiredProductiveSheets = spec.requiredSheets,
            totalProducedCapacity = spec.totalProducedCapacity,
            layoutOverageItems = spec.overageQuantity,
            sheetYieldPercentage = spec.yieldPercentage,
            wasteAreaPercentage = wastePct,
            integrityHash = spec.integrityHash,
            generatedAt = System.currentTimeMillis()
        )
    }
}
