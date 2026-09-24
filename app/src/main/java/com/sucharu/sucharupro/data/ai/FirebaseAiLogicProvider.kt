package com.sucharu.sucharupro.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.sucharu.sucharupro.BuildConfig
import com.sucharu.sucharupro.domain.service.ai.SucharuAiProvider

/**
 * Concrete Provider Implementation for Google AI Studio Gemini API & Firebase AI Advisor.
 */
class FirebaseAiLogicProvider(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelName: String = "gemini-1.5-flash"
) : SucharuAiProvider {

    private val naturalSystemInstruction = """
        You are Sucharu AI, the friendly representative of Sucharu Graphics & Printing.

        Speak 100% naturally, warmly, and conversationally in clear Bengali.
        Communicate like a helpful human assistant having a direct chat with a customer.

        STRICT FORMATTING RULES:
        - DO NOT use markdown bold tags (**), headers, bullet points (•), numbered lists, tables, or rigid form templates.
        - Write in clean, fluid conversational Bengali sentences.
        - Keep short questions answered concisely and naturally.
        - Never fabricate business names, phone numbers, addresses, quantities, or prices.
    """.trimIndent()

    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            systemInstruction = content { text(naturalSystemInstruction) }
        )
    }

    override suspend fun generateResponse(prompt: String): Result<String> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be empty"))
        }

        return try {
            val response = generativeModel.generateContent(prompt)
            val rawText = response.text
            if (rawText.isNullOrBlank()) {
                Result.failure(IllegalStateException("Gemini AI returned empty or null response"))
            } else {
                // Strip any residual markdown bullet points or bold tags for 100% natural conversation
                val cleanText = rawText
                    .replace(Regex("\\*\\*"), "")
                    .replace(Regex("^[•\\-*]\\s+", RegexOption.MULTILINE), "")
                Result.success(cleanText)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generatePrintingAdvice(userQuery: String, customerContext: String?): Result<String> {
        val userPrompt = "গ্রাহক প্রশ্ন/কথা: $userQuery (প্রেক্ষাপট: ${customerContext ?: "সাধারণ গ্রাহক"})"
        return generateResponse(userPrompt)
    }

    suspend fun enrichOrderInstruction(userInstruction: String): Result<String> {
        val enrichmentPrompt = """
            Customer provided notes: "$userInstruction"
            Clarify and polish this into a clear, professional order instruction in natural Bengali.
            Do NOT invent any business names, phone numbers, quantities, prices, paper GSM, or sizes that were not provided.
        """.trimIndent()

        return generateResponse(enrichmentPrompt)
    }
}
