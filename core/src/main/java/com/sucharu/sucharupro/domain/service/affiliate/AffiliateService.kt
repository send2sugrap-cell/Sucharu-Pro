package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Commands for Affiliate Service Operations.
 */
data class CreateAffiliateCommand(
    val userId: String,
    val customerId: String? = null,
    val displayName: String,
    val affiliateCode: String? = null,
    val affiliateType: AffiliateType = AffiliateType.INDIVIDUAL,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val taxIdOrGst: String? = null,
    val agreementReference: String? = null,
    val agreementVersion: String? = null,
    val metadataJson: String? = null
)

data class UpdateAffiliateProfileCommand(
    val displayName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val taxIdOrGst: String? = null,
    val affiliateType: AffiliateType? = null,
    val verificationState: VerificationState? = null,
    val metadataJson: String? = null
)

/**
 * Authoritative Service Interface for Affiliate Management Foundation (Module 20 Step 01).
 */
interface AffiliateService {

    suspend fun createAffiliate(
        tenantId: String,
        command: CreateAffiliateCommand,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun getAffiliateById(
        tenantId: String,
        affiliateId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun getAffiliateByCode(
        tenantId: String,
        affiliateCode: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun getMyAffiliateProfile(
        tenantId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun updateAffiliateProfile(
        tenantId: String,
        affiliateId: String,
        command: UpdateAffiliateProfileCommand,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun activateAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun suspendAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun reactivateAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun rejectAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun terminateAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun acceptAgreement(
        tenantId: String,
        affiliateId: String,
        agreementReference: String,
        agreementVersion: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile

    suspend fun evaluateEligibility(
        tenantId: String,
        affiliateId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateEligibility

    suspend fun listAffiliates(
        tenantId: String,
        status: AffiliateStatus? = null,
        affiliateType: AffiliateType? = null,
        actorPrincipal: AuthenticatedPrincipal
    ): List<AffiliateProfile>

    suspend fun listAuditRecords(
        tenantId: String,
        affiliateId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): List<AffiliateAuditRecord>

    suspend fun getGovernanceSummary(
        tenantId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateGovernanceSummary

    suspend fun getHandoffContract(
        tenantId: String,
        affiliateId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): Module20Step01AffiliateHandoffContract
}
