package com.sucharu.sucharupro.domain.service.productionscheduling

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

object ProductionSchedulingMathUtils {

    fun p4(value: Long): BigDecimal {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP)
    }

    fun p4(value: Double): BigDecimal {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP)
    }

    fun BigDecimal.p4(): BigDecimal = setScale(4, RoundingMode.HALF_UP)

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun generateFingerprint(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        version: Int,
        totalEstimatedMinutes: Int,
        slotCount: Int
    ): String {
        return sha256("$tenantId|$executionJobId|$orderId|$version|$totalEstimatedMinutes|$slotCount")
    }

    fun calculateUtilizationRate(allocatedMinutes: BigDecimal, totalCapacityMinutes: BigDecimal): BigDecimal {
        if (totalCapacityMinutes.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO.p4()
        return allocatedMinutes.divide(totalCapacityMinutes, 4, RoundingMode.HALF_UP)
    }
}
