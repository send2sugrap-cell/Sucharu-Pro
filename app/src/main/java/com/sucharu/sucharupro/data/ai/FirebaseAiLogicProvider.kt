package com.sucharu.sucharupro.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.sucharu.sucharupro.BuildConfig
import com.sucharu.sucharupro.domain.service.ai.SucharuAiProvider

/**
 * Concrete Provider Implementation for Google AI Studio Gemini API & Firebase AI Advisor.
 */
class FirebaseAiLogicProvider(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelName: String = "gemini-1.5-flash"
) : SucharuAiProvider {

    private val consultantSystemInstruction = """
        You are Sucharu AI, the smart AI printing consultant and creative copywriting expert for Sucharu Graphics & Printing.

        CORE ROLE & BEHAVIOR:
        1. COPYWRITING & CREATIVE IDEAS: When users ask for content, slogan, or copywriting ideas (e.g., text for Hajj agency leaflets, business cards, restaurant menus, promotional flyers), generate high-quality advertising copy, catchy headlines, and engaging bullet points in natural Bengali instead of jumping straight to pricing or generic offers.
        2. PRINTING ADVICE: When users ask for printing advice or paper selection, suggest suitable GSM (e.g., 300 GSM art card for business cards, 150 GSM art paper for flyers), finishing (matte, gloss, spot UV, embossing), and printing type (digital for short run vs offset for bulk) tailored to their specific business category.
        3. NATURAL CONVERSATIONAL TONE: Maintain a professional, warm, helpful, and natural conversational Bengali tone. Do NOT repeat previous canned messages or rigid database templates.
        4. ACCURACY: Never fabricate customer identity, phone numbers, addresses, or false prices that were not specified.
    """.trimIndent()

    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topP = 0.95f
                topK = 40
            },
            systemInstruction = content { text(consultantSystemInstruction) }
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
                Result.success(rawText.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateChatResponse(history: List<Pair<String, Boolean>>, prompt: String): Result<String> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be empty"))
        }

        return try {
            // Drop initial model greeting so history starts with a user message as required by Gemini SDK
            val userStartedHistory = history.dropWhile { !it.second }
            val chatHistory = userStartedHistory.map { (msg, isUser) ->
                content(if (isUser) "user" else "model") { text(msg) }
            }
            val chatSession = generativeModel.startChat(history = chatHistory)
            val response = chatSession.sendMessage(prompt)
            val rawText = response.text
            if (rawText.isNullOrBlank()) {
                generateResponse(prompt)
            } else {
                Result.success(rawText.trim())
            }
        } catch (e: Exception) {
            generateResponse(prompt)
        }
    }

    override suspend fun generatePrintingAdvice(userQuery: String, customerContext: String?): Result<String> {
        return generateResponse(userQuery)
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
