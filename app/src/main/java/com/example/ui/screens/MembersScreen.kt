package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Group
import com.example.data.model.Member
import com.example.ui.components.DeaconHeader
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.ui.viewmodel.DeaconsViewModel
import com.example.ui.viewmodel.MemberSortOrder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.AbsentRed
import com.example.util.ExcelImporter

@Composable
fun MembersScreen(
    viewModel: DeaconsViewModel,
    onSelectMember: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val members by viewModel.filteredMembers.collectAsStateWithLifecycle()
    val allMembers by viewModel.members.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val selectedSchoolClass by viewModel.selectedSchoolClass.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()

    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()
    val importError by viewModel.importErrorMessage.collectAsStateWithLifecycle()
    val canManageMembers by viewModel.canManageMembers.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.parseExcelForImport(context, uri)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (canManageMembers) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = BurgundyPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة مخدوم")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة مخدوم", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DeaconHeader(
                title = "سجل المخدومين والشمامسة",
                subtitle = "إجمالي ${allMembers.size} مخدوم مسجل"
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("بحث بالاسم، الهاتف، الفصل، العنوان، المجموعة...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "بحث")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Excel Import & Template Bar (Only for members managers)
            if (canManageMembers) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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
                        Text("استيراد من Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.downloadExcelTemplate(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00796B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "تحميل نموذج",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحميل النموذج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // School Class Filter Chips
            val schoolClassesFromDb by viewModel.schoolClasses.collectAsStateWithLifecycle()
            val sortOrder by viewModel.memberSortOrder.collectAsStateWithLifecycle()
            var sortMenuExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الفصل الدراسي:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sort Dropdown Button
                Box {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "ترتيب",
                            modifier = Modifier.size(16.dp),
                            tint = BurgundyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (sortOrder) {
                                MemberSortOrder.NAME_ASC -> "أبجدي (أ - ي)"
                                MemberSortOrder.ATTENDANCE_DESC -> "الأعلى حضوراً"
                                MemberSortOrder.ATTENDANCE_ASC -> "الأقل حضوراً"
                                MemberSortOrder.CLASS_AND_NAME -> "حسب الفصل"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("أبجدياً (أ - ي)") },
                            onClick = {
                                viewModel.setMemberSortOrder(MemberSortOrder.NAME_ASC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("نسبة الحضور: الأعلى أولاً") },
                            onClick = {
                                viewModel.setMemberSortOrder(MemberSortOrder.ATTENDANCE_DESC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("نسبة الحضور: الأقل أولاً") },
                            onClick = {
                                viewModel.setMemberSortOrder(MemberSortOrder.ATTENDANCE_ASC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حسب الفصل الدراسي") },
                            onClick = {
                                viewModel.setMemberSortOrder(MemberSortOrder.CLASS_AND_NAME)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            val classList = listOf("كل الفصول" to null) + (
                if (schoolClassesFromDb.isNotEmpty()) {
                    schoolClassesFromDb.map { it.name to it.name }
                } else {
                    listOf(
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
                }
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(classList) { (label, classVal) ->
                    FilterChip(
                        selected = selectedSchoolClass == classVal,
                        onClick = { viewModel.setSelectedSchoolClass(classVal) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BurgundyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Members List
            if (members.isEmpty()) {
                EmptyStateView(
                    message = "لا توجد نتائج مطابقة",
                    subMessage = if (allMembers.isEmpty()) "ابدأ بإضافة مخدوم جديد أو استيراد ملف Excel المعتمد" else "جرّب تغيير كلمات البحث أو الفلتر",
                    icon = Icons.Default.Person
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(members, key = { it.id }) { member ->
                        MemberItemCard(
                            member = member,
                            onClick = { onSelectMember(member.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemberDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newMember ->
                viewModel.addMember(newMember)
                showAddDialog = false
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

@Composable
fun MemberItemCard(
    member: Member,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(
                photoPath = member.profileImage,
                name = member.fullName,
                size = 50.dp,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (member.schoolClass.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BurgundyPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = member.schoolClass,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BurgundyPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (member.area.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldSecondary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = member.area,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${member.area} - ${member.street.ifBlank { "شارع الكنيسة" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (member.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = member.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldSecondary.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "باركود",
                            tint = GoldSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "عرض الملف",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (Member) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var schoolClass by remember { mutableStateOf("أولى ابتدائي") }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var governorate by remember { mutableStateOf("أسيوط") }
    var center by remember { mutableStateOf("القوصية") }
    var area by remember { mutableStateOf("مير") }
    var street by remember { mutableStateOf("") }
    var houseNumber by remember { mutableStateOf("") }
    var addressDetails by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var classDropdownExpanded by remember { mutableStateOf(false) }
    var showUnsavedWarning by remember { mutableStateOf(false) }

    val hasUnsavedData = fullName.isNotBlank() ||
            phone.isNotBlank() ||
            parentPhone.isNotBlank() ||
            notes.isNotBlank() ||
            birthDate.isNotBlank()

    val handleDismissRequest = {
        if (hasUnsavedData) {
            showUnsavedWarning = true
        } else {
            onDismiss()
        }
    }

    val standardClasses = listOf(
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

    AlertDialog(
        onDismissRequest = handleDismissRequest,
        title = {
            Text(
                text = "إضافة مخدوم جديد",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم رباعي *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // School Class Selector
                ExposedDropdownMenuBox(
                    expanded = classDropdownExpanded,
                    onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = schoolClass,
                        onValueChange = { schoolClass = it },
                        label = { Text("الفصل الدراسي *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        standardClasses.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = {
                                    schoolClass = cls
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("هاتف المخدوم") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text("تاريخ الميلاد") },
                        placeholder = { Text("2012-05-10") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("هاتف ولي الأمر *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "بيانات العنوان التفصيلي (توزيع تلقائي):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BurgundyPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = governorate,
                        onValueChange = { governorate = it },
                        label = { Text("المحافظة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = center,
                        onValueChange = { center = it },
                        label = { Text("المركز/القسم") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("المنطقة/القرية") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text("الشارع") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = houseNumber,
                        onValueChange = { houseNumber = it },
                        label = { Text("رقم المنزل") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = addressDetails,
                        onValueChange = { addressDetails = it },
                        label = { Text("تفاصيل / علامة مميزة") },
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        val member = Member(
                            fullName = fullName.trim(),
                            schoolClass = schoolClass.trim().ifBlank { "أولى ابتدائي" },
                            birthDate = birthDate.trim(),
                            phone = phone.trim(),
                            parentPhone = parentPhone.trim(),
                            governorate = governorate.trim(),
                            center = center.trim(),
                            area = area.trim(),
                            street = street.trim(),
                            houseNumber = houseNumber.trim(),
                            addressDetails = addressDetails.trim(),
                            notes = notes.trim()
                        )
                        onConfirm(member)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = fullName.isNotBlank()
            ) {
                Text("حفظ المخدوم")
            }
        },
        dismissButton = {
            TextButton(onClick = handleDismissRequest) {
                Text("إلغاء")
            }
        }
    )

    if (showUnsavedWarning) {
        AlertDialog(
            onDismissRequest = { showUnsavedWarning = false },
            title = {
                Text(
                    text = "تنبيه: بيانات غير محفوظة",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text("لقد قمت بإدخال بعض بيانات المخدوم دون حفظها. هل أنت متأكد من رغبتك في الإلغاء؟")
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تجاهل والخروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedWarning = false }) {
                    Text("متابعة الإدخال")
                }
            }
        )
    }
}
