package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.ArtworkFileType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FileReference] and [ArtworkFileType] validation (Module 05 Step 02).
 */
class ArtworkFileReferenceTest {

    @Test
    fun validFileReference_passesValidation() {
        val file = FileReference(
            fileId = "f1",
            fileName = "catalog_cover.ai",
            mimeType = "application/illustrator",
            fileSize = 5242880L,
            storagePath = "/storage/artworks/catalog_cover.ai",
            uploadedAt = "2026-08-16T10:00:00Z"
        )

        val result = DesignArtworkValidator.validateFileReference(file)
        assertTrue(result is DomainResult.Success)
        assertEquals(ArtworkFileType.AI, file.fileType)
        assertEquals("5.00 MB", file.formattedSize)
    }

    @Test
    fun fileTypeDetection_correctlyIdentifiesStandardFormats() {
        assertEquals(ArtworkFileType.PDF, ArtworkFileType.fromFileNameOrMime("doc.pdf", null))
        assertEquals(ArtworkFileType.AI, ArtworkFileType.fromFileNameOrMime("doc.ai", null))
        assertEquals(ArtworkFileType.PSD, ArtworkFileType.fromFileNameOrMime("doc.psd", null))
        assertEquals(ArtworkFileType.EPS, ArtworkFileType.fromFileNameOrMime("doc.eps", null))
        assertEquals(ArtworkFileType.SVG, ArtworkFileType.fromFileNameOrMime("doc.svg", null))
        assertEquals(ArtworkFileType.TIFF, ArtworkFileType.fromFileNameOrMime("doc.tiff", null))
        assertEquals(ArtworkFileType.JPEG, ArtworkFileType.fromFileNameOrMime("doc.jpg", null))
        assertEquals(ArtworkFileType.PNG, ArtworkFileType.fromFileNameOrMime("doc.png", null))
        assertEquals(ArtworkFileType.OTHER, ArtworkFileType.fromFileNameOrMime("doc.unknown", null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroByteFile_throwsExceptionOnInit() {
        FileReference(
            fileId = "f-zero",
            fileName = "empty.pdf",
            mimeType = "application/pdf",
            fileSize = 0L,
            storagePath = "/storage/empty.pdf",
            uploadedAt = "2026-08-16T10:00:00Z"
        )
    }
}
