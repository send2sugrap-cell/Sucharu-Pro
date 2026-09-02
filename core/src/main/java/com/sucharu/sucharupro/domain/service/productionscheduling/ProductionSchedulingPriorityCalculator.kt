package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder
import com.sucharu.sucharupro.domain.service.productionscheduling.ProductionSchedulingMathUtils.p4
import java.math.BigDecimal
import java.math.RoundingMode

object ProductionSchedulingPriorityCalculator {

    /**
     * Computes a deterministic priority score in range [0.0000, 150.0000].
     */
    fun calculateSlotPriority(
        job: ProductionJobExecution,
        workOrder: ProductionWorkOrder,
        targetStartTime: Long,
        dueDateTime: Long?
    ): BigDecimal {
        var score = when (job.priority) {
            OrderPriority.URGENT -> BigDecimal("100.0000")
            OrderPriority.HIGH -> BigDecimal("75.0000")
            OrderPriority.NORMAL -> BigDecimal("50.0000")
        }

        // Proximity to due date adjustment
        if (dueDateTime != null && dueDateTime > 0L) {
            val millisRemaining = dueDateTime - targetStartTime
            val hoursRemaining = BigDecimal.valueOf(millisRemaining).divide(BigDecimal("3600000.0000"), 4, RoundingMode.HALF_UP)

            if (hoursRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                score = score.add(BigDecimal("30.0000")) // Overdue or zero buffer
            } else if (hoursRemaining.compareTo(BigDecimal("24.0000")) <= 0) {
                score = score.add(BigDecimal("20.0000")) // Due within 24 hours
            } else if (hoursRemaining.compareTo(BigDecimal("48.0000")) <= 0) {
                score = score.add(BigDecimal("10.0000")) // Due within 48 hours
            }
        }

        // Sequence number weighting: earlier stages get slight bonus for workflow priming
        val seqDeduction = BigDecimal.valueOf(workOrder.sequenceNumber.toLong()).multiply(BigDecimal("0.5000"))
        score = score.subtract(seqDeduction)

        return score.max(BigDecimal.ZERO).p4()
    }
}
