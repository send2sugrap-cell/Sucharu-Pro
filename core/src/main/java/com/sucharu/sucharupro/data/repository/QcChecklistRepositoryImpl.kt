package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.QcChecklistDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcActivityType
import com.sucharu.sucharupro.domain.model.qc.QcChecklistCategory
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItemType
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.QcChecklistRepository
import com.sucharu.sucharupro.domain.validation.QcChecklistItemValidator
import com.sucharu.sucharupro.domain.validation.QcChecklistTemplateValidator
import com.sucharu.sucharupro.domain.validation.QcInspectionChecklistValidator
import com.sucharu.sucharupro.domain.validation.QcInspectionResponseValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [QcChecklistRepository] (Module 06 Step 03).
 */
class QcChecklistRepositoryImpl(
    private val checklistDataSource: QcChecklistDataSource,
    private val qcDataSource: ProductionQcDataSource? = null
) : QcChecklistRepository {

    private val repositoryMutex = Mutex()

    private suspend fun recordActivity(
        qcId: String,
        productionJobId: String,
        actorId: String? = null,
        actorName: String? = null,
        activityType: QcActivityType,
        notes: String? = null,
        timestamp: String
    ) {
        if (qcDataSource == null) return
        val event = QcActivityEvent(
            eventId = "act-qc-" + UUID.randomUUID().toString(),
            qcId = qcId,
            productionJobId = productionJobId,
            actorId = actorId,
            actorName = actorName,
            activityType = activityType,
            timestamp = timestamp,
            notes = notes
        )
        qcDataSource.insertActivityEvent(event)
    }

    override fun observeTemplates(): Flow<List<QcChecklistTemplate>> = checklistDataSource.observeTemplates()

    override fun getTemplateById(templateId: String): Flow<QcChecklistTemplate?> {
        return checklistDataSource.observeTemplates().map { list ->
            list.find { it.checklistTemplateId == templateId }
        }
    }

    override suspend fun findTemplateById(templateId: String): DomainResult<QcChecklistTemplate> {
        return checklistDataSource.fetchTemplateById(templateId)
    }

    override suspend fun createTemplate(
        name: String,
        description: String?,
        qcType: QcType,
        applicableStageType: String?,
        createdBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcChecklistTemplate> = repositoryMutex.withLock {
        val paramsValidation = QcChecklistTemplateValidator.validateCreationParams(name, callerRole)
        if (paramsValidation is DomainResult.Error) return paramsValidation

        val templateId = "tmpl-qc-" + UUID.randomUUID().toString()
        val template = QcChecklistTemplate(
            checklistTemplateId = templateId,
            name = name,
            description = description,
            qcType = qcType,
            applicableStageType = applicableStageType,
            version = 1,
            isActive = true,
            createdBy = createdBy,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val validation = QcChecklistTemplateValidator.validateTemplate(template)
        if (validation is DomainResult.Error) return validation

        return checklistDataSource.insertTemplate(template)
    }

    override suspend fun createTemplateVersion(
        templateId: String,
        createdBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcChecklistTemplate> = repositoryMutex.withLock {
        val rbac = QcChecklistTemplateValidator.validateTemplateManagementPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = checklistDataSource.fetchTemplateById(templateId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val updated = current.copy(
            version = current.version + 1,
            updatedAt = timestamp
        )

        return checklistDataSource.updateTemplate(updated)
    }

    override suspend fun activateTemplate(templateId: String, callerRole: UserRole?): DomainResult<QcChecklistTemplate> = repositoryMutex.withLock {
        val rbac = QcChecklistTemplateValidator.validateTemplateManagementPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = checklistDataSource.fetchTemplateById(templateId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val updated = current.copy(isActive = true)
        return checklistDataSource.updateTemplate(updated)
    }

    override suspend fun deactivateTemplate(templateId: String, callerRole: UserRole?): DomainResult<QcChecklistTemplate> = repositoryMutex.withLock {
        val rbac = QcChecklistTemplateValidator.validateTemplateManagementPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = checklistDataSource.fetchTemplateById(templateId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val updated = current.copy(isActive = false)
        return checklistDataSource.updateTemplate(updated)
    }

    override fun observeCategories(templateId: String): Flow<List<QcChecklistCategory>> {
        return checklistDataSource.observeCategories().map { list ->
            list.filter { it.checklistTemplateId == templateId }.sortedBy { it.sequence }
        }
    }

    override suspend fun addCategory(
        templateId: String,
        name: String,
        sequence: Int,
        callerRole: UserRole?
    ): DomainResult<QcChecklistCategory> = repositoryMutex.withLock {
        val rbac = QcChecklistTemplateValidator.validateTemplateManagementPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val categoryId = "cat-qc-" + UUID.randomUUID().toString()
        val category = QcChecklistCategory(
            categoryId = categoryId,
            checklistTemplateId = templateId,
            name = name,
            sequence = sequence
        )
        return checklistDataSource.insertCategory(category)
    }

    override fun observeItems(templateId: String): Flow<List<QcChecklistItem>> {
        return checklistDataSource.observeItems().map { list ->
            list.filter { it.checklistTemplateId == templateId }.sortedBy { it.sequence }
        }
    }

    override suspend fun addItem(
        templateId: String,
        categoryId: String,
        title: String,
        description: String?,
        sequence: Int,
        code: String?,
        itemType: QcChecklistItemType,
        isRequired: Boolean,
        expectedValue: String?,
        tolerance: String?,
        unit: String?,
        instructions: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcChecklistItem> = repositoryMutex.withLock {
        val rbac = QcChecklistTemplateValidator.validateTemplateManagementPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val itemId = "item-qc-" + UUID.randomUUID().toString()
        val item = QcChecklistItem(
            itemId = itemId,
            checklistTemplateId = templateId,
            categoryId = categoryId,
            title = title,
            description = description,
            sequence = sequence,
            code = code,
            itemType = itemType,
            isRequired = isRequired,
            expectedValue = expectedValue,
            tolerance = tolerance,
            unit = unit,
            instructions = instructions,
            active = true,
            createdAt = timestamp
        )

        val validation = QcChecklistItemValidator.validateItem(item)
        if (validation is DomainResult.Error) return validation

        return checklistDataSource.insertItem(item)
    }

    override suspend fun createInspectionChecklist(
        inspectionId: String,
        templateId: String,
        productionJobId: String,
        productionQcId: String,
        productionStageId: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcInspectionChecklist> = repositoryMutex.withLock {
        val rbac = QcInspectionChecklistValidator.validateInspectionPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val template = when (val res = checklistDataSource.fetchTemplateById(templateId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        if (!template.isActive) {
            return DomainResult.Error(message = "Cannot create checklist from inactive template: ${template.name}")
        }

        val checklistId = "chk-qc-" + UUID.randomUUID().toString()
        val checklist = QcInspectionChecklist(
            inspectionChecklistId = checklistId,
            inspectionId = inspectionId,
            checklistTemplateId = templateId,
            checklistTemplateVersion = template.version,
            productionJobId = productionJobId,
            productionQcId = productionQcId,
            productionStageId = productionStageId,
            status = QcChecklistStatus.READY,
            createdAt = timestamp,
            notes = notes
        )

        val insertRes = checklistDataSource.insertInspectionChecklist(checklist)
        if (insertRes is DomainResult.Success) {
            recordActivity(
                qcId = productionQcId,
                productionJobId = productionJobId,
                activityType = QcActivityType.QC_INSPECTION_CHECKLIST_CREATED,
                notes = "Created inspection checklist from template '${template.name}' (V${template.version}).",
                timestamp = timestamp
            )
        }
        return insertRes
    }

    override fun observeInspectionChecklist(inspectionChecklistId: String): Flow<QcInspectionChecklist?> {
        return checklistDataSource.observeInspectionChecklists().map { list ->
            list.find { it.inspectionChecklistId == inspectionChecklistId }
        }
    }

    override fun getInspectionChecklistForInspection(inspectionId: String): Flow<QcInspectionChecklist?> {
        return checklistDataSource.observeInspectionChecklists().map { list ->
            list.find { it.inspectionId == inspectionId }
        }
    }

    override suspend fun startChecklist(
        inspectionChecklistId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcInspectionChecklist> = repositoryMutex.withLock {
        val rbac = QcInspectionChecklistValidator.validateInspectionPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = checklistDataSource.fetchInspectionChecklistById(inspectionChecklistId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = QcInspectionChecklistValidator.validateStatusTransition(current, QcChecklistStatus.IN_PROGRESS)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(status = QcChecklistStatus.IN_PROGRESS)
        val updateRes = checklistDataSource.updateInspectionChecklist(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                qcId = current.productionQcId,
                productionJobId = current.productionJobId,
                activityType = QcActivityType.QC_INSPECTION_CHECKLIST_STARTED,
                notes = "Started execution of inspection checklist '${current.inspectionChecklistId}'.",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override fun observeResponses(inspectionId: String): Flow<List<QcInspectionResponse>> {
        return checklistDataSource.observeResponses().map { list ->
            list.filter { it.inspectionId == inspectionId }
        }
    }

    override suspend fun saveResponse(
        inspectionId: String,
        checklistItemId: String,
        status: QcResponseStatus,
        value: String?,
        numericValue: Double?,
        selectedValue: String?,
        remarks: String?,
        respondedBy: String,
        respondedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcInspectionResponse> = repositoryMutex.withLock {
        val rbac = QcInspectionChecklistValidator.validateInspectionPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val allItems = checklistDataSource.observeItems().first()
        val item = allItems.find { it.itemId == checklistItemId }
            ?: return DomainResult.Error(message = "Checklist item '$checklistItemId' not found.")

        val responseId = "resp-qc-" + UUID.randomUUID().toString()
        val response = QcInspectionResponse(
            responseId = responseId,
            inspectionId = inspectionId,
            checklistItemId = checklistItemId,
            status = status,
            value = value,
            numericValue = numericValue,
            selectedValue = selectedValue,
            remarks = remarks,
            respondedBy = respondedBy,
            respondedByName = respondedByName,
            respondedAt = timestamp
        )

        val validation = QcInspectionResponseValidator.validateResponse(response, item)
        if (validation is DomainResult.Error) return validation

        val saveRes = checklistDataSource.insertOrUpdateResponse(response)
        return saveRes
    }

    override suspend fun completeInspectionChecklist(
        inspectionChecklistId: String,
        decision: QcDecision,
        completedBy: String,
        completedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcInspectionChecklist> = repositoryMutex.withLock {
        val checklist = when (val res = checklistDataSource.fetchInspectionChecklistById(inspectionChecklistId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val allItems = checklistDataSource.observeItems().first().filter { it.checklistTemplateId == checklist.checklistTemplateId }
        val responses = checklistDataSource.observeResponses().first().filter { it.inspectionId == checklist.inspectionId }

        val completionValidation = QcInspectionChecklistValidator.validateCompletion(
            checklist = checklist,
            items = allItems,
            responses = responses,
            targetDecision = decision,
            callerRole = callerRole
        )
        if (completionValidation is DomainResult.Error) return completionValidation

        val updated = checklist.copy(
            status = QcChecklistStatus.COMPLETED,
            completedAt = timestamp,
            notes = if (notes.isNullOrBlank()) checklist.notes else "${checklist.notes ?: ""}\n$notes"
        )

        val updateRes = checklistDataSource.updateInspectionChecklist(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                qcId = checklist.productionQcId,
                productionJobId = checklist.productionJobId,
                actorId = completedBy,
                actorName = completedByName,
                activityType = QcActivityType.QC_INSPECTION_CHECKLIST_COMPLETED,
                notes = "Completed checklist inspection with decision: ${decision.defaultLabel}.",
                timestamp = timestamp
            )
            recordActivity(
                qcId = checklist.productionQcId,
                productionJobId = checklist.productionJobId,
                actorId = completedBy,
                actorName = completedByName,
                activityType = if (decision == QcDecision.PASS) QcActivityType.QC_INSPECTION_PASSED else QcActivityType.QC_INSPECTION_FAILED,
                notes = "Inspection result: ${decision.defaultLabel}.",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun cancelChecklist(
        inspectionChecklistId: String,
        reason: String,
        cancelledBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<QcInspectionChecklist> = repositoryMutex.withLock {
        val checklist = when (val res = checklistDataSource.fetchInspectionChecklistById(inspectionChecklistId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = QcInspectionChecklistValidator.validateStatusTransition(checklist, QcChecklistStatus.CANCELLED)
        if (transition is DomainResult.Error) return transition

        val updated = checklist.copy(
            status = QcChecklistStatus.CANCELLED,
            notes = "${checklist.notes ?: ""}\nCancelled: $reason"
        )

        return checklistDataSource.updateInspectionChecklist(updated)
    }
}
