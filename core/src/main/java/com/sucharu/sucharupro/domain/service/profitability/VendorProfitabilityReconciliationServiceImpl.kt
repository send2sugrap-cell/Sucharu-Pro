package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.VendorProfitabilityMathUtils
import com.sucharu.sucharupro.domain.model.profitability.VendorProfitabilityReconciliationEvent
import com.sucharu.sucharupro.domain.model.profitability.VendorProfitabilitySnapshot
import com.sucharu.sucharupro.domain.model.profitability.VendorSourceCollectionResult
import java.math.BigDecimal
import java.util.UUID

/**
 * Production implementation of non-mutating Vendor Profitability Reconciliation Service.
 * Module 16 Step 05.
 */
class VendorProfitabilityReconciliationServiceImpl : VendorProfitabilityReconciliationService {

    override suspend fun reconcile(
        snapshot: VendorProfitabilitySnapshot,
        sourceData: VendorSourceCollectionResult
    ): VendorProfitabilityReconciliationEvent {
        val errors = mutableListOf<String>()

        // 1. Total cost vs. Source total cost
        val totalCostDiff = snapshot.totalVendorCost.subtract(sourceData.totalVendorCost).abs()
        if (totalCostDiff > BigDecimal("0.0001")) {
            errors.add("Total vendor cost (${snapshot.totalVendorCost}) does not match source total cost (${sourceData.totalVendorCost})")
        }

        // 2. Cost Component Breakdown Sum vs Total Vendor Cost
        val componentSum = snapshot.costBreakdown.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
        val compDiff = snapshot.totalVendorCost.subtract(componentSum).abs()
        if (compDiff > BigDecimal("0.0001")) {
            errors.add("Cost component breakdown sum ($componentSum) does not match total vendor cost (${snapshot.totalVendorCost})")
        }

        // 3. Provenance Attributions Sum vs Total Vendor Cost
        val provenanceSum = sourceData.costAttributions.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.attributedAmount) }
        val provDiff = snapshot.totalVendorCost.subtract(provenanceSum).abs()
        if (provDiff > BigDecimal("0.0001")) {
            errors.add("Cost provenance attributions sum ($provenanceSum) does not match total vendor cost (${snapshot.totalVendorCost})")
        }

        // 4. Job Summaries Sum vs Total Vendor Cost
        val jobSum = sourceData.jobSummaries.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.vendorCost) }
        val jobDiff = if (sourceData.jobSummaries.isNotEmpty()) {
            snapshot.totalVendorCost.subtract(jobSum).abs()
        } else {
            BigDecimal.ZERO
        }

        // 5. Product Summaries Sum vs Total Vendor Cost
        val prodSum = sourceData.productSummaries.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.vendorCost) }
        val prodDiff = if (sourceData.productSummaries.isNotEmpty()) {
            snapshot.totalVendorCost.subtract(prodSum).abs()
        } else {
            BigDecimal.ZERO
        }

        // 6. Customer Summaries Sum vs Total Vendor Cost
        val custSum = sourceData.customerSummaries.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.vendorCost) }
        val custDiff = if (sourceData.customerSummaries.isNotEmpty()) {
            snapshot.totalVendorCost.subtract(custSum).abs()
        } else {
            BigDecimal.ZERO
        }

        // 7. Paid amount vs recognized liability invariant (paid <= total, outstanding >= 0)
        val paidVsLiabilityValid = snapshot.paidVendorCost <= snapshot.totalVendorCost && snapshot.outstandingExposure >= BigDecimal.ZERO
        if (!paidVsLiabilityValid) {
            errors.add("Paid amount (${snapshot.paidVendorCost}) exceeds recognized liability (${snapshot.totalVendorCost}) or outstanding is negative (${snapshot.outstandingExposure})")
        }

        val isBalanced = errors.isEmpty()

        return VendorProfitabilityReconciliationEvent(
            eventId = UUID.randomUUID().toString(),
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            vendorId = snapshot.vendorId,
            snapshotId = snapshot.snapshotId,
            isBalanced = isBalanced,
            totalCostDifference = totalCostDiff.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            componentDifference = compDiff.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            provenanceDifference = provDiff.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            jobDifference = jobDiff.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            productDifference = prodDiff.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            customerDifference = custDiff.setScale(VendorProfitabilityMathUtils.SCALE, VendorProfitabilityMathUtils.ROUNDING),
            paidVsLiabilityValid = paidVsLiabilityValid,
            errorDetails = errors
        )
    }
}
