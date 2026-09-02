package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.repository.imposition.DynamicNestingRepository
import java.math.BigDecimal

/**
 * Service Layer Interface for Dynamic 2D Nesting & Wastage Optimization.
 * Module 18 Step 03.
 */
interface DynamicNestingService {
    suspend fun optimizeAndSave(
        tenantId: String,
        name: String,
        candidateItems: List<NestingCandidateItem>,
        parentSheetDimension: PrintingDimension,
        marginSpec: ImpositionMarginSpec = ImpositionMarginSpec(),
        spacingSpec: ImpositionSpacingSpec = ImpositionSpacingSpec(),
        orientationPolicy: NestingOrientationPolicy = NestingOrientationPolicy.ALLOW_ROTATION,
        placementStrategy: NestingPlacementStrategy = NestingPlacementStrategy.BOTTOM_LEFT_FILL,
        minOffcutDimensionMm: BigDecimal = BigDecimal("100.0000"),
        saveSpecification: Boolean = true,
        actor: String = "prepress_operator"
    ): DynamicNestingSpecification

    suspend fun getNestingSpecification(tenantId: String, nestingId: String): DynamicNestingSpecification?
    suspend fun listNestingSpecifications(tenantId: String, limit: Int = 50, offset: Int = 0): List<DynamicNestingSpecification>
    suspend fun updateNestingStatus(tenantId: String, nestingId: String, status: NestingStatus, actor: String, notes: String? = null): Boolean
    suspend fun exportHandoffContract(tenantId: String, nestingId: String): Module18Step03NestingHandoffContract?
}

/**
 * Production Implementation of DynamicNestingService.
 * Module 18 Step 03.
 */
class DynamicNestingServiceImpl(
    private val repository: DynamicNestingRepository
) : DynamicNestingService {

    override suspend fun optimizeAndSave(
        tenantId: String,
        name: String,
        candidateItems: List<NestingCandidateItem>,
        parentSheetDimension: PrintingDimension,
        marginSpec: ImpositionMarginSpec,
        spacingSpec: ImpositionSpacingSpec,
        orientationPolicy: NestingOrientationPolicy,
        placementStrategy: NestingPlacementStrategy,
        minOffcutDimensionMm: BigDecimal,
        saveSpecification: Boolean,
        actor: String
    ): DynamicNestingSpecification {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(candidateItems.isNotEmpty()) { "Candidate items pool cannot be empty." }

        val spec = DynamicNestingEngine.optimizeNesting(
            tenantId = tenantId,
            name = name,
            candidateItems = candidateItems,
            parentSheetDimension = parentSheetDimension,
            marginSpec = marginSpec,
            spacingSpec = spacingSpec,
            orientationPolicy = orientationPolicy,
            placementStrategy = placementStrategy,
            minOffcutDimensionMm = minOffcutDimensionMm,
            actor = actor
        )

        if (saveSpecification) {
            repository.save(spec)
        }

        return spec
    }

    override suspend fun getNestingSpecification(tenantId: String, nestingId: String): DynamicNestingSpecification? {
        return repository.findById(tenantId, nestingId)
    }

    override suspend fun listNestingSpecifications(tenantId: String, limit: Int, offset: Int): List<DynamicNestingSpecification> {
        return repository.listAll(tenantId, limit, offset)
    }

    override suspend fun updateNestingStatus(
        tenantId: String,
        nestingId: String,
        status: NestingStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return repository.updateStatus(tenantId, nestingId, status, actor, notes)
    }

    override suspend fun exportHandoffContract(tenantId: String, nestingId: String): Module18Step03NestingHandoffContract? {
        val spec = repository.findById(tenantId, nestingId) ?: return null
        return Module18Step03NestingHandoffContract(
            contractVersion = "1.0.0",
            nestingId = spec.nestingId,
            tenantId = spec.tenantId,
            paperStockType = spec.paperStockType.name,
            gsm = spec.gsm,
            parentSheetWidthMm = spec.parentSheetDimension.width,
            parentSheetHeightMm = spec.parentSheetDimension.height,
            totalParentSheetsRequired = spec.commonRequiredSheets,
            totalPlacedItems = spec.totalItemsPlaced,
            distinctJobIds = spec.jobSummaries.map { it.jobId },
            orderItemIds = spec.jobSummaries.map { it.orderItemId },
            totalProducedItems = spec.totalProducedItems,
            totalOverageItems = spec.totalOverageItems,
            sheetUtilizationPercentage = spec.sheetUtilizationPercentage,
            usableYieldPercentage = spec.usableYieldPercentage,
            recoverableOffcutAreaMm2 = spec.recoverableOffcutAreaMm2,
            integrityHash = spec.integrityHash,
            generatedAt = System.currentTimeMillis()
        )
    }
}
