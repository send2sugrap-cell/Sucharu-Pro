package com.sucharu.sucharupro.domain.validation.communication.campaign

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignLifecycleTest {

    @Test
    fun transition_sameStatus_alwaysSucceeds() {
        CampaignStatus.entries.forEach { status ->
            val result = CampaignLifecycleValidator.validateTransition(status, status)
            assertTrue("Same status transition should succeed for $status", result is DomainResult.Success)
        }
    }

    @Test
    fun transition_draft_to_pendingApproval_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.DRAFT, CampaignStatus.PENDING_APPROVAL)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_draft_to_approved_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.DRAFT, CampaignStatus.APPROVED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_draft_to_cancelled_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.DRAFT, CampaignStatus.CANCELLED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_pendingApproval_to_approved_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.PENDING_APPROVAL, CampaignStatus.APPROVED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_pendingApproval_to_rejected_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.PENDING_APPROVAL, CampaignStatus.REJECTED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_approved_to_published_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.APPROVED, CampaignStatus.PUBLISHED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_approved_to_scheduled_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.APPROVED, CampaignStatus.SCHEDULED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_scheduled_to_published_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.SCHEDULED, CampaignStatus.PUBLISHED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_published_to_completed_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.PUBLISHED, CampaignStatus.COMPLETED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_rejected_to_draft_succeeds() {
        val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.REJECTED, CampaignStatus.DRAFT)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun transition_fromCompleted_toAnyOther_fails() {
        CampaignStatus.entries.filter { it != CampaignStatus.COMPLETED }.forEach { target ->
            val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.COMPLETED, target)
            assertTrue("Completed terminal state cannot transition to $target", result is DomainResult.Error)
        }
    }

    @Test
    fun transition_fromCancelled_toAnyOther_fails() {
        CampaignStatus.entries.filter { it != CampaignStatus.CANCELLED }.forEach { target ->
            val result = CampaignLifecycleValidator.validateTransition(CampaignStatus.CANCELLED, target)
            assertTrue("Cancelled terminal state cannot transition to $target", result is DomainResult.Error)
        }
    }
}
