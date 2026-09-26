package com.sucharu.sucharupro.ui.admin.offer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.repository.PromotionalOfferRepositoryImpl
import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.offer.OfferType
import com.sucharu.sucharupro.domain.model.offer.PromotionalOffer
import com.sucharu.sucharupro.domain.model.offer.WallType
import com.sucharu.sucharupro.domain.repository.PromotionalOfferRepository
import com.sucharu.sucharupro.domain.service.offer.PromotionalOfferService
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Form 03 — Admin Offer & Audience Eligibility Management.
 */
class AdminOfferEligibilityViewModel(
    repository: PromotionalOfferRepository = PromotionalOfferRepositoryImpl()
) : ViewModel() {

    private val service = PromotionalOfferService(repository)

    var offerList = mutableStateListOf<PromotionalOffer>()
        private set

    var activeOfferState by mutableStateOf<PromotionalOffer>(
        PromotionalOffer(
            offerId = "OFFER-2026-EID",
            offerName = "Eid Special Bulk Visiting Card Offer",
            offerCode = "EID-2026-VC",
            offerType = OfferType.PROMOTIONAL_DISCOUNT,
            description = "১০০০ মেট ফিনিশ ভিজিটিং কার্ড বিশেষ ছাড়ে।",
            badgeText = "১৫% ছাড়",
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "ADMIN"
        )
    )
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadOffers()
    }

    fun loadOffers() {
        viewModelScope.launch {
            val list = service.listAllOffers()
            offerList.clear()
            offerList.addAll(list)
            list.firstOrNull()?.let { activeOfferState = it }
        }
    }

    fun updateActiveOffer(updated: PromotionalOffer) {
        activeOfferState = updated
    }

    fun saveOffer() {
        viewModelScope.launch {
            val timestamp = "2026-09-26T12:30:00Z"
            val saved = service.createOfferConfig(
                activeOfferState.copy(
                    updatedAt = timestamp,
                    updatedBy = "ADMIN"
                )
            )
            activeOfferState = saved
            loadOffers()
            statusMessage = "✓ Offer configuration saved successfully!"
        }
    }
}
