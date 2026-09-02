package com.sucharu.sucharupro.data.persistence.postgres

import java.sql.SQLException

/**
 * Failure classification for PostgreSQL operations (INFRA-02 Step 03).
 */
enum class PostgresFailureType {
    /** Transient failure (e.g. deadlock, serialization conflict, connection drop) eligible for idempotent retry. */
    TRANSIENT_RETRYABLE,

    /** Constraint or business invariant failure (e.g. unique, FK, check, journal balance) - NEVER retry automatically. */
    NON_RETRYABLE_CONSTRAINT,

    /** Client / Syntax / Parameter error - NEVER retry. */
    NON_RETRYABLE_CLIENT,

    /** Unknown server exception. */
    UNKNOWN
}

/**
 * Policy defining safe retry classification for PostgreSQL errors (INFRA-02 Step 03).
 *
 * Guarantees that business mutations (such as orders, payments, journal postings, returns)
 * are NEVER automatically retried if they fail due to domain/constraint violations.
 */
object PostgresRetryPolicy {

    /**
     * Determines whether an error is transient and safe for idempotent retry.
     */
    fun classifyFailure(throwable: Throwable): PostgresFailureType {
        val sqlEx = findSqlException(throwable) ?: return PostgresFailureType.UNKNOWN
        val sqlState = sqlEx.sqlState ?: return PostgresFailureType.UNKNOWN

        return when {
            // Class 40: Transaction Rollback (40001 serialization failure, 40P01 deadlock detected)
            sqlState.startsWith("40") -> PostgresFailureType.TRANSIENT_RETRYABLE
            
            // Class 08: Connection Exception (08000, 08003, 08006 connection failure)
            sqlState.startsWith("08") -> PostgresFailureType.TRANSIENT_RETRYABLE
            
            // Class 57P01: admin shutdown / connection termination
            sqlState == "57P01" -> PostgresFailureType.TRANSIENT_RETRYABLE

            // Class 23: Integrity Constraint Violations (23505 unique, 23503 FK, 23514 check, 23502 not null)
            sqlState.startsWith("23") -> PostgresFailureType.NON_RETRYABLE_CONSTRAINT

            // Class 42: Syntax / Access Rule Violation
            sqlState.startsWith("42") -> PostgresFailureType.NON_RETRYABLE_CLIENT

            else -> PostgresFailureType.UNKNOWN
        }
    }

    /**
     * Returns true ONLY if the failure is transient and safe for retry.
     */
    fun isRetryable(throwable: Throwable): Boolean {
        return classifyFailure(throwable) == PostgresFailureType.TRANSIENT_RETRYABLE
    }

    private fun findSqlException(throwable: Throwable?): SQLException? {
        var current = throwable
        while (current != null) {
            if (current is SQLException) return current
            current = current.cause
        }
        return null
    }
}
