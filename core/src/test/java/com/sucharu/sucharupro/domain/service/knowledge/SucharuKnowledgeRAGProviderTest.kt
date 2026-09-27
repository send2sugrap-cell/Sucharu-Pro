package com.sucharu.sucharupro.domain.service.knowledge

import com.sucharu.sucharupro.domain.model.knowledge.KnowledgeCategory
import com.sucharu.sucharupro.domain.model.knowledge.KnowledgeCategoryRegistry
import org.junit.Assert.assertEquals
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
    fun `taxonomyRegistry_containsExactly25CanonicalDomains`() {
        val domains = KnowledgeCategoryRegistry.ALL_25_DOMAINS

        assertEquals(25, domains.size)

        val uniqueCodes = domains.map { it.code }.toSet()
        assertEquals(25, uniqueCodes.size)

        val uniqueNumbers = domains.map { it.domainNumber }.toSet()
        assertEquals(25, uniqueNumbers.size)

        // Verify First and Last Domains
        assertEquals(1, domains.first().domainNumber)
        assertEquals(KnowledgeCategory.PRINTING_TECHNICAL, domains.first().code)

        assertEquals(25, domains.last().domainNumber)
        assertEquals(KnowledgeCategory.CROSS_INDUSTRY_PROFESSION, domains.last().code)
    }

    @Test
    fun `searchKnowledge_returnsRelevantApprovedKnowledgeForCoreOperationalDomains`() {
        // Query GSM paper guide
        val paperResults = provider.searchKnowledge("GSM", userRole = "CUSTOMER")
        assertTrue(paperResults.isNotEmpty())
        assertEquals("KNOW-PRINT-001", paperResults.first().document.knowledgeId)

        // Query VAT compliance guide
        val vatResults = provider.searchKnowledge("VAT", userRole = "CUSTOMER")
        assertTrue(vatResults.isNotEmpty())
        assertEquals(KnowledgeCategory.BANGLADESH_COMMERCIAL_COMPLIANCE, vatResults.first().document.category)

        // Query QC inspection SOP for Staff
        val qcResults = provider.searchKnowledge("inspection", userRole = "STAFF")
        assertTrue(qcResults.isNotEmpty())
        assertEquals(KnowledgeCategory.QUALITY_REWORK, qcResults.first().document.category)

        // Query Finished Goods Inventory SOP
        val invResults = provider.searchKnowledge("challan", userRole = "CUSTOMER")
        assertTrue(invResults.isNotEmpty())
        assertEquals(KnowledgeCategory.INVENTORY_DISTRIBUTION, invResults.first().document.category)
    }

    @Test
    fun `searchKnowledge_returnsRelevantGrowthMarketingAndSkillKnowledge`() {
        // Query Marketing Acquisition
        val mktResults = provider.searchKnowledge("acquisition", userRole = "CUSTOMER")
        assertTrue(mktResults.isNotEmpty())
        assertEquals(KnowledgeCategory.MARKETING_CUSTOMER_ACQUISITION, mktResults.first().document.category)

        // Query Digital Marketing Content Strategy
        val digiResults = provider.searchKnowledge("campaign", userRole = "CUSTOMER")
        assertTrue(digiResults.isNotEmpty())

        // Query B2B Sales Discovery
        val salesResults = provider.searchKnowledge("objection", userRole = "CUSTOMER")
        assertTrue(salesResults.isNotEmpty())
        assertEquals(KnowledgeCategory.B2B_B2C_SALES, salesResults.first().document.category)

        // Query AI Productivity
        val aiProdResults = provider.searchKnowledge("drafting", userRole = "CUSTOMER")
        assertTrue(aiProdResults.isNotEmpty())
        assertEquals(KnowledgeCategory.AI_BUSINESS_PRODUCTIVITY, aiProdResults.first().document.category)
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
