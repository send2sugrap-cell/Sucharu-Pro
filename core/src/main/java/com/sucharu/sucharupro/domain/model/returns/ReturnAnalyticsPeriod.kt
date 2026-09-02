package com.sucharu.sucharupro.domain.model.returns

/**
 * Period filter for Return Analytics aggregation (Module 11 Step 06).
 */
enum class ReturnAnalyticsPeriod(val displayName: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_QUARTER("This Quarter"),
    ALL_TIME("All Time");

    /**
     * Calculates the inclusive start timestamp (in milliseconds) for this period relative to [nowMillis].
     */
    fun calculateStartTimestamp(nowMillis: Long): Long {
        val oneDayMillis = 86_400_000L
        return when (this) {
            TODAY -> nowMillis - oneDayMillis
            THIS_WEEK -> nowMillis - (7L * oneDayMillis)
            THIS_MONTH -> nowMillis - (30L * oneDayMillis)
            THIS_QUARTER -> nowMillis - (90L * oneDayMillis)
            ALL_TIME -> 0L
        }
    }
}
