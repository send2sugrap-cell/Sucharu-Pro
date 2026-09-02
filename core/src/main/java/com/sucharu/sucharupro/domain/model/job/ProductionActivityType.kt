package com.sucharu.sucharupro.domain.model.job

/**
 * Discrete types of production activity and stage execution events in Sucharu Pro ERP.
 */
enum class ProductionActivityType(val defaultLabel: String) {
    /** Operator assigned to a production stage. */
    STAGE_ASSIGNED("Stage Assigned"),

    /** Operator reassigned on a production stage. */
    STAGE_REASSIGNED("Stage Reassigned"),

    /** Operator unassigned from a production stage. */
    STAGE_UNASSIGNED("Stage Unassigned"),

    /** Stage execution started. */
    STAGE_STARTED("Stage Started"),

    /** Stage execution completed. */
    STAGE_COMPLETED("Stage Completed"),

    /** Stage was skipped according to workflow rules. */
    STAGE_SKIPPED("Stage Skipped"),

    /** Operational execution note was added to a stage. */
    STAGE_EXECUTION_NOTE("Execution Note Added"),

    /** Production Job was put on hold. */
    JOB_HELD("Job Held"),

    /** Production Job was resumed from on-hold state. */
    JOB_RESUMED("Job Resumed"),

    /** Production Job was cancelled. */
    JOB_CANCELLED("Job Cancelled"),

    /** Production Job marked ready for delivery. */
    JOB_READY("Job Ready"),

    /** Production Job marked delivered. */
    JOB_DELIVERED("Job Delivered"),

    /** Output quantity recorded against a stage execution. */
    STAGE_OUTPUT_RECORDED("Stage Output Recorded")
}
