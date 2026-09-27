package com.sucharu.sucharupro.data.api.model.ai.market

import kotlinx.serialization.Serializable

@Serializable
data class GroundedMarketSignalDto(
    val signalId: String,
    val category: String,
    val scope: String = "BANGLADESH",
    val title: String,
    val factObservation: String,
    val aiInterpretation: String,
    val recommendedAction: String,
    val sourceReference: String = "VERIFIED_MARKET_RESEARCH",
    val confidenceScore: Double = 0.90
)

@Serializable
data class ExecutiveMarketIntelligenceBriefDto(
    val totalSignalsAnalyzed: Int,
    val highPrioritySignalsCount: Int,
    val internalSalesRevenueYtd: String,
    val internalGrossMarginPct: String,
    val marketSignals: List<GroundedMarketSignalDto> = emptyList(),
    val campaignDrafts: List<String> = emptyList(),
    val isPricingSecretsProtected: Boolean = true,
    val generatedAt: String
)

@Serializable
data class GenerateCampaignDraftRequestDto(
    val theme: String = "Corporate Calendar"
)
