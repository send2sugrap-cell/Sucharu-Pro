package com.sucharu.sucharupro.domain.model.qc

/**
 * Categorical type of Quality Control inspection in Sucharu Pro ERP (Module 06).
 */
enum class QcType(val defaultLabel: String) {
    /**
     * Pre-production QC check (materials, paper stock, ink shades, plate accuracy, proof comparison).
     */
    PRE_PRODUCTION("Pre-Production QC"),

    /**
     * Final QC check (print quality, cutting dimensions, binding, lamination, packaging count).
     */
    FINAL("Final QC")
}
