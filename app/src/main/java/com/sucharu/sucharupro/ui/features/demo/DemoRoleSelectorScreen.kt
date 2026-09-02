package com.sucharu.sucharupro.ui.features.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.composition.DemoRole

/**
 * Enterprise Development Demo Role Selector Screen (INFRA-06).
 *
 * Allows the owner/developer to select and launch any implemented role workspace
 * (Customer, Affiliate, Staff, Manager, Admin) in an isolated showcase environment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoRoleSelectorScreen(
    onSelectRole: (DemoRole) -> Unit,
    onBackToPublic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SUCHARU PRO ERP SHOWCASE",
                            color = Color(0xFF9ECAFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Development Mode • All-Role Capability Showcase",
                            color = Color(0xFFB7C8D8),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToPublic) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Public Home",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B132B)
                )
            )
        },
        containerColor = Color(0xFF0B132B),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Banner
            item {
                Surface(
                    color = Color(0xFF1C2541),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF00497D),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "ISOLATED SHOWCASE",
                                    color = Color(0xFF9ECAFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zero Production DB Access",
                                color = Color(0xFF8692A6),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Choose a Demo Experience",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Text(
                            text = "Select any role below to explore fully implemented dashboards, production tracking, financial governance, and workflows using synthetic in-memory fixtures.",
                            color = Color(0xFFB7C8D8),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Role Cards
            items(DemoRole.entries) { role ->
                DemoRoleCard(
                    role = role,
                    onClick = { onSelectRole(role) }
                )
            }

            // Bottom Guest Action
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onBackToPublic,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF9ECAFF)
                    )
                ) {
                    Text("Return to Public Home", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DemoRoleCard(
    role: DemoRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (role) {
        DemoRole.CUSTOMER -> Color(0xFF00B4D8)
        DemoRole.AFFILIATE -> Color(0xFFFFB703)
        DemoRole.STAFF -> Color(0xFF06D6A0)
        DemoRole.MANAGER -> Color(0xFF7209B7)
        DemoRole.ADMIN -> Color(0xFFE63946)
    }

    val badgeBackground = when (role) {
        DemoRole.CUSTOMER -> Color(0xFF00384D)
        DemoRole.AFFILIATE -> Color(0xFF4D3800)
        DemoRole.STAFF -> Color(0xFF003E2F)
        DemoRole.MANAGER -> Color(0xFF2E004E)
        DemoRole.ADMIN -> Color(0xFF4D0B12)
    }

    Surface(
        color = Color(0xFF1C2541),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = role.iconDescription,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Column {
                        Text(
                            text = role.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "User: ${role.demoUsername}",
                            color = Color(0xFF8692A6),
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = badgeBackground,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = role.roleBadge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = role.roleDescription,
                color = Color(0xFFB7C8D8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Highlight chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                role.highlightFeatures.take(2).forEach { feature ->
                    Surface(
                        color = Color(0xFF0B132B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "• $feature",
                            color = Color(0xFF9ECAFF),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Demo OTP: 123456",
                    color = Color(0xFF00B4D8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Enter Showcase",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
