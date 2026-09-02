package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationEventType
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationRule
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationTriggerEvent
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import org.junit.Assert.assertEquals
import org.junit.Test

class CommunicationAutomationRecipientTest {

    private fun sampleCandidates() = listOf(
        CommunicationAutomationRecipientResolver.CandidateAutomationRecipient("proj-1", "cus-1", "CUSTOMER", "cus-1"),
        CommunicationAutomationRecipientResolver.CandidateAutomationRecipient("proj-1", "ven-1", "VENDOR", "ven-1"),
        CommunicationAutomationRecipientResolver.CandidateAutomationRecipient("proj-1", "staff-1", "STAFF", "staff-1"),
        CommunicationAutomationRecipientResolver.CandidateAutomationRecipient("proj-2", "cus-2", "CUSTOMER", "cus-2") // Wrong project
    )

    private fun baseTrigger() = CommunicationTriggerEvent(
        triggerId = "trg-1",
        projectId = "proj-1",
        eventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
        sourceEntityType = "CUSTOMER",
        sourceEntityId = "cus-1",
        actorUserId = "sys"
    )

    private fun baseRule(audienceType: CampaignAudienceType) = CommunicationAutomationRule(
        ruleId = "rule-1",
        ruleNo = "AUT-001",
        projectId = "proj-1",
        name = "Test",
        eventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
        audienceType = audienceType,
        titleTemplate = "T",
        messageTemplate = "M",
        createdBy = "admin"
    )

    @Test
    fun resolveRecipients_projectIsolation_excludesOtherProjects() {
        val trigger = baseTrigger()
        val rule = baseRule(CampaignAudienceType.ALL_PROJECT_USERS)
        val recipients = CommunicationAutomationRecipientResolver.resolveRecipients(trigger, rule, sampleCandidates())

        // Should include cus-1, ven-1, staff-1, but NOT cus-2 (proj-2)
        assertEquals(3, recipients.size)
        assertEquals(false, recipients.contains("cus-2"))
    }

    @Test
    fun resolveRecipients_customerSegment_targetsCorrectEntity() {
        val trigger = baseTrigger()
        val rule = baseRule(CampaignAudienceType.CUSTOMER_SEGMENT)
        val recipients = CommunicationAutomationRecipientResolver.resolveRecipients(trigger, rule, sampleCandidates())

        assertEquals(1, recipients.size)
        assertEquals("cus-1", recipients.first())
    }

    @Test
    fun resolveRecipients_internalOnlyBoundary_excludesExternalUsers() {
        // A rule with ROLE audience should default to STAFF/USER and NEVER return CUSTOMER/VENDOR
        val trigger = baseTrigger()
        val rule = baseRule(CampaignAudienceType.ROLE)
        val recipients = CommunicationAutomationRecipientResolver.resolveRecipients(trigger, rule, sampleCandidates())

        assertEquals(1, recipients.size)
        assertEquals("staff-1", recipients.first())
    }
}
