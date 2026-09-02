package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryReturnDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.DispatchExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsBreakdown
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsSummary
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsTrend
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceActivityType
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryAnalyticsRepository
import com.sucharu.sucharupro.domain.service.delivery.DeliveryAnalyticsCalculator
import com.sucharu.sucharupro.domain.validation.DeliveryGovernanceAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DeliveryGovernanceLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DeliveryGovernanceValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade repository implementation for Delivery Analytics & Governance (Module 08 Step 10).
 */
class DeliveryAnalyticsRepositoryImpl(
    private val governanceDataSource: DeliveryGovernanceDataSource,
    private val orderDataSource: DeliveryOrderDataSource,
    private val challanDataSource: DeliveryChallanDataSource,
    private val dispatchDataSource: DispatchExecutionDataSource,
    private val shipmentDataSource: DeliveryShipmentDataSource? = null,
    private val verificationDataSource: DeliveryItemVerificationDataSource? = null,
    private val returnDataSource: DeliveryReturnDataSource? = null,
    private val proofDataSource: DeliveryProofDataSource? = null,
    private val reconciliationDataSource: DeliveryReconciliationDataSource? = null
) : DeliveryAnalyticsRepository {

    private val mutex = Mutex()

    override suspend fun getSummary(
        filter: DeliveryAnalyticsFilter,
        callerRole: UserRole
    ): DomainResult<DeliveryAnalyticsSummary> {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateViewAnalytics(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val projectId = filter.projectId
        val allOrders = orderDataSource.observeDeliveryOrders(projectId).first()
        val orders = allOrders.filter { order ->
            (filter.customerId == null || order.customerId == filter.customerId) &&
            (filter.deliveryOrderId == null || order.deliveryOrderId == filter.deliveryOrderId) &&
            (filter.dateFrom == null || order.createdAt >= filter.dateFrom) &&
            (filter.dateTo == null || order.createdAt <= filter.dateTo)
        }

        val matchingOrderIds = orders.map { it.deliveryOrderId }.toSet()
        val orderLines = mutableListOf<DeliveryOrderLine>()
        for (oId in matchingOrderIds) {
            orderLines.addAll(orderDataSource.getDeliveryOrderLines(oId))
        }

        val allChallans = challanDataSource.observeChallans(projectId).first()
        val challans = allChallans.filter { it.deliveryOrderId in matchingOrderIds }
        val challanLines = mutableListOf<DeliveryChallanLine>()
        for (c in challans) {
            challanLines.addAll(challanDataSource.getChallanLines(c.challanId))
        }

        val allDispatches = dispatchDataSource.observeDispatches(projectId).first()
        val dispatches = allDispatches.filter { it.deliveryOrderId in matchingOrderIds }
        val dispatchLines = mutableListOf<DispatchExecutionLine>()
        for (d in dispatches) {
            dispatchLines.addAll(dispatchDataSource.getDispatchLines(d.dispatchExecutionId))
        }

        val allShipments = shipmentDataSource?.observeShipments(projectId)?.first() ?: emptyList()
        val shipments = allShipments.filter { it.deliveryOrderId in matchingOrderIds }

        val verificationLines = mutableListOf<DeliveryItemVerificationLine>()
        val allVerifications = verificationDataSource?.observeVerifications(projectId)?.first() ?: emptyList()
        val verifications = allVerifications.filter { it.deliveryOrderId in matchingOrderIds }
        for (v in verifications) {
            verificationLines.addAll(verificationDataSource?.getVerificationLines(v.verificationId) ?: emptyList())
        }

        val allReturns = returnDataSource?.observeReturns(projectId)?.first() ?: emptyList()
        val returns = allReturns.filter { it.deliveryOrderId in matchingOrderIds }
        val returnLines = mutableListOf<DeliveryReturnLine>()
        for (r in returns) {
            returnLines.addAll(returnDataSource?.getReturnLines(r.returnId) ?: emptyList())
        }

        val allProofs = proofDataSource?.observeProofs(projectId)?.first() ?: emptyList()
        val proofs = allProofs.filter { it.deliveryOrderId in matchingOrderIds }

        val allReconciliations = reconciliationDataSource?.observeReconciliations(projectId)?.first() ?: emptyList()
        val reconciliations = allReconciliations.filter { it.deliveryOrderId in matchingOrderIds }

        val summary = DeliveryAnalyticsCalculator.calculateSummary(
            projectId = projectId,
            orders = orders,
            orderLines = orderLines,
            challans = challans,
            challanLines = challanLines,
            dispatches = dispatches,
            dispatchLines = dispatchLines,
            shipments = shipments,
            verificationLines = verificationLines,
            returns = returns,
            returnLines = returnLines,
            proofs = proofs,
            reconciliations = reconciliations
        )

        return DomainResult.Success(summary)
    }

    override suspend fun getBreakdown(
        filter: DeliveryAnalyticsFilter,
        callerRole: UserRole
    ): DomainResult<DeliveryAnalyticsBreakdown> {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateViewAnalytics(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val projectId = filter.projectId
        val allOrders = orderDataSource.observeDeliveryOrders(projectId).first()
        val orders = allOrders.filter { order ->
            (filter.customerId == null || order.customerId == filter.customerId) &&
            (filter.dateFrom == null || order.createdAt >= filter.dateFrom) &&
            (filter.dateTo == null || order.createdAt <= filter.dateTo)
        }

        val matchingOrderIds = orders.map { it.deliveryOrderId }.toSet()
        val orderLines = mutableListOf<DeliveryOrderLine>()
        for (oId in matchingOrderIds) {
            orderLines.addAll(orderDataSource.getDeliveryOrderLines(oId))
        }

        val allShipments = shipmentDataSource?.observeShipments(projectId)?.first() ?: emptyList()
        val shipments = allShipments.filter { it.deliveryOrderId in matchingOrderIds }

        val allProofs = proofDataSource?.observeProofs(projectId)?.first() ?: emptyList()
        val proofs = allProofs.filter { it.deliveryOrderId in matchingOrderIds }

        val allReconciliations = reconciliationDataSource?.observeReconciliations(projectId)?.first() ?: emptyList()
        val reconciliations = allReconciliations.filter { it.deliveryOrderId in matchingOrderIds }

        val allReturns = returnDataSource?.observeReturns(projectId)?.first() ?: emptyList()
        val returns = allReturns.filter { it.deliveryOrderId in matchingOrderIds }
        val returnLines = mutableListOf<DeliveryReturnLine>()
        for (r in returns) {
            returnLines.addAll(returnDataSource?.getReturnLines(r.returnId) ?: emptyList())
        }

        val breakdown = DeliveryAnalyticsCalculator.calculateBreakdown(
            projectId = projectId,
            orders = orders,
            shipments = shipments,
            proofs = proofs,
            reconciliations = reconciliations,
            returns = returns,
            orderLines = orderLines,
            returnLines = returnLines
        )

        return DomainResult.Success(breakdown)
    }

    override suspend fun getTrends(
        projectId: String,
        period: DeliveryAnalyticsPeriod,
        callerRole: UserRole
    ): DomainResult<DeliveryAnalyticsTrend> {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateViewAnalytics(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val orders = orderDataSource.observeDeliveryOrders(projectId).first()
        val matchingOrderIds = orders.map { it.deliveryOrderId }.toSet()
        val dispatches = dispatchDataSource.observeDispatches(projectId).first().filter { it.deliveryOrderId in matchingOrderIds }
        val shipments = shipmentDataSource?.observeShipments(projectId)?.first()?.filter { it.deliveryOrderId in matchingOrderIds } ?: emptyList()
        val proofs = proofDataSource?.observeProofs(projectId)?.first()?.filter { it.deliveryOrderId in matchingOrderIds } ?: emptyList()
        val returns = returnDataSource?.observeReturns(projectId)?.first()?.filter { it.deliveryOrderId in matchingOrderIds } ?: emptyList()
        val reconciliations = reconciliationDataSource?.observeReconciliations(projectId)?.first()?.filter { it.deliveryOrderId in matchingOrderIds } ?: emptyList()

        val trend = DeliveryAnalyticsCalculator.calculateTrends(
            projectId = projectId,
            period = period,
            orders = orders,
            dispatches = dispatches,
            shipments = shipments,
            proofs = proofs,
            returns = returns,
            reconciliations = reconciliations
        )

        return DomainResult.Success(trend)
    }

    override suspend fun refreshGovernanceAlerts(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<DeliveryGovernanceAlert>> = mutex.withLock {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateViewGovernance(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val orders = orderDataSource.observeDeliveryOrders(projectId).first()
        val shipments = shipmentDataSource?.observeShipments(projectId)?.first() ?: emptyList()
        val returns = returnDataSource?.observeReturns(projectId)?.first() ?: emptyList()
        val proofs = proofDataSource?.observeProofs(projectId)?.first() ?: emptyList()
        val reconciliations = reconciliationDataSource?.observeReconciliations(projectId)?.first() ?: emptyList()

        val existingAlerts = governanceDataSource.getAlerts(projectId)
        val newAlerts = DeliveryAnalyticsCalculator.generateGovernanceAlerts(
            projectId = projectId,
            orders = orders,
            shipments = shipments,
            returns = returns,
            proofs = proofs,
            reconciliations = reconciliations,
            existingAlerts = existingAlerts
        )

        for (alert in newAlerts) {
            governanceDataSource.insertAlert(alert)
            governanceDataSource.insertActivityEvent(
                DeliveryGovernanceActivityEvent(
                    eventId = UUID.randomUUID().toString(),
                    alertId = alert.alertId,
                    projectId = projectId,
                    activityType = DeliveryGovernanceActivityType.ALERT_GENERATED,
                    actorId = actorId,
                    details = "Governance alert generated: ${alert.title}"
                )
            )
        }

        val allAlerts = governanceDataSource.getAlerts(projectId)
        return DomainResult.Success(allAlerts)
    }

    override suspend fun getAlerts(
        projectId: String,
        callerRole: UserRole
    ): DomainResult<List<DeliveryGovernanceAlert>> {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateViewGovernance(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val alerts = governanceDataSource.getAlerts(projectId)
        return DomainResult.Success(alerts)
    }

    override suspend fun getAlertById(
        alertId: String,
        callerRole: UserRole
    ): DomainResult<DeliveryGovernanceAlert> {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateViewGovernance(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val alert = governanceDataSource.getAlertById(alertId)
            ?: return DomainResult.Error(message = "Governance Alert '$alertId' not found.")
        return DomainResult.Success(alert)
    }

    override suspend fun acknowledgeAlert(
        alertId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<DeliveryGovernanceAlert> = mutex.withLock {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateAcknowledgeAlert(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val alert = governanceDataSource.getAlertById(alertId)
            ?: return DomainResult.Error(message = "Governance Alert '$alertId' not found.")

        val transitionResult = DeliveryGovernanceLifecycleValidator.validateTransition(
            alert.status,
            DeliveryGovernanceAlertStatus.ACKNOWLEDGED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = alert.copy(
            status = DeliveryGovernanceAlertStatus.ACKNOWLEDGED,
            acknowledgedBy = actorId,
            acknowledgedAt = now,
            updatedAt = now
        )

        val validationResult = DeliveryGovernanceValidator.validateAlert(updated, alert.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        governanceDataSource.updateAlert(updated)
        governanceDataSource.insertActivityEvent(
            DeliveryGovernanceActivityEvent(
                eventId = UUID.randomUUID().toString(),
                alertId = alertId,
                projectId = alert.projectId,
                activityType = DeliveryGovernanceActivityType.ALERT_ACKNOWLEDGED,
                actorId = actorId,
                details = "Alert acknowledged by user '$actorId'."
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun resolveAlert(
        alertId: String,
        actorId: String,
        resolutionNotes: String,
        callerRole: UserRole
    ): DomainResult<DeliveryGovernanceAlert> = mutex.withLock {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateResolveAlert(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (resolutionNotes.trim().isEmpty()) {
            return DomainResult.Error(message = "Resolution notes cannot be blank when resolving an alert.")
        }

        val alert = governanceDataSource.getAlertById(alertId)
            ?: return DomainResult.Error(message = "Governance Alert '$alertId' not found.")

        val transitionResult = DeliveryGovernanceLifecycleValidator.validateTransition(
            alert.status,
            DeliveryGovernanceAlertStatus.RESOLVED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = alert.copy(
            status = DeliveryGovernanceAlertStatus.RESOLVED,
            resolvedBy = actorId,
            resolvedAt = now,
            resolutionNotes = resolutionNotes.trim(),
            updatedAt = now
        )

        val validationResult = DeliveryGovernanceValidator.validateAlert(updated, alert.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        governanceDataSource.updateAlert(updated)
        governanceDataSource.insertActivityEvent(
            DeliveryGovernanceActivityEvent(
                eventId = UUID.randomUUID().toString(),
                alertId = alertId,
                projectId = alert.projectId,
                activityType = DeliveryGovernanceActivityType.ALERT_RESOLVED,
                actorId = actorId,
                details = "Alert resolved by user '$actorId': $resolutionNotes"
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun dismissAlert(
        alertId: String,
        actorId: String,
        dismissalReason: String,
        callerRole: UserRole
    ): DomainResult<DeliveryGovernanceAlert> = mutex.withLock {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateDismissAlert(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (dismissalReason.trim().isEmpty()) {
            return DomainResult.Error(message = "Dismissal reason cannot be blank when dismissing an alert.")
        }

        val alert = governanceDataSource.getAlertById(alertId)
            ?: return DomainResult.Error(message = "Governance Alert '$alertId' not found.")

        val transitionResult = DeliveryGovernanceLifecycleValidator.validateTransition(
            alert.status,
            DeliveryGovernanceAlertStatus.DISMISSED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        val now = System.currentTimeMillis()
        val updated = alert.copy(
            status = DeliveryGovernanceAlertStatus.DISMISSED,
            resolvedBy = actorId,
            resolvedAt = now,
            resolutionNotes = dismissalReason.trim(),
            updatedAt = now
        )

        val validationResult = DeliveryGovernanceValidator.validateAlert(updated, alert.projectId)
        if (validationResult is DomainResult.Error) return validationResult

        governanceDataSource.updateAlert(updated)
        governanceDataSource.insertActivityEvent(
            DeliveryGovernanceActivityEvent(
                eventId = UUID.randomUUID().toString(),
                alertId = alertId,
                projectId = alert.projectId,
                activityType = DeliveryGovernanceActivityType.ALERT_DISMISSED,
                actorId = actorId,
                details = "Alert dismissed by user '$actorId': $dismissalReason"
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getActivityEvents(
        alertId: String,
        callerRole: UserRole
    ): DomainResult<List<DeliveryGovernanceActivityEvent>> {
        val authResult = DeliveryGovernanceAuthorizationValidator.validateViewGovernance(callerRole)
        if (authResult is DomainResult.Error) return authResult

        val events = governanceDataSource.getActivityEvents(alertId)
        return DomainResult.Success(events)
    }

    override fun observeAlerts(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<DeliveryGovernanceAlert>> {
        if (!callerRole.isInternal) {
            return flowOf(emptyList())
        }
        return governanceDataSource.observeAlerts(projectId)
    }
}
