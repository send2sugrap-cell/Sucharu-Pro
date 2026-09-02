package com.sucharu.sucharupro.data.job.fairness

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tenant-aware concurrency throttler preventing worker pool starvation (INFRA-04 Step 04).
 */
class TenantFairnessThrottler(
    val defaultMaxConcurrentPerTenant: Int = 5
) {
    private val activePerTenant = ConcurrentHashMap<String, AtomicInteger>()

    /**
     * Attempts to acquire execution permit for a tenant.
     * @return true if permit acquired, false if tenant concurrency exceeded.
     */
    fun tryAcquire(projectId: String, maxConcurrent: Int = defaultMaxConcurrentPerTenant): Boolean {
        val counter = activePerTenant.computeIfAbsent(projectId) { AtomicInteger(0) }
        while (true) {
            val current = counter.get()
            if (current >= maxConcurrent) {
                return false
            }
            if (counter.compareAndSet(current, current + 1)) {
                return true
            }
        }
    }

    /**
     * Releases an execution permit for a tenant.
     */
    fun release(projectId: String) {
        val counter = activePerTenant[projectId]
        if (counter != null) {
            counter.decrementAndGet()
        }
    }

    /**
     * Gets current in-flight job count for a tenant.
     */
    fun getActiveCount(projectId: String): Int {
        return activePerTenant[projectId]?.get() ?: 0
    }
}
