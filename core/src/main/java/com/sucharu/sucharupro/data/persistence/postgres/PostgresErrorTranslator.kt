package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.sql.SQLException

/**
 * Maps PostgreSQL and JDBC SQLExceptions into structured DomainResult.Error outcomes (INFRA-01 Step 03).
 *
 * Prevents raw SQL, table internals, and credentials from leaking to application/domain layers.
 */
object PostgresErrorTranslator {

    fun <T> translate(throwable: Throwable, operationDescription: String): DomainResult<T> {
        if (throwable is OptimisticLockException) {
            return DomainResult.Error(
                exception = throwable,
                message = "Concurrent update detected: The record has been modified by another operation."
            )
        }

        if (throwable is SQLException) {
            val sqlState = throwable.sqlState ?: ""
            val cleanMessage = when {
                sqlState == "23505" -> "A record with this identifier or unique attribute already exists."
                sqlState == "23503" -> "Foreign key relationship error: Referenced record not found or cannot be deleted."
                sqlState == "23514" -> "Data validation failed: Check constraint violated."
                sqlState == "P0001" -> {
                    // PL/pgSQL RAISE EXCEPTION (e.g., Journal imbalance)
                    val rawMsg = throwable.message ?: "Business rule constraint violated."
                    if (rawMsg.contains("Journal imbalance", ignoreCase = true)) {
                        "Financial journal is out of balance: Total debits must equal total credits."
                    } else {
                        "Database constraint rule rejected operation: $rawMsg"
                    }
                }
                sqlState == "40001" || sqlState == "40P01" -> "Database concurrency conflict / deadlock. Please retry."
                sqlState.startsWith("08") || sqlState == "57P01" -> "Database connection is currently unavailable."
                else -> "Database operation failed: $operationDescription"
            }
            return DomainResult.Error(exception = throwable, message = cleanMessage)
        }

        return DomainResult.Error(
            exception = throwable,
            message = throwable.message ?: "An unexpected persistence error occurred during $operationDescription."
        )
    }
}

/**
 * Exception raised when an optimistic concurrency version check fails.
 */
class OptimisticLockException(
    val entityType: String,
    val entityId: String,
    val expectedVersion: Long
) : RuntimeException("Optimistic lock conflict on $entityType with ID '$entityId' (expected version: $expectedVersion).")
