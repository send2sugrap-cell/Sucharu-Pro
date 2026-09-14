package com.sucharu.sucharupro.ui.customer.wall

/**
 * Wall Feed Offer Item Model.
 */
data class WallOfferItem(
    val offerId: String,
    val title: String,
    val description: String,
    val discountTag: String,
    val validUntil: String
)

/**
 * Wall Feed Service Item Model.
 */
data class WallServiceItem(
    val serviceId: String,
    val title: String,
    val description: String,
    val category: String
)

/**
 * Wall Feed Product Item Model.
 */
data class WallProductItem(
    val productId: String,
    val title: String,
    val category: String,
    val priceFormatted: String
)

/**
 * Wall Feed Announcement Item Model.
 */
data class WallAnnouncementItem(
    val announcementId: String,
    val title: String,
    val message: String,
    val dateFormatted: String,
    val tag: String
)

/**
 * Wall Feed Personal Activity Item Model.
 */
data class WallActivityItem(
    val activityId: String,
    val title: String,
    val timestamp: String,
    val statusLabel: String,
    val isPersonal: Boolean = true
)

/**
 * Top-Level Aggregate Data Model for Sucharu Wall Feed.
 */
data class WallFeedData(
    val featuredOffer: WallOfferItem?,
    val offers: List<WallOfferItem>,
    val services: List<WallServiceItem>,
    val products: List<WallProductItem>,
    val announcements: List<WallAnnouncementItem>,
    val personalActivities: List<WallActivityItem>,
    val unreadNotificationsCount: Int = 0
)

/**
 * Reactive Presentation UI State for Sucharu Wall / Universal Mobile Home.
 */
sealed interface SucharuWallUiState {
    object Loading : SucharuWallUiState
    data class Success(
        val feedData: WallFeedData,
        val isRefreshing: Boolean = false
    ) : SucharuWallUiState
    data class Error(val errorMessage: String) : SucharuWallUiState
    data class Empty(val message: String = "No Wall Feed Updates Available") : SucharuWallUiState
}
