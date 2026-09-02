package com.sucharu.sucharupro.domain.model.design

/**
 * Categorical types of discrete audit/activity events in the Design domain.
 */
enum class DesignActivityType(val defaultLabel: String) {
    PROJECT_CREATED("Design Project Created"),
    DESIGNER_ASSIGNED("Designer Assigned"),
    DESIGNER_REASSIGNED("Designer Reassigned"),
    DESIGNER_UNASSIGNED("Designer Unassigned"),
    STATUS_CHANGED("Status Changed"),
    PROJECT_STARTED("Design Work Started"),
    PROJECT_CANCELLED("Design Project Cancelled"),
    NOTE_ADDED("Note Added"),

    // Step 02: Artwork & File Management Activity Types
    ARTWORK_CREATED("Artwork Created"),
    ARTWORK_VERSION_CREATED("Artwork Version Created"),
    ARTWORK_UPDATED("Artwork Updated"),
    ARTWORK_ARCHIVED("Artwork Archived"),
    ARTWORK_VERSION_ARCHIVED("Artwork Version Archived"),
    FILE_ATTACHED("File Attached"),

    // Step 03: Proof & Revision Workflow Activity Types
    PROOF_CREATED("Proof Created"),
    PROOF_VERSION_CREATED("Proof Version Created"),
    PROOF_SUBMITTED_FOR_REVIEW("Proof Submitted for Review"),
    REVISION_REQUESTED("Revision Requested"),
    REVISION_STARTED("Revision Started"),
    PROOF_RESUBMITTED("Proof Resubmitted"),
    REVISION_RESOLVED("Revision Resolved"),
    PROOF_ARCHIVED("Proof Archived"),

    // Step 04: Approval Workflow Activity Types
    APPROVAL_REQUESTED("Approval Requested"),
    APPROVAL_REVIEW_STARTED("Approval Review Started"),
    APPROVAL_APPROVED("Proof Approved"),
    APPROVAL_REVISION_REQUIRED("Approval Revision Required"),
    APPROVAL_REJECTED("Approval Rejected"),
    APPROVAL_RESUBMITTED("Approval Resubmitted"),
    APPROVAL_FINAL_LOCKED("Final Approval Locked"),

    // Step 05: Integration & Final Validation Activity Types
    PRODUCTION_HANDOFF_AUTHORIZED("Production Handoff Authorized")
}
