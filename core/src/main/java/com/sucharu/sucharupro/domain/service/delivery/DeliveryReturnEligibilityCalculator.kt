package com.sucharu.sucharupro.domain.service.delivery

import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnSummary

/**
 * Pure calculation engine for Delivery Return eligibility and reconciliation (Module 08 Step 07).
 */
object DeliveryReturnEligibilityCalculator {

    /**
     * Calculates the remaining returnable quantity for a delivery order line by subtracting
     * all non-cancelled/non-rejected returns from previously delivered/verified quantities.
     */
    fun calculateEligibleReturnQuantity(
        deliveryOrderLineId: String,
        deliveredQuantity: Double,
        existingReturns: List<Pair<DeliveryReturn, List<DeliveryReturnLine>>>
    ): Double {
        val alreadyReturned = existingReturns
            .filter { (ret, _) -> ret.status != DeliveryReturnStatus.CANCELLED && ret.status != DeliveryReturnStatus.REJECTED }
            .flatMap { (_, lines) -> lines }
            .filter { it.deliveryOrderLineId == deliveryOrderLineId }
            .sumOf { it.returnedQuantity }

        return (deliveredQuantity - alreadyReturned).coerceAtLeast(0.0)
    }

    /**
     * Builds summary projection from return aggregate and lines.
     */
    fun buildSummary(
        ret: DeliveryReturn,
        lines: List<DeliveryReturnLine>
    ): DeliveryReturnSummary {
        val totalReturned = lines.sumOf { it.returnedQuantity }
        val totalReceived = lines.sumOf { it.receivedQuantity }
        val totalAccepted = lines.sumOf { it.acceptedQuantity }
        val totalRejected = lines.sumOf { it.rejectedQuantity }
        val totalRestocked = lines.sumOf { it.restockedQuantity }

        val restockableAccepted = lines.filter { it.disposition.allowsRestock }.sumOf { it.acceptedQuantity }
        val isFullyRestocked = restockableAccepted > 0 && totalRestocked >= (restockableAccepted - 0.001)

        return DeliveryReturnSummary(
            returnId = ret.returnId,
            projectId = ret.projectId,
            returnNo = ret.returnNo,
            deliveryOrderId = ret.deliveryOrderId,
            customerId = ret.customerId,
            returnType = ret.returnType,
            returnReason = ret.returnReason,
            status = ret.status,
            priority = ret.priority,
            totalReturnedQuantity = totalReturned,
            totalReceivedQuantity = totalReceived,
            totalAcceptedQuantity = totalAccepted,
            totalRejectedQuantity = totalRejected,
            totalRestockedQuantity = totalRestocked,
            isFullyRestocked = isFullyRestocked,
            lineCount = lines.size,
            createdAt = ret.createdAt,
            updatedAt = ret.updatedAt
        )
    }
}
