package com.sucharu.sucharupro.domain.preflight.rules

import com.sucharu.sucharupro.domain.preflight.*
import java.util.UUID

/**
 * Step 05 Rule 1: Validates font presence and flags missing or unresolvable font references.
 */
class FontPresenceAndMissingRule : PreflightRule {
    override val ruleId: String = "R-301-FONT-MISSING"
    override val ruleCode: String = "RULE_301_FONT_MISSING_CHECK"
    override val ruleName: String = "Referenced Fonts Presence Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.TYPOGRAPHY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("missingFonts") ||
                context.artworkMetadataMap.containsKey("hasMissingFonts") ||
                context.artworkMetadataMap.containsKey("fonts")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        @Suppress("UNCHECKED_CAST")
        val missingFonts = (context.artworkMetadataMap["missingFonts"] as? List<String>) ?: emptyList()
        val hasMissing = (context.artworkMetadataMap["hasMissingFonts"] as? Boolean) == true || missingFonts.isNotEmpty()

        if (hasMissing) {
            val fontNames = if (missingFonts.isNotEmpty()) missingFonts.joinToString(", ") else "referenced font(s)"
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Referenced font(s) missing or unresolvable in document: $fontNames.",
                expectedValue = "All referenced fonts present",
                actualValue = "Missing: $fontNames"
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
 * Step 05 Rule 2: Inspects font embedding status (embedded, non-embedded, subset).
 */
class FontEmbeddingStatusRule : PreflightRule {
    override val ruleId: String = "R-302-FONT-EMBEDDING"
    override val ruleCode: String = "RULE_302_FONT_EMBEDDING_STATUS"
    override val ruleName: String = "Font Embedding Status Inspection"
    override val category: PreflightRuleCategory = PreflightRuleCategory.TYPOGRAPHY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("nonEmbeddedFonts") ||
                context.artworkMetadataMap.containsKey("embeddedFonts") ||
                context.artworkMetadataMap.containsKey("fontEmbeddingStatus")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        @Suppress("UNCHECKED_CAST")
        val nonEmbedded = (context.artworkMetadataMap["nonEmbeddedFonts"] as? List<String>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val embedded = (context.artworkMetadataMap["embeddedFonts"] as? List<String>) ?: emptyList()
        val requireEmbedding = (context.orderSpecificationMap["requireEmbeddedFonts"] as? Boolean) == true

        if (nonEmbedded.isNotEmpty()) {
            val fontNames = nonEmbedded.joinToString(", ")
            val severity = if (requireEmbedding) PreflightRuleSeverity.ERROR else PreflightRuleSeverity.WARNING
            val execResult = if (requireEmbedding) PreflightExecutionResult.ERROR else PreflightExecutionResult.WARNING

            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = severity,
                message = "Font(s) not embedded in document: $fontNames.",
                expectedValue = "Fully/Subset embedded fonts",
                actualValue = "Not embedded: $fontNames"
            )
            return PreflightRuleExecutionResult(
                result = execResult,
                findings = listOf(finding)
            )
        }

        val infoMsg = if (embedded.isNotEmpty()) "Font embedding check passed: ${embedded.size} font(s) embedded or subset." else "No non-embedded fonts detected."
        val infoFinding = PreflightFinding(
            findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = context.tenantId,
            preflightRunId = "",
            ruleCode = ruleCode,
            category = category,
            severity = PreflightRuleSeverity.INFO,
            message = infoMsg,
            actualValue = "${embedded.size} embedded"
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(infoFinding)
        )
    }
}

/**
 * Step 05 Rule 3: Detects font types/formats (TrueType, OpenType, Type 1, CID, Type 3) and checks compatibility.
 */
class FontTypeCompatibilityRule : PreflightRule {
    override val ruleId: String = "R-303-FONT-TYPE-COMPAT"
    override val ruleCode: String = "RULE_303_FONT_TYPE_COMPATIBILITY"
    override val ruleName: String = "Font Type & Format Compatibility Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.TYPOGRAPHY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("fontTypes") ||
                context.artworkMetadataMap.containsKey("fontFormats")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        @Suppress("UNCHECKED_CAST")
        val fontTypes = (context.artworkMetadataMap["fontTypes"] as? Map<String, String>)
            ?: (context.artworkMetadataMap["fontFormats"] as? Map<String, String>)
            ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        val prohibitedTypes = (context.orderSpecificationMap["prohibitedFontTypes"] as? List<*>)
            ?.mapNotNull { it?.toString()?.uppercase() } ?: emptyList()

        // 1. Check specification prohibited font types
        if (prohibitedTypes.isNotEmpty()) {
            val prohibitedMatches = fontTypes.filter { (_, type) -> prohibitedTypes.contains(type.uppercase()) }
            if (prohibitedMatches.isNotEmpty()) {
                val matchDetails = prohibitedMatches.entries.joinToString(", ") { "${it.key} (${it.value})" }
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.ERROR,
                    message = "Font(s) use prohibited font format according to specification: $matchDetails.",
                    expectedValue = "Not in ${prohibitedTypes.joinToString(", ")}",
                    actualValue = matchDetails
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.ERROR,
                    findings = listOf(finding)
                )
            }
        }

        // 2. Check for Type 3 (bitmap) fonts
        val type3Fonts = fontTypes.filter { (_, type) -> type.uppercase().contains("TYPE3") || type.uppercase().contains("TYPE 3") }
        if (type3Fonts.isNotEmpty()) {
            val fontNames = type3Fonts.keys.joinToString(", ")
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.WARNING,
                message = "Document contains Type 3 (bitmap) font(s): $fontNames which may cause rasterization issues.",
                expectedValue = "TrueType / OpenType / CIDFont",
                actualValue = "Type 3: $fontNames"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.WARNING,
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
            message = "Detected font types/formats: ${fontTypes.values.distinct().joinToString(", ")}.",
            actualValue = fontTypes.values.distinct().joinToString(", ")
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(infoFinding)
        )
    }
}

/**
 * Step 05 Rule 4: Flags font substitution occurrences in the document.
 */
class FontSubstitutionRule : PreflightRule {
    override val ruleId: String = "R-304-FONT-SUBSTITUTION"
    override val ruleCode: String = "RULE_304_FONT_SUBSTITUTION_CHECK"
    override val ruleName: String = "Font Substitution Occurrence Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.TYPOGRAPHY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("substitutedFonts") ||
                context.artworkMetadataMap.containsKey("hasSubstitutedFonts")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        @Suppress("UNCHECKED_CAST")
        val substitutedMap = (context.artworkMetadataMap["substitutedFonts"] as? Map<String, String>) ?: emptyMap()
        val hasSubstitution = (context.artworkMetadataMap["hasSubstitutedFonts"] as? Boolean) == true || substitutedMap.isNotEmpty()
        val prohibitSubstitution = (context.orderSpecificationMap["prohibitSubstitutedFonts"] as? Boolean) == true

        if (hasSubstitution) {
            val details = if (substitutedMap.isNotEmpty()) {
                substitutedMap.entries.joinToString(", ") { "${it.key} -> ${it.value}" }
            } else {
                "font substitution detected"
            }

            val severity = if (prohibitSubstitution) PreflightRuleSeverity.ERROR else PreflightRuleSeverity.WARNING
            val execResult = if (prohibitSubstitution) PreflightExecutionResult.ERROR else PreflightExecutionResult.WARNING

            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = severity,
                message = "Font substitution detected in document: $details.",
                expectedValue = "Original fonts without substitution",
                actualValue = details
            )
            return PreflightRuleExecutionResult(
                result = execResult,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 05 Rule 5: Compares document fonts against required/allowed font families in job specification.
 */
class FontRequirementComplianceRule : PreflightRule {
    override val ruleId: String = "R-305-FONT-COMPLIANCE"
    override val ruleCode: String = "RULE_305_FONT_REQUIREMENT_COMPLIANCE"
    override val ruleName: String = "Job Specification Font Family Compliance"
    override val category: PreflightRuleCategory = PreflightRuleCategory.TYPOGRAPHY
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.orderSpecificationMap.containsKey("allowedFontFamilies") ||
                context.orderSpecificationMap.containsKey("requiredFonts")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        @Suppress("UNCHECKED_CAST")
        val allowedFamilies = (context.orderSpecificationMap["allowedFontFamilies"] as? List<*>)
            ?.mapNotNull { it?.toString()?.uppercase() }
            ?: (context.orderSpecificationMap["requiredFonts"] as? List<*>)
            ?.mapNotNull { it?.toString()?.uppercase() }
            ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        val actualFonts = (context.artworkMetadataMap["fonts"] as? List<String>) ?: emptyList()

        val disallowed = actualFonts.filter { fontName ->
            allowedFamilies.none { allowed -> fontName.uppercase().contains(allowed) }
        }

        if (disallowed.isNotEmpty()) {
            val disallowedStr = disallowed.joinToString(", ")
            val allowedStr = allowedFamilies.joinToString(", ")
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Document contains font(s) '$disallowedStr' not allowed by job specification '$allowedStr'.",
                expectedValue = allowedStr,
                actualValue = disallowedStr
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.ERROR,
                findings = listOf(finding)
            )
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}
