package com.sucharu.sucharupro.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.sucharu.sucharupro.domain.service.ai.SucharuAiProvider

/**
 * Concrete Provider Implementation for Firebase AI Logic / Google Generative AI (Gemini).
 *
 * Implements [SucharuAiProvider] using model "gemini-1.5-flash".
 * Safely handles network errors, App Check 403s, empty responses, and converts exceptions
 * into safe [Result.failure] without crashing the application.
 */
class FirebaseAiLogicProvider(
    private val apiKey: String = "AIzaSyAAJ0seMLnsNB9hU5fRHEeWXUfUMYB0Rs0",
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
                Result.failure(IllegalStateException("Firebase AI returned empty or null response"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generatePrintingAdvice(userQuery: String, customerContext: String?): Result<String> {
        val systemPrompt = """
            You are Sucharu Pro AI Print Advisor, an expert commercial printing & packaging assistant.
            Provide accurate advice on paper GSM, finishing types (Spot UV, Embossing, Lamination),
            offset vs digital press selection, and cost estimation in Bengali or English.
            
            Customer Context: ${customerContext ?: "General Customer"}
            User Query: $userQuery
        """.trimIndent()

        return generateResponse(systemPrompt)
    }
}
