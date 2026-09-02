package com.sucharu.sucharupro.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Mobile-First Affiliate Workspace Navigation Shell (INFRA-03 Step 06).
 */
@Composable
fun AffiliateWorkspaceShell(
    principal: AuthenticatedPrincipal,
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(16.dp)
    ) {
        // Affiliate Header Card
        Surface(
            color = Color(0xFF1C2541),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("AFFILIATE PARTNER PORTAL", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
                    Text("Partner: ${principal.username}", color = Color.White, fontSize = 12.sp)
                    Text("Affiliate ID: ${principal.userId}", color = Color(0xFFB7C8D8), fontSize = 10.sp)
                }
                Surface(color = Color(0xFF00497D), shape = RoundedCornerShape(8.dp)) {
                    Text("Verified Partner", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentDestination == AppDestination.Affiliate.Home,
                onClick = { onNavigate(AppDestination.Affiliate.Home) },
                label = { Text("Overview") }
            )
            FilterChip(
                selected = currentDestination == AppDestination.Affiliate.ReferralLinks,
                onClick = { onNavigate(AppDestination.Affiliate.ReferralLinks) },
                label = { Text("Links") }
            )
            FilterChip(
                selected = currentDestination == AppDestination.Affiliate.Referrals,
                onClick = { onNavigate(AppDestination.Affiliate.Referrals) },
                label = { Text("Referrals") }
            )
            FilterChip(
                selected = currentDestination == AppDestination.Affiliate.Commission,
                onClick = { onNavigate(AppDestination.Affiliate.Commission) },
                label = { Text("Commissions") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workspace Active Destination Content
        Surface(
            color = Color(0xFF1C2541),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                when (currentDestination) {
                    is AppDestination.Affiliate.ReferralLinks -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("UNIQUE REFERRAL LINKS", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
                            Text("Generate brand campaign links and track conversions.", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    is AppDestination.Affiliate.Commission -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COMMISSION & PAYOUTS", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
                            Text("Earned commissions, payout history, and balance statements.", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AFFILIATE PERFORMANCE CENTER", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
                            Text("Track referrals, convert orders, and view earnings", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
