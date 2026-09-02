package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Centralized RBAC validator for all Communication Analytics (Step 09) and
 * Communication Intelligence (Step 10) operations.
 *
 * Access boundary:
 * - Analytics:    ADMIN, MANAGER, STAFF, ACCOUNTS
 * - Governance:   ADMIN, MANAGER
 * - Verification: ADMIN, MANAGER
 * - Export:       ADMIN, MANAGER
 * - Audit:        ADMIN, MANAGER
 * - External roles (CUSTOMER, VENDOR, AFFILIATE): always denied.
 */
object CommunicationAnalyticsAuthorizationValidator {

    fun validateAnalyticsAccess(actorRole: UserRole): DomainResult<Unit> =
        when (actorRole) {
            UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.ACCOUNTS -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "External roles are strictly prohibited from viewing organization-wide communication analytics."
            )
        }

    fun validateGovernanceAccess(actorRole: UserRole): DomainResult<Unit> =
        when (actorRole) {
            UserRole.ADMIN, UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Governance and risk analytics require MANAGER or ADMIN privileges."
            )
        }

    fun validateSnapshotVerificationAccess(actorRole: UserRole): DomainResult<Unit> =
        when (actorRole) {
            UserRole.ADMIN, UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Snapshot integrity verification requires MANAGER or ADMIN privileges."
            )
        }

    fun validateExportAccess(actorRole: UserRole): DomainResult<Unit> =
        when (actorRole) {
            UserRole.ADMIN, UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Analytics export requires MANAGER or ADMIN privileges."
            )
        }

    fun validateAuditAccess(actorRole: UserRole): DomainResult<Unit> =
        when (actorRole) {
            UserRole.ADMIN, UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Audit trail access requires MANAGER or ADMIN privileges."
            )
        }
}

