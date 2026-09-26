package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.content.ContentFoundation
import com.sucharu.sucharupro.domain.model.content.PublicationStatus

/**
 * Repository contract for Form 01 Content & Product Foundation persistence.
 */
interface ContentFoundationRepository {
    suspend fun createContentFoundation(content: ContentFoundation): ContentFoundation
    suspend fun updateContentFoundation(content: ContentFoundation): ContentFoundation
    suspend fun getContentFoundationById(contentId: String): ContentFoundation?
    suspend fun getAllContentFoundations(): List<ContentFoundation>
    suspend fun getContentFoundationsByPublicationStatus(status: PublicationStatus): List<ContentFoundation>
    suspend fun getContentFoundationsByCategory(categoryName: String): List<ContentFoundation>
    suspend fun deleteContentFoundation(contentId: String): Boolean
}
