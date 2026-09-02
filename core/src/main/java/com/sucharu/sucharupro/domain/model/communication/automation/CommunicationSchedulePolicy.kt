package com.sucharu.sucharupro.domain.model.communication.automation

/**
 * Scheduling policies for automation execution (Module 10 Step 08).
 */
data class CommunicationSchedulePolicy(
    val type: SchedulePolicyType = SchedulePolicyType.IMMEDIATE,
    val delayMs: Long = 0L,
    val targetEpochTime: Long? = null
)

enum class SchedulePolicyType(val defaultLabel: String) {
    IMMEDIATE("Immediate Dispatch"),
    DELAYED("Delayed Dispatch"),
    SCHEDULED("Scheduled Time"),
    BUSINESS_HOURS("Next Business Hours"),
    NEXT_WORKING_DAY("Next Working Day")
}
