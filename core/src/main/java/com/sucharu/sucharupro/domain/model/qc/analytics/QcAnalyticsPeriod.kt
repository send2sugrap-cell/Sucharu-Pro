package com.sucharu.sucharupro.domain.model.qc.analytics

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Period scope for QC analytics queries (Module 06 Step 09).
 */
enum class QcPeriodType(val defaultLabel: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom Period")
}

/**
 * Encapsulates the time boundaries for QC analytics queries.
 */
data class QcAnalyticsPeriod(
    val type: QcPeriodType,
    val startTimestamp: String,
    val endTimestamp: String
) {
    companion object {
        fun today(): QcAnalyticsPeriod {
            val now = Instant.now()
            val startOfDay = now.truncatedTo(ChronoUnit.DAYS)
            return QcAnalyticsPeriod(
                type = QcPeriodType.TODAY,
                startTimestamp = startOfDay.toString(),
                endTimestamp = now.toString()
            )
        }

        fun thisWeek(): QcAnalyticsPeriod {
            val now = Instant.now()
            val startOfWeek = now.minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)
            return QcAnalyticsPeriod(
                type = QcPeriodType.THIS_WEEK,
                startTimestamp = startOfWeek.toString(),
                endTimestamp = now.toString()
            )
        }

        fun thisMonth(): QcAnalyticsPeriod {
            val now = Instant.now()
            val startOfMonth = now.minus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)
            return QcAnalyticsPeriod(
                type = QcPeriodType.THIS_MONTH,
                startTimestamp = startOfMonth.toString(),
                endTimestamp = now.toString()
            )
        }

        fun custom(start: String, end: String): QcAnalyticsPeriod {
            return QcAnalyticsPeriod(
                type = QcPeriodType.CUSTOM,
                startTimestamp = start,
                endTimestamp = end
            )
        }
    }
}
