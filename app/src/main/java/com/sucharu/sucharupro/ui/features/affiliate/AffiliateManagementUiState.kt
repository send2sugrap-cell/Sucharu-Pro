package com.sucharu.sucharupro.ui.features.affiliate

import com.sucharu.sucharupro.data.api.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.Module20Step01AffiliateHandoffContract

enum class AffiliateCommandTab(val title: String) {
    OVERVIEW("Overview"),
    DIRECTORY("Directory"),
    PENDING_APPROVAL("Pending"),
    ACTIVE_SUSPENDED("Active & Suspended"),
    PROFILE_ELIGIBILITY("Profile & Eligibility"),
    AI_HANDOFF("AI Handoff")
}

data class AffiliateManagementUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedTab: AffiliateCommandTab = AffiliateCommandTab.OVERVIEW,
    val summary: AffiliateGovernanceSummaryDto? = null,
    val affiliatesList: List<AffiliateProfileDto> = emptyList(),
    val selectedAffiliate: AffiliateProfileDto? = null,
    val selectedEligibility: AffiliateEligibilityDto? = null,
    val selectedAuditRecords: List<AffiliateAuditRecordDto> = emptyList(),
    val selectedHandoffContract: Module20Step01AffiliateHandoffContract? = null,
    val statusFilter: String? = null,
    val typeFilter: String? = null,
    val searchQuery: String = "",
    val isCreatingAffiliate: Boolean = false,
    val isPersonalView: Boolean = false
) {
    val filteredAffiliates: List<AffiliateProfileDto>
        get() = affiliatesList.filter { aff ->
            val matchesQuery = searchQuery.isBlank() ||
                    aff.displayName.contains(searchQuery, ignoreCase = true) ||
                    aff.affiliateCode.contains(searchQuery, ignoreCase = true) ||
                    aff.userId.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == null || aff.status.equals(statusFilter, ignoreCase = true)
            val matchesType = typeFilter == null || aff.affiliateType.equals(typeFilter, ignoreCase = true)
            matchesQuery && matchesStatus && matchesType
        }

    val pendingCount: Int get() = affiliatesList.count { it.status == "PENDING" }
    val activeCount: Int get() = affiliatesList.count { it.status == "ACTIVE" }
    val suspendedCount: Int get() = affiliatesList.count { it.status == "SUSPENDED" }
}
