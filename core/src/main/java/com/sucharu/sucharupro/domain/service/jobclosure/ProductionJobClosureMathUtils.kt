package com.sucharu.sucharupro.domain.service.jobclosure

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

object ProductionJobClosureMathUtils {

    fun roundScale4(value: BigDecimal): BigDecimal {
        return value.setScale(4, RoundingMode.HALF_UP)
    }

    fun calculateOtifScore(onTime: Boolean, goodQuantity: BigDecimal, orderedQuantity: BigDecimal): BigDecimal {
        if (orderedQuantity.compareTo(BigDecimal.ZERO) <= 0) return roundScale4(BigDecimal("100.0000"))
        if (!onTime) return roundScale4(BigDecimal.ZERO)
        val fillRate = goodQuantity.divide(orderedQuantity, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100.0000"))
        return roundScale4(if (fillRate > BigDecimal("100.0000")) BigDecimal("100.0000") else fillRate)
    }

    fun calculateRightFirstTimeScore(totalUnitsProduced: BigDecimal, reworkOrScrapUnits: BigDecimal): BigDecimal {
        if (totalUnitsProduced.compareTo(BigDecimal.ZERO) <= 0) return roundScale4(BigDecimal("100.0000"))
        val firstTimeGood = totalUnitsProduced.subtract(reworkOrScrapUnits)
        if (firstTimeGood <= BigDecimal.ZERO) return roundScale4(BigDecimal.ZERO)
        val rft = firstTimeGood.divide(totalUnitsProduced, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100.0000"))
        return roundScale4(rft)
    }

    fun calculateCostAdherenceIndex(estimatedCost: BigDecimal, actualCost: BigDecimal): BigDecimal {
        if (actualCost.compareTo(BigDecimal.ZERO) <= 0) return roundScale4(BigDecimal("100.0000"))
        val cai = estimatedCost.divide(actualCost, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100.0000"))
        return roundScale4(cai)
    }

    fun calculateOverallManufacturingIndex(
        otif: BigDecimal,
        rft: BigDecimal,
        costAdherence: BigDecimal,
        machineEfficiency: BigDecimal
    ): BigDecimal {
        // Weighted formula: OTIF 30%, RFT 30%, Cost Adherence 25%, Machine Eff 15%
        val otifPart = otif.multiply(BigDecimal("0.3000"))
        val rftPart = rft.multiply(BigDecimal("0.3000"))
        val costPart = (if (costAdherence > BigDecimal("100.0000")) BigDecimal("100.0000") else costAdherence).multiply(BigDecimal("0.2500"))
        val macPart = (if (machineEfficiency > BigDecimal("100.0000")) BigDecimal("100.0000") else machineEfficiency).multiply(BigDecimal("0.1500"))

        val total = otifPart.add(rftPart).add(costPart).add(macPart)
        return roundScale4(total)
    }

    fun calculatePerformanceGrade(overallScore: BigDecimal): String {
        return when {
            overallScore.compareTo(BigDecimal("95.0000")) >= 0 -> "A+"
            overallScore.compareTo(BigDecimal("85.0000")) >= 0 -> "A"
            overallScore.compareTo(BigDecimal("75.0000")) >= 0 -> "B"
            overallScore.compareTo(BigDecimal("60.0000")) >= 0 -> "C"
            else -> "D"
        }
    }

    fun generateMasterClosureSealHash(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        actualTotalCost: BigDecimal,
        totalCostVariance: BigDecimal,
        overallPerformanceScore: BigDecimal,
        closedAt: Long,
        closedBy: String
    ): String {
        val payload = "$tenantId|$executionJobId|$orderId|${roundScale4(actualTotalCost)}|${roundScale4(totalCostVariance)}|${roundScale4(overallPerformanceScore)}|$closedAt|$closedBy"
        val bytes = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
