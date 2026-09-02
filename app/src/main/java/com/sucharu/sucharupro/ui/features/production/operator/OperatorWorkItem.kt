package com.sucharu.sucharupro.ui.features.production.operator

import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment

/**
 * Composite presentation model linking an operator assignment with its parent Production Job and Stage.
 */
data class OperatorWorkItem(
    val assignment: ProductionStageAssignment,
    val job: ProductionJob,
    val stage: ProductionJobStage
) {
    val isExecutable: Boolean
        get() = stage.status == com.sucharu.sucharupro.domain.model.production.ProductionStageStatus.PENDING ||
                stage.status == com.sucharu.sucharupro.domain.model.production.ProductionStageStatus.IN_PROGRESS
}
