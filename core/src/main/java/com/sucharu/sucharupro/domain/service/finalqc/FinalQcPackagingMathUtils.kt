package com.sucharu.sucharupro.domain.service.finalqc

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

object FinalQcPackagingMathUtils {

    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
    val HUNDRED: BigDecimal = BigDecimal("100.0000").setScale(4, RoundingMode.HALF_UP)

    fun roundScale4(value: BigDecimal): BigDecimal {
        return value.setScale(4, RoundingMode.HALF_UP)
    }

    fun calculateYieldPercentage(goodQuantity: BigDecimal, totalLotQuantity: BigDecimal): BigDecimal {
        if (totalLotQuantity.compareTo(BigDecimal.ZERO) <= 0) return ZERO
        return roundScale4(goodQuantity.multiply(BigDecimal(100)).divide(totalLotQuantity, 4, RoundingMode.HALF_UP))
    }

    fun calculateDefectRatePercentage(defectQuantity: BigDecimal, totalLotQuantity: BigDecimal): BigDecimal {
        if (totalLotQuantity.compareTo(BigDecimal.ZERO) <= 0) return ZERO
        return roundScale4(defectQuantity.multiply(BigDecimal(100)).divide(totalLotQuantity, 4, RoundingMode.HALF_UP))
    }

    fun calculatePackagingVariance(packagedQuantity: BigDecimal, acceptedQuantity: BigDecimal): BigDecimal {
        return roundScale4(packagedQuantity.subtract(acceptedQuantity))
    }

    fun generateReleaseCertificateHash(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        inspectionId: String,
        packagingId: String,
        releasedQuantity: BigDecimal,
        destination: String,
        authorizedBy: String,
        authorizedAt: Long
    ): String {
        val raw = "$tenantId|$executionJobId|$orderId|$inspectionId|$packagingId|${roundScale4(releasedQuantity)}|$destination|$authorizedBy|$authorizedAt"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
