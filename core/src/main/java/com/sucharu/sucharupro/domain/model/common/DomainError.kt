package com.sucharu.sucharupro.domain.model.common

/**
 * Typed domain error hierarchy for structured error propagation.
 * Used in UI state and repository boundaries to communicate failure reasons
 * without exposing raw exceptions to the presentation layer.
 */
sealed class DomainError(open val message: String) {

    /** Access denied due to insufficient role/permissions. */
    data class UnauthorizedError(override val message: String) : DomainError(message)

    /** A required resource was not found. */
    data class NotFoundError(override val message: String) : DomainError(message)

    /** Input validation failed. */
    data class ValidationError(override val message: String) : DomainError(message)

    /** Concurrent/duplicate operation detected. */
    data class ConcurrencyError(override val message: String) : DomainError(message)

    /**
     * Snapshot or record integrity check failed.
     * [storedHash] and [recomputedHash] are included for auditability.
     */
    data class IntegrityError(
        override val message: String,
        val storedHash: String? = null,
        val recomputedHash: String? = null
    ) : DomainError(message)

    /** Export pipeline failed. */
    data class ExportError(override val message: String, val cause: Throwable? = null) : DomainError(message)

    /** Project scope mismatch — cross-project access attempt. */
    data class ProjectMismatchError(override val message: String) : DomainError(message)

    /** Catch-all for unexpected failures. */
    data class UnexpectedError(override val message: String, val cause: Throwable? = null) : DomainError(message)
}
