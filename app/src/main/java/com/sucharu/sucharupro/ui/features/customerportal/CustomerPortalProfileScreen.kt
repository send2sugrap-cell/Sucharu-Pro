package com.sucharu.sucharupro.ui.features.customerportal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.CustomerProfileDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalProfileScreen(
    profile: CustomerProfileDto?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        },
        containerColor = Color(0xFF0A0F1D)
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (profile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Profile information unavailable.", color = Color.LightGray)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(profile.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("Code: ${profile.customerCode}", color = Color.LightGray, fontSize = 13.sp)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF334155))

                        ProfileDetailRow(label = "Company Name", value = profile.companyName ?: "N/A")
                        ProfileDetailRow(label = "Primary Phone", value = profile.phone ?: "N/A")
                        ProfileDetailRow(label = "Email Address", value = profile.email ?: "N/A")
                        ProfileDetailRow(label = "Credit Limit", value = "৳${profile.creditLimit}")
                        ProfileDetailRow(label = "Current Outstanding", value = "৳${profile.currentBalance}")
                        ProfileDetailRow(label = "Account Status", value = profile.status)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.LightGray, fontSize = 14.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
