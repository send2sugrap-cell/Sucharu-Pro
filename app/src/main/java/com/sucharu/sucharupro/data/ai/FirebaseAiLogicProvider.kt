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
        val systemPrompt = """
            আপনি সুচারু প্রো (Sucharu Pro) কমার্শিয়াল প্রিন্টিং ও কাস্টম প্যাকেজিং এআই সহকারী।
            গ্রাহকের যেকোনো প্রশ্ন (যেমন: কাগজের GSM, অফসেট বনাম ডিজিটাল প্রিন্টিং, স্পট ইউভি ল্যামিনেশন, প্যাকেজিং বক্স, খরচ ও দামের হিসাব) এর বিস্তারিত ও সঠিক উত্তর সম্পূর্ণ বাংলায় প্রদান করুন।
            
            কাস্টমার আইডি / প্রেক্ষাপট: ${customerContext ?: "সাধারণ গ্রাহক"}
            গ্রাহকের প্রশ্ন: $userQuery
        """.trimIndent()

        return generateResponse(systemPrompt)
    }
}
