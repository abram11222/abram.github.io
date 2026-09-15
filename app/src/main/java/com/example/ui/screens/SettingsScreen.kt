package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.DeaconsDatabase
import com.example.ui.components.DeaconHeader
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AbsentRedBg
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryContainer
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenBg
import com.example.ui.viewmodel.DeaconsViewModel
import com.example.util.BackupData
import com.example.util.BackupManager
import com.example.util.MergePreviewResult
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    viewModel: DeaconsViewModel,
    onSelectMember: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val servants by viewModel.servants.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val hymns by viewModel.hymns.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val bibleLessons by viewModel.bibleLessons.collectAsStateWithLifecycle()
    val schoolClasses by viewModel.schoolClasses.collectAsStateWithLifecycle()

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showAddServantDialog by remember { mutableStateOf(false) }
    var showAddHymnDialog by remember { mutableStateOf(false) }
    var showAddBibleLessonDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var showMasterDataDialog by remember { mutableStateOf(false) }
    var masterDataInitialTab by remember { mutableIntStateOf(0) }
    var showSchoolClassesDialog by remember { mutableStateOf(false) }
    var showMissingDataDialog by remember { mutableStateOf(false) }
    var showMonthlyFollowUpDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val serviceAttendanceTarget by viewModel.serviceAttendanceTarget.collectAsStateWithLifecycle()
    val serviceMinistryTarget by viewModel.serviceMinistryTarget.collectAsStateWithLifecycle()
    val graceSettings by viewModel.graceSettings.collectAsStateWithLifecycle()
    val overdueMembersInfo by viewModel.overdueMembersInfo.collectAsStateWithLifecycle()
    val overdueCount = overdueMembersInfo.count { it.hasAnyOverdue }
    var isCheckingOverdue by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "تم تفعيل إذن الإشعارات بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "تم رفض إذن الإشعارات", Toast.LENGTH_SHORT).show()
        }
    }

    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var mergePreviewData by remember { mutableStateOf<Pair<BackupData, MergePreviewResult>?>(null) }
    var mergeResultDialogMessage by remember { mutableStateOf<String?>(null) }

    val openBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isRestoring = true
                    val db = DeaconsDatabase.getDatabase(context, scope)
                    val backupData = BackupManager.parseBackupUri(context, uri)
                    val preview = BackupManager.generateMergePreview(backupData, db)
                    mergePreviewData = Pair(backupData, preview)
                } catch (e: Exception) {
                    Toast.makeText(context, "خطأ في قراءة النسخة الاحتياطية: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isRestoring = false
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            DeaconHeader(
                title = "إعدادات الخدمة ومدرسة الشمامسة",
                subtitle = "إدارة المجموعات، الألحان، الامتحانات، وقاعدة البيانات"
            )
        }

        // About the App & Service Official Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(GoldSecondaryContainer)
                            .border(2.dp, GoldSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "شعار خدمة الشمامسة",
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Deacons Service Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BurgundyPrimary
                    )

                    Text(
                        text = "إدارة خدمة الشمامسة",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "مدرسة القديس اسطفانوس للشمامسة بمير - إيبارشية القوصية ومير",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = BurgundyPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حول التطبيق",
                                style = MaterialTheme.typography.labelMedium,
                                color = BurgundyPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                NotificationHelper.showNotification(
                                    context = context,
                                    notificationId = 101,
                                    title = "تنبيه خدمة الشمامسة",
                                    message = "تذكير: موعد حصة الألحان والقداس الإلهي القادم لمدرسة الشمامسة بمير."
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GoldSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "اختبار إشعار",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldSecondary
                            )
                        }
                    }
                }
            }
        }

        // Service & Mass Target Settings Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Church,
                            contentDescription = null,
                            tint = BurgundyPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعدادات أهداف القداسات والخدمة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "تحديد النصاب المستهدف لحضور القداسات الإلهية للشماس شهرياً لحساب نسب التقييم والتقارير:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "عدد القداسات المطلوبة شهرياً:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (serviceAttendanceTarget > 1) {
                                        viewModel.setServiceAttendanceTarget(serviceAttendanceTarget - 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "تقليل القداسات",
                                    tint = BurgundyPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BurgundyPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.widthIn(min = 85.dp)
                            ) {
                                Text(
                                    text = "$serviceAttendanceTarget قداسات",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BurgundyPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (serviceAttendanceTarget < 8) {
                                        viewModel.setServiceAttendanceTarget(serviceAttendanceTarget + 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "زيادة القداسات",
                                    tint = BurgundyPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "عدد الخدمة المطلوبة:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (serviceMinistryTarget > 1) {
                                        viewModel.setServiceMinistryTarget(serviceMinistryTarget - 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "تقليل الخدمة",
                                    tint = GoldSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldSecondary.copy(alpha = 0.15f),
                                modifier = Modifier.widthIn(min = 85.dp)
                            ) {
                                Text(
                                    text = "$serviceMinistryTarget خدمات",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (serviceMinistryTarget < 8) {
                                        viewModel.setServiceMinistryTarget(serviceMinistryTarget + 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "زيادة الخدمة",
                                    tint = GoldSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overdue Grace Period & Automated Notification Settings Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = GoldSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "إعدادات مدة المتابعة وفترات السماح",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldSecondary
                                )
                                Text(
                                    text = "تحديد مهل الغياب وتأخير التسميع والإشعارات التلقائية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Master Notification Switch
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "التنبيهات التلقائية في ستارة النظام",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "فحص دوري يومي في الخلفية وتنبيه الخادم بالمتأخرين",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = graceSettings.notificationsEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.updateGraceSettings(notificationsEnabled = isChecked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GoldSecondary,
                                    checkedTrackColor = BurgundyPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Attendance Absence Threshold
                    GraceThresholdRow(
                        title = "الحد الأقصى لأيام الغياب عن الخدمة:",
                        subtitle = "تنبيه بعد غياب متواصل لمدة:",
                        days = graceSettings.attendanceAbsenceDays,
                        minDays = 1,
                        maxDays = 90,
                        onDaysChanged = { newDays ->
                            viewModel.updateGraceSettings(attendanceDays = newDays)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 2. Hymn Overdue Threshold
                    GraceThresholdRow(
                        title = "الحد الأقصى لتأخير تسميع الألحان:",
                        subtitle = "تنبيه إذا لم يسمع اللحن خلال:",
                        days = graceSettings.hymnOverdueDays,
                        minDays = 1,
                        maxDays = 180,
                        onDaysChanged = { newDays ->
                            viewModel.updateGraceSettings(hymnDays = newDays)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 3. Bible Assessment Overdue Threshold
                    GraceThresholdRow(
                        title = "الحد الأقصى لتأخير تقييم الكتاب المقدس:",
                        subtitle = "تنبيه إذا تأخر تسميع الآيات والدروس لمدة:",
                        days = graceSettings.bibleOverdueDays,
                        minDays = 1,
                        maxDays = 180,
                        onDaysChanged = { newDays ->
                            viewModel.updateGraceSettings(bibleDays = newDays)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 4. Exam Overdue Threshold
                    GraceThresholdRow(
                        title = "الحد الأقصى لتأخير تقديم الامتحان:",
                        subtitle = "تنبيه بعد مرور مدة من فتح الامتحان:",
                        days = graceSettings.examOverdueDays,
                        minDays = 1,
                        maxDays = 90,
                        onDaysChanged = { newDays ->
                            viewModel.updateGraceSettings(examDays = newDays)
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Realtime Overdue Status Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (overdueCount > 0) AbsentRed.copy(alpha = 0.12f) else PresentGreen.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (overdueCount > 0) AbsentRed.copy(alpha = 0.4f) else PresentGreen.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (overdueCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (overdueCount > 0) AbsentRed else PresentGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (overdueCount > 0)
                                        "عدد المخدومين المتأخرين حالياً: $overdueCount مخدوم"
                                    else
                                        "ممتاز! لا يوجد أي مخدوم متأخر عن فترات السماح",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (overdueCount > 0) AbsentRed else PresentGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trigger Overdue Notification Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                isCheckingOverdue = true
                                viewModel.triggerManualOverdueCheck(context) { count ->
                                    isCheckingOverdue = false
                                    Toast.makeText(
                                        context,
                                        "تم فحص المتأخرين بنجاح ($count مخدوم يحتاج متابعة)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                            enabled = !isCheckingOverdue
                        ) {
                            if (isCheckingOverdue) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = GoldSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "فحص وإرسال تنبيه الآن",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(
                                onClick = {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "طلب الإذن",
                                    modifier = Modifier.size(18.dp),
                                    tint = GoldSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إذن التنبيهات", fontSize = 11.sp, color = GoldSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Offline-First Database & Complete JSON Backup & Merge Card
        item {
            var showConfirmClearDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = PresentGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "النسخ الاحتياطي والدمج الآمن (Room DB)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PresentGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• إجمالي المخدومين الحاليين: ${members.size} مخدوم\n• جميع السجلات والبيانات مشفرة ومحفوظة محلياً على الجهاز.\n• عند دمج نسخة احتياطية: لن يتم مسح بياناتك الحالية وسيتم دمج البيانات الجديدة بسلاسة (CURRENT + BACKUP = MERGED).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export Backup
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        isBackingUp = true
                                        val db = DeaconsDatabase.getDatabase(context, scope)
                                        val backupFile = BackupManager.createBackupJson(context, db)
                                        val fileUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            backupFile
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, fileUri)
                                            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - خدمة الشمامسة")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة وحفظ النسخة الاحتياطية"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "فشل إنشاء النسخة الاحتياطية: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isBackingUp = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                            enabled = !isBackingUp && !isRestoring
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير نسخة JSON", style = MaterialTheme.typography.labelMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Import & Merge Backup
                        Button(
                            onClick = {
                                openBackupFileLauncher.launch("application/json")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                            enabled = !isBackingUp && !isRestoring
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استيراد ودمج", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showConfirmClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.AbsentRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = com.example.ui.theme.AbsentRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مسح جميع بيانات المخدومين الحالية",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.AbsentRed
                        )
                    }
                }
            }

            // Merge Preview AlertDialog
            mergePreviewData?.let { (backupData, preview) ->
                AlertDialog(
                    onDismissRequest = { mergePreviewData = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = BurgundyPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("معاينة دمج النسخة الاحتياطية", fontWeight = FontWeight.Bold, color = BurgundyPrimary)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("تاريخ النسخة: ${preview.backupDate}", fontWeight = FontWeight.SemiBold)
                            HorizontalDivider()
                            Text("• إجمالي مخدومي النسخة: ${preview.totalBackupMembers}")
                            Text("• مخدومين جدد سيتم إضافتهم: ${preview.newMembersCount}", color = PresentGreen, fontWeight = FontWeight.Bold)
                            Text("• مخدومين موجودين سيتم تحديث بياناتهم: ${preview.existingMembersCount}", color = GoldSecondary, fontWeight = FontWeight.Bold)
                            Text("• سجلات حضور للدمج: ${preview.attendancesCount}")
                            Text("• تقييمات ألحان وكتاب مقدس: ${preview.assessmentsCount}")
                            Text("• امتحانات وزيارات: ${preview.examsCount + preview.visitsCount}")
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = PresentGreenBg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ملاحظة أمان: لن يتم حذف أي مخدوم أو سجل مسجل حالياً. سيتم الدمج بنمط (CURRENT + BACKUP = MERGED).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PresentGreen,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val currentData = backupData
                                mergePreviewData = null
                                scope.launch {
                                    try {
                                        val db = DeaconsDatabase.getDatabase(context, scope)
                                        val execResult = BackupManager.executeMerge(currentData, db)
                                        mergeResultDialogMessage = execResult.message
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "فشل إتمام الدمج: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                        ) {
                            Text("تأكيد الدمج والاحتفاظ بالكل")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { mergePreviewData = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Merge Result Dialog
            mergeResultDialogMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { mergeResultDialogMessage = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PresentGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اكتمل الدمج بنجاح", fontWeight = FontWeight.Bold, color = PresentGreen)
                        }
                    },
                    text = {
                        Text(msg, style = MaterialTheme.typography.bodyMedium)
                    },
                    confirmButton = {
                        Button(
                            onClick = { mergeResultDialogMessage = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PresentGreen)
                        ) {
                            Text("حسناً")
                        }
                    }
                )
            }

            if (showConfirmClearDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmClearDialog = false },
                    title = {
                        Text(
                            text = "تأكيد مسح بيانات المخدومين",
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.AbsentRed
                        )
                    },
                    text = {
                        Text("هل أنت متأكد من رغبتك في حذف جميع بيانات المخدومين من قاعدة البيانات؟ سيمكنك بعدها استيراد ملف Excel المعتمد أو البدء من الصفر.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearAllMembersDatabase()
                                showConfirmClearDialog = false
                                Toast.makeText(context, "تم مسح جميع بيانات المخدومين بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AbsentRed)
                        ) {
                            Text("تأكيد المسح")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmClearDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }

        // =============================================================
        // MASTER DATA MANAGEMENT SECTION (إدارة البيانات الأساسية)
        // =============================================================
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(
                title = "إدارة البيانات الأساسية",
                icon = Icons.Default.Assignment
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "المنهج الدراسي وقوائم التقييم المعتمدة للخدمة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. إدارة الألحان
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BurgundyPrimary.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = BurgundyPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "إدارة الألحان",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BurgundyPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BurgundyPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${hymns.size} لحن مسجل",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BurgundyPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddHymnDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة لحن سريع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        masterDataInitialTab = 0
                                        showMasterDataDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("عرض وبحث الكل", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // 2. دروس / أجزاء الإنجيل
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldSecondary.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = GoldSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "دروس / أجزاء الإنجيل",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GoldSecondary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${bibleLessons.size} درس مسجل",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BurgundyPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddBibleLessonDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة درس سريع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        masterDataInitialTab = 1
                                        showMasterDataDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("عرض وبحث الكل", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // 3. الامتحانات
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PresentGreen.copy(alpha = 0.06f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Quiz,
                                        contentDescription = null,
                                        tint = PresentGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "الامتحانات الدورية",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PresentGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${exams.size} امتحان مسجل",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PresentGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddExamDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة امتحان سريع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        masterDataInitialTab = 2
                                        showMasterDataDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("عرض وبحث الكل", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Auxiliary Master Tools (School Classes, Missing Data, Monthly Follow-Up)
                    Text(
                        text = "أدوات التنظيم والمتابعة الإدارية:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSchoolClassesDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الفصول الدراسية (${schoolClasses.size})", fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showMissingDataDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("البيانات الناقصة", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = { showMonthlyFollowUpDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = BurgundyPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("المتابعة الشهرية والغياب المتكرر", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Groups Management Section
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(
                title = "مجموعات الخدمة (${groups.size})",
                icon = Icons.Default.Groups,
                actionText = "+ إضافة مجموعة",
                onActionClick = { showAddGroupDialog = true }
            )
        }

        items(groups) { group ->
            val count = members.count { it.groupId == group.id }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (group.description.isNotBlank()) {
                            Text(
                                text = group.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BurgundyPrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "$count مخدوم",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Servants Management Section
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(
                title = "خدام مدرسة الشمامسة (${servants.size})",
                icon = Icons.Default.Person,
                actionText = "+ إضافة خادم",
                onActionClick = { showAddServantDialog = true }
            )
        }

        items(servants) { servant ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            text = servant.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (servant.phone.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = servant.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldSecondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = servant.role,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Footer & Copyright
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "© 2026 Kirolos Sabry Fouad. All Rights Reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Developed by Kirolos Sabry Fouad",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    if (showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { name, stage ->
                viewModel.addGroup(name, stage)
                showAddGroupDialog = false
            }
        )
    }

    if (showAddServantDialog) {
        AddServantDialog(
            onDismiss = { showAddServantDialog = false },
            onConfirm = { name, phone, role ->
                viewModel.addServant(name, phone, role)
                showAddServantDialog = false
            }
        )
    }

    if (showAddHymnDialog) {
        HymnFormDialog(
            title = "إضافة لحن جديد للمنهج",
            onDismiss = { showAddHymnDialog = false },
            onConfirm = { name, category, maxScore, notes, isActive ->
                viewModel.addHymn(
                    name = name,
                    maxScore = maxScore,
                    category = category,
                    notes = notes,
                    isActive = isActive
                )
                showAddHymnDialog = false
            }
        )
    }

    if (showAddBibleLessonDialog) {
        BibleLessonFormDialog(
            title = "إضافة درس / جزء إنجيل جديد",
            onDismiss = { showAddBibleLessonDialog = false },
            onConfirm = { name, notes, isActive ->
                viewModel.addBibleLesson(
                    name = name,
                    notes = notes,
                    isActive = isActive
                )
                showAddBibleLessonDialog = false
            }
        )
    }

    if (showAddExamDialog) {
        ExamFormDialog(
            title = "إضافة امتحان دوري جديد",
            initialDate = viewModel.todayDate,
            onDismiss = { showAddExamDialog = false },
            onConfirm = { name, date, maxScore, notes, isActive ->
                viewModel.addExam(
                    name = name,
                    date = date,
                    maxScore = maxScore,
                    notes = notes,
                    isActive = isActive
                )
                showAddExamDialog = false
            }
        )
    }

    if (showMasterDataDialog) {
        MasterDataManagementDialog(
            viewModel = viewModel,
            initialTab = masterDataInitialTab,
            onDismiss = { showMasterDataDialog = false }
        )
    }

    if (showSchoolClassesDialog) {
        SchoolClassesManagementDialog(
            viewModel = viewModel,
            onDismiss = { showSchoolClassesDialog = false }
        )
    }

    if (showMissingDataDialog) {
        MissingDataAuditDialog(
            members = members,
            onSelectMember = { memberId ->
                showMissingDataDialog = false
                onSelectMember?.invoke(memberId)
            },
            onDismiss = { showMissingDataDialog = false }
        )
    }

    if (showMonthlyFollowUpDialog) {
        MonthlyFollowUpDialog(
            viewModel = viewModel,
            onSelectMember = { memberId ->
                showMonthlyFollowUpDialog = false
                onSelectMember?.invoke(memberId)
            },
            onDismiss = { showMonthlyFollowUpDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutAppDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
fun AboutAppDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(GoldSecondaryContainer)
                        .border(2.dp, GoldSecondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "شعار خدمة الشمامسة",
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Deacons Service Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BurgundyPrimary
                )

                Text(
                    text = "إدارة خدمة الشمامسة",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "خدمة الشمامسة\nكنيسة أبي سيفين والعزب ودير الملاك ميخائيل\nإيبارشية القوصية ومير",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "الإصدار: 1.0.0 (Offline-First)",
                    style = MaterialTheme.typography.labelSmall,
                    color = BurgundyPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "© 2026 Kirolos Sabry Fouad. All Rights Reserved.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BurgundyPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Developed by Kirolos Sabry Fouad",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldSecondary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("ابتدائي") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مجموعة جديدة", fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المجموعة (مثال: مجموعة القديس يوحنا)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = stage,
                    onValueChange = { stage = it },
                    label = { Text("المرحلة الدراسية") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), stage.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                enabled = name.isNotBlank()
            ) {
                Text("حفظ المجموعة")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun AddServantDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("خادم ألحان") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة خادم جديد", fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الخادم *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("الدور / المسؤولية (مثال: أمين الخدمة، خادم ألحان)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), phone.trim(), role.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                enabled = name.isNotBlank()
            ) {
                Text("حفظ الخادم")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun GraceThresholdRow(
    title: String,
    subtitle: String,
    days: Int,
    minDays: Int = 1,
    maxDays: Int = 365,
    onDaysChanged: (Int) -> Unit
) {
    var isWeeksMode by remember { mutableStateOf(days >= 7 && days % 7 == 0) }
    val step = if (isWeeksMode) 7 else 1

    val weeksCount = days / 7
    val remainingDays = days % 7
    val displayText = when {
        days == 1 -> "1 يوم"
        days == 2 -> "يومان"
        days in 3..10 && isWeeksMode -> "$days أيام ($weeksCount ${if (weeksCount == 1) "أسبوع" else "أسابيع"})"
        days in 3..10 -> "$days أيام"
        days % 7 == 0 -> "$days يوم ($weeksCount ${if (weeksCount == 1) "أسبوع" else if (weeksCount == 2) "أسبوعين" else if (weeksCount in 3..10) "أسابيع" else "أسبوع"})"
        weeksCount > 0 -> "$days يوم ($weeksCount أسبوع و $remainingDays يوم)"
        else -> "$days يوم"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Days / Weeks toggle selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (!isWeeksMode) BurgundyPrimary else Color.Transparent,
                    modifier = Modifier.clickable { isWeeksMode = false }
                ) {
                    Text(
                        text = "أيام",
                        fontSize = 11.sp,
                        fontWeight = if (!isWeeksMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isWeeksMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isWeeksMode) BurgundyPrimary else Color.Transparent,
                    modifier = Modifier.clickable { isWeeksMode = true }
                ) {
                    Text(
                        text = "أسابيع",
                        fontSize = 11.sp,
                        fontWeight = if (isWeeksMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (isWeeksMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val newDays = (days - step).coerceAtLeast(minDays)
                        onDaysChanged(newDays)
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    enabled = days > minDays
                ) {
                    Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldSecondary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val newDays = (days + step).coerceAtMost(maxDays)
                        onDaysChanged(newDays)
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    enabled = days < maxDays
                ) {
                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

