package com.sucharu.sucharupro.domain.model.ai.market

import java.math.BigDecimal

/**
 * Controlled Market Signal Categories.
 */
enum class MarketSignalCategory {
    COMMERCIAL_PRINTING_DEMAND,
    CUSTOMER_DEMAND_PATTERNS,
    MARKETING_CAMPAIGN_OPPORTUNITIES,
    COMPETITIVE_OBSERVATIONS,
    BUSINESS_ENVIRONMENT_DEVELOPMENTS
}

/**
 * Source-Grounded Market Signal Observation with Fact / Interpretation / Recommendation Separation.
 */
data class GroundedMarketSignal(
    val signalId: String,
    val category: MarketSignalCategory,
    val scope: String = "BANGLADESH", // BANGLADESH, REGIONAL, GLOBAL
    val title: String,
    val factObservation: String,      // Observed External Fact
    val aiInterpretation: String,     // Source-Based Analysis
    val recommendedAction: String,    // Recommendation for Management
    val sourceReference: String = "VERIFIED_MARKET_RESEARCH",
    val confidenceScore: Double = 0.90
)

/**
 * Master Executive Market Intelligence & Business Copilot Briefing Model.
 */
data class ExecutiveMarketIntelligenceBrief(
    val totalSignalsAnalyzed: Int,
    val highPrioritySignalsCount: Int,
    val internalSalesRevenueYtd: BigDecimal,
    val internalGrossMarginPct: BigDecimal,
    val marketSignals: List<GroundedMarketSignal> = emptyList(),
    val campaignDrafts: List<String> = emptyList(),
    val isPricingSecretsProtected: Boolean = true, // Critical Invariant: Always true!
    val generatedAt: String
)
