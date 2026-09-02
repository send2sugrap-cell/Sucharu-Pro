package com.sucharu.sucharupro.domain.service.productionexecution

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

const val PRODUCTION_SCALE = 4
val PRODUCTION_ROUNDING = RoundingMode.HALF_UP
val PRODUCTION_ZERO: BigDecimal = BigDecimal.ZERO.setScale(PRODUCTION_SCALE, PRODUCTION_ROUNDING)
val PRODUCTION_ONE_HUNDRED: BigDecimal = BigDecimal("100.0000").setScale(PRODUCTION_SCALE, PRODUCTION_ROUNDING)

fun BigDecimal.p4(): BigDecimal = this.setScale(PRODUCTION_SCALE, PRODUCTION_ROUNDING)

fun BigDecimal?.p4OrZero(): BigDecimal = this?.setScale(PRODUCTION_SCALE, PRODUCTION_ROUNDING) ?: PRODUCTION_ZERO

object ProductionExecutionMathUtils {

    fun isQuantityBalanced(
        planned: BigDecimal,
        completed: BigDecimal,
        rejected: BigDecimal,
        wastage: BigDecimal,
        remaining: BigDecimal
    ): Boolean {
        val totalActual = completed.add(rejected).add(wastage).add(remaining).p4()
        return planned.p4().compareTo(totalActual) == 0
    }

    fun generateFingerprint(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        planningId: String,
        plannedQuantity: BigDecimal,
        status: String
    ): String {
        val raw = "$tenantId|$orderId|$orderItemId|$planningId|${plannedQuantity.p4()}|$status"
        return sha256(raw)
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
