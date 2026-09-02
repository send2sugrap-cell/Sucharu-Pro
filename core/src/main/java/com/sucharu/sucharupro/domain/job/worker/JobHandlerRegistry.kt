package com.sucharu.sucharupro.domain.job.worker

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry for background job handlers (INFRA-04 Step 04).
 */
class JobHandlerRegistry {

    private val handlers = ConcurrentHashMap<String, JobHandler>()

    /**
     * Registers a job handler.
     */
    fun registerHandler(handler: JobHandler) {
        require(handler.supportedJobType.isNotBlank()) { "supportedJobType cannot be blank" }
        require(handler.supportedVersion.isNotBlank()) { "supportedVersion cannot be blank" }

        val key = "${handler.supportedJobType}:${handler.supportedVersion}"
        handlers[key] = handler
    }

    /**
     * Unregisters a job handler by type and version.
     */
    fun unregisterHandler(jobType: String, version: String = "v1") {
        handlers.remove("$jobType:$version")
    }

    /**
     * Retrieves the matching handler for a job type and version.
     */
    fun getHandler(jobType: String, version: String = "v1"): JobHandler? {
        return handlers["$jobType:$version"]
    }

    /**
     * Returns true if a handler is registered for the specified job type.
     */
    fun hasHandler(jobType: String, version: String = "v1"): Boolean {
        return handlers.containsKey("$jobType:$version")
    }

    /**
     * Clears all registrations (for testing).
     */
    fun clear() {
        handlers.clear()
    }
}
