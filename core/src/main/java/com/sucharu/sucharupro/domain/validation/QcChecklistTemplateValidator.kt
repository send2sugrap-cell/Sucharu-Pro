package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Validator for [QcChecklistTemplate] definitions and management RBAC (Module 06 Step 03).
 */
object QcChecklistTemplateValidator {

    val AUTHORIZED_TEMPLATE_MANAGERS = setOf(UserRole.ADMIN, UserRole.MANAGER)

    /**
     * Validates whether caller is authorized to create/manage checklist templates.
     */
    fun validateTemplateManagementPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_TEMPLATE_MANAGERS) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to manage QC checklist templates."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates template creation parameters prior to entity instantiation.
     */
    fun validateCreationParams(
        name: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateTemplateManagementPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (name.isBlank()) {
            return DomainResult.Error(message = "Checklist Template name cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates template fields and constraints.
     */
    fun validateTemplate(template: QcChecklistTemplate): DomainResult<Unit> {
        if (template.checklistTemplateId.isBlank()) {
            return DomainResult.Error(message = "Checklist Template ID cannot be blank.")
        }
        if (template.name.isBlank()) {
            return DomainResult.Error(message = "Checklist Template name cannot be blank.")
        }
        if (template.version < 1) {
            return DomainResult.Error(message = "Checklist Template version must be at least 1.")
        }
        if (template.createdAt.isBlank()) {
            return DomainResult.Error(message = "Creation timestamp cannot be blank.")
        }
        if (template.updatedAt.isBlank()) {
            return DomainResult.Error(message = "Update timestamp cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }
}
