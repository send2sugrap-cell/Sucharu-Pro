package com.sucharu.sucharupro.ui.features.dashboard.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * 1-Tap Quick Actions bar for frequent shop operations.
 *
 * Prepared for role-aware visibility: displays only operations relevant
 * to the authenticated [userRole] (when provided).
 */
@Composable
fun DashboardQuickActions(
    onNewOrderClick: () -> Unit,
    onNewCustomerClick: () -> Unit,
    onPrintJobClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onCreateInvoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    userRole: UserRole? = null
) {
    val showOrders = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.DESIGNER, UserRole.WAREHOUSE)
    val showCalculator = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.DESIGNER, UserRole.QC_INSPECTOR)
    val showCustomers = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.ACCOUNTS, UserRole.STAFF)
    val showPayments = userRole == null || userRole.hasFinancialAccess
    val showInvoices = userRole == null || userRole.hasFinancialAccess

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Primary Action: New Order
        if (showOrders) {
            AppButton(
                text = "New Order",
                onClick = onNewOrderClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        // Shortcut: Print Cost Calculator
        if (showCalculator) {
            AppOutlinedButton(
                text = "Print Calculator",
                onClick = onPrintJobClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        // Shortcut: New Customer
        if (showCustomers) {
            AppOutlinedButton(
                text = "Add Customer",
                onClick = onNewCustomerClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        // Shortcut: Record Payment
        if (showPayments) {
            AppOutlinedButton(
                text = "Record Payment",
                onClick = onRecordPaymentClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        // Shortcut: Create Invoice
        if (showInvoices) {
            AppOutlinedButton(
                text = "Create Invoice",
                onClick = onCreateInvoiceClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}
