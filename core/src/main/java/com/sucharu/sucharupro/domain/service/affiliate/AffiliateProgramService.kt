package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateActorType
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollment
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollmentStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgram
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditRecord
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramGovernanceSummary
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramStatus
import com.sucharu.sucharupro.domain.model.affiliate.Module20Step02ProgramHandoffContract

/**
 * Service Contract for Affiliate Programs and Enrollment Governance.
 */
interface AffiliateProgramService {
    suspend fun createProgram(
        tenantId: String,
        programCode: String,
        programName: String,
        description: String?,
        startDate: Long,
        endDate: Long?,
        eligibilityPolicy: String,
        termsReference: String?,
        termsVersion: String?,
        maxParticipants: Int?,
        actorId: String,
        actorRole: String,
        metadataJson: String? = null
    ): AffiliateProgram

    suspend fun getProgramById(tenantId: String, programId: String): AffiliateProgram?
    suspend fun getProgramByCode(tenantId: String, programCode: String): AffiliateProgram?
    suspend fun listPrograms(tenantId: String, status: AffiliateProgramStatus? = null): List<AffiliateProgram>

    suspend fun updateProgram(
        tenantId: String,
        programId: String,
        programName: String?,
        description: String?,
        startDate: Long?,
        endDate: Long?,
        eligibilityPolicy: String?,
        termsReference: String?,
        termsVersion: String?,
        maxParticipants: Int?,
        actorId: String,
        actorRole: String,
        metadataJson: String? = null
    ): AffiliateProgram

    suspend fun activateProgram(tenantId: String, programId: String, actorId: String, actorRole: String, reason: String): AffiliateProgram
    suspend fun pauseProgram(tenantId: String, programId: String, actorId: String, actorRole: String, reason: String): AffiliateProgram
    suspend fun closeProgram(tenantId: String, programId: String, actorId: String, actorRole: String, reason: String): AffiliateProgram
    suspend fun archiveProgram(tenantId: String, programId: String, actorId: String, actorRole: String, reason: String): AffiliateProgram

    suspend fun enrollAffiliate(
        tenantId: String,
        affiliateId: String,
        programId: String,
        enrollmentReason: String?,
        effectiveFrom: Long?,
        effectiveTo: Long?,
        actorId: String,
        actorRole: String,
        metadataJson: String? = null
    ): AffiliateEnrollment

    suspend fun getEnrollmentById(tenantId: String, enrollmentId: String): AffiliateEnrollment?
    suspend fun listEnrollments(tenantId: String, status: AffiliateEnrollmentStatus? = null): List<AffiliateEnrollment>
    suspend fun findEnrollmentsByAffiliate(tenantId: String, affiliateId: String): List<AffiliateEnrollment>
    suspend fun findEnrollmentsByProgram(tenantId: String, programId: String, status: AffiliateEnrollmentStatus? = null): List<AffiliateEnrollment>

    suspend fun approveEnrollment(tenantId: String, enrollmentId: String, actorId: String, actorRole: String, reason: String): AffiliateEnrollment
    suspend fun rejectEnrollment(tenantId: String, enrollmentId: String, actorId: String, actorRole: String, reason: String): AffiliateEnrollment
    suspend fun activateEnrollment(tenantId: String, enrollmentId: String, actorId: String, actorRole: String, reason: String): AffiliateEnrollment
    suspend fun suspendEnrollment(tenantId: String, enrollmentId: String, actorId: String, actorRole: String, reason: String): AffiliateEnrollment
    suspend fun resumeEnrollment(tenantId: String, enrollmentId: String, actorId: String, actorRole: String, reason: String): AffiliateEnrollment
    suspend fun terminateEnrollment(tenantId: String, enrollmentId: String, actorId: String, actorRole: String, reason: String): AffiliateEnrollment

    suspend fun listProgramAuditRecords(tenantId: String, programId: String): List<AffiliateProgramAuditRecord>
    suspend fun listEnrollmentAuditRecords(tenantId: String, enrollmentId: String): List<AffiliateProgramAuditRecord>
    suspend fun getGovernanceSummary(tenantId: String): AffiliateProgramGovernanceSummary
    suspend fun getHandoffContract(tenantId: String, enrollmentId: String): Module20Step02ProgramHandoffContract
}
