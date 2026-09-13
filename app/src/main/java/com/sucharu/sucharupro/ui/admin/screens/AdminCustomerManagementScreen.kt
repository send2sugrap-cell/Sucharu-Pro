package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminBadge
import com.sucharu.sucharupro.ui.admin.components.AdminButton
import com.sucharu.sucharupro.ui.admin.components.AdminButtonStyle
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminKpiCard
import com.sucharu.sucharupro.ui.admin.components.AdminSection
import com.sucharu.sucharupro.ui.admin.components.AdminStatusChip
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.admin.workspace.AdminModuleWorkspaceContainer
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Unified Admin Workspace for Module 02 Customer Management.
 */
@Composable
fun AdminCustomerManagementScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    AdminModuleWorkspaceContainer(
        title = "Customer Management Directory",
        subtitle = "Master customer identity, credit profiles, and contact addresses",
        icon = Icons.Default.Group,
        canonicalModule = "Module 02",
        requiredCapability = "STAFF_READ_CUSTOMERS",
        principal = principal,
        modifier = modifier,
        actionSlot = {
            AdminButton(
                text = "New Customer",
                onClick = { onNavigateToDestination(AppDestination.Customer.Orders) },
                style = AdminButtonStyle.PRIMARY,
                icon = Icons.Default.Add
            )
        }
    ) {
        // KPI Summary Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Total Active Customers",
                value = "1,450 Accounts",
                trendDeltaPercentage = 5.2,
                subtitle = "Commercial accounts",
                icon = Icons.Default.Person,
                accentColor = AdminTheme.colors.accentPrimary,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Credit Risk Rating",
                value = "98.1% Low Risk",
                subtitle = "Credit limit enforced",
                icon = Icons.Default.Group,
                accentColor = AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        // Directory Table / List
        AdminSection(
            title = "Customer Directory Records",
            subtitle = "Active managed commercial accounts and contact profiles"
        ) {
            AdminCard {
                Column(verticalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)) {
                    CustomerRowItem("CUS-001", "Acme Printing Corporation", "+8801700000001", "ACTIVE")
                    CustomerRowItem("CUS-002", "Apex Digital Media Ltd", "+8801800000002", "ACTIVE")
                    CustomerRowItem("CUS-003", "Green Tech Creators", "+8801900000003", "ACTIVE")
                }
            }
        }
    }
}

@Composable
private fun CustomerRowItem(
    code: String,
    name: String,
    phone: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdminBadge(text = code)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(name, style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
            }
            Text("Phone: $phone", style = AdminTheme.typography.caption, color = AdminTheme.colors.secondaryText)
        }
        AdminStatusChip(label = status, dotColor = AdminTheme.colors.success)
    }
}
