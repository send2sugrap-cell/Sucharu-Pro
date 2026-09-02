package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistCategory
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for QC Checklist Templates, Items, Inspection Checklists, and Responses (Module 06 Step 03).
 */
interface QcChecklistDataSource {
    fun observeTemplates(): Flow<List<QcChecklistTemplate>>
    suspend fun fetchTemplateById(templateId: String): DomainResult<QcChecklistTemplate>
    suspend fun insertTemplate(template: QcChecklistTemplate): DomainResult<QcChecklistTemplate>
    suspend fun updateTemplate(template: QcChecklistTemplate): DomainResult<QcChecklistTemplate>

    fun observeCategories(): Flow<List<QcChecklistCategory>>
    suspend fun insertCategory(category: QcChecklistCategory): DomainResult<QcChecklistCategory>

    fun observeItems(): Flow<List<QcChecklistItem>>
    suspend fun insertItem(item: QcChecklistItem): DomainResult<QcChecklistItem>
    suspend fun updateItem(item: QcChecklistItem): DomainResult<QcChecklistItem>

    fun observeInspectionChecklists(): Flow<List<QcInspectionChecklist>>
    suspend fun fetchInspectionChecklistById(checklistId: String): DomainResult<QcInspectionChecklist>
    suspend fun insertInspectionChecklist(checklist: QcInspectionChecklist): DomainResult<QcInspectionChecklist>
    suspend fun updateInspectionChecklist(checklist: QcInspectionChecklist): DomainResult<QcInspectionChecklist>

    fun observeResponses(): Flow<List<QcInspectionResponse>>
    suspend fun insertOrUpdateResponse(response: QcInspectionResponse): DomainResult<QcInspectionResponse>
}
