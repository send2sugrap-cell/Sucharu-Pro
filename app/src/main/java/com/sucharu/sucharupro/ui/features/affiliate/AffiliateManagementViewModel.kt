package com.sucharu.sucharupro.ui.features.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.affiliate.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Affiliate Management Command Center (Module 20 Step 01).
 */
class AffiliateManagementViewModel(
    private val useCases: BackendUseCases,
    private val principal: AuthenticatedPrincipal,
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        AffiliateManagementUiState(isPersonalView = principal.role == UserRole.AFFILIATE)
    )
    val uiState: StateFlow<AffiliateManagementUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                if (principal.role == UserRole.AFFILIATE) {
                    val myProfile = useCases.getMyAffiliateProfile(principal)
                    val eligibility = useCases.evaluateAffiliateEligibility(principal, myProfile.affiliateId)
                    val audits = useCases.listAffiliateAuditRecords(principal, myProfile.affiliateId)
                    val handoff = useCases.getAffiliateHandoffContract(principal, myProfile.affiliateId)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            affiliatesList = listOf(myProfile),
                            selectedAffiliate = myProfile,
                            selectedEligibility = eligibility,
                            selectedAuditRecords = audits,
                            selectedHandoffContract = handoff,
                            selectedTab = AffiliateCommandTab.PROFILE_ELIGIBILITY
                        )
                    }
                } else {
                    val summary = useCases.getAffiliateGovernanceSummary(principal)
                    val list = useCases.listAffiliates(principal)
                    val firstSelected = list.firstOrNull()

                    var eligibility: AffiliateEligibilityDto? = null
                    var audits: List<AffiliateAuditRecordDto> = emptyList()
                    var handoff: com.sucharu.sucharupro.domain.model.affiliate.Module20Step01AffiliateHandoffContract? = null

                    if (firstSelected != null) {
                        try {
                            eligibility = useCases.evaluateAffiliateEligibility(principal, firstSelected.affiliateId)
                            audits = useCases.listAffiliateAuditRecords(principal, firstSelected.affiliateId)
                            handoff = useCases.getAffiliateHandoffContract(principal, firstSelected.affiliateId)
                        } catch (_: Exception) {}
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            summary = summary,
                            affiliatesList = list,
                            selectedAffiliate = firstSelected,
                            selectedEligibility = eligibility,
                            selectedAuditRecords = audits,
                            selectedHandoffContract = handoff
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load affiliate governance data"
                    )
                }
            }
        }
    }

    fun selectTab(tab: AffiliateCommandTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectAffiliate(affiliateId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val aff = useCases.getAffiliateById(principal, affiliateId)
                val eligibility = useCases.evaluateAffiliateEligibility(principal, affiliateId)
                val audits = useCases.listAffiliateAuditRecords(principal, affiliateId)
                val handoff = useCases.getAffiliateHandoffContract(principal, affiliateId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedAffiliate = aff,
                        selectedEligibility = eligibility,
                        selectedAuditRecords = audits,
                        selectedHandoffContract = handoff
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to select affiliate: ${e.message}"
                    )
                }
            }
        }
    }

    fun createAffiliate(
        userId: String,
        displayName: String,
        affiliateCode: String?,
        affiliateType: String,
        contactPhone: String?,
        contactEmail: String?,
        taxIdOrGst: String?,
        agreementReference: String?
    ) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = CreateAffiliateRequestDto(
                    userId = userId,
                    displayName = displayName,
                    affiliateCode = affiliateCode,
                    affiliateType = affiliateType,
                    contactPhone = contactPhone,
                    contactEmail = contactEmail,
                    taxIdOrGst = taxIdOrGst,
                    agreementReference = agreementReference
                )
                val created = useCases.createAffiliate(principal, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Affiliate '${created.displayName}' (${created.affiliateCode}) created successfully!"
                    )
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to create affiliate: ${e.message}"
                    )
                }
            }
        }
    }

    fun activateAffiliate(affiliateId: String, reason: String = "Approved by Manager/Admin") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val activated = useCases.activateAffiliate(principal, affiliateId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Affiliate '${activated.affiliateCode}' has been ACTIVATED."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to activate affiliate: ${e.message}")
                }
            }
        }
    }

    fun suspendAffiliate(affiliateId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val suspended = useCases.suspendAffiliate(principal, affiliateId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Affiliate '${suspended.affiliateCode}' has been SUSPENDED."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to suspend affiliate: ${e.message}")
                }
            }
        }
    }

    fun reactivateAffiliate(affiliateId: String, reason: String = "Reactivated") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val reactivated = useCases.reactivateAffiliate(principal, affiliateId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Affiliate '${reactivated.affiliateCode}' has been REACTIVATED."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to reactivate affiliate: ${e.message}")
                }
            }
        }
    }

    fun rejectAffiliate(affiliateId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val rejected = useCases.rejectAffiliate(principal, affiliateId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Affiliate application '${rejected.affiliateCode}' was REJECTED."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to reject affiliate: ${e.message}")
                }
            }
        }
    }

    fun terminateAffiliate(affiliateId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val terminated = useCases.terminateAffiliate(principal, affiliateId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Affiliate '${terminated.affiliateCode}' has been TERMINATED."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to terminate affiliate: ${e.message}")
                }
            }
        }
    }

    fun acceptAgreement(affiliateId: String, agreementRef: String, version: String = "v1.0") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val req = AcceptAffiliateAgreementRequestDto(agreementRef, version)
                useCases.acceptAffiliateAgreement(principal, affiliateId, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Agreement accepted successfully."
                    )
                }
                selectAffiliate(affiliateId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to accept agreement: ${e.message}")
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(statusFilter = status) }
    }

    fun setTypeFilter(type: String?) {
        _uiState.update { it.copy(typeFilter = type) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
