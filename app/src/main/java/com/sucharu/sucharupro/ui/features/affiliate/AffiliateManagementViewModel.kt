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
 * ViewModel for Affiliate Management Command Center (Module 20 Steps 01, 02 & 03).
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
                    val myEnrollments = useCases.listAffiliateEnrollments(principal, affiliateId = myProfile.affiliateId)
                    val availablePrograms = useCases.listAffiliatePrograms(principal, status = "ACTIVE")

                    // Step 03 loads
                    var opProfile: AffiliateOperationalProfileResponseDto? = null
                    var completeness: ProfileCompletenessResponseDto? = null
                    var verifs: List<AffiliateVerificationResponseDto> = emptyList()
                    var docs: List<AffiliateDocumentResponseDto> = emptyList()
                    var profileAudits: List<AffiliateProfileAuditResponseDto> = emptyList()
                    var profileHandoff: com.sucharu.sucharupro.domain.model.affiliate.Module20Step03AffiliateProfileHandoffContract? = null

                    try {
                        opProfile = useCases.getAffiliateOperationalProfile(principal, myProfile.affiliateId)
                        completeness = useCases.getAffiliateProfileCompleteness(principal, myProfile.affiliateId)
                        verifs = useCases.listAffiliateVerifications(principal, myProfile.affiliateId)
                        docs = useCases.listAffiliateDocuments(principal, myProfile.affiliateId)
                        profileAudits = useCases.listAffiliateProfileAuditRecords(principal, myProfile.affiliateId)
                        profileHandoff = useCases.getAffiliateProfileHandoffContract(principal, myProfile.affiliateId)
                    } catch (_: Exception) {}

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            affiliatesList = listOf(myProfile),
                            selectedAffiliate = myProfile,
                            selectedEligibility = eligibility,
                            selectedAuditRecords = audits,
                            selectedHandoffContract = handoff,
                            enrollmentsList = myEnrollments,
                            programsList = availablePrograms,
                            selectedOperationalProfile = opProfile,
                            selectedCompleteness = completeness,
                            selectedVerifications = verifs,
                            selectedDocuments = docs,
                            selectedProfileAudits = profileAudits,
                            selectedProfileHandoffContract = profileHandoff,
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
                    var opProfile: AffiliateOperationalProfileResponseDto? = null
                    var completeness: ProfileCompletenessResponseDto? = null
                    var verifs: List<AffiliateVerificationResponseDto> = emptyList()
                    var docs: List<AffiliateDocumentResponseDto> = emptyList()
                    var profileAudits: List<AffiliateProfileAuditResponseDto> = emptyList()
                    var profileHandoff: com.sucharu.sucharupro.domain.model.affiliate.Module20Step03AffiliateProfileHandoffContract? = null

                    if (firstSelected != null) {
                        try {
                            eligibility = useCases.evaluateAffiliateEligibility(principal, firstSelected.affiliateId)
                            audits = useCases.listAffiliateAuditRecords(principal, firstSelected.affiliateId)
                            handoff = useCases.getAffiliateHandoffContract(principal, firstSelected.affiliateId)
                            opProfile = useCases.getAffiliateOperationalProfile(principal, firstSelected.affiliateId)
                            completeness = useCases.getAffiliateProfileCompleteness(principal, firstSelected.affiliateId)
                            verifs = useCases.listAffiliateVerifications(principal, firstSelected.affiliateId)
                            docs = useCases.listAffiliateDocuments(principal, firstSelected.affiliateId)
                            profileAudits = useCases.listAffiliateProfileAuditRecords(principal, firstSelected.affiliateId)
                            profileHandoff = useCases.getAffiliateProfileHandoffContract(principal, firstSelected.affiliateId)
                        } catch (_: Exception) {}
                    }

                    // Step 02 loads
                    var progSummary: AffiliateProgramGovernanceSummaryDto? = null
                    var progs: List<AffiliateProgramDto> = emptyList()
                    var enrolls: List<AffiliateEnrollmentDto> = emptyList()
                    try {
                        progSummary = useCases.getAffiliateProgramGovernanceSummary(principal)
                        progs = useCases.listAffiliatePrograms(principal)
                        enrolls = useCases.listAffiliateEnrollments(principal)
                    } catch (_: Exception) {}

                    // Step 03 Governance Summary
                    var profSummary: AffiliateProfileGovernanceSummaryResponseDto? = null
                    try {
                        profSummary = useCases.getAffiliateProfileGovernanceSummary(principal)
                    } catch (_: Exception) {}

                    // Step 04 Governance Summary & Communications
                    var commSummary: com.sucharu.sucharupro.data.api.model.affiliate.AffiliateNotificationGovernanceSummaryResponseDto? = null
                    var commsList: List<com.sucharu.sucharupro.data.api.model.affiliate.AffiliateCommunicationResponseDto> = emptyList()
                    var notifPrefs: List<com.sucharu.sucharupro.data.api.model.affiliate.AffiliateNotificationPreferenceResponseDto> = emptyList()
                    var unreadCount: Long = 0L
                    try {
                        commSummary = useCases.getAffiliateCommunicationGovernanceSummary(principal)
                        if (firstSelected != null) {
                            commsList = useCases.listAffiliateNotifications(principal, firstSelected.affiliateId)
                            notifPrefs = useCases.getAffiliateNotificationPreferences(principal, firstSelected.affiliateId)
                            unreadCount = useCases.getAffiliateUnreadNotificationCount(principal, firstSelected.affiliateId).totalUnread
                        }
                    } catch (_: Exception) {}

                    val firstProg = progs.firstOrNull()
                    val firstEnroll = enrolls.firstOrNull()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            summary = summary,
                            affiliatesList = list,
                            selectedAffiliate = firstSelected,
                            selectedEligibility = eligibility,
                            selectedAuditRecords = audits,
                            selectedHandoffContract = handoff,
                            programSummary = progSummary,
                            programsList = progs,
                            selectedProgram = firstProg,
                            enrollmentsList = enrolls,
                            selectedEnrollment = firstEnroll,
                            profileSummary = profSummary,
                            selectedOperationalProfile = opProfile,
                            selectedCompleteness = completeness,
                            selectedVerifications = verifs,
                            selectedDocuments = docs,
                            selectedProfileAudits = profileAudits,
                            selectedProfileHandoffContract = profileHandoff,
                            communicationSummary = commSummary,
                            communicationsList = commsList,
                            notificationPreferences = notifPrefs,
                            unreadCommunicationCount = unreadCount
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

                var opProfile: AffiliateOperationalProfileResponseDto? = null
                var completeness: ProfileCompletenessResponseDto? = null
                var verifs: List<AffiliateVerificationResponseDto> = emptyList()
                var docs: List<AffiliateDocumentResponseDto> = emptyList()
                var profileAudits: List<AffiliateProfileAuditResponseDto> = emptyList()
                var profileHandoff: com.sucharu.sucharupro.domain.model.affiliate.Module20Step03AffiliateProfileHandoffContract? = null

                try {
                    opProfile = useCases.getAffiliateOperationalProfile(principal, affiliateId)
                    completeness = useCases.getAffiliateProfileCompleteness(principal, affiliateId)
                    verifs = useCases.listAffiliateVerifications(principal, affiliateId)
                    docs = useCases.listAffiliateDocuments(principal, affiliateId)
                    profileAudits = useCases.listAffiliateProfileAuditRecords(principal, affiliateId)
                    profileHandoff = useCases.getAffiliateProfileHandoffContract(principal, affiliateId)
                } catch (_: Exception) {}

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedAffiliate = aff,
                        selectedEligibility = eligibility,
                        selectedAuditRecords = audits,
                        selectedHandoffContract = handoff,
                        selectedOperationalProfile = opProfile,
                        selectedCompleteness = completeness,
                        selectedVerifications = verifs,
                        selectedDocuments = docs,
                        selectedProfileAudits = profileAudits,
                        selectedProfileHandoffContract = profileHandoff
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
                        successMessage = "Affiliate '${created.displayName}' created with code '${created.affiliateCode}'."
                    )
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to create affiliate: ${e.message}")
                }
            }
        }
    }

    fun activateAffiliate(affiliateId: String, reason: String = "Activated by Admin") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val activated = useCases.activateAffiliate(principal, affiliateId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Affiliate '${activated.displayName}' ACTIVATED."
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
                        successMessage = "Affiliate '${suspended.displayName}' SUSPENDED."
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
                        successMessage = "Affiliate '${reactivated.displayName}' REACTIVATED."
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
                        successMessage = "Affiliate '${rejected.displayName}' REJECTED."
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

    fun acceptAgreement(affiliateId: String, agreementReference: String, agreementVersion: String = "v1.0") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val req = AcceptAffiliateAgreementRequestDto(
                    agreementReference = agreementReference,
                    agreementVersion = agreementVersion
                )
                val accepted = useCases.acceptAffiliateAgreement(principal, affiliateId, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Agreement '${accepted.agreementReference}' accepted."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to accept agreement: ${e.message}")
                }
            }
        }
    }

    // --- Step 02 Program & Enrollment Operations ---

    fun createProgram(
        programCode: String,
        programName: String,
        description: String?,
        startDate: Long,
        endDate: Long?,
        eligibilityPolicy: String = "STANDARD",
        termsReference: String? = null,
        termsVersion: String? = null,
        maxParticipants: Int? = null,
        metadataJson: String? = null
    ) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = CreateAffiliateProgramRequestDto(
                    programCode = programCode,
                    programName = programName,
                    description = description,
                    startDate = startDate,
                    endDate = endDate,
                    eligibilityPolicy = eligibilityPolicy,
                    termsReference = termsReference,
                    termsVersion = termsVersion,
                    maxParticipants = maxParticipants,
                    metadataJson = metadataJson
                )
                val prog = useCases.createAffiliateProgram(principal, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Affiliate Program '${prog.programName}' created (${prog.programCode})."
                    )
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to create program: ${e.message}")
                }
            }
        }
    }

    fun selectProgram(programId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prog = useCases.getAffiliateProgramById(principal, programId)
                val audits = useCases.listAffiliateProgramAuditRecords(principal, programId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedProgram = prog,
                        selectedProgramAudits = audits
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to select program: ${e.message}")
                }
            }
        }
    }

    fun activateProgram(programId: String, reason: String = "Launched") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prog = useCases.activateAffiliateProgram(principal, programId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Program '${prog.programName}' ACTIVATED."
                    )
                }
                selectProgram(programId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to activate program: ${e.message}")
                }
            }
        }
    }

    fun pauseProgram(programId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prog = useCases.pauseAffiliateProgram(principal, programId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Program '${prog.programName}' PAUSED."
                    )
                }
                selectProgram(programId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to pause program: ${e.message}")
                }
            }
        }
    }

    fun closeProgram(programId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prog = useCases.closeAffiliateProgram(principal, programId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Program '${prog.programName}' CLOSED."
                    )
                }
                selectProgram(programId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to close program: ${e.message}")
                }
            }
        }
    }

    fun selectEnrollment(enrollmentId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val enr = useCases.getAffiliateEnrollmentById(principal, enrollmentId)
                val audits = useCases.listAffiliateEnrollmentAuditRecords(principal, enrollmentId)
                val handoff = useCases.getAffiliateProgramHandoffContract(principal, enrollmentId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedEnrollment = enr,
                        selectedEnrollmentAudits = audits,
                        selectedProgramHandoffContract = handoff
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to select enrollment: ${e.message}")
                }
            }
        }
    }

    fun enrollAffiliate(
        programId: String,
        affiliateId: String,
        enrollmentReason: String? = null,
        effectiveFrom: Long? = null,
        effectiveTo: Long? = null,
        metadataJson: String? = null
    ) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = EnrollAffiliateRequestDto(
                    affiliateId = affiliateId,
                    programId = programId,
                    enrollmentReason = enrollmentReason,
                    effectiveFrom = effectiveFrom,
                    effectiveTo = effectiveTo,
                    metadataJson = metadataJson
                )
                val enrollment = useCases.enrollAffiliateInProgram(principal, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Enrollment created in status '${enrollment.enrollmentStatus}'."
                    )
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to enroll affiliate: ${e.message}")
                }
            }
        }
    }

    fun approveEnrollment(enrollmentId: String, reason: String = "Approved") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val approved = useCases.approveAffiliateEnrollment(principal, enrollmentId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Enrollment ${approved.enrollmentId} APPROVED."
                    )
                }
                selectEnrollment(enrollmentId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to approve enrollment: ${e.message}")
                }
            }
        }
    }

    fun rejectEnrollment(enrollmentId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val rejected = useCases.rejectAffiliateEnrollment(principal, enrollmentId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Enrollment ${rejected.enrollmentId} REJECTED."
                    )
                }
                selectEnrollment(enrollmentId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to reject enrollment: ${e.message}")
                }
            }
        }
    }

    fun activateEnrollment(enrollmentId: String, reason: String = "Activated") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val activated = useCases.activateAffiliateEnrollment(principal, enrollmentId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Enrollment ${activated.enrollmentId} ACTIVATED."
                    )
                }
                selectEnrollment(enrollmentId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to activate enrollment: ${e.message}")
                }
            }
        }
    }

    fun suspendEnrollment(enrollmentId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val suspended = useCases.suspendAffiliateEnrollment(principal, enrollmentId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Enrollment ${suspended.enrollmentId} SUSPENDED."
                    )
                }
                selectEnrollment(enrollmentId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to suspend enrollment: ${e.message}")
                }
            }
        }
    }

    fun resumeEnrollment(enrollmentId: String, reason: String = "Resumed") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resumed = useCases.resumeAffiliateEnrollment(principal, enrollmentId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Enrollment ${resumed.enrollmentId} RESUMED."
                    )
                }
                selectEnrollment(enrollmentId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to resume enrollment: ${e.message}")
                }
            }
        }
    }

    fun terminateEnrollment(enrollmentId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val terminated = useCases.terminateAffiliateEnrollment(principal, enrollmentId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Enrollment ${terminated.enrollmentId} TERMINATED."
                    )
                }
                selectEnrollment(enrollmentId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to terminate enrollment: ${e.message}")
                }
            }
        }
    }

    // --- Step 03 Profile, Verification & Governance Operations ---

    fun upsertOperationalProfile(
        affiliateId: String,
        displayName: String,
        legalName: String? = null,
        businessType: String = "INDIVIDUAL",
        businessDescription: String? = null,
        contactEmail: String? = null,
        contactPhone: String? = null,
        website: String? = null,
        addressLine1: String? = null,
        addressLine2: String? = null,
        city: String? = null,
        region: String? = null,
        country: String? = null,
        postalCode: String? = null,
        taxIdOrGst: String? = null,
        taxInformationReference: String? = null,
        metadataJson: String? = null
    ) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = UpsertAffiliateProfileRequestDto(
                    displayName = displayName,
                    legalName = legalName,
                    businessType = businessType,
                    businessDescription = businessDescription,
                    contactEmail = contactEmail,
                    contactPhone = contactPhone,
                    website = website,
                    addressLine1 = addressLine1,
                    addressLine2 = addressLine2,
                    city = city,
                    region = region,
                    country = country,
                    postalCode = postalCode,
                    taxIdOrGst = taxIdOrGst,
                    taxInformationReference = taxInformationReference,
                    metadataJson = metadataJson
                )
                val profile = useCases.upsertAffiliateOperationalProfile(principal, affiliateId, req)
                val completeness = useCases.getAffiliateProfileCompleteness(principal, affiliateId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedOperationalProfile = profile,
                        selectedCompleteness = completeness,
                        successMessage = "Operational profile updated (Completeness: ${profile.completenessScore}%)."
                    )
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to update profile: ${e.message}")
                }
            }
        }
    }

    fun submitOperationalProfile(affiliateId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val profile = useCases.submitAffiliateOperationalProfile(principal, affiliateId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedOperationalProfile = profile,
                        successMessage = "Profile submitted for verification review."
                    )
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to submit profile: ${e.message}")
                }
            }
        }
    }

    fun requestVerification(
        affiliateId: String,
        verificationType: String,
        reason: String? = null,
        metadataReference: String? = null
    ) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = RequestVerificationRequestDto(
                    verificationType = verificationType,
                    reason = reason,
                    metadataReference = metadataReference
                )
                val verif = useCases.requestAffiliateVerification(principal, affiliateId, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Verification check requested (${verif.verificationType})."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to request verification: ${e.message}")
                }
            }
        }
    }

    fun approveVerification(verificationId: String, reason: String = "Verified") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = ReviewVerificationRequestDto(reason = reason)
                val verif = useCases.approveAffiliateVerification(principal, verificationId, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Verification ${verif.verificationId} APPROVED."
                    )
                }
                val affId = _uiState.value.selectedAffiliate?.affiliateId
                if (affId != null) selectAffiliate(affId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to approve verification: ${e.message}")
                }
            }
        }
    }

    fun rejectVerification(verificationId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = ReviewVerificationRequestDto(reason = reason)
                val verif = useCases.rejectAffiliateVerification(principal, verificationId, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Verification ${verif.verificationId} REJECTED."
                    )
                }
                val affId = _uiState.value.selectedAffiliate?.affiliateId
                if (affId != null) selectAffiliate(affId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to reject verification: ${e.message}")
                }
            }
        }
    }

    fun requestVerificationChanges(verificationId: String, reason: String, changeRequestNotes: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = ReviewVerificationRequestDto(
                    reason = reason,
                    changeRequestNotes = changeRequestNotes
                )
                val verif = useCases.requestAffiliateVerificationChanges(principal, verificationId, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Changes requested for verification ${verif.verificationId}."
                    )
                }
                val affId = _uiState.value.selectedAffiliate?.affiliateId
                if (affId != null) selectAffiliate(affId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to request changes: ${e.message}")
                }
            }
        }
    }

    fun addDocumentReference(
        affiliateId: String,
        documentType: String,
        storageReference: String,
        fileName: String,
        verificationId: String? = null,
        fileSizeBytes: Long? = null,
        mimeType: String? = null
    ) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = AddDocumentReferenceRequestDto(
                    verificationId = verificationId,
                    documentType = documentType,
                    storageReference = storageReference,
                    fileName = fileName,
                    fileSizeBytes = fileSizeBytes,
                    mimeType = mimeType
                )
                val doc = useCases.addAffiliateDocumentReference(principal, affiliateId, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Document '${doc.fileName}' uploaded."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to upload document reference: ${e.message}")
                }
            }
        }
    }

    fun verifyDocumentReference(documentId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val doc = useCases.verifyAffiliateDocument(principal, documentId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Document '${doc.fileName}' VERIFIED."
                    )
                }
                val affId = _uiState.value.selectedAffiliate?.affiliateId
                if (affId != null) selectAffiliate(affId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to verify document: ${e.message}")
                }
            }
        }
    }

    fun rejectDocumentReference(documentId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val req = ReviewDocumentRequestDto(rejectionReason = reason)
                val doc = useCases.rejectAffiliateDocument(principal, documentId, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Document '${doc.fileName}' REJECTED."
                    )
                }
                val affId = _uiState.value.selectedAffiliate?.affiliateId
                if (affId != null) selectAffiliate(affId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to reject document: ${e.message}")
                }
            }
        }
    }

    fun suspendOperationalProfile(affiliateId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val prof = useCases.suspendAffiliateProfile(principal, affiliateId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedOperationalProfile = prof,
                        successMessage = "Profile SUSPENDED."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to suspend profile: ${e.message}")
                }
            }
        }
    }

    fun reactivateOperationalProfile(affiliateId: String, reason: String = "Reactivated") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val prof = useCases.reactivateAffiliateProfile(principal, affiliateId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedOperationalProfile = prof,
                        successMessage = "Profile REACTIVATED."
                    )
                }
                selectAffiliate(affiliateId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to reactivate profile: ${e.message}")
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

    fun setProgramSearchQuery(query: String) {
        _uiState.update { it.copy(programSearchQuery = query) }
    }

    fun setProgramStatusFilter(status: String?) {
        _uiState.update { it.copy(programStatusFilter = status) }
    }

    fun setEnrollmentStatusFilter(status: String?) {
        _uiState.update { it.copy(enrollmentStatusFilter = status) }
    }

    fun setVerificationStatusFilter(status: String?) {
        _uiState.update { it.copy(verificationStatusFilter = status) }
    }

    fun sendAffiliateCommunication(
        affiliateId: String,
        communicationType: String,
        title: String? = null,
        message: String? = null
    ) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val req = com.sucharu.sucharupro.data.api.model.affiliate.EmitAffiliateCommunicationRequestDto(
                    affiliateId = affiliateId,
                    communicationType = communicationType,
                    title = title,
                    message = message
                )
                useCases.emitAffiliateCommunication(principal, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Communication sent successfully."
                    )
                }
                loadCommunicationsForAffiliate(affiliateId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to send communication: ${e.message}")
                }
            }
        }
    }

    fun markCommunicationRead(communicationId: String) {
        scope.launch {
            val affId = _uiState.value.selectedAffiliate?.affiliateId 
                ?: _uiState.value.affiliatesList.firstOrNull()?.affiliateId 
                ?: return@launch
            try {
                useCases.markAffiliateNotificationRead(principal, affId, communicationId)
                loadCommunicationsForAffiliate(affId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to mark read: ${e.message}")
                }
            }
        }
    }

    fun updateNotificationPreference(
        affiliateId: String,
        communicationType: String,
        inAppEnabled: Boolean = true,
        pushEnabled: Boolean = true,
        emailEnabled: Boolean = false,
        smsEnabled: Boolean = false
    ) {
        scope.launch {
            try {
                val req = com.sucharu.sucharupro.data.api.model.affiliate.UpdateAffiliateNotificationPreferenceRequestDto(
                    communicationType = communicationType,
                    inAppEnabled = inAppEnabled,
                    pushEnabled = pushEnabled,
                    emailEnabled = emailEnabled,
                    smsEnabled = smsEnabled
                )
                useCases.updateAffiliateNotificationPreference(principal, affiliateId, req)
                loadNotificationPreferences(affiliateId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to update preference: ${e.message}")
                }
            }
        }
    }

    fun loadCommunicationsForAffiliate(affiliateId: String) {
        scope.launch {
            try {
                val list = useCases.listAffiliateNotifications(principal, affiliateId)
                val prefs = useCases.getAffiliateNotificationPreferences(principal, affiliateId)
                val unread = useCases.getAffiliateUnreadNotificationCount(principal, affiliateId)
                _uiState.update {
                    it.copy(
                        communicationsList = list,
                        notificationPreferences = prefs,
                        unreadCommunicationCount = unread.totalUnread
                    )
                }
            } catch (e: Exception) { }
        }
    }

    fun loadNotificationPreferences(affiliateId: String) {
        scope.launch {
            try {
                val prefs = useCases.getAffiliateNotificationPreferences(principal, affiliateId)
                _uiState.update { it.copy(notificationPreferences = prefs) }
            } catch (e: Exception) { }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // --- Step 06: Final Governance Integrity & Cross-Module Readiness ---

    /**
     * Loads the Step 06 governance integrity state for the given affiliate.
     *
     * This is READ-ONLY. No mutations are dispatched.
     * Populates:
     *  - lifecycleIntegrityResult
     *  - selectedIntegrationReadiness
     *  - selectedFinalHandoffContract
     *  - auditChainVerificationResult
     */
    fun loadGovernanceIntegrity(affiliateId: String) {
        scope.launch {
            _uiState.update { it.copy(isGovernanceIntegrityLoading = true) }
            try {
                val integrityResult = useCases.getAffiliateLifecycleIntegrityResult(principal, affiliateId)
                val readiness = useCases.getAffiliateIntegrationReadiness(principal, affiliateId)
                val finalHandoff = useCases.getAffiliateFinalGovernanceHandoffContract(principal, affiliateId)
                val chainResult = useCases.verifyAffiliateAuditChain(principal, affiliateId)

                _uiState.update {
                    it.copy(
                        isGovernanceIntegrityLoading = false,
                        lifecycleIntegrityResult = integrityResult,
                        selectedIntegrationReadiness = readiness,
                        selectedFinalHandoffContract = finalHandoff,
                        auditChainVerificationResult = chainResult,
                        selectedTab = AffiliateCommandTab.GOVERNANCE_INTEGRITY
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGovernanceIntegrityLoading = false,
                        errorMessage = "Failed to load governance integrity: ${e.message}"
                    )
                }
            }
        }
    }
}
