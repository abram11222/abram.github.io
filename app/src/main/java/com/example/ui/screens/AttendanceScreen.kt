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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.data.model.Member
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.components.DeaconHeader
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AbsentRedBg
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.ExcusedAmber
import com.example.ui.theme.ExcusedAmberBg
import com.example.ui.theme.GoldSecondary
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenBg
import com.example.ui.viewmodel.DeaconsViewModel

import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import com.example.ui.theme.FridayGold
import com.example.ui.theme.FridayGoldBg
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: DeaconsViewModel,
    onSelectMember: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val attendances by viewModel.attendances.collectAsStateWithLifecycle()
    val schoolClasses by viewModel.schoolClasses.collectAsStateWithLifecycle()

    val selectedDate by viewModel.selectedAttendanceDate.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedAttendanceType.collectAsStateWithLifecycle()
    val selectedMassNumber by viewModel.selectedMassNumber.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val preselectedClass by viewModel.selectedSchoolClass.collectAsStateWithLifecycle()
    val assignedClass by viewModel.assignedClass.collectAsStateWithLifecycle()
    val assignedClasses by viewModel.assignedClasses.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canRecordAttendance by viewModel.canRecordAttendance.collectAsStateWithLifecycle()

    var selectedClass by remember(preselectedClass, assignedClass) {
        mutableStateOf(
            preselectedClass ?: if (assignedClass.isNotBlank()) assignedClass else null
        )
    }
    var excusedDialogMember by remember { mutableStateOf<Member?>(null) }
    var excuseReasonInput by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Barcode Scanner & Quick Input Dialog State
    var showScannerDialog by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var scannedInputCode by remember { mutableStateOf("") }
    var scanFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var scanFeedbackIsSuccess by remember { mutableStateOf(true) }

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            cal.time = sdf.parse(selectedDate) ?: java.util.Date()
        } catch (_: Exception) {}

        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                viewModel.setSelectedAttendanceDate(formatted)
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    // Trigger quick background sync upon entering or changing class/date so screen is always live
    LaunchedEffect(selectedClass, selectedDate) {
        viewModel.triggerQuickSync()
    }

    val isFriday = remember(selectedDate) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance().apply { time = sdf.parse(selectedDate) ?: return@remember false }
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        } catch (e: Exception) {
            false
        }
    }

    val classOptions = remember(members, schoolClasses, assignedClasses) {
        val fromDb = schoolClasses.map { it.name }
        val fromMembers = members.map { it.schoolClass }.filter { it.isNotBlank() }
        val all = (fromDb + fromMembers).distinct()
        if (currentUser?.isAdmin == true || currentUser?.isViewer == true) all
        else all.filter { it in assignedClasses }
    }

    val filteredMembers = remember(members, selectedClass) {
        members.filter { member ->
            selectedClass == null || member.schoolClass == selectedClass
        }
    }

    // Find current attendance status for each member for selected date & type & massNumber
    val currentAttendanceMap = attendances
        .filter { it.date == selectedDate && it.type == selectedType && (selectedType != AttendanceType.MASS || it.massNumber == selectedMassNumber) }
        .associateBy { it.memberId }

    val presentCount = currentAttendanceMap.values.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = currentAttendanceMap.values.count { it.status == AttendanceStatus.ABSENT }
    val excusedCount = currentAttendanceMap.values.count { it.status == AttendanceStatus.EXCUSED }

    // Excused Reason Dialog
    if (excusedDialogMember != null) {
        AlertDialog(
            onDismissRequest = { excusedDialogMember = null },
            title = { Text("تسجيل عذر للمخدوم") },
            text = {
                Column {
                    Text(
                        text = "المخدوم: ${excusedDialogMember?.fullName}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = excuseReasonInput,
                        onValueChange = { excuseReasonInput = it },
                        label = { Text("سبب العذر (سفر، مرض، دراسة...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        excusedDialogMember?.let { m ->
                            viewModel.recordMemberAttendance(
                                memberId = m.id,
                                status = AttendanceStatus.EXCUSED,
                                notes = excuseReasonInput.trim()
                            )
                        }
                        excusedDialogMember = null
                        excuseReasonInput = ""
                    }
                ) {
                    Text("حفظ العذر")
                }
            },
            dismissButton = {
                TextButton(onClick = { excusedDialogMember = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Camera Barcode Scanner Fullscreen / Popup Dialog
    if (showCameraScanner) {
        CameraBarcodeScannerDialog(
            onDismiss = { showCameraScanner = false },
            onBarcodeScanned = { code ->
                scannedInputCode = code
                viewModel.recordAttendanceByBarcode(code) { success, member, msg ->
                    scanFeedbackIsSuccess = success
                    scanFeedbackMessage = msg
                }
            }
        )
    }

    // Barcode Scanner & Quick Input Dialog
    if (showScannerDialog) {
        AlertDialog(
            onDismissRequest = {
                showScannerDialog = false
                scannedInputCode = ""
                scanFeedbackMessage = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = GoldSecondary
                    )
                    Text("مسح وتسجيل باركود الحضور", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "وجه ماسح الباركود أو أدخل رقم الباركود/المخدوم لتسجيل الحضور فورياً:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showCameraScanner = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مسح عبر كاميرا الهاتف 📷", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = scannedInputCode,
                        onValueChange = {
                            scannedInputCode = it
                            scanFeedbackMessage = null
                        },
                        label = { Text("رقم الباركود (مثال: DCN-1001 أو 1001)") },
                        placeholder = { Text("DCN-1001") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            IconButton(onClick = { showCameraScanner = true }) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "فتح الكاميرا للمسح",
                                    tint = BurgundyPrimary
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = { showCameraScanner = true }) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "تشغيل الكاميرا",
                                    tint = GoldSecondary
                                )
                            }
                        }
                    )

                    if (scanFeedbackMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (scanFeedbackIsSuccess) PresentGreenBg else AbsentRedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (scanFeedbackIsSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (scanFeedbackIsSuccess) PresentGreen else AbsentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = scanFeedbackMessage ?: "",
                                    color = if (scanFeedbackIsSuccess) PresentGreen else AbsentRed,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (scannedInputCode.isNotBlank()) {
                            viewModel.recordAttendanceByBarcode(scannedInputCode) { success, member, msg ->
                                scanFeedbackIsSuccess = success
                                scanFeedbackMessage = msg
                                if (success) {
                                    scannedInputCode = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                    enabled = scannedInputCode.isNotBlank()
                ) {
                    Text("تسجيل الحضور")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showScannerDialog = false
                    scannedInputCode = ""
                    scanFeedbackMessage = null
                }) {
                    Text("إغلاق")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DeaconHeader(
            title = "تسجيل الحضور والغياب",
            subtitle = if (selectedType == AttendanceType.SERVICE) "حضور حصة خدمة الشمامسة" else "حضور القداس الإلهي (قداس رقم $selectedMassNumber)"
        )

        // Live Auto-Sync Status Indicator
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            shape = RoundedCornerShape(8.dp),
            color = PresentGreen.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PresentGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مزامنة سحابية تلقائية حية ⚡ (التغييرات تظهر لجميع الخدام بدون ضغط زر)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PresentGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Type Switcher Tabs (حضور الخدمة vs حضور القداسات)
        PrimaryTabRow(
            selectedTabIndex = if (selectedType == AttendanceType.SERVICE) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedType == AttendanceType.SERVICE,
                onClick = { viewModel.setSelectedAttendanceType(AttendanceType.SERVICE) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حضور الخدمة", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedType == AttendanceType.MASS,
                onClick = { viewModel.setSelectedAttendanceType(AttendanceType.MASS) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Church, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حضور القداسات", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Sub Controls (Date & Mass Number 1 vs 2)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { viewModel.setSelectedAttendanceDate(it) },
                        label = { Text("التاريخ") },
                        leadingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "اختيار التاريخ من النتيجة", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = { viewModel.setSelectedAttendanceDate(viewModel.todayDate) }) {
                                Text("اليوم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    if (selectedType == AttendanceType.MASS) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("رقم القداس بالشهر", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = selectedMassNumber == 1,
                                    onClick = { viewModel.setSelectedMassNumber(1) },
                                    label = { Text("قداس 1") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                FilterChip(
                                    selected = selectedMassNumber == 2,
                                    onClick = { viewModel.setSelectedMassNumber(2) },
                                    label = { Text("قداس 2") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Friday Church Day Highlight (Gold)
                if (isFriday) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = FridayGoldBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = FridayGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "يوم الجمعة — يوم الخدمة والقداس المعتمد لمدرسة الشمامسة",
                                color = FridayGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats summary banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "حاضر: $presentCount", color = PresentGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text(text = "غائب: $absentCount", color = AbsentRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text(text = "إذن: $excusedCount", color = ExcusedAmber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text(text = "إجمالي: ${filteredMembers.size}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (!canRecordAttendance) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "حسابك في وضع العرض فقط. ليس لديك صلاحية تسجيل الحضور والغياب.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Quick Bulk & Barcode Scanner Actions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Barcode Scanner Button (Gold & Burgundy theme)
            Button(
                onClick = { showScannerDialog = true },
                enabled = canRecordAttendance,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldSecondary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("مسح باركود", fontWeight = FontWeight.Bold, color = Color.Black)
            }

            // Quick Bulk Action: تسجيل الكل حاضر
            if (filteredMembers.isNotEmpty()) {
                Button(
                    onClick = { viewModel.markAllMembersPresent(filteredMembers) },
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val bulkLabel = when {
                        selectedClass != null -> "تسجيل حاضر ($selectedClass)"
                        else -> "تسجيل الكل حاضر (${filteredMembers.size})"
                    }
                    Text(bulkLabel, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        // School Class Filter Chips (الفصل الدراسي) with "كل الفصول"
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedClass == null,
                    onClick = { selectedClass = null },
                    label = { Text("كل الفصول") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            items(classOptions) { cls ->
                FilterChip(
                    selected = selectedClass == cls,
                    onClick = { selectedClass = if (selectedClass == cls) null else cls },
                    label = { Text(cls) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Members Attendance List
        if (filteredMembers.isEmpty()) {
            EmptyStateView(
                message = "لا يوجد مخدومين مسجلين",
                icon = Icons.Default.CalendarMonth
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMembers, key = { it.id }) { member ->
                    val currentAttendance = currentAttendanceMap[member.id]
                    val currentStatus = currentAttendance?.status

                    MemberAttendanceRow(
                        member = member,
                        currentAttendance = currentAttendance,
                        onStatusSelected = { status ->
                            if (status == AttendanceStatus.EXCUSED) {
                                excuseReasonInput = currentAttendance?.notes ?: ""
                                excusedDialogMember = member
                            } else {
                                viewModel.recordMemberAttendance(memberId = member.id, status = status)
                            }
                        },
                        onEditExcuse = {
                            excuseReasonInput = currentAttendance?.notes ?: ""
                            excusedDialogMember = member
                        },
                        onResetAttendance = {
                            viewModel.removeAttendanceForMember(member.id)
                        },
                        onOpenProfile = { onSelectMember(member.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun MemberAttendanceRow(
    member: Member,
    currentAttendance: com.example.data.model.Attendance?,
    onStatusSelected: (AttendanceStatus) -> Unit,
    onEditExcuse: () -> Unit,
    onResetAttendance: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val currentStatus = currentAttendance?.status

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenProfile() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemberAvatar(
                    photoPath = member.profileImage,
                    name = member.fullName,
                    size = 46.dp,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = member.schoolClass,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (member.area.isNotBlank()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = member.area,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentStatus != null) {
                        AttendanceStatusBadge(status = currentStatus)
                        IconButton(
                            onClick = onResetAttendance,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إلغاء التسجيل",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Excused Note Banner
            if (currentStatus == AttendanceStatus.EXCUSED && !currentAttendance.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = ExcusedAmberBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onEditExcuse() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سبب العذر: ${currentAttendance.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExcusedAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل العذر",
                            tint = ExcusedAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: حاضر / غائب / إذن
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusSelectButton(
                    text = "حاضر",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    color = PresentGreen,
                    bgColor = PresentGreenBg,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected(AttendanceStatus.PRESENT) }
                )
                StatusSelectButton(
                    text = "غائب",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    color = AbsentRed,
                    bgColor = AbsentRedBg,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected(AttendanceStatus.ABSENT) }
                )
                StatusSelectButton(
                    text = if (currentStatus == AttendanceStatus.EXCUSED && !currentAttendance?.notes.isNullOrBlank()) "معذور (مُحدد)" else "إذن / عذر",
                    isSelected = currentStatus == AttendanceStatus.EXCUSED,
                    color = ExcusedAmber,
                    bgColor = ExcusedAmberBg,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected(AttendanceStatus.EXCUSED) }
                )
            }
        }
    }
}

@Composable
fun StatusSelectButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else bgColor.copy(alpha = 0.5f),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else color,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
