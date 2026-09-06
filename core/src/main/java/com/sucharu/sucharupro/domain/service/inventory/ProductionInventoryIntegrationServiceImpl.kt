package com.sucharu.sucharupro.domain.service.inventory

import com.sucharu.sucharupro.data.datasource.FinishedProductInventoryDataSource
import com.sucharu.sucharupro.data.datasource.InventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.finalqc.FinalQcPackagingDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finalqc.FinalQcInspectionStatus
import com.sucharu.sucharupro.domain.model.finalqc.FinishedGoodsReleaseStatus
import com.sucharu.sucharupro.domain.model.inventory.FinishedProductInventoryReceipt
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.ProductionInventoryEligibility
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Thread-safe production-grade implementation of [ProductionInventoryIntegrationService].
 */
class ProductionInventoryIntegrationServiceImpl(
    private val finishedProductInventoryDataSource: FinishedProductInventoryDataSource,
    private val finalQcPackagingDataSource: FinalQcPackagingDataSource,
    private val inventoryProductDataSource: InventoryProductDataSource
) : ProductionInventoryIntegrationService {

    override suspend fun evaluateInventoryEligibility(
        tenantId: String,
        executionJobId: String
    ): DomainResult<ProductionInventoryEligibility> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID is required.")
        }
        if (executionJobId.isBlank()) {
            return DomainResult.Error(message = "Execution Job ID is required.")
        }

        // Check if receipt already exists
        val existingReceipt = finishedProductInventoryDataSource.getReceiptByJob(tenantId, executionJobId)
        if (existingReceipt != null) {
            return DomainResult.Success(
                ProductionInventoryEligibility(
                    executionJobId = executionJobId,
                    orderId = existingReceipt.orderId,
                    isEligible = true,
                    eligibleQuantity = existingReceipt.receivedQuantity,
                    qcInspectionId = existingReceipt.qcInspectionId,
                    releaseId = existingReceipt.releaseId,
                    refusalReasons = emptyList()
                )
            )
        }

        // Check release records from Final QC & Packaging
        val releaseRecords = finalQcPackagingDataSource.listReleaseRecordsByJob(tenantId, executionJobId)
        val validRelease = releaseRecords.firstOrNull {
            it.status == FinishedGoodsReleaseStatus.RELEASE_APPROVED ||
                    it.status == FinishedGoodsReleaseStatus.DISPATCHED_TO_WAREHOUSE
        }

        if (validRelease != null && validRelease.releasedQuantity > BigDecimal.ZERO) {
            return DomainResult.Success(
                ProductionInventoryEligibility(
                    executionJobId = executionJobId,
                    orderId = validRelease.orderId,
                    isEligible = true,
                    eligibleQuantity = validRelease.releasedQuantity,
                    qcInspectionId = validRelease.inspectionId,
                    releaseId = validRelease.releaseId,
                    refusalReasons = emptyList()
                )
            )
        }

        // Check Final QC Inspection status directly if release record is not explicitly created
        val inspection = finalQcPackagingDataSource.listInspectionsByJob(tenantId, executionJobId).firstOrNull()
        if (inspection != null &&
            (inspection.status == FinalQcInspectionStatus.ACCEPTED || inspection.status == FinalQcInspectionStatus.CONDITIONALLY_ACCEPTED) &&
            inspection.acceptedQuantity > BigDecimal.ZERO
        ) {
            return DomainResult.Success(
                ProductionInventoryEligibility(
                    executionJobId = executionJobId,
                    orderId = inspection.orderId,
                    isEligible = true,
                    eligibleQuantity = inspection.acceptedQuantity,
                    qcInspectionId = inspection.inspectionId,
                    releaseId = null,
                    refusalReasons = emptyList()
                )
            )
        }

        val refusalReasons = mutableListOf<String>()
        if (inspection == null && validRelease == null) {
            refusalReasons.add("No Final Quality Control inspection or release record found for Job '$executionJobId'.")
        } else if (inspection != null && inspection.status != FinalQcInspectionStatus.ACCEPTED && inspection.status != FinalQcInspectionStatus.CONDITIONALLY_ACCEPTED) {
            refusalReasons.add("Final QC inspection status is '${inspection.status.name}'. Job must pass Final QC before receiving finished goods.")
        } else if (inspection != null && inspection.acceptedQuantity <= BigDecimal.ZERO) {
            refusalReasons.add("Final QC accepted good quantity is 0. Cannot receive 0 quantity into inventory.")
        }

        return DomainResult.Success(
            ProductionInventoryEligibility(
                executionJobId = executionJobId,
                orderId = inspection?.orderId ?: validRelease?.orderId ?: "UNKNOWN-ORDER",
                isEligible = false,
                eligibleQuantity = BigDecimal.ZERO,
                qcInspectionId = inspection?.inspectionId ?: validRelease?.inspectionId,
                releaseId = validRelease?.releaseId,
                refusalReasons = refusalReasons
            )
        )
    }

    override suspend fun receiveFinishedGoodsFromProduction(
        tenantId: String,
        executionJobId: String,
        warehouseId: String,
        binId: String?,
        actor: String,
        idempotencyKey: String?
    ): DomainResult<FinishedProductInventoryReceipt> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID is required.")
        }
        if (executionJobId.isBlank()) {
            return DomainResult.Error(message = "Execution Job ID is required.")
        }
        if (warehouseId.isBlank()) {
            return DomainResult.Error(message = "Warehouse ID is required.")
        }
        if (actor.isBlank()) {
            return DomainResult.Error(message = "Actor is required.")
        }

        // Idempotency check: if receipt already exists, return existing receipt safely
        val existing = finishedProductInventoryDataSource.getReceiptByJob(tenantId, executionJobId)
        if (existing != null) {
            return DomainResult.Success(existing)
        }

        // Evaluate eligibility
        val eligibilityRes = evaluateInventoryEligibility(tenantId, executionJobId)
        val eligibility = when (eligibilityRes) {
            is DomainResult.Success -> eligibilityRes.data
            is DomainResult.Error -> return DomainResult.Error(message = eligibilityRes.message)
            DomainResult.Loading -> return DomainResult.Error(message = "Evaluating eligibility.")
        }

        if (!eligibility.isEligible) {
            return DomainResult.Error(
                message = "Job '$executionJobId' is not eligible for Finished Goods Inventory receiving: " +
                        eligibility.refusalReasons.joinToString("; ")
            )
        }

        // Derive received quantity strictly from authoritative Final QC inspection / release data
        val finalQuantity = eligibility.eligibleQuantity
        if (finalQuantity <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Received quantity must be greater than zero.")
        }

        val receiptId = "RC-FP-${UUID.randomUUID().toString().take(8).uppercase()}"
        val productId = "PROD-FG-$executionJobId"
        val nowStr = Instant.now().toString()

        // Ensure Finished Product Master record exists
        val product = InventoryProduct(
            id = productId,
            sku = "SKU-FG-$executionJobId",
            name = "Finished Printed Product (Job $executionJobId)",
            description = "Finished Good from Production Job $executionJobId / Order ${eligibility.orderId}",
            categoryId = "FINISHED_GOODS",
            productType = InventoryProductType.FINISHED_PRODUCT,
            unitOfMeasure = InventoryUnit.PCS,
            isStockTracked = true,
            isFinishedProduct = true,
            isSaleable = true,
            isActive = true,
            createdAt = nowStr,
            updatedAt = nowStr,
            createdBy = actor
        )
        inventoryProductDataSource.insertProduct(product)

        val receipt = FinishedProductInventoryReceipt(
            receiptId = receiptId,
            projectId = tenantId,
            executionJobId = executionJobId,
            orderId = eligibility.orderId,
            productId = productId,
            warehouseId = warehouseId,
            binId = binId,
            receivedQuantity = finalQuantity,
            unit = InventoryUnit.PCS,
            unitCost = BigDecimal.ZERO,
            qcInspectionId = eligibility.qcInspectionId,
            releaseId = eligibility.releaseId,
            status = "COMPLETED",
            receivedBy = actor,
            receivedAt = System.currentTimeMillis(),
            notes = "Finished Goods received from Production Execution Job $executionJobId."
        )

        finishedProductInventoryDataSource.saveReceipt(tenantId, receipt)

        return DomainResult.Success(receipt)
    }

    override suspend fun getFinishedGoodsReceiptForJob(
        tenantId: String,
        executionJobId: String
    ): DomainResult<FinishedProductInventoryReceipt?> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID is required.")
        }
        if (executionJobId.isBlank()) {
            return DomainResult.Error(message = "Execution Job ID is required.")
        }

        val receipt = finishedProductInventoryDataSource.getReceiptByJob(tenantId, executionJobId)
        return DomainResult.Success(receipt)
    }
}
