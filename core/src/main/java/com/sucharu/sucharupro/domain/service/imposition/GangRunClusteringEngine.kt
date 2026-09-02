package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.UUID

/**
 * Deterministic Engine for Multi-Job Compatibility Clustering and Gang-Run Batching.
 * Module 18 Step 02.
 */
class GangRunClusteringEngine(
    private val singleJobEngine: SingleJobImpositionEngine = SingleJobImpositionEngine()
) {

    /**
     * Clusters candidate jobs into compatible groups according to clustering policy.
     */
    fun formClusters(
        candidates: List<GangRunCandidateItem>,
        policy: GangRunClusteringPolicy = GangRunClusteringPolicy.STRICT_IDENTICAL_SUBSTRATE
    ): List<GangRunCluster> {
        if (candidates.isEmpty()) return emptyList()

        val clusters = mutableListOf<GangRunCluster>()
        // Stable deterministic sort by paperStockType, gsm, colorMode, printingSideOption, jobId
        val sortedCandidates = candidates.sortedWith(
            compareBy<GangRunCandidateItem>(
                { it.paperStockType.name },
                { it.gsm },
                { it.colorMode.name },
                { it.printingSideOption.name },
                { it.jobId }
            )
        )

        for (candidate in sortedCandidates) {
            val matchingClusterIndex = clusters.indexOfFirst { cluster ->
                isCompatible(candidate, cluster, policy)
            }

            if (matchingClusterIndex >= 0) {
                val existing = clusters[matchingClusterIndex]
                clusters[matchingClusterIndex] = existing.copy(
                    candidateItems = existing.candidateItems + candidate
                )
            } else {
                val newClusterId = "CLUSTER-${UUID.nameUUIDFromBytes("${candidate.paperStockType}_${candidate.gsm}_${candidate.colorMode}_${candidate.printingSideOption}".toByteArray()).toString().take(8).uppercase()}"
                clusters.add(
                    GangRunCluster(
                        clusterId = newClusterId,
                        paperStockType = candidate.paperStockType,
                        representativeGsm = candidate.gsm,
                        colorMode = candidate.colorMode,
                        printingSideOption = candidate.printingSideOption,
                        candidateItems = listOf(candidate)
                    )
                )
            }
        }

        return clusters
    }

    private fun isCompatible(
        candidate: GangRunCandidateItem,
        cluster: GangRunCluster,
        policy: GangRunClusteringPolicy
    ): Boolean {
        if (candidate.paperStockType != cluster.paperStockType) return false
        if (candidate.colorMode != cluster.colorMode) return false
        if (candidate.printingSideOption != cluster.printingSideOption) return false

        return when (policy) {
            GangRunClusteringPolicy.STRICT_IDENTICAL_SUBSTRATE,
            GangRunClusteringPolicy.MAXIMIZE_SHEET_YIELD -> {
                candidate.gsm.compareTo(cluster.representativeGsm) == 0
            }
            GangRunClusteringPolicy.RELAXED_GSM_TOLERANCE -> {
                val diff = candidate.gsm.subtract(cluster.representativeGsm).abs()
                diff <= BigDecimal("10.0000")
            }
        }
    }

    /**
     * Optimizes a compatible cluster of jobs into a shared gang-run sheet layout.
     */
    fun optimizeGangRun(
        tenantId: String,
        batchName: String,
        cluster: GangRunCluster,
        parentSheetDimension: PrintingDimension,
        margins: ImpositionMarginSpec = ImpositionMarginSpec(),
        spacing: ImpositionSpacingSpec = ImpositionSpacingSpec(),
        actor: String = "prepress_operator"
    ): GangRunSpecification {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(batchName.isNotBlank()) { "Batch name must not be blank." }
        require(cluster.candidateItems.isNotEmpty()) { "Cannot optimize an empty cluster." }

        val normSheetDim = ImpositionMathUtils.toMillimeters(parentSheetDimension)
        val (usableW, usableH) = ImpositionMathUtils.calculateUsableSheetDimension(
            normSheetDim.width,
            normSheetDim.height,
            margins
        )
        val usableArea = usableW.multiply(usableH).setScale(4, RoundingMode.HALF_UP)

        // Evaluate baseline grid capacity for the cluster items
        val sortedItems = cluster.candidateItems.sortedBy { it.jobId }

        // Find common maximum orthogonal slot dimensions across items
        val maxItemWidth = sortedItems.maxOf { ImpositionMathUtils.toMillimeters(it.finishedDimension).width }
        val maxItemHeight = sortedItems.maxOf { ImpositionMathUtils.toMillimeters(it.finishedDimension).height }

        val gH = spacing.horizontalGutterMm.coerceAtLeast(BigDecimal.ZERO)
        val gV = spacing.verticalGutterMm.coerceAtLeast(BigDecimal.ZERO)

        // Standard orientation fit
        val stdCols = if (maxItemWidth > BigDecimal.ZERO && usableW >= maxItemWidth) {
            usableW.add(gH).divide(maxItemWidth.add(gH), 0, RoundingMode.FLOOR).toInt().coerceAtLeast(0)
        } else 0
        val stdRows = if (maxItemHeight > BigDecimal.ZERO && usableH >= maxItemHeight) {
            usableH.add(gV).divide(maxItemHeight.add(gV), 0, RoundingMode.FLOOR).toInt().coerceAtLeast(0)
        } else 0
        val stdCapacity = stdCols * stdRows

        // Rotated orientation fit
        val rotCols = if (maxItemHeight > BigDecimal.ZERO && usableW >= maxItemHeight) {
            usableW.add(gH).divide(maxItemHeight.add(gH), 0, RoundingMode.FLOOR).toInt().coerceAtLeast(0)
        } else 0
        val rotRows = if (maxItemWidth > BigDecimal.ZERO && usableH >= maxItemWidth) {
            usableH.add(gV).divide(maxItemWidth.add(gV), 0, RoundingMode.FLOOR).toInt().coerceAtLeast(0)
        } else 0
        val rotCapacity = rotCols * rotRows

        val useRotated = rotCapacity > stdCapacity
        val totalSlots = if (useRotated) rotCapacity else stdCapacity
        val chosenOrientation = if (useRotated) ImpositionLayoutOrientation.ROTATED else ImpositionLayoutOrientation.STANDARD

        require(totalSlots > 0) {
            "Items in cluster exceed usable sheet dimensions (${usableW}mm x ${usableH}mm)."
        }

        // Proportional slot allocation based on required quantities
        val totalDemand = sortedItems.sumOf { it.requiredQuantity }
        var remainingSlots = totalSlots
        val itemAllocationsDraft = mutableListOf<Triple<GangRunCandidateItem, Int, BigDecimal>>()

        // Step A: Guarantee at least 1 slot per job if totalSlots >= job count
        val jobCount = sortedItems.size
        require(totalSlots >= jobCount) {
            "Not enough slots ($totalSlots) on sheet for ${jobCount} candidate jobs in cluster."
        }

        // Allocate slots proportionally
        for (item in sortedItems) {
            val demandRatio = BigDecimal(item.requiredQuantity).divide(BigDecimal(totalDemand), 6, RoundingMode.HALF_UP)
            val proportionalSlots = BigDecimal(totalSlots).multiply(demandRatio).setScale(0, RoundingMode.FLOOR).toInt().coerceAtLeast(1)
            itemAllocationsDraft.add(Triple(item, proportionalSlots, demandRatio))
        }

        val allocatedSoFar = itemAllocationsDraft.sumOf { it.second }
        remainingSlots = totalSlots - allocatedSoFar

        // Distribute remaining slots to jobs that have highest demand per slot
        val finalAllocatedMap = itemAllocationsDraft.associate { it.first.jobId to it.second }.toMutableMap()
        if (remainingSlots > 0) {
            val sortedByNeed = sortedItems.sortedByDescending { item: GangRunCandidateItem ->
                val currentSlots = finalAllocatedMap[item.jobId] ?: 1
                item.requiredQuantity / currentSlots
            }
            var ptr = 0
            while (remainingSlots > 0) {
                val target = sortedByNeed[ptr % sortedByNeed.size]
                finalAllocatedMap[target.jobId] = (finalAllocatedMap[target.jobId] ?: 1) + 1
                remainingSlots--
                ptr++
            }
        }

        // Calculate common required sheet run length = max of (ceil(item.qty / item.slots))
        var commonSheets = 1L
        for (item in sortedItems) {
            val slots = finalAllocatedMap[item.jobId] ?: 1
            val sheetsForItem = ImpositionMathUtils.calculateRequiredSheets(item.requiredQuantity, slots)
            if (sheetsForItem > commonSheets) {
                commonSheets = sheetsForItem
            }
        }

        // Build item allocations
        val allocations = mutableListOf<GangRunItemAllocation>()
        var totalOccupiedArea = BigDecimal.ZERO

        for (item in sortedItems) {
            val slots = finalAllocatedMap[item.jobId] ?: 1
            val normDim = ImpositionMathUtils.toMillimeters(item.finishedDimension)
            val singleItemArea = normDim.width.multiply(normDim.height).setScale(4, RoundingMode.HALF_UP)
            val itemOccupied = singleItemArea.multiply(BigDecimal(slots)).setScale(4, RoundingMode.HALF_UP)
            totalOccupiedArea = totalOccupiedArea.add(itemOccupied)

            val produced = commonSheets * slots.toLong()
            val overage = produced - item.requiredQuantity
            val relYield = if (usableArea.compareTo(BigDecimal.ZERO) > 0) {
                itemOccupied.divide(usableArea, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100.0000")).setScale(4, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO

            allocations.add(
                GangRunItemAllocation(
                    jobId = item.jobId,
                    orderId = item.orderId,
                    orderItemId = item.orderItemId,
                    productName = item.productName,
                    assignedSlots = slots,
                    orientation = chosenOrientation,
                    slotItemWidthMm = if (useRotated) normDim.height else normDim.width,
                    slotItemHeightMm = if (useRotated) normDim.width else normDim.height,
                    requiredQuantity = item.requiredQuantity,
                    producedQuantity = produced,
                    overageQuantity = overage,
                    itemOccupiedAreaMm2 = itemOccupied,
                    relativeYieldPercentage = relYield
                )
            )
        }

        val totalWasteArea = usableArea.subtract(totalOccupiedArea).coerceAtLeast(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val overallYield = if (usableArea.compareTo(BigDecimal.ZERO) > 0) {
            totalOccupiedArea.divide(usableArea, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100.0000")).setScale(4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val gangRunId = "GANG-${UUID.randomUUID().toString().take(12).uppercase()}"
        val totalAllocatedSlots = allocations.sumOf { it.assignedSlots }
        val totalProducedItems = allocations.sumOf { it.producedQuantity }
        val totalOverageItems = allocations.sumOf { it.overageQuantity }

        // Compute deterministic SHA-256 hash
        val hashPayload = buildString {
            append(tenantId).append("|")
            append(batchName).append("|")
            append(cluster.paperStockType.name).append("|")
            append(cluster.representativeGsm.toPlainString()).append("|")
            append(normSheetDim.width.toPlainString()).append("|")
            append(normSheetDim.height.toPlainString()).append("|")
            append(totalAllocatedSlots).append("|")
            append(commonSheets).append("|")
            allocations.forEach {
                append(it.jobId).append(":").append(it.assignedSlots).append(";")
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val integrityHash = digest.digest(hashPayload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        return GangRunSpecification(
            gangRunId = gangRunId,
            tenantId = tenantId,
            batchName = batchName,
            paperStockType = cluster.paperStockType,
            gsm = cluster.representativeGsm.setScale(4, RoundingMode.HALF_UP),
            colorMode = cluster.colorMode,
            printingSideOption = cluster.printingSideOption,
            parentSheetDimension = normSheetDim,
            marginSpec = margins,
            spacingSpec = spacing,
            totalAvailableSlots = totalSlots,
            allocatedSlotsCount = totalAllocatedSlots,
            commonRequiredSheets = commonSheets,
            totalProducedItems = totalProducedItems,
            totalOverageItems = totalOverageItems,
            usableAreaMm2 = usableArea,
            occupiedAreaMm2 = totalOccupiedArea.setScale(4, RoundingMode.HALF_UP),
            wasteAreaMm2 = totalWasteArea,
            sheetYieldPercentage = overallYield,
            allocations = allocations,
            version = 1,
            status = GangRunStatus.OPTIMIZED,
            integrityHash = integrityHash,
            notes = "Optimized gang-run for ${allocations.size} jobs on ${normSheetDim.width}mm x ${normSheetDim.height}mm parent sheets.",
            createdAt = System.currentTimeMillis(),
            createdBy = actor
        )
    }
}
