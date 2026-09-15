package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BibleLesson
import com.example.data.model.Exam
import com.example.data.model.Hymn
import com.example.data.model.Member
import com.example.data.model.SchoolClass
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.ui.viewmodel.DeaconsViewModel

@Composable
fun MasterDataManagementDialog(
    viewModel: DeaconsViewModel,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = listOf("الألحان المقررة", "دروس وأجزاء الإنجيل", "الامتحانات الدورية")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = BurgundyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إدارة البيانات الأساسية",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == index) BurgundyPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> HymnsMasterTab(viewModel = viewModel)
                        1 -> BibleLessonsMasterTab(viewModel = viewModel)
                        2 -> ExamsMasterTab(viewModel = viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تم / إغلاق", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// 1. HYMNS MASTER MANAGEMENT
// =========================================================================

@Composable
fun HymnsMasterTab(viewModel: DeaconsViewModel) {
    val hymns by viewModel.allHymnsMaster.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingHymn by remember { mutableStateOf<Hymn?>(null) }
    var deletingHymn by remember { mutableStateOf<Hymn?>(null) }

    val filteredHymns = remember(hymns, searchQuery) {
        if (searchQuery.isBlank()) hymns
        else hymns.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true) ||
                    it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("بحث في الألحان المقررة...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ إضافة لحن", fontWeight = FontWeight.Bold)
        }

        if (filteredHymns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    message = if (searchQuery.isNotBlank()) "لا توجد ألحان تطابق البحث" else "لا توجد ألحان مضافة",
                    subMessage = "اضغط على (+ إضافة لحن) لإدراج ألحان المنهج",
                    icon = Icons.Default.MusicNote
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredHymns, key = { it.id }) { hymn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = hymn.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Status Badge
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (hymn.isActive) PresentGreen.copy(alpha = 0.15f) else AbsentRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (hymn.isActive) "نشط" else "غير نشط",
                                            color = if (hymn.isActive) PresentGreen else AbsentRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "التصنيف: ${hymn.category.ifBlank { "عام" }} | الدرجة: ${hymn.maxScore.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hymn.notes.isNotBlank()) {
                                    Text(
                                        text = hymn.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldSecondary
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { editingHymn = hymn },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { deletingHymn = hymn },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AbsentRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        HymnFormDialog(
            title = "إضافة لحن جديد",
            initialName = "",
            initialCategory = "عام",
            initialMaxScore = 10.0,
            initialNotes = "",
            initialIsActive = true,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, category, maxScore, notes, isActive ->
                viewModel.addHymn(
                    Hymn(
                        name = name.trim(),
                        category = category.trim().ifBlank { "عام" },
                        maxScore = maxScore,
                        notes = notes.trim(),
                        isActive = isActive
                    )
                )
                showAddDialog = false
            }
        )
    }

    if (editingHymn != null) {
        val h = editingHymn!!
        HymnFormDialog(
            title = "تعديل بيانات اللحن",
            initialName = h.name,
            initialCategory = h.category,
            initialMaxScore = h.maxScore,
            initialNotes = h.notes,
            initialIsActive = h.isActive,
            onDismiss = { editingHymn = null },
            onConfirm = { name, category, maxScore, notes, isActive ->
                viewModel.updateHymn(
                    h.copy(
                        name = name.trim(),
                        category = category.trim(),
                        maxScore = maxScore,
                        notes = notes.trim(),
                        isActive = isActive
                    )
                )
                editingHymn = null
            }
        )
    }

    if (deletingHymn != null) {
        val h = deletingHymn!!
        AlertDialog(
            onDismissRequest = { deletingHymn = null },
            title = { Text("حذف اللحن", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف لحن '${h.name}'؟\nفي حال وجود تقييمات سابقة للمخدومين، سيتم تعطيل اللحن للحفاظ على درجاتهم التاريخية.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHymn(h)
                        deletingHymn = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingHymn = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun HymnFormDialog(
    title: String,
    initialName: String = "",
    initialCategory: String = "عام",
    initialMaxScore: Double = 10.0,
    initialNotes: String = "",
    initialIsActive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, maxScore: Double, notes: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(initialCategory) }
    var maxScoreStr by remember { mutableStateOf(initialMaxScore.toString()) }
    var notes by remember { mutableStateOf(initialNotes) }
    var isActive by remember { mutableStateOf(initialIsActive) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
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
                    label = { Text("اسم اللحن *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("التصنيف / المناسبة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxScoreStr,
                        onValueChange = { maxScoreStr = it },
                        label = { Text("الدرجة العظمى") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات اختيارية") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isActive) "الحالة: نشط بالمنهج" else "الحالة: غير نشط (مؤرشف)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) PresentGreen else AbsentRed
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = AbsentRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = maxScoreStr.toDoubleOrNull() ?: 10.0
                    if (name.isBlank()) {
                        errorMessage = "يرجى كتابة اسم اللحن"
                    } else {
                        onConfirm(name.trim(), category.trim(), score, notes.trim(), isActive)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

// =========================================================================
// 2. BIBLE LESSONS MASTER MANAGEMENT
// =========================================================================

@Composable
fun BibleLessonsMasterTab(viewModel: DeaconsViewModel) {
    val lessons by viewModel.bibleLessons.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLesson by remember { mutableStateOf<BibleLesson?>(null) }
    var deletingLesson by remember { mutableStateOf<BibleLesson?>(null) }

    val filteredLessons = remember(lessons, searchQuery) {
        if (searchQuery.isBlank()) lessons
        else lessons.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("بحث في أجزاء ودروس الإنجيل...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ إضافة درس", fontWeight = FontWeight.Bold)
        }

        if (filteredLessons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    message = if (searchQuery.isNotBlank()) "لا توجد دروس تطابق البحث" else "لا توجد أجزاء إنجيل مسجلة",
                    subMessage = "اضغط على (+ إضافة درس) لإدراج أصحاحات ودروس الكتاب المقدس المقررة",
                    icon = Icons.Default.MenuBook
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLessons, key = { it.id }) { lesson ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = lesson.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (lesson.isActive) PresentGreen.copy(alpha = 0.15f) else AbsentRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (lesson.isActive) "نشط" else "غير نشط",
                                            color = if (lesson.isActive) PresentGreen else AbsentRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (lesson.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lesson.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { editingLesson = lesson },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { deletingLesson = lesson },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AbsentRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        BibleLessonFormDialog(
            title = "إضافة جزء / درس إنجيل جديد",
            initialName = "",
            initialNotes = "",
            initialIsActive = true,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, notes, isActive ->
                viewModel.addBibleLesson(name, notes, isActive)
                showAddDialog = false
            }
        )
    }

    if (editingLesson != null) {
        val l = editingLesson!!
        BibleLessonFormDialog(
            title = "تعديل درس الإنجيل",
            initialName = l.name,
            initialNotes = l.notes,
            initialIsActive = l.isActive,
            onDismiss = { editingLesson = null },
            onConfirm = { name, notes, isActive ->
                viewModel.updateBibleLesson(
                    l.copy(
                        name = name.trim(),
                        notes = notes.trim(),
                        isActive = isActive
                    )
                )
                editingLesson = null
            }
        )
    }

    if (deletingLesson != null) {
        val l = deletingLesson!!
        AlertDialog(
            onDismissRequest = { deletingLesson = null },
            title = { Text("حذف درس الإنجيل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف درس '${l.name}'؟\nإذا كانت هناك تقييمات سابقة مرتبطة بهذا الدرس، سيتم أرشفته للحفاظ على سجلات الشمامسة.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBibleLesson(l)
                        deletingLesson = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingLesson = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun BibleLessonFormDialog(
    title: String,
    initialName: String = "",
    initialNotes: String = "",
    initialIsActive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (name: String, notes: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var notes by remember { mutableStateOf(initialNotes) }
    var isActive by remember { mutableStateOf(initialIsActive) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("اسم الدرس / الجزء / الأصحاح *") },
                    placeholder = { Text("مثال: متى ٥ (الموعظة على الجبل)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات اختيارية (الآيات المطلوبة...)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isActive) "الحالة: نشط بالمنهج" else "الحالة: غير نشط (مؤرشف)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) PresentGreen else AbsentRed
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = AbsentRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "يرجى كتابة اسم الدرس أو الأصحاح"
                    } else {
                        onConfirm(name.trim(), notes.trim(), isActive)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

// =========================================================================
// 3. EXAMS MASTER MANAGEMENT
// =========================================================================

@Composable
fun ExamsMasterTab(viewModel: DeaconsViewModel) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<Exam?>(null) }
    var deletingExam by remember { mutableStateOf<Exam?>(null) }

    val filteredExams = remember(exams, searchQuery) {
        if (searchQuery.isBlank()) exams
        else exams.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.notes.contains(searchQuery, ignoreCase = true) ||
                    it.date.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("بحث في الامتحانات المقررة...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ إضافة امتحان", fontWeight = FontWeight.Bold)
        }

        if (filteredExams.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    message = if (searchQuery.isNotBlank()) "لا توجد امتحانات تطابق البحث" else "لا توجد امتحانات مضافة",
                    subMessage = "اضغط على (+ إضافة امتحان) لإدراج امتحانات التيرم أو الشهر",
                    icon = Icons.Default.Quiz
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExams, key = { it.id }) { exam ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = exam.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (exam.isActive) PresentGreen.copy(alpha = 0.15f) else AbsentRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (exam.isActive) "نشط" else "غير نشط",
                                            color = if (exam.isActive) PresentGreen else AbsentRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "التاريخ: ${exam.date.ifBlank { "غير محدد" }} | النهاية العظمى: ${exam.maxScore.toInt()} درجة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (exam.notes.isNotBlank()) {
                                    Text(
                                        text = exam.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldSecondary
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { editingExam = exam },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { deletingExam = exam },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AbsentRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ExamFormDialog(
            title = "إضافة امتحان دوري جديد",
            initialName = "",
            initialDate = viewModel.todayDate,
            initialMaxScore = 100.0,
            initialNotes = "",
            initialIsActive = true,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, date, maxScore, notes, isActive ->
                viewModel.addExam(name, date, maxScore, notes, isActive)
                showAddDialog = false
            }
        )
    }

    if (editingExam != null) {
        val ex = editingExam!!
        ExamFormDialog(
            title = "تعديل الامتحان",
            initialName = ex.name,
            initialDate = ex.date,
            initialMaxScore = ex.maxScore,
            initialNotes = ex.notes,
            initialIsActive = ex.isActive,
            onDismiss = { editingExam = null },
            onConfirm = { name, date, maxScore, notes, isActive ->
                viewModel.updateExam(
                    ex.copy(
                        name = name.trim(),
                        date = date.trim(),
                        maxScore = maxScore,
                        notes = notes.trim(),
                        isActive = isActive
                    )
                )
                editingExam = null
            }
        )
    }

    if (deletingExam != null) {
        val ex = deletingExam!!
        AlertDialog(
            onDismissRequest = { deletingExam = null },
            title = { Text("حذف الامتحان", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف امتحان '${ex.name}'؟\nفي حال وجود نتائج سابقة مسجلة للمخدومين، سيتم تعطيل الامتحان للحفاظ على سجلاتهم.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExam(ex)
                        deletingExam = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExam = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ExamFormDialog(
    title: String,
    initialName: String = "",
    initialDate: String = "",
    initialMaxScore: Double = 100.0,
    initialNotes: String = "",
    initialIsActive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (name: String, date: String, maxScore: Double, notes: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var date by remember { mutableStateOf(initialDate) }
    var maxScoreStr by remember { mutableStateOf(initialMaxScore.toString()) }
    var notes by remember { mutableStateOf(initialNotes) }
    var isActive by remember { mutableStateOf(initialIsActive) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = BurgundyPrimary) },
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
                    label = { Text("اسم الامتحان *") },
                    placeholder = { Text("مثال: امتحان تيرم أول ألحان وطقس") },
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
                        onValueChange = { maxScoreStr = it },
                        label = { Text("الدرجة العظمى") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات اختيارية") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isActive) "الحالة: نشط" else "الحالة: غير نشط (مؤرشف)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) PresentGreen else AbsentRed
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = AbsentRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = maxScoreStr.toDoubleOrNull() ?: 100.0
                    if (name.isBlank()) {
                        errorMessage = "يرجى كتابة اسم الامتحان"
                    } else {
                        onConfirm(name.trim(), date.trim(), score, notes.trim(), isActive)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

// =========================================================================
// 4. SCHOOL CLASSES MANAGEMENT DIALOG
// =========================================================================

@Composable
fun SchoolClassesManagementDialog(
    viewModel: DeaconsViewModel,
    onDismiss: () -> Unit
) {
    val schoolClasses by viewModel.schoolClasses.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<SchoolClass?>(null) }
    var deletingClass by remember { mutableStateOf<SchoolClass?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = BurgundyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إدارة الفصول الدراسية", fontWeight = FontWeight.Bold, color = BurgundyPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الفصول الدراسية المعتمدة (${schoolClasses.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة فصل", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (schoolClasses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateView(
                            message = "لا توجد فصول دراسية مضافة",
                            subMessage = "اضغط على (إضافة فصل) لإدراج فصول ومراحل مدرسة الشمامسة",
                            icon = Icons.Default.School
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(schoolClasses, key = { it.id }) { sc ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = BurgundyPrimary.copy(alpha = 0.1f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${sc.orderIndex}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BurgundyPrimary
                                                )
                                            }
                                        }
                                        Text(
                                            text = sc.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { editingClass = sc },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { deletingClass = sc },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AbsentRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)) {
                Text("إغلاق")
            }
        }
    )

    if (showAddDialog) {
        var className by remember { mutableStateOf("") }
        var orderStr by remember { mutableStateOf("${schoolClasses.size + 1}") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة فصل دراسي جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = {
                            className = it
                            error = null
                        },
                        label = { Text("اسم الفصل الدراسي *") },
                        placeholder = { Text("مثال: أولى ابتدائي، ثانية إعدادي...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = orderStr,
                        onValueChange = { orderStr = it },
                        label = { Text("رقم الترتيب") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (error != null) {
                        Text(error!!, color = AbsentRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (className.isBlank()) {
                            error = "يرجى كتابة اسم الفصل"
                        } else {
                            val order = orderStr.toIntOrNull() ?: 0
                            viewModel.addSchoolClass(className.trim(), order)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (editingClass != null) {
        val sc = editingClass!!
        var className by remember { mutableStateOf(sc.name) }
        var orderStr by remember { mutableStateOf("${sc.orderIndex}") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingClass = null },
            title = { Text("تعديل الفصل الدراسي", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = {
                            className = it
                            error = null
                        },
                        label = { Text("اسم الفصل الدراسي *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = orderStr,
                        onValueChange = { orderStr = it },
                        label = { Text("رقم الترتيب") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (error != null) {
                        Text(error!!, color = AbsentRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (className.isBlank()) {
                            error = "يرجى كتابة اسم الفصل"
                        } else {
                            val order = orderStr.toIntOrNull() ?: sc.orderIndex
                            viewModel.updateSchoolClass(sc.copy(name = className.trim(), orderIndex = order))
                            editingClass = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingClass = null }) { Text("إلغاء") }
            }
        )
    }

    if (deletingClass != null) {
        val sc = deletingClass!!
        AlertDialog(
            onDismissRequest = { deletingClass = null },
            title = { Text("حذف الفصل الدراسي", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف فصل '${sc.name}'؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSchoolClass(sc)
                        deletingClass = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingClass = null }) { Text("إلغاء") }
            }
        )
    }
}

// =========================================================================
// 5. MISSING DATA AUDIT DIALOG
// =========================================================================

@Composable
fun MissingDataAuditDialog(
    members: List<Member>,
    onSelectMember: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val missingPhone = remember(members) { members.filter { it.phone.isBlank() } }
    val missingParentPhone = remember(members) { members.filter { it.parentPhone.isBlank() } }
    val missingSchoolClass = remember(members) { members.filter { it.schoolClass.isBlank() } }
    val missingArea = remember(members) { members.filter { it.area.isBlank() || it.area == "غير محدد" } }
    val missingBirthDate = remember(members) { members.filter { it.birthDate.isBlank() } }

    var selectedCategory by remember { mutableIntStateOf(0) }
    val categories = listOf(
        "هاتف المخدوم (${missingPhone.size})" to missingPhone,
        "هاتف ولي الأمر (${missingParentPhone.size})" to missingParentPhone,
        "الفصل الدراسي (${missingSchoolClass.size})" to missingSchoolClass,
        "المنطقة / العنوان (${missingArea.size})" to missingArea,
        "تاريخ الميلاد (${missingBirthDate.size})" to missingBirthDate
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = GoldSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فحص واكتمال البيانات الناقصة", fontWeight = FontWeight.Bold, color = BurgundyPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                Text(
                    text = "حدد الحقل لمراجعة المخدومين الذين تنقصهم هذه البيانات واستكمالها مباشرة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal scrollable categories chips
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories.size) { idx ->
                        val isSelected = selectedCategory == idx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BurgundyPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedCategory = idx }
                        ) {
                            Text(
                                text = categories[idx].first,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val currentList = categories[selectedCategory].second

                if (currentList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PresentGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ممتاز! جميع المخدومين مسجل لهم هذا الحقل.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PresentGreen
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(currentList, key = { it.id }) { m ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.clickable {
                                    onDismiss()
                                    onSelectMember(m.id)
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = m.fullName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "الفصل: ${m.schoolClass.ifBlank { "غير مسجل" }} | المنطقة: ${m.area.ifBlank { "غير مسجلة" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onSelectMember(m.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary.copy(alpha = 0.12f)),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("استكمال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BurgundyPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)) {
                Text("إغلاق")
            }
        }
    )
}

// =========================================================================
// 7. MISSING ACADEMIC & ASSESSMENT DATA AUDIT DIALOG
// =========================================================================

data class MissingAcademicItem(
    val member: Member,
    val itemType: String, // "لحن" or "إنجيل" or "امتحان"
    val itemName: String,
    val itemId: Long,
    val maxScore: Double,
    val category: String
)

@Composable
fun MissingAcademicDataAuditDialog(
    viewModel: DeaconsViewModel,
    onSelectMember: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val schoolClasses by viewModel.schoolClasses.collectAsStateWithLifecycle()
    val hymns by viewModel.hymns.collectAsStateWithLifecycle()
    val hymnAssessments by viewModel.hymnAssessments.collectAsStateWithLifecycle()
    val bibleLessons by viewModel.bibleLessons.collectAsStateWithLifecycle()
    val bibleAssessments by viewModel.bibleAssessments.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val examResults by viewModel.examResults.collectAsStateWithLifecycle()

    var selectedClassFilter by remember { mutableStateOf<String?>(null) }
    var selectedCategoryTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val activeHymns = remember(hymns) { hymns.filter { it.isActive } }
    val activeBibleLessons = remember(bibleLessons) { bibleLessons.filter { it.isActive } }
    val activeExams = remember(exams) { exams.filter { it.isActive } }

    val filteredMembers = remember(members, selectedClassFilter, searchQuery) {
        members.filter { m ->
            (selectedClassFilter == null || m.schoolClass == selectedClassFilter) &&
                    (searchQuery.isBlank() || m.fullName.contains(searchQuery, ignoreCase = true))
        }
    }

    // Compute missing hymns
    val missingHymnsList = remember(filteredMembers, activeHymns, hymnAssessments) {
        val list = mutableListOf<MissingAcademicItem>()
        for (m in filteredMembers) {
            val assessedHymnIds = hymnAssessments.filter { it.memberId == m.id }.map { it.hymnId }.toSet()
            for (h in activeHymns) {
                if (h.id !in assessedHymnIds) {
                    list.add(
                        MissingAcademicItem(
                            member = m,
                            itemType = "لحن",
                            itemName = h.name,
                            itemId = h.id,
                            maxScore = h.maxScore,
                            category = h.category
                        )
                    )
                }
            }
        }
        list
    }

    // Compute missing Bible lessons
    val missingBibleList = remember(filteredMembers, activeBibleLessons, bibleAssessments) {
        val list = mutableListOf<MissingAcademicItem>()
        for (m in filteredMembers) {
            val assessedLessonNames = bibleAssessments.filter { it.memberId == m.id }.map { it.lessonName.trim() }.toSet()
            for (bl in activeBibleLessons) {
                if (bl.name.trim() !in assessedLessonNames) {
                    list.add(
                        MissingAcademicItem(
                            member = m,
                            itemType = "إنجيل",
                            itemName = bl.name,
                            itemId = bl.id,
                            maxScore = 10.0,
                            category = "الكتاب المقدس"
                        )
                    )
                }
            }
        }
        list
    }

    // Compute missing Exams
    val missingExamsList = remember(filteredMembers, activeExams, examResults) {
        val list = mutableListOf<MissingAcademicItem>()
        for (m in filteredMembers) {
            val takenExamIds = examResults.filter { it.memberId == m.id }.map { it.examId }.toSet()
            for (ex in activeExams) {
                if (ex.id !in takenExamIds) {
                    list.add(
                        MissingAcademicItem(
                            member = m,
                            itemType = "امتحان",
                            itemName = ex.name,
                            itemId = ex.id,
                            maxScore = ex.maxScore,
                            category = "امتحان دوري"
                        )
                    )
                }
            }
        }
        list
    }

    // Aggregate by Member for Summary View
    val memberSummaryList = remember(filteredMembers, missingHymnsList, missingBibleList, missingExamsList) {
        filteredMembers.map { m ->
            val hCount = missingHymnsList.count { it.member.id == m.id }
            val bCount = missingBibleList.count { it.member.id == m.id }
            val eCount = missingExamsList.count { it.member.id == m.id }
            Triple(m, hCount + bCount + eCount, "ألحان: $hCount | إنجيل: $bCount | امتحانات: $eCount")
        }.filter { it.second > 0 }.sortedByDescending { it.second }
    }

    val tabs = listOf(
        "ملخص المخدومين (${memberSummaryList.size})",
        "ألحان متبقية (${missingHymnsList.size})",
        "إنجيل متبقي (${missingBibleList.size})",
        "امتحانات متبقية (${missingExamsList.size})"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = GoldSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شاشة النواقص الأكاديمية والتقييمات",
                        fontWeight = FontWeight.Bold,
                        color = BurgundyPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            ) {
                Text(
                    text = "حصر فوري لجميع التسميعات والامتحانات غير المسجلة للمخدومين لمتابعتهم واستكمال الدرجات:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم الشماس...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Class filter chips
                if (schoolClasses.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedClassFilter == null) BurgundyPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedClassFilter = null }
                            ) {
                                Text(
                                    text = "جميع الفصول",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedClassFilter == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedClassFilter == null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        items(schoolClasses) { sc ->
                            val isSelected = selectedClassFilter == sc.name
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BurgundyPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedClassFilter = sc.name }
                            ) {
                                Text(
                                    text = sc.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                TabRow(
                    selectedTabIndex = selectedCategoryTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedCategoryTab == idx,
                            onClick = { selectedCategoryTab = idx },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedCategoryTab == idx) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategoryTab == idx) BurgundyPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (selectedCategoryTab) {
                    0 -> {
                        // Member Summary
                        if (memberSummaryList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyStateView(
                                    message = "ممتاز! جميع المخدومين استكملوا تقييماتهم",
                                    subMessage = "لا توجد نواقص أكاديمية مسجلة حسب الفلاتر المحددة",
                                    icon = Icons.Default.CheckCircle
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(memberSummaryList, key = { it.first.id }) { (m, totalMissing, details) ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = m.fullName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "فصل: ${m.schoolClass.ifBlank { "غير مسجل" }}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = details,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = AbsentRed,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = AbsentRed.copy(alpha = 0.12f),
                                                modifier = Modifier.clickable {
                                                    onDismiss()
                                                    onSelectMember(m.id)
                                                }
                                            ) {
                                                Text(
                                                    text = "$totalMissing متأخر",
                                                    color = AbsentRed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Missing Hymns
                        MissingItemList(
                            items = missingHymnsList,
                            emptyMessage = "رائع! لا توجد ألحان غير مسمعة",
                            onSelectMember = {
                                onDismiss()
                                onSelectMember(it)
                            }
                        )
                    }
                    2 -> {
                        // Missing Bible
                        MissingItemList(
                            items = missingBibleList,
                            emptyMessage = "رائع! جميع دروس الإنجيل تم تقييمها",
                            onSelectMember = {
                                onDismiss()
                                onSelectMember(it)
                            }
                        )
                    }
                    3 -> {
                        // Missing Exams
                        MissingItemList(
                            items = missingExamsList,
                            emptyMessage = "رائع! جميع المخدومين قدموا امتحاناتهم",
                            onSelectMember = {
                                onDismiss()
                                onSelectMember(it)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun MissingItemList(
    items: List<MissingAcademicItem>,
    emptyMessage: String,
    onSelectMember: (Long) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PresentGreen, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PresentGreen,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items, key = { "${it.member.id}_${it.itemType}_${it.itemId}_${it.itemName}" }) { item ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.member.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "فصل: ${item.member.schoolClass.ifBlank { "غير مسجل" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "المطلوب: ${item.itemType} '${item.itemName}' (${item.category})",
                                style = MaterialTheme.typography.bodySmall,
                                color = AbsentRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { onSelectMember(item.member.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("تسجيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyFollowUpDialog(
    viewModel: DeaconsViewModel,
    onSelectMember: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val attendances by viewModel.attendances.collectAsStateWithLifecycle()
    val visitRecords by viewModel.allVisitRecords.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Find members with consecutive absences (e.g. absent in 2 or more recent Fridays)
    val membersWithConsecutiveAbsence = remember(members, attendances) {
        val fridayAttendances = attendances.filter { it.type == com.example.data.model.AttendanceType.SERVICE }
            .sortedByDescending { it.date }
        val recentDates = fridayAttendances.map { it.date }.distinct().take(4)

        members.mapNotNull { member ->
            val memberAttendances = fridayAttendances.filter { it.memberId == member.id && it.date in recentDates }
            val absencesCount = memberAttendances.count { it.status == com.example.data.model.AttendanceStatus.ABSENT }
            if (absencesCount >= 2) {
                Pair(member, absencesCount)
            } else null
        }.sortedByDescending { it.second }
    }

    // Members not visited in 30+ days
    val membersNeedingVisit = remember(members, visitRecords) {
        members.filter { m ->
            val visits = visitRecords.filter { it.memberId == m.id }
            visits.isEmpty()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val followUpTabs = listOf(
        "غياب متكرر (${membersWithConsecutiveAbsence.size})",
        "بدون افتقاد (${membersNeedingVisit.size})"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = BurgundyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("المتابعة الشهرية والغياب المتكرر", fontWeight = FontWeight.Bold, color = BurgundyPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    followUpTabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == idx) BurgundyPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    if (membersWithConsecutiveAbsence.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PresentGreen, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "رائع! لا يوجد مخدومون متكررو الغياب بالأسابيع الأخيرة.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PresentGreen,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(membersWithConsecutiveAbsence, key = { it.first.id }) { (m, count) ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = m.fullName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "فصل: ${m.schoolClass.ifBlank { "غير مسجل" }} | غياب $count مرات حديثاً",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AbsentRed,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (m.phone.isNotBlank() || m.parentPhone.isNotBlank()) {
                                                Text(
                                                    text = "هاتف: ${if (m.phone.isNotBlank()) m.phone else m.parentPhone}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (m.phone.isNotBlank() || m.parentPhone.isNotBlank()) {
                                                val ph = if (m.phone.isNotBlank()) m.phone else m.parentPhone
                                                IconButton(
                                                    onClick = {
                                                        try {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                                data = android.net.Uri.parse("tel:$ph")
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (_: Exception) {}
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Call, contentDescription = "اتصال", tint = PresentGreen)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    onDismiss()
                                                    onSelectMember(m.id)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("الملف والافتقاد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (membersNeedingVisit.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PresentGreen, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "تبارك الله! جميع المخدومين تم تسجيل افتقادات لهم.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PresentGreen,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(membersNeedingVisit, key = { it.id }) { m ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = m.fullName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "فصل: ${m.schoolClass.ifBlank { "غير مسجل" }} | المنطقة: ${m.area.ifBlank { "غير مسجلة" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onSelectMember(m.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("تسجيل افتقاد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)) {
                Text("إغلاق")
            }
        }
    )
}

