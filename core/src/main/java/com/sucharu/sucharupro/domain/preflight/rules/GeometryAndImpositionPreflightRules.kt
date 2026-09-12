package com.sucharu.sucharupro.domain.preflight.rules

import com.sucharu.sucharupro.domain.preflight.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Step 06 Rule 1: Evaluates artwork bleed geometry against required minimum bleed threshold when specified.
 */
class BleedBoxReadinessRule : PreflightRule {
    override val ruleId: String = "R-401-BLEED-GEOMETRY"
    override val ruleCode: String = "RULE_401_BLEED_GEOMETRY_READINESS"
    override val ruleName: String = "Bleed Margin & Geometry Readiness Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.GEOMETRY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val actualBleed = (context.artworkMetadataMap["bleedMarginMm"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["bleedWidthMm"] as? Number)?.toDouble()

        val minBleedReq = (context.orderSpecificationMap["minBleedMm"] as? Number)?.toDouble()
            ?: (context.orderSpecificationMap["requiredBleedMm"] as? Number)?.toDouble()

        if (actualBleed == null || actualBleed <= 0.0) {
            if (minBleedReq != null && minBleedReq > 0.0) {
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.ERROR,
                    message = "Artwork lacks required bleed margin (detected $actualBleed mm vs required $minBleedReq mm).",
                    expectedValue = ">= $minBleedReq mm",
                    actualValue = "${actualBleed ?: 0.0} mm"
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.ERROR,
                    findings = listOf(finding)
                )
            } else {
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.WARNING,
                    message = "No bleed margin detected in artwork geometry.",
                    actualValue = "0.0 mm"
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.WARNING,
                    findings = listOf(finding)
                )
            }
        }

        if (minBleedReq != null && actualBleed < minBleedReq) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Artwork bleed margin ($actualBleed mm) is less than required specification minimum ($minBleedReq mm).",
                expectedValue = ">= $minBleedReq mm",
                actualValue = "$actualBleed mm"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        val infoFinding = PreflightFinding(
            findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = context.tenantId,
            preflightRunId = "",
            ruleCode = ruleCode,
            category = category,
            severity = PreflightRuleSeverity.INFO,
            message = "Detected artwork bleed margin: $actualBleed mm.",
            actualValue = "$actualBleed mm"
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(infoFinding)
        )
    }
}

/**
 * Step 06 Rule 2: Checks TrimBox presence and compares TrimBox dimensions against job finished size.
 */
class TrimBoxPresenceAndSizeRule : PreflightRule {
    override val ruleId: String = "R-402-TRIMBOX-SIZE"
    override val ruleCode: String = "RULE_402_TRIM_BOX_PRESENCE_AND_SIZE"
    override val ruleName: String = "TrimBox Boundary & Dimension Readiness Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.GEOMETRY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val hasTrimBox = (context.artworkMetadataMap["hasTrimBox"] as? Boolean)
            ?: (context.artworkMetadataMap["trimBoxWidthMm"] != null)

        if (hasTrimBox == false) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.WARNING,
                message = "Document lacks explicit TrimBox page boundary.",
                actualValue = "NO_TRIMBOX"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.WARNING,
                findings = listOf(finding)
            )
        }

        val actW = (context.artworkMetadataMap["trimBoxWidthMm"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["widthMm"] as? Number)?.toDouble()
        val actH = (context.artworkMetadataMap["trimBoxHeightMm"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["heightMm"] as? Number)?.toDouble()

        val expW = (context.orderSpecificationMap["finishedWidthMm"] as? Number)?.toDouble()
            ?: (context.orderSpecificationMap["finishedWidthMm"] as? String)?.toDoubleOrNull()
        val expH = (context.orderSpecificationMap["finishedHeightMm"] as? Number)?.toDouble()
            ?: (context.orderSpecificationMap["finishedHeightMm"] as? String)?.toDoubleOrNull()

        if (expW != null && expH != null && actW != null && actH != null) {
            val wDiff = Math.abs(expW - actW)
            val hDiff = Math.abs(expH - actH)
            if (wDiff > 1.0 || hDiff > 1.0) {
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.ERROR,
                    message = "TrimBox dimensions (${actW}x${actH} mm) do not match finished job specification (${expW}x${expH} mm).",
                    expectedValue = "${expW}x${expH} mm",
                    actualValue = "${actW}x${actH} mm"
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.ERROR,
                    findings = listOf(finding)
                )
            }
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 06 Rule 3: Validates PDF page boundary hierarchy (MediaBox >= BleedBox >= TrimBox).
 */
class PageBoxRelationshipRule : PreflightRule {
    override val ruleId: String = "R-403-PAGEBOX-HIERARCHY"
    override val ruleCode: String = "RULE_403_PAGE_BOX_RELATIONSHIPS"
    override val ruleName: String = "Page Box Hierarchy & Relationship Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.GEOMETRY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("isPageBoxHierarchyValid") ||
                context.artworkMetadataMap.containsKey("trimExceedsMedia") ||
                context.artworkMetadataMap.containsKey("cropCutsTrim")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val isValid = (context.artworkMetadataMap["isPageBoxHierarchyValid"] as? Boolean) != false
        val trimExceeds = (context.artworkMetadataMap["trimExceedsMedia"] as? Boolean) == true
        val cropCuts = (context.artworkMetadataMap["cropCutsTrim"] as? Boolean) == true

        if (!isValid || trimExceeds || cropCuts) {
            val reason = when {
                trimExceeds -> "TrimBox exceeds MediaBox boundary"
                cropCuts -> "CropBox cuts inside TrimBox boundary"
                else -> "Page box hierarchy is invalid"
            }
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Invalid page box hierarchy: $reason.",
                expectedValue = "MediaBox >= BleedBox >= TrimBox",
                actualValue = reason
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
 * Step 06 Rule 4: Validates page size and orientation consistency across multi-page documents.
 */
class PageGeometryConsistencyRule : PreflightRule {
    override val ruleId: String = "R-404-GEOMETRY-CONSISTENCY"
    override val ruleCode: String = "RULE_404_PAGE_GEOMETRY_CONSISTENCY"
    override val ruleName: String = "Multi-Page Geometry Consistency Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.GEOMETRY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        val pageCount = (context.artworkMetadataMap["pageCount"] as? Number)?.toInt() ?: 1
        return pageCount > 1
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val isInconsistent = (context.artworkMetadataMap["isPageGeometryConsistent"] as? Boolean) == false ||
                (context.artworkMetadataMap["hasMixedPageSizes"] as? Boolean) == true

        if (isInconsistent) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.WARNING,
                message = "Inconsistent page geometry detected across multi-page document: pages have mixed trim sizes or orientations.",
                expectedValue = "Uniform page size & orientation across all pages",
                actualValue = "Mixed page sizes/orientations"
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
 * Step 06 Rule 5: Compares safe area margin against specification threshold when specified.
 */
class SafeAreaGeometryRule : PreflightRule {
    override val ruleId: String = "R-405-SAFE-AREA"
    override val ruleCode: String = "RULE_405_SAFE_AREA_GEOMETRY"
    override val ruleName: String = "Safe Area Margin Readiness Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.GEOMETRY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.orderSpecificationMap.containsKey("requiredSafeAreaMm") ||
                context.artworkMetadataMap.containsKey("safeAreaMarginMm")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val reqSafeArea = (context.orderSpecificationMap["requiredSafeAreaMm"] as? Number)?.toDouble()
            ?: return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)

        val actualSafeArea = (context.artworkMetadataMap["safeAreaMarginMm"] as? Number)?.toDouble()

        if (actualSafeArea != null && actualSafeArea < reqSafeArea) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.WARNING,
                message = "Safe area margin ($actualSafeArea mm) is narrower than required threshold ($reqSafeArea mm).",
                expectedValue = ">= $reqSafeArea mm",
                actualValue = "$actualSafeArea mm"
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
 * Step 06 Rule 6: Validates page count and geometry readiness for prepress imposition.
 */
class ImpositionReadinessRule : PreflightRule {
    override val ruleId: String = "R-406-IMPOSITION-READINESS"
    override val ruleCode: String = "RULE_406_IMPOSITION_READINESS"
    override val ruleName: String = "Prepress Imposition Readiness Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.PRODUCTION_READINESS
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val pageCount = (context.artworkMetadataMap["pageCount"] as? Number)?.toInt() ?: 1
        val bindingMethod = (context.orderSpecificationMap["bindingMethod"] as? String)?.uppercase() ?: ""
        val productType = (context.orderSpecificationMap["productType"] as? String)?.uppercase() ?: ""

        val isBookletOrSignature = bindingMethod == "SADDLE_STITCH" || bindingMethod == "PERFECT_BIND" ||
                productType.contains("BOOKLET") || productType.contains("BOOK") || productType.contains("CATALOG")

        if (isBookletOrSignature && pageCount % 4 != 0) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Page count ($pageCount) is not a multiple of 4 required for booklet/signature prepress imposition.",
                expectedValue = "Multiple of 4 pages (4, 8, 12, 16, ...)",
                actualValue = "$pageCount page(s)"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        val infoFinding = PreflightFinding(
            findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = context.tenantId,
            preflightRunId = "",
            ruleCode = ruleCode,
            category = category,
            severity = PreflightRuleSeverity.INFO,
            message = "Artwork geometry is technically ready for prepress imposition ($pageCount page(s)).",
            actualValue = "IMPOSITION_READY"
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(infoFinding)
        )
    }
}
