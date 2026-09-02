package com.sucharu.sucharupro.domain.model.common

/**
 * Standard professional artwork file categories.
 */
enum class ArtworkFileType(val extension: String, val defaultMimeType: String) {
    PDF(".pdf", "application/pdf"),
    AI(".ai", "application/illustrator"),
    PSD(".psd", "image/vnd.adobe.photoshop"),
    EPS(".eps", "application/postscript"),
    SVG(".svg", "image/svg+xml"),
    TIFF(".tiff", "image/tiff"),
    JPEG(".jpg", "image/jpeg"),
    PNG(".png", "image/png"),
    OTHER("", "application/octet-stream");

    companion object {
        fun fromFileNameOrMime(fileName: String, mimeType: String?): ArtworkFileType {
            val lowerName = fileName.lowercase()
            return when {
                lowerName.endsWith(".pdf") || mimeType == "application/pdf" -> PDF
                lowerName.endsWith(".ai") || mimeType == "application/illustrator" -> AI
                lowerName.endsWith(".psd") || mimeType == "image/vnd.adobe.photoshop" -> PSD
                lowerName.endsWith(".eps") || mimeType == "application/postscript" -> EPS
                lowerName.endsWith(".svg") || mimeType == "image/svg+xml" -> SVG
                lowerName.endsWith(".tif") || lowerName.endsWith(".tiff") || mimeType == "image/tiff" -> TIFF
                lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || mimeType == "image/jpeg" -> JPEG
                lowerName.endsWith(".png") || mimeType == "image/png" -> PNG
                else -> OTHER
            }
        }
    }
}

/**
 * Generic file and document reference in Sucharu Pro ERP.
 *
 * Represents an immutable pointer to a stored binary asset in the system file store.
 */
data class FileReference(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val storagePath: String,
    val checksum: String? = null,
    val uploadedAt: String,
    val uploadedBy: String? = null
) {
    init {
        require(fileId.isNotBlank()) { "File ID cannot be blank." }
        require(fileName.isNotBlank()) { "File Name cannot be blank." }
        require(mimeType.isNotBlank()) { "MIME Type cannot be blank." }
        require(fileSize > 0) { "File Size must be greater than zero bytes." }
        require(storagePath.isNotBlank()) { "Storage path cannot be blank." }
        require(uploadedAt.isNotBlank()) { "Uploaded timestamp cannot be blank." }
    }

    val fileType: ArtworkFileType get() = ArtworkFileType.fromFileNameOrMime(fileName, mimeType)

    /** Human-readable formatted file size. */
    val formattedSize: String
        get() {
            return when {
                fileSize < 1024 -> "$fileSize B"
                fileSize < 1024 * 1024 -> String.format("%.1f KB", fileSize.toDouble() / 1024.0)
                else -> String.format("%.2f MB", fileSize.toDouble() / (1024.0 * 1024.0))
            }
        }
}
