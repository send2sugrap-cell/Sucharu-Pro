package com.sucharu.sucharupro.domain.service.delivery

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancySeverity
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancyType
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItemStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import java.util.UUID

/**
 * Deterministic calculation engine for Delivery Reconciliation & Settlement (Module 08 Step 09).
 */
object DeliveryReconciliationCalculator {

    data class CalculationResult(
        val aggregate: DeliveryReconciliation,
        val items: List<DeliveryReconciliationItem>,
        val discrepancies: List<DeliveryReconciliationDiscrepancy>
    )

    fun calculateReconciliation(
        reconciliationId: String,
        deliveryOrder: DeliveryOrder,
        orderLines: List<DeliveryOrderLine>,
        challanLines: List<DeliveryChallanLine> = emptyList(),
        dispatchLines: List<DispatchExecutionLine> = emptyList(),
        shipments: List<DeliveryShipment> = emptyList(),
        verificationLines: List<DeliveryItemVerificationLine> = emptyList(),
        returnLines: List<DeliveryReturnLine> = emptyList(),
        proofs: List<DeliveryProof> = emptyList(),
        existingReconciliation: DeliveryReconciliation? = null,
        actorId: String,
        timestamp: Long = System.currentTimeMillis()
    ): CalculationResult {
        val projectId = deliveryOrder.projectId
        val acceptedProofs = proofs.filter { it.proofStatus == DeliveryProofStatus.ACCEPTED }
        val hasAcceptedPodForOrder = acceptedProofs.isNotEmpty()

        val items = mutableListOf<DeliveryReconciliationItem>()
        val discrepancies = mutableListOf<DeliveryReconciliationDiscrepancy>()

        for (line in orderLines) {
            val doLineId = line.lineId
            val orderedQty = line.requestedQuantity

            val challanedQty = challanLines
                .filter { it.deliveryOrderLineId == doLineId }
                .sumOf { it.quantity }

            val dispatchedQty = dispatchLines
                .filter { it.deliveryOrderLineId == doLineId }
                .sumOf { it.dispatchQuantity }

            val lineVerifications = verificationLines.filter { it.deliveryOrderLineId == doLineId }
            val verifiedQty = lineVerifications.sumOf { it.verifiedQuantity }

            val matchingReturnLines = returnLines.filter { it.deliveryOrderLineId == doLineId }
            val returnedQty = matchingReturnLines.sumOf { it.returnedQuantity }
            val rejectedQty = lineVerifications.sumOf { it.issueQuantity } + matchingReturnLines.sumOf { it.rejectedQuantity }

            // Delivered quantity derived from verified items, delivered shipments, or dispatched quantities
            val deliveredQty = when {
                lineVerifications.isNotEmpty() -> verifiedQty
                shipments.isNotEmpty() -> {
                    val deliveredShipmentDispatches = shipments
                        .filter { it.currentStatus == DeliveryShipmentStatus.DELIVERED }
                        .map { it.dispatchExecutionId }
                    dispatchLines
                        .filter { it.deliveryOrderLineId == doLineId && it.dispatchExecutionId in deliveredShipmentDispatches }
                        .sumOf { it.dispatchQuantity }
                }
                else -> dispatchedQty
            }

            // POD accepted quantity is recognized only if accepted POD covers this delivery
            val acceptedPodQty = if (hasAcceptedPodForOrder || acceptedProofs.any { it.deliveryOrderId == deliveryOrder.deliveryOrderId }) {
                deliveredQty
            } else {
                0.0
            }

            val outstandingQty = (orderedQty - deliveredQty).coerceAtLeast(0.0)

            var lineDiscrepancyQty = 0.0

            // 1. Dispatch exceeds order
            if (dispatchedQty > orderedQty + 0.0001) {
                val diff = dispatchedQty - orderedQty
                lineDiscrepancyQty += diff
                discrepancies.add(
                    DeliveryReconciliationDiscrepancy(
                        discrepancyId = UUID.randomUUID().toString(),
                        reconciliationId = reconciliationId,
                        projectId = projectId,
                        deliveryOrderLineId = doLineId,
                        discrepancyType = DeliveryReconciliationDiscrepancyType.DISPATCH_EXCEEDS_ORDER,
                        severity = DeliveryReconciliationDiscrepancySeverity.HIGH,
                        expectedValue = orderedQty,
                        actualValue = dispatchedQty,
                        description = "Dispatched quantity ($dispatchedQty) exceeds ordered quantity ($orderedQty) for line '${line.productId}'.",
                        detectedAt = timestamp
                    )
                )
            }

            // 2. Delivered exceeds dispatched
            if (deliveredQty > dispatchedQty + 0.0001 && dispatchedQty > 0.0) {
                val diff = deliveredQty - dispatchedQty
                lineDiscrepancyQty += diff
                discrepancies.add(
                    DeliveryReconciliationDiscrepancy(
                        discrepancyId = UUID.randomUUID().toString(),
                        reconciliationId = reconciliationId,
                        projectId = projectId,
                        deliveryOrderLineId = doLineId,
                        discrepancyType = DeliveryReconciliationDiscrepancyType.DELIVERY_EXCEEDS_DISPATCH,
                        severity = DeliveryReconciliationDiscrepancySeverity.CRITICAL,
                        expectedValue = dispatchedQty,
                        actualValue = deliveredQty,
                        description = "Delivered quantity ($deliveredQty) exceeds dispatched quantity ($dispatchedQty) for line '${line.productId}'.",
                        detectedAt = timestamp
                    )
                )
            }

            // 3. POD missing when delivered > 0
            if (deliveredQty > 0.0 && acceptedPodQty <= 0.0) {
                discrepancies.add(
                    DeliveryReconciliationDiscrepancy(
                        discrepancyId = UUID.randomUUID().toString(),
                        reconciliationId = reconciliationId,
                        projectId = projectId,
                        deliveryOrderLineId = doLineId,
                        discrepancyType = DeliveryReconciliationDiscrepancyType.POD_MISSING,
                        severity = DeliveryReconciliationDiscrepancySeverity.MEDIUM,
                        expectedValue = deliveredQty,
                        actualValue = 0.0,
                        description = "Accepted Proof of Delivery missing for delivered quantity ($deliveredQty) of line '${line.productId}'.",
                        detectedAt = timestamp
                    )
                )
            }

            // 4. Returned exceeds delivered
            if (returnedQty > deliveredQty + 0.0001) {
                val diff = returnedQty - deliveredQty
                lineDiscrepancyQty += diff
                discrepancies.add(
                    DeliveryReconciliationDiscrepancy(
                        discrepancyId = UUID.randomUUID().toString(),
                        reconciliationId = reconciliationId,
                        projectId = projectId,
                        deliveryOrderLineId = doLineId,
                        discrepancyType = DeliveryReconciliationDiscrepancyType.RETURN_EXCEEDS_DELIVERY,
                        severity = DeliveryReconciliationDiscrepancySeverity.CRITICAL,
                        expectedValue = deliveredQty,
                        actualValue = returnedQty,
                        description = "Returned quantity ($returnedQty) exceeds delivered quantity ($deliveredQty) for line '${line.productId}'.",
                        detectedAt = timestamp
                    )
                )
            }

            // 5. Unresolved rejections
            if (rejectedQty > 0.0) {
                discrepancies.add(
                    DeliveryReconciliationDiscrepancy(
                        discrepancyId = UUID.randomUUID().toString(),
                        reconciliationId = reconciliationId,
                        projectId = projectId,
                        deliveryOrderLineId = doLineId,
                        discrepancyType = DeliveryReconciliationDiscrepancyType.REJECTION_UNRESOLVED,
                        severity = DeliveryReconciliationDiscrepancySeverity.HIGH,
                        expectedValue = 0.0,
                        actualValue = rejectedQty,
                        description = "Rejected items ($rejectedQty) detected during delivery verification for line '${line.productId}'.",
                        detectedAt = timestamp
                    )
                )
            }

            val itemStatus = when {
                lineDiscrepancyQty > 0.0 || (deliveredQty > 0.0 && acceptedPodQty <= 0.0) -> DeliveryReconciliationItemStatus.DISCREPANCY
                deliveredQty >= orderedQty && outstandingQty <= 0.0 && (deliveredQty == 0.0 || acceptedPodQty >= deliveredQty) -> DeliveryReconciliationItemStatus.MATCHED
                deliveredQty > 0.0 -> DeliveryReconciliationItemStatus.PENDING
                else -> DeliveryReconciliationItemStatus.PENDING
            }

            val existingItem = existingReconciliation?.let {
                null
            }

            val reconciliationItem = DeliveryReconciliationItem(
                reconciliationItemId = existingItem ?: UUID.randomUUID().toString(),
                reconciliationId = reconciliationId,
                projectId = projectId,
                deliveryOrderLineId = doLineId,
                productId = line.productId,
                orderedQuantity = orderedQty,
                challanedQuantity = challanedQty,
                dispatchedQuantity = dispatchedQty,
                deliveredQuantity = deliveredQty,
                acceptedPodQuantity = acceptedPodQty,
                rejectedQuantity = rejectedQty,
                returnedQuantity = returnedQty,
                outstandingQuantity = outstandingQty,
                discrepancyQuantity = lineDiscrepancyQty,
                status = itemStatus,
                createdAt = existingReconciliation?.createdAt ?: timestamp,
                updatedAt = timestamp
            )
            items.add(reconciliationItem)
        }

        val totalOrdered = items.sumOf { it.orderedQuantity }
        val totalChallaned = items.sumOf { it.challanedQuantity }
        val totalDispatched = items.sumOf { it.dispatchedQuantity }
        val totalDelivered = items.sumOf { it.deliveredQuantity }
        val totalAcceptedPod = items.sumOf { it.acceptedPodQuantity }
        val totalRejected = items.sumOf { it.rejectedQuantity }
        val totalReturned = items.sumOf { it.returnedQuantity }
        val totalOutstanding = items.sumOf { it.outstandingQuantity }
        val totalDiscrepancy = items.sumOf { it.discrepancyQuantity }

        val determinedReconciliationStatus = when {
            existingReconciliation?.reconciliationStatus == DeliveryReconciliationStatus.CLOSED -> DeliveryReconciliationStatus.CLOSED
            existingReconciliation?.reconciliationStatus == DeliveryReconciliationStatus.RECONCILED -> DeliveryReconciliationStatus.RECONCILED
            existingReconciliation?.reconciliationStatus == DeliveryReconciliationStatus.DISPUTED -> DeliveryReconciliationStatus.DISPUTED
            existingReconciliation?.reconciliationStatus == DeliveryReconciliationStatus.RESOLVED -> DeliveryReconciliationStatus.RESOLVED
            discrepancies.isNotEmpty() -> DeliveryReconciliationStatus.REQUIRES_REVIEW
            totalDelivered >= totalOrdered && totalOutstanding <= 0.0 && totalAcceptedPod >= totalDelivered && totalOrdered > 0.0 -> DeliveryReconciliationStatus.RECONCILED
            totalDelivered > 0.0 -> DeliveryReconciliationStatus.PARTIALLY_RECONCILED
            else -> DeliveryReconciliationStatus.OPEN
        }

        val determinedSettlementStatus = when {
            existingReconciliation?.settlementStatus == DeliverySettlementStatus.SETTLED -> DeliverySettlementStatus.SETTLED
            existingReconciliation?.settlementStatus == DeliverySettlementStatus.DISPUTED -> DeliverySettlementStatus.DISPUTED
            discrepancies.isNotEmpty() -> DeliverySettlementStatus.DISPUTED
            totalDelivered >= totalOrdered && totalOutstanding <= 0.0 && totalAcceptedPod >= totalDelivered && totalOrdered > 0.0 -> DeliverySettlementStatus.SETTLED
            totalDelivered > 0.0 -> DeliverySettlementStatus.PARTIALLY_DELIVERED
            totalReturned > 0.0 -> DeliverySettlementStatus.PARTIALLY_RETURNED
            else -> DeliverySettlementStatus.OPEN
        }

        val primaryChallanId = challanLines.firstOrNull()?.challanId
        val primaryShipmentId = shipments.firstOrNull()?.shipmentId
        val primaryProofId = acceptedProofs.firstOrNull()?.proofId

        val aggregate = DeliveryReconciliation(
            reconciliationId = reconciliationId,
            projectId = projectId,
            deliveryOrderId = deliveryOrder.deliveryOrderId,
            deliveryChallanId = primaryChallanId,
            deliveryShipmentId = primaryShipmentId,
            proofId = primaryProofId,
            orderedQuantity = totalOrdered,
            challanedQuantity = totalChallaned,
            dispatchedQuantity = totalDispatched,
            deliveredQuantity = totalDelivered,
            acceptedPodQuantity = totalAcceptedPod,
            rejectedQuantity = totalRejected,
            returnedQuantity = totalReturned,
            outstandingQuantity = totalOutstanding,
            discrepancyQuantity = totalDiscrepancy,
            reconciliationStatus = determinedReconciliationStatus,
            settlementStatus = determinedSettlementStatus,
            reconciliationReason = existingReconciliation?.reconciliationReason,
            resolutionNotes = existingReconciliation?.resolutionNotes,
            resolvedBy = existingReconciliation?.resolvedBy,
            resolvedAt = existingReconciliation?.resolvedAt,
            closedBy = existingReconciliation?.closedBy,
            closedAt = existingReconciliation?.closedAt,
            createdBy = existingReconciliation?.createdBy ?: actorId,
            updatedBy = actorId,
            createdAt = existingReconciliation?.createdAt ?: timestamp,
            updatedAt = timestamp
        )

        return CalculationResult(
            aggregate = aggregate,
            items = items,
            discrepancies = discrepancies
        )
    }
}
