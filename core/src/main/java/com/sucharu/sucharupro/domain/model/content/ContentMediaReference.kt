package com.sucharu.sucharupro.domain.model.content

/**
 * Media metadata reference entity for Form 01 Content Foundation.
 */
data class ContentMediaReference(
    val mediaId: String,
    val contentId: String,
    val mediaType: MediaType = MediaType.MAIN_IMAGE,
    val mediaUri: String,
    val altText: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: String
) {
    init {
        require(mediaId.isNotBlank()) { "Media ID cannot be blank." }
        require(contentId.isNotBlank()) { "Content ID cannot be blank." }
        require(mediaUri.isNotBlank()) { "Media URI cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
    }
}
