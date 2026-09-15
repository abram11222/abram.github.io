package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.data.model.BibleAssessment
import com.example.data.model.BibleLesson
import com.example.data.model.ExamResult
import com.example.data.model.HymnAssessment
import com.example.data.model.Member
import com.example.data.model.VisitRecord
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AbsentRedBg
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.ExcusedAmber
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenBg
import com.example.ui.viewmodel.DeaconsViewModel
import com.example.util.BarcodeGenerator
import com.example.util.DeaconPdfPrinter
import com.example.util.PhotoManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberProfileScreen(
    memberId: Long,
    viewModel: DeaconsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val member = members.find { it.id == memberId }
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val groupName = member?.groupId?.let { gid -> groups.find { it.id == gid }?.name } ?: "بدون مجموعة"

    val attendances by viewModel.attendances.collectAsStateWithLifecycle()
    val memberAttendances = attendances.filter { it.memberId == memberId }
    val serviceLogs = memberAttendances.filter { it.type == AttendanceType.SERVICE }
    val massLogs = memberAttendances.filter { it.type == AttendanceType.MASS }

    val hymns by viewModel.hymns.collectAsStateWithLifecycle()
    val allHymnsMaster by viewModel.allHymnsMaster.collectAsStateWithLifecycle()
    val hymnAssessments by viewModel.hymnAssessments.collectAsStateWithLifecycle()
    val memberHymnAssessments = hymnAssessments.filter { it.memberId == memberId }

    val bibleLessons by viewModel.bibleLessons.collectAsStateWithLifecycle()
    val activeBibleLessons by viewModel.activeBibleLessons.collectAsStateWithLifecycle()
    val bibleAssessments by viewModel.bibleAssessments.collectAsStateWithLifecycle()
    val memberBibleAssessments = bibleAssessments.filter { it.memberId == memberId }

    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val examResults by viewModel.examResults.collectAsStateWithLifecycle()
    val memberExamResults = examResults.filter { it.memberId == memberId }

    val allVisits by viewModel.allVisitRecords.collectAsStateWithLifecycle()
    val canManageMembers by viewModel.canManageMembers.collectAsStateWithLifecycle()
    val memberVisits = remember(allVisits, memberId) {
        allVisits.filter { it.memberId == memberId }.sortedByDescending { it.visitDate }
    }

    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }
    var showIdCardDialog by remember { mutableStateOf(false) }
    var showAddVisitDialog by remember { mutableStateOf(false) }
    var editingVisit by remember { mutableStateOf<VisitRecord?>(null) }
    var deletingVisit by remember { mutableStateOf<VisitRecord?>(null) }

    // Quick direct recording dialogs
    var showQuickAttendanceDialog by remember { mutableStateOf(false) }
    var quickAttendanceInitialType by remember { mutableStateOf(AttendanceType.SERVICE) }
    var showQuickHymnDialog by remember { mutableStateOf(false) }
    var showQuickBibleDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && member != null) {
            val savedPath = PhotoManager.saveUriLocally(context, uri, member.id)
            PhotoManager.deletePhotoFile(member.profileImage)
            viewModel.updateMember(member.copy(profileImage = savedPath))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null && member != null) {
            val savedPath = PhotoManager.saveBitmapLocally(context, bitmap, member.id)
            PhotoManager.deletePhotoFile(member.profileImage)
            viewModel.updateMember(member.copy(profileImage = savedPath))
        }
    }

    val tabTitles = listOf("البيانات", "الافتقاد", "الحضور", "القداسات", "الألحان", "الإنجيل", "الامتحانات", "التقرير")

    if (member == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لم يتم العثور على المخدوم")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ملف المخدوم", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (canManageMembers) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل المخدوم", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المخدوم", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Profile Top Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.clickable { showPhotoDialog = true },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        MemberAvatar(
                            photoPath = member.profileImage,
                            name = member.fullName,
                            size = 66.dp,
                            fontSize = 24.sp,
                            borderWidth = 2.dp,
                            borderColor = GoldSecondary
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GoldSecondary)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "تغيير الصورة",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = member.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (!member.isActive) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AbsentRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "غير نشط / أرشيف",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AbsentRed,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldSecondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = groupName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            if (member.schoolClass.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF00796B).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = member.schoolClass,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00796B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${member.area}، ${member.center}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Direct Registration Action Card (التسجيل المباشر من ملف المخدوم)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "⚡ تسجيل مباشر للمخدوم:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BurgundyPrimary,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                quickAttendanceInitialType = AttendanceType.SERVICE
                                showQuickAttendanceDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("حضور خدمة", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                quickAttendanceInitialType = AttendanceType.MASS
                                showQuickAttendanceDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.Default.Church, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("حضور قداس", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = { showQuickHymnDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("تسميع لحن", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = { showQuickBibleDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("تسميع إنجيل", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            // Tabs Row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) BurgundyPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> ProfileInfoTab(
                    member = member,
                    groupName = groupName,
                    onShowBarcode = { showBarcodeDialog = true },
                    onShowIdCard = { showIdCardDialog = true }
                )
                1 -> VisitsTab(
                    visits = memberVisits,
                    onAddVisit = { showAddVisitDialog = true },
                    onEditVisit = { editingVisit = it },
                    onDeleteVisit = { deletingVisit = it }
                )
                2 -> ServiceAttendanceTab(
                    logs = serviceLogs,
                    onRecordAttendance = {
                        quickAttendanceInitialType = AttendanceType.SERVICE
                        showQuickAttendanceDialog = true
                    }
                )
                3 -> MassAttendanceTab(
                    logs = massLogs,
                    onRecordMass = {
                        quickAttendanceInitialType = AttendanceType.MASS
                        showQuickAttendanceDialog = true
                    }
                )
                4 -> HymnsTab(
                    hymns = if (allHymnsMaster.isNotEmpty()) allHymnsMaster else hymns,
                    assessments = memberHymnAssessments,
                    onRecordHymn = { showQuickHymnDialog = true }
                )
                5 -> BibleTab(
                    assessments = memberBibleAssessments,
                    onRecordBible = { showQuickBibleDialog = true }
                )
                6 -> ExamsTab(exams = exams, results = memberExamResults)
                7 -> SummaryReportTab(
                    member = member,
                    groupName = groupName,
                    serviceLogs = serviceLogs,
                    massLogs = massLogs,
                    hymns = hymns,
                    hymnAssessments = memberHymnAssessments,
                    bibleAssessments = memberBibleAssessments,
                    exams = exams,
                    examResults = memberExamResults
                )
            }
        }
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("صورة المخدوم الشخصية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showPhotoDialog = false
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("التقاط صورة بالكاميرا")
                    }

                    Button(
                        onClick = {
                            showPhotoDialog = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار من معرض الصور")
                    }

                    if (!member.profileImage.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                showPhotoDialog = false
                                PhotoManager.deletePhotoFile(member.profileImage)
                                viewModel.updateMember(member.copy(profileImage = null))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = AbsentRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حذف الصورة الحالية")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showBarcodeDialog) {
        val barcodeId = BarcodeGenerator.getBarcodeId(member.id)
        val barcodeBitmap = remember(member.id, member.fullName) {
            BarcodeGenerator.generateBarcodeBitmap(barcodeId, member.fullName, 600, 240)
        }

        AlertDialog(
            onDismissRequest = { showBarcodeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = GoldSecondary)
                    Text("باركود المخدوم", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = barcodeBitmap.asImageBitmap(),
                                contentDescription = "باركود الشماس",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = member.fullName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "رقم الباركود: $barcodeId",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BurgundyPrimary
                            )
                            if (member.schoolClass.isNotBlank()) {
                                Text(
                                    text = "الفصل: ${member.schoolClass}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            BarcodeGenerator.shareBarcode(context, barcodeBitmap, member)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة الباركود", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBarcodeDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (showIdCardDialog) {
        PrintableIdCardDialog(
            member = member,
            onDismiss = { showIdCardDialog = false }
        )
    }

    if (showAddVisitDialog) {
        var visitDate by remember {
            mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
        }
        var visitNotes by remember { mutableStateOf("") }
        var showDatePickerForVisit by remember { mutableStateOf(false) }

        if (showDatePickerForVisit) {
            val cal = Calendar.getInstance()
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                cal.time = sdf.parse(visitDate) ?: java.util.Date()
            } catch (_: Exception) {}

            android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    visitDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    showDatePickerForVisit = false
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnDismissListener { showDatePickerForVisit = false }
                show()
            }
        }

        AlertDialog(
            onDismissRequest = { showAddVisitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = BurgundyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل افتقاد جديد", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("المخدوم: ${member.fullName}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = visitDate,
                        onValueChange = { visitDate = it },
                        label = { Text("تاريخ الافتقاد") },
                        leadingIcon = {
                            IconButton(onClick = { showDatePickerForVisit = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "اختيار التاريخ", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = {
                                visitDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                            }) {
                                Text("اليوم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = visitNotes,
                        onValueChange = { visitNotes = it },
                        label = { Text("ملاحظات الافتقاد / ما تم بالزيارة") },
                        placeholder = { Text("مثال: تم الافتقاد بالمنزل، تشجيع على الحضور، الصلاة مع الأسرة...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (visitDate.isNotBlank()) {
                            viewModel.addVisitRecord(
                                memberId = member.id,
                                visitDate = visitDate.trim(),
                                notes = visitNotes.trim()
                            )
                            showAddVisitDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("حفظ الافتقاد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVisitDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (editingVisit != null) {
        val visitToEdit = editingVisit!!
        var editDate by remember(visitToEdit) { mutableStateOf(visitToEdit.visitDate) }
        var editNotes by remember(visitToEdit) { mutableStateOf(visitToEdit.notes) }
        var showDatePickerForEdit by remember { mutableStateOf(false) }

        if (showDatePickerForEdit) {
            val cal = Calendar.getInstance()
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                cal.time = sdf.parse(editDate) ?: java.util.Date()
            } catch (_: Exception) {}

            android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    editDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    showDatePickerForEdit = false
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnDismissListener { showDatePickerForEdit = false }
                show()
            }
        }

        AlertDialog(
            onDismissRequest = { editingVisit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = BurgundyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل بيانات الافتقاد", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text("تاريخ الافتقاد") },
                        leadingIcon = {
                            IconButton(onClick = { showDatePickerForEdit = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "اختيار التاريخ", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("الملاحظات") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editDate.isNotBlank()) {
                            viewModel.updateVisitRecord(
                                visitToEdit.copy(
                                    visitDate = editDate.trim(),
                                    notes = editNotes.trim()
                                )
                            )
                            editingVisit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingVisit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (deletingVisit != null) {
        val visitToDelete = deletingVisit!!
        AlertDialog(
            onDismissRequest = { deletingVisit = null },
            title = { Text("حذف سجل الافتقاد", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف سجل افتقاد تاريخ (${visitToDelete.visitDate})؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVisitRecord(visitToDelete)
                        deletingVisit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingVisit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showQuickAttendanceDialog) {
        QuickRecordAttendanceDialog(
            member = member,
            initialType = quickAttendanceInitialType,
            onDismiss = { showQuickAttendanceDialog = false },
            onSave = { type, status, date, massNumber, notes ->
                viewModel.recordAttendanceDirect(
                    memberId = member.id,
                    type = type,
                    status = status,
                    date = date,
                    massNumber = massNumber,
                    notes = notes
                )
                showQuickAttendanceDialog = false
            }
        )
    }

    if (showQuickHymnDialog) {
        val availableHymns = if (allHymnsMaster.isNotEmpty()) allHymnsMaster else hymns
        QuickRecordHymnDialog(
            member = member,
            hymns = availableHymns,
            onDismiss = { showQuickHymnDialog = false },
            onSave = { hymnId, score, maxScore, date, notes ->
                viewModel.recordHymnAssessment(
                    memberId = member.id,
                    hymnId = hymnId,
                    score = score,
                    maxScore = maxScore,
                    date = date,
                    notes = notes
                )
                showQuickHymnDialog = false
            }
        )
    }

    if (showQuickBibleDialog) {
        val availableLessons = if (activeBibleLessons.isNotEmpty()) activeBibleLessons else bibleLessons
        QuickRecordBibleDialog(
            member = member,
            lessons = availableLessons,
            onDismiss = { showQuickBibleDialog = false },
            onSave = { lessonName, score, maxScore, date, notes, lessonId ->
                viewModel.recordBibleAssessment(
                    memberId = member.id,
                    date = date,
                    lessonName = lessonName,
                    score = score,
                    maxScore = maxScore,
                    notes = notes,
                    lessonId = lessonId
                )
                showQuickBibleDialog = false
            }
        )
    }

    if (showEditDialog) {
        EditMemberDialog(
            member = member,
            groups = groups,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedMember ->
                viewModel.updateMember(updatedMember)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("حذف المخدوم نهائيًا", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف المخدوم '${member.fullName}'؟ سيتم حذف بياناته وسجلات حضوره وتقييماته المرتبطة به تلقائيًا وبشكل نهائي.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteMember(member)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun VisitsTab(
    visits: List<VisitRecord>,
    onAddVisit: () -> Unit,
    onEditVisit: (VisitRecord) -> Unit,
    onDeleteVisit: (VisitRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary & Add Button Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "سجل الافتقاد والمتابعة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BurgundyPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (visits.isNotEmpty()) "إجمالي الافتقادات: ${visits.size} | آخر افتقاد: ${visits.first().visitDate}" else "لم يسجل افتقاد بعد",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onAddVisit,
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل افتقاد", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Visits List
        if (visits.isEmpty()) {
            item {
                EmptyStateView(
                    message = "لم يتم تسجيل افتقادات لهذا المخدوم حتى الآن",
                    subMessage = "اضغط على زر (تسجيل افتقاد) لتوثيق زيارة المخدوم ومتابعة أحواله",
                    icon = Icons.Default.Home
                )
            }
        } else {
            items(visits, key = { it.id }) { visit ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BurgundyPrimary.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = BurgundyPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = visit.visitDate,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BurgundyPrimary
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { onEditVisit(visit) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل الافتقاد",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteVisit(visit) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف الافتقاد",
                                        tint = AbsentRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (visit.notes.isNotBlank()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Text(
                                text = visit.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ProfileInfoTab(
    member: Member,
    groupName: String,
    onShowBarcode: () -> Unit = {},
    onShowIdCard: () -> Unit = {}
) {
    val context = LocalContext.current
    val barcodeId = remember(member.id) { BarcodeGenerator.getBarcodeId(member.id) }
    val barcodeBitmap = remember(member.id, member.fullName) {
        BarcodeGenerator.generateBarcodeBitmap(barcodeId, member.fullName, 600, 240)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Barcode Card Section: "باركود المخدوم"
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = GoldSecondary
                            )
                            Text(
                                text = "باركود المخدوم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BurgundyPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldSecondary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = barcodeId,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Barcode Image Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowBarcode() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = barcodeBitmap.asImageBitmap(),
                                contentDescription = "باركود ${member.fullName}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${member.fullName} • $barcodeId",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    }

                    // Action Buttons: عرض الباركود ومشاركة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShowBarcode,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("عرض الباركود", fontSize = 12.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                BarcodeGenerator.shareBarcode(context, barcodeBitmap, member)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    // ID Card Dedicated Button
                    Button(
                        onClick = onShowIdCard,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("عرض / طباعة الكارنيه 🪪", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "البيانات الشخصية والدراسية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BurgundyPrimary
                    )

                    InfoRow(icon = Icons.Default.Person, label = "الاسم الكامل", value = member.fullName)
                    InfoRow(icon = Icons.Default.School, label = "الفصل الدراسي", value = member.schoolClass.ifBlank { "غير مسجل" })
                    InfoRow(icon = Icons.Default.Phone, label = "رقم هاتف المخدوم", value = member.phone.ifBlank { "غير مسجل" })
                    InfoRow(icon = Icons.Default.Cake, label = "تاريخ الميلاد", value = member.birthDate.ifBlank { "غير مسجل" })
                    InfoRow(icon = Icons.Default.Call, label = "هاتف ولي الأمر", value = member.parentPhone.ifBlank { "غير مسجل" })
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "العنوان التفصيلي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BurgundyPrimary
                    )

                    InfoRow(icon = Icons.Default.LocationOn, label = "المحافظة والمركز", value = "${member.governorate} - ${member.center}")
                    InfoRow(icon = Icons.Default.LocationOn, label = "المنطقة / القرية", value = member.area)
                    InfoRow(icon = Icons.Default.Home, label = "الشارع ورقم المنزل", value = "${member.street.ifBlank { "غير محدد" }} (منزل ${member.houseNumber.ifBlank { "-" }})")
                    if (member.addressDetails.isNotBlank()) {
                        InfoRow(icon = Icons.Default.Info, label = "تفاصيل إضافية", value = member.addressDetails)
                    }
                }
            }
        }

        if (member.notes.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ملاحظات الخادم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                        Text(
                            text = member.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ServiceAttendanceTab(
    logs: List<Attendance>,
    onRecordAttendance: () -> Unit = {}
) {
    val present = logs.count { it.status == AttendanceStatus.PRESENT }
    val absent = logs.count { it.status == AttendanceStatus.ABSENT }
    val excused = logs.count { it.status == AttendanceStatus.EXCUSED }
    val rate = if (logs.isNotEmpty()) (present * 100) / logs.size else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Action & Summary Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "حضور حصص الخدمة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BurgundyPrimary
                            )
                            Text(
                                text = "إجمالي الحصص: ${logs.size} | نسبة الحضور: $rate%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onRecordAttendance,
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسجيل حضور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (logs.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem(label = "إجمالي", value = "${logs.size}")
                            StatItem(label = "حاضر", value = "$present", color = PresentGreen)
                            StatItem(label = "غائب", value = "$absent", color = AbsentRed)
                            StatItem(label = "إذن", value = "$excused", color = ExcusedAmber)
                            StatItem(label = "النسبة", value = "$rate%", color = BurgundyPrimary)
                        }
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                EmptyStateView(
                    message = "لا توجد سجلات حضور للخدمة بعد",
                    subMessage = "اضغط على زر (تسجيل حضور) لإضافة حصة جديدة مباشرة",
                    icon = Icons.Default.DateRange
                )
            }
        } else {
            items(logs) { log ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "حصة الخدمة: ${log.date}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (log.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        AttendanceStatusBadge(status = log.status)
                    }
                }
            }
        }
    }
}

@Composable
fun MassAttendanceTab(
    logs: List<Attendance>,
    onRecordMass: () -> Unit = {}
) {
    val present = logs.count { it.status == AttendanceStatus.PRESENT }
    val requiredMasses = 2 // 2 per month
    val ratioText = "$present / $requiredMasses"
    val percent = (present * 100) / requiredMasses.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BurgundyPrimary.copy(alpha = 0.08f))
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
                        Column {
                            Text(
                                text = "حضور القداسات الشهرية المطلوبة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BurgundyPrimary
                            )
                            Text(
                                text = "الهدف: قداسان شهرياً لكل شماس",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onRecordMass,
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسجيل قداس", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حالة إتمام الهدف:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ratioText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (present >= requiredMasses) PresentGreen else AbsentRed
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (present.toFloat() / requiredMasses.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (present >= requiredMasses) PresentGreen else GoldSecondary,
                        trackColor = Color.LightGray.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "نسبة تحقيق حضور القداسات: $percent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                EmptyStateView(
                    message = "لم يتم تسجيل قداسات لهذا المخدوم بعد",
                    subMessage = "اضغط على زر (تسجيل قداس) لإضافة قداس أول أو ثانٍ",
                    icon = Icons.Default.Church
                )
            }
        } else {
            items(logs) { log ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val massTitle = if (log.massNumber != null) "قداس رقم ${log.massNumber}" else "قداس إلهي"
                            Text(
                                text = "$massTitle - ${log.date}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (log.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        AttendanceStatusBadge(status = log.status)
                    }
                }
            }
        }
    }
}

@Composable
fun HymnsTab(
    hymns: List<com.example.data.model.Hymn>,
    assessments: List<HymnAssessment>,
    onRecordHymn: () -> Unit = {}
) {
    val hymnMap = hymns.associateBy { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tab Header with Quick Action
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تقييمات الألحان الكنسية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                        Text(
                            text = if (assessments.isNotEmpty()) "تم تسميع ${assessments.size} لحن" else "لا توجد ألحان مسجلة بعد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onRecordHymn,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسميع لحن", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (assessments.isEmpty()) {
            item {
                EmptyStateView(
                    message = "لا توجد درجات تسميع ألحان مسجلة",
                    subMessage = "اضغط على زر (تسميع لحن) لتقييم لحن جديد من المنهج",
                    icon = Icons.Default.MusicNote
                )
            }
        } else {
            items(assessments) { item ->
                val hymn = hymnMap[item.hymnId]
                val hymnName = hymn?.name ?: "لحن غير معروف"
                val category = hymn?.category ?: "عام"

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = hymnName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$category - تاريخ: ${item.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BurgundyPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${item.score} / ${item.maxScore}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = BurgundyPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BibleTab(
    assessments: List<BibleAssessment>,
    onRecordBible: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tab Header with Quick Action
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تقييمات الكتاب المقدس",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                        Text(
                            text = if (assessments.isNotEmpty()) "تم تسميع ${assessments.size} درس / إصحاح" else "لا توجد دروس مسجلة بعد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onRecordBible,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسميع إنجيل", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (assessments.isEmpty()) {
            item {
                EmptyStateView(
                    message = "لا توجد تقييمات إنجيل مسجلة",
                    subMessage = "اضغط على زر (تسميع إنجيل) لحفظ تقييم درس جديد",
                    icon = Icons.Default.MenuBook
                )
            }
        } else {
            items(assessments) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.lessonName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "تاريخ التسميع: ${item.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldSecondary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${item.score} / ${item.maxScore}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamsTab(exams: List<com.example.data.model.Exam>, results: List<ExamResult>) {
    if (results.isEmpty()) {
        EmptyStateView(
            message = "لا توجد نتائج امتحانات مسجلة",
            subMessage = "سجل نتائج امتحانات الشمامسة الشهرية ونصف السنة",
            icon = Icons.Default.Grade
        )
    } else {
        val examMap = exams.associateBy { it.id }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(results) { res ->
                val exam = examMap[res.examId]
                val examName = exam?.name ?: "امتحان"
                val maxScore = exam?.maxScore ?: 100.0
                val percent = ((res.score / maxScore) * 100).toInt()

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = examName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (res.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = res.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${res.score} / $maxScore",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = BurgundyPrimary
                            )
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (percent >= 85) PresentGreen else GoldSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryReportTab(
    member: Member,
    groupName: String,
    serviceLogs: List<Attendance>,
    massLogs: List<Attendance>,
    hymns: List<com.example.data.model.Hymn>,
    hymnAssessments: List<HymnAssessment>,
    bibleAssessments: List<BibleAssessment>,
    exams: List<com.example.data.model.Exam>,
    examResults: List<ExamResult>
) {
    val servicePresent = serviceLogs.count { it.status == AttendanceStatus.PRESENT }
    val serviceRate = if (serviceLogs.isNotEmpty()) (servicePresent * 100) / serviceLogs.size else 0

    val massPresent = massLogs.count { it.status == AttendanceStatus.PRESENT }
    val massRatio = "$massPresent / 2"

    val hymnAvg = if (hymnAssessments.isNotEmpty()) {
        String.format("%.1f", hymnAssessments.map { it.score }.average())
    } else "-"

    val bibleAvg = if (bibleAssessments.isNotEmpty()) {
        String.format("%.1f", bibleAssessments.map { it.score }.average())
    } else "-"

    val examAvg = if (examResults.isNotEmpty()) {
        String.format("%.1f", examResults.map { it.score }.average())
    } else "-"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BurgundyPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ملخص تقييم المخدوم الشامل",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${member.fullName} - $groupName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "مؤشرات الأداء",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BurgundyPrimary
                    )

                    ReportMetricRow(title = "حضور حصص الخدمة", value = "$servicePresent من أصل ${serviceLogs.size} ($serviceRate%)")
                    ReportMetricRow(title = "حضور القداسات الشهرية", value = massRatio)
                    ReportMetricRow(title = "متوسط درجات الألحان", value = "$hymnAvg / 10")
                    ReportMetricRow(title = "متوسط درجات الإنجيل", value = "$bibleAvg / 10")
                    ReportMetricRow(title = "متوسط درجات الامتحانات", value = "$examAvg / 100")
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PresentGreenBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "التقدير الإجمالي والتقييم الروحي",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresentGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (serviceRate >= 80 && massPresent >= 2) "ممتاز - شماس ملتزم ومواظب على الألحان والقداس الإلهي" else "جيد جداً - يحتاج مزيد من الانتظام في قداسات الجمعة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PresentGreen
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BurgundyPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReportMetricRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = BurgundyPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberDialog(
    member: Member,
    groups: List<com.example.data.model.Group>,
    onDismiss: () -> Unit,
    onConfirm: (Member) -> Unit
) {
    var fullName by remember { mutableStateOf(member.fullName) }
    var schoolClass by remember { mutableStateOf(member.schoolClass.ifBlank { "أولى ابتدائي" }) }
    var birthDate by remember { mutableStateOf(member.birthDate) }
    var phone by remember { mutableStateOf(member.phone) }
    var parentPhone by remember { mutableStateOf(member.parentPhone) }
    var governorate by remember { mutableStateOf(member.governorate) }
    var center by remember { mutableStateOf(member.center) }
    var area by remember { mutableStateOf(member.area) }
    var street by remember { mutableStateOf(member.street) }
    var houseNumber by remember { mutableStateOf(member.houseNumber) }
    var addressDetails by remember { mutableStateOf(member.addressDetails) }
    var selectedGroupId by remember { mutableStateOf<Long?>(member.groupId) }
    var isActive by remember { mutableStateOf(member.isActive) }
    var notes by remember { mutableStateOf(member.notes) }

    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var showUnsavedWarning by remember { mutableStateOf(false) }

    val hasUnsavedChanges = fullName != member.fullName ||
            schoolClass != (member.schoolClass.ifBlank { "أولى ابتدائي" }) ||
            birthDate != member.birthDate ||
            phone != member.phone ||
            parentPhone != member.parentPhone ||
            governorate != member.governorate ||
            center != member.center ||
            area != member.area ||
            street != member.street ||
            houseNumber != member.houseNumber ||
            addressDetails != member.addressDetails ||
            selectedGroupId != member.groupId ||
            isActive != member.isActive ||
            notes != member.notes

    val handleDismissRequest = {
        if (hasUnsavedChanges) {
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
                text = "تعديل بيانات المخدوم",
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

                // Group selector
                ExposedDropdownMenuBox(
                    expanded = groupDropdownExpanded,
                    onExpandedChange = { groupDropdownExpanded = !groupDropdownExpanded }
                ) {
                    val currentGroupName = groups.find { it.id == selectedGroupId }?.name ?: "بدون مجموعة"
                    OutlinedTextField(
                        value = currentGroupName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المجموعة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون مجموعة") },
                            onClick = {
                                selectedGroupId = null
                                groupDropdownExpanded = false
                            }
                        )
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    selectedGroupId = group.id
                                    groupDropdownExpanded = false
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
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("هاتف ولي الأمر") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "بيانات العنوان:",
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
                        label = { Text("علامة مميزة") },
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                }

                // Active status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isActive) "حالة القيد: مخدوم نشط" else "حالة القيد: غير نشط (أرشيف)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BurgundyPrimary
                        )
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
                        val updated = member.copy(
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
                            groupId = selectedGroupId,
                            isActive = isActive,
                            notes = notes.trim()
                        )
                        onConfirm(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = fullName.isNotBlank()
            ) {
                Text("حفظ التعديلات")
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
                    text = "تنبيه: تعديلات غير محفوظة",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text("لقد قمت بتعديل بعض بيانات المخدوم. هل أنت متأكد من رغبتك في الإلغاء وتجاهل هذه التعديلات؟")
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
                    Text("متابعة التعديل")
                }
            }
        )
    }
}

@Composable
fun PrintableIdCardDialog(
    member: Member,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExportingPdf by remember { mutableStateOf(false) }

    val idCardBitmap = remember(member) {
        DeaconPdfPrinter.createIdCardBitmap(context, member, 1100, 700)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = GoldSecondary)
                Text("كارنيه الشماس الشامل للطباعة", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card Preview Frame
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        bitmap = idCardBitmap.asImageBitmap(),
                        contentDescription = "كارنيه ${member.fullName}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    )
                }

                Text(
                    text = "كارنيه هوية معتمد يحتوي على بيانات المخدوم وصورته والباركود المربع الذكي QR Code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Actions Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Print & Export PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                DeaconPdfPrinter.printMemberIdCard(context, member)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طباعة الكارنيه", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExportingPdf = true
                                    try {
                                        val pdfFile = DeaconPdfPrinter.exportMemberIdCardPdf(context, member)
                                        DeaconPdfPrinter.openPdfFile(context, pdfFile)
                                    } finally {
                                        isExportingPdf = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isExportingPdf
                        ) {
                            if (isExportingPdf) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    // Row 2: Save as Image & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val barcodeId = BarcodeGenerator.getBarcodeId(member.id)
                                BarcodeGenerator.saveBarcodeToGallery(context, idCardBitmap, barcodeId, "كارنيه_${member.fullName}")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ كصورة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val pdfFile = DeaconPdfPrinter.exportMemberIdCardPdf(context, member)
                                    DeaconPdfPrinter.sharePdfFile(context, pdfFile, "كارنيه الشماس: ${member.fullName}")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun QuickRecordAttendanceDialog(
    member: Member,
    initialType: AttendanceType = AttendanceType.SERVICE,
    onDismiss: () -> Unit,
    onSave: (AttendanceType, AttendanceStatus, String, Int?, String) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedMassNumber by remember { mutableIntStateOf(1) }
    var attendanceDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
    }
    var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            cal.time = sdf.parse(attendanceDate) ?: java.util.Date()
        } catch (_: Exception) {}

        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                attendanceDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (selectedType == AttendanceType.SERVICE) Icons.Default.DateRange else Icons.Default.Church,
                    contentDescription = null,
                    tint = BurgundyPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الحضور للمخدوم", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Type selector: Service vs Mass
                Text("نوع الحضور:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == AttendanceType.SERVICE,
                        onClick = { selectedType = AttendanceType.SERVICE },
                        label = { Text("حصة الخدمة", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedType == AttendanceType.MASS,
                        onClick = { selectedType = AttendanceType.MASS },
                        label = { Text("القداس الإلهي", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.Church, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                if (selectedType == AttendanceType.MASS) {
                    Text("رقم القداس:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMassNumber == 1,
                            onClick = { selectedMassNumber = 1 },
                            label = { Text("القداس الأول", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMassNumber == 2,
                            onClick = { selectedMassNumber = 2 },
                            label = { Text("القداس الثاني", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Date Picker field
                OutlinedTextField(
                    value = attendanceDate,
                    onValueChange = { attendanceDate = it },
                    label = { Text("تاريخ الحضور") },
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "اختيار التاريخ", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    trailingIcon = {
                        TextButton(onClick = {
                            attendanceDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                        }) {
                            Text("اليوم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Status selector
                Text("حالة الحضور:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == AttendanceStatus.PRESENT,
                        onClick = { selectedStatus = AttendanceStatus.PRESENT },
                        label = { Text("حاضر", color = if (selectedStatus == AttendanceStatus.PRESENT) Color.White else PresentGreen, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PresentGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedStatus == AttendanceStatus.ABSENT,
                        onClick = { selectedStatus = AttendanceStatus.ABSENT },
                        label = { Text("غائب", color = if (selectedStatus == AttendanceStatus.ABSENT) Color.White else AbsentRed, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AbsentRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedStatus == AttendanceStatus.EXCUSED,
                        onClick = { selectedStatus = AttendanceStatus.EXCUSED },
                        label = { Text("إذن / عذر", color = if (selectedStatus == AttendanceStatus.EXCUSED) Color.Black else GoldSecondary, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldSecondary,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    placeholder = { Text("أي ملاحظات إضافية...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (attendanceDate.isNotBlank()) {
                        val massNum = if (selectedType == AttendanceType.MASS) selectedMassNumber else null
                        onSave(selectedType, selectedStatus, attendanceDate.trim(), massNum, notes.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ الحضور")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRecordHymnDialog(
    member: Member,
    hymns: List<com.example.data.model.Hymn>,
    onDismiss: () -> Unit,
    onSave: (Long, Double, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedHymn by remember { mutableStateOf(hymns.firstOrNull()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var assessmentDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
    }
    var scoreText by remember { mutableStateOf("10") }
    var maxScoreText by remember(selectedHymn) { mutableStateOf("${selectedHymn?.maxScore?.toInt() ?: 10}") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            cal.time = sdf.parse(assessmentDate) ?: java.util.Date()
        } catch (_: Exception) {}

        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                assessmentDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = BurgundyPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل تسميع لحن", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Hymn Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedHymn?.name ?: "اختر اللحن من المنهج",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اسم اللحن المقرر") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        hymns.forEach { hymn ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(hymn.name, fontWeight = FontWeight.Bold)
                                        Text("${hymn.category} • الدرجة: ${hymn.maxScore}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    selectedHymn = hymn
                                    maxScoreText = "${hymn.maxScore.toInt()}"
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker
                OutlinedTextField(
                    value = assessmentDate,
                    onValueChange = { assessmentDate = it },
                    label = { Text("تاريخ التسميع") },
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "اختيار التاريخ", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    trailingIcon = {
                        TextButton(onClick = {
                            assessmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                        }) {
                            Text("اليوم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Scores Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = {
                            scoreText = it
                            errorMessage = null
                        },
                        label = { Text("الدرجة المحققة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxScoreText,
                        onValueChange = { maxScoreText = it },
                        label = { Text("الدرجة النهائية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات التسميع (اختياري)") },
                    placeholder = { Text("مثال: إتقان ممتاز للوقفات، يحتاج مراجعة الربع الثاني...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hymn = selectedHymn
                    if (hymn == null) {
                        errorMessage = "يرجى اختيار اللحن المراد تقييمه"
                        return@Button
                    }
                    val score = scoreText.toDoubleOrNull()
                    val maxScore = maxScoreText.toDoubleOrNull() ?: hymn.maxScore
                    if (score == null || score < 0 || score > maxScore) {
                        errorMessage = "يرجى إدخال درجة صحيحة بين 0 و $maxScore"
                        return@Button
                    }
                    onSave(hymn.id, score, maxScore, assessmentDate.trim(), notes.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ التسميع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRecordBibleDialog(
    member: Member,
    lessons: List<com.example.data.model.BibleLesson>,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, String, String, Long?) -> Unit
) {
    val context = LocalContext.current
    var selectedLesson by remember { mutableStateOf<BibleLesson?>(lessons.firstOrNull()) }
    var lessonName by remember { mutableStateOf(selectedLesson?.name ?: "") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var assessmentDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
    }
    var scoreText by remember { mutableStateOf("10") }
    var maxScoreText by remember { mutableStateOf("10") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            cal.time = sdf.parse(assessmentDate) ?: java.util.Date()
        } catch (_: Exception) {}

        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                assessmentDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = BurgundyPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل تسميع الكتاب المقدس", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Lesson Selector / Input
                if (lessons.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = lessonName,
                            onValueChange = {
                                lessonName = it
                                selectedLesson = null
                            },
                            label = { Text("الدرس أو الإصحاح المقرر") },
                            placeholder = { Text("اختر من القائمة أو اكتب اسماً مخصصاً") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            lessons.forEach { lesson ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(lesson.name, fontWeight = FontWeight.Bold)
                                            if (lesson.notes.isNotBlank()) {
                                                Text(lesson.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedLesson = lesson
                                        lessonName = lesson.name
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = lessonName,
                        onValueChange = { lessonName = it },
                        label = { Text("اسم الدرس / الإصحاح") },
                        placeholder = { Text("مثال: إنجيل متى 5 - الموعظة على الجبل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Date Picker
                OutlinedTextField(
                    value = assessmentDate,
                    onValueChange = { assessmentDate = it },
                    label = { Text("تاريخ التسميع") },
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "اختيار التاريخ", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    trailingIcon = {
                        TextButton(onClick = {
                            assessmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                        }) {
                            Text("اليوم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Scores Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = {
                            scoreText = it
                            errorMessage = null
                        },
                        label = { Text("الدرجة المحققة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxScoreText,
                        onValueChange = { maxScoreText = it },
                        label = { Text("الدرجة النهائية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات التسميع (اختياري)") },
                    placeholder = { Text("مثال: حفظ رائع للشواهد والآية الذهبية...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (lessonName.isBlank()) {
                        errorMessage = "يرجى تحديد أو كتابة اسم الدرس"
                        return@Button
                    }
                    val score = scoreText.toDoubleOrNull()
                    val maxScore = maxScoreText.toDoubleOrNull() ?: 10.0
                    if (score == null || score < 0 || score > maxScore) {
                        errorMessage = "يرجى إدخال درجة صحيحة بين 0 و $maxScore"
                        return@Button
                    }
                    onSave(
                        lessonName.trim(),
                        score,
                        maxScore,
                        assessmentDate.trim(),
                        notes.trim(),
                        selectedLesson?.id
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ التسميع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
