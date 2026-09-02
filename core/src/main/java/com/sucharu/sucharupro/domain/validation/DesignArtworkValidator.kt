package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ArtworkMetadata
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Authoritative domain validator for Artwork & File Management (Module 05 Step 02).
 */
object DesignArtworkValidator {

    /** Roles authorized to create and manage artwork assets. */
    val AUTHORIZED_ARTWORK_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.DESIGNER)

    /**
     * Validates whether a caller with [callerRole] is authorized to manage artwork assets.
     */
    fun validateArtworkPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_ARTWORK_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to manage artwork files."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates eligibility to create a new [DesignArtwork] within a [DesignProject].
     */
    fun validateArtworkCreation(
        project: DesignProject,
        name: String,
        existingArtworks: List<DesignArtwork>,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        // 1. RBAC check
        val rbacResult = validateArtworkPermission(callerRole)
        if (rbacResult !is DomainResult.Success) {
            return rbacResult
        }

        // 2. Project terminal/cancellation check
        if (project.status == DesignStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cannot create artwork for a cancelled Design Project '${project.projectNumber}'."
            )
        }

        // 3. Name check
        if (name.isBlank()) {
            return DomainResult.Error(message = "Artwork name cannot be blank.")
        }

        // 4. Duplicate name within the same project check
        val hasDuplicate = existingArtworks.any {
            it.projectId == project.projectId &&
                    it.name.equals(name.trim(), ignoreCase = true) &&
                    it.isActive
        }
        if (hasDuplicate) {
            return DomainResult.Error(
                message = "Active artwork with name '$name' already exists in Design Project '${project.projectNumber}'."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates eligibility to create a new [DesignArtworkVersion].
     */
    fun validateVersionCreation(
        artwork: DesignArtwork,
        project: DesignProject,
        versionNumber: Int,
        fileReference: FileReference,
        metadata: ArtworkMetadata,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        // 1. RBAC check
        val rbacResult = validateArtworkPermission(callerRole)
        if (rbacResult !is DomainResult.Success) {
            return rbacResult
        }

        // 2. Project terminal check
        if (project.status == DesignStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cannot create artwork version for a cancelled Design Project '${project.projectNumber}'."
            )
        }

        // 3. Artwork status check
        if (artwork.isArchived) {
            return DomainResult.Error(
                message = "Cannot create version for an archived artwork '${artwork.name}'."
            )
        }

        // 4. Version number validation
        if (versionNumber < 1) {
            return DomainResult.Error(message = "Version number must be at least 1.")
        }

        // 5. Immutability / duplicate version number check
        val duplicateVersion = artwork.versions.find { it.versionNumber == versionNumber }
        if (duplicateVersion != null) {
            return DomainResult.Error(
                message = "Version V$versionNumber already exists for artwork '${artwork.name}'. Historical versions are immutable."
            )
        }

        // 6. File reference integrity
        val fileValidation = validateFileReference(fileReference)
        if (fileValidation !is DomainResult.Success) {
            return fileValidation
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates file reference domain constraints.
     */
    fun validateFileReference(fileReference: FileReference): DomainResult<Unit> {
        if (fileReference.fileId.isBlank()) {
            return DomainResult.Error(message = "File ID cannot be blank.")
        }
        if (fileReference.fileName.isBlank()) {
            return DomainResult.Error(message = "File Name cannot be blank.")
        }
        if (fileReference.storagePath.isBlank()) {
            return DomainResult.Error(message = "File storage path cannot be blank.")
        }
        if (fileReference.fileSize <= 0) {
            return DomainResult.Error(message = "File size must be greater than zero bytes.")
        }
        if (fileReference.mimeType.isBlank()) {
            return DomainResult.Error(message = "MIME type cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates archival of an artwork.
     */
    fun validateArtworkArchival(
        artwork: DesignArtwork,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateArtworkPermission(callerRole)
        if (rbacResult !is DomainResult.Success) {
            return rbacResult
        }

        if (artwork.isArchived) {
            return DomainResult.Error(message = "Artwork '${artwork.name}' is already archived.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates archival of an individual artwork version.
     */
    fun validateVersionArchival(
        version: DesignArtworkVersion,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateArtworkPermission(callerRole)
        if (rbacResult !is DomainResult.Success) {
            return rbacResult
        }

        if (version.isArchived) {
            return DomainResult.Error(message = "Version ${version.versionTag} is already archived.")
        }

        return DomainResult.Success(Unit)
    }
}
