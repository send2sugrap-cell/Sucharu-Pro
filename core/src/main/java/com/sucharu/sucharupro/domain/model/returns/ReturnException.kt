package com.sucharu.sucharupro.domain.model.returns

/**
 * Immutable root aggregate representing an automated governance anomaly or policy exception (Module 11 Step 06).
 */
data class ReturnException(
    val exceptionId: String,
    val projectId: String,
    val returnId: String? = null,
    val exceptionType: ReturnExceptionType,
    val severity: String = exceptionType.defaultSeverity,
    val status: ReturnExceptionStatus = ReturnExceptionStatus.OPEN,
    val detectedAt: Long = System.currentTimeMillis(),
    val thresholdValue: Double = 0.0,
    val actualValue: Double = 0.0,
    val description: String,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolutionNotes: String? = null,
    val version: Long = 1L,
    val idempotencyKey: String
) {
    init {
        require(exceptionId.isNotBlank()) { "Exception ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(severity.isNotBlank()) { "Severity cannot be blank." }
        require(detectedAt > 0) { "Detected At timestamp must be positive." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(version > 0) { "Version must be strictly positive (was $version)." }
        require(idempotencyKey.isNotBlank()) { "Idempotency key cannot be blank." }
        if (status == ReturnExceptionStatus.ACKNOWLEDGED) {
            require(!acknowledgedBy.isNullRespBlank()) { "Acknowledged By is required when status is ACKNOWLEDGED." }
        }
        if (status == ReturnExceptionStatus.RESOLVED || status == ReturnExceptionStatus.DISMISSED) {
            require(!resolvedBy.isNullRespBlank()) { "Resolved By is required when status is terminal (${status.name})." }
        }
    }
}

private fun String?.isNullRespBlank(): Boolean = this == null || this.trim().isEmpty()
