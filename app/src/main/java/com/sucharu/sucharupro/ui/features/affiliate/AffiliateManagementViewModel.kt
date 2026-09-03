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
 * ViewModel for Affiliate Management Command Center (Module 20 Steps 01 & 02).
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

                    // Step 02 loads
                    var progSummary: AffiliateProgramGovernanceSummaryDto? = null
                    var progs: List<AffiliateProgramDto> = emptyList()
                    var enrolls: List<AffiliateEnrollmentDto> = emptyList()
                    try {
                        progSummary = useCases.getAffiliateProgramGovernanceSummary(principal)
                        progs = useCases.listAffiliatePrograms(principal)
                        enrolls = useCases.listAffiliateEnrollments(principal)
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
                            selectedEnrollment = firstEnroll
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

    // =========================================================================
    // STEP 02: PROGRAM & ENROLLMENT ACTIONS
    // =========================================================================

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

    fun createProgram(
        programCode: String,
        programName: String,
        description: String?,
        startDate: Long,
        endDate: Long? = null,
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
                val created = useCases.createAffiliateProgram(principal, req)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Program '${created.programName}' (${created.programCode}) created successfully!"
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

    fun activateProgram(programId: String, reason: String = "Program activated") {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val activated = useCases.activateAffiliateProgram(principal, programId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Program '${activated.programCode}' ACTIVATED."
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
                val paused = useCases.pauseAffiliateProgram(principal, programId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Program '${paused.programCode}' PAUSED."
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
                val closed = useCases.closeAffiliateProgram(principal, programId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Program '${closed.programCode}' CLOSED."
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

    fun archiveProgram(programId: String, reason: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val archived = useCases.archiveAffiliateProgram(principal, programId, reason)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Program '${archived.programCode}' ARCHIVED."
                    )
                }
                selectProgram(programId)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to archive program: ${e.message}")
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
                val handoff = try {
                    useCases.getAffiliateProgramHandoffContract(principal, enrollmentId)
                } catch (_: Exception) { null }

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

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
