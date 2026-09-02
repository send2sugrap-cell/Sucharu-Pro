package com.sucharu.sucharupro.domain.model.communication.campaign

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Provider-neutral interface for scheduling and executing campaigns and broadcasts (Module 10 Step 07).
 */
interface CampaignScheduler {
    suspend fun scheduleCampaign(projectId: String, campaignId: String, scheduledAt: Long): DomainResult<Unit>
    suspend fun cancelScheduledCampaign(projectId: String, campaignId: String): DomainResult<Unit>
    suspend fun executeDueCampaigns(projectId: String): DomainResult<List<String>>
}
