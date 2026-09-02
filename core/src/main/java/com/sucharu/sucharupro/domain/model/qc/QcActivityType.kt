package com.sucharu.sucharupro.domain.model.qc

/**
 * Categorical types of discrete audit/activity events in the Quality Control domain.
 */
enum class QcActivityType(val defaultLabel: String) {
    QC_CREATED("QC Record Created"),
    QC_ASSIGNED("QC Inspector Assigned"),
    QC_REASSIGNED("QC Inspector Reassigned"),
    QC_UNASSIGNED("QC Inspector Unassigned"),
    QC_INSPECTION_STARTED("QC Inspection Started"),
    QC_INSPECTION_COMPLETED("QC Inspection Completed"),
    QC_PASSED("QC Passed"),
    QC_FAILED("QC Failed"),
    QC_CANCELLED("QC Cancelled"),

    // Module 06 Step 02: Pre-Production QC events
    PRE_PRODUCTION_QC_ITEMS_INITIALIZED("Pre-Production QC Items Initialized"),
    PRE_PRODUCTION_QC_ITEM_UPDATED("Pre-Production QC Item Updated"),
    PRE_PRODUCTION_QC_SUBMITTED("Pre-Production QC Submitted"),

    // Module 06 Step 03: Generic QC Checklist & Inspection Engine events
    QC_CHECKLIST_TEMPLATE_CREATED("QC Checklist Template Created"),
    QC_CHECKLIST_TEMPLATE_VERSION_CREATED("QC Checklist Template Version Created"),
    QC_CHECKLIST_ACTIVATED("QC Checklist Activated"),
    QC_CHECKLIST_DEACTIVATED("QC Checklist Deactivated"),
    QC_INSPECTION_CHECKLIST_CREATED("QC Inspection Checklist Created"),
    QC_INSPECTION_CHECKLIST_STARTED("QC Inspection Checklist Started"),
    QC_INSPECTION_RESPONSE_UPDATED("QC Inspection Response Updated"),
    QC_INSPECTION_CHECKLIST_COMPLETED("QC Inspection Checklist Completed"),
    QC_INSPECTION_PASSED("QC Inspection Passed"),
    QC_INSPECTION_FAILED("QC Inspection Failed")
}
