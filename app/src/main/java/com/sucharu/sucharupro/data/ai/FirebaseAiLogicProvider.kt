package com.sucharu.sucharupro.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.sucharu.sucharupro.BuildConfig
import com.sucharu.sucharupro.domain.service.ai.SucharuAiProvider

/**
 * Concrete Provider Implementation for Google AI Studio Gemini API & Firebase AI Advisor.
 *
 * Implements [SucharuAiProvider] using model "gemini-1.5-flash" with secure key binding [BuildConfig.GEMINI_API_KEY].
 * Bypasses Firebase Blaze plan requirement while providing high-quality Bengali printing advice.
 *
 * SECURITY GUARANTEE:
 * Zero hardcoded raw keys in source code. [BuildConfig.GEMINI_API_KEY] is safely read from local.properties / environment.
 */
class FirebaseAiLogicProvider(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelName: String = "gemini-1.5-flash"
) : SucharuAiProvider {

    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )
    }

    override suspend fun generateResponse(prompt: String): Result<String> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be empty"))
        }

        return try {
            val response = generativeModel.generateContent(prompt)
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Gemini AI returned empty or null response"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generatePrintingAdvice(userQuery: String, customerContext: String?): Result<String> {
        val naturalSystemPrompt = """
            You are Sucharu AI, the friendly AI assistant of Sucharu Graphics & Printing.

            Speak naturally and conversationally in clear Bengali.
            Your communication should feel like a helpful person having a real conversation with the customer.
            Do not sound like a form, database, ERP report, technical manual, or automated bot.
            Do not unnecessarily use headings, numbered lists, bullet points, labels, tables, JSON, Markdown blocks, or rigid templates unless explicitly requested.

            Keep responses concise when a short answer is enough.
            If information is missing, ask one useful follow-up question naturally.
            Never invent customer information, business names, phone numbers, addresses, prices, or specifications that were not provided.

            কাস্টমার প্রেক্ষাপট: ${customerContext ?: "সাধারণ গ্রাহক"}
            গ্রাহকের কথা: $userQuery
        """.trimIndent()

        return generateResponse(naturalSystemPrompt)
    }
}
