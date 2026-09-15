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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BibleAssessment
import com.example.data.model.BibleLesson
import com.example.data.model.Exam
import com.example.data.model.ExamResult
import com.example.data.model.Hymn
import com.example.data.model.HymnAssessment
import com.example.data.model.Member
import com.example.ui.components.DeaconHeader
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.ui.viewmodel.DeaconsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentsScreen(
    viewModel: DeaconsViewModel,
    onSelectMember: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val hymns by viewModel.hymns.collectAsStateWithLifecycle()
    val hymnAssessments by viewModel.hymnAssessments.collectAsStateWithLifecycle()
    val bibleLessons by viewModel.bibleLessons.collectAsStateWithLifecycle()
    val bibleAssessments by viewModel.bibleAssessments.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val examResults by viewModel.examResults.collectAsStateWithLifecycle()
    val canRecordAssessments by viewModel.canRecordAssessments.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("الألحان والتسميع", "الكتاب المقدس", "الامتحانات")

    var showAddHymnDialog by remember { mutableStateOf(false) }
    var showRecordHymnAssessmentDialog by remember { mutableStateOf(false) }
    var showAddBibleLessonDialog by remember { mutableStateOf(false) }
    var showRecordBibleAssessmentDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var showRecordExamResultDialog by remember { mutableStateOf(false) }
    var hymnAssessmentToEdit by remember { mutableStateOf<HymnAssessment?>(null) }
    var bibleAssessmentToEdit by remember { mutableStateOf<BibleAssessment?>(null) }
    var showMissingAcademicAuditDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (canRecordAssessments) {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> showRecordHymnAssessmentDialog = true
                            1 -> showRecordBibleAssessmentDialog = true
                            2 -> showRecordExamResultDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (selectedTab) {
                                0 -> "تسجيل تسميع لحن"
                                1 -> "تسجيل تقييم إنجيل"
                                else -> "تسجيل نتيجة امتحان"
                            },
                            fontWeight = FontWeight.Bold
                        )
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
                title = "نظام التقييم والدرجات",
                subtitle = "الألحان، دروس الإنجيل، والامتحانات الدورية"
            )

            // Quick Action Bar for Missing Academic Audit
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GoldSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "متابعة النواقص الأكاديمية والتسميعات المتبقية",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { showMissingAcademicAuditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("كشف النواقص", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> HymnsSection(
                    hymns = hymns,
                    assessments = hymnAssessments,
                    members = members,
                    onAddNewHymn = { showAddHymnDialog = true },
                    onDeleteHymn = { hymn -> viewModel.deleteHymn(hymn) },
                    onDeleteAssessment = { item -> viewModel.deleteHymnAssessment(item) },
                    onEditAssessment = { item -> hymnAssessmentToEdit = item },
                    onSelectMember = onSelectMember
                )
                1 -> BibleSection(
                    lessons = bibleLessons,
                    assessments = bibleAssessments,
                    members = members,
                    onAddNewLesson = { showAddBibleLessonDialog = true },
                    onDeleteLesson = { lesson -> viewModel.deleteBibleLesson(lesson) },
                    onDeleteAssessment = { item -> viewModel.deleteBibleAssessment(item) },
                    onEditAssessment = { item -> bibleAssessmentToEdit = item },
                    onSelectMember = onSelectMember
                )
                2 -> ExamsSection(
                    exams = exams,
                    results = examResults,
                    members = members,
                    onAddNewExam = { showAddExamDialog = true },
                    onDeleteExam = { exam -> viewModel.deleteExam(exam) },
                    onDeleteResult = { res -> viewModel.deleteExamResult(res) },
                    onSelectMember = onSelectMember
                )
            }
        }
    }

    if (showAddHymnDialog) {
        AddHymnDialog(
            onDismiss = { showAddHymnDialog = false },
            onConfirm = { name, maxScore, category ->
                viewModel.addHymn(name, maxScore, category)
                showAddHymnDialog = false
            }
        )
    }

    if (showRecordHymnAssessmentDialog) {
        RecordHymnAssessmentDialog(
            members = members,
            hymns = hymns,
            existingAssessments = hymnAssessments,
            defaultDate = viewModel.todayDate,
            onDismiss = { showRecordHymnAssessmentDialog = false },
            onConfirm = { memberId, hymnId, date, score, maxScore, notes ->
                viewModel.recordHymnAssessment(memberId, hymnId, date, score, maxScore, notes)
                showRecordHymnAssessmentDialog = false
            }
        )
    }

    if (hymnAssessmentToEdit != null) {
        val currentItem = hymnAssessmentToEdit!!
        val member = members.find { it.id == currentItem.memberId }
        val hymn = hymns.find { it.id == currentItem.hymnId }
        EditHymnAssessmentDialog(
            assessment = currentItem,
            hymnName = hymn?.name ?: "لحن",
            memberName = member?.fullName ?: "مخدوم",
            onDismiss = { hymnAssessmentToEdit = null },
            onConfirm = { updated ->
                viewModel.updateHymnAssessment(updated)
                hymnAssessmentToEdit = null
            }
        )
    }

    if (showAddBibleLessonDialog) {
        AddBibleLessonDialog(
            defaultDate = viewModel.todayDate,
            onDismiss = { showAddBibleLessonDialog = false },
            onConfirm = { name, notes, isActive ->
                viewModel.addBibleLesson(name, notes, isActive)
                showAddBibleLessonDialog = false
            }
        )
    }

    if (showRecordBibleAssessmentDialog) {
        RecordBibleAssessmentDialog(
            members = members,
            lessons = bibleLessons,
            defaultDate = viewModel.todayDate,
            onDismiss = { showRecordBibleAssessmentDialog = false },
            onConfirm = { memberId, date, lessonName, score, maxScore, notes ->
                viewModel.recordBibleAssessment(memberId, date, lessonName, score, maxScore, notes)
                showRecordBibleAssessmentDialog = false
            }
        )
    }

    if (bibleAssessmentToEdit != null) {
        val currentItem = bibleAssessmentToEdit!!
        val member = members.find { it.id == currentItem.memberId }
        EditBibleAssessmentDialog(
            assessment = currentItem,
            memberName = member?.fullName ?: "مخدوم",
            onDismiss = { bibleAssessmentToEdit = null },
            onConfirm = { updated ->
                viewModel.updateBibleAssessment(updated)
                bibleAssessmentToEdit = null
            }
        )
    }

    if (showAddExamDialog) {
        AddExamDialog(
            defaultDate = viewModel.todayDate,
            onDismiss = { showAddExamDialog = false },
            onConfirm = { name, date, maxScore, notes ->
                viewModel.addExam(name, date, maxScore, notes)
                showAddExamDialog = false
            }
        )
    }

    if (showRecordExamResultDialog) {
        RecordExamResultDialog(
            members = members,
            exams = exams,
            onDismiss = { showRecordExamResultDialog = false },
            onConfirm = { examId, memberId, score, notes ->
                viewModel.recordExamResult(examId, memberId, score, notes)
                showRecordExamResultDialog = false
            }
        )
    }

    if (showMissingAcademicAuditDialog) {
        MissingAcademicDataAuditDialog(
            viewModel = viewModel,
            onSelectMember = onSelectMember,
            onDismiss = { showMissingAcademicAuditDialog = false }
        )
    }
}

// -------------------------------------------------------------
// Hymns Section
// -------------------------------------------------------------

@Composable
fun HymnsSection(
    hymns: List<Hymn>,
    assessments: List<HymnAssessment>,
    members: List<Member>,
    onAddNewHymn: () -> Unit,
    onDeleteHymn: (Hymn) -> Unit,
    onDeleteAssessment: (HymnAssessment) -> Unit,
    onEditAssessment: (HymnAssessment) -> Unit,
    onSelectMember: (Long) -> Unit
) {
    val memberMap = members.associateBy { it.id }
    val hymnMap = hymns.associateBy { it.id }
    var hymnToDelete by remember { mutableStateOf<Hymn?>(null) }
    var assessmentToDelete by remember { mutableStateOf<HymnAssessment?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "فهرس ألحان الخدمة (${hymns.size} ألحان)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                        Text(
                            text = "قائمة الألحان المقررة لجميع المراحل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onAddNewHymn,
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة لحن")
                    }
                }
            }
        }

        // Available Hymns Horizontal Cards (if any)
        if (hymns.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "الألحان المسجلة بالفهرس:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        hymns.forEach { h ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(h.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${h.category} — النهاية: ${h.maxScore}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { hymnToDelete = h },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف اللحن",
                                        tint = AbsentRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "سجل درجات التسميع الأخير", icon = Icons.Default.MusicNote)
        }

        if (assessments.isEmpty()) {
            item {
                EmptyStateView(
                    message = "لا توجد سجلات تسميع بعد",
                    subMessage = "اضغط على زر 'تسجيل تسميع لحن' بالأسفل للبدء",
                    icon = Icons.Default.MusicNote
                )
            }
        } else {
            items(assessments) { item ->
                val memberName = memberMap[item.memberId]?.fullName ?: "مخدوم"
                val hymnName = hymnMap[item.hymnId]?.name ?: "لحن"

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                text = memberName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { onSelectMember(item.memberId) }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "لحن: $hymnName - تاريخ: ${item.date}",
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${item.score} / ${item.maxScore}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = { onEditAssessment(item) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "تعديل التسميع",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { assessmentToDelete = item },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف التسميع",
                                    tint = AbsentRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (hymnToDelete != null) {
        val h = hymnToDelete!!
        AlertDialog(
            onDismissRequest = { hymnToDelete = null },
            title = { Text("حذف اللحن", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد بالتأكيد حذف لحن '${h.name}' من الفهرس؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteHymn(h)
                        hymnToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { hymnToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (assessmentToDelete != null) {
        val a = assessmentToDelete!!
        AlertDialog(
            onDismissRequest = { assessmentToDelete = null },
            title = { Text("حذف سجل التسميع", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا التسميع؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAssessment(a)
                        assessmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { assessmentToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Bible Section
// -------------------------------------------------------------

@Composable
fun BibleSection(
    lessons: List<BibleLesson>,
    assessments: List<BibleAssessment>,
    members: List<Member>,
    onAddNewLesson: () -> Unit,
    onDeleteLesson: (BibleLesson) -> Unit,
    onDeleteAssessment: (BibleAssessment) -> Unit,
    onEditAssessment: (BibleAssessment) -> Unit,
    onSelectMember: (Long) -> Unit
) {
    val memberMap = members.associateBy { it.id }
    var lessonToDelete by remember { mutableStateOf<BibleLesson?>(null) }
    var assessmentToDelete by remember { mutableStateOf<BibleAssessment?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Master Bible Syllabus Header & Card
        item {
            Card(
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = GoldSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "فهرس أجزاء ودروس الكتاب المقدس المقررة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onAddNewLesson,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة درس", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الأصحاحات والأجزاء المحفوظة بالمنهج لتقييم الشمامسة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (lessons.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "لا توجد أجزاء إنجيل مسجلة بالفهرس بعد. اضغط (إضافة درس) لإدراج المنهج.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        lessons.forEach { l ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(l.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (l.isActive) PresentGreen.copy(alpha = 0.15f) else AbsentRed.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (l.isActive) "نشط" else "مؤرشف",
                                                color = if (l.isActive) PresentGreen else AbsentRed,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (l.notes.isNotBlank()) {
                                        Text(l.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(
                                    onClick = { lessonToDelete = l },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف الدرس",
                                        tint = AbsentRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "سجل درجات وتسميع الكتاب المقدس", icon = Icons.Default.MenuBook)
        }

        if (assessments.isEmpty()) {
            item {
                EmptyStateView(
                    message = "لا توجد تقييمات إنجيل مسجلة بعد",
                    subMessage = "اضغط على زر 'تسجيل تقييم إنجيل' بالأسفل للبدء",
                    icon = Icons.Default.MenuBook
                )
            }
        } else {
            items(assessments) { item ->
                val memberName = memberMap[item.memberId]?.fullName ?: "مخدوم"

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                text = memberName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { onSelectMember(item.memberId) }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "الدرس: ${item.lessonName} - تاريخ: ${item.date}",
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                            IconButton(
                                onClick = { onEditAssessment(item) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "تعديل التقييم",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { assessmentToDelete = item },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف التقييم",
                                    tint = AbsentRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (lessonToDelete != null) {
        val l = lessonToDelete!!
        AlertDialog(
            onDismissRequest = { lessonToDelete = null },
            title = { Text("حذف درس الإنجيل", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد بالتأكيد حذف درس '${l.name}' من الفهرس؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLesson(l)
                        lessonToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { lessonToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (assessmentToDelete != null) {
        val a = assessmentToDelete!!
        AlertDialog(
            onDismissRequest = { assessmentToDelete = null },
            title = { Text("حذف تقييم الإنجيل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا التقييم؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAssessment(a)
                        assessmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { assessmentToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Exams Section
// -------------------------------------------------------------

@Composable
fun ExamsSection(
    exams: List<Exam>,
    results: List<ExamResult>,
    members: List<Member>,
    onAddNewExam: () -> Unit,
    onDeleteExam: (Exam) -> Unit,
    onDeleteResult: (ExamResult) -> Unit,
    onSelectMember: (Long) -> Unit
) {
    val memberMap = members.associateBy { it.id }
    val examMap = exams.associateBy { it.id }
    var examToDelete by remember { mutableStateOf<Exam?>(null) }
    var resultToDelete by remember { mutableStateOf<ExamResult?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الامتحانات المنشأة (${exams.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                        Text(
                            text = "امتحانات الشمامسة الشهرية ونصف التيرم",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onAddNewExam,
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إنشاء امتحان")
                    }
                }
            }
        }

        // Available Exams List
        if (exams.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "قائمة الامتحانات المقررة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        exams.forEach { ex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("التاريخ: ${ex.date} — النهاية: ${ex.maxScore}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { examToDelete = ex },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف الامتحان",
                                        tint = AbsentRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "نتائج الامتحانات المسجلة", icon = Icons.Default.Grade)
        }

        if (results.isEmpty()) {
            item {
                EmptyStateView(
                    message = "لا توجد نتائج مسجلة للامتحانات",
                    subMessage = "اضغط على الزر لتسجيل درجات مخدوم في امتحان",
                    icon = Icons.Default.Grade
                )
            }
        } else {
            items(results) { res ->
                val memberName = memberMap[res.memberId]?.fullName ?: "مخدوم"
                val exam = examMap[res.examId]
                val examName = exam?.name ?: "امتحان"
                val maxScore = exam?.maxScore ?: 100.0
                val percent = if (maxScore > 0) ((res.score / maxScore) * 100).toInt() else 0

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                text = memberName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onSelectMember(res.memberId) }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = examName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (res.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = res.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PresentGreen
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { resultToDelete = res },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف النتيجة",
                                    tint = AbsentRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (examToDelete != null) {
        val ex = examToDelete!!
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text("حذف الامتحان", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد بالتأكيد حذف امتحان '${ex.name}'؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExam(ex)
                        examToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { examToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (resultToDelete != null) {
        val r = resultToDelete!!
        AlertDialog(
            onDismissRequest = { resultToDelete = null },
            title = { Text("حذف نتيجة الامتحان", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف نتيجة هذا الامتحان؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteResult(r)
                        resultToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { resultToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Validation-Enhanced Dialogs
// -------------------------------------------------------------

@Composable
fun AddHymnDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var maxScoreStr by remember { mutableStateOf("10.0") }
    var category by remember { mutableStateOf("ألحان سنوية") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة لحن جديد للفهرس", fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("اسم اللحن (مثال: طون سينا، هيتيني...) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxScoreStr,
                    onValueChange = {
                        maxScoreStr = it
                        errorMessage = null
                    },
                    label = { Text("الدرجة النهائية للحن *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("التصنيف (مثال: سنوي، أسبوع الآلام، كيهك)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxScore = maxScoreStr.toDoubleOrNull()
                    if (name.isBlank()) {
                        errorMessage = "يرجى كتابة اسم اللحن"
                    } else if (maxScore == null || maxScore <= 0) {
                        errorMessage = "يرجى إدخال درجة نهائية صحيحة أكبر من صفر"
                    } else {
                        onConfirm(name.trim(), maxScore, category.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ اللحن")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordHymnAssessmentDialog(
    members: List<Member>,
    hymns: List<Hymn>,
    existingAssessments: List<HymnAssessment> = emptyList(),
    defaultDate: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, String, Double, Double, String) -> Unit
) {
    var selectedMemberId by remember { mutableStateOf<Long?>(members.firstOrNull()?.id) }
    var selectedHymnId by remember { mutableStateOf<Long?>(hymns.firstOrNull()?.id) }
    var date by remember { mutableStateOf(defaultDate) }
    var scoreStr by remember { mutableStateOf("10.0") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var memberMenuExpanded by remember { mutableStateOf(false) }
    var hymnMenuExpanded by remember { mutableStateOf(false) }

    val selectedHymn = hymns.find { it.id == selectedHymnId }
    val maxScore = selectedHymn?.maxScore ?: 10.0
    val isAlreadyAssessed = existingAssessments.any { it.memberId == selectedMemberId && it.hymnId == selectedHymnId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل تسميع لحن", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Warning if already assessed
                if (isAlreadyAssessed) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "تم تسجيل تسميع لهذا اللحن لهذا المخدوم بالفعل! يرجى تعديل الدرجة من قائمة التسميعات لتفادي التكرار.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Member Selector
                ExposedDropdownMenuBox(
                    expanded = memberMenuExpanded,
                    onExpandedChange = { memberMenuExpanded = !memberMenuExpanded }
                ) {
                    val memberName = members.find { it.id == selectedMemberId }?.fullName ?: "اختر المخدوم"
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المخدوم *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = memberMenuExpanded,
                        onDismissRequest = { memberMenuExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.fullName) },
                                onClick = {
                                    selectedMemberId = m.id
                                    memberMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Hymn Selector
                ExposedDropdownMenuBox(
                    expanded = hymnMenuExpanded,
                    onExpandedChange = { hymnMenuExpanded = !hymnMenuExpanded }
                ) {
                    val hymnName = hymns.find { it.id == selectedHymnId }?.name ?: "اختر اللحن"
                    OutlinedTextField(
                        value = hymnName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اللحن *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hymnMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = hymnMenuExpanded,
                        onDismissRequest = { hymnMenuExpanded = false }
                    ) {
                        hymns.forEach { h ->
                            DropdownMenuItem(
                                text = { Text("${h.name} (${h.category}) - ${h.maxScore} درجة") },
                                onClick = {
                                    selectedHymnId = h.id
                                    scoreStr = "${h.maxScore}"
                                    hymnMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("التاريخ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = scoreStr,
                        onValueChange = {
                            scoreStr = it
                            errorMessage = null
                        },
                        label = { Text("الدرجة (من $maxScore)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الأداء والهزات") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val memberId = selectedMemberId
                    val hymnId = selectedHymnId
                    val score = scoreStr.toDoubleOrNull()
                    if (isAlreadyAssessed) {
                        errorMessage = "تم تسميع هذا اللحن لهذا المخدوم مسبقًا. يرجى تعديل التسميع السابق."
                    } else if (memberId == null) {
                        errorMessage = "يرجى اختيار المخدوم"
                    } else if (hymnId == null) {
                        errorMessage = "يرجى اختيار اللحن"
                    } else if (score == null || score < 0) {
                        errorMessage = "يرجى إدخال درجة صحيحة (صفر أو أكثر)"
                    } else if (score > maxScore) {
                        errorMessage = "الدرجة ($score) لا يمكن أن تتجاوز النهاية العظمى ($maxScore)"
                    } else {
                        onConfirm(memberId, hymnId, date.trim(), score, maxScore, notes.trim())
                    }
                },
                enabled = !isAlreadyAssessed,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ التسميع")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun EditHymnAssessmentDialog(
    assessment: HymnAssessment,
    hymnName: String,
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: (HymnAssessment) -> Unit
) {
    var scoreStr by remember { mutableStateOf("${assessment.score}") }
    var notes by remember { mutableStateOf(assessment.notes) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل درجة التسميع", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "المخدوم: $memberName", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "لحن: $hymnName (النهاية العظمى: ${assessment.maxScore})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = scoreStr,
                    onValueChange = {
                        scoreStr = it
                        errorMessage = null
                    },
                    label = { Text("الدرجة (من ${assessment.maxScore}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الأداء والهزات") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = scoreStr.toDoubleOrNull()
                    if (score == null || score < 0) {
                        errorMessage = "يرجى إدخال درجة صحيحة (صفر أو أكثر)"
                    } else if (score > assessment.maxScore) {
                        errorMessage = "الدرجة ($score) لا يمكن أن تتجاوز النهاية العظمى (${assessment.maxScore})"
                    } else {
                        onConfirm(assessment.copy(score = score, notes = notes.trim()))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("تحديث الدرجة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AddBibleLessonDialog(
    defaultDate: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, notes: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(defaultDate) }
    var maxScoreStr by remember { mutableStateOf("10.0") }
    var notes by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة درس / أصحاح جديد", fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("اسم الدرس / الأصحاح / الجزء *") },
                    placeholder = { Text("مثال: متى ٥ (الموعظة على الجبل)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("تاريخ الإضافة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxScoreStr,
                        onValueChange = {
                            maxScoreStr = it
                            errorMessage = null
                        },
                        label = { Text("الدرجة المقترحة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات المنهج والآيات المقررة") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "يرجى إدخال اسم الدرس / الأصحاح"
                    } else {
                        val finalNotes = if (notes.isNotBlank() && maxScoreStr.isNotBlank() && maxScoreStr != "10.0") {
                            "${notes.trim()} (درجة: $maxScoreStr)"
                        } else {
                            notes.trim()
                        }
                        onConfirm(name.trim(), finalNotes, isActive)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("إضافة الدرس")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun EditBibleAssessmentDialog(
    assessment: BibleAssessment,
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: (BibleAssessment) -> Unit
) {
    var scoreStr by remember { mutableStateOf(assessment.score.toString()) }
    var maxScoreStr by remember { mutableStateOf(assessment.maxScore.toString()) }
    var notes by remember { mutableStateOf(assessment.notes) }
    var date by remember { mutableStateOf(assessment.date) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل تقييم الإنجيل", fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "المخدوم: $memberName", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "الدرس: ${assessment.lessonName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scoreStr,
                        onValueChange = {
                            scoreStr = it
                            errorMessage = null
                        },
                        label = { Text("الدرجة المحصلة *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxScoreStr,
                        onValueChange = {
                            maxScoreStr = it
                            errorMessage = null
                        },
                        label = { Text("الدرجة النهائية *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("التاريخ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الحفظ والتفسير") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = scoreStr.toDoubleOrNull()
                    val maxScore = maxScoreStr.toDoubleOrNull() ?: assessment.maxScore
                    if (score == null || score < 0) {
                        errorMessage = "يرجى إدخال درجة صحيحة (صفر أو أكثر)"
                    } else if (score > maxScore) {
                        errorMessage = "الدرجة ($score) لا يمكن أن تتجاوز النهاية العظمى ($maxScore)"
                    } else {
                        onConfirm(
                            assessment.copy(
                                score = score,
                                maxScore = maxScore,
                                date = date.trim(),
                                notes = notes.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("تحديث الدرجة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordBibleAssessmentDialog(
    members: List<Member>,
    lessons: List<BibleLesson> = emptyList(),
    defaultDate: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, Double, Double, String) -> Unit
) {
    var selectedMemberId by remember { mutableStateOf<Long?>(members.firstOrNull()?.id) }
    var date by remember { mutableStateOf(defaultDate) }
    var lessonName by remember { mutableStateOf(lessons.firstOrNull { it.isActive }?.name ?: "") }
    var scoreStr by remember { mutableStateOf("10.0") }
    var maxScoreStr by remember { mutableStateOf("10.0") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var memberMenuExpanded by remember { mutableStateOf(false) }
    var lessonMenuExpanded by remember { mutableStateOf(false) }

    val activeLessons = remember(lessons) { lessons.filter { it.isActive } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل تقييم إنجيل", fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = memberMenuExpanded,
                    onExpandedChange = { memberMenuExpanded = !memberMenuExpanded }
                ) {
                    val memberName = members.find { it.id == selectedMemberId }?.fullName ?: "اختر المخدوم"
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المخدوم *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = memberMenuExpanded,
                        onDismissRequest = { memberMenuExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.fullName) },
                                onClick = {
                                    selectedMemberId = m.id
                                    memberMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (activeLessons.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = lessonMenuExpanded,
                        onExpandedChange = { lessonMenuExpanded = !lessonMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = lessonName,
                            onValueChange = {
                                lessonName = it
                                errorMessage = null
                            },
                            label = { Text("اختر من الفهرس أو اكتب درساً *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lessonMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = lessonMenuExpanded,
                            onDismissRequest = { lessonMenuExpanded = false }
                        ) {
                            activeLessons.forEach { l ->
                                DropdownMenuItem(
                                    text = { Text(l.name) },
                                    onClick = {
                                        lessonName = l.name
                                        lessonMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = lessonName,
                        onValueChange = {
                            lessonName = it
                            errorMessage = null
                        },
                        label = { Text("اسم الجزء / الأصحاح / الدرس *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scoreStr,
                        onValueChange = {
                            scoreStr = it
                            errorMessage = null
                        },
                        label = { Text("الدرجة المحصلة *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxScoreStr,
                        onValueChange = {
                            maxScoreStr = it
                            errorMessage = null
                        },
                        label = { Text("الدرجة النهائية *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("التاريخ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الحفظ والتفسير") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val memberId = selectedMemberId
                    val score = scoreStr.toDoubleOrNull()
                    val maxScore = maxScoreStr.toDoubleOrNull() ?: 10.0
                    if (memberId == null) {
                        errorMessage = "يرجى اختيار المخدوم"
                    } else if (lessonName.isBlank()) {
                        errorMessage = "يرجى كتابة اسم الدرس أو الأصحاح"
                    } else if (score == null || score < 0) {
                        errorMessage = "يرجى إدخال درجة صحيحة (صفر أو أكثر)"
                    } else if (score > maxScore) {
                        errorMessage = "الدرجة ($score) لا يمكن أن تتجاوز النهاية ($maxScore)"
                    } else {
                        onConfirm(memberId, date.trim(), lessonName.trim(), score, maxScore, notes.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ التقييم")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun AddExamDialog(
    defaultDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(defaultDate) }
    var maxScoreStr by remember { mutableStateOf("100.0") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء امتحان جديد", fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("اسم الامتحان (مثال: امتحان شهر سبتمبر) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("تاريخ الامتحان") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxScoreStr,
                        onValueChange = {
                            maxScoreStr = it
                            errorMessage = null
                        },
                        label = { Text("الدرجة النهائية *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات ومقرر الامتحان") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxScore = maxScoreStr.toDoubleOrNull()
                    if (name.isBlank()) {
                        errorMessage = "يرجى كتابة اسم الامتحان"
                    } else if (maxScore == null || maxScore <= 0) {
                        errorMessage = "يرجى إدخال درجة نهائية صحيحة أكبر من صفر"
                    } else {
                        onConfirm(name.trim(), date.trim(), maxScore, notes.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("إنشاء الامتحان")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordExamResultDialog(
    members: List<Member>,
    exams: List<Exam>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Double, String) -> Unit
) {
    var selectedMemberId by remember { mutableStateOf<Long?>(members.firstOrNull()?.id) }
    var selectedExamId by remember { mutableStateOf<Long?>(exams.firstOrNull()?.id) }
    var scoreStr by remember { mutableStateOf("90.0") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var memberMenuExpanded by remember { mutableStateOf(false) }
    var examMenuExpanded by remember { mutableStateOf(false) }

    val exam = exams.find { it.id == selectedExamId }
    val maxScore = exam?.maxScore ?: 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل نتيجة مخدوم في امتحان", fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = examMenuExpanded,
                    onExpandedChange = { examMenuExpanded = !examMenuExpanded }
                ) {
                    val examName = exams.find { it.id == selectedExamId }?.name ?: "اختر الامتحان"
                    OutlinedTextField(
                        value = examName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الامتحان *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = examMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = examMenuExpanded,
                        onDismissRequest = { examMenuExpanded = false }
                    ) {
                        exams.forEach { ex ->
                            DropdownMenuItem(
                                text = { Text("${ex.name} (نهاية ${ex.maxScore})") },
                                onClick = {
                                    selectedExamId = ex.id
                                    scoreStr = "${ex.maxScore}"
                                    examMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = memberMenuExpanded,
                    onExpandedChange = { memberMenuExpanded = !memberMenuExpanded }
                ) {
                    val memberName = members.find { it.id == selectedMemberId }?.fullName ?: "اختر المخدوم"
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المخدوم *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = memberMenuExpanded,
                        onDismissRequest = { memberMenuExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.fullName) },
                                onClick = {
                                    selectedMemberId = m.id
                                    memberMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = scoreStr,
                    onValueChange = {
                        scoreStr = it
                        errorMessage = null
                    },
                    label = { Text("درجة الامتحان (من $maxScore) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات النتيجة والتقدير") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AbsentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val memberId = selectedMemberId
                    val examId = selectedExamId
                    val score = scoreStr.toDoubleOrNull()
                    if (examId == null) {
                        errorMessage = "يرجى اختيار الامتحان"
                    } else if (memberId == null) {
                        errorMessage = "يرجى اختيار المخدوم"
                    } else if (score == null || score < 0) {
                        errorMessage = "يرجى إدخال درجة صحيحة (صفر أو أكثر)"
                    } else if (score > maxScore) {
                        errorMessage = "الدرجة ($score) لا يمكن أن تتجاوز النهاية ($maxScore)"
                    } else {
                        onConfirm(examId, memberId, score, notes.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ النتيجة")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
