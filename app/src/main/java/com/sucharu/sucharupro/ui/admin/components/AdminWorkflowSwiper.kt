package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

private fun Int.toBengaliDigits(): String {
    val enDigits = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val bnDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    var str = this.toString()
    for (i in 0..9) {
        str = str.replace(enDigits[i], bnDigits[i])
    }
    return str
}

data class WorkflowNodeItem(
    val stageType: ProductionStageType,
    val title: String,
    val count: Int,
    val icon: ImageVector,
    val accentColor: Color
)

/**
 * Interactive 13-Stage Workflow Swiper / Carousel for Order-to-Delivery Pipeline.
 *
 * Renders all 13 canonical [ProductionStageType] stages in a horizontal swiper.
 * Tapping any stage opens [WorkflowStageDetailSheet] showing active jobs in that stage!
 */
@Composable
fun AdminWorkflowSwiper(
    modifier: Modifier = Modifier,
    onStageClick: (ProductionStageType) -> Unit = {}
) {
    var selectedStageForDetail by remember { mutableStateOf<WorkflowNodeItem?>(null) }

    val nodes = remember {
        listOf(
            WorkflowNodeItem(ProductionStageType.DESIGN, "ডিজাইন", 88, Icons.Default.Palette, Color(0xFF38BDF8)),
            WorkflowNodeItem(ProductionStageType.APPROVAL, "অনুমোদন", 61, Icons.Default.CheckCircle, Color(0xFF10B981)),
            WorkflowNodeItem(ProductionStageType.QC, "কিউসি", 38, Icons.Default.Shield, Color(0xFFF59E0B)),
            WorkflowNodeItem(ProductionStageType.ITEM_APPROVAL, "আইটেম প্রুফ", 22, Icons.Default.FactCheck, Color(0xFFA855F7)),
            WorkflowNodeItem(ProductionStageType.CTP, "সিটিপি", 18, Icons.Default.Edit, Color(0xFF00B4D8)),
            WorkflowNodeItem(ProductionStageType.PRINTING, "প্রিন্টিং", 38, Icons.Default.Print, Color(0xFF3B82F6)),
            WorkflowNodeItem(ProductionStageType.LAMINATION, "লেমিনেশন", 25, Icons.Default.Layers, Color(0xFFEC4899)),
            WorkflowNodeItem(ProductionStageType.FOLDING, "ফোল্ডিং", 18, Icons.Default.Engineering, Color(0xFF8B5CF6)),
            WorkflowNodeItem(ProductionStageType.BINDING, "বাইন্ডিং", 14, Icons.Default.Build, Color(0xFFF97316)),
            WorkflowNodeItem(ProductionStageType.FINAL_QC, "ফাইনাল কিউসি", 12, Icons.Default.Shield, Color(0xFFEF4444)),
            WorkflowNodeItem(ProductionStageType.PACKAGING, "প্যাকেজিং", 18, Icons.Default.Archive, Color(0xFFEAB308)),
            WorkflowNodeItem(ProductionStageType.READY, "রেডি", 24, Icons.Default.Check, Color(0xFF10B981)),
            WorkflowNodeItem(ProductionStageType.DELIVERED, "ডেলিভারড", 45, Icons.Default.LocalShipping, Color(0xFF64748B))
        )
    }

    if (selectedStageForDetail != null) {
        WorkflowStageDetailSheet(
            node = selectedStageForDetail!!,
            onDismiss = { selectedStageForDetail = null },
            onOpenStageWorkspace = { stage ->
                selectedStageForDetail = null
                onStageClick(stage)
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "অর্ডার থেকে ডেলিভারি (১৩টি ওয়ার্কফ্লো ধাপ)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Text(
                text = "স্লাইড করুন >",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(nodes) { node ->
                Surface(
                    modifier = Modifier
                        .width(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedStageForDetail = node
                        },
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, node.accentColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(node.accentColor.copy(alpha = 0.2f))
                                .border(1.dp, node.accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = node.icon,
                                contentDescription = node.title,
                                tint = node.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = node.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${node.count.toBengaliDigits()} টি জব",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = node.accentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Modal Bottom Sheet showing active jobs for a selected stage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowStageDetailSheet(
    node: WorkflowNodeItem,
    onDismiss: () -> Unit,
    onOpenStageWorkspace: (ProductionStageType) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B132B),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(node.accentColor.copy(alpha = 0.2f))
                            .border(1.dp, node.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = node.icon, contentDescription = null, tint = node.accentColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "${node.title} ধাপের কাজের বিবরণ", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "মোট চলমান কাজ: ${node.count.toBengaliDigits()} টি", fontSize = 11.sp, color = node.accentColor)
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "লাইভ জব ব্রেকডাউন:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "• অর্ডারের নাম: ভিজিটিং কার্ড ৫০০ পিস (বিডি মার্ট)", fontSize = 11.5.sp, color = Color.White)
                    Text(text = "  বর্তমান অগ্রগতি: ৭০% সম্পন্ন", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "• অর্ডারের নাম: রিজিড গিফট বক্স ১০০ পিস (সুচারু ব্র্যান্ড)", fontSize = 11.5.sp, color = Color.White)
                    Text(text = "  বর্তমান অগ্রগতি: ৪৫% সম্পন্ন", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenStageWorkspace(node.stageType) },
                color = Color(0xFF00B4D8)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "প্রোডাকশন মডিউলে বিস্তারিত দেখুন",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
