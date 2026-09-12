package com.sucharu.sucharupro.domain.preflight.rules

import com.sucharu.sucharupro.domain.preflight.*
import java.util.UUID

/**
 * Step 02 Rule 1: Verifies artwork file reference presence in context.
 */
class FileExistenceRule : PreflightRule {
    override val ruleId: String = "R-001-FILE-EXISTS"
    override val ruleCode: String = "RULE_001_FILE_EXISTS"
    override val ruleName: String = "Artwork File Existence Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.DOCUMENT
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val fileUrl = (context.artworkMetadataMap["fileUrl"] as? String)
            ?: (context.artworkMetadataMap["filePath"] as? String)
            ?: context.artworkId

        if (fileUrl.isBlank()) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Artwork file reference is missing or empty."
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        val isMissing = (context.artworkMetadataMap["fileNotFound"] as? Boolean) == true
        if (isMissing) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Artwork file '$fileUrl' does not exist in storage."
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 02 Rule 2: Rejects zero-byte or empty artwork files.
 */
class FileNonEmptyRule : PreflightRule {
    override val ruleId: String = "R-002-FILE-NON-EMPTY"
    override val ruleCode: String = "RULE_002_FILE_NON_EMPTY"
    override val ruleName: String = "Artwork File Non-Empty Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.DOCUMENT
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val fileSize = (context.artworkMetadataMap["fileSize"] as? Number)?.toLong()
            ?: (context.artworkMetadataMap["fileSizeBytes"] as? Number)?.toLong()

        if (fileSize != null && fileSize <= 0L) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Artwork file is zero bytes (empty).",
                expectedValue = "> 0 bytes",
                actualValue = "0 bytes"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 02 Rule 3: Validates artwork format against supported prepress formats.
 */
class FileFormatSupportedRule : PreflightRule {
    override val ruleId: String = "R-003-FORMAT-SUPPORTED"
    override val ruleCode: String = "RULE_003_FILE_FORMAT_SUPPORTED"
    override val ruleName: String = "File Format Support Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.DOCUMENT
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    private val supportedFormats = setOf("PDF", "TIFF", "TIF", "PNG", "JPEG", "JPG", "EPS", "AI", "PSD")

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val format = ((context.artworkMetadataMap["format"] as? String)
            ?: (context.artworkMetadataMap["extension"] as? String)
            ?: "PDF").uppercase()

        if (!supportedFormats.contains(format)) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "File format '$format' is not supported for prepress preflight.",
                expectedValue = "PDF, TIFF, PNG, JPEG, EPS, AI, PSD",
                actualValue = format
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 02 Rule 4: Flags extension and declared MIME type mismatches.
 */
class FileFormatMatchRule : PreflightRule {
    override val ruleId: String = "R-004-FORMAT-MATCH"
    override val ruleCode: String = "RULE_004_FILE_FORMAT_MATCH"
    override val ruleName: String = "Extension & MIME Mismatch Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.DOCUMENT
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("mimeType") &&
                (context.artworkMetadataMap.containsKey("extension") || context.artworkMetadataMap.containsKey("format"))
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val mimeType = (context.artworkMetadataMap["mimeType"] as? String)?.lowercase() ?: ""
        val ext = ((context.artworkMetadataMap["extension"] as? String)
            ?: (context.artworkMetadataMap["format"] as? String))?.lowercase() ?: ""

        val isMismatch = when {
            mimeType.contains("pdf") && !ext.contains("pdf") -> true
            mimeType.contains("png") && !ext.contains("png") -> true
            mimeType.contains("jpeg") && (!ext.contains("jpg") && !ext.contains("jpeg")) -> true
            mimeType.contains("tiff") && (!ext.contains("tif") && !ext.contains("tiff")) -> true
            else -> false
        }

        if (isMismatch) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.WARNING,
                message = "File extension '$ext' does not match declared MIME type '$mimeType'.",
                expectedValue = "Matching extension/MIME",
                actualValue = "$ext vs $mimeType"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.WARNING,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 02 Rule 5: Inspects file header magic-byte signatures against declared format.
 */
class FileSignatureValidRule : PreflightRule {
    override val ruleId: String = "R-005-SIGNATURE-VALID"
    override val ruleCode: String = "RULE_005_FILE_SIGNATURE_VALID"
    override val ruleName: String = "File Header Signature Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.DOCUMENT
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("magicSignature") ||
                context.artworkMetadataMap.containsKey("headerBytes")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val signature = (context.artworkMetadataMap["magicSignature"] as? String)
            ?: (context.artworkMetadataMap["headerBytes"] as? String)
            ?: ""
        val format = ((context.artworkMetadataMap["format"] as? String)
            ?: (context.artworkMetadataMap["extension"] as? String)
            ?: "PDF").uppercase()

        val isValid = when (format) {
            "PDF" -> signature.contains("%PDF") || signature.startsWith("25504446")
            "PNG" -> signature.contains("PNG") || signature.startsWith("89504E47")
            "JPEG", "JPG" -> signature.startsWith("FFD8FF") || signature.contains("JFIF") || signature.contains("Exif")
            "TIFF", "TIF" -> signature.startsWith("4949") || signature.startsWith("4D4M") || signature.contains("II") || signature.contains("MM")
            else -> true
        }

        if (!isValid) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "File header signature '$signature' does not match declared format '$format'.",
                expectedValue = "Valid $format header signature",
                actualValue = signature
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 02 Rule 6: Checks if document structure can be parsed without corruption errors.
 */
class DocumentParseableRule : PreflightRule {
    override val ruleId: String = "R-006-DOC-PARSEABLE"
    override val ruleCode: String = "RULE_006_DOCUMENT_PARSEABLE"
    override val ruleName: String = "Document Parsability Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.DOCUMENT
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val isCorrupted = (context.artworkMetadataMap["isCorrupted"] as? Boolean) == true
        val parseError = context.artworkMetadataMap["parseError"] as? String

        if (isCorrupted || !parseError.isNullOrBlank()) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Document structure is unparseable or corrupted: ${parseError ?: "Header/Trailer structure corrupt"}.",
                actualValue = parseError ?: "CORRUPTED"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 02 Rule 7: Extracts and logs document page count metadata.
 */
class DocumentPageCountRule : PreflightRule {
    override val ruleId: String = "R-007-PAGE-COUNT"
    override val ruleCode: String = "RULE_007_DOCUMENT_PAGE_COUNT"
    override val ruleName: String = "Document Page Count Inspection"
    override val category: PreflightRuleCategory = PreflightRuleCategory.DOCUMENT
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.INFO

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val pageCount = (context.artworkMetadataMap["pageCount"] as? Number)?.toInt()

        if (pageCount != null && pageCount > 0) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.INFO,
                message = "Document page count extracted: $pageCount page(s).",
                actualValue = "$pageCount"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.PASS,
                findings = listOf(finding)
            )
        }

        val format = ((context.artworkMetadataMap["format"] as? String) ?: "PDF").uppercase()
        if (format == "PDF" || format == "TIFF") {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.WARNING,
                message = "Document page count could not be extracted for format '$format'.",
                expectedValue = "Positive integer page count",
                actualValue = "null"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.WARNING,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 02 Rule 8: Extracts raster image pixel dimensions without evaluating DPI compliance.
 */
class ImageStructureReadableRule : PreflightRule {
    override val ruleId: String = "R-008-IMAGE-STRUCTURE"
    override val ruleCode: String = "RULE_008_IMAGE_STRUCTURE_READABLE"
    override val ruleName: String = "Image Pixel Dimensions Extraction"
    override val category: PreflightRuleCategory = PreflightRuleCategory.IMAGE
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    private val rasterFormats = setOf("PNG", "JPEG", "JPG", "TIFF", "TIF", "PSD")

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        val format = ((context.artworkMetadataMap["format"] as? String)
            ?: (context.artworkMetadataMap["extension"] as? String)
            ?: "").uppercase()
        return rasterFormats.contains(format)
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val width = (context.artworkMetadataMap["widthPx"] as? Number)?.toInt()
            ?: (context.artworkMetadataMap["width"] as? Number)?.toInt()
        val height = (context.artworkMetadataMap["heightPx"] as? Number)?.toInt()
            ?: (context.artworkMetadataMap["height"] as? Number)?.toInt()

        if (width == null || height == null || width <= 0 || height <= 0) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.WARNING,
                message = "Image pixel dimensions (width/height) could not be extracted.",
                expectedValue = "Positive width & height in pixels",
                actualValue = "width=$width, height=$height"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.WARNING,
                findings = listOf(finding)
            )
        }

        val finding = PreflightFinding(
            findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = context.tenantId,
            preflightRunId = "",
            ruleCode = ruleCode,
            category = category,
            severity = PreflightRuleSeverity.INFO,
            message = "Image dimensions extracted: ${width}x${height} px.",
            actualValue = "${width}x${height} px"
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(finding)
        )
    }
}
