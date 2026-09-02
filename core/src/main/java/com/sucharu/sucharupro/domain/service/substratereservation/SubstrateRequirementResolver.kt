package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperMaterialSpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateRequirement
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class SubstrateRequirementResolver {

    /**
     * Deterministically resolves material demand parameters into a canonical SubstrateRequirement.
     */
    fun resolveRequirement(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        calculationId: String? = null,
        materialSpec: PaperMaterialSpecification,
        productiveSheetsRequired: Long,
        wasteSheetsRequired: Long,
        grainDirection: String = "LONG_GRAIN",
        requiredByTimestamp: Long? = null
    ): SubstrateRequirement {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(orderItemId.isNotBlank()) { "Order Item ID cannot be blank." }
        require(productiveSheetsRequired >= 0L) { "Productive sheets cannot be negative: $productiveSheetsRequired" }
        require(wasteSheetsRequired >= 0L) { "Waste sheets cannot be negative: $wasteSheetsRequired" }

        val totalSheets = productiveSheetsRequired + wasteSheetsRequired
        require(totalSheets > 0L) { "Total required sheets must be strictly positive: $totalSheets" }

        val gsm = materialSpec.gsm ?: BigDecimal("120.0000").setScale(4, RoundingMode.HALF_UP)
        val defaultDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS) // 25" x 36"
        val parentDimension = materialSpec.sheetDimension ?: defaultDimension
        val normalizedDim = SubstrateReservationMathUtils.toMillimeters(parentDimension)

        val totalReams = SubstrateReservationMathUtils.calculateReams(totalSheets)
        val totalWeightKg = SubstrateReservationMathUtils.calculateTotalWeightKg(totalSheets, gsm, normalizedDim)

        return SubstrateRequirement(
            requirementId = "REQ-${UUID.randomUUID().toString().take(12)}",
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            calculationId = calculationId,
            stockType = materialSpec.stockType,
            requestedMaterialCode = materialSpec.materialCode ?: materialSpec.materialId,
            requestedMaterialName = materialSpec.materialName,
            gsm = gsm,
            sheetDimension = normalizedDim,
            productiveSheetsRequired = productiveSheetsRequired,
            wasteSheetsRequired = wasteSheetsRequired,
            totalSheetsRequired = totalSheets,
            totalReamsRequired = totalReams,
            totalWeightKg = totalWeightKg,
            grainDirection = grainDirection,
            requiredByTimestamp = requiredByTimestamp,
            resolvedAt = System.currentTimeMillis()
        )
    }
}
