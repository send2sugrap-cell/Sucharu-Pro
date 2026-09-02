package com.sucharu.sucharupro.domain.service.delivery

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.analytics.CustomerDeliveryMetric
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsBreakdown
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsSummary
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsTrend
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsTrendPoint
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertCategory
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertSeverity
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Deterministic calculation engine for Delivery Analytics & Governance (Module 08 Step 10).
 */
object DeliveryAnalyticsCalculator {

    private const val DEFAULT_TIMEZONE = "Asia/Dhaka"

    private fun getFormatter(): SimpleDateFormat {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone(DEFAULT_TIMEZONE)
        return sdf
    }

    /**
     * Calculates high-level summary KPIs from canonical Module 08 data.
     */
    fun calculateSummary(
        projectId: String,
        orders: List<DeliveryOrder> = emptyList(),
        orderLines: List<DeliveryOrderLine> = emptyList(),
        challans: List<DeliveryChallan> = emptyList(),
        challanLines: List<DeliveryChallanLine> = emptyList(),
        dispatches: List<DispatchExecution> = emptyList(),
        dispatchLines: List<DispatchExecutionLine> = emptyList(),
        shipments: List<DeliveryShipment> = emptyList(),
        verificationLines: List<DeliveryItemVerificationLine> = emptyList(),
        returns: List<DeliveryReturn> = emptyList(),
        returnLines: List<DeliveryReturnLine> = emptyList(),
        proofs: List<DeliveryProof> = emptyList(),
        reconciliations: List<DeliveryReconciliation> = emptyList(),
        timestamp: Long = System.currentTimeMillis()
    ): DeliveryAnalyticsSummary {
        val totalOrders = orders.size
        val totalChallansCount = challans.size
        val totalDispatchesCount = dispatches.size
        val totalShipmentsCount = shipments.size

        val deliveredShipments = shipments.count { it.currentStatus == DeliveryShipmentStatus.DELIVERED }
        val partialDeliveries = dispatches.count { it.dispatchType == com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType.CUSTOMER_DELIVERY }
        val totalReturnsCount = returns.size
        val totalRejectionsCount = verificationLines.count { it.issueQuantity > 0 } + returnLines.count { it.rejectedQuantity > 0 }

        val acceptedProofs = proofs.count { it.proofStatus == DeliveryProofStatus.ACCEPTED }
        val pendingProofs = proofs.count { it.proofStatus == DeliveryProofStatus.DRAFT || it.proofStatus == DeliveryProofStatus.SUBMITTED || it.proofStatus == DeliveryProofStatus.PENDING_REVIEW }
        val reconciledCount = reconciliations.count { it.reconciliationStatus == DeliveryReconciliationStatus.RECONCILED || it.reconciliationStatus == DeliveryReconciliationStatus.CLOSED }
        val discrepancyCount = reconciliations.sumOf { if (it.discrepancyQuantity > 0.0 || it.reconciliationStatus == DeliveryReconciliationStatus.REQUIRES_REVIEW || it.reconciliationStatus == DeliveryReconciliationStatus.DISPUTED) 1 else 0 }

        val totalOrderedQty = if (reconciliations.isNotEmpty()) {
            reconciliations.sumOf { it.orderedQuantity }
        } else {
            orderLines.sumOf { it.requestedQuantity }
        }

        val totalDispatchedQty = if (reconciliations.isNotEmpty()) {
            reconciliations.sumOf { it.dispatchedQuantity }
        } else {
            dispatchLines.sumOf { it.dispatchQuantity }
        }

        val totalDeliveredQty = if (reconciliations.isNotEmpty()) {
            reconciliations.sumOf { it.deliveredQuantity }
        } else {
            verificationLines.sumOf { it.verifiedQuantity }
        }

        val totalReturnedQty = if (reconciliations.isNotEmpty()) {
            reconciliations.sumOf { it.returnedQuantity }
        } else {
            returnLines.sumOf { it.returnedQuantity }
        }

        val totalOutstandingQty = if (reconciliations.isNotEmpty()) {
            reconciliations.sumOf { it.outstandingQuantity }
        } else {
            (totalOrderedQty - totalDeliveredQty).coerceAtLeast(0.0)
        }

        val totalDiscrepancyQty = if (reconciliations.isNotEmpty()) {
            reconciliations.sumOf { it.discrepancyQuantity }
        } else {
            0.0
        }

        val deliverySuccessRate = if (totalDispatchedQty > 0.0) {
            ((totalDeliveredQty / totalDispatchedQty) * 100.0).coerceIn(0.0, 100.0)
        } else if (totalDispatchesCount > 0) {
            ((deliveredShipments.toDouble() / totalDispatchesCount) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val podAcceptanceRate = if (totalDeliveredQty > 0.0 && reconciliations.isNotEmpty()) {
            val totalAcceptedPodQty = reconciliations.sumOf { it.acceptedPodQuantity }
            ((totalAcceptedPodQty / totalDeliveredQty) * 100.0).coerceIn(0.0, 100.0)
        } else if (deliveredShipments > 0) {
            ((acceptedProofs.toDouble() / deliveredShipments) * 100.0).coerceIn(0.0, 100.0)
        } else if (proofs.isNotEmpty()) {
            ((acceptedProofs.toDouble() / proofs.size) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val returnRate = if (totalDispatchedQty > 0.0) {
            ((totalReturnedQty / totalDispatchedQty) * 100.0).coerceIn(0.0, 100.0)
        } else if (totalDispatchesCount > 0) {
            ((totalReturnsCount.toDouble() / totalDispatchesCount) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val discrepancyRate = if (totalOrderedQty > 0.0) {
            ((totalDiscrepancyQty / totalOrderedQty) * 100.0).coerceIn(0.0, 100.0)
        } else if (reconciliations.isNotEmpty()) {
            ((discrepancyCount.toDouble() / reconciliations.size) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        return DeliveryAnalyticsSummary(
            projectId = projectId,
            totalDeliveryOrders = totalOrders,
            totalChallans = totalChallansCount,
            totalDispatches = totalDispatchesCount,
            totalShipments = totalShipmentsCount,
            totalDelivered = deliveredShipments,
            totalPartiallyDelivered = partialDeliveries,
            totalReturned = totalReturnsCount,
            totalRejected = totalRejectionsCount,
            totalAcceptedPod = acceptedProofs,
            totalPendingPod = pendingProofs,
            totalReconciled = reconciledCount,
            totalDiscrepancies = discrepancyCount,
            totalOrderedQuantity = totalOrderedQty,
            totalDispatchedQuantity = totalDispatchedQty,
            totalDeliveredQuantity = totalDeliveredQty,
            totalReturnedQuantity = totalReturnedQty,
            totalOutstandingQuantity = totalOutstandingQty,
            totalDiscrepancyQuantity = totalDiscrepancyQty,
            deliverySuccessRate = deliverySuccessRate,
            podAcceptanceRate = podAcceptanceRate,
            returnRate = returnRate,
            discrepancyRate = discrepancyRate,
            generatedAt = timestamp
        )
    }

    /**
     * Calculates analytical breakdown across business dimensions.
     */
    fun calculateBreakdown(
        projectId: String,
        orders: List<DeliveryOrder> = emptyList(),
        shipments: List<DeliveryShipment> = emptyList(),
        proofs: List<DeliveryProof> = emptyList(),
        reconciliations: List<DeliveryReconciliation> = emptyList(),
        returns: List<DeliveryReturn> = emptyList(),
        orderLines: List<DeliveryOrderLine> = emptyList(),
        returnLines: List<DeliveryReturnLine> = emptyList()
    ): DeliveryAnalyticsBreakdown {
        val byOrderStatus = orders.groupBy { it.status }.mapValues { it.value.size }
        val byShipmentStatus = shipments.groupBy { it.currentStatus }.mapValues { it.value.size }
        val byProofStatus = proofs.groupBy { it.proofStatus }.mapValues { it.value.size }
        val byReconciliationStatus = reconciliations.groupBy { it.reconciliationStatus }.mapValues { it.value.size }

        val byCarrier = shipments.mapNotNull { it.carrierName }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .mapValues { it.value.size }

        val customerMap = mutableMapOf<String, CustomerDeliveryMetric>()
        for (order in orders) {
            val cId = order.customerId ?: continue
            val existing = customerMap[cId]
            val orderQty = orderLines.filter { it.deliveryOrderId == order.deliveryOrderId }.sumOf { it.requestedQuantity }
            val matchingRec = reconciliations.find { it.deliveryOrderId == order.deliveryOrderId }
            val delQty = matchingRec?.deliveredQuantity ?: 0.0
            val retQty = matchingRec?.returnedQuantity ?: 0.0
            val isDelivered = order.status == DeliveryOrderStatus.DELIVERED || matchingRec?.reconciliationStatus == DeliveryReconciliationStatus.RECONCILED
            val isReturned = returns.any { it.deliveryOrderId == order.deliveryOrderId }

            if (existing == null) {
                customerMap[cId] = CustomerDeliveryMetric(
                    customerId = cId,
                    totalOrders = 1,
                    deliveredOrders = if (isDelivered) 1 else 0,
                    returnedOrders = if (isReturned) 1 else 0,
                    orderedQuantity = orderQty,
                    deliveredQuantity = delQty,
                    returnedQuantity = retQty
                )
            } else {
                customerMap[cId] = existing.copy(
                    totalOrders = existing.totalOrders + 1,
                    deliveredOrders = existing.deliveredOrders + (if (isDelivered) 1 else 0),
                    returnedOrders = existing.returnedOrders + (if (isReturned) 1 else 0),
                    orderedQuantity = existing.orderedQuantity + orderQty,
                    deliveredQuantity = existing.deliveredQuantity + delQty,
                    returnedQuantity = existing.returnedQuantity + retQty
                )
            }
        }

        return DeliveryAnalyticsBreakdown(
            projectId = projectId,
            byOrderStatus = byOrderStatus,
            byShipmentStatus = byShipmentStatus,
            byProofStatus = byProofStatus,
            byReconciliationStatus = byReconciliationStatus,
            byCustomer = customerMap,
            byCarrier = byCarrier
        )
    }

    /**
     * Calculates time-series trend points grouped by day.
     */
    fun calculateTrends(
        projectId: String,
        period: DeliveryAnalyticsPeriod,
        orders: List<DeliveryOrder> = emptyList(),
        dispatches: List<DispatchExecution> = emptyList(),
        shipments: List<DeliveryShipment> = emptyList(),
        proofs: List<DeliveryProof> = emptyList(),
        returns: List<DeliveryReturn> = emptyList(),
        reconciliations: List<DeliveryReconciliation> = emptyList()
    ): DeliveryAnalyticsTrend {
        val sdf = getFormatter()

        val datesMap = mutableMapOf<String, MutableList<Long>>()
        orders.forEach {
            val key = sdf.format(Date(it.createdAt))
            datesMap.getOrPut(key) { mutableListOf() }.add(it.createdAt)
        }
        dispatches.forEach {
            val key = sdf.format(Date(it.createdAt))
            datesMap.getOrPut(key) { mutableListOf() }.add(it.createdAt)
        }
        shipments.forEach {
            val key = sdf.format(Date(it.createdAt))
            datesMap.getOrPut(key) { mutableListOf() }.add(it.createdAt)
        }

        val points = mutableListOf<DeliveryAnalyticsTrendPoint>()

        for ((dateKey, timestamps) in datesMap.toSortedMap()) {
            val midTimestamp = timestamps.firstOrNull() ?: 1000L
            val dayOrders = orders.count { sdf.format(Date(it.createdAt)) == dateKey }
            val dayDispatches = dispatches.count { sdf.format(Date(it.createdAt)) == dateKey }
            val dayDelivered = shipments.count { sdf.format(Date(it.createdAt)) == dateKey && it.currentStatus == DeliveryShipmentStatus.DELIVERED }
            val dayAcceptedPod = proofs.count { sdf.format(Date(it.createdAt)) == dateKey && it.proofStatus == DeliveryProofStatus.ACCEPTED }
            val dayReturns = returns.count { sdf.format(Date(it.createdAt)) == dateKey }
            val dayDiscrepancies = reconciliations.count { sdf.format(Date(it.createdAt)) == dateKey && it.discrepancyQuantity > 0.0 }
            val dayDeliveredQty = reconciliations.filter { sdf.format(Date(it.createdAt)) == dateKey }.sumOf { it.deliveredQuantity }

            points.add(
                DeliveryAnalyticsTrendPoint(
                    timestamp = midTimestamp,
                    dateLabel = dateKey,
                    orderCount = dayOrders,
                    dispatchedCount = dayDispatches,
                    deliveredCount = dayDelivered,
                    acceptedPodCount = dayAcceptedPod,
                    returnedCount = dayReturns,
                    discrepancyCount = dayDiscrepancies,
                    deliveredQuantity = dayDeliveredQty
                )
            )
        }

        return DeliveryAnalyticsTrend(
            projectId = projectId,
            period = period,
            points = points
        )
    }

    /**
     * Scans canonical records and generates deterministic governance alerts.
     */
    fun generateGovernanceAlerts(
        projectId: String,
        orders: List<DeliveryOrder> = emptyList(),
        shipments: List<DeliveryShipment> = emptyList(),
        returns: List<DeliveryReturn> = emptyList(),
        proofs: List<DeliveryProof> = emptyList(),
        reconciliations: List<DeliveryReconciliation> = emptyList(),
        existingAlerts: List<DeliveryGovernanceAlert> = emptyList(),
        timestamp: Long = System.currentTimeMillis()
    ): List<DeliveryGovernanceAlert> {
        val alerts = mutableListOf<DeliveryGovernanceAlert>()

        fun isDuplicate(category: DeliveryGovernanceAlertCategory, refType: String, refId: String): Boolean {
            return existingAlerts.any {
                it.projectId == projectId &&
                it.category == category &&
                it.referenceType == refType &&
                it.referenceId == refId &&
                it.status in setOf(DeliveryGovernanceAlertStatus.OPEN, DeliveryGovernanceAlertStatus.ACKNOWLEDGED)
            } || alerts.any {
                it.category == category && it.referenceType == refType && it.referenceId == refId
            }
        }

        // 1. Overdue Deliveries
        for (shipment in shipments) {
            val isOverdue = shipment.currentStatus !in setOf(DeliveryShipmentStatus.DELIVERED, DeliveryShipmentStatus.CANCELLED) &&
                shipment.estimatedDeliveryAt != null && shipment.estimatedDeliveryAt < timestamp
            if (isOverdue && !isDuplicate(DeliveryGovernanceAlertCategory.OVERDUE_DELIVERY, "SHIPMENT", shipment.shipmentId)) {
                alerts.add(
                    DeliveryGovernanceAlert(
                        alertId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        category = DeliveryGovernanceAlertCategory.OVERDUE_DELIVERY,
                        severity = DeliveryGovernanceAlertSeverity.CRITICAL,
                        referenceType = "SHIPMENT",
                        referenceId = shipment.shipmentId,
                        title = "Overdue Shipment #${shipment.shipmentNo}",
                        description = "Shipment '${shipment.shipmentNo}' missed its estimated delivery timestamp (${shipment.estimatedDeliveryAt}) and remains in '${shipment.currentStatus.defaultLabel}'.",
                        detectedAt = timestamp,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )
                )
            }
        }

        for (order in orders) {
            val isOverdue = order.status !in setOf(DeliveryOrderStatus.DELIVERED, DeliveryOrderStatus.CANCELLED) &&
                order.requestedDeliveryDate < timestamp
            if (isOverdue && !isDuplicate(DeliveryGovernanceAlertCategory.OVERDUE_DELIVERY, "DELIVERY_ORDER", order.deliveryOrderId)) {
                alerts.add(
                    DeliveryGovernanceAlert(
                        alertId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        category = DeliveryGovernanceAlertCategory.OVERDUE_DELIVERY,
                        severity = DeliveryGovernanceAlertSeverity.WARNING,
                        referenceType = "DELIVERY_ORDER",
                        referenceId = order.deliveryOrderId,
                        title = "Overdue Delivery Order #${order.deliveryOrderNo}",
                        description = "Delivery Order '${order.deliveryOrderNo}' is past its requested delivery date and has not completed delivery.",
                        detectedAt = timestamp,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )
                )
            }
        }

        // 2. Missing Proof of Delivery (Delivered shipment with no accepted POD)
        for (shipment in shipments) {
            if (shipment.currentStatus == DeliveryShipmentStatus.DELIVERED) {
                val hasAcceptedPod = proofs.any {
                    (it.deliveryShipmentId == shipment.shipmentId || it.deliveryOrderId == shipment.deliveryOrderId) &&
                    it.proofStatus == DeliveryProofStatus.ACCEPTED
                }
                if (!hasAcceptedPod && !isDuplicate(DeliveryGovernanceAlertCategory.MISSING_POD, "SHIPMENT", shipment.shipmentId)) {
                    alerts.add(
                        DeliveryGovernanceAlert(
                            alertId = UUID.randomUUID().toString(),
                            projectId = projectId,
                            category = DeliveryGovernanceAlertCategory.MISSING_POD,
                            severity = DeliveryGovernanceAlertSeverity.WARNING,
                            referenceType = "SHIPMENT",
                            referenceId = shipment.shipmentId,
                            title = "Missing Accepted POD for Shipment #${shipment.shipmentNo}",
                            description = "Shipment is marked DELIVERED but lacks an accepted Proof of Delivery record.",
                            detectedAt = timestamp,
                            createdAt = timestamp,
                            updatedAt = timestamp
                        )
                    )
                }
            }
        }

        // 3. Rejected Proof of Delivery
        for (proof in proofs) {
            if (proof.proofStatus == DeliveryProofStatus.REJECTED && !isDuplicate(DeliveryGovernanceAlertCategory.POD_REJECTED, "POD", proof.proofId)) {
                alerts.add(
                    DeliveryGovernanceAlert(
                        alertId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        category = DeliveryGovernanceAlertCategory.POD_REJECTED,
                        severity = DeliveryGovernanceAlertSeverity.CRITICAL,
                        referenceType = "POD",
                        referenceId = proof.proofId,
                        title = "Proof of Delivery Rejected (#${proof.proofNo})",
                        description = "Proof of Delivery '${proof.proofNo}' was rejected during inspection. Reason: ${proof.rejectionReason ?: "Unspecified"}.",
                        detectedAt = timestamp,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )
                )
            }
        }

        // 4. Reconciliation Discrepancy
        for (rec in reconciliations) {
            if ((rec.reconciliationStatus in setOf(DeliveryReconciliationStatus.REQUIRES_REVIEW, DeliveryReconciliationStatus.DISPUTED) || rec.discrepancyQuantity > 0.0) &&
                !isDuplicate(DeliveryGovernanceAlertCategory.RECONCILIATION_DISCREPANCY, "RECONCILIATION", rec.reconciliationId)) {
                alerts.add(
                    DeliveryGovernanceAlert(
                        alertId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        category = DeliveryGovernanceAlertCategory.RECONCILIATION_DISCREPANCY,
                        severity = DeliveryGovernanceAlertSeverity.CRITICAL,
                        referenceType = "RECONCILIATION",
                        referenceId = rec.reconciliationId,
                        title = "Reconciliation Discrepancy (DO #${rec.deliveryOrderId})",
                        description = "Delivery reconciliation for Order '${rec.deliveryOrderId}' flagged ${rec.discrepancyQuantity} discrepancy units in status '${rec.reconciliationStatus.defaultLabel}'.",
                        detectedAt = timestamp,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )
                )
            }
        }

        // 5. Excessive Returns / Rejections
        for (ret in returns) {
            if (ret.status == com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus.APPROVED &&
                !isDuplicate(DeliveryGovernanceAlertCategory.EXCESSIVE_RETURN, "RETURN", ret.returnId)) {
                alerts.add(
                    DeliveryGovernanceAlert(
                        alertId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        category = DeliveryGovernanceAlertCategory.EXCESSIVE_RETURN,
                        severity = DeliveryGovernanceAlertSeverity.WARNING,
                        referenceType = "RETURN",
                        referenceId = ret.returnId,
                        title = "Active Delivery Return #${ret.returnNo}",
                        description = "Return record '${ret.returnNo}' requires reverse logistics receiving and quality inspection.",
                        detectedAt = timestamp,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )
                )
            }
        }

        return alerts
    }
}
