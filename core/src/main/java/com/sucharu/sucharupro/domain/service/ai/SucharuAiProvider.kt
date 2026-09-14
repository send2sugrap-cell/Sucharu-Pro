package com.sucharu.sucharupro.domain.service.ai

/**
 * Domain Abstraction Contract for Sucharu Pro AI Advisor & Intelligence Services.
 *
 * Encapsulates generative AI operations without coupling domain layers or UI components
 * to external SDK types (e.g. Firebase AI, Vertex AI, Gemini).
 */
interface SucharuAiProvider {

    /**
     * Generates a text response for a given [prompt].
     * Returns [Result.success] with model text, or [Result.failure] with domain exception.
     */
    suspend fun generateResponse(prompt: String): Result<String>

    /**
     * Generates specialized printing advice, GSM paper recommendations, and finishing cost guidance.
     */
    suspend fun generatePrintingAdvice(userQuery: String, customerContext: String? = null): Result<String>
}
