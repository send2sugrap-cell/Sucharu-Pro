package com.sucharu.sucharupro.domain.service.ai.market

import com.sucharu.sucharupro.domain.model.ai.market.MarketSignalCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class MarketIntelligenceAgentServiceTest {

    private lateinit var service: MarketIntelligenceAgentService

    @Before
    fun setUp() {
        service = MarketIntelligenceAgentService()
    }

    @Test
    fun `generateMarketIntelligenceBrief_synthesizesMarketSignalsAndInternalBusinessFact`() = runBlocking {
        val brief = service.generateMarketIntelligenceBrief(userRole = "MANAGER", tenantId = "TENANT-001")

        assertNotNull(brief)
        assertEquals(2, brief.totalSignalsAnalyzed)
        assertEquals(BigDecimal("2450000.00"), brief.internalSalesRevenueYtd)
        assertEquals(BigDecimal("37.39"), brief.internalGrossMarginPct)
        assertTrue("Commercial secrecy MUST be protected in executive brief!", brief.isPricingSecretsProtected)

        assertEquals(2, brief.marketSignals.size)
        val sig1 = brief.marketSignals.first()
        assertEquals(MarketSignalCategory.COMMERCIAL_PRINTING_DEMAND, sig1.category)
        assertTrue(sig1.factObservation.startsWith("FACT:"))
        assertTrue(sig1.aiInterpretation.startsWith("ANALYSIS:"))
        assertTrue(sig1.recommendedAction.startsWith("RECOMMENDATION:"))
    }

    @Test(expected = IllegalAccessException::class)
    fun `generateMarketIntelligenceBrief_deniesAccessToCustomerRole`() {
        runBlocking {
            // Customer Role attempting to access internal executive market brief MUST be denied!
            service.generateMarketIntelligenceBrief(userRole = "CUSTOMER", tenantId = "TENANT-001")
        }
    }

    @Test
    fun `generateMarketingCampaignDraft_generatesNonBindingCampaignDraft`() = runBlocking {
        val draft = service.generateMarketingCampaignDraft(theme = "Hajj Leaflets", userRole = "STAFF")

        assertNotNull(draft)
        assertTrue(draft.contains("Hajj Leaflets"))
        assertTrue(draft.contains("অনুমোদন প্রয়োজন"))
    }
}
