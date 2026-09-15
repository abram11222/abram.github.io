package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Group
import com.example.data.model.Member
import com.example.ui.theme.BurgundyDark
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.util.ExcelExportConfig
import com.example.util.ExcelExporter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExcelConfigDialog(
    initialConfig: ExcelExportConfig,
    members: List<Member>,
    groups: List<Group>,
    onDismiss: () -> Unit,
    onApply: (ExcelExportConfig) -> Unit,
    onExportDirect: (ExcelExportConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }
    var memberDropdownExpanded by remember { mutableStateOf(false) }

    val periodOptions = listOf(
        "شهر واحد" to ExcelExporter.PERIOD_MONTH,
        "4 أشهر (التيرم)" to ExcelExporter.PERIOD_4_MONTHS,
        "نصف سنة (6 أشهر)" to ExcelExporter.PERIOD_HALF_YEAR,
        "سنة كاملة (12 شهر)" to ExcelExporter.PERIOD_YEAR,
        "فترة مخصصة" to ExcelExporter.PERIOD_CUSTOM
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Bar
                Surface(
                    color = BurgundyPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "إعداد وتخصيص تقرير Excel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "اختر النطاق، الفترة، والأعمدة المطلوبة للتصدير",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Target Scope Section
                    SectionCard(title = "1. نطاق التقرير والمخدومين", icon = Icons.Default.Groups) {
                        // Scope selector chips (All / Group / Member)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = config.memberId == null && config.groupId == null,
                                onClick = { config = config.copy(memberId = null, groupId = null) },
                                label = { Text("جميع المخدومين") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BurgundyPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Group Picker
                        Text("تصفية حسب المجموعة:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            groups.forEach { group ->
                                FilterChip(
                                    selected = config.groupId == group.id,
                                    onClick = {
                                        config = if (config.groupId == group.id) {
                                            config.copy(groupId = null)
                                        } else {
                                            config.copy(groupId = group.id, memberId = null)
                                        }
                                    },
                                    label = { Text(group.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BurgundyPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // School Class Filter
                        Text("تصفية حسب الفصل الدراسي:", style = MaterialTheme.typography.labelMedium)
                        val classOptions = listOf(
                            "الكل" to null,
                            "أولى ابتدائي" to "أولى ابتدائي",
                            "ثانية ابتدائي" to "ثانية ابتدائي",
                            "ثالثة ابتدائي" to "ثالثة ابتدائي",
                            "رابعة ابتدائي" to "رابعة ابتدائي",
                            "خامسة ابتدائي" to "خامسة ابتدائي",
                            "سادسة ابتدائي" to "سادسة ابتدائي",
                            "أولى إعدادي" to "أولى إعدادي",
                            "ثانية إعدادي" to "ثانية إعدادي",
                            "ثالثة إعدادي" to "ثالثة إعدادي",
                            "أولى ثانوي" to "أولى ثانوي"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            classOptions.forEach { (label, value) ->
                                FilterChip(
                                    selected = config.classFilter == value,
                                    onClick = { config = config.copy(classFilter = value) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BurgundyPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Individual Member Picker Dropdown
                        Text("أو اختيار مخدوم فردي بالاسم:", style = MaterialTheme.typography.labelMedium)
                        ExposedDropdownMenuBox(
                            expanded = memberDropdownExpanded,
                            onExpandedChange = { memberDropdownExpanded = !memberDropdownExpanded }
                        ) {
                            val currentMemberName = members.find { it.id == config.memberId }?.fullName ?: "جميع مخدومي النطاق الحالي"
                            OutlinedTextField(
                                value = currentMemberName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = memberDropdownExpanded,
                                onDismissRequest = { memberDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("جميع المخدومين (إلغاء التحديد الفردي)") },
                                    onClick = {
                                        config = config.copy(memberId = null)
                                        memberDropdownExpanded = false
                                    }
                                )
                                members.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m.fullName) },
                                        onClick = {
                                            config = config.copy(memberId = m.id, groupId = null)
                                            memberDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Period Selection Section
                    SectionCard(title = "2. الفترة التقييمية", icon = Icons.Default.DateRange) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            periodOptions.forEach { (label, type) ->
                                FilterChip(
                                    selected = config.periodType == type,
                                    onClick = { config = config.copy(periodType = type) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BurgundyPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        if (config.periodType == ExcelExporter.PERIOD_CUSTOM) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = config.customStartDate,
                                    onValueChange = { config = config.copy(customStartDate = it) },
                                    label = { Text("من تاريخ (YYYY-MM-DD)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = config.customEndDate,
                                    onValueChange = { config = config.copy(customEndDate = it) },
                                    label = { Text("إلى تاريخ (YYYY-MM-DD)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // 3. Sheet Structure Mode
                    SectionCard(title = "3. هيكل أوراق ملف Excel", icon = Icons.Default.TableChart) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (config.isMultiSheetMode) "تصدير أوراق عمل متعددة (Multi-Sheets)" else "تصدير ورقة شاملة واحدة (Single Sheet)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (config.isMultiSheetMode) "إنشاء صفحات منفصلة لكل من الحضور، الألحان، الإنجيل، والامتحانات" else "تجميع كافة الأعمدة المحددة داخل ورقة واحدة فقط",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.isMultiSheetMode,
                                onCheckedChange = { config = config.copy(isMultiSheetMode = it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BurgundyPrimary
                                )
                            )
                        }

                        if (config.isMultiSheetMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("الأوراق المطلوب تضمينها في الملف:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SheetCheckItem(label = "الملخص العام", isChecked = config.includeSummarySheet) {
                                    config = config.copy(includeSummarySheet = it)
                                }
                                SheetCheckItem(label = "حضور الخدمة", isChecked = config.includeAttendanceSheet) {
                                    config = config.copy(includeAttendanceSheet = it)
                                }
                                SheetCheckItem(label = "حضور القداسات", isChecked = config.includeMassSheet) {
                                    config = config.copy(includeMassSheet = it)
                                }
                                SheetCheckItem(label = "الألحان", isChecked = config.includeHymnsSheet) {
                                    config = config.copy(includeHymnsSheet = it)
                                }
                                SheetCheckItem(label = "الإنجيل", isChecked = config.includeBibleSheet) {
                                    config = config.copy(includeBibleSheet = it)
                                }
                                SheetCheckItem(label = "الامتحانات", isChecked = config.includeExamsSheet) {
                                    config = config.copy(includeExamsSheet = it)
                                }
                                SheetCheckItem(label = "سجل المخدومين", isChecked = config.includeMembersSheet) {
                                    config = config.copy(includeMembersSheet = it)
                                }
                                SheetCheckItem(label = "الافتقاد", isChecked = config.includeVisitsSheet) {
                                    config = config.copy(includeVisitsSheet = it)
                                }
                            }
                        }
                    }

                    // 4. Column Selection
                    SectionCard(title = "4. اختيار الأعمدة والبيانات المطلوبة (Columns)", icon = Icons.Default.FilterList) {
                        // Quick Presets Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { config = ExcelExportConfig.allSelected(config) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تحديد الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { config = ExcelExportConfig.standardSelected(config) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("الوضع القياسي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category A: Personal Info
                        ColumnGroupHeader("البيانات الشخصية")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ColumnCheckItem("اسم المخدوم (إجباري)", isChecked = true, enabled = false) {}
                            ColumnCheckItem("الفصل الدراسي", isChecked = config.includeSchoolClass) { config = config.copy(includeSchoolClass = it) }
                            ColumnCheckItem("تاريخ الميلاد", isChecked = config.includeBirthDate) { config = config.copy(includeBirthDate = it) }
                            ColumnCheckItem("رقم الهاتف", isChecked = config.includePhone) { config = config.copy(includePhone = it) }
                            ColumnCheckItem("هاتف ولي الأمر", isChecked = config.includeParentPhone) { config = config.copy(includeParentPhone = it) }
                            ColumnCheckItem("العنوان", isChecked = config.includeAddress) { config = config.copy(includeAddress = it) }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category B: Attendance
                        ColumnGroupHeader("حضور الخدمة والقداسات")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ColumnCheckItem("حضور الخدمة", isChecked = config.includeServicePresent) { config = config.copy(includeServicePresent = it) }
                            ColumnCheckItem("غياب الخدمة", isChecked = config.includeServiceAbsent) { config = config.copy(includeServiceAbsent = it) }
                            ColumnCheckItem("الأعذار", isChecked = config.includeServiceExcused) { config = config.copy(includeServiceExcused = it) }
                            ColumnCheckItem("نسبة الخدمة %", isChecked = config.includeServicePercentage) { config = config.copy(includeServicePercentage = it) }
                            ColumnCheckItem("حضور القداس", isChecked = config.includeMassPresent) { config = config.copy(includeMassPresent = it) }
                            ColumnCheckItem("نسبة القداس %", isChecked = config.includeMassPercentage) { config = config.copy(includeMassPercentage = it) }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category C: Assessments
                        ColumnGroupHeader("التقييمات الروحية والدراسية")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ColumnCheckItem("تسميع الألحان", isChecked = config.includeHymnScores) { config = config.copy(includeHymnScores = it) }
                            ColumnCheckItem("متوسط الألحان", isChecked = config.includeHymnAverage) { config = config.copy(includeHymnAverage = it) }
                            ColumnCheckItem("تقييمات الإنجيل", isChecked = config.includeBibleScores) { config = config.copy(includeBibleScores = it) }
                            ColumnCheckItem("متوسط الإنجيل", isChecked = config.includeBibleAverage) { config = config.copy(includeBibleAverage = it) }
                            ColumnCheckItem("درجات الامتحانات", isChecked = config.includeExamScores) { config = config.copy(includeExamScores = it) }
                            ColumnCheckItem("متوسط الامتحانات", isChecked = config.includeExamAverage) { config = config.copy(includeExamAverage = it) }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category D: Results
                        ColumnGroupHeader("النتائج والتقدير العام")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ColumnCheckItem("المجموع النهائي", isChecked = config.includeTotalScore) { config = config.copy(includeTotalScore = it) }
                            ColumnCheckItem("النسبة المئوية %", isChecked = config.includeTotalPercentage) { config = config.copy(includeTotalPercentage = it) }
                            ColumnCheckItem("التقدير العام", isChecked = config.includeOverallGrade) { config = config.copy(includeOverallGrade = it) }
                            ColumnCheckItem("الملاحظات", isChecked = config.includeNotes) { config = config.copy(includeNotes = it) }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onApply(config)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تطبيق والمعاينة", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onApply(config)
                                onExportDirect(config)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BurgundyPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير Excel الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = BurgundyPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BurgundyPrimary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ColumnGroupHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(GoldSecondary, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GoldSecondary)
    }
}

@Composable
private fun ColumnCheckItem(
    label: String,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) },
        shape = RoundedCornerShape(8.dp),
        color = if (isChecked) BurgundyPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isChecked) androidx.compose.foundation.BorderStroke(1.dp, BurgundyPrimary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = CheckboxDefaults.colors(checkedColor = BurgundyPrimary),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isChecked) BurgundyPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SheetCheckItem(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clickable { onCheckedChange(!isChecked) },
        shape = RoundedCornerShape(8.dp),
        color = if (isChecked) PresentGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isChecked) androidx.compose.foundation.BorderStroke(1.dp, PresentGreen.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = PresentGreen),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isChecked) PresentGreen else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
