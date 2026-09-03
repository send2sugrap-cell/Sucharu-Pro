package com.sucharu.sucharupro.ui.features.affiliate

import com.sucharu.sucharupro.data.api.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.Module20Step01AffiliateHandoffContract
import com.sucharu.sucharupro.domain.model.affiliate.Module20Step02ProgramHandoffContract

enum class AffiliateCommandTab(val title: String) {
    OVERVIEW("Overview"),
    DIRECTORY("Affiliates"),
    PROGRAMS("Programs"),
    ENROLLMENTS("Enrollments"),
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
    // Step 01 State
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
    val isPersonalView: Boolean = false,
    // Step 02 State: Programs & Enrollments
    val programSummary: AffiliateProgramGovernanceSummaryDto? = null,
    val programsList: List<AffiliateProgramDto> = emptyList(),
    val selectedProgram: AffiliateProgramDto? = null,
    val selectedProgramAudits: List<AffiliateProgramAuditRecordDto> = emptyList(),
    val enrollmentsList: List<AffiliateEnrollmentDto> = emptyList(),
    val selectedEnrollment: AffiliateEnrollmentDto? = null,
    val selectedEnrollmentAudits: List<AffiliateProgramAuditRecordDto> = emptyList(),
    val selectedProgramHandoffContract: Module20Step02ProgramHandoffContract? = null,
    val programStatusFilter: String? = null,
    val enrollmentStatusFilter: String? = null,
    val programSearchQuery: String = ""
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

    val filteredPrograms: List<AffiliateProgramDto>
        get() = programsList.filter { prog ->
            val matchesQuery = programSearchQuery.isBlank() ||
                    prog.programName.contains(programSearchQuery, ignoreCase = true) ||
                    prog.programCode.contains(programSearchQuery, ignoreCase = true)
            val matchesStatus = programStatusFilter == null || prog.status.equals(programStatusFilter, ignoreCase = true)
            matchesQuery && matchesStatus
        }

    val filteredEnrollments: List<AffiliateEnrollmentDto>
        get() = enrollmentsList.filter { enr ->
            enrollmentStatusFilter == null || enr.enrollmentStatus.equals(enrollmentStatusFilter, ignoreCase = true)
        }

    val pendingCount: Int get() = affiliatesList.count { it.status == "PENDING" }
    val activeCount: Int get() = affiliatesList.count { it.status == "ACTIVE" }
    val suspendedCount: Int get() = affiliatesList.count { it.status == "SUSPENDED" }

    val activeProgramsCount: Int get() = programsList.count { it.status == "ACTIVE" }
    val activeEnrollmentsCount: Int get() = enrollmentsList.count { it.enrollmentStatus == "ACTIVE" }
    val pendingEnrollmentsCount: Int get() = enrollmentsList.count { it.enrollmentStatus == "PENDING" }
}
