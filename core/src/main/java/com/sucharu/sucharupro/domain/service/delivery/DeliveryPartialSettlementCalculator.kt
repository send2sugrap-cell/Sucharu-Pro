package com.sucharu.sucharupro.domain.service.delivery

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import java.util.UUID

/**
 * Deterministic Quantity Reconciliation Engine for Delivery Partial Settlement (Module 08 Step 06).
 */
object DeliveryPartialSettlementCalculator {

    fun calculateLineSettlement(
        settlementId: String,
        orderLine: DeliveryOrderLine,
        challanLines: List<DeliveryChallanLine> = emptyList(),
        dispatchLines: List<DispatchExecutionLine> = emptyList(),
        verificationLines: List<DeliveryItemVerificationLine> = emptyList(),
        splitLines: List<DeliverySplitDispatchLine> = emptyList(),
        recordedDeliveredQuantity: Double = 0.0,
        returnedQuantity: Double = 0.0,
        replacementQuantity: Double = 0.0,
        timestamp: Long = System.currentTimeMillis()
    ): DeliveryPartialSettlementLine {
        val doLineId = orderLine.lineId
        val orderedQty = orderLine.requestedQuantity

        val allocatedQty = challanLines
            .filter { it.deliveryOrderLineId == doLineId }
            .sumOf { it.quantity }

        val dispatchedQty = dispatchLines
            .filter { it.deliveryOrderLineId == doLineId }
            .sumOf { it.dispatchQuantity }

        val verifiedQty = verificationLines
            .filter { it.deliveryOrderLineId == doLineId }
            .sumOf { it.verifiedQuantity }

        val splitDeliveredQty = splitLines
            .filter { it.deliveryOrderLineId == doLineId }
            .sumOf { it.quantity }

        val deliveredQty = maxOf(verifiedQty, splitDeliveredQty, recordedDeliveredQuantity)
            .coerceAtLeast(0.0)

        val shortQty = if (dispatchedQty > deliveredQty) (dispatchedQty - deliveredQty) else 0.0
        val excessQty = if (deliveredQty > orderedQty) (deliveredQty - orderedQty) else 0.0
        val pendingQty = (orderedQty - deliveredQty).coerceAtLeast(0.0)

        val status = when {
            deliveredQty >= orderedQty && pendingQty <= 0.0 -> DeliverySettlementStatus.FULLY_DELIVERED
            deliveredQty > 0.0 -> DeliverySettlementStatus.PARTIALLY_DELIVERED
            returnedQuantity > 0.0 -> DeliverySettlementStatus.PARTIALLY_RETURNED
            else -> DeliverySettlementStatus.OPEN
        }

        return DeliveryPartialSettlementLine(
            settlementLineId = UUID.randomUUID().toString(),
            projectId = orderLine.projectId,
            settlementId = settlementId,
            deliveryOrderLineId = doLineId,
            productId = orderLine.productId,
            orderedQuantity = orderedQty,
            allocatedQuantity = allocatedQty,
            dispatchedQuantity = dispatchedQty,
            deliveredQuantity = deliveredQty,
            shortQuantity = shortQty,
            excessQuantity = excessQty,
            returnedQuantity = returnedQuantity,
            replacementQuantity = replacementQuantity,
            pendingQuantity = pendingQty,
            status = status,
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    fun calculateAggregateSettlement(
        settlementId: String,
        projectId: String,
        deliveryOrderId: String,
        customerId: String?,
        lines: List<DeliveryPartialSettlementLine>,
        currentStatus: DeliverySettlementStatus = DeliverySettlementStatus.OPEN,
        version: Int = 1,
        createdBy: String,
        createdAt: Long,
        updatedBy: String? = null,
        updatedAt: Long = System.currentTimeMillis()
    ): DeliveryPartialSettlement {
        val totalOrdered = lines.sumOf { it.orderedQuantity }
        val totalAllocated = lines.sumOf { it.allocatedQuantity }
        val totalDispatched = lines.sumOf { it.dispatchedQuantity }
        val totalDelivered = lines.sumOf { it.deliveredQuantity }
        val totalShort = lines.sumOf { it.shortQuantity }
        val totalExcess = lines.sumOf { it.excessQuantity }
        val totalReturned = lines.sumOf { it.returnedQuantity }
        val totalReplacement = lines.sumOf { it.replacementQuantity }
        val totalPending = lines.sumOf { it.pendingQuantity }

        val determinedStatus = if (currentStatus.isTerminal || currentStatus == DeliverySettlementStatus.DISPUTED) {
            currentStatus
        } else {
            when {
                totalDelivered >= totalOrdered && totalPending <= 0.0 && totalOrdered > 0.0 -> DeliverySettlementStatus.FULLY_DELIVERED
                totalDelivered > 0.0 -> DeliverySettlementStatus.PARTIALLY_DELIVERED
                totalReturned > 0.0 -> DeliverySettlementStatus.PARTIALLY_RETURNED
                else -> DeliverySettlementStatus.OPEN
            }
        }

        return DeliveryPartialSettlement(
            settlementId = settlementId,
            projectId = projectId,
            deliveryOrderId = deliveryOrderId,
            customerId = customerId,
            status = determinedStatus,
            totalOrderedQuantity = totalOrdered,
            totalAllocatedQuantity = totalAllocated,
            totalDispatchedQuantity = totalDispatched,
            totalDeliveredQuantity = totalDelivered,
            totalShortQuantity = totalShort,
            totalExcessQuantity = totalExcess,
            totalReturnedQuantity = totalReturned,
            totalReplacementQuantity = totalReplacement,
            totalPendingQuantity = totalPending,
            settlementVersion = version,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedBy = updatedBy,
            updatedAt = updatedAt
        )
    }
}
