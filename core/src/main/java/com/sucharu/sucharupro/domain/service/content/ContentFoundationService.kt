package com.sucharu.sucharupro.domain.service.content

import com.sucharu.sucharupro.domain.model.content.ContentFoundation
import com.sucharu.sucharupro.domain.model.content.PublicationStatus
import com.sucharu.sucharupro.domain.repository.ContentFoundationRepository
import java.util.UUID

/**
 * Domain Service for Form 01 — Content & Product Foundation Lifecycle & Publishing Governance.
 */
class ContentFoundationService(
    private val repository: ContentFoundationRepository
) {
    suspend fun createContentRecord(content: ContentFoundation): ContentFoundation {
        require(content.productCode.isNotBlank()) { "Product code is required." }
        require(content.title.isNotBlank()) { "Title is required." }
        return repository.createContentFoundation(content)
    }

    suspend fun publishContent(contentId: String, actorId: String): ContentFoundation {
        val existing = repository.getContentFoundationById(contentId)
            ?: throw IllegalArgumentException("Content record not found for ID: $contentId")

        val published = existing.copy(
            publicationStatus = PublicationStatus.PUBLISHED,
            updatedBy = actorId
        )
        return repository.updateContentFoundation(published)
    }

    suspend fun unpublishContent(contentId: String, actorId: String): ContentFoundation {
        val existing = repository.getContentFoundationById(contentId)
            ?: throw IllegalArgumentException("Content record not found for ID: $contentId")

        val unpublished = existing.copy(
            publicationStatus = PublicationStatus.UNPUBLISHED,
            updatedBy = actorId
        )
        return repository.updateContentFoundation(unpublished)
    }

    suspend fun scheduleContent(
        contentId: String,
        startAt: String,
        endAt: String?,
        actorId: String
    ): ContentFoundation {
        val existing = repository.getContentFoundationById(contentId)
            ?: throw IllegalArgumentException("Content record not found for ID: $contentId")

        val scheduled = existing.copy(
            publicationStatus = PublicationStatus.SCHEDULED,
            scheduledStartAt = startAt,
            scheduledEndAt = endAt,
            updatedBy = actorId
        )
        return repository.updateContentFoundation(scheduled)
    }

    suspend fun listPublishedContent(): List<ContentFoundation> {
        return repository.getContentFoundationsByPublicationStatus(PublicationStatus.PUBLISHED)
    }

    suspend fun listAllContentRecords(): List<ContentFoundation> {
        return repository.getAllContentFoundations()
    }
}
