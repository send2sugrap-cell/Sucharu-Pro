package com.sucharu.sucharupro.domain.preflight.rules

import com.sucharu.sucharupro.domain.preflight.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import kotlin.math.abs

/**
 * Step 03 Rule 1: Compares required specification page count against actual artwork page count.
 */
class SpecPageCountMatchRule : PreflightRule {
    override val ruleId: String = "R-101-SPEC-PAGE-COUNT"
    override val ruleCode: String = "RULE_101_SPEC_PAGE_COUNT_MATCH"
    override val ruleName: String = "Job Specification Page Count Match"
    override val category: PreflightRuleCategory = PreflightRuleCategory.SPECIFICATION
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.orderSpecificationMap.containsKey("requiredPageCount") ||
                context.orderSpecificationMap.containsKey("pageCount") ||
                context.orderSpecificationMap.containsKey("sheetsPerItem")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val expected = (context.orderSpecificationMap["requiredPageCount"] as? Number)?.toInt()
            ?: (context.orderSpecificationMap["pageCount"] as? Number)?.toInt()
            ?: (context.orderSpecificationMap["sheetsPerItem"] as? Number)?.toInt()
            ?: return PreflightRuleExecutionResult(result = PreflightExecutionResult.SKIPPED)

        val actual = (context.artworkMetadataMap["pageCount"] as? Number)?.toInt()
        if (actual == null) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Compliance check unable to evaluate: Artwork page count could not be extracted.",
                expectedValue = "$expected page(s)",
                actualValue = "null"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        if (actual != expected) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Artwork page count ($actual) does not match required job specification page count ($expected).",
                expectedValue = "$expected page(s)",
                actualValue = "$actual page(s)"
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
 * Step 03 Rule 2: Compares required finished dimensions (mm) against actual artwork dimensions within ±1.0 mm tolerance.
 */
class SpecDocumentSizeMatchRule : PreflightRule {
    override val ruleId: String = "R-102-SPEC-DOC-SIZE"
    override val ruleCode: String = "RULE_102_SPEC_DOCUMENT_SIZE_MATCH"
    override val ruleName: String = "Job Specification Finished Dimensions Match"
    override val category: PreflightRuleCategory = PreflightRuleCategory.SPECIFICATION
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return (context.orderSpecificationMap.containsKey("finishedWidthMm") && context.orderSpecificationMap.containsKey("finishedHeightMm")) ||
                (context.orderSpecificationMap.containsKey("requiredWidthMm") && context.orderSpecificationMap.containsKey("requiredHeightMm"))
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val expWidth = parseBigDecimal(context.orderSpecificationMap["finishedWidthMm"] ?: context.orderSpecificationMap["requiredWidthMm"])
            ?: return PreflightRuleExecutionResult(result = PreflightExecutionResult.SKIPPED)
        val expHeight = parseBigDecimal(context.orderSpecificationMap["finishedHeightMm"] ?: context.orderSpecificationMap["requiredHeightMm"])
            ?: return PreflightRuleExecutionResult(result = PreflightExecutionResult.SKIPPED)

        var actWidth = parseBigDecimal(context.artworkMetadataMap["widthMm"])
        var actHeight = parseBigDecimal(context.artworkMetadataMap["heightMm"])

        // Convert points to mm if widthPt / heightPt supplied (1 pt = 0.352778 mm)
        if (actWidth == null || actHeight == null) {
            val widthPt = parseBigDecimal(context.artworkMetadataMap["widthPt"])
            val heightPt = parseBigDecimal(context.artworkMetadataMap["heightPt"])
            if (widthPt != null && heightPt != null) {
                val ptToMm = BigDecimal("0.352778")
                actWidth = widthPt.multiply(ptToMm).setScale(2, RoundingMode.HALF_UP)
                actHeight = heightPt.multiply(ptToMm).setScale(2, RoundingMode.HALF_UP)
            }
        }

        if (actWidth == null || actHeight == null) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Compliance check unable to evaluate: Artwork geometric dimensions (width/height) could not be extracted.",
                expectedValue = "${expWidth}x${expHeight} mm",
                actualValue = "null"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        val widthDiff = expWidth.subtract(actWidth).abs()
        val heightDiff = expHeight.subtract(actHeight).abs()
        val tolerance = BigDecimal("1.00") // ±1.0 mm tolerance

        if (widthDiff > tolerance || heightDiff > tolerance) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Artwork finished dimensions (${actWidth}x${actHeight} mm) do not match required specification (${expWidth}x${expHeight} mm).",
                expectedValue = "${expWidth}x${expHeight} mm",
                actualValue = "${actWidth}x${actHeight} mm"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }

    private fun parseBigDecimal(value: Any?): BigDecimal? {
        if (value == null) return null
        if (value is BigDecimal) return value
        if (value is Number) return BigDecimal(value.toString())
        if (value is String) return value.toBigDecimalOrNull()
        return null
    }
}

/**
 * Step 03 Rule 3: Compares required orientation against artwork document aspect ratio.
 */
class SpecOrientationMatchRule : PreflightRule {
    override val ruleId: String = "R-103-SPEC-ORIENTATION"
    override val ruleCode: String = "RULE_103_SPEC_ORIENTATION_MATCH"
    override val ruleName: String = "Job Specification Orientation Match"
    override val category: PreflightRuleCategory = PreflightRuleCategory.SPECIFICATION
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.orderSpecificationMap.containsKey("requiredOrientation") ||
                context.orderSpecificationMap.containsKey("orientation") ||
                (context.orderSpecificationMap.containsKey("finishedWidthMm") && context.orderSpecificationMap.containsKey("finishedHeightMm"))
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val reqOrientation = (context.orderSpecificationMap["requiredOrientation"] as? String)?.uppercase()
            ?: (context.orderSpecificationMap["orientation"] as? String)?.uppercase()
            ?: deriveOrientationFromDimensions(
                context.orderSpecificationMap["finishedWidthMm"],
                context.orderSpecificationMap["finishedHeightMm"]
            )
            ?: return PreflightRuleExecutionResult(result = PreflightExecutionResult.SKIPPED)

        val actualOrientation = deriveOrientationFromContext(context)
        if (actualOrientation == null) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Compliance check unable to evaluate: Artwork orientation could not be derived.",
                expectedValue = reqOrientation,
                actualValue = "null"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        if (reqOrientation != actualOrientation) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Artwork orientation ($actualOrientation) does not match required specification orientation ($reqOrientation).",
                expectedValue = reqOrientation,
                actualValue = actualOrientation
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }

    private fun deriveOrientationFromContext(context: PreflightExecutionContext): String? {
        val explicit = (context.artworkMetadataMap["orientation"] as? String)?.uppercase()
        if (explicit != null) return explicit

        val w = (context.artworkMetadataMap["widthMm"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["widthPx"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["widthPt"] as? Number)?.toDouble()
        val h = (context.artworkMetadataMap["heightMm"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["heightPx"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["heightPt"] as? Number)?.toDouble()

        if (w == null || h == null) return null
        val diff = abs(w - h)
        return when {
            diff <= 2.0 -> "SQUARE"
            h > w -> "PORTRAIT"
            else -> "LANDSCAPE"
        }
    }

    private fun deriveOrientationFromDimensions(wObj: Any?, hObj: Any?): String? {
        val w = (wObj as? Number)?.toDouble() ?: (wObj as? String)?.toDoubleOrNull()
        val h = (hObj as? Number)?.toDouble() ?: (hObj as? String)?.toDoubleOrNull()
        if (w == null || h == null) return null
        val diff = abs(w - h)
        return when {
            diff <= 2.0 -> "SQUARE"
            h > w -> "PORTRAIT"
            else -> "LANDSCAPE"
        }
    }
}

/**
 * Step 03 Rule 4: Compares required file format from specification against actual detected format.
 */
class SpecFormatMatchRule : PreflightRule {
    override val ruleId: String = "R-104-SPEC-FORMAT"
    override val ruleCode: String = "RULE_104_SPEC_FORMAT_MATCH"
    override val ruleName: String = "Job Specification File Format Match"
    override val category: PreflightRuleCategory = PreflightRuleCategory.SPECIFICATION
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.orderSpecificationMap.containsKey("requiredFormat") ||
                context.orderSpecificationMap.containsKey("acceptedFormats")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val reqFormatStr = (context.orderSpecificationMap["requiredFormat"] as? String)?.uppercase()
        val acceptedList = (context.orderSpecificationMap["acceptedFormats"] as? List<*>)
            ?.mapNotNull { it?.toString()?.uppercase() } ?: emptyList()

        val actualFormat = ((context.artworkMetadataMap["format"] as? String)
            ?: (context.artworkMetadataMap["extension"] as? String)
            ?: "").uppercase()

        val isCompliant = when {
            acceptedList.isNotEmpty() -> acceptedList.contains(actualFormat)
            !reqFormatStr.isNullOrBlank() -> reqFormatStr == actualFormat
            else -> true
        }

        if (!isCompliant) {
            val expectedStr = if (acceptedList.isNotEmpty()) acceptedList.joinToString(", ") else reqFormatStr ?: ""
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Actual artwork format '$actualFormat' does not match required specification format '$expectedStr'.",
                expectedValue = expectedStr,
                actualValue = actualFormat
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
 * Step 03 Rule 5: Compares required product document classification against actual artwork page structure.
 */
class SpecDocumentTypeMatchRule : PreflightRule {
    override val ruleId: String = "R-105-SPEC-DOC-TYPE"
    override val ruleCode: String = "RULE_105_SPEC_DOCUMENT_TYPE_MATCH"
    override val ruleName: String = "Job Specification Product Document Type Match"
    override val category: PreflightRuleCategory = PreflightRuleCategory.SPECIFICATION
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.orderSpecificationMap.containsKey("productType") ||
                context.orderSpecificationMap.containsKey("requiredDocumentType")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val productType = ((context.orderSpecificationMap["productType"] as? String)
            ?: (context.orderSpecificationMap["requiredDocumentType"] as? String)
            ?: "").uppercase()

        val actualPageCount = (context.artworkMetadataMap["pageCount"] as? Number)?.toInt() ?: 1

        val isSinglePageProduct = productType.contains("FLYER") || productType.contains("POSTER") ||
                productType.contains("LABEL") || productType.contains("STICKER") ||
                productType.contains("BUSINESS_CARD") || productType == "SINGLE_PAGE"

        val isMultiPageProduct = productType.contains("CATALOG") || productType.contains("BOOK") ||
                productType.contains("MAGAZINE") || productType.contains("BROCHURE") ||
                productType == "MULTI_PAGE"

        var isMismatch = false
        var reason = ""

        if (isSinglePageProduct && actualPageCount > 1) {
            isMismatch = true
            reason = "Product type '$productType' requires a single-page artwork, but document contains $actualPageCount pages."
        } else if (isMultiPageProduct && actualPageCount == 1) {
            isMismatch = true
            reason = "Multi-page product type '$productType' requires a multi-page document, but artwork contains only 1 page."
        }

        if (isMismatch) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = reason,
                expectedValue = productType,
                actualValue = "$actualPageCount page(s)"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}
