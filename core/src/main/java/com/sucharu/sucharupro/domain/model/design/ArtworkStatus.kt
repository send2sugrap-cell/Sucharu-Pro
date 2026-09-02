package com.sucharu.sucharupro.domain.model.design

/**
 * Lifecycle status of an Artwork or Artwork Version in Sucharu Pro ERP.
 */
enum class ArtworkStatus(val defaultLabel: String) {
    /** Initial draft state. */
    DRAFT("Draft"),

    /** Active current artwork or usable historical version. */
    ACTIVE("Active"),

    /** Superseded, archived, or soft-deleted historical version. */
    ARCHIVED("Archived");

    val isArchived: Boolean get() = this == ARCHIVED
    val isActive: Boolean get() = this == ACTIVE
}
