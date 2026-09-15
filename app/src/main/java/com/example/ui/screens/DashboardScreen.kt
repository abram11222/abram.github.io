package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.data.model.Member
import com.example.ui.components.DeaconHeader
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AbsentRedBg
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.ExcusedAmber
import com.example.ui.theme.ExcusedAmberBg
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenBg
import com.example.ui.viewmodel.ClassSummaryStats
import com.example.ui.viewmodel.DeaconsViewModel
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DeaconsViewModel,
    onNavigateToMembers: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToAssessments: () -> Unit,
    onNavigateToReports: () -> Unit,
    onSelectMember: (Long) -> Unit,
    onNavigateToAdmin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val attendances by viewModel.attendances.collectAsStateWithLifecycle()
    val hymnAssessments by viewModel.hymnAssessments.collectAsStateWithLifecycle()
    val overdueMembersInfo by viewModel.overdueMembersInfo.collectAsStateWithLifecycle()
    val classStatsList by viewModel.classStatsList.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val followUpOverdue = overdueMembersInfo.filter { it.hasAnyOverdue }
    val today = viewModel.todayDate

    // Tab state: 0 = Overview, 1 = Class Dashboard
    var selectedDashboardTab by remember { mutableIntStateOf(0) }
    var selectedStageFilter by remember { mutableStateOf("الكل") }
    var classSearchQuery by remember { mutableStateOf("") }

    // Compute Overall Metrics
    val totalMembers = members.size
    val totalGroups = groups.size

    val serviceAttendances = attendances.filter { it.type == AttendanceType.SERVICE }
    val presentCount = serviceAttendances.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = serviceAttendances.count { it.status == AttendanceStatus.ABSENT }
    val serviceRate = if (serviceAttendances.isNotEmpty()) {
        (presentCount * 100) / serviceAttendances.size
    } else 0

    // Mass Attendance for current month
    val massAttendances = attendances.filter { it.type == AttendanceType.MASS }
    val massPresent = massAttendances.count { it.status == AttendanceStatus.PRESENT }
    val massRatioText = "$massPresent / ${members.size * 2}"

    // Top performers
    val topMembers = members.take(4)

    // Filtered Classes for Tab 1
    val filteredClassStats = remember(classStatsList, selectedStageFilter, classSearchQuery) {
        classStatsList.filter { stat ->
            val matchesStage = when (selectedStageFilter) {
                "ابتدائي" -> stat.stage == "ابتدائي"
                "إعدادي" -> stat.stage == "إعدادي"
                "ثانوي" -> stat.stage == "ثانوي"
                else -> true
            }
            val matchesSearch = classSearchQuery.isBlank() || stat.className.contains(classSearchQuery.trim(), ignoreCase = true)
            matchesStage && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            DeaconHeader(
                title = "لوحة التحكم والمتابعة",
                subtitle = "مدرسة الشمامسة - القديس اسطفانوس بمير"
            )
        }

        // Welcome / Role Strip with quick Admin button if applicable
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "مرحباً، ${currentUser?.fullName ?: "الخادم"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                        Text(
                            text = if (isAdmin) "صلاحيات: مسؤول النظام (أدمن)" else "الرتبة: خادم مخصص",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isAdmin && onNavigateToAdmin != null) {
                        OutlinedButton(
                            onClick = onNavigateToAdmin,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp), tint = BurgundyPrimary)
                            Spacer(Modifier.width(4.dp))
                            Text("إدارة الصلاحيات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Mode Tabs: Overview vs Class Dashboard
        item {
            PrimaryTabRow(
                selectedTabIndex = selectedDashboardTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Tab(
                    selected = selectedDashboardTab == 0,
                    onClick = { selectedDashboardTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("نظرة عامة والخدمة", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedDashboardTab == 1,
                    onClick = { selectedDashboardTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("داشبورد الفصول (${classStatsList.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        // -------------------------------------------------------------
        // TAB 0: OVERVIEW & GENERAL SERVICE METRICS
        // -------------------------------------------------------------
        if (selectedDashboardTab == 0) {
            // Stats Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            title = "إجمالي المخدومين",
                            value = "$totalMembers مخدوم",
                            icon = Icons.Default.People,
                            accentColor = BurgundyPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "الفصول الدراسية",
                            value = "${classStatsList.size} فصول",
                            icon = Icons.Default.School,
                            accentColor = GoldSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            title = "حضور الخدمة",
                            value = "$presentCount حاضر",
                            icon = Icons.Default.CheckCircle,
                            accentColor = PresentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "غياب الخدمة",
                            value = "$absentCount غائب",
                            icon = Icons.Default.Close,
                            accentColor = AbsentRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            title = "حضور القداسات (شهرياً)",
                            value = massRatioText,
                            icon = Icons.Default.Church,
                            accentColor = Color(0xFF673AB7),
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "متوسط نسبة الحضور",
                            value = "$serviceRate%",
                            icon = Icons.Default.TrendingUp,
                            accentColor = Color(0xFF00796B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Actions Row
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "الإجراءات السريعة", icon = Icons.Default.Assignment)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        QuickActionCard(
                            title = "تسجيل الحضور",
                            subtitle = "خدمة وقداسات",
                            icon = Icons.Default.CalendarMonth,
                            color = BurgundyPrimary,
                            onClick = onNavigateToAttendance
                        )
                    }
                    item {
                        QuickActionCard(
                            title = "تسميع الألحان",
                            subtitle = "تسجيل الدرجات",
                            icon = Icons.Default.MusicNote,
                            color = GoldSecondary,
                            onClick = onNavigateToAssessments
                        )
                    }
                    item {
                        QuickActionCard(
                            title = "إضافة مخدوم",
                            subtitle = "بيانات جديدة",
                            icon = Icons.Default.Add,
                            color = PresentGreen,
                            onClick = onNavigateToMembers
                        )
                    }
                    item {
                        QuickActionCard(
                            title = "التقارير الشاملة",
                            subtitle = "تصدير ومعاينة Excel",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFF0288D1),
                            onClick = onNavigateToReports
                        )
                    }
                }
            }

            // Top Performers Section
            if (members.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "أفضل المخدومين تميزاً",
                        icon = Icons.Default.Star,
                        actionText = "عرض الكل",
                        onActionClick = onNavigateToMembers
                    )
                }

                items(topMembers) { member ->
                    TopMemberCard(
                        member = member,
                        onClick = { onSelectMember(member.id) }
                    )
                }

                // Follow-up Required Section
                if (followUpOverdue.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(
                            title = "مخدومين يحتاجون متابعة (${followUpOverdue.size})",
                            icon = Icons.Default.Warning
                        )
                    }

                    items(followUpOverdue, key = { it.member.id }) { overdueInfo ->
                        FollowUpMemberCard(
                            member = overdueInfo.member,
                            reason = overdueInfo.summaryReasons.joinToString(" • "),
                            isAttendanceAlert = overdueInfo.isAttendanceOverdue,
                            onClick = { onSelectMember(overdueInfo.member.id) }
                        )
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = BurgundyPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "قاعدة البيانات جاهزة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BurgundyPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يمكنك إضافة مخدومين جدد أو تصفح داشبورد الفصول لمعرفة إحصائيات كل فصل.",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // TAB 1: DEDICATED CLASS-BY-CLASS DASHBOARD
        // -------------------------------------------------------------
        if (selectedDashboardTab == 1) {
            // Stage Filters & Search
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Stage Filter Chips
                    val stages = listOf("الكل", "ابتدائي", "إعدادي", "ثانوي")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(stages) { stage ->
                            FilterChip(
                                selected = selectedStageFilter == stage,
                                onClick = { selectedStageFilter = stage },
                                label = { Text(if (stage == "الكل") "جميع المراحل" else "المرحلة الـ$stage") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BurgundyPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Class Name Search
                    OutlinedTextField(
                        value = classSearchQuery,
                        onValueChange = { classSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث عن فصل بالاسم...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (classSearchQuery.isNotBlank()) {
                                IconButton(onClick = { classSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Top Class Highlights Cards (Best Class, Total Students, Follow-ups)
            item {
                val bestClass = classStatsList.filter { it.totalMembers > 0 }.maxByOrNull { it.attendanceRate }
                val totalStudentsInClasses = classStatsList.sumOf { it.totalMembers }
                val totalOverdueInClasses = classStatsList.sumOf { it.overdueFollowUpCount }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Best Performing Class
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PresentGreenBg)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("أعلى فصل حضوراً", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PresentGreen)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = bestClass?.className ?: "لا توجد بيانات",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PresentGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (bestClass != null) "${bestClass.attendanceRate}% نسبة الحضور" else "-",
                                    fontSize = 12.sp,
                                    color = PresentGreen
                                )
                            }
                        }

                        // Total Follow-up Needed
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (totalOverdueInClasses > 0) AbsentRedBg else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AbsentRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("يحتاجون افتقاد", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AbsentRed)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "$totalOverdueInClasses شماس",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AbsentRed
                                )
                                Text(
                                    text = "في جميع الفصول",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "تفاصيل الفصول ومعدلات الحضور (${filteredClassStats.size})",
                    icon = Icons.Default.School
                )
            }

            if (filteredClassStats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد فصول مطابقة للبحث أو المرحلة المختارة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredClassStats, key = { it.className }) { classStat ->
                    ClassDashboardCard(
                        stat = classStat,
                        onRecordAttendance = {
                            viewModel.setSelectedSchoolClass(classStat.className)
                            onNavigateToAttendance()
                        },
                        onViewMembers = {
                            viewModel.setSelectedSchoolClass(classStat.className)
                            onNavigateToMembers()
                        },
                        onAssessments = {
                            viewModel.setSelectedSchoolClass(classStat.className)
                            onNavigateToAssessments()
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ClassDashboardCard(
    stat: ClassSummaryStats,
    onRecordAttendance: () -> Unit,
    onViewMembers: () -> Unit,
    onAssessments: () -> Unit
) {
    val rateColor = when {
        stat.attendanceRate >= 75 -> PresentGreen
        stat.attendanceRate >= 50 -> GoldSecondary
        else -> AbsentRed
    }

    val rateBg = when {
        stat.attendanceRate >= 75 -> PresentGreenBg
        stat.attendanceRate >= 50 -> Color(0xFFFFF8E1)
        else -> AbsentRedBg
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Class Name, Stage Badge, and Attendance Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = BurgundyPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.School, contentDescription = null, tint = BurgundyPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stat.className,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "مرحلة ${stat.stage}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Attendance Rate Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = rateBg,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${stat.attendanceRate}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = rateColor
                        )
                        Text(
                            text = if (stat.hasRecordsToday) "حضور اليوم" else "نسبة عامة",
                            fontSize = 10.sp,
                            color = rateColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Proportional Visual Bar for Attendance
            val totalToday = (stat.todayPresent + stat.todayAbsent + stat.todayExcused).coerceAtLeast(1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (stat.todayPresent > 0) {
                    Box(
                        modifier = Modifier
                            .weight(stat.todayPresent.toFloat())
                            .fillMaxSize()
                            .background(PresentGreen)
                    )
                }
                if (stat.todayExcused > 0) {
                    Box(
                        modifier = Modifier
                            .weight(stat.todayExcused.toFloat())
                            .fillMaxSize()
                            .background(ExcusedAmber)
                    )
                }
                if (stat.todayAbsent > 0) {
                    Box(
                        modifier = Modifier
                            .weight(stat.todayAbsent.toFloat())
                            .fillMaxSize()
                            .background(AbsentRed)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Metrics Grid (Detailed Numbers)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(
                    label = "المخدومين",
                    value = "${stat.totalMembers}",
                    color = MaterialTheme.colorScheme.onSurface
                )
                MetricPill(
                    label = "حاضر اليوم",
                    value = "${stat.todayPresent}",
                    color = PresentGreen
                )
                MetricPill(
                    label = "غائب اليوم",
                    value = "${stat.todayAbsent}",
                    color = AbsentRed
                )
                MetricPill(
                    label = "معتذر",
                    value = "${stat.todayExcused}",
                    color = ExcusedAmber
                )
            }

            Spacer(Modifier.height(8.dp))

            // Secondary Info Strip: Hymns Avg, Mass Count, Overdue follow-up
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "الألحان: ${stat.averageHymnScore?.let { "$it/10" } ?: "غير مرصود"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Church, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "القداسات: ${stat.monthlyMassCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (stat.overdueFollowUpCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AbsentRed, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${stat.overdueFollowUpCount} شماس للافتقاد",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AbsentRed
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons for this class
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRecordAttendance,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تسجيل الحضور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onViewMembers,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("المخدومين", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAssessments,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("الألحان", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TopMemberCard(
    member: Member,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() }
            .testTag("top_member_${member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BurgundyPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.fullName.firstOrNull()?.toString() ?: "م",
                    fontWeight = FontWeight.Bold,
                    color = BurgundyPrimary,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${member.schoolClass} — ${member.area}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PresentGreenBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "متميز",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresentGreen
                    )
                }
            }
        }
    }
}

@Composable
fun FollowUpMemberCard(
    member: Member,
    reason: String,
    isAttendanceAlert: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val alertAccentColor = if (isAttendanceAlert) AbsentRed else Color(0xFFE65100)
    val alertBgColor = if (isAttendanceAlert) AbsentRedBg else Color(0xFFFFF3E0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() }
            .testTag("followup_member_${member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(alertBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAttendanceAlert) Icons.Default.Close else Icons.Default.Alarm,
                        contentDescription = null,
                        tint = alertAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${member.schoolClass} — هاتف: ${member.parentPhone.ifBlank { member.phone.ifBlank { "غير متوفر" } }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = alertBgColor
                ) {
                    Text(
                        text = if (isAttendanceAlert) "غياب متكرر" else "مطلوب متابعة",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = alertAccentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic overdue reasons description
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = reason,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Call & Visit Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val callPhone = member.parentPhone.ifBlank { member.phone }
                if (callPhone.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$callPhone"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("call_parent_button_${member.id}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = alertAccentColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "اتصال",
                            modifier = Modifier.size(16.dp),
                            tint = alertAccentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اتصال بولي الأمر",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = alertAccentColor
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onClick() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("visit_button_${member.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "افتقاد",
                        modifier = Modifier.size(16.dp),
                        tint = GoldSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تسجيل افتقاد",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary
                    )
                }
            }
        }
    }
}
