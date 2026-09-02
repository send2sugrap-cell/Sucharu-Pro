package com.sucharu.sucharupro.data.event.integration.n8n

/**
 * Server-authoritative configuration for outgoing n8n workflow automations (INFRA-04 Step 03).
 */
data class N8nConfig(
    val webhookBaseUrl: String = "https://automation.sucharu.internal/webhook",
    val signingSecret: String = "sucharu_pro_n8n_hmac_secret_2026",
    val connectTimeoutMs: Long = 5000L,
    val readTimeoutMs: Long = 10000L,
    val maxRetries: Int = 3,
    val isEnabled: Boolean = true
)
