package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsBreakdown
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsSummary
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsTrend
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for Delivery Analytics & Governance (Module 08 Step 10).
 */
interface DeliveryAnalyticsRepository {
    suspend fun getSummary(
        filter: DeliveryAnalyticsFilter,
        callerRole: UserRole
    ): DomainResult<DeliveryAnalyticsSummary>

    suspend fun getBreakdown(
        filter: DeliveryAnalyticsFilter,
        callerRole: UserRole
    ): DomainResult<DeliveryAnalyticsBreakdown>

    suspend fun getTrends(
        projectId: String,
        period: DeliveryAnalyticsPeriod,
        callerRole: UserRole
    ): DomainResult<DeliveryAnalyticsTrend>

    suspend fun refreshGovernanceAlerts(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<DeliveryGovernanceAlert>>

    suspend fun getAlerts(
        projectId: String,
        callerRole: UserRole
    ): DomainResult<List<DeliveryGovernanceAlert>>

    suspend fun getAlertById(
        alertId: String,
        callerRole: UserRole
    ): DomainResult<DeliveryGovernanceAlert>

    suspend fun acknowledgeAlert(
        alertId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<DeliveryGovernanceAlert>

    suspend fun resolveAlert(
        alertId: String,
        actorId: String,
        resolutionNotes: String,
        callerRole: UserRole
    ): DomainResult<DeliveryGovernanceAlert>

    suspend fun dismissAlert(
        alertId: String,
        actorId: String,
        dismissalReason: String,
        callerRole: UserRole
    ): DomainResult<DeliveryGovernanceAlert>

    suspend fun getActivityEvents(
        alertId: String,
        callerRole: UserRole
    ): DomainResult<List<DeliveryGovernanceActivityEvent>>

    fun observeAlerts(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<DeliveryGovernanceAlert>>
}
