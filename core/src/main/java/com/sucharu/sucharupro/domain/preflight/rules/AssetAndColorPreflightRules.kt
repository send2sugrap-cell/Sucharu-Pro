package com.sucharu.sucharupro.domain.preflight.rules

import com.sucharu.sucharupro.domain.preflight.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Step 04 Rule 1: Computes effective DPI and validates against canonical minDpi threshold when required.
 */
class AssetDpiResolutionRule : PreflightRule {
    override val ruleId: String = "R-201-ASSET-DPI"
    override val ruleCode: String = "RULE_201_ASSET_DPI_RESOLUTION"
    override val ruleName: String = "Image Resolution & DPI Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.IMAGE
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val actualDpi = calculateOrExtractDpi(context)
        val minDpiReq = (context.orderSpecificationMap["minDpi"] as? Number)?.toInt()
            ?: (context.orderSpecificationMap["requiredDpi"] as? Number)?.toInt()

        if (actualDpi == null) {
            if (minDpiReq != null) {
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.ERROR,
                    message = "Image resolution (DPI) metadata is missing and could not be evaluated against required minimum of $minDpiReq DPI.",
                    expectedValue = ">= $minDpiReq DPI",
                    actualValue = "null"
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
                    message = "Image resolution (DPI) metadata is not present or could not be extracted.",
                    actualValue = "null"
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.WARNING,
                    findings = listOf(finding)
                )
            }
        }

        if (minDpiReq != null) {
            if (actualDpi < minDpiReq) {
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.ERROR,
                    message = "Image resolution ($actualDpi DPI) is below required minimum threshold ($minDpiReq DPI).",
                    expectedValue = ">= $minDpiReq DPI",
                    actualValue = "$actualDpi DPI"
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.ERROR,
                    findings = listOf(finding)
                )
            }
        }

        val infoFinding = PreflightFinding(
            findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = context.tenantId,
            preflightRunId = "",
            ruleCode = ruleCode,
            category = category,
            severity = PreflightRuleSeverity.INFO,
            message = "Extracted image resolution: $actualDpi DPI.",
            actualValue = "$actualDpi DPI"
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(infoFinding)
        )
    }

    private fun calculateOrExtractDpi(context: PreflightExecutionContext): Int? {
        val directDpi = (context.artworkMetadataMap["effectiveDpi"] as? Number)?.toInt()
            ?: (context.artworkMetadataMap["resolutionDpi"] as? Number)?.toInt()
            ?: (context.artworkMetadataMap["dpi"] as? Number)?.toInt()
        if (directDpi != null) return directDpi

        val widthPx = (context.artworkMetadataMap["widthPx"] as? Number)?.toDouble()
        val widthMm = (context.artworkMetadataMap["widthMm"] as? Number)?.toDouble()
        if (widthPx != null && widthMm != null && widthMm > 0.0) {
            val widthInches = widthMm / 25.4
            return (widthPx / widthInches).toInt()
        }
        return null
    }
}

/**
 * Step 04 Rule 2: Detects color space and validates against canonical specification requirements.
 */
class AssetColorSpaceRule : PreflightRule {
    override val ruleId: String = "R-202-ASSET-COLOR-SPACE"
    override val ruleCode: String = "RULE_202_ASSET_COLOR_SPACE"
    override val ruleName: String = "Artwork Color Space Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.COLOR
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val actualColorSpace = (context.artworkMetadataMap["colorSpace"] as? String)?.uppercase()
        val requiredColorSpace = (context.orderSpecificationMap["requiredColorSpace"] as? String)?.uppercase()
        val acceptedColorSpaces = (context.orderSpecificationMap["acceptedColorSpaces"] as? List<*>)
            ?.mapNotNull { it?.toString()?.uppercase() } ?: emptyList()

        if (actualColorSpace == null) {
            if (!requiredColorSpace.isNullOrBlank() || acceptedColorSpaces.isNotEmpty()) {
                val expectedStr = if (acceptedColorSpaces.isNotEmpty()) acceptedColorSpaces.joinToString(", ") else requiredColorSpace ?: ""
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.ERROR,
                    message = "Artwork color space metadata is missing and could not be evaluated against required color space '$expectedStr'.",
                    expectedValue = expectedStr,
                    actualValue = "null"
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
                    message = "Color space metadata could not be detected.",
                    actualValue = "null"
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.WARNING,
                    findings = listOf(finding)
                )
            }
        }

        if (!requiredColorSpace.isNullOrBlank() || acceptedColorSpaces.isNotEmpty()) {
            val isMatch = when {
                acceptedColorSpaces.isNotEmpty() -> acceptedColorSpaces.contains(actualColorSpace)
                !requiredColorSpace.isNullOrBlank() -> requiredColorSpace == actualColorSpace
                else -> true
            }

            if (!isMatch) {
                val expectedStr = if (acceptedColorSpaces.isNotEmpty()) acceptedColorSpaces.joinToString(", ") else requiredColorSpace ?: ""
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.ERROR,
                    message = "Artwork color space '$actualColorSpace' does not match required specification color space '$expectedStr'.",
                    expectedValue = expectedStr,
                    actualValue = actualColorSpace
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.ERROR,
                    findings = listOf(finding)
                )
            }
        }

        val infoFinding = PreflightFinding(
            findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = context.tenantId,
            preflightRunId = "",
            ruleCode = ruleCode,
            category = category,
            severity = PreflightRuleSeverity.INFO,
            message = "Detected artwork color space: $actualColorSpace.",
            actualValue = actualColorSpace
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(infoFinding)
        )
    }
}

/**
 * Step 04 Rule 3: Detects ICC color profile identity and compares against required profile when specified.
 */
class AssetColorProfileRule : PreflightRule {
    override val ruleId: String = "R-203-ASSET-COLOR-PROFILE"
    override val ruleCode: String = "RULE_203_ASSET_COLOR_PROFILE"
    override val ruleName: String = "ICC Color Profile Inspection"
    override val category: PreflightRuleCategory = PreflightRuleCategory.COLOR
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.INFO

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val actualProfile = (context.artworkMetadataMap["colorProfile"] as? String)
            ?: (context.artworkMetadataMap["iccProfile"] as? String)
        val requiredProfile = (context.orderSpecificationMap["requiredColorProfile"] as? String)

        if (!requiredProfile.isNullOrBlank()) {
            if (actualProfile == null) {
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.WARNING,
                    message = "Artwork is missing required ICC color profile '$requiredProfile'.",
                    expectedValue = requiredProfile,
                    actualValue = "NONE"
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.WARNING,
                    findings = listOf(finding)
                )
            } else if (!actualProfile.contains(requiredProfile, ignoreCase = true)) {
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.WARNING,
                    message = "Artwork color profile '$actualProfile' does not match required profile '$requiredProfile'.",
                    expectedValue = requiredProfile,
                    actualValue = actualProfile
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.WARNING,
                    findings = listOf(finding)
                )
            }
        }

        val infoMsg = if (actualProfile != null) "Detected color profile: $actualProfile." else "No embedded ICC color profile detected."
        val infoFinding = PreflightFinding(
            findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = context.tenantId,
            preflightRunId = "",
            ruleCode = ruleCode,
            category = category,
            severity = PreflightRuleSeverity.INFO,
            message = infoMsg,
            actualValue = actualProfile ?: "NONE"
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(infoFinding)
        )
    }
}

/**
 * Step 04 Rule 4: Validates availability of linked / external asset references.
 */
class AssetIntegrityMissingRule : PreflightRule {
    override val ruleId: String = "R-204-ASSET-MISSING"
    override val ruleCode: String = "RULE_204_ASSET_MISSING_CHECK"
    override val ruleName: String = "Missing Asset References Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.IMAGE
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("missingAssets") ||
                context.artworkMetadataMap.containsKey("hasMissingAssets") ||
                context.artworkMetadataMap.containsKey("linkedAssets")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        @Suppress("UNCHECKED_CAST")
        val missingList = (context.artworkMetadataMap["missingAssets"] as? List<String>) ?: emptyList()
        val hasMissing = (context.artworkMetadataMap["hasMissingAssets"] as? Boolean) == true || missingList.isNotEmpty()

        if (hasMissing) {
            val assetNames = if (missingList.isNotEmpty()) missingList.joinToString(", ") else "linked asset(s)"
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Linked image asset(s) missing or unresolvable: $assetNames.",
                expectedValue = "All linked assets present",
                actualValue = "Missing: $assetNames"
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
 * Step 04 Rule 5: Detects corrupted or unreadable image asset payloads without crashing engine.
 */
class AssetIntegrityCorruptRule : PreflightRule {
    override val ruleId: String = "R-205-ASSET-CORRUPT"
    override val ruleCode: String = "RULE_205_ASSET_CORRUPT_UNREADABLE"
    override val ruleName: String = "Corrupted Image Asset Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.IMAGE
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("corruptAssets") ||
                context.artworkMetadataMap.containsKey("hasCorruptAssets")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        @Suppress("UNCHECKED_CAST")
        val corruptList = (context.artworkMetadataMap["corruptAssets"] as? List<String>) ?: emptyList()
        val hasCorrupt = (context.artworkMetadataMap["hasCorruptAssets"] as? Boolean) == true || corruptList.isNotEmpty()

        if (hasCorrupt) {
            val assetNames = if (corruptList.isNotEmpty()) corruptList.joinToString(", ") else "embedded/linked image"
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Referenced image asset is corrupted or unreadable: $assetNames.",
                expectedValue = "Valid readable image payload",
                actualValue = "Corrupt: $assetNames"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}
