package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistCategory
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory implementation of [QcChecklistDataSource] with Coroutine [Mutex] synchronization.
 */
class FakeQcChecklistDataSource(
    initialTemplates: List<QcChecklistTemplate> = emptyList(),
    initialCategories: List<QcChecklistCategory> = emptyList(),
    initialItems: List<QcChecklistItem> = emptyList(),
    initialChecklists: List<QcInspectionChecklist> = emptyList(),
    initialResponses: List<QcInspectionResponse> = emptyList()
) : QcChecklistDataSource {

    private val mutex = Mutex()
    private val _templates = MutableStateFlow<List<QcChecklistTemplate>>(initialTemplates)
    private val _categories = MutableStateFlow<List<QcChecklistCategory>>(initialCategories)
    private val _items = MutableStateFlow<List<QcChecklistItem>>(initialItems)
    private val _checklists = MutableStateFlow<List<QcInspectionChecklist>>(initialChecklists)
    private val _responses = MutableStateFlow<List<QcInspectionResponse>>(initialResponses)

    override fun observeTemplates(): Flow<List<QcChecklistTemplate>> = _templates.asStateFlow()

    override suspend fun fetchTemplateById(templateId: String): DomainResult<QcChecklistTemplate> = mutex.withLock {
        val template = _templates.value.find { it.checklistTemplateId == templateId }
        return if (template != null) {
            DomainResult.Success(template)
        } else {
            DomainResult.Error(message = "QC Checklist Template not found with ID: $templateId")
        }
    }

    override suspend fun insertTemplate(template: QcChecklistTemplate): DomainResult<QcChecklistTemplate> = mutex.withLock {
        if (_templates.value.any { it.checklistTemplateId == template.checklistTemplateId }) {
            return DomainResult.Error(message = "Template with ID '${template.checklistTemplateId}' already exists.")
        }
        _templates.value = _templates.value + template
        DomainResult.Success(template)
    }

    override suspend fun updateTemplate(template: QcChecklistTemplate): DomainResult<QcChecklistTemplate> = mutex.withLock {
        val index = _templates.value.indexOfFirst { it.checklistTemplateId == template.checklistTemplateId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent template: ${template.checklistTemplateId}")
        }
        val current = _templates.value.toMutableList()
        current[index] = template
        _templates.value = current.toList()
        DomainResult.Success(template)
    }

    override fun observeCategories(): Flow<List<QcChecklistCategory>> = _categories.asStateFlow()

    override suspend fun insertCategory(category: QcChecklistCategory): DomainResult<QcChecklistCategory> = mutex.withLock {
        if (_categories.value.any { it.categoryId == category.categoryId }) {
            return DomainResult.Error(message = "Category with ID '${category.categoryId}' already exists.")
        }
        _categories.value = _categories.value + category
        DomainResult.Success(category)
    }

    override fun observeItems(): Flow<List<QcChecklistItem>> = _items.asStateFlow()

    override suspend fun insertItem(item: QcChecklistItem): DomainResult<QcChecklistItem> = mutex.withLock {
        if (_items.value.any { it.itemId == item.itemId }) {
            return DomainResult.Error(message = "Item with ID '${item.itemId}' already exists.")
        }
        _items.value = _items.value + item
        DomainResult.Success(item)
    }

    override suspend fun updateItem(item: QcChecklistItem): DomainResult<QcChecklistItem> = mutex.withLock {
        val index = _items.value.indexOfFirst { it.itemId == item.itemId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent item: ${item.itemId}")
        }
        val current = _items.value.toMutableList()
        current[index] = item
        _items.value = current.toList()
        DomainResult.Success(item)
    }

    override fun observeInspectionChecklists(): Flow<List<QcInspectionChecklist>> = _checklists.asStateFlow()

    override suspend fun fetchInspectionChecklistById(checklistId: String): DomainResult<QcInspectionChecklist> = mutex.withLock {
        val checklist = _checklists.value.find { it.inspectionChecklistId == checklistId }
        return if (checklist != null) {
            DomainResult.Success(checklist)
        } else {
            DomainResult.Error(message = "QC Inspection Checklist not found with ID: $checklistId")
        }
    }

    override suspend fun insertInspectionChecklist(checklist: QcInspectionChecklist): DomainResult<QcInspectionChecklist> = mutex.withLock {
        if (_checklists.value.any { it.inspectionChecklistId == checklist.inspectionChecklistId }) {
            return DomainResult.Error(message = "Checklist with ID '${checklist.inspectionChecklistId}' already exists.")
        }
        _checklists.value = _checklists.value + checklist
        DomainResult.Success(checklist)
    }

    override suspend fun updateInspectionChecklist(checklist: QcInspectionChecklist): DomainResult<QcInspectionChecklist> = mutex.withLock {
        val index = _checklists.value.indexOfFirst { it.inspectionChecklistId == checklist.inspectionChecklistId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent checklist: ${checklist.inspectionChecklistId}")
        }
        val current = _checklists.value.toMutableList()
        current[index] = checklist
        _checklists.value = current.toList()
        DomainResult.Success(checklist)
    }

    override fun observeResponses(): Flow<List<QcInspectionResponse>> = _responses.asStateFlow()

    override suspend fun insertOrUpdateResponse(response: QcInspectionResponse): DomainResult<QcInspectionResponse> = mutex.withLock {
        val index = _responses.value.indexOfFirst { it.inspectionId == response.inspectionId && it.checklistItemId == response.checklistItemId }
        val current = _responses.value.toMutableList()
        if (index != -1) {
            current[index] = response
        } else {
            current.add(response)
        }
        _responses.value = current.toList()
        DomainResult.Success(response)
    }
}
