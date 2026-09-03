package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateActorType
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEligibility
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollment
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollmentStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProfile
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgram
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditEventType
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditRecord
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramEntityCategory
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateStatus
import com.sucharu.sucharupro.domain.model.affiliate.Module20Step02ProgramHandoffContract
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/**
 * Validation and Cryptographic Hashing Engine for Affiliate Programs and Enrollments (Module 20 Step 02).
 */
object AffiliateProgramValidationEngine {

    private val PROGRAM_CODE_REGEX = Regex("^[A-Z0-9_-]{3,32}$")
    private const val GENESIS_AUDIT_HASH = "GENESIS_AFFILIATE_PROGRAM_AUDIT_BLOCK"

    /**
     * Validates and normalizes program code slug.
     */
    fun validateProgramCode(rawCode: String): Result<String> {
        val trimmed = rawCode.trim().uppercase()
        return if (trimmed.matches(PROGRAM_CODE_REGEX)) {
            Result.success(trimmed)
        } else {
            Result.failure(
                IllegalArgumentException(
                    "Invalid program code '$rawCode'. Must be 3-32 alphanumeric characters, uppercase, hyphens or underscores."
                )
            )
        }
    }

    /**
     * Validates program creation attributes.
     */
    fun validateProgramCreation(
        programCode: String,
        programName: String,
        startDate: Long,
        endDate: Long?
    ): Result<Unit> {
        if (programName.isBlank() || programName.length < 3 || programName.length > 255) {
            return Result.failure(IllegalArgumentException("Program name must be between 3 and 255 characters."))
        }
        val codeResult = validateProgramCode(programCode)
        if (codeResult.isFailure) {
            return Result.failure(codeResult.exceptionOrNull()!!)
        }
        if (endDate != null && endDate < startDate) {
            return Result.failure(IllegalArgumentException("Program end date ($endDate) cannot be before start date ($startDate)."))
        }
        return Result.success(Unit)
    }

    /**
     * Validates deterministic Program status transitions.
     */
    fun validateProgramStatusTransition(
        currentStatus: AffiliateProgramStatus,
        newStatus: AffiliateProgramStatus
    ): Result<Unit> {
        if (currentStatus == newStatus) {
            return Result.success(Unit) // Idempotent
        }

        val isValid = when (currentStatus) {
            AffiliateProgramStatus.DRAFT -> newStatus == AffiliateProgramStatus.ACTIVE || newStatus == AffiliateProgramStatus.CLOSED
            AffiliateProgramStatus.ACTIVE -> newStatus == AffiliateProgramStatus.PAUSED || newStatus == AffiliateProgramStatus.CLOSED
            AffiliateProgramStatus.PAUSED -> newStatus == AffiliateProgramStatus.ACTIVE || newStatus == AffiliateProgramStatus.CLOSED
            AffiliateProgramStatus.CLOSED -> newStatus == AffiliateProgramStatus.ARCHIVED
            AffiliateProgramStatus.ARCHIVED -> false
        }

        return if (isValid) {
            Result.success(Unit)
        } else {
            Result.failure(
                IllegalStateException("Illegal Affiliate Program transition from '$currentStatus' to '$newStatus'.")
            )
        }
    }

    /**
     * Validates deterministic Enrollment status transitions.
     */
    fun validateEnrollmentStatusTransition(
        currentStatus: AffiliateEnrollmentStatus,
        newStatus: AffiliateEnrollmentStatus
    ): Result<Unit> {
        if (currentStatus == newStatus) {
            return Result.success(Unit) // Idempotent
        }

        val isValid = when (currentStatus) {
            AffiliateEnrollmentStatus.PENDING ->
                newStatus == AffiliateEnrollmentStatus.APPROVED || newStatus == AffiliateEnrollmentStatus.REJECTED
            AffiliateEnrollmentStatus.APPROVED ->
                newStatus == AffiliateEnrollmentStatus.ACTIVE || newStatus == AffiliateEnrollmentStatus.REJECTED
            AffiliateEnrollmentStatus.ACTIVE ->
                newStatus == AffiliateEnrollmentStatus.SUSPENDED ||
                newStatus == AffiliateEnrollmentStatus.TERMINATED ||
                newStatus == AffiliateEnrollmentStatus.EXPIRED
            AffiliateEnrollmentStatus.SUSPENDED ->
                newStatus == AffiliateEnrollmentStatus.ACTIVE ||
                newStatus == AffiliateEnrollmentStatus.TERMINATED
            AffiliateEnrollmentStatus.TERMINATED -> false
            AffiliateEnrollmentStatus.EXPIRED -> false
            AffiliateEnrollmentStatus.REJECTED -> false
        }

        return if (isValid) {
            Result.success(Unit)
        } else {
            Result.failure(
                IllegalStateException("Illegal Affiliate Enrollment transition from '$currentStatus' to '$newStatus'.")
            )
        }
    }

    /**
     * Validates program availability and affiliate eligibility for enrollment.
     */
    fun validateEnrollmentEligibility(
        affiliate: AffiliateProfile,
        affiliateEligibility: AffiliateEligibility?,
        program: AffiliateProgram,
        existingEnrollments: List<AffiliateEnrollment>
    ): Result<Unit> {
        // 1. Cross-tenant isolation check
        if (affiliate.tenantId != program.tenantId) {
            return Result.failure(
                IllegalStateException("Tenant mismatch between Affiliate (${affiliate.tenantId}) and Program (${program.tenantId}).")
            )
        }

        // 2. Program availability check
        if (!program.isActive) {
            return Result.failure(
                IllegalStateException("Program '${program.programCode}' is not ACTIVE (current status: ${program.status}).")
            )
        }

        // 3. Program date range check
        val now = System.currentTimeMillis()
        if (now < program.startDate) {
            return Result.failure(
                IllegalStateException("Program '${program.programCode}' has not started yet (starts at ${program.startDate}).")
            )
        }
        if (program.endDate != null && now > program.endDate) {
            return Result.failure(
                IllegalStateException("Program '${program.programCode}' has already ended at ${program.endDate}.")
            )
        }

        // 4. Program capacity check
        if (program.maxParticipants != null && program.maxParticipants > 0) {
            val activeCount = existingEnrollments.count { it.isActive && it.programId == program.programId }
            if (activeCount >= program.maxParticipants) {
                return Result.failure(
                    IllegalStateException("Program '${program.programCode}' has reached maximum participant capacity (${program.maxParticipants}).")
                )
            }
        }

        // 5. Affiliate status check
        if (affiliate.status != AffiliateStatus.ACTIVE) {
            return Result.failure(
                IllegalStateException("Affiliate '${affiliate.affiliateCode}' is not ACTIVE (current status: ${affiliate.status}).")
            )
        }

        // 6. Affiliate eligibility verification check
        if (affiliateEligibility != null && !affiliateEligibility.isEligible) {
            val reasons = affiliateEligibility.rejectionReasons.joinToString(", ")
            return Result.failure(
                IllegalStateException("Affiliate '${affiliate.affiliateCode}' is not eligible: $reasons.")
            )
        }

        // 7. Duplicate active/pending enrollment check
        val duplicate = existingEnrollments.any {
            it.affiliateId == affiliate.affiliateId &&
            it.programId == program.programId &&
            (it.isActive || it.isPending || it.isApproved)
        }
        if (duplicate) {
            return Result.failure(
                IllegalStateException("Affiliate '${affiliate.affiliateCode}' already has an active or pending enrollment in program '${program.programCode}'.")
            )
        }

        return Result.success(Unit)
    }

    /**
     * Computes SHA-256 record hash for Program/Enrollment audit entry.
     */
    fun computeRecordHash(
        tenantId: String,
        entityType: AffiliateProgramEntityCategory,
        entityId: String,
        eventType: AffiliateProgramAuditEventType,
        previousStatus: String?,
        newStatus: String,
        actorType: AffiliateActorType,
        actorId: String,
        actorRole: String,
        timestamp: Long,
        correlationId: String,
        reason: String
    ): String {
        val payload = "$tenantId|${entityType.name}|$entityId|${eventType.name}|${previousStatus ?: ""}|$newStatus|${actorType.name}|$actorId|$actorRole|$timestamp|$correlationId|$reason"
        return sha256Hex(payload)
    }

    /**
     * Computes SHA-256 chain hash linking previous audit hash to current record hash.
     */
    fun computeChainHash(previousAuditHash: String?, recordHash: String): String {
        val prev = previousAuditHash ?: GENESIS_AUDIT_HASH
        return sha256Hex("$prev|$recordHash")
    }

    /**
     * Builds and signs a Module20Step02ProgramHandoffContract.
     */
    fun buildHandoffContract(
        tenantId: String,
        enrollment: AffiliateEnrollment,
        affiliate: AffiliateProfile,
        program: AffiliateProgram
    ): Module20Step02ProgramHandoffContract {
        val isEligibleForCommission = enrollment.isActive && program.isActive && affiliate.isActive
        val isEligibleForAttribution = enrollment.isActive && program.isActive && affiliate.isActive

        val rawContent = "$tenantId|${enrollment.enrollmentId}|${affiliate.affiliateId}|${affiliate.affiliateCode}|${program.programId}|${program.programCode}|${program.status}|${enrollment.enrollmentStatus}|${enrollment.effectiveFrom}|${enrollment.effectiveTo}|$isEligibleForCommission|$isEligibleForAttribution"
        val integritySeal = sha256Hex(rawContent)

        return Module20Step02ProgramHandoffContract(
            tenantId = tenantId,
            enrollmentId = enrollment.enrollmentId,
            affiliateId = affiliate.affiliateId,
            affiliateCode = affiliate.affiliateCode,
            programId = program.programId,
            programCode = program.programCode,
            programName = program.programName,
            programStatus = program.status,
            enrollmentStatus = enrollment.enrollmentStatus,
            effectiveFrom = enrollment.effectiveFrom,
            effectiveTo = enrollment.effectiveTo,
            isEligibleForCommission = isEligibleForCommission,
            isEligibleForAttribution = isEligibleForAttribution,
            integritySealHash = integritySeal
        )
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
