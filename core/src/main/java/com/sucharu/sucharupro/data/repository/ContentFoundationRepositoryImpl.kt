package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.domain.model.content.ContentFoundation
import com.sucharu.sucharupro.domain.model.content.PublicationStatus
import com.sucharu.sucharupro.domain.repository.ContentFoundationRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe Repository Implementation for Form 01 Content & Product Foundation persistence.
 */
class ContentFoundationRepositoryImpl : ContentFoundationRepository {
    private val store = ConcurrentHashMap<String, ContentFoundation>()

    override suspend fun createContentFoundation(content: ContentFoundation): ContentFoundation {
        store[content.contentId] = content
        return content
    }

    override suspend fun updateContentFoundation(content: ContentFoundation): ContentFoundation {
        store[content.contentId] = content
        return content
    }

    override suspend fun getContentFoundationById(contentId: String): ContentFoundation? {
        return store[contentId]
    }

    override suspend fun getAllContentFoundations(): List<ContentFoundation> {
        return store.values.toList()
    }

    override suspend fun getContentFoundationsByPublicationStatus(status: PublicationStatus): List<ContentFoundation> {
        return store.values.filter { it.publicationStatus == status }
    }

    override suspend fun getContentFoundationsByCategory(categoryName: String): List<ContentFoundation> {
        return store.values.filter { it.categoryName.equals(categoryName, ignoreCase = true) }
    }

    override suspend fun deleteContentFoundation(contentId: String): Boolean {
        return store.remove(contentId) != null
    }
}
