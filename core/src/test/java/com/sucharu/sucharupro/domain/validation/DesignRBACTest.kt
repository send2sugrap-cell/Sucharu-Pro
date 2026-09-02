package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC permission tests for Design Domain operations (Module 05 Step 01).
 */
class DesignRBACTest {

    private val sampleProject = DesignProject(
        projectId = "des-01",
        projectNumber = "DES-2026-0001",
        productionJobId = "job-01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-01",
        title = "বই কভার ডিজাইন",
        status = DesignStatus.NOT_STARTED,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Test
    fun adminAndManager_authorizedToAssignDesigners() {
        val adminResult = DesignAssignmentValidator.validateAssignment(
            project = sampleProject,
            designerId = "des-01",
            designerName = "তানভীর",
            callerRole = UserRole.ADMIN
        )
        assertTrue(adminResult is DomainResult.Success)

        val managerResult = DesignAssignmentValidator.validateAssignment(
            project = sampleProject,
            designerId = "des-01",
            designerName = "তানভীর",
            callerRole = UserRole.MANAGER
        )
        assertTrue(managerResult is DomainResult.Success)
    }

    @Test
    fun externalAndUnauthorizedRoles_cannotAssignDesigners() {
        val unauthorizedRoles = listOf(
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE,
            UserRole.STAFF,
            UserRole.DESIGNER
        )

        unauthorizedRoles.forEach { role ->
            val result = DesignAssignmentValidator.validateAssignment(
                project = sampleProject,
                designerId = "des-01",
                designerName = "তানভীর",
                callerRole = role
            )
            assertTrue("Role ${role.name} should not be authorized to assign designers", result is DomainResult.Error)
            val error = result as DomainResult.Error
            assertTrue(error.message.contains("is not authorized to manage designer assignments"))
        }
    }
}
