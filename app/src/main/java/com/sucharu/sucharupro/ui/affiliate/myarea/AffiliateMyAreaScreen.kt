package com.sucharu.sucharupro.ui.affiliate.myarea

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.customer.components.CardSkeletonLoader
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomNavigation
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomTab
import com.sucharu.sucharupro.ui.customer.components.CustomerEmptyState
import com.sucharu.sucharupro.ui.customer.components.CustomerErrorState
import com.sucharu.sucharupro.ui.customer.components.MobileTopBar
import com.sucharu.sucharupro.ui.customer.components.QuickActionCard
import com.sucharu.sucharupro.ui.customer.components.StatusChip
import com.sucharu.sucharupro.ui.customer.components.SucharuWallCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Mobile-First Affiliate My Area / Business Activity Center Screen.
 *
 * Renders authenticated affiliate partner workspace:
 * - Partner Profile & Identity
 * - Referral Center & Shareable Link
 * - Performance & Commission Summary
 * - Module 23 Wallet Snapshot & Payout Requests
 * - Active Campaigns & Bonus Boosters
 */
@Composable
fun AffiliateMyAreaScreen(
    viewModel: AffiliateMyAreaViewModel,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (destination: AppDestination) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(CustomerBottomTab.ACCOUNT) }

    LaunchedEffect(principal) {
        viewModel.loadMyArea(principal)
    }

    CustomerTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = CustomerTheme.colors.background,
            topBar = {
                MobileTopBar(
                    title = "Affiliate Business Center",
                    onBackClick = onNavigateBack
                )
            },
            bottomBar = {
                CustomerBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelect = { tab ->
                        selectedTab = tab
                        when (tab) {
                            CustomerBottomTab.HOME -> onNavigateToDestination(AppDestination.Public.Home)
                            CustomerBottomTab.ACCOUNT -> { /* Stay on My Area */ }
                        }
                    },
                    userRole = principal?.role
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = CustomerTheme.spacing.lg)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))

                when (val state = uiState) {
                    is AffiliateMyAreaUiState.Loading -> {
                        CardSkeletonLoader()
                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        CardSkeletonLoader()
                    }

                    is AffiliateMyAreaUiState.Error -> {
                        CustomerErrorState(
                            errorMessage = state.errorMessage,
                            onRetry = { viewModel.refresh(principal) }
                        )
                    }

                    is AffiliateMyAreaUiState.Empty -> {
                        CustomerEmptyState(
                            message = state.message,
                            subtitle = "You do not have any active referral commissions on record."
                        )
                    }

                    is AffiliateMyAreaUiState.Success -> {
                        val summary = state.summary

                        // 1. Partner Profile Card
                        AffiliateProfileCard(profile = summary.profile)

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 2. Referral Center Card
                        AffiliateReferralCenterCard(referrals = summary.referrals)

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 3. Quick Action Buttons
                        AffiliateQuickActionsSection(onNavigateToDestination = onNavigateToDestination)

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 4. Performance & Commission Summary
                        AffiliatePerformanceCard(performance = summary.performance)

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 5. Module 23 Wallet Snapshot & Payouts
                        AffiliateWalletCard(
                            wallet = summary.wallet,
                            onNavigateToDestination = onNavigateToDestination
                        )

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 6. Active Campaigns Card
                        AffiliateCampaignsSection(campaigns = summary.campaigns)

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.xxl))
                    }
                }
            }
        }
    }
}

@Composable
private fun AffiliateProfileCard(profile: AffiliateProfileInfo) {
    SucharuWallCard(
        containerColor = CustomerTheme.colors.surface,
        borderColor = CustomerTheme.colors.border
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = CustomerTheme.colors.accentPrimary)
                    Spacer(modifier = Modifier.width(CustomerTheme.spacing.sm))
                    Text(text = profile.displayName, style = CustomerTheme.typography.title, color = CustomerTheme.colors.primaryText)
                }
                StatusChip(label = profile.status, dotColor = CustomerTheme.colors.success)
            }
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = CustomerTheme.colors.secondaryText)
                Spacer(modifier = Modifier.width(CustomerTheme.spacing.xs))
                Text(text = profile.primaryPhone, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = CustomerTheme.colors.secondaryText)
                Spacer(modifier = Modifier.width(CustomerTheme.spacing.xs))
                Text(text = profile.email, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
            }
        }
    }
}

@Composable
private fun AffiliateReferralCenterCard(referrals: ReferralCenterSummary) {
    SucharuWallCard(
        containerColor = CustomerTheme.colors.elevatedSurface,
        borderColor = CustomerTheme.colors.border
    ) {
        Column {
            Text("Referral Link & Code", style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Referral Code: ${referrals.referralCode}", style = CustomerTheme.typography.bodyBold, color = CustomerTheme.colors.accentPrimary)
                    Text(referrals.referralLink, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                }
                Icon(Icons.Default.Share, contentDescription = "Share", tint = CustomerTheme.colors.accentPrimary)
            }
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Referrals", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text("${referrals.totalReferralsCount}", style = CustomerTheme.typography.title, color = CustomerTheme.colors.primaryText)
                }
                Column {
                    Text("Active Customers", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text("${referrals.activeReferredCustomersCount}", style = CustomerTheme.typography.title, color = CustomerTheme.colors.success)
                }
                Column {
                    Text("Total Orders", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text("${referrals.totalReferredOrdersCount}", style = CustomerTheme.typography.title, color = CustomerTheme.colors.accentPrimary)
                }
            }
        }
    }
}

@Composable
private fun AffiliateQuickActionsSection(onNavigateToDestination: (AppDestination) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)
    ) {
        QuickActionCard(
            label = "Share Link",
            icon = Icons.Default.Share,
            onClick = { onNavigateToDestination(AppDestination.Affiliate.ReferralLinks) },
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            label = "Referrals",
            icon = Icons.Default.Campaign,
            onClick = { onNavigateToDestination(AppDestination.Affiliate.Referrals) },
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            label = "Commissions",
            icon = Icons.Default.LocalOffer,
            onClick = { onNavigateToDestination(AppDestination.Affiliate.Commission) },
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            label = "Payouts",
            icon = Icons.Default.Wallet,
            onClick = { onNavigateToDestination(AppDestination.Affiliate.Payouts) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AffiliatePerformanceCard(performance: PerformanceCommissionSummary) {
    SucharuWallCard(
        containerColor = CustomerTheme.colors.surface,
        borderColor = CustomerTheme.colors.success
    ) {
        Column {
            Text("Commissions Summary", style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Earned", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text(performance.totalEarnedFormatted, style = CustomerTheme.typography.title, color = CustomerTheme.colors.primaryText)
                }
                Column {
                    Text("Approved", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text(performance.approvedCommissionFormatted, style = CustomerTheme.typography.title, color = CustomerTheme.colors.success)
                }
                Column {
                    Text("Disbursed Paid", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text(performance.paidDisbursedFormatted, style = CustomerTheme.typography.title, color = CustomerTheme.colors.accentPrimary)
                }
            }
        }
    }
}

@Composable
private fun AffiliateWalletCard(
    wallet: WalletSnapshot,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    SucharuWallCard(
        containerColor = CustomerTheme.colors.elevatedSurface,
        borderColor = CustomerTheme.colors.border
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wallet, contentDescription = null, tint = CustomerTheme.colors.accentPrimary)
                    Spacer(modifier = Modifier.width(CustomerTheme.spacing.sm))
                    Text("Module 23 Wallet", style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
                }
                StatusChip(label = wallet.walletId, dotColor = CustomerTheme.colors.accentPrimary)
            }
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Available to Withdraw", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text(wallet.availableToWithdrawFormatted, style = CustomerTheme.typography.number, color = CustomerTheme.colors.success)
                }
                Column {
                    Text("Held / Reserve", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Text(wallet.heldReserveFormatted, style = CustomerTheme.typography.title, color = CustomerTheme.colors.warning)
                }
            }
        }
    }
}

@Composable
private fun AffiliateCampaignsSection(campaigns: List<AffiliateCampaignItem>) {
    Column {
        Text("Active Partner Campaigns", style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
        Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))

        campaigns.forEach { camp ->
            SucharuWallCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(camp.title, style = CustomerTheme.typography.bodyBold, color = CustomerTheme.colors.primaryText)
                        StatusChip(label = camp.commissionRateLabel, dotColor = CustomerTheme.colors.accentPrimary)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(camp.description, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
                    Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))
                    Text("Valid Until: ${camp.validUntilFormatted}", style = CustomerTheme.typography.caption, color = CustomerTheme.colors.mutedText)
                }
            }
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
        }
    }
}
