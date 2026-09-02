package com.sucharu.sucharupro.backend.health

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real-time health, readiness, and liveness tracker for the standalone backend runtime.
 */
class ServerHealthTracker {

    private val applicationStarted = AtomicBoolean(false)
    private val databaseReady = AtomicBoolean(false)
    private val migrationsValid = AtomicBoolean(false)
    private val coreDependenciesReady = AtomicBoolean(false)
    private val workersReady = AtomicBoolean(false)

    fun markApplicationStarted(value: Boolean = true) = applicationStarted.set(value)
    fun markDatabaseReady(value: Boolean = true) = databaseReady.set(value)
    fun markMigrationsValid(value: Boolean = true) = migrationsValid.set(value)
    fun markCoreDependenciesReady(value: Boolean = true) = coreDependenciesReady.set(value)
    fun markWorkersReady(value: Boolean = true) = workersReady.set(value)

    fun isLive(): Boolean {
        return applicationStarted.get()
    }

    fun isReady(): Boolean {
        return applicationStarted.get() &&
               databaseReady.get() &&
               migrationsValid.get() &&
               coreDependenciesReady.get()
    }

    fun getHealthReport(): Map<String, Any> {
        val ready = isReady()
        val live = isLive()

        return mapOf(
            "status" to if (ready) "UP" else if (live) "STARTING" else "DOWN",
            "live" to live,
            "ready" to ready,
            "components" to mapOf(
                "application" to mapOf("status" to if (applicationStarted.get()) "UP" else "DOWN"),
                "database" to mapOf("status" to if (databaseReady.get()) "UP" else "DOWN"),
                "migrations" to mapOf("status" to if (migrationsValid.get()) "UP" else "DOWN"),
                "coreDependencies" to mapOf("status" to if (coreDependenciesReady.get()) "UP" else "DOWN"),
                "workers" to mapOf("status" to if (workersReady.get()) "UP" else "DOWN")
            ),
            "timestamp" to System.currentTimeMillis()
        )
    }
}
