package com.sucharu.sucharupro.domain.model.common

/**
 * Minimal domain result wrapper for Sucharu Pro.
 *
 * Represents the outcome of a domain-level data operation with three states:
 * Success, Error, and Loading.
 *
 * Design principles:
 *  - Lives in the DOMAIN layer — no Android or Compose dependencies.
 *  - Complements (does NOT replace) [DashboardUiState] which is UI-layer only.
 *  - Complements (does NOT replace) Kotlin's [Result] — [Result] is used for
 *    simple suspend refresh calls (e.g. refreshDashboardSummary).
 *  - This class is used at the Repository ↔ DataSource boundary to propagate
 *    structured errors from data sources up to repository implementations.
 *
 * Usage:
 * ```kotlin
 * // In DataSource:
 * suspend fun fetchKpis(): DomainResult<DashboardKpis>
 *
 * // In RepositoryImpl:
 * when (val result = dataSource.fetchKpis()) {
 *     is DomainResult.Success -> emit(result.data)
 *     is DomainResult.Error   -> { /* map to UI error state */ }
 *     is DomainResult.Loading -> { /* handle loading */ }
 * }
 * ```
 *
 * Future: use-case layer will map [DomainResult] to [DashboardUiState] for the
 * ViewModel, keeping the mapping logic outside of ViewModel.
 */
sealed class DomainResult<out T> {

    /**
     * Successful data retrieval.
     * @param data The successfully retrieved domain data.
     */
    data class Success<out T>(val data: T) : DomainResult<T>()

    /**
     * Operation failed with a structured error.
     * @param exception The underlying exception, if available.
     * @param message A human-readable error description (safe to display in logs).
     */
    data class Error(
        val exception: Throwable? = null,
        val message: String = exception?.message ?: "An unexpected error occurred."
    ) : DomainResult<Nothing>()

    /**
     * Operation is in progress. Used when a data source does not yet have data
     * available and is actively fetching it.
     */
    data object Loading : DomainResult<Nothing>()

    // =========================================================================
    // Convenience helpers
    // =========================================================================

    /** Returns true if this is a [Success] result. */
    val isSuccess: Boolean get() = this is Success

    /** Returns true if this is an [Error] result. */
    val isError: Boolean get() = this is Error

    /** Returns true if this is a [Loading] result. */
    val isLoading: Boolean get() = this is Loading

    /**
     * Returns the data if [Success], or null otherwise.
     */
    fun getOrNull(): T? = if (this is Success) data else null

    /**
     * Returns the data if [Success], or the given [default] value.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = if (this is Success) data else default

    /**
     * Transforms a [Success] result's data using [transform].
     * [Error] and [Loading] pass through unchanged.
     */
    fun <R> map(transform: (T) -> R): DomainResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error   -> this
        is Loading -> this
    }

    companion object {
        /**
         * Wraps a suspending [block] in a [DomainResult].
         * Returns [Success] if the block succeeds, [Error] if it throws.
         *
         * Example:
         * ```kotlin
         * val result = DomainResult.of { dataSource.fetchKpis() }
         * ```
         */
        suspend fun <T> of(block: suspend () -> T): DomainResult<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(exception = e, message = e.message ?: "Operation failed.")
        }
    }
}
