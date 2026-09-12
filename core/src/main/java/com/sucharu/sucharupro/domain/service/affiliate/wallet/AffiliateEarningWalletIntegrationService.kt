package com.sucharu.sucharupro.domain.service.affiliate.wallet

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntry
import com.sucharu.sucharupro.domain.model.affiliate.wallet.ApprovedEarningHandoff

/**
 * Domain Service interface for Approved Earning to Wallet Credit Integration (Module 23 Step 03).
 */
interface AffiliateEarningWalletIntegrationService {
    suspend fun processApprovedEarningCredit(
        principalTenantId: String,
        handoff: ApprovedEarningHandoff,
        actorId: String
    ): DomainResult<AffiliateWalletLedgerEntry>
}
