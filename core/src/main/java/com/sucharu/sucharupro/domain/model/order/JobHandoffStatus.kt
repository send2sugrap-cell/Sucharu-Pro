package com.sucharu.sucharupro.domain.model.order

/**
 * Minimal handoff indicator determining whether an Order is ready to transition to Module 04 (Job Card & Production).
 */
enum class JobHandoffStatus(val defaultLabel: String) {
    /** Awaiting advance deposit, artwork confirmation, or administrative review. */
    NOT_READY("Not Ready for Job"),

    /** Fully confirmed and eligible for production job handoff. */
    READY_FOR_JOB("Ready for Job Handoff")
}
