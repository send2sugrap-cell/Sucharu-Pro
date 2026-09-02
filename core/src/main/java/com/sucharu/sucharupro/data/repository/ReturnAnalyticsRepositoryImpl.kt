package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.ReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.ReturnDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityEvent
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityType
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsSummary
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsTrendPoint
import com.sucharu.sucharupro.domain.model.returns.ReturnDefectBreakdown
import com.sucharu.sucharupro.domain.model.returns.ReturnException
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnFinancialBreakdown
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ReturnAnalyticsRepository
import com.sucharu.sucharupro.domain.service.returns.ReturnAnalyticsEvaluator
import com.sucharu.sucharupro.domain.service.returns.ReturnGovernanceInspector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade implementation of [ReturnAnalyticsRepository] (Module 11 Step 06).
 */
class ReturnAnalyticsRepositoryImpl(
    private val returnDataSource: ReturnDataSource,
    private val analyticsDataSource: ReturnAnalyticsDataSource
) : ReturnAnalyticsRepository {

    private val mutex = Mutex()
    private val authorizedRoles = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.ACCOUNTS)

    override suspend fun getAnalyticsSummary(
        projectId: String,
        period: ReturnAnalyticsPeriod,
        totalDispatchedCount: Int?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnAnalyticsSummary> = mutex.withLock {
        val authCheck = checkAuthorization(projectId, callerRole, callerProjectId)
        if (authCheck != null) return authCheck

        val (returns, settlements, itemsMap) = fetchProjectData(projectId)
        val summary = ReturnAnalyticsEvaluator.evaluateAnalytics(
            projectId = projectId,
            period = period,
            returns = returns,
            settlements = settlements,
            items = itemsMap,
            totalDispatchedCount = totalDispatchedCount
        )
        DomainResult.Success(summary)
    }

    override suspend fun getDefectBreakdown(
        projectId: String,
        period: ReturnAnalyticsPeriod,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnDefectBreakdown>> = mutex.withLock {
        val authCheck = checkAuthorization(projectId, callerRole, callerProjectId)
        if (authCheck != null) return authCheck

        val (returns, _, itemsMap) = fetchProjectData(projectId)
        val nowMillis = System.currentTimeMillis()
        val startMillis = period.calculateStartTimestamp(nowMillis)
        val filteredReturns = returns.filter {
            period == ReturnAnalyticsPeriod.ALL_TIME || it.createdAt >= startMillis
        }
        val breakdown = ReturnAnalyticsEvaluator.calculateDefectBreakdown(filteredReturns, itemsMap)
        DomainResult.Success(breakdown)
    }

    override suspend fun getFinancialBreakdown(
        projectId: String,
        period: ReturnAnalyticsPeriod,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnFinancialBreakdown>> = mutex.withLock {
        val authCheck = checkAuthorization(projectId, callerRole, callerProjectId)
        if (authCheck != null) return authCheck

        val (returns, settlements, _) = fetchProjectData(projectId)
        val nowMillis = System.currentTimeMillis()
        val startMillis = period.calculateStartTimestamp(nowMillis)
        val filteredReturnIds = returns.filter {
            period == ReturnAnalyticsPeriod.ALL_TIME || it.createdAt >= startMillis
        }.map { it.returnId }.toSet()

        val filteredSettlements = settlements.filter { it.returnId in filteredReturnIds }
        val breakdown = ReturnAnalyticsEvaluator.calculateFinancialBreakdown(filteredSettlements)
        DomainResult.Success(breakdown)
    }

    override suspend fun getTrends(
        projectId: String,
        period: ReturnAnalyticsPeriod,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnAnalyticsTrendPoint>> = mutex.withLock {
        val authCheck = checkAuthorization(projectId, callerRole, callerProjectId)
        if (authCheck != null) return authCheck

        val (returns, settlements, itemsMap) = fetchProjectData(projectId)
        val trends = ReturnAnalyticsEvaluator.calculateTrends(
            returns = returns,
            settlements = settlements,
            items = itemsMap,
            period = period
        )
        DomainResult.Success(trends)
    }

    override suspend fun getExceptions(
        projectId: String,
        statusFilter: ReturnExceptionStatus?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnException>> = mutex.withLock {
        val authCheck = checkAuthorization(projectId, callerRole, callerProjectId)
        if (authCheck != null) return authCheck

        val allExceptions = analyticsDataSource.getExceptions(projectId)
        val filtered = if (statusFilter != null) {
            allExceptions.filter { it.status == statusFilter }
        } else {
            allExceptions
        }
        DomainResult.Success(filtered)
    }

    override fun observeExceptions(projectId: String): Flow<List<ReturnException>> {
        return analyticsDataSource.observeExceptions(projectId)
    }

    override suspend fun runGovernanceInspection(
        projectId: String,
        actorId: String,
        totalDispatchedCount: Int?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnException>> = mutex.withLock {
        val authCheck = checkAuthorization(projectId, callerRole, callerProjectId)
        if (authCheck != null) return authCheck

        val (returns, settlements, _) = fetchProjectData(projectId)
        val detected = ReturnGovernanceInspector.inspect(
            projectId = projectId,
            returns = returns,
            settlements = settlements,
            totalDispatchedCount = totalDispatchedCount
        )

        for (exception in detected) {
            val existing = analyticsDataSource.getExceptionByIdempotencyKey(exception.idempotencyKey)
            if (existing == null) {
                analyticsDataSource.insertOrUpdateException(exception)

                // Audit event if linked to a specific return
                if (exception.returnId != null) {
                    val event = ReturnActivityEvent(
                        eventId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        returnId = exception.returnId,
                        activityType = ReturnActivityType.RETURN_EXCEPTION_DETECTED,
                        actorId = actorId,
                        actorRole = callerRole,
                        timestamp = exception.detectedAt,
                        metadata = mapOf(
                            "exceptionId" to exception.exceptionId,
                            "exceptionType" to exception.exceptionType.name,
                            "severity" to exception.severity
                        ),
                        notes = exception.description
                    )
                    returnDataSource.insertActivityEvent(event)
                }
            }
        }

        val allCurrent = analyticsDataSource.getExceptions(projectId)
        DomainResult.Success(allCurrent)
    }

    override suspend fun acknowledgeException(
        exceptionId: String,
        actorId: String,
        expectedVersion: Long,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnException> = mutex.withLock {
        val existing = analyticsDataSource.getExceptionById(exceptionId)
            ?: return DomainResult.Error(message = "Governance exception '$exceptionId' not found.")

        val authCheck = checkAuthorization(existing.projectId, callerRole, callerProjectId)
        if (authCheck != null) return authCheck

        if (existing.version != expectedVersion) {
            return DomainResult.Error(
                message = "Concurrency conflict on exception '$exceptionId': expected version $expectedVersion but found ${existing.version}."
            )
        }

        if (existing.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot acknowledge exception '$exceptionId' in terminal status '${existing.status}'."
            )
        }

        val nowMillis = System.currentTimeMillis()
        val updated = existing.copy(
            status = ReturnExceptionStatus.ACKNOWLEDGED,
            acknowledgedBy = actorId,
            acknowledgedAt = nowMillis,
            version = existing.version + 1
        )
        analyticsDataSource.insertOrUpdateException(updated)

        if (updated.returnId != null) {
            val event = ReturnActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = updated.projectId,
                returnId = updated.returnId,
                activityType = ReturnActivityType.RETURN_EXCEPTION_ACKNOWLEDGED,
                actorId = actorId,
                actorRole = callerRole,
                timestamp = nowMillis,
                metadata = mapOf(
                    "exceptionId" to updated.exceptionId,
                    "acknowledgedBy" to actorId
                ),
                notes = "Exception ${updated.exceptionId} acknowledged by $actorId."
            )
            returnDataSource.insertActivityEvent(event)
        }

        DomainResult.Success(updated)
    }

    override suspend fun resolveException(
        exceptionId: String,
        actorId: String,
        resolutionNotes: String,
        expectedVersion: Long,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnException> = mutex.withLock {
        val existing = analyticsDataSource.getExceptionById(exceptionId)
            ?: return DomainResult.Error(message = "Governance exception '$exceptionId' not found.")

        val authCheck = checkAuthorization(existing.projectId, callerRole, callerProjectId)
        if (authCheck != null) return authCheck

        if (existing.version != expectedVersion) {
            return DomainResult.Error(
                message = "Concurrency conflict on exception '$exceptionId': expected version $expectedVersion but found ${existing.version}."
            )
        }

        if (existing.status.isTerminal) {
            return DomainResult.Error(
                message = "Exception '$exceptionId' is already in terminal status '${existing.status}'."
            )
        }

        val nowMillis = System.currentTimeMillis()
        val updated = existing.copy(
            status = ReturnExceptionStatus.RESOLVED,
            resolvedBy = actorId,
            resolvedAt = nowMillis,
            resolutionNotes = resolutionNotes,
            version = existing.version + 1
        )
        analyticsDataSource.insertOrUpdateException(updated)

        if (updated.returnId != null) {
            val event = ReturnActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = updated.projectId,
                returnId = updated.returnId,
                activityType = ReturnActivityType.RETURN_EXCEPTION_RESOLVED,
                actorId = actorId,
                actorRole = callerRole,
                timestamp = nowMillis,
                metadata = mapOf(
                    "exceptionId" to updated.exceptionId,
                    "resolvedBy" to actorId
                ),
                notes = "Exception ${updated.exceptionId} resolved: $resolutionNotes"
            )
            returnDataSource.insertActivityEvent(event)
        }

        DomainResult.Success(updated)
    }

    override suspend fun dismissException(
        exceptionId: String,
        actorId: String,
        resolutionNotes: String,
        expectedVersion: Long,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnException> = mutex.withLock {
        val existing = analyticsDataSource.getExceptionById(exceptionId)
            ?: return DomainResult.Error(message = "Governance exception '$exceptionId' not found.")

        val authCheck = checkAuthorization(existing.projectId, callerRole, callerProjectId)
        if (authCheck != null) return authCheck

        if (existing.version != expectedVersion) {
            return DomainResult.Error(
                message = "Concurrency conflict on exception '$exceptionId': expected version $expectedVersion but found ${existing.version}."
            )
        }

        if (existing.status.isTerminal) {
            return DomainResult.Error(
                message = "Exception '$exceptionId' is already in terminal status '${existing.status}'."
            )
        }

        val nowMillis = System.currentTimeMillis()
        val updated = existing.copy(
            status = ReturnExceptionStatus.DISMISSED,
            resolvedBy = actorId,
            resolvedAt = nowMillis,
            resolutionNotes = resolutionNotes,
            version = existing.version + 1
        )
        analyticsDataSource.insertOrUpdateException(updated)

        if (updated.returnId != null) {
            val event = ReturnActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = updated.projectId,
                returnId = updated.returnId,
                activityType = ReturnActivityType.RETURN_EXCEPTION_DISMISSED,
                actorId = actorId,
                actorRole = callerRole,
                timestamp = nowMillis,
                metadata = mapOf(
                    "exceptionId" to updated.exceptionId,
                    "dismissedBy" to actorId
                ),
                notes = "Exception ${updated.exceptionId} dismissed: $resolutionNotes"
            )
            returnDataSource.insertActivityEvent(event)
        }

        DomainResult.Success(updated)
    }

    private fun checkAuthorization(
        projectId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult.Error? {
        if (callerProjectId != null && callerProjectId != projectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot access analytics for project '$projectId'."
            )
        }
        if (callerRole != null && callerRole !in authorizedRoles) {
            return DomainResult.Error(
                message = "Access denied: Role '$callerRole' is unauthorized for Return Analytics & Governance. Requires ADMIN, MANAGER, or ACCOUNTS."
            )
        }
        return null
    }

    private suspend fun fetchProjectData(projectId: String): Triple<List<com.sucharu.sucharupro.domain.model.returns.ReturnRequest>, List<ReturnSettlement>, Map<String, List<ReturnItem>>> {
        val returns = returnDataSource.getReturnsByProject(projectId)
        val settlements = mutableListOf<ReturnSettlement>()
        val itemsMap = mutableMapOf<String, List<ReturnItem>>()

        for (ret in returns) {
            val items = returnDataSource.getReturnItems(ret.returnId)
            itemsMap[ret.returnId] = items
            val settlement = returnDataSource.getSettlement(ret.returnId)
            if (settlement != null) {
                settlements.add(settlement)
            }
        }
        return Triple(returns, settlements, itemsMap)
    }
}
