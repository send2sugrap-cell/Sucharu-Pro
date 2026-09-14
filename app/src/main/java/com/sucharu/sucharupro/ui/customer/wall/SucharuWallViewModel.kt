package com.sucharu.sucharupro.ui.customer.wall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing presentation state and feed orchestration for Sucharu Wall / Universal Mobile Home.
 */
class SucharuWallViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SucharuWallUiState>(SucharuWallUiState.Loading)
    val uiState: StateFlow<SucharuWallUiState> = _uiState.asStateFlow()

    fun loadWallFeed(principal: AuthenticatedPrincipal?) {
        viewModelScope.launch {
            _uiState.value = SucharuWallUiState.Loading

            try {
                val feed = buildWallFeedData(principal)
                _uiState.value = if (feed.offers.isEmpty() && feed.services.isEmpty() && feed.announcements.isEmpty()) {
                    SucharuWallUiState.Empty("No Wall Feed Updates Available")
                } else {
                    SucharuWallUiState.Success(feed)
                }
            } catch (e: Exception) {
                _uiState.value = SucharuWallUiState.Error(
                    e.localizedMessage ?: "Failed to load Sucharu Wall feed. Please try again."
                )
            }
        }
    }

    fun refresh(principal: AuthenticatedPrincipal?) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is SucharuWallUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            try {
                val feed = buildWallFeedData(principal)
                _uiState.value = SucharuWallUiState.Success(feedData = feed, isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = SucharuWallUiState.Error(
                    e.localizedMessage ?: "Failed to refresh Sucharu Wall feed."
                )
            }
        }
    }

    private fun buildWallFeedData(principal: AuthenticatedPrincipal?): WallFeedData {
        val role = principal?.role ?: UserRole.CUSTOMER

        val featured = WallOfferItem(
            offerId = "OFFER-EX-001",
            title = "Eid Special Bulk Printing Offer",
            description = "Get 15% discount on custom business cards, brochures, and packaging.",
            discountTag = "15% DISCOUNT",
            validUntil = "Valid until 30 Sept 2026"
        )

        val offers = listOf(
            featured,
            WallOfferItem(
                offerId = "OFFER-EX-002",
                title = "2027 Executive Calendar Pre-Order",
                description = "Early bird pricing for custom desktop and wall calendars.",
                discountTag = "EARLY BIRD 10%",
                validUntil = "Valid until 15 Oct 2026"
            )
        )

        val services = listOf(
            WallServiceItem("SRV-001", "Custom Business Cards & Stationery", "300GSM art card with matte/gloss lamination", "Stationery"),
            WallServiceItem("SRV-002", "Corporate Brochure & Packaging", "Offset full color with precision fold & die-cut", "Packaging"),
            WallServiceItem("SRV-003", "Digital Fast Printing", "Same-day dispatch for urgent print jobs", "Digital")
        )

        val products = listOf(
            WallProductItem("PROD-001", "Holy Quran Sharif (Sub-Continent Font)", "Religious Book", "৳350.00 / Pc"),
            WallProductItem("PROD-002", "Custom Executive Leather Diary 2027", "Corporate Gift", "৳450.00 / Pc"),
            WallProductItem("PROD-003", "Commercial Wall Calendar (7 Leaf)", "Calendar", "৳120.00 / Pc")
        )

        val announcements = listOf(
            WallAnnouncementItem(
                announcementId = "ANN-001",
                title = "New High-Speed HP Indigo Digital Press Added",
                message = "We have expanded our digital printing capacity for faster same-day order fulfillment.",
                dateFormatted = "12 Sept 2026",
                tag = "EQUIPMENT UPGRADE"
            )
        )

        val personalActivities = if (role == UserRole.AFFILIATE) {
            listOf(
                WallActivityItem("ACT-AFF-101", "Referral Signup: Apex Partner Network", "Today, 11:20 AM", "APPROVED"),
                WallActivityItem("ACT-AFF-102", "Commission Earned: ৳12,500.00 (Order #ORD-000001)", "Yesterday", "CREDITED"),
                WallActivityItem("ACT-AFF-103", "Wallet Payout Request #PO-REQ-001 Processed", "10 Sept 2026", "COMPLETED")
            )
        } else {
            listOf(
                WallActivityItem("ACT-CUS-201", "Commercial Order #ORD-000001 Confirmed", "Today, 10:30 AM", "CONFIRMED"),
                WallActivityItem("ACT-CUS-202", "Invoice #INV-000001 Payment Received", "Yesterday", "PAID"),
                WallActivityItem("ACT-CUS-203", "Challan #CH-000001 Dispatched via Paperfly", "10 Sept 2026", "DELIVERED")
            )
        }

        return WallFeedData(
            featuredOffer = featured,
            offers = offers,
            services = services,
            products = products,
            announcements = announcements,
            personalActivities = personalActivities,
            unreadNotificationsCount = 0
        )
    }
}
