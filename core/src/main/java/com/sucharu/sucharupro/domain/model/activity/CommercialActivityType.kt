package com.sucharu.sucharupro.domain.model.activity

/**
 * Discrete types of audit events that can be recorded on a commercial entity
 * (Inquiry, Quotation, or Order) in Sucharu Pro.
 *
 * Each value corresponds to a distinct, observable state-change or lifecycle action.
 * These values are IMMUTABLE once recorded — no retroactive editing is permitted.
 */
enum class CommercialActivityType(val defaultLabel: String) {

    /** Entity was first created in the system. */
    CREATED("Created"),

    /** Entity was viewed (recorded once on explicit screen entry). */
    VIEWED("Viewed"),

    /** Commercial lifecycle status changed (e.g., DRAFT → SENT). */
    STATUS_CHANGED("Status Changed"),

    /** A new quotation revision was created. */
    REVISED("Revised"),

    /** A quotation revision was formally approved by an authorised actor. */
    APPROVED("Approved"),

    /** A quotation was rejected by the customer or internal authority. */
    REJECTED("Rejected"),

    /** A quotation or inquiry was cancelled before completion. */
    CANCELLED("Cancelled"),

    /** An approved quotation was converted into a confirmed customer order. */
    ORDER_CONVERTED("Order Converted"),

    /** Commercial priority of an order was updated. */
    PRIORITY_CHANGED("Priority Changed"),

    /** Order was marked as commercially ready for job handoff to production. */
    HANDOFF_READY("Handoff Ready"),

    /** Operational remarks or notes were updated on the entity. */
    NOTES_UPDATED("Notes Updated")
}
