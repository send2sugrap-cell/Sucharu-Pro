package com.sucharu.sucharupro.ui.admin.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.repository.ContentFoundationRepositoryImpl
import com.sucharu.sucharupro.domain.model.content.ContentFoundation
import com.sucharu.sucharupro.domain.model.content.PublicationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.product.ProductType
import com.sucharu.sucharupro.domain.repository.ContentFoundationRepository
import com.sucharu.sucharupro.domain.service.content.ContentFoundationService
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Form 01 — Admin Content & Product Foundation Management.
 */
class AdminContentFoundationViewModel(
    repository: ContentFoundationRepository = ContentFoundationRepositoryImpl()
) : ViewModel() {

    private val service = ContentFoundationService(repository)

    var contentRecords = mutableStateListOf<ContentFoundation>()
        private set

    var isSuccessMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadContentRecords()
    }

    fun loadContentRecords() {
        viewModelScope.launch {
            val items = service.listAllContentRecords()
            contentRecords.clear()
            contentRecords.addAll(items)
        }
    }

    fun createOrUpdateContent(
        contentId: String?,
        title: String,
        subtitle: String,
        description: String,
        productName: String,
        productCode: String,
        categoryName: String,
        templateCode: String,
        badgeText: String,
        availableQty: Int,
        publicationStatus: PublicationStatus
    ) {
        viewModelScope.launch {
            val id = contentId.takeUnless { it.isNullOrBlank() } ?: ("CNT-2026-" + UUID.randomUUID().toString().take(6).uppercase())
            val timestamp = "2026-09-26T11:00:00Z"

            val record = ContentFoundation(
                contentId = id,
                contentType = ProductType.FINISHED_PRODUCT,
                productName = productName.ifBlank { title },
                productCode = productCode.ifBlank { "SKU-$id" },
                categoryName = categoryName.ifBlank { "General" },
                templateCode = templateCode.ifBlank { "#TMPL-01" },
                title = title,
                subtitle = subtitle.ifBlank { null },
                description = description.ifBlank { null },
                badgeText = badgeText.ifBlank { null },
                unit = InventoryUnit.PCS,
                minimumQuantity = 1,
                availableQuantity = availableQty,
                publicationStatus = publicationStatus,
                createdAt = timestamp,
                updatedAt = timestamp,
                createdBy = "ADMIN"
            )

            service.createContentRecord(record)
            loadContentRecords()
            isSuccessMessage = "✓ Content record successfully saved!"
        }
    }

    fun publishRecord(contentId: String) {
        viewModelScope.launch {
            service.publishContent(contentId, "ADMIN")
            loadContentRecords()
        }
    }

    fun unpublishRecord(contentId: String) {
        viewModelScope.launch {
            service.unpublishContent(contentId, "ADMIN")
            loadContentRecords()
        }
    }
}
