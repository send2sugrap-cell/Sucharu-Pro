package com.sucharu.sucharupro.ui.features.finance.adjustment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.CustomerCreditNote
import com.sucharu.sucharupro.domain.model.finance.VendorDebitNote
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialAdjustmentRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCreditNoteDetailsScreen(
    adjustmentRepository: FinancialAdjustmentRepository,
    creditNoteId: String,
    callerRole: UserRole,
    onNavigateBack: () -> Unit,
    authenticatedCustomerId: String? = null,
    modifier: Modifier = Modifier
) {
    var creditNote by remember { mutableStateOf<CustomerCreditNote?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    remember(creditNoteId) {
        scope.launch {
            isLoading = true
            val res = adjustmentRepository.getCreditNoteById(creditNoteId, callerRole, authenticatedCustomerId)
            if (res is DomainResult.Success) {
                creditNote = res.data
            } else if (res is DomainResult.Error) {
                errorMessage = res.message
            }
            isLoading = false
        }
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(creditNote?.let { "Credit Note #${it.creditNoteNo}" } ?: "Credit Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            creditNote != null -> {
                val cn = creditNote!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Customer #${cn.customerId}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "IMMUTABLE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF16A34A))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "${cn.amount.formatted()} ${cn.currency}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Reason: ${cn.reason}", fontSize = 13.sp)
                            Text(text = "Reference: ${cn.referenceType.name} #${cn.referenceId}", fontSize = 13.sp)
                            Text(text = "Issued by: ${cn.issuedBy}", fontSize = 12.sp)
                            Text(text = "Issued at: ${dateFormat.format(Date(cn.issuedAt))}", fontSize = 12.sp)
                            Text(
                                text = "Ledger Transaction: #${cn.financialTransactionId}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDebitNoteDetailsScreen(
    adjustmentRepository: FinancialAdjustmentRepository,
    debitNoteId: String,
    callerRole: UserRole,
    onNavigateBack: () -> Unit,
    authenticatedVendorId: String? = null,
    modifier: Modifier = Modifier
) {
    var debitNote by remember { mutableStateOf<VendorDebitNote?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    remember(debitNoteId) {
        scope.launch {
            isLoading = true
            val res = adjustmentRepository.getDebitNoteById(debitNoteId, callerRole, authenticatedVendorId)
            if (res is DomainResult.Success) {
                debitNote = res.data
            } else if (res is DomainResult.Error) {
                errorMessage = res.message
            }
            isLoading = false
        }
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(debitNote?.let { "Debit Note #${it.debitNoteNo}" } ?: "Debit Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            debitNote != null -> {
                val dn = debitNote!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Vendor #${dn.vendorId}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "IMMUTABLE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFDC2626))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "${dn.amount.formatted()} ${dn.currency}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Reason: ${dn.reason}", fontSize = 13.sp)
                            Text(text = "Reference: ${dn.referenceType.name} #${dn.referenceId}", fontSize = 13.sp)
                            Text(text = "Issued by: ${dn.issuedBy}", fontSize = 12.sp)
                            Text(text = "Issued at: ${dateFormat.format(Date(dn.issuedAt))}", fontSize = 12.sp)
                            Text(
                                text = "Ledger Transaction: #${dn.financialTransactionId}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
