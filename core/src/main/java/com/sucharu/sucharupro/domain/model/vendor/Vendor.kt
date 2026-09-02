package com.sucharu.sucharupro.domain.model.vendor

/**
 * Master aggregate root representing an external Vendor in Sucharu Pro ERP (Module 12 Step 01).
 *
 * Serves as the authoritative source of truth for external service providers, material suppliers,
 * and production subcontractors.
 *
 * Architectural Boundary:
 *  - Vendor stores canonical identity, master attributes, classification foundation, and lifecycle state.
 *  - Vendor does NOT directly own transactional job executions, ledgers, or payment balances.
 *  - Future Module 12 steps (Contacts, Rate Cards, Documents, Performance) and downstream modules
 *    (Module 13 Jobs, Module 15 Payables) attach to this canonical record via [vendorId].
 */
data class Vendor(
    /** Unique internal immutable identifier (e.g. UUID). */
    val vendorId: String,

    /** Authoritative tenant/project scope. */
    val projectId: String,

    /** Human-readable unique vendor code within the tenant (e.g. "VND-000101"). */
    val vendorCode: String,

    /** Primary trading or business name of the vendor (e.g. "Creative CTP & Plates"). */
    val vendorName: String,

    /** Optional official legally registered company name. */
    val legalName: String? = null,

    /** High-level vendor classification. */
    val vendorType: VendorType = VendorType.SERVICE_PROVIDER,

    /** Functional vendor category foundation. */
    val vendorCategory: VendorCategory = VendorCategory.OTHER,

    /** Current lifecycle status. */
    val status: VendorStatus = VendorStatus.ACTIVE,

    /** Primary contact person name. */
    val primaryContactName: String? = null,

    /** Primary contact phone number. */
    val primaryPhone: String? = null,

    /** Primary contact email address. */
    val primaryEmail: String? = null,

    /** Internal business remarks or notes. */
    val notes: String? = null,

    /** Identity of actor who created this record. */
    val createdBy: String = "system",

    /** Identity of actor who last modified this record. */
    val updatedBy: String? = null,

    /** Epoch millisecond timestamp of creation. */
    val createdAt: Long = System.currentTimeMillis(),

    /** Epoch millisecond timestamp of last update. */
    val updatedAt: Long = System.currentTimeMillis(),

    /** Optimistic concurrency version counter. */
    val version: Long = 1L
) {
    init {
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(vendorCode.isNotBlank()) { "Vendor code cannot be blank." }
        require(vendorName.isNotBlank()) { "Vendor name cannot be blank." }
    }
}
