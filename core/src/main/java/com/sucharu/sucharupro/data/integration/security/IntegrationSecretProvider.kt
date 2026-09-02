package com.sucharu.sucharupro.data.integration.security

import java.util.concurrent.ConcurrentHashMap

/**
 * Interface for secure resolution and management of integration provider credentials (INFRA-05 Step 05).
 */
interface IntegrationSecretProvider {
    /**
     * Resolves the plaintext secret associated with a configuration reference.
     * Returns null if no credential is bound.
     */
    fun resolveSecret(configurationReference: String?): String?

    /**
     * Returns a masked, log-safe version of the credential for diagnostics.
     */
    fun maskSecret(secret: String?): String {
        if (secret.isNullOrBlank()) return "[NONE]"
        return if (secret.length <= 8) {
            "***"
        } else {
            "${secret.take(4)}****${secret.takeLast(4)}"
        }
    }
}

/**
 * Standard Environment and In-Memory Secret Provider.
 * Checks environment variables and registered secure memory maps.
 */
class DefaultIntegrationSecretProvider(
    private val staticSecrets: Map<String, String> = emptyMap()
) : IntegrationSecretProvider {

    private val runtimeSecretStore = ConcurrentHashMap<String, String>(staticSecrets)

    override fun resolveSecret(configurationReference: String?): String? {
        if (configurationReference.isNullOrBlank()) return null

        // 1. Check runtime store
        runtimeSecretStore[configurationReference]?.let { return it }

        // 2. Check environment variables (normalizing ref to UPPER_SNAKE_CASE)
        val envKey = configurationReference.replace(".", "_").replace("-", "_").uppercase()
        val envVal = System.getenv(envKey) ?: System.getenv(configurationReference)
        if (!envVal.isNullOrBlank()) return envVal

        return null
    }

    fun registerSecret(reference: String, secret: String) {
        require(reference.isNotBlank()) { "Secret reference cannot be blank" }
        require(secret.isNotBlank()) { "Secret value cannot be blank" }
        runtimeSecretStore[reference] = secret
    }

    fun removeSecret(reference: String) {
        runtimeSecretStore.remove(reference)
    }
}
