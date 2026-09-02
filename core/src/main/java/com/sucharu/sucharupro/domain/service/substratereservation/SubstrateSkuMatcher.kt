package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateRequirement
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateSkuMatchConfidence
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateSkuResolutionResult
import java.util.UUID

class SubstrateSkuMatcher {

    /**
     * Resolves a SubstrateRequirement against a list of canonical Module 06 InventoryProduct records.
     */
    fun matchSku(
        requirement: SubstrateRequirement,
        inventoryProducts: List<InventoryProduct>,
        onHandPhysicalSheets: Long,
        currentlyReservedSheets: Long,
        warehouseId: String? = "WH-MAIN-01",
        warehouseName: String? = "Central Paper Warehouse"
    ): SubstrateSkuResolutionResult {
        val reqCode = requirement.requestedMaterialCode?.trim()?.uppercase()

        // 1. Exact SKU Match
        var matchedProduct = if (!reqCode.isNullOrBlank()) {
            inventoryProducts.firstOrNull { it.normalizedSku == reqCode }
        } else null

        var confidence = if (matchedProduct != null) {
            SubstrateSkuMatchConfidence.EXACT_SKU_MATCH
        } else {
            // 2. Name / Keyword Specification Match
            val reqName = requirement.requestedMaterialName.trim().lowercase()
            matchedProduct = inventoryProducts.firstOrNull {
                it.name.lowercase().contains(reqName) || reqName.contains(it.name.lowercase())
            }

            if (matchedProduct != null) {
                SubstrateSkuMatchConfidence.SPECIFICATION_MATCH
            } else {
                // 3. Fallback: match by stockType category
                matchedProduct = inventoryProducts.firstOrNull { it.isActive && it.isStockTracked }
                if (matchedProduct != null) {
                    SubstrateSkuMatchConfidence.COMPATIBLE_SUBSTITUTE
                } else {
                    SubstrateSkuMatchConfidence.UNMATCHED_NO_SKU
                }
            }
        }

        val available = SubstrateReservationMathUtils.calculateAvailableStock(onHandPhysicalSheets, currentlyReservedSheets)
        val isSufficient = available >= requirement.totalSheetsRequired
        val deficit = if (isSufficient) 0L else (requirement.totalSheetsRequired - available)

        val diagnostic = when {
            matchedProduct == null -> "No matching inventory substrate product found in Module 06 master catalog."
            !isSufficient -> "Insufficient available stock: Required ${requirement.totalSheetsRequired} sheets, but only $available sheets available (Deficit: $deficit sheets)."
            else -> null
        }

        return SubstrateSkuResolutionResult(
            resolutionId = "RES-${UUID.randomUUID().toString().take(12)}",
            tenantId = requirement.tenantId,
            requirement = requirement,
            matchedProductId = matchedProduct?.id,
            matchedSku = matchedProduct?.sku,
            matchedProductName = matchedProduct?.name,
            warehouseId = warehouseId,
            warehouseName = warehouseName,
            confidence = confidence,
            onHandPhysicalSheets = onHandPhysicalSheets,
            currentlyReservedSheets = currentlyReservedSheets,
            availableReservableSheets = available,
            isSufficientStockAvailable = isSufficient && matchedProduct != null,
            missingDeficitSheets = deficit,
            diagnosticReason = diagnostic
        )
    }
}
