package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.data.model.Group
import com.example.data.model.Member
import com.example.ui.components.DeaconHeader
import com.example.ui.components.ExcelConfigDialog
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.BurgundyDark
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.ui.viewmodel.DeaconsViewModel
import com.example.util.ExcelExportConfig
import com.example.util.ExcelExporter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: DeaconsViewModel,
    onSelectMember: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val members by viewModel.members.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val attendances by viewModel.attendances.collectAsStateWithLifecycle()
    val hymns by viewModel.hymns.collectAsStateWithLifecycle()
    val hymnAssessments by viewModel.hymnAssessments.collectAsStateWithLifecycle()
    val bibleAssessments by viewModel.bibleAssessments.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val examResults by viewModel.examResults.collectAsStateWithLifecycle()

    val exportConfig by viewModel.exportConfig.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val lastExportedFile by viewModel.lastExportedFile.collectAsStateWithLifecycle()
    val canExportReports by viewModel.canExportReports.collectAsStateWithLifecycle()

    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()
    val importError by viewModel.importErrorMessage.collectAsStateWithLifecycle()

    var showConfigDialog by remember { mutableStateOf(false) }
    var showSuccessExportDialog by remember { mutableStateOf(false) }
    var memberDropdownExpanded by remember { mutableStateOf(false) }

    // Preview Sheet Tab
    var previewSheetTab by remember { mutableIntStateOf(0) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.parseExcelForImport(context, uri)
        }
    }

    val previewData = remember(
        exportConfig,
        members,
        groups,
        attendances,
        hymns,
        hymnAssessments,
        bibleAssessments,
        exams,
        examResults
    ) {
        ExcelExporter.generateInAppPreview(
            config = exportConfig,
            members = members,
            groups = groups,
            attendances = attendances,
            hymns = hymns,
            hymnAssessments = hymnAssessments,
            bibleAssessments = bibleAssessments,
            exams = exams,
            examResults = examResults
        )
    }

    val periodTabs = listOf(
        "شهر" to ExcelExporter.PERIOD_MONTH,
        "4 أشهر (التيرم)" to ExcelExporter.PERIOD_4_MONTHS,
        "نصف سنة (6 أشهر)" to ExcelExporter.PERIOD_HALF_YEAR,
        "سنة كاملة" to ExcelExporter.PERIOD_YEAR,
        "فترة مخصصة" to ExcelExporter.PERIOD_CUSTOM
    )

    val sheetTitles = listOf(
        "التقرير المخصص الشامل",
        "حضور الخدمة",
        "حضور القداسات",
        "الألحان",
        "الإنجيل",
        "الامتحانات",
        "سجل المخدومين"
    )

    val groupMap = remember(groups) { groups.associateBy { it.id } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            DeaconHeader(
                title = "نظام وتقارير Excel الاحترافية",
                subtitle = "مدرسة القديس اسطفانوس — تخصيص الأعمدة، تصدير، استيراد، ومعاينة حية"
            )
        }

        // Top Action Bar: Customization + Export + Import
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "عمليات وإعدادات Excel المعتمدة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BurgundyPrimary
                        )

                        // Button to trigger full Config Dialog
                        OutlinedButton(
                            onClick = { showConfigDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BurgundyPrimary)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تخصيص الخانات والأوراق", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.exportFullReport(context, exportConfig) {
                                showSuccessExportDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                        enabled = !isExporting && canExportReports
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري استخراج وتجهيز التقرير...", fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "تصدير",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (canExportReports) "تصدير ملف Excel المعتمد (.xlsx)" else "تصدير Excel (غير مصرح لهذا الحساب)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (!canExportReports) {
                        Text(
                            text = "ملاحظة: تصدير التقارير متاح فقط لمسؤول النظام أو الحسابات ذات صلاحية التصدير.",
                            fontSize = 11.sp,
                            color = AbsentRed,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BurgundyPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "استيراد",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استيراد مخدومين", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.downloadExcelTemplate(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00796B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "نموذج",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحميل النموذج", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Quick Period Selection Tabs
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "الفترة التقييمية للتقرير:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                ScrollableTabRow(
                    selectedTabIndex = periodTabs.indexOfFirst { it.second == exportConfig.periodType }.coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = BurgundyPrimary,
                    divider = {}
                ) {
                    periodTabs.forEach { (label, type) ->
                        val selected = exportConfig.periodType == type
                        Tab(
                            selected = selected,
                            onClick = { viewModel.setExportConfig(exportConfig.copy(periodType = type)) },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selected) BurgundyPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }

        // Custom Date Pickers (if Custom Period is selected)
        if (exportConfig.periodType == ExcelExporter.PERIOD_CUSTOM) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = exportConfig.customStartDate,
                        onValueChange = { viewModel.setExportConfig(exportConfig.copy(customStartDate = it)) },
                        label = { Text("من تاريخ (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exportConfig.customEndDate,
                        onValueChange = { viewModel.setExportConfig(exportConfig.copy(customEndDate = it)) },
                        label = { Text("إلى تاريخ (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Scope Filter Card (Group and Member)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "نطاق التقرير والتصفية السريعة:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = exportConfig.groupId == null && exportConfig.memberId == null,
                                onClick = {
                                    viewModel.setExportConfig(exportConfig.copy(groupId = null, memberId = null))
                                },
                                label = { Text("جميع المجموعات") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        items(groups) { group ->
                            FilterChip(
                                selected = exportConfig.groupId == group.id,
                                onClick = {
                                    if (exportConfig.groupId == group.id) {
                                        viewModel.setExportConfig(exportConfig.copy(groupId = null))
                                    } else {
                                        viewModel.setExportConfig(exportConfig.copy(groupId = group.id, memberId = null))
                                    }
                                },
                                label = { Text(group.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // School Class Multi-Filter Row
                    val reportClassList = listOf(
                        "أولى ابتدائي",
                        "ثانية ابتدائي",
                        "ثالثة ابتدائي",
                        "رابعة ابتدائي",
                        "خامسة ابتدائي",
                        "سادسة ابتدائي",
                        "أولى إعدادي",
                        "ثانية إعدادي",
                        "ثالثة إعدادي",
                        "أولى ثانوي",
                        "ثانية ثانوي",
                        "ثالثة ثانوي"
                    )
                    val selectedClasses = exportConfig.classFilters
                    val isAllClassesSelected = selectedClasses.isEmpty() && exportConfig.classFilter == null

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = isAllClassesSelected,
                                onClick = {
                                    viewModel.setExportConfig(exportConfig.copy(classFilter = null, classFilters = emptySet()))
                                    viewModel.setReportSelectedClasses(emptySet())
                                },
                                label = { Text("كل الفصول") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        items(reportClassList) { cls ->
                            val isSelected = selectedClasses.contains(cls) || exportConfig.classFilter == cls
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newSet = if (isSelected) {
                                        selectedClasses - cls
                                    } else {
                                        selectedClasses + cls
                                    }
                                    viewModel.setExportConfig(exportConfig.copy(classFilter = null, classFilters = newSet))
                                    viewModel.setReportSelectedClasses(newSet)
                                },
                                label = { Text(cls) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val groupMembers = if (exportConfig.groupId != null) {
                        members.filter { it.groupId == exportConfig.groupId }
                    } else members

                    ExposedDropdownMenuBox(
                        expanded = memberDropdownExpanded,
                        onExpandedChange = { memberDropdownExpanded = !memberDropdownExpanded }
                    ) {
                        val selectedName = members.find { it.id == exportConfig.memberId }?.fullName ?: "كل مخدومي النطاق (${groupMembers.size})"
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المخدوم المستهدف") },
                            trailingIcon = {
                                if (exportConfig.memberId != null) {
                                    IconButton(onClick = { viewModel.setExportConfig(exportConfig.copy(memberId = null)) }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح")
                                    }
                                } else {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberDropdownExpanded)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = memberDropdownExpanded,
                            onDismissRequest = { memberDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("جميع المخدومين (إلغاء التحديد الفردي)") },
                                onClick = {
                                    viewModel.setExportConfig(exportConfig.copy(memberId = null))
                                    memberDropdownExpanded = false
                                }
                            )
                            groupMembers.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.fullName) },
                                    onClick = {
                                        viewModel.setExportConfig(exportConfig.copy(memberId = m.id, groupId = null))
                                        memberDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Preview Header & Statistics Summary Card
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BurgundyDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = previewData.reportTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${previewData.periodLabel} (${previewData.periodDates})",
                                color = GoldSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldSecondary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${previewData.totalMembers} مخدوم",
                                color = GoldSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("حضور الخدمة", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("${previewData.overallServiceAttendanceRate}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("حضور القداس", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("${previewData.overallMassAttendanceCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("متوسط الألحان", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(
                                if (previewData.overallHymnsAverage > 0) String.format(Locale.US, "%.1f", previewData.overallHymnsAverage) else "-",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("متوسط الإنجيل", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(
                                if (previewData.overallBibleAverage > 0) String.format(Locale.US, "%.1f", previewData.overallBibleAverage) else "-",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Sheet Selector Tabs for In-App Excel Preview
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    tint = BurgundyPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "المعاينة الحية لملف Excel (In-App Live Preview)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BurgundyPrimary
                )
            }

            ScrollableTabRow(
                selectedTabIndex = previewSheetTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = BurgundyPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                sheetTitles.forEachIndexed { idx, title ->
                    val isTabSelected = previewSheetTab == idx
                    Tab(
                        selected = isTabSelected,
                        onClick = { previewSheetTab = idx },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        // In-App Excel Preview Table
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val horizontalScrollState = rememberScrollState()

                if (previewData.rows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد بيانات مطابقة للنطاق والفترة المحددة.",
                            color = Color(0xFF555555),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                            .padding(8.dp)
                    ) {
                        when (previewSheetTab) {
                            0 -> RenderDynamicConfiguredPreviewTable(previewData, onSelectMember)
                            1 -> RenderAttendancePreviewTable(attendances.filter { it.type == AttendanceType.SERVICE }, members, groupMap)
                            2 -> RenderMassPreviewTable(attendances.filter { it.type == AttendanceType.MASS }, members, groupMap)
                            3 -> RenderHymnsPreviewTable(hymnAssessments, hymns, members)
                            4 -> RenderBiblePreviewTable(bibleAssessments, members)
                            5 -> RenderExamsPreviewTable(examResults, exams, members)
                            6 -> RenderMembersPreviewTable(members.filter { m ->
                                (exportConfig.groupId == null || m.groupId == exportConfig.groupId) &&
                                        (exportConfig.memberId == null || m.id == exportConfig.memberId)
                            }, groupMap, onSelectMember)
                        }
                    }
                }
            }
        }

        // Footer Copyright & School Info
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F9)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "مدرسة القديس اسطفانوس للشمامسة بمير",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BurgundyPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "كنيسة أبي سيفين والعزب ودير الملاك ميخائيل — إيبارشية القوصية ومير",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "© 2026 Kirolos Sabry Fouad. All Rights Reserved.",
                        fontSize = 11.sp,
                        color = Color(0xFF888888),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // -------------------------------------------------------------
    // Excel Configuration Dialog (Full Customizable Columns & Sheets)
    // -------------------------------------------------------------
    if (showConfigDialog) {
        ExcelConfigDialog(
            initialConfig = exportConfig,
            members = members,
            groups = groups,
            onDismiss = { showConfigDialog = false },
            onApply = { newConfig ->
                viewModel.setExportConfig(newConfig)
            },
            onExportDirect = { directConfig ->
                viewModel.setExportConfig(directConfig)
                viewModel.exportFullReport(context, directConfig) {
                    showSuccessExportDialog = true
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Export Success Dialog
    // -------------------------------------------------------------
    if (showSuccessExportDialog && lastExportedFile != null) {
        val file = lastExportedFile!!
        AlertDialog(
            onDismissRequest = { showSuccessExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PresentGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تم استخراج التقرير بنجاح", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "تم إنشاء ملف Excel الشامل بنجاح بحسب الخانات والأوراق المحددة.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اسم الملف: ${file.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BurgundyPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "حجم الملف: ${file.length() / 1024} KB",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ExcelExporter.openExcelFile(context, file)
                        showSuccessExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فتح في Excel")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        ExcelExporter.shareExcelFile(context, file)
                        showSuccessExportDialog = false
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Import Preview Dialog
    // -------------------------------------------------------------
    if (importPreview != null) {
        val preview = importPreview!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportPreview() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = BurgundyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("معاينة استيراد بيانات Excel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "تم التعرف على ${preview.totalRows} صف في الملف (${preview.validCount} صحيح، ${preview.invalidCount} يحتاج تدقيق).",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (preview.allErrors.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AbsentRed.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "تنبيهات وملاحظات التحقق (${preview.allErrors.size}):",
                                    color = AbsentRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                preview.allErrors.take(3).forEach { err ->
                                    Text("• $err", fontSize = 11.sp, color = AbsentRed)
                                }
                                if (preview.allErrors.size > 3) {
                                    Text("• ... والمزيد", fontSize = 11.sp, color = AbsentRed)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = "الأعمدة المكتشفة: ${preview.detectedColumns.joinToString("، ")}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("معاينة لأول المخدومين:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    preview.previewRows.take(4).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(row.fullName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(row.groupName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmImportMembers(ignoreInvalidRows = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("تأكيد واستيراد (${preview.validCount})")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportPreview() }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Import Result Summary Dialog
    // -------------------------------------------------------------
    if (importResult != null) {
        val result = importResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PresentGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نتيجة الاستيراد", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(result.summaryMessage, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• مخدومين جدد مضافين: ${result.importedCount}", fontSize = 13.sp, color = PresentGreen)
                    Text("• مخدومين تم تحديثهم: ${result.updatedCount}", fontSize = 13.sp, color = GoldSecondary)
                    Text("• صفوف تم تجاهلها: ${result.ignoredCount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissImportResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("حسناً")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Import Error Dialog
    // -------------------------------------------------------------
    if (importError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportPreview() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AbsentRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("خطأ أثناء الاستيراد", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(importError ?: "") },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissImportPreview() },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("حسناً")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Dynamic In-App Excel Preview Renderers
// -------------------------------------------------------------

@Composable
private fun RenderDynamicConfiguredPreviewTable(
    previewData: ExcelExporter.InAppExcelPreview,
    onSelectMember: (Long) -> Unit
) {
    Column {
        // Table Header
        Row(
            modifier = Modifier
                .background(BurgundyPrimary, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            previewData.columns.forEach { col ->
                val colWidth = (col.width * 8).coerceIn(40, 200).dp
                TableCell(text = col.title, width = colWidth, isHeader = true)
            }
        }

        // Table Body
        previewData.rows.forEachIndexed { index, row ->
            val bgColor = if (index % 2 == 0) Color.White else Color(0xFFF4F5F7)
            Row(
                modifier = Modifier
                    .background(bgColor)
                    .clickable { onSelectMember(row.memberId) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                previewData.columns.forEach { col ->
                    val colWidth = (col.width * 8).coerceIn(40, 200).dp
                    val cellVal = row.values[col.key] ?: "-"
                    val isBold = col.key == "memberName" || col.key == "totalPercentage" || col.key == "overallGrade"
                    val cellColor = when (col.key) {
                        "memberName" -> Color(0xFF000000)
                        "overallGrade" -> Color(0xFF1B5E20)
                        "totalPercentage" -> Color(0xFF720026)
                        else -> Color(0xFF000000)
                    }
                    TableCell(text = cellVal, width = colWidth, isBold = isBold, color = cellColor)
                }
            }
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun RenderAttendancePreviewTable(
    attendances: List<com.example.data.model.Attendance>,
    members: List<Member>,
    groupMap: Map<Long, Group>
) {
    val memberMap = members.associateBy { it.id }
    Column {
        Row(
            modifier = Modifier
                .background(BurgundyPrimary, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "م", width = 36.dp, isHeader = true)
            TableCell(text = "التاريخ", width = 110.dp, isHeader = true)
            TableCell(text = "اسم المخدوم", width = 160.dp, isHeader = true)
            TableCell(text = "المجموعة", width = 120.dp, isHeader = true)
            TableCell(text = "الحالة", width = 90.dp, isHeader = true)
            TableCell(text = "الملاحظات", width = 160.dp, isHeader = true)
        }

        attendances.take(50).forEachIndexed { idx, item ->
            val member = memberMap[item.memberId]
            val statusText = when (item.status) {
                AttendanceStatus.PRESENT -> "حاضر"
                AttendanceStatus.ABSENT -> "غائب"
                AttendanceStatus.EXCUSED -> "عذر"
            }
            val statusColor = when (item.status) {
                AttendanceStatus.PRESENT -> Color(0xFF1B5E20)
                AttendanceStatus.ABSENT -> Color(0xFFC62828)
                AttendanceStatus.EXCUSED -> Color(0xFF9E7D0A)
            }
            val bgColor = if (idx % 2 == 0) Color.White else Color(0xFFF4F5F7)
            Row(
                modifier = Modifier
                    .background(bgColor)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = "${idx + 1}", width = 36.dp, color = Color(0xFF000000))
                TableCell(text = item.date, width = 110.dp, color = Color(0xFF000000))
                TableCell(text = member?.fullName ?: "مخدوم رقم ${item.memberId}", width = 160.dp, isBold = true, color = Color(0xFF000000))
                TableCell(text = member?.groupId?.let { groupMap[it]?.name } ?: "عام", width = 120.dp, color = Color(0xFF000000))
                TableCell(text = statusText, width = 90.dp, color = statusColor, isBold = true)
                TableCell(text = item.notes.ifBlank { "-" }, width = 160.dp, color = Color(0xFF000000))
            }
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun RenderMassPreviewTable(
    attendances: List<com.example.data.model.Attendance>,
    members: List<Member>,
    groupMap: Map<Long, Group>
) {
    val memberMap = members.associateBy { it.id }
    Column {
        Row(
            modifier = Modifier
                .background(BurgundyPrimary, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "م", width = 36.dp, isHeader = true)
            TableCell(text = "التاريخ", width = 110.dp, isHeader = true)
            TableCell(text = "اسم المخدوم", width = 160.dp, isHeader = true)
            TableCell(text = "المجموعة", width = 120.dp, isHeader = true)
            TableCell(text = "رقم القداس", width = 100.dp, isHeader = true)
            TableCell(text = "الحالة", width = 90.dp, isHeader = true)
            TableCell(text = "الملاحظات", width = 150.dp, isHeader = true)
        }

        attendances.take(50).forEachIndexed { idx, item ->
            val member = memberMap[item.memberId]
            val massText = if (item.massNumber == 2) "القداس الثاني" else "القداس الأول"
            val statusText = if (item.status == AttendanceStatus.PRESENT) "حاضر" else "غائب"
            val bgColor = if (idx % 2 == 0) Color.White else Color(0xFFF4F5F7)

            Row(
                modifier = Modifier
                    .background(bgColor)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = "${idx + 1}", width = 36.dp, color = Color(0xFF000000))
                TableCell(text = item.date, width = 110.dp, color = Color(0xFF000000))
                TableCell(text = member?.fullName ?: "مخدوم رقم ${item.memberId}", width = 160.dp, isBold = true, color = Color(0xFF000000))
                TableCell(text = member?.groupId?.let { groupMap[it]?.name } ?: "عام", width = 120.dp, color = Color(0xFF000000))
                TableCell(text = massText, width = 100.dp, color = Color(0xFF000000))
                TableCell(text = statusText, width = 90.dp, color = if (item.status == AttendanceStatus.PRESENT) Color(0xFF1B5E20) else Color(0xFFC62828), isBold = true)
                TableCell(text = item.notes.ifBlank { "-" }, width = 150.dp, color = Color(0xFF000000))
            }
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun RenderHymnsPreviewTable(
    assessments: List<com.example.data.model.HymnAssessment>,
    hymns: List<com.example.data.model.Hymn>,
    members: List<Member>
) {
    val memberMap = members.associateBy { it.id }
    val hymnMap = hymns.associateBy { it.id }

    Column {
        Row(
            modifier = Modifier
                .background(BurgundyPrimary, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "م", width = 36.dp, isHeader = true)
            TableCell(text = "اسم المخدوم", width = 160.dp, isHeader = true)
            TableCell(text = "اسم اللحن", width = 150.dp, isHeader = true)
            TableCell(text = "التاريخ", width = 110.dp, isHeader = true)
            TableCell(text = "الدرجة", width = 70.dp, isHeader = true)
            TableCell(text = "النهائية", width = 70.dp, isHeader = true)
            TableCell(text = "النسبة %", width = 80.dp, isHeader = true)
        }

        assessments.take(50).forEachIndexed { idx, item ->
            val member = memberMap[item.memberId]
            val hymn = hymnMap[item.hymnId]
            val pct = if (item.maxScore > 0) (item.score / item.maxScore) * 100 else 0.0
            val bgColor = if (idx % 2 == 0) Color.White else Color(0xFFF4F5F7)

            Row(
                modifier = Modifier
                    .background(bgColor)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = "${idx + 1}", width = 36.dp, color = Color(0xFF000000))
                TableCell(text = member?.fullName ?: "مخدوم ${item.memberId}", width = 160.dp, isBold = true, color = Color(0xFF000000))
                TableCell(text = hymn?.name ?: "لحن ${item.hymnId}", width = 150.dp, color = Color(0xFF000000))
                TableCell(text = item.date, width = 110.dp, color = Color(0xFF000000))
                TableCell(text = "${item.score}", width = 70.dp, isBold = true, color = Color(0xFF720026))
                TableCell(text = "${item.maxScore}", width = 70.dp, color = Color(0xFF000000))
                TableCell(text = String.format(Locale.US, "%.0f%%", pct), width = 80.dp, isBold = true, color = Color(0xFF000000))
            }
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun RenderBiblePreviewTable(
    assessments: List<com.example.data.model.BibleAssessment>,
    members: List<Member>
) {
    val memberMap = members.associateBy { it.id }

    Column {
        Row(
            modifier = Modifier
                .background(BurgundyPrimary, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "م", width = 36.dp, isHeader = true)
            TableCell(text = "اسم المخدوم", width = 160.dp, isHeader = true)
            TableCell(text = "الدرس / الإصحاح", width = 180.dp, isHeader = true)
            TableCell(text = "التاريخ", width = 110.dp, isHeader = true)
            TableCell(text = "الدرجة", width = 70.dp, isHeader = true)
            TableCell(text = "النهائية", width = 70.dp, isHeader = true)
        }

        assessments.take(50).forEachIndexed { idx, item ->
            val member = memberMap[item.memberId]
            val bgColor = if (idx % 2 == 0) Color.White else Color(0xFFF4F5F7)

            Row(
                modifier = Modifier
                    .background(bgColor)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = "${idx + 1}", width = 36.dp, color = Color(0xFF000000))
                TableCell(text = member?.fullName ?: "مخدوم ${item.memberId}", width = 160.dp, isBold = true, color = Color(0xFF000000))
                TableCell(text = item.lessonName, width = 180.dp, color = Color(0xFF000000))
                TableCell(text = item.date, width = 110.dp, color = Color(0xFF000000))
                TableCell(text = "${item.score}", width = 70.dp, isBold = true, color = Color(0xFF720026))
                TableCell(text = "${item.maxScore}", width = 70.dp, color = Color(0xFF000000))
            }
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun RenderExamsPreviewTable(
    examResults: List<com.example.data.model.ExamResult>,
    exams: List<com.example.data.model.Exam>,
    members: List<Member>
) {
    val memberMap = members.associateBy { it.id }
    val examMap = exams.associateBy { it.id }

    Column {
        Row(
            modifier = Modifier
                .background(BurgundyPrimary, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "م", width = 36.dp, isHeader = true)
            TableCell(text = "اسم المخدوم", width = 160.dp, isHeader = true)
            TableCell(text = "اسم الامتحان", width = 160.dp, isHeader = true)
            TableCell(text = "التاريخ", width = 110.dp, isHeader = true)
            TableCell(text = "الدرجة", width = 70.dp, isHeader = true)
            TableCell(text = "النهائية", width = 70.dp, isHeader = true)
        }

        examResults.take(50).forEachIndexed { idx, item ->
            val member = memberMap[item.memberId]
            val exam = examMap[item.examId]
            val bgColor = if (idx % 2 == 0) Color.White else Color(0xFFF4F5F7)

            Row(
                modifier = Modifier
                    .background(bgColor)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = "${idx + 1}", width = 36.dp, color = Color(0xFF000000))
                TableCell(text = member?.fullName ?: "مخدوم ${item.memberId}", width = 160.dp, isBold = true, color = Color(0xFF000000))
                TableCell(text = exam?.name ?: "امتحان ${item.examId}", width = 160.dp, color = Color(0xFF000000))
                TableCell(text = exam?.date ?: "-", width = 110.dp, color = Color(0xFF000000))
                TableCell(text = "${item.score}", width = 70.dp, isBold = true, color = Color(0xFF720026))
                TableCell(text = "${exam?.maxScore ?: 100.0}", width = 70.dp, color = Color(0xFF000000))
            }
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun RenderMembersPreviewTable(
    members: List<Member>,
    groupMap: Map<Long, Group>,
    onSelectMember: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .background(BurgundyPrimary, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "كود", width = 45.dp, isHeader = true)
            TableCell(text = "الاسم", width = 160.dp, isHeader = true)
            TableCell(text = "المجموعة", width = 120.dp, isHeader = true)
            TableCell(text = "تاريخ الميلاد", width = 110.dp, isHeader = true)
            TableCell(text = "الهاتف", width = 110.dp, isHeader = true)
            TableCell(text = "هاتف ولي الأمر", width = 110.dp, isHeader = true)
            TableCell(text = "المنطقة / الشارع", width = 160.dp, isHeader = true)
        }

        members.forEachIndexed { idx, item ->
            val bgColor = if (idx % 2 == 0) Color.White else Color(0xFFF4F5F7)
            val groupName = item.groupId?.let { groupMap[it]?.name } ?: "عام"
            val address = listOf(item.area, item.street, item.houseNumber).filter { it.isNotBlank() }.joinToString(" - ")

            Row(
                modifier = Modifier
                    .background(bgColor)
                    .clickable { onSelectMember(item.id) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = "${item.id}", width = 45.dp, color = Color(0xFF000000))
                TableCell(text = item.fullName, width = 160.dp, isBold = true, color = Color(0xFF000000))
                TableCell(text = groupName, width = 120.dp, color = Color(0xFF000000))
                TableCell(text = item.birthDate.ifBlank { "-" }, width = 110.dp, color = Color(0xFF000000))
                TableCell(text = item.phone.ifBlank { "-" }, width = 110.dp, color = Color(0xFF000000))
                TableCell(text = item.parentPhone.ifBlank { "-" }, width = 110.dp, color = Color(0xFF000000))
                TableCell(text = address.ifBlank { "-" }, width = 160.dp, color = Color(0xFF000000))
            }
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    color: Color? = null
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        textAlign = TextAlign.Center,
        fontSize = if (isHeader) 12.sp else 11.sp,
        fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
        color = when {
            isHeader -> Color.White
            color != null -> color
            else -> Color(0xFF000000)
        },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
