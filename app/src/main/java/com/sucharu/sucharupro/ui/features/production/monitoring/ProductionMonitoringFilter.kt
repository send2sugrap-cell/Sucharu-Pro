package com.sucharu.sucharupro.ui.features.production.monitoring

/**
 * Filter options for the Live Production Monitoring Dashboard.
 */
enum class ProductionMonitoringFilter(val label: String) {
    ALL("All"),
    IN_PROGRESS("In Production"),
    READY_FOR_PRODUCTION("Ready to Start"),
    ON_HOLD("On Hold"),
    READY("Ready"),
    URGENT("Urgent"),
    UNASSIGNED("Unassigned")
}
