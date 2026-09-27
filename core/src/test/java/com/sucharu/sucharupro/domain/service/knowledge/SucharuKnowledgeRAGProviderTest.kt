package com.sucharu.sucharupro.domain.service.knowledge

import com.sucharu.sucharupro.domain.model.knowledge.KnowledgeCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SucharuKnowledgeRAGProviderTest {

    private lateinit var provider: SucharuKnowledgeRAGProvider

    @Before
    fun setUp() {
        provider = SucharuKnowledgeRAGProvider()
    }

    @Test
    fun `searchKnowledge_returnsRelevantApprovedKnowledge`() {
        val results = provider.searchKnowledge("GSM", userRole = "CUSTOMER")

        assertNotNull(results)
        assertTrue(results.isNotEmpty())

        val matchedDoc = results.first().document
        assertEquals("KNOW-PRINT-002", matchedDoc.knowledgeId)
        assertEquals(KnowledgeCategory.PRINTING_SPECIFICATIONS, matchedDoc.category)
        assertTrue(matchedDoc.contentChunk.contains("300 GSM Art Card"))
    }

    @Test
    fun `searchKnowledge_enforcesRoleSensitivityBoundariesAndProtectsSecrets`() {
        // Customer Role searching for confidential margin/costing secrets -> MUST BE EMPTY!
        val customerResults = provider.searchKnowledge("margin", userRole = "CUSTOMER")
        assertTrue("Customer MUST NOT receive confidential margin secrets!", customerResults.isEmpty())

        // Admin Role searching for confidential margin secrets -> ACCESSIBLE!
        val adminResults = provider.searchKnowledge("margin", userRole = "ADMIN")
        assertEquals(1, adminResults.size)
        assertEquals("KNOW-CONF-004", adminResults.first().document.knowledgeId)
    }
}
