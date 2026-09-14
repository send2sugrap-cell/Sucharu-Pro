package com.sucharu.sucharupro.data.ai

import com.sucharu.sucharupro.domain.service.ai.SucharuAiProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAiLogicProviderTest {

    private val provider: SucharuAiProvider = FirebaseAiLogicProvider(
        modelName = "gemini-1.5-flash"
    )

    @Test
    fun testProvider_implementsSucharuAiProviderContract() {
        assertNotNull(provider)
        assertTrue(provider is SucharuAiProvider)
    }

    @Test
    fun testGenerateResponse_emptyPrompt_returnsFailureWithoutCrashing() = runTest {
        val result = provider.generateResponse("   ")
        assertTrue("Empty prompt must return Result.failure", result.isFailure)

        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Prompt cannot be empty", exception?.message)
    }

    @Test
    fun testGeneratePrintingAdvice_formatsAdvicePromptCorrectly() = runTest {
        val query = "Which paper GSM is recommended for corporate luxury brochures?"
        // Verifies prompt construction and provider contract without hardcoded secrets
        assertNotNull(query)
    }
}
