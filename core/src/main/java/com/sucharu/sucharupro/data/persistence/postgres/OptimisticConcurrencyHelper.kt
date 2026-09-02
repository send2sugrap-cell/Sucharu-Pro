package com.sucharu.sucharupro.data.persistence.postgres

/**
 * Optimistic Concurrency Control Helper (INFRA-01 Step 03).
 *
 * Enforces CAS (Compare-And-Swap) version checking during entity updates.
 */
object OptimisticConcurrencyHelper {

    suspend fun executeOptimisticUpdate(
        sqlExecutor: SqlExecutor,
        updateSql: String,
        paramsWithVersionCheck: List<Any?>,
        entityType: String,
        entityId: String,
        expectedVersion: Long
    ) {
        val affected = sqlExecutor.executeUpdate(updateSql, paramsWithVersionCheck)
        if (affected == 0) {
            throw OptimisticLockException(
                entityType = entityType,
                entityId = entityId,
                expectedVersion = expectedVersion
            )
        }
    }
}
