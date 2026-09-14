package com.sucharu.sucharupro.data.ai

import com.google.firebase.Firebase
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.vertexAI
import com.sucharu.sucharupro.domain.service.ai.SucharuAiProvider

/**
 * Concrete Provider Implementation for Official Firebase Vertex AI / Firebase AI Logic.
 *
 * Implements [SucharuAiProvider] using official Firebase Vertex AI Android SDK ("gemini-1.5-flash").
 *
 * SECURITY GUARANTEE:
 * Zero hardcoded API keys in source code. Firebase.vertexAI automatically leverages the
 * 'google-services.json' context, App Check attestation, and Firebase backend security rules.
 */
class FirebaseAiLogicProvider(
    private val modelName: String = "gemini-1.5-flash"
) : SucharuAiProvider {

    private val generativeModel: GenerativeModel by lazy {
        Firebase.vertexAI.generativeModel(modelName)
    }

    override suspend fun generateResponse(prompt: String): Result<String> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Prompt cannot be empty"))
        }

        return try {
            val response = generativeModel.generateContent(prompt)
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Firebase Vertex AI returned empty or null response"))
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
