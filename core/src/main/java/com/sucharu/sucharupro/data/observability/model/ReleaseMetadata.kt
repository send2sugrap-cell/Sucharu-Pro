package com.sucharu.sucharupro.data.observability.model

/**
 * Authoritative release identity for the Sucharu Pro runtime (INFRA-05 Step 07).
 * Exposes safe build and version metadata while preventing any secret leakage.
 */
data class ReleaseMetadata(
    val appName: String = "sucharu-backend",
    val appVersion: String = "1.0.0",
    val buildVersion: String = "1.0.0-PROD",
    val gitRevision: String = "HEAD",
    val environment: String = "production",
    val buildTimestamp: String = "2026-08-25T00:00:00Z"
) {
    fun toSafeMap(): Map<String, String> = mapOf(
        "appName" to appName,
        "appVersion" to appVersion,
        "buildVersion" to buildVersion,
        "gitRevision" to gitRevision,
        "environment" to environment,
        "buildTimestamp" to buildTimestamp
    )
}
