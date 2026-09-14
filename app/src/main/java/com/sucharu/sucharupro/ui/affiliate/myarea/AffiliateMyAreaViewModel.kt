package com.sucharu.sucharupro.ui.affiliate.myarea

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing presentation state for Affiliate My Area / Business Activity Center.
 */
class AffiliateMyAreaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AffiliateMyAreaUiState>(AffiliateMyAreaUiState.Loading)
    val uiState: StateFlow<AffiliateMyAreaUiState> = _uiState.asStateFlow()

    fun loadMyArea(principal: AuthenticatedPrincipal?) {
        viewModelScope.launch {
            _uiState.value = AffiliateMyAreaUiState.Loading

            try {
                val summary = buildAffiliateMyAreaSummary(principal)
                _uiState.value = AffiliateMyAreaUiState.Success(summary)
            } catch (e: Exception) {
                _uiState.value = AffiliateMyAreaUiState.Error(
                    e.localizedMessage ?: "Failed to load Affiliate My Area. Please try again."
                )
            }
        }
    }

    fun refresh(principal: AuthenticatedPrincipal?) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is AffiliateMyAreaUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            try {
                val summary = buildAffiliateMyAreaSummary(principal)
                _uiState.value = AffiliateMyAreaUiState.Success(summary = summary, isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = AffiliateMyAreaUiState.Error(
                    e.localizedMessage ?: "Failed to refresh Affiliate My Area."
                )
            }
        }
    }

    private fun buildAffiliateMyAreaSummary(principal: AuthenticatedPrincipal?): AffiliateMyAreaSummary {
        val affId = principal?.effectiveAffiliateId ?: "aff-001"
        val username = principal?.username ?: "Affiliate Partner"

        val profile = AffiliateProfileInfo(
            affiliateId = affId,
            affiliateCode = "APEX2026",
            displayName = if (username.isBlank()) "Apex Partner Network" else username,
            partnerType = "BUSINESS PARTNER",
            primaryPhone = "+880 1800-000088",
            email = "partner@apexnetwork.bd",
            status = "ACTIVE",
            referralLink = "https://sucharu.com/ref/APEX2026"
        )

        val referrals = ReferralCenterSummary(
            referralCode = "APEX2026",
            referralLink = "https://sucharu.com/ref/APEX2026",
            totalReferralsCount = 28,
            activeReferredCustomersCount = 14,
            totalReferredOrdersCount = 42,
            recentReferrals = listOf(
                ReferralItem("REF-101", "Acme Print (CUS-001)", "ORD-000001", "Today", "৳12,500.00", "APPROVED"),
                ReferralItem("REF-102", "Green Tech (CUS-003)", "ORD-000003", "Yesterday", "৳5,700.00", "PENDING")
            )
        )

        val performance = PerformanceCommissionSummary(
            totalEarnedFormatted = "৳142,500.00",
            pendingClearanceFormatted = "৳18,200.00",
            approvedCommissionFormatted = "৳124,300.00",
            paidDisbursedFormatted = "৳10,000.00"
        )

        val wallet = WalletSnapshot(
            walletId = "WLT-AFF-001",
            availableToWithdrawFormatted = "৳42,300.00",
            heldReserveFormatted = "৳8,000.00",
            totalDisbursedFormatted = "৳10,000.00",
            recentPayouts = listOf(
                PayoutItem("PO-REQ-001", "৳10,000.00", "Bank Transfer", "10 Sept 2026", "COMPLETED")
            )
        )

        val campaigns = listOf(
            AffiliateCampaignItem(
                campaignId = "CAMP-001",
                title = "Bulk Printing Commission Booster",
                description = "Earn 5% extra bonus commission on all commercial brochure orders over 2,000 units.",
                commissionRateLabel = "+5% BONUS",
                validUntilFormatted = "30 Sept 2026"
            )
        )

        return AffiliateMyAreaSummary(
            profile = profile,
            referrals = referrals,
            performance = performance,
            wallet = wallet,
            campaigns = campaigns,
            unreadNotificationsCount = 0
        )
    }
}
