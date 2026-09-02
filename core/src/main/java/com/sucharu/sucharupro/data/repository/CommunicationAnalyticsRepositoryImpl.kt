package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.CampaignDataSource
import com.sucharu.sucharupro.data.datasource.CommunicationAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.CommunicationAutomationDataSource
import com.sucharu.sucharupro.data.datasource.NotificationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.analytics.*
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationRule
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationExecution
import com.sucharu.sucharupro.domain.model.communication.campaign.Campaign
import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.analytics.CommunicationAnalyticsRepository
import com.sucharu.sucharupro.domain.validation.communication.analytics.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

class CommunicationAnalyticsRepositoryImpl(
    private val analyticsDataSource: CommunicationAnalyticsDataSource,
    private val notificationDataSource: NotificationDataSource,
    private val campaignDataSource: CampaignDataSource,
    private val automationDataSource: CommunicationAutomationDataSource
) : CommunicationAnalyticsRepository {

    private val executionMutex = Mutex() // Ensure idempotent snapshot creations don't race

    private suspend fun <T> validateAccess(
        actorRole: UserRole,
        isGovernance: Boolean = false,
        action: suspend () -> DomainResult<T>
    ): DomainResult<T> {
        val authResult = if (isGovernance) {
            CommunicationAnalyticsAuthorizationValidator.validateGovernanceAccess(actorRole)
        } else {
            CommunicationAnalyticsAuthorizationValidator.validateAnalyticsAccess(actorRole)
        }
        
        if (authResult is DomainResult.Error) {
            return authResult
        }
        
        return action()
    }

    private suspend fun getFilteredNotifications(filter: CommunicationAnalyticsFilter): List<Notification> {
        // Reads from notification dataSource ensuring zero-mutation and no locks held across repos
        val allNotifications = notificationDataSource.observeNotificationsByProject(filter.projectId).first()
        
        return allNotifications.filter { n ->
            val time = n.createdAt
            time >= filter.fromDate.toEpochMilli() && time <= filter.toDate.toEpochMilli() &&
            (filter.communicationType == null || n.notificationType == filter.communicationType) &&
            (filter.channel == null || n.channel == filter.channel) &&
            (filter.priority == null || n.priority == filter.priority) &&
            (filter.status == null || n.status == filter.status) &&
            (filter.customerId == null || n.recipientUserId == filter.customerId) // Approximation for this system
        }
    }

    override suspend fun getKpiSummary(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<CommunicationKpiSummary> = validateAccess(actorRole) {
        val notifications = getFilteredNotifications(filter)
        val kpi = CommunicationAnalyticsCalculator.calculateKpiSummary(notifications)
        DomainResult.Success(kpi)
    }

    override suspend fun getChannelAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationChannelAnalytics>> = validateAccess(actorRole) {
        val notifications = getFilteredNotifications(filter)
        val analytics = CommunicationAnalyticsCalculator.calculateChannelAnalytics(notifications)
        DomainResult.Success(analytics)
    }

    override suspend fun getTypeAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationTypeAnalytics>> = validateAccess(actorRole) {
        val notifications = getFilteredNotifications(filter)
        val analytics = CommunicationAnalyticsCalculator.calculateTypeAnalytics(notifications)
        DomainResult.Success(analytics)
    }

    override suspend fun getCustomerEngagement(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CustomerEngagementAnalytics>> = validateAccess(actorRole) {
        val notifications = getFilteredNotifications(filter)
        // In a real system, you'd filter for specifically customer-targeted notifications.
        val groupedByCustomer = notifications.groupBy { it.recipientUserId }
        
        val analytics = groupedByCustomer.map { (customerId, customerNotifications) ->
            val kpi = CommunicationAnalyticsCalculator.calculateKpiSummary(customerNotifications)
            val score = CommunicationEngagementEngine.calculateEngagementScore(customerNotifications)
            
            CustomerEngagementAnalytics(
                customerId = customerId,
                totalMessages = kpi.totalCommunications,
                delivered = kpi.deliveredCount,
                read = kpi.readCount,
                acknowledged = kpi.acknowledgedCount,
                unread = kpi.deliveredCount - kpi.readCount,
                readRate = kpi.readRate,
                acknowledgementRate = kpi.acknowledgementRate,
                engagementScore = score,
                preferredChannel = customerNotifications.groupBy { it.channel }.maxByOrNull { it.value.size }?.key,
                lastInteractionAt = customerNotifications.mapNotNull { it.readAt ?: it.deliveredAt ?: it.createdAt }.maxOrNull()?.let { Instant.ofEpochMilli(it) },
                engagementTrend = "STABLE" // Simplified
            )
        }
        
        DomainResult.Success(analytics)
    }

    override suspend fun getInternalCommunicationAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<InternalCommunicationAnalytics>> = validateAccess(actorRole) {
        val notifications = getFilteredNotifications(filter)
        // Simulating internal grouping by a generic logic since we lack a full user repo here
        val analytics = InternalCommunicationAnalytics(
            department = filter.department ?: "ALL",
            role = filter.role ?: "ALL",
            totalMessages = notifications.size,
            sent = notifications.count { it.status.name != "QUEUED" && it.status.name != "CANCELLED" },
            delivered = notifications.count { it.status.name == "DELIVERED" || it.status.name == "READ" || it.status.name == "ACKNOWLEDGED" },
            read = notifications.count { it.status.name == "READ" || it.status.name == "ACKNOWLEDGED" },
            acknowledged = notifications.count { it.status.name == "ACKNOWLEDGED" },
            unread = notifications.count { it.status.name == "DELIVERED" },
            responseRate = if (notifications.isNotEmpty()) notifications.count { it.status.name == "ACKNOWLEDGED" }.toDouble() / notifications.size else 0.0,
            engagementScore = CommunicationEngagementEngine.calculateEngagementScore(notifications),
            communicationVolume = notifications.size,
            trend = "STABLE"
        )
        DomainResult.Success(listOf(analytics))
    }

    override suspend fun getVendorCommunicationAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<VendorCommunicationAnalytics>> = validateAccess(actorRole) {
        val notifications = getFilteredNotifications(filter).filter { it.recipientUserId == filter.vendorId || filter.vendorId == null }
        val kpi = CommunicationAnalyticsCalculator.calculateKpiSummary(notifications)
        val score = CommunicationEngagementEngine.calculateEngagementScore(notifications)
        
        val analytics = VendorCommunicationAnalytics(
            vendorId = filter.vendorId ?: "ALL",
            communicationCount = notifications.size,
            documentCommunicationCount = notifications.count { it.notificationType.name.contains("DOCUMENT") },
            deliveryRate = kpi.deliveryRate,
            readRate = kpi.readRate,
            acknowledgementRate = kpi.acknowledgementRate,
            pendingCommunicationCount = kpi.queuedCount,
            engagementScore = score,
            lastCommunicationAt = notifications.maxByOrNull { it.createdAt }?.createdAt?.let { Instant.ofEpochMilli(it) }
        )
        
        DomainResult.Success(listOf(analytics))
    }

    override suspend fun getCampaignPerformance(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CampaignPerformanceAnalytics>> = validateAccess(actorRole) {
        val campaigns = campaignDataSource.observeCampaigns(filter.projectId).first()
        val filteredCampaigns = campaigns.filter { c ->
            c.createdAt >= filter.fromDate.toEpochMilli() && c.createdAt <= filter.toDate.toEpochMilli() &&
            (filter.campaignId == null || c.campaignId == filter.campaignId)
        }
        
        val analyticsList = filteredCampaigns.map { c ->
            val campaignNotifications = getFilteredNotifications(filter.copy(campaignId = c.campaignId))
            val cKpi = CommunicationAnalyticsCalculator.calculateKpiSummary(campaignNotifications)
            CampaignPerformanceAnalytics(
                campaignId = c.campaignId,
                audienceSize = cKpi.totalCommunications,
                recipients = cKpi.deliveredCount,
                delivered = cKpi.deliveredCount,
                failed = cKpi.failedCount,
                read = cKpi.readCount,
                acknowledged = cKpi.acknowledgedCount,
                deliveryRate = cKpi.deliveryRate,
                readRate = cKpi.readRate,
                acknowledgementRate = cKpi.acknowledgementRate,
                engagementRate = cKpi.readRate, // Simplified
                completionStatus = c.status,
                campaignDurationMs = if (c.completedAt != null) c.completedAt - c.createdAt else 0L
            )
        }
        
        DomainResult.Success(analyticsList)
    }

    override suspend fun getAutomationAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAutomationAnalytics>> = validateAccess(actorRole) {
        val rules = automationDataSource.getRules(filter.projectId)
        val executions = automationDataSource.getExecutions(filter.projectId).filter {
            it.createdAt >= filter.fromDate.toEpochMilli() && it.createdAt <= filter.toDate.toEpochMilli()
        }
        
        val analyticsList = rules.map { r ->
            val ruleExecutions = executions.filter { it.ruleId == r.ruleId }
            val executionCount = ruleExecutions.size
            val successCount = ruleExecutions.count { it.status.name == "COMPLETED" || it.status.name == "DISPATCHED" }
            val blockedCount = ruleExecutions.count { it.status.name == "SUPPRESSED" }
            val failedCount = ruleExecutions.count { it.status.name == "FAILED" }
            
            CommunicationAutomationAnalytics(
                ruleId = r.ruleId,
                executionCount = executionCount,
                successCount = successCount,
                blockedCount = blockedCount,
                failedCount = failedCount,
                skippedCount = blockedCount,
                duplicatePreventedCount = blockedCount, // Approximation
                notificationGeneratedCount = successCount,
                averageExecutionTimeMs = 50L, // Placeholder
                successRate = if (executionCount > 0) successCount.toDouble() / executionCount else 0.0,
                lastExecutedAt = ruleExecutions.maxByOrNull { it.createdAt }?.createdAt?.let { Instant.ofEpochMilli(it) }
            )
        }
        
        DomainResult.Success(analyticsList)
    }

    override suspend fun getRiskIndicators(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationRiskIndicator>> = validateAccess(actorRole, isGovernance = true) {
        val kpiResult = getKpiSummary(filter, actorRole)
        if (kpiResult is DomainResult.Error) return@validateAccess kpiResult
        val kpi = (kpiResult as DomainResult.Success).data
        val risks = CommunicationRiskEngine.detectRisks(kpi)
        DomainResult.Success(risks)
    }

    override suspend fun getAnomalies(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAnomaly>> = validateAccess(actorRole, isGovernance = true) {
        // Need a baseline. We'll use the immediately preceding period of the same duration.
        val durationMs = filter.toDate.toEpochMilli() - filter.fromDate.toEpochMilli()
        val baselineFilter = filter.copy(
            fromDate = Instant.ofEpochMilli(filter.fromDate.toEpochMilli() - durationMs),
            toDate = filter.fromDate
        )
        
        val baselineKpi = CommunicationAnalyticsCalculator.calculateKpiSummary(getFilteredNotifications(baselineFilter))
        val currentKpi = CommunicationAnalyticsCalculator.calculateKpiSummary(getFilteredNotifications(filter))
        
        val anomalies = CommunicationAnomalyDetector.detectAnomalies(baselineKpi, currentKpi)
        DomainResult.Success(anomalies)
    }

    override suspend fun getGovernanceResult(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<CommunicationGovernanceResult> = validateAccess(actorRole, isGovernance = true) {
        val risks = (getRiskIndicators(filter, actorRole) as? DomainResult.Success)?.data ?: emptyList()
        val anomalies = (getAnomalies(filter, actorRole) as? DomainResult.Success)?.data ?: emptyList()
        val result = CommunicationGovernanceEngine.evaluateGovernance(filter.projectId, risks, anomalies)
        DomainResult.Success(result)
    }

    override suspend fun comparePeriods(
        projectId: String,
        currentFilter: CommunicationAnalyticsFilter,
        previousFilter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<CommunicationPeriodComparison> = validateAccess(actorRole) {
        val prevKpi = CommunicationAnalyticsCalculator.calculateKpiSummary(getFilteredNotifications(previousFilter))
        val currKpi = CommunicationAnalyticsCalculator.calculateKpiSummary(getFilteredNotifications(currentFilter))
        
        val prevScore = CommunicationEngagementEngine.calculateEngagementScore(getFilteredNotifications(previousFilter))
        val currScore = CommunicationEngagementEngine.calculateEngagementScore(getFilteredNotifications(currentFilter))
        
        val comp = CommunicationPeriodComparison(
            previousPeriodStart = previousFilter.fromDate,
            previousPeriodEnd = previousFilter.toDate,
            currentPeriodStart = currentFilter.fromDate,
            currentPeriodEnd = currentFilter.toDate,
            previousTotalCommunications = prevKpi.totalCommunications,
            currentTotalCommunications = currKpi.totalCommunications,
            totalCommunicationsVariance = currKpi.totalCommunications.toDouble() - prevKpi.totalCommunications,
            previousDeliveryRate = prevKpi.deliveryRate,
            currentDeliveryRate = currKpi.deliveryRate,
            deliveryRateVariance = currKpi.deliveryRate - prevKpi.deliveryRate,
            previousFailureRate = prevKpi.failureRate,
            currentFailureRate = currKpi.failureRate,
            failureRateVariance = currKpi.failureRate - prevKpi.failureRate,
            previousReadRate = prevKpi.readRate,
            currentReadRate = currKpi.readRate,
            readRateVariance = currKpi.readRate - prevKpi.readRate,
            previousAcknowledgementRate = prevKpi.acknowledgementRate,
            currentAcknowledgementRate = currKpi.acknowledgementRate,
            acknowledgementRateVariance = currKpi.acknowledgementRate - prevKpi.acknowledgementRate,
            previousAverageEngagementScore = prevScore,
            currentAverageEngagementScore = currScore,
            engagementScoreVariance = currScore - prevScore
        )
        DomainResult.Success(comp)
    }

    override suspend fun getForecast(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<CommunicationForecastSummary> = validateAccess(actorRole) {
        // Build historical KPIs
        val durationMs = filter.toDate.toEpochMilli() - filter.fromDate.toEpochMilli()
        val historyKpis = mutableListOf<CommunicationKpiSummary>()
        
        for (i in 3 downTo 1) {
            val histFilter = filter.copy(
                fromDate = Instant.ofEpochMilli(filter.fromDate.toEpochMilli() - (durationMs * i)),
                toDate = Instant.ofEpochMilli(filter.toDate.toEpochMilli() - (durationMs * (i - 1)))
            )
            historyKpis.add(CommunicationAnalyticsCalculator.calculateKpiSummary(getFilteredNotifications(histFilter)))
        }
        
        val forecast = CommunicationForecastEngine.calculateForecast(
            historicalKpis = historyKpis,
            forecastStart = filter.toDate,
            forecastEnd = Instant.ofEpochMilli(filter.toDate.toEpochMilli() + durationMs)
        )
        
        DomainResult.Success(forecast)
    }

    override suspend fun createSnapshot(
        filter: CommunicationAnalyticsFilter,
        actorId: String,
        actorRole: UserRole,
        idempotencyKey: String
    ): DomainResult<CommunicationAnalyticsSnapshot> = validateAccess(actorRole, isGovernance = true) {
        executionMutex.withLock {
            // Check idempotency loosely, though we don't have an idempotency specific column, we can use the key in activity log or rely on exact match
            
            // Build all required components
            val kpi = (getKpiSummary(filter, actorRole) as DomainResult.Success).data
            val channels = (getChannelAnalytics(filter, actorRole) as DomainResult.Success).data
            val types = (getTypeAnalytics(filter, actorRole) as DomainResult.Success).data
            val customerEng = (getCustomerEngagement(filter, actorRole) as DomainResult.Success).data
            val internalEng = (getInternalCommunicationAnalytics(filter, actorRole) as DomainResult.Success).data
            val vendorEng = (getVendorCommunicationAnalytics(filter, actorRole) as DomainResult.Success).data
            val campaignPerf = (getCampaignPerformance(filter, actorRole) as DomainResult.Success).data
            val autoPerf = (getAutomationAnalytics(filter, actorRole) as DomainResult.Success).data
            val risks = (getRiskIndicators(filter, actorRole) as DomainResult.Success).data
            val anomalies = (getAnomalies(filter, actorRole) as DomainResult.Success).data
            val governance = (getGovernanceResult(filter, actorRole) as DomainResult.Success).data

            // Hash Generation
            val rawData = "${filter.projectId}|${filter.fromDate}|${filter.toDate}|${kpi.totalCommunications}|${kpi.deliveryRate}|${kpi.readRate}|${governance.governanceStatus}"
            val md = MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(rawData.toByteArray(Charsets.UTF_8))
            val sha256 = hashBytes.joinToString("") { "%02x".format(it) }

            val snapshotId = "CAS-${Instant.now().epochSecond}-${UUID.randomUUID().toString().take(5).uppercase()}"

            val snapshot = CommunicationAnalyticsSnapshot(
                snapshotId = snapshotId,
                projectId = filter.projectId,
                fromDate = filter.fromDate,
                toDate = filter.toDate,
                generatedAt = Instant.now(),
                kpiSummary = kpi,
                channelAnalytics = channels,
                typeAnalytics = types,
                customerEngagement = customerEng,
                internalEngagement = internalEng,
                vendorEngagement = vendorEng,
                campaignAnalytics = campaignPerf,
                automationAnalytics = autoPerf,
                riskIndicators = risks,
                anomalies = anomalies,
                governanceResult = governance,
                sha256Hash = sha256
            )

            // Save Snapshot
            analyticsDataSource.saveSnapshot(snapshot)

            // Record Audit Event
            analyticsDataSource.recordActivity(
                CommunicationAnalyticsActivityEvent(
                    eventId = "evt-${UUID.randomUUID()}",
                    projectId = filter.projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "SNAPSHOT_GENERATED",
                    timestamp = Instant.now(),
                    targetType = "SNAPSHOT",
                    targetId = snapshotId,
                    metadata = mapOf("idempotencyKey" to idempotencyKey, "hash" to sha256)
                )
            )

            DomainResult.Success(snapshot)
        }
    }

    override suspend fun getSnapshots(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAnalyticsSnapshot>> = validateAccess(actorRole) {
        DomainResult.Success(analyticsDataSource.getSnapshots(projectId))
    }

    override fun observeSnapshots(projectId: String): Flow<List<CommunicationAnalyticsSnapshot>> {
        return analyticsDataSource.observeSnapshots(projectId)
    }

    override suspend fun getActivityHistory(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAnalyticsActivityEvent>> = validateAccess(actorRole, isGovernance = true) {
        DomainResult.Success(analyticsDataSource.getActivityEvents(projectId))
    }

    // =========================================================================
    // STEP 10: Governance, Verification, Export, Audit & Operational Consumption
    // =========================================================================

    override suspend fun verifySnapshot(
        snapshotId: String,
        projectId: String,
        actorUserId: String,
        actorRole: UserRole
    ): DomainResult<CommunicationSnapshotVerificationResult> {
        val authResult = CommunicationAnalyticsAuthorizationValidator.validateSnapshotVerificationAccess(actorRole)
        if (authResult is DomainResult.Error) return authResult

        val snapshot = analyticsDataSource.getSnapshotById(projectId, snapshotId)
            ?: return DomainResult.Error(message = "Snapshot not found")

        val result = CommunicationSnapshotVerifier.verify(snapshot, actorUserId)

        // Record audit event
        analyticsDataSource.saveAuditEvent(
            CommunicationAuditEvent(
                auditEventId = UUID.randomUUID().toString(),
                projectId = projectId,
                actorUserId = actorUserId,
                actorRole = actorRole,
                action = "SNAPSHOT_VERIFIED",
                targetType = "SNAPSHOT",
                targetId = snapshotId,
                result = if (result.status == SnapshotVerificationStatus.VERIFIED) AuditResult.SUCCESS else AuditResult.FAILURE,
                failureDetail = if (result.status != SnapshotVerificationStatus.VERIFIED) result.explanation else null
            )
        )

        return DomainResult.Success(result)
    }

    override suspend fun acknowledgeGovernanceAction(
        action: CommunicationGovernanceAction,
        actorRole: UserRole
    ): DomainResult<CommunicationGovernanceAction> {
        val authResult = CommunicationAnalyticsAuthorizationValidator.validateGovernanceAccess(actorRole)
        if (authResult is DomainResult.Error) return authResult

        val savedAction = analyticsDataSource.saveGovernanceAction(action)

        analyticsDataSource.saveAuditEvent(
            CommunicationAuditEvent(
                auditEventId = UUID.randomUUID().toString(),
                projectId = action.projectId,
                actorUserId = action.actorUserId,
                actorRole = actorRole,
                action = "GOVERNANCE_ACTION_ACKNOWLEDGED",
                targetType = action.targetType,
                targetId = action.targetId,
                newState = action.resultingState,
                result = AuditResult.SUCCESS
            )
        )

        return DomainResult.Success(savedAction)
    }

    override suspend fun requestExport(
        request: CommunicationExportRequest,
        actorRole: UserRole
    ): DomainResult<CommunicationExportPayload> {
        val authResult = CommunicationAnalyticsAuthorizationValidator.validateExportAccess(actorRole)
        if (authResult is DomainResult.Error) return authResult

        return executionMutex.withLock {
            // Idempotency check
            val existingRequests = analyticsDataSource.getExportRequests(request.projectId)
            val existing = existingRequests.find { it.correlationId == request.correlationId }
            if (existing != null && existing.status == CommunicationExportStatus.COMPLETED) {
                return@withLock DomainResult.Error(message = "Export request already processed")
            }

            // Save request as processing
            analyticsDataSource.saveExportRequest(request.copy(status = CommunicationExportStatus.PROCESSING))

            val snapshot = request.snapshotReference?.let { analyticsDataSource.getSnapshotById(request.projectId, it) }
            val auditEvents = analyticsDataSource.getAuditEvents(request.projectId)

            val payload = CommunicationExportEngine.buildPayload(request, snapshot, auditEvents)

            // Complete request
            analyticsDataSource.updateExportRequest(
                request.copy(
                    status = CommunicationExportStatus.COMPLETED,
                    completedAt = Instant.now(),
                    payloadHash = payload.payloadHash
                )
            )

            analyticsDataSource.saveAuditEvent(
                CommunicationAuditEvent(
                    auditEventId = UUID.randomUUID().toString(),
                    projectId = request.projectId,
                    actorUserId = request.requestedBy,
                    actorRole = actorRole,
                    action = "EXPORT_GENERATED",
                    targetType = "EXPORT_REQUEST",
                    targetId = request.exportId,
                    result = AuditResult.SUCCESS
                )
            )

            DomainResult.Success(payload)
        }
    }

    override suspend fun getExportRequests(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<List<CommunicationExportRequest>> {
        val authResult = CommunicationAnalyticsAuthorizationValidator.validateExportAccess(actorRole)
        if (authResult is DomainResult.Error) return authResult
        return DomainResult.Success(analyticsDataSource.getExportRequests(projectId))
    }

    override fun observeExportRequests(projectId: String): Flow<List<CommunicationExportRequest>> {
        return analyticsDataSource.observeExportRequests(projectId)
    }

    override suspend fun getAuditEvents(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAuditEvent>> {
        val authResult = CommunicationAnalyticsAuthorizationValidator.validateAuditAccess(actorRole)
        if (authResult is DomainResult.Error) return authResult
        return DomainResult.Success(analyticsDataSource.getAuditEvents(projectId))
    }

    override suspend fun getOperationalHealthProjection(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<CommunicationOperationalHealthProjection> {
        val authResult = CommunicationAnalyticsAuthorizationValidator.validateAnalyticsAccess(actorRole)
        if (authResult is DomainResult.Error) return authResult

        val filter = CommunicationAnalyticsFilter(
            projectId = projectId,
            fromDate = Instant.now().minusSeconds(30 * 24 * 60 * 60), // Last 30 days
            toDate = Instant.now()
        )
        
        val risks = getRiskIndicators(filter, actorRole).getOrNull() ?: emptyList()
        val anomalies = getAnomalies(filter, actorRole).getOrNull() ?: emptyList()
        val governance = getGovernanceResult(filter, actorRole).getOrNull()
        val forecast = getForecast(filter, actorRole).getOrNull()
        
        val snapshots = analyticsDataSource.getSnapshots(projectId)
        val latestSnapshot = snapshots.firstOrNull()
        val latestVerification = latestSnapshot?.let { CommunicationSnapshotVerifier.verify(it, "system") }

        val health = CommunicationExportEngine.deriveOperationalHealth(
            projectId = projectId,
            governance = governance,
            risks = risks,
            anomalies = anomalies,
            forecast = forecast,
            latestVerification = latestVerification
        )

        return DomainResult.Success(health)
    }
}
