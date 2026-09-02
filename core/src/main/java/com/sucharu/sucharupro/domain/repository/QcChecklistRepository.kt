package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistCategory
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItemType
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for QC Checklist Templates, Items, Inspection Checklists, and Responses (Module 06 Step 03).
 */
interface QcChecklistRepository {

    // ==========================================
    // Template & Item Operations
    // ==========================================

    fun observeTemplates(): Flow<List<QcChecklistTemplate>>
    fun getTemplateById(templateId: String): Flow<QcChecklistTemplate?>
    suspend fun findTemplateById(templateId: String): DomainResult<QcChecklistTemplate>

    suspend fun createTemplate(
        name: String,
        description: String? = null,
        qcType: QcType,
        applicableStageType: String? = null,
        createdBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcChecklistTemplate>

    suspend fun createTemplateVersion(
        templateId: String,
        createdBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcChecklistTemplate>

    suspend fun activateTemplate(templateId: String, callerRole: UserRole? = null): DomainResult<QcChecklistTemplate>
    suspend fun deactivateTemplate(templateId: String, callerRole: UserRole? = null): DomainResult<QcChecklistTemplate>

    fun observeCategories(templateId: String): Flow<List<QcChecklistCategory>>
    suspend fun addCategory(templateId: String, name: String, sequence: Int, callerRole: UserRole? = null): DomainResult<QcChecklistCategory>

    fun observeItems(templateId: String): Flow<List<QcChecklistItem>>
    suspend fun addItem(
        templateId: String,
        categoryId: String,
        title: String,
        description: String? = null,
        sequence: Int = 1,
        code: String? = null,
        itemType: QcChecklistItemType = QcChecklistItemType.PASS_FAIL,
        isRequired: Boolean = true,
        expectedValue: String? = null,
        tolerance: String? = null,
        unit: String? = null,
        instructions: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcChecklistItem>

    // ==========================================
    // Inspection Checklist Execution Operations
    // ==========================================

    suspend fun createInspectionChecklist(
        inspectionId: String,
        templateId: String,
        productionJobId: String,
        productionQcId: String,
        productionStageId: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcInspectionChecklist>

    fun observeInspectionChecklist(inspectionChecklistId: String): Flow<QcInspectionChecklist?>
    fun getInspectionChecklist(inspectionChecklistId: String): Flow<QcInspectionChecklist?> = observeInspectionChecklist(inspectionChecklistId)
    fun getInspectionChecklistForInspection(inspectionId: String): Flow<QcInspectionChecklist?>

    suspend fun startChecklist(
        inspectionChecklistId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcInspectionChecklist>

    fun observeResponses(inspectionId: String): Flow<List<QcInspectionResponse>>

    suspend fun saveResponse(
        inspectionId: String,
        checklistItemId: String,
        status: QcResponseStatus,
        value: String? = null,
        numericValue: Double? = null,
        selectedValue: String? = null,
        remarks: String? = null,
        respondedBy: String,
        respondedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcInspectionResponse>

    suspend fun completeInspectionChecklist(
        inspectionChecklistId: String,
        decision: QcDecision,
        completedBy: String,
        completedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcInspectionChecklist>

    suspend fun cancelChecklist(
        inspectionChecklistId: String,
        reason: String,
        cancelledBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<QcInspectionChecklist>
}
