package com.sucharu.sucharupro.domain.preflight.rules

import com.sucharu.sucharupro.domain.preflight.*
import java.util.UUID

/**
 * Step 07 Rule 1: Validates proof ID and proof version reference integrity.
 */
class ProofReferenceIntegrityRule : PreflightRule {
    override val ruleId: String = "R-501-PROOF-REF-INTEGRITY"
    override val ruleCode: String = "RULE_501_PROOF_REFERENCE_INTEGRITY"
    override val ruleName: String = "Proof Reference Integrity Check"
    override val category: PreflightRuleCategory = PreflightRuleCategory.PROOF
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return !context.proofId.isNullOrBlank() ||
                context.artworkMetadataMap.containsKey("proofId") ||
                context.artworkMetadataMap.containsKey("proofVersionId")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val proofId = context.proofId
            ?: (context.artworkMetadataMap["proofId"] as? String)
            ?: (context.artworkMetadataMap["proofVersionId"] as? String)
            ?: ""

        val isNotFound = (context.artworkMetadataMap["proofNotFound"] as? Boolean) == true
        if (proofId.isBlank() || isNotFound) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Referenced proof version '$proofId' is unresolvable or missing.",
                expectedValue = "Valid proof reference",
                actualValue = "MISSING"
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
 * Step 07 Rule 2: Compares proof page count against reference artwork page count.
 */
class ProofPageCountComparisonRule : PreflightRule {
    override val ruleId: String = "R-502-PROOF-PAGE-COUNT"
    override val ruleCode: String = "RULE_502_PROOF_PAGE_COUNT_COMPARISON"
    override val ruleName: String = "Proof Page Count Comparison"
    override val category: PreflightRuleCategory = PreflightRuleCategory.PROOF
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("proofPageCount") ||
                context.artworkMetadataMap.containsKey("referencePageCount")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val proofPages = (context.artworkMetadataMap["proofPageCount"] as? Number)?.toInt()
            ?: (context.artworkMetadataMap["pageCount"] as? Number)?.toInt()
        val refPages = (context.artworkMetadataMap["referencePageCount"] as? Number)?.toInt()
            ?: (context.orderSpecificationMap["requiredPageCount"] as? Number)?.toInt()

        if (proofPages != null && refPages != null && proofPages != refPages) {
            val finding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.ERROR,
                message = "Proof page count ($proofPages) differs from reference artwork page count ($refPages).",
                expectedValue = "$refPages page(s)",
                actualValue = "$proofPages page(s)"
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
 * Step 07 Rule 3: Compares proof trim dimensions against reference artwork dimensions within ±1.0 mm.
 */
class ProofGeometryComparisonRule : PreflightRule {
    override val ruleId: String = "R-503-PROOF-GEOMETRY"
    override val ruleCode: String = "RULE_503_PROOF_GEOMETRY_COMPARISON"
    override val ruleName: String = "Proof Finished Dimensions Comparison"
    override val category: PreflightRuleCategory = PreflightRuleCategory.PROOF
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.ERROR

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return (context.artworkMetadataMap.containsKey("proofWidthMm") && context.artworkMetadataMap.containsKey("proofHeightMm")) ||
                (context.artworkMetadataMap.containsKey("refWidthMm") && context.artworkMetadataMap.containsKey("refHeightMm"))
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val proofW = (context.artworkMetadataMap["proofWidthMm"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["widthMm"] as? Number)?.toDouble()
        val proofH = (context.artworkMetadataMap["proofHeightMm"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["heightMm"] as? Number)?.toDouble()

        val refW = (context.artworkMetadataMap["refWidthMm"] as? Number)?.toDouble()
            ?: (context.orderSpecificationMap["finishedWidthMm"] as? Number)?.toDouble()
            ?: (context.orderSpecificationMap["finishedWidthMm"] as? String)?.toDoubleOrNull()
        val refH = (context.artworkMetadataMap["refHeightMm"] as? Number)?.toDouble()
            ?: (context.orderSpecificationMap["finishedHeightMm"] as? Number)?.toDouble()
            ?: (context.orderSpecificationMap["finishedHeightMm"] as? String)?.toDoubleOrNull()

        if (proofW != null && proofH != null && refW != null && refH != null) {
            val wDiff = Math.abs(proofW - refW)
            val hDiff = Math.abs(proofH - refH)
            if (wDiff > 1.0 || hDiff > 1.0) {
                val finding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.ERROR,
                    message = "Proof finished dimensions (${proofW}x${proofH} mm) differ from reference artwork dimensions (${refW}x${refH} mm).",
                    expectedValue = "${refW}x${refH} mm",
                    actualValue = "${proofW}x${proofH} mm"
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
 * Step 07 Rule 4: Compares content hashes/checksums to track content changes without confusing byte changes with visual errors.
 */
class ProofContentFingerprintRule : PreflightRule {
    override val ruleId: String = "R-504-PROOF-FINGERPRINT"
    override val ruleCode: String = "RULE_504_PROOF_FINGERPRINT_COMPARISON"
    override val ruleName: String = "Proof Content Fingerprint & Checksum Comparison"
    override val category: PreflightRuleCategory = PreflightRuleCategory.PROOF
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.INFO

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return (context.artworkMetadataMap.containsKey("proofChecksum") || context.artworkMetadataMap.containsKey("proofHash")) &&
                (context.artworkMetadataMap.containsKey("refChecksum") || context.artworkMetadataMap.containsKey("refHash"))
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val proofHash = (context.artworkMetadataMap["proofChecksum"] as? String)
            ?: (context.artworkMetadataMap["proofHash"] as? String)
            ?: ""
        val refHash = (context.artworkMetadataMap["refChecksum"] as? String)
            ?: (context.artworkMetadataMap["refHash"] as? String)
            ?: ""

        if (proofHash.isNotBlank() && refHash.isNotBlank()) {
            if (proofHash == refHash) {
                val infoFinding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.INFO,
                    message = "Proof content hash is identical to reference artwork version.",
                    actualValue = proofHash
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.PASS,
                    findings = listOf(infoFinding)
                )
            } else {
                val infoFinding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = context.tenantId,
                    preflightRunId = "",
                    ruleCode = ruleCode,
                    category = category,
                    severity = PreflightRuleSeverity.INFO,
                    message = "Proof content checksum differs from reference artwork version (content/version change detected).",
                    expectedValue = refHash,
                    actualValue = proofHash
                )
                return PreflightRuleExecutionResult(
                    result = PreflightExecutionResult.PASS,
                    findings = listOf(infoFinding)
                )
            }
        }

        return PreflightRuleExecutionResult(result = PreflightExecutionResult.PASS)
    }
}

/**
 * Step 07 Rule 5: Compares Proof Version A vs Proof Version B technical metadata.
 */
class ProofVersionComparisonRule : PreflightRule {
    override val ruleId: String = "R-505-PROOF-VERSION-COMPARE"
    override val ruleCode: String = "RULE_505_PROOF_VERSION_COMPARISON"
    override val ruleName: String = "Proof Version A vs Version B Comparison"
    override val category: PreflightRuleCategory = PreflightRuleCategory.PROOF
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.INFO

    override fun isApplicable(context: PreflightExecutionContext): Boolean {
        return context.artworkMetadataMap.containsKey("previousVersionMetadata")
    }

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        @Suppress("UNCHECKED_CAST")
        val prevMeta = (context.artworkMetadataMap["previousVersionMetadata"] as? Map<String, Any>) ?: emptyMap()
        val currMeta = context.artworkMetadataMap

        val diffs = mutableListOf<String>()

        val prevPages = prevMeta["pageCount"]
        val currPages = currMeta["pageCount"]
        if (prevPages != null && currPages != null && prevPages != currPages) {
            diffs.add("pageCount changed from $prevPages to $currPages")
        }

        val prevColor = prevMeta["colorSpace"]
        val currColor = currMeta["colorSpace"]
        if (prevColor != null && currColor != null && prevColor != currColor) {
            diffs.add("colorSpace changed from $prevColor to $currColor")
        }

        val prevDpi = prevMeta["resolutionDpi"]
        val currDpi = currMeta["resolutionDpi"]
        if (prevDpi != null && currDpi != null && prevDpi != currDpi) {
            diffs.add("resolutionDpi changed from $prevDpi to $currDpi")
        }

        val msg = if (diffs.isNotEmpty()) {
            "Technical metadata differences between Proof Version A and Version B: ${diffs.joinToString("; ")}."
        } else {
            "No technical metadata differences detected between proof versions."
        }

        val infoFinding = PreflightFinding(
            findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = context.tenantId,
            preflightRunId = "",
            ruleCode = ruleCode,
            category = category,
            severity = PreflightRuleSeverity.INFO,
            message = msg,
            actualValue = if (diffs.isNotEmpty()) diffs.joinToString("; ") else "IDENTICAL"
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(infoFinding)
        )
    }
}

/**
 * Step 07 Rule 6: Evaluates visual pixel diff ratio metadata when present without manufacturing visual comparisons.
 */
class ProofVisualComparisonRule : PreflightRule {
    override val ruleId: String = "R-506-PROOF-VISUAL-COMPARE"
    override val ruleCode: String = "RULE_506_PROOF_VISUAL_COMPARISON"
    override val ruleName: String = "Proof Visual Pixel Diff Ratio Inspection"
    override val category: PreflightRuleCategory = PreflightRuleCategory.PROOF
    override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.INFO

    override fun isApplicable(context: PreflightExecutionContext): Boolean = true

    override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
        val diffRatio = (context.artworkMetadataMap["visualDiffRatio"] as? Number)?.toDouble()
            ?: (context.artworkMetadataMap["pixelDiffPercent"] as? Number)?.toDouble()

        if (diffRatio != null) {
            val infoFinding = PreflightFinding(
                findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                tenantId = context.tenantId,
                preflightRunId = "",
                ruleCode = ruleCode,
                category = category,
                severity = PreflightRuleSeverity.INFO,
                message = "Automated visual pixel diff ratio: ${String.format("%.2f", diffRatio)} %.",
                actualValue = "${String.format("%.2f", diffRatio)} %"
            )
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.PASS,
                findings = listOf(infoFinding)
            )
        }

        val infoFinding = PreflightFinding(
            findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = context.tenantId,
            preflightRunId = "",
            ruleCode = ruleCode,
            category = category,
            severity = PreflightRuleSeverity.INFO,
            message = "Visual pixel comparison not evaluated for this proof run.",
            actualValue = "NOT_EVALUATED"
        )
        return PreflightRuleExecutionResult(
            result = PreflightExecutionResult.PASS,
            findings = listOf(infoFinding)
        )
    }
}
