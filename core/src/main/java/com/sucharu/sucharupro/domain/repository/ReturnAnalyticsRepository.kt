package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsSummary
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsTrendPoint
import com.sucharu.sucharupro.domain.model.returns.ReturnDefectBreakdown
import com.sucharu.sucharupro.domain.model.returns.ReturnException
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnFinancialBreakdown
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Return Analytics and Governance Exceptions (Module 11 Step 06).
 */
interface ReturnAnalyticsRepository {

    suspend fun getAnalyticsSummary(
        projectId: String,
        period: ReturnAnalyticsPeriod = ReturnAnalyticsPeriod.THIS_MONTH,
        totalDispatchedCount: Int? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnAnalyticsSummary>

    suspend fun getDefectBreakdown(
        projectId: String,
        period: ReturnAnalyticsPeriod = ReturnAnalyticsPeriod.THIS_MONTH,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnDefectBreakdown>>

    suspend fun getFinancialBreakdown(
        projectId: String,
        period: ReturnAnalyticsPeriod = ReturnAnalyticsPeriod.THIS_MONTH,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnFinancialBreakdown>>

    suspend fun getTrends(
        projectId: String,
        period: ReturnAnalyticsPeriod = ReturnAnalyticsPeriod.THIS_MONTH,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnAnalyticsTrendPoint>>

    suspend fun getExceptions(
        projectId: String,
        statusFilter: ReturnExceptionStatus? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnException>>

    fun observeExceptions(
        projectId: String
    ): Flow<List<ReturnException>>

    suspend fun runGovernanceInspection(
        projectId: String,
        actorId: String,
        totalDispatchedCount: Int? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnException>>

    suspend fun acknowledgeException(
        exceptionId: String,
        actorId: String,
        expectedVersion: Long,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnException>

    suspend fun resolveException(
        exceptionId: String,
        actorId: String,
        resolutionNotes: String,
        expectedVersion: Long,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnException>

    suspend fun dismissException(
        exceptionId: String,
        actorId: String,
        resolutionNotes: String,
        expectedVersion: Long,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnException>
}
