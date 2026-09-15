package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.DeaconsDatabase
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.data.model.BibleAssessment
import com.example.data.model.BibleLesson
import com.example.data.model.EvaluationPeriod
import com.example.data.model.Exam
import com.example.data.model.ExamResult
import com.example.data.model.Group
import com.example.data.model.Hymn
import com.example.data.model.HymnAssessment
import com.example.data.model.Member
import com.example.data.model.MemberBarcode
import com.example.data.model.SchoolClass
import com.example.data.model.Servant
import com.example.data.model.VisitRecord
import com.example.data.repository.DeaconsRepository
import com.example.util.BarcodeGenerator
import com.example.util.ExcelExportConfig
import com.example.util.ExcelExporter
import com.example.util.ExcelImporter
import com.example.util.MemberOverdueInfo
import com.example.util.NotificationHelper
import com.example.util.OverdueCalculator
import com.example.util.OverdueGraceSettings
import com.example.workers.OverdueWorkScheduler
import com.example.data.remote.AppUser
import com.example.data.remote.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MemberSortOrder {
    NAME_ASC,
    ATTENDANCE_DESC,
    ATTENDANCE_ASC,
    CLASS_AND_NAME
}

data class ClassSummaryStats(
    val className: String,
    val stage: String, // "ابتدائي", "إعدادي", "ثانوي", "عام"
    val totalMembers: Int,
    val todayPresent: Int,
    val todayAbsent: Int,
    val todayExcused: Int,
    val attendanceRate: Int, // 0 - 100%
    val hasRecordsToday: Boolean,
    val overdueFollowUpCount: Int,
    val averageHymnScore: Double?,
    val monthlyMassCount: Int
)

data class MemberMissingRecord(
    val member: Member,
    val missingHymns: List<Hymn>,
    val missingBible: List<BibleLesson>,
    val missingExams: List<Exam>
)

data class BackupMergePreview(
    val backupMembersCount: Int,
    val newMembersCount: Int,
    val existingMembersCount: Int,
    val attendancesCount: Int,
    val hymnsCount: Int,
    val assessmentsCount: Int,
    val examsCount: Int,
    val backupDate: String
)

class DeaconsViewModel(
    application: Application,
    private val repository: DeaconsRepository,
    private val authManager: AuthManager
) : AndroidViewModel(application) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayDate: String = dateFormatter.format(Date())

    // ----------------------- AUTH / SESSION -----------------------
    val currentUser: StateFlow<AppUser?> = authManager.currentUser
    val isAdmin: StateFlow<Boolean> = currentUser.map { it?.isAdmin == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isServant: StateFlow<Boolean> = currentUser.map { it?.isServant == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isViewer: StateFlow<Boolean> = currentUser.map { it?.isViewer == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canRecordAttendance: StateFlow<Boolean> = currentUser.map { it?.isAdmin == true || it?.canRecordAttendance == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val canRecordAssessments: StateFlow<Boolean> = currentUser.map { it?.isAdmin == true || it?.canRecordAssessments == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val canManageMembers: StateFlow<Boolean> = currentUser.map { it?.isAdmin == true || it?.canManageMembers == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val canExportReports: StateFlow<Boolean> = currentUser.map { it?.isAdmin == true || it?.canExportReports == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val assignedClasses: StateFlow<List<String>> = currentUser.map { it?.effectiveAssignedClasses.orEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assignedClass: StateFlow<String> = assignedClasses.map { it.firstOrNull().orEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isRemoteEnabled: Boolean get() = repository.isRemoteEnabled

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _cloudTestResult = MutableStateFlow<String?>(null)
    val cloudTestResult: StateFlow<String?> = _cloudTestResult.asStateFlow()
    private val _isTestingCloud = MutableStateFlow(false)
    val isTestingCloud: StateFlow<Boolean> = _isTestingCloud.asStateFlow()

    private val _cloudUrl = MutableStateFlow(com.example.data.remote.SupabaseConfig.getProjectUrl())
    val cloudUrl: StateFlow<String> = _cloudUrl.asStateFlow()

    private val _cloudAnonKey = MutableStateFlow(com.example.data.remote.SupabaseConfig.getAnonKey())
    val cloudAnonKey: StateFlow<String> = _cloudAnonKey.asStateFlow()

    val isCloudConfigured: Boolean get() = com.example.data.remote.SupabaseConfig.isConfigured()

    fun saveCloudConfig(url: String, anonKey: String) {
        com.example.data.remote.SupabaseConfig.saveConfig(getApplication(), url, anonKey)
        _cloudUrl.value = com.example.data.remote.SupabaseConfig.getProjectUrl()
        _cloudAnonKey.value = com.example.data.remote.SupabaseConfig.getAnonKey()
        _cloudTestResult.value = null
        _syncMessage.value = "تم حفظ إعدادات السيرفر السحابي بنجاح"
    }

    fun testCloudConnection() {
        viewModelScope.launch {
            _isTestingCloud.value = true
            _cloudTestResult.value = null
            val res = repository.testCloudConnection()
            _cloudTestResult.value = res.getOrElse { it.localizedMessage ?: "تعذر الاتصال بالسيرفر" }
            _isTestingCloud.value = false
        }
    }

    fun clearCloudTestResult() {
        _cloudTestResult.value = null
    }

    fun syncFromRemote(forceReplace: Boolean = false, onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _syncing.value = true
            val r = repository.syncFromRemote(forceReplace)
            _syncMessage.value = if (r.isSuccess) r.getOrNull() ?: "تمت المزامنة بنجاح" else "فشل سحب البيانات: ${r.exceptionOrNull()?.message}"
            _syncing.value = false
            onDone?.invoke(r.isSuccess)
        }
    }

    fun pushToRemote(onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _syncing.value = true
            val r = repository.pushToRemote()
            _syncMessage.value = if (r.isSuccess) r.getOrNull() ?: "تم رفع البيانات بنجاح" else "فشل رفع البيانات: ${r.exceptionOrNull()?.message}"
            _syncing.value = false
            onDone?.invoke(r.isSuccess)
        }
    }

    fun smartSync(onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _syncing.value = true
            val r = repository.smartSync()
            _syncMessage.value = if (r.isSuccess) r.getOrNull() ?: "تمت المزامنة الذكية بنجاح" else "فشل المزامنة: ${r.exceptionOrNull()?.message}"
            _syncing.value = false
            onDone?.invoke(r.isSuccess)
        }
    }

    private val _isLiveSyncActive = MutableStateFlow(true)
    val isLiveSyncActive: StateFlow<Boolean> = _isLiveSyncActive.asStateFlow()

    fun triggerQuickSync() {
        if (!isCloudConfigured) return
        viewModelScope.launch {
            try {
                repository.smartSync()
            } catch (_: Exception) {}
        }
    }

    fun clearSyncMessage() { _syncMessage.value = null }

    // ----------------------- ADMIN (users) -----------------------
    private val _adminUsers = MutableStateFlow<List<AppUser>>(emptyList())
    val adminUsers: StateFlow<List<AppUser>> = _adminUsers.asStateFlow()
    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    fun loadAdminUsers() {
        viewModelScope.launch {
            _adminUsers.value = authManager.listUsers()
        }
    }
    fun setUserRole(id: String, role: String) {
        viewModelScope.launch {
            val ok = authManager.setUserRole(id, role)
            _adminMessage.value = if (ok) "تم تحديث الدور" else "فشل تحديث الدور"
            loadAdminUsers()
        }
    }
    fun setUserActive(id: String, active: Boolean) {
        viewModelScope.launch {
            val ok = authManager.setUserActive(id, active)
            _adminMessage.value = if (ok) (if (active) "تم تفعيل الحساب" else "تم تعطيل الحساب") else "فشل التحديث"
            loadAdminUsers()
        }
    }
    fun setUserFullName(id: String, name: String) {
        viewModelScope.launch { authManager.setUserFullName(id, name); loadAdminUsers() }
    }
    fun setUserPermissions(
        id: String,
        assignedClasses: List<String>,
        canAttendance: Boolean,
        canAssessments: Boolean,
        canMembers: Boolean,
        canReports: Boolean
    ) {
        viewModelScope.launch {
            authManager.setUserPermissions(id, assignedClasses, canAttendance, canAssessments, canMembers, canReports)
            _adminMessage.value = "تم حفظ صلاحيات المستخدم بنجاح"
            loadAdminUsers()
        }
    }
    fun addServantUser(
        fullName: String,
        email: String,
        role: String,
        assignedClasses: List<String>,
        canAttendance: Boolean,
        canAssessments: Boolean,
        canMembers: Boolean,
        canReports: Boolean
    ) {
        viewModelScope.launch {
            authManager.addServantUser(
                fullName, email, role, assignedClasses, canAttendance, canAssessments, canMembers, canReports
            )
            _adminMessage.value = "تم إضافة المستخدم وتحديد صلاحياته بنجاح"
            loadAdminUsers()
        }
    }
    fun deleteUser(id: String) {
        viewModelScope.launch {
            authManager.deleteUser(id)
            _adminMessage.value = "تم حذف المستخدم بنجاح"
            loadAdminUsers()
        }
    }
    fun clearAdminMessage() { _adminMessage.value = null }

    // ----------------------- AUTH ACTIONS -----------------------
    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()
    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    fun login(email: String, password: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _authLoading.value = true
            val r = authManager.signIn(email.trim(), password)
            _authMessage.value = r.fold(onSuccess = { null }, onFailure = { it.localizedMessage ?: "فشل تسجيل الدخول" })
            _authLoading.value = false
            onResult(r.isSuccess)
        }
    }
    fun signup(email: String, password: String, fullName: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _authLoading.value = true
            val r = authManager.signUp(email.trim(), password, fullName.trim())
            _authMessage.value = r.fold(
                onSuccess = { "تم إنشاء الحساب. في انتظار تفعيل الأدمن." },
                onFailure = { it.localizedMessage ?: "فشل إنشاء الحساب" }
            )
            _authLoading.value = false
            onResult(r.isSuccess)
        }
    }
    fun restoreSession(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val u = authManager.loadSession()
            onResult(u != null)
        }
    }
    fun logout() {
        authManager.signOut()
    }
    fun clearAuthMessage() { _authMessage.value = null }

    // Database Streams
    val members: StateFlow<List<Member>> = combine(repository.allMembers, currentUser) { all, user ->
        if (user?.isAdmin == true || user?.isViewer == true) all
        else {
            val allowed = user?.effectiveAssignedClasses.orEmpty().toSet()
            all.filter { it.schoolClass in allowed }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val groups: StateFlow<List<Group>> = repository.allGroups.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val servants: StateFlow<List<Servant>> = repository.allServants.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val attendances: StateFlow<List<Attendance>> = repository.allAttendances.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val hymns: StateFlow<List<Hymn>> = repository.allHymns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allHymnsMaster: StateFlow<List<Hymn>> = repository.allHymnsIncludingInactive.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val hymnAssessments: StateFlow<List<HymnAssessment>> = repository.allHymnAssessments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bibleLessons: StateFlow<List<BibleLesson>> = repository.allBibleLessons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeBibleLessons: StateFlow<List<BibleLesson>> = repository.activeBibleLessons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bibleAssessments: StateFlow<List<BibleAssessment>> = repository.allBibleAssessments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val exams: StateFlow<List<Exam>> = repository.allExams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeExams: StateFlow<List<Exam>> = repository.activeExams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val examResults: StateFlow<List<ExamResult>> = repository.allExamResults.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val periods: StateFlow<List<EvaluationPeriod>> = repository.allPeriods.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val schoolClasses: StateFlow<List<SchoolClass>> = repository.allSchoolClasses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allVisitRecords: StateFlow<List<VisitRecord>> = repository.allVisitRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allBarcodes: StateFlow<List<MemberBarcode>> = repository.allBarcodes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getBarcodeForMember(memberId: Long): kotlinx.coroutines.flow.Flow<MemberBarcode?> {
        return repository.getBarcodeForMember(memberId)
    }

    // App Settings: Service Attendance Target (Mass target per month: 1 - 8)
    private val _serviceAttendanceTarget = MutableStateFlow(4)
    val serviceAttendanceTarget: StateFlow<Int> = _serviceAttendanceTarget.asStateFlow()

    // Service Ministry Target (Service/Tasbeha target per month: 1 - 8)
    private val _serviceMinistryTarget = MutableStateFlow(4)
    val serviceMinistryTarget: StateFlow<Int> = _serviceMinistryTarget.asStateFlow()

    // Overdue Grace Period Settings
    private val _graceSettings = MutableStateFlow(OverdueGraceSettings.DEFAULT)
    val graceSettings: StateFlow<OverdueGraceSettings> = _graceSettings.asStateFlow()

    // Overdue Members Flow based on configured dynamic grace periods
    val overdueMembersInfo: StateFlow<List<MemberOverdueInfo>> = combine(
        combine(members, attendances, hymns) { m, att, h -> Triple(m, att, h) },
        combine(hymnAssessments, bibleLessons, bibleAssessments) { ha, bl, ba -> Triple(ha, bl, ba) },
        combine(exams, examResults, _graceSettings) { e, er, gs -> Triple(e, er, gs) }
    ) { (mList, attList, hList), (haList, blList, baList), (eList, erList, gSettings) ->
        OverdueCalculator.calculateOverdueForMembers(
            members = mList,
            attendances = attList,
            hymns = hList,
            hymnAssessments = haList,
            bibleLessons = blList,
            bibleAssessments = baList,
            exams = eList,
            examResults = erList,
            settings = gSettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Class-specific aggregated statistics for the Admin / Service Dashboard
    val classStatsList: StateFlow<List<ClassSummaryStats>> = combine(
        members,
        attendances,
        schoolClasses,
        hymnAssessments,
        overdueMembersInfo
    ) { mList, attList, scList, haList, overdueList ->
        val defaultClasses = listOf(
            "أولى ابتدائي", "ثانية ابتدائي", "ثالثة ابتدائي", "رابعة ابتدائي", "خامسة ابتدائي", "سادسة ابتدائي",
            "أولى إعدادي", "ثانية إعدادي", "ثالثة إعدادي",
            "أولى ثانوي", "ثانية ثانوي", "ثالثة ثانوي"
        )
        val allClasses = (scList.map { it.name } + defaultClasses + mList.map { it.schoolClass })
            .filter { it.isNotBlank() }
            .distinct()

        val overdueMap = overdueList.filter { it.hasAnyOverdue }.groupBy { it.member.schoolClass }
        val serviceAttToday = attList.filter { it.date == todayDate && it.type == AttendanceType.SERVICE }
        val massAttThisMonth = attList.filter { it.type == AttendanceType.MASS }

        allClasses.map { className ->
            val classMembers = mList.filter { it.schoolClass == className && it.isActive }
            val memberIds = classMembers.map { it.id }.toSet()
            val classAttToday = serviceAttToday.filter { it.memberId in memberIds }
            val presentToday = classAttToday.count { it.status == AttendanceStatus.PRESENT }
            val absentToday = classAttToday.count { it.status == AttendanceStatus.ABSENT }
            val excusedToday = classAttToday.count { it.status == AttendanceStatus.EXCUSED }
            val hasRecords = classAttToday.isNotEmpty()

            val attRate = if (hasRecords) {
                if (classAttToday.isNotEmpty()) (presentToday * 100) / classAttToday.size else 0
            } else {
                val recentClassAtt = attList.filter { it.memberId in memberIds && it.type == AttendanceType.SERVICE }
                if (recentClassAtt.isNotEmpty()) {
                    (recentClassAtt.count { it.status == AttendanceStatus.PRESENT } * 100) / recentClassAtt.size
                } else 0
            }

            val classHymns = haList.filter { it.memberId in memberIds }
            val avgScore = if (classHymns.isNotEmpty()) {
                classHymns.map { it.score }.average()
            } else null

            val massCount = massAttThisMonth.count { it.memberId in memberIds && it.status == AttendanceStatus.PRESENT }
            val overdueCount = overdueMap[className]?.size ?: 0

            val stage = when {
                className.contains("ابتدائي") -> "ابتدائي"
                className.contains("إعدادي") -> "إعدادي"
                className.contains("ثانوي") -> "ثانوي"
                else -> "عام"
            }

            ClassSummaryStats(
                className = className,
                stage = stage,
                totalMembers = classMembers.size,
                todayPresent = presentToday,
                todayAbsent = absentToday,
                todayExcused = excusedToday,
                attendanceRate = attRate,
                hasRecordsToday = hasRecords,
                overdueFollowUpCount = overdueCount,
                averageHymnScore = avgScore?.let { (it * 10).toInt() / 10.0 },
                monthlyMassCount = massCount
            )
        }.sortedWith(compareBy<ClassSummaryStats> {
            when (it.stage) {
                "ابتدائي" -> 1
                "إعدادي" -> 2
                "ثانوي" -> 3
                else -> 4
            }
        }.thenBy { it.className })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.checkAndSeedIfEmpty()
            _serviceAttendanceTarget.value = repository.getServiceAttendanceTarget()
            _serviceMinistryTarget.value = repository.getServiceMinistryAttendanceTarget()
            val loadedGrace = repository.getOverdueGraceSettings()
            _graceSettings.value = loadedGrace
            if (loadedGrace.notificationsEnabled) {
                OverdueWorkScheduler.schedulePeriodicOverdueCheck(getApplication())
            }
        }

        // Automatic cloud sync while the app is active.
        // Uses the authenticated user's RLS-filtered server snapshot and the offline write queue.
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            if (isCloudConfigured) {
                try {
                    repository.smartSync()
                } catch (_: Exception) {}
            }
            while (isActive) {
                kotlinx.coroutines.delay(20000)
                if (isCloudConfigured && _isLiveSyncActive.value) {
                    try {
                        repository.smartSync()
                    } catch (_: Exception) {
                        // Silent in background - does not interrupt user interaction
                    }
                }
            }
        }
    }

    fun setServiceAttendanceTarget(target: Int) {
        val clamped = target.coerceIn(1, 10)
        _serviceAttendanceTarget.value = clamped
        viewModelScope.launch {
            repository.setServiceAttendanceTarget(clamped)
        }
    }

    fun setServiceMinistryTarget(target: Int) {
        val clamped = target.coerceIn(1, 10)
        _serviceMinistryTarget.value = clamped
        viewModelScope.launch {
            repository.setServiceMinistryAttendanceTarget(clamped)
        }
    }

    fun updateGraceSettings(
        attendanceDays: Int = _graceSettings.value.attendanceAbsenceDays,
        hymnDays: Int = _graceSettings.value.hymnOverdueDays,
        bibleDays: Int = _graceSettings.value.bibleOverdueDays,
        examDays: Int = _graceSettings.value.examOverdueDays,
        notificationsEnabled: Boolean = _graceSettings.value.notificationsEnabled
    ) {
        val updated = OverdueGraceSettings(
            attendanceAbsenceDays = attendanceDays.coerceIn(1, 180),
            hymnOverdueDays = hymnDays.coerceIn(1, 365),
            bibleOverdueDays = bibleDays.coerceIn(1, 365),
            examOverdueDays = examDays.coerceIn(1, 180),
            notificationsEnabled = notificationsEnabled
        )
        _graceSettings.value = updated
        viewModelScope.launch {
            repository.saveOverdueGraceSettings(updated)
            if (updated.notificationsEnabled) {
                OverdueWorkScheduler.schedulePeriodicOverdueCheck(getApplication())
            } else {
                OverdueWorkScheduler.cancelPeriodicCheck(getApplication())
            }
        }
    }

    fun triggerManualOverdueCheck(context: Context, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            OverdueWorkScheduler.triggerImmediateCheck(context)
            val currentOverdue = overdueMembersInfo.value.filter { it.hasAnyOverdue }
            if (_graceSettings.value.notificationsEnabled && currentOverdue.isNotEmpty()) {
                NotificationHelper.showSummaryOverdueNotification(context, currentOverdue)
            }
            onComplete?.invoke(currentOverdue.size)
        }
    }

    // Search and Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    private val _selectedSchoolClass = MutableStateFlow<String?>(null)
    val selectedSchoolClass: StateFlow<String?> = _selectedSchoolClass.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<Boolean?>(null) // null: All, true: Active, false: Inactive
    val selectedStatusFilter: StateFlow<Boolean?> = _selectedStatusFilter.asStateFlow()

    private val _memberSortOrder = MutableStateFlow(MemberSortOrder.NAME_ASC)
    val memberSortOrder: StateFlow<MemberSortOrder> = _memberSortOrder.asStateFlow()

    fun setMemberSortOrder(order: MemberSortOrder) {
        _memberSortOrder.value = order
    }

    val filteredMembers: StateFlow<List<Member>> = combine(
        combine(members, searchQuery, selectedGroupId) { m, q, g -> Triple(m, q, g) },
        combine(selectedSchoolClass, selectedStatusFilter, groups) { s, st, gr -> Triple(s, st, gr) },
        combine(_memberSortOrder, attendances) { sort, att -> Pair(sort, att) }
    ) { (memberList, query, groupId), (sClass, statusFilter, groupList), (sortOrder, attList) ->
        val groupMap = groupList.associateBy { it.id }
        val filtered = memberList.filter { member ->
            val matchesGroup = groupId == null || member.groupId == groupId
            val matchesClass = sClass.isNullOrBlank() || member.schoolClass == sClass
            val matchesStatus = statusFilter == null || member.isActive == statusFilter
            val groupName = member.groupId?.let { groupMap[it]?.name } ?: ""
            val matchesQuery = query.isBlank() ||
                    member.fullName.contains(query, ignoreCase = true) ||
                    member.schoolClass.contains(query, ignoreCase = true) ||
                    member.phone.contains(query) ||
                    member.parentPhone.contains(query) ||
                    member.street.contains(query, ignoreCase = true) ||
                    member.area.contains(query, ignoreCase = true) ||
                    groupName.contains(query, ignoreCase = true)
            matchesGroup && matchesClass && matchesStatus && matchesQuery
        }

        when (sortOrder) {
            MemberSortOrder.NAME_ASC -> filtered.sortedBy { it.fullName }
            MemberSortOrder.CLASS_AND_NAME -> filtered.sortedWith(compareBy({ it.schoolClass }, { it.fullName }))
            MemberSortOrder.ATTENDANCE_DESC, MemberSortOrder.ATTENDANCE_ASC -> {
                val memberAttendanceCounts = attList.filter { it.type == AttendanceType.SERVICE }
                    .groupBy { it.memberId }
                    .mapValues { entry ->
                        val total = entry.value.size
                        val present = entry.value.count { it.status == AttendanceStatus.PRESENT }
                        if (total > 0) (present * 100.0) / total else 0.0
                    }
                if (sortOrder == MemberSortOrder.ATTENDANCE_DESC) {
                    filtered.sortedByDescending { memberAttendanceCounts[it.id] ?: 0.0 }
                } else {
                    filtered.sortedBy { memberAttendanceCounts[it.id] ?: 0.0 }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Missing Assessments & Overdue Summary
    val missingAssessmentsSummary: StateFlow<List<MemberMissingRecord>> = combine(
        combine(members, hymns, hymnAssessments) { m, h, ha -> Triple(m, h, ha) },
        combine(bibleLessons, bibleAssessments) { b, ba -> Pair(b, ba) },
        combine(exams, examResults) { e, er -> Pair(e, er) }
    ) { (memberList, hymnList, hymnAttList), (bibleList, bibleAttList), (examList, examResList) ->
        val activeHymns = hymnList.filter { it.isActive }
        val activeBibles = bibleList.filter { it.isActive }
        val activeExamList = examList.filter { it.isActive }

        val hymnAssessedMap = hymnAttList.groupBy { it.memberId }.mapValues { it.value.map { a -> a.hymnId }.toSet() }
        val bibleAssessedMap = bibleAttList.groupBy { it.memberId }.mapValues { it.value.mapNotNull { a -> a.lessonId }.toSet() }
        val examResultMap = examResList.groupBy { it.memberId }.mapValues { it.value.map { a -> a.examId }.toSet() }

        memberList.mapNotNull { member ->
            val assessedHymnIds = hymnAssessedMap[member.id] ?: emptySet()
            val assessedBibleIds = bibleAssessedMap[member.id] ?: emptySet()
            val takenExamIds = examResultMap[member.id] ?: emptySet()

            val missingH = activeHymns.filter { it.id !in assessedHymnIds }
            val missingB = activeBibles.filter { it.id !in assessedBibleIds }
            val missingE = activeExamList.filter { it.id !in takenExamIds }

            if (missingH.isNotEmpty() || missingB.isNotEmpty() || missingE.isNotEmpty()) {
                MemberMissingRecord(
                    member = member,
                    missingHymns = missingH,
                    missingBible = missingB,
                    missingExams = missingE
                )
            } else {
                null
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Profile Detail Selection
    private val _selectedMemberId = MutableStateFlow<Long?>(null)
    val selectedMemberId: StateFlow<Long?> = _selectedMemberId.asStateFlow()

    // Attendance selection screen state
    private val _selectedAttendanceDate = MutableStateFlow(todayDate)
    val selectedAttendanceDate: StateFlow<String> = _selectedAttendanceDate.asStateFlow()

    private val _selectedAttendanceType = MutableStateFlow(AttendanceType.SERVICE)
    val selectedAttendanceType: StateFlow<AttendanceType> = _selectedAttendanceType.asStateFlow()

    private val _selectedMassNumber = MutableStateFlow(1) // 1 or 2
    val selectedMassNumber: StateFlow<Int> = _selectedMassNumber.asStateFlow()

    // Selected Evaluation Report Period: 0: شهر, 1: 4 أشهر, 2: نصف سنة (6 أشهر), 3: سنة (12 شهر), 4: مخصص
    private val _selectedPeriodType = MutableStateFlow(ExcelExporter.PERIOD_MONTH)
    val selectedPeriodType: StateFlow<Int> = _selectedPeriodType.asStateFlow()

    private val _customStartDate = MutableStateFlow("2026-09-01")
    val customStartDate: StateFlow<String> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow("2026-09-30")
    val customEndDate: StateFlow<String> = _customEndDate.asStateFlow()

    private val _reportSelectedMemberId = MutableStateFlow<Long?>(null)
    val reportSelectedMemberId: StateFlow<Long?> = _reportSelectedMemberId.asStateFlow()

    private val _reportSelectedGroupId = MutableStateFlow<Long?>(null)
    val reportSelectedGroupId: StateFlow<Long?> = _reportSelectedGroupId.asStateFlow()

    private val _reportSelectedClass = MutableStateFlow<String?>(null)
    val reportSelectedClass: StateFlow<String?> = _reportSelectedClass.asStateFlow()

    // Multi-Class Selection for Reports & Excel Export
    private val _reportSelectedClasses = MutableStateFlow<Set<String>>(emptySet())
    val reportSelectedClasses: StateFlow<Set<String>> = _reportSelectedClasses.asStateFlow()

    private val _isRefreshingReport = MutableStateFlow(false)
    val isRefreshingReport: StateFlow<Boolean> = _isRefreshingReport.asStateFlow()

    // Hymn Assessment Error & Notice State
    private val _hymnAssessmentError = MutableStateFlow<String?>(null)
    val hymnAssessmentError: StateFlow<String?> = _hymnAssessmentError.asStateFlow()

    // Full Excel Export Configuration State
    private val _exportConfig = MutableStateFlow(ExcelExportConfig.DEFAULT)
    val exportConfig: StateFlow<ExcelExportConfig> = _exportConfig.asStateFlow()

    // Excel Import States
    private val _importPreview = MutableStateFlow<ExcelImporter.ImportPreviewData?>(null)
    val importPreview: StateFlow<ExcelImporter.ImportPreviewData?> = _importPreview.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importResult = MutableStateFlow<ExcelImporter.ImportResult?>(null)
    val importResult: StateFlow<ExcelImporter.ImportResult?> = _importResult.asStateFlow()

    private val _importErrorMessage = MutableStateFlow<String?>(null)
    val importErrorMessage: StateFlow<String?> = _importErrorMessage.asStateFlow()

    // Excel Export State
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _lastExportedFile = MutableStateFlow<File?>(null)
    val lastExportedFile: StateFlow<File?> = _lastExportedFile.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGroupId(groupId: Long?) {
        _selectedGroupId.value = groupId
    }

    fun setSelectedSchoolClass(schoolClass: String?) {
        _selectedSchoolClass.value = schoolClass
    }

    fun setSelectedStatusFilter(isActive: Boolean?) {
        _selectedStatusFilter.value = isActive
    }

    fun setSelectedMemberId(id: Long?) {
        _selectedMemberId.value = id
    }

    fun setSelectedAttendanceDate(date: String) {
        _selectedAttendanceDate.value = date
    }

    fun setSelectedAttendanceType(type: AttendanceType) {
        _selectedAttendanceType.value = type
    }

    fun setSelectedMassNumber(number: Int) {
        _selectedMassNumber.value = number
    }

    fun setSelectedPeriodType(typeIndex: Int) {
        _selectedPeriodType.value = typeIndex
    }

    fun setCustomStartDate(date: String) {
        _customStartDate.value = date
    }

    fun setCustomEndDate(date: String) {
        _customEndDate.value = date
    }

    fun setReportSelectedMemberId(id: Long?) {
        _reportSelectedMemberId.value = id
    }

    fun setReportSelectedGroupId(id: Long?) {
        _reportSelectedGroupId.value = id
        _exportConfig.value = _exportConfig.value.copy(groupId = id)
    }

    fun setReportSelectedClass(className: String?) {
        _reportSelectedClass.value = className
        _exportConfig.value = _exportConfig.value.copy(classFilter = className)
    }

    fun toggleReportSelectedClass(className: String) {
        val current = _reportSelectedClasses.value.toMutableSet()
        if (current.contains(className)) {
            current.remove(className)
        } else {
            current.add(className)
        }
        _reportSelectedClasses.value = current
        _exportConfig.value = _exportConfig.value.copy(classFilters = current)
    }

    fun selectAllReportClasses(classes: List<String>) {
        val all = classes.toSet()
        _reportSelectedClasses.value = all
        _exportConfig.value = _exportConfig.value.copy(classFilters = all)
    }

    fun deselectAllReportClasses() {
        _reportSelectedClasses.value = emptySet()
        _exportConfig.value = _exportConfig.value.copy(classFilters = emptySet())
    }

    fun setReportSelectedClasses(classes: Set<String>) {
        _reportSelectedClasses.value = classes
        _exportConfig.value = _exportConfig.value.copy(classFilters = classes)
    }

    fun refreshReportData() {
        viewModelScope.launch {
            _isRefreshingReport.value = true
            try {
                kotlinx.coroutines.delay(350)
            } finally {
                _isRefreshingReport.value = false
            }
        }
    }

    fun setExportConfig(config: ExcelExportConfig) {
        _exportConfig.value = config
        _reportSelectedMemberId.value = config.memberId
        _reportSelectedGroupId.value = config.groupId
        _reportSelectedClass.value = config.classFilter
        _reportSelectedClasses.value = config.classFilters
        _selectedPeriodType.value = config.periodType
        _customStartDate.value = config.customStartDate
        _customEndDate.value = config.customEndDate
    }

    // Member Actions
    fun addMember(member: Member) {
        viewModelScope.launch {
            repository.insertMember(member)
        }
    }

    fun updateMember(member: Member) {
        viewModelScope.launch {
            repository.updateMember(member.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun toggleMemberStatus(member: Member) {
        viewModelScope.launch {
            repository.updateMember(member.copy(isActive = !member.isActive, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            repository.deleteMember(member)
            if (_selectedMemberId.value == member.id) {
                _selectedMemberId.value = null
            }
        }
    }

    // Attendance Actions
    fun recordAttendanceDirect(
        memberId: Long,
        date: String,
        type: AttendanceType,
        status: AttendanceStatus,
        massNumber: Int? = null,
        notes: String = "",
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.recordAttendance(
                memberId = memberId,
                date = date,
                type = type,
                status = status,
                massNumber = massNumber,
                notes = notes
            )
            onSuccess?.invoke()
        }
    }

    fun recordMemberAttendance(
        memberId: Long,
        status: AttendanceStatus,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val type = _selectedAttendanceType.value
            val date = _selectedAttendanceDate.value
            val massNumber = if (type == AttendanceType.MASS) _selectedMassNumber.value else null
            repository.recordAttendance(
                memberId = memberId,
                date = date,
                type = type,
                status = status,
                massNumber = massNumber,
                notes = notes
            )
        }
    }

    fun markAllMembersPresent(targetMembers: List<Member>) {
        viewModelScope.launch {
            val type = _selectedAttendanceType.value
            val date = _selectedAttendanceDate.value
            val massNumber = if (type == AttendanceType.MASS) _selectedMassNumber.value else null
            targetMembers.forEach { member ->
                repository.recordAttendance(
                    memberId = member.id,
                    date = date,
                    type = type,
                    status = AttendanceStatus.PRESENT,
                    massNumber = massNumber
                )
            }
        }
    }

    fun removeAttendanceForMember(memberId: Long) {
        viewModelScope.launch {
            val type = _selectedAttendanceType.value
            val date = _selectedAttendanceDate.value
            val massNumber = if (type == AttendanceType.MASS) _selectedMassNumber.value else null
            repository.deleteAttendanceForMemberAndDate(memberId, date, type, massNumber)
        }
    }

    fun updateAttendance(attendance: Attendance) {
        viewModelScope.launch {
            repository.updateAttendance(attendance)
        }
    }

    fun deleteAttendance(attendance: Attendance) {
        viewModelScope.launch {
            repository.deleteAttendance(attendance)
        }
    }

    /**
     * Process scanned barcode for attendance marking.
     * Returns the matched Member or null if barcode is not found.
     */
    fun recordAttendanceByBarcode(
        scannedCode: String,
        status: AttendanceStatus = AttendanceStatus.PRESENT,
        notes: String = "",
        onResult: (Boolean, Member?, String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanedCode = scannedCode.trim()
            val barcodeEntity = repository.getBarcodeByValue(cleanedCode)
            val memberId = if (barcodeEntity != null) {
                barcodeEntity.memberId
            } else {
                BarcodeGenerator.parseMemberId(cleanedCode)
            }

            if (memberId == null) {
                onResult(false, null, "لم يتم العثور على باركود مطابق: $cleanedCode")
                return@launch
            }

            val member = repository.getMemberByIdDirect(memberId)
            if (member == null) {
                onResult(false, null, "لم يتم العثور على المخدوم صاحب الباركود رقم: $memberId")
                return@launch
            }

            val type = _selectedAttendanceType.value
            val date = _selectedAttendanceDate.value
            val massNumber = if (type == AttendanceType.MASS) _selectedMassNumber.value else null

            repository.recordAttendance(
                memberId = member.id,
                date = date,
                type = type,
                status = status,
                massNumber = massNumber,
                notes = notes
            )

            onResult(true, member, "تم تسجيل حضور الشماس ${member.fullName} بنجاح")
        }
    }

    // School Class Actions
    fun addSchoolClass(name: String, orderIndex: Int = 0) {
        viewModelScope.launch {
            repository.insertSchoolClass(SchoolClass(name = name.trim(), orderIndex = orderIndex))
        }
    }

    fun updateSchoolClass(schoolClass: SchoolClass) {
        viewModelScope.launch {
            repository.updateSchoolClass(schoolClass)
        }
    }

    fun deleteSchoolClass(schoolClass: SchoolClass) {
        viewModelScope.launch {
            repository.deleteSchoolClass(schoolClass)
        }
    }

    // Hymn Master Actions
    fun addHymn(
        name: String,
        maxScore: Double = 10.0,
        category: String = "عام",
        notes: String = "",
        isActive: Boolean = true,
        durationInDays: Int = 14,
        deadline: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.insertHymn(
                Hymn(
                    name = name.trim(),
                    category = category.trim().ifBlank { "عام" },
                    maxScore = maxScore,
                    notes = notes.trim(),
                    isActive = isActive,
                    durationInDays = durationInDays,
                    deadline = deadline.trim()
                )
            )
            onComplete?.invoke()
        }
    }

    fun addHymn(hymn: Hymn, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.insertHymn(hymn)
            onComplete?.invoke()
        }
    }

    fun updateHymn(hymn: Hymn, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateHymn(hymn)
            onComplete?.invoke()
        }
    }

    fun deleteHymn(hymn: Hymn, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteHymn(hymn)
            onComplete?.invoke()
        }
    }

    // Bible Lesson Master Actions
    fun addBibleLesson(
        name: String,
        notes: String = "",
        isActive: Boolean = true,
        durationInDays: Int = 7,
        deadline: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.insertBibleLesson(
                BibleLesson(
                    name = name.trim(),
                    notes = notes.trim(),
                    isActive = isActive,
                    durationInDays = durationInDays,
                    deadline = deadline.trim()
                )
            )
            onComplete?.invoke()
        }
    }

    fun updateBibleLesson(lesson: BibleLesson, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateBibleLesson(lesson)
            onComplete?.invoke()
        }
    }

    fun deleteBibleLesson(lesson: BibleLesson, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteBibleLesson(lesson)
            onComplete?.invoke()
        }
    }

    // Hymn Assessment Actions
    fun clearHymnAssessmentError() {
        _hymnAssessmentError.value = null
    }

    fun recordHymnAssessment(
        memberId: Long,
        hymnId: Long,
        date: String,
        score: Double,
        maxScore: Double,
        notes: String = "",
        onSuccess: (() -> Unit)? = null,
        onAlreadyAssessed: ((HymnAssessment) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (score < 0 || score > maxScore) {
                _hymnAssessmentError.value = "الدرجة يجب أن تكون بين 0 و $maxScore"
                return@launch
            }

            val existing = repository.getAssessmentForMemberAndHymn(memberId, hymnId)
            if (existing != null) {
                _hymnAssessmentError.value = "تم تسميع هذا اللحن لهذا المخدوم بالفعل (الدرجة: ${existing.score}/$maxScore). يمكنك تعديل الدرجة من سجل التسميعات."
                onAlreadyAssessed?.invoke(existing)
                return@launch
            }

            try {
                repository.insertHymnAssessment(
                    HymnAssessment(
                        memberId = memberId,
                        hymnId = hymnId,
                        date = date,
                        score = score,
                        maxScore = maxScore,
                        notes = notes
                    )
                )
                _hymnAssessmentError.value = null
                onSuccess?.invoke()
            } catch (e: Exception) {
                _hymnAssessmentError.value = "تعذر تسجيل التسميع: ${e.localizedMessage}"
            }
        }
    }

    fun updateHymnAssessment(
        assessment: HymnAssessment,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (assessment.score < 0 || assessment.score > assessment.maxScore) {
                _hymnAssessmentError.value = "الدرجة يجب أن تكون بين 0 و ${assessment.maxScore}"
                return@launch
            }
            try {
                repository.updateHymnAssessment(assessment)
                _hymnAssessmentError.value = null
                onSuccess?.invoke()
            } catch (e: Exception) {
                _hymnAssessmentError.value = "تعذر تعديل التسميع: ${e.localizedMessage}"
            }
        }
    }

    fun deleteHymnAssessment(
        assessment: HymnAssessment,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.deleteHymnAssessment(assessment)
            onSuccess?.invoke()
        }
    }

    // Bible Assessment Actions
    fun recordBibleAssessment(
        memberId: Long,
        date: String,
        lessonName: String,
        score: Double,
        maxScore: Double = 10.0,
        notes: String = "",
        lessonId: Long? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.insertBibleAssessment(
                BibleAssessment(
                    memberId = memberId,
                    date = date,
                    lessonName = lessonName,
                    score = score,
                    maxScore = maxScore,
                    notes = notes,
                    lessonId = lessonId
                )
            )
            onSuccess?.invoke()
        }
    }

    fun updateBibleAssessment(assessment: BibleAssessment, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateBibleAssessment(assessment)
            onSuccess?.invoke()
        }
    }

    fun deleteBibleAssessment(assessment: BibleAssessment, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteBibleAssessment(assessment)
            onSuccess?.invoke()
        }
    }

    // Exam Master Actions
    fun addExam(
        name: String,
        date: String = "",
        maxScore: Double = 100.0,
        notes: String = "",
        isActive: Boolean = true,
        durationInDays: Int = 30,
        deadline: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.insertExam(
                Exam(
                    name = name.trim(),
                    date = date.trim(),
                    maxScore = maxScore,
                    notes = notes.trim(),
                    isActive = isActive,
                    durationInDays = durationInDays,
                    deadline = deadline.trim()
                )
            )
            onComplete?.invoke()
        }
    }

    fun addExam(exam: Exam, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.insertExam(exam)
            onComplete?.invoke()
        }
    }

    fun updateExam(exam: Exam, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateExam(exam)
            onComplete?.invoke()
        }
    }

    fun deleteExam(exam: Exam, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteExam(exam)
            onComplete?.invoke()
        }
    }

    // Exam Result Actions
    fun recordExamResult(
        examId: Long,
        memberId: Long,
        score: Double,
        notes: String
    ) {
        recordExamResult(examId = examId, memberId = memberId, score = score, maxScore = 100.0, notes = notes)
    }

    fun recordExamResult(
        examId: Long,
        memberId: Long,
        score: Double,
        maxScore: Double = 100.0,
        date: String = "",
        notes: String = "",
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.insertExamResult(
                ExamResult(
                    examId = examId,
                    memberId = memberId,
                    score = score,
                    maxScore = maxScore,
                    date = date,
                    notes = notes
                )
            )
            onSuccess?.invoke()
        }
    }

    fun updateExamResult(result: ExamResult, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateExamResult(result)
            onSuccess?.invoke()
        }
    }

    fun deleteExamResult(result: ExamResult, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteExamResult(result)
            onSuccess?.invoke()
        }
    }

    // Group Actions
    fun addGroup(name: String, description: String = "") {
        viewModelScope.launch {
            repository.insertGroup(
                Group(
                    name = name,
                    description = description
                )
            )
        }
    }

    fun updateGroup(group: Group) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }

    fun deleteGroup(group: Group) {
        viewModelScope.launch {
            repository.deleteGroup(group)
        }
    }

    // Servant Actions
    fun addServant(name: String, phone: String, role: String) {
        viewModelScope.launch {
            repository.insertServant(
                Servant(
                    name = name,
                    phone = phone,
                    role = role
                )
            )
        }
    }

    fun updateServant(servant: Servant) {
        viewModelScope.launch {
            repository.updateServant(servant)
        }
    }

    fun deleteServant(servant: Servant) {
        viewModelScope.launch {
            repository.deleteServant(servant)
        }
    }

    // SchoolClass Actions
    fun addSchoolClass(name: String, orderIndex: Int = 0, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.insertSchoolClass(
                SchoolClass(
                    name = name.trim(),
                    orderIndex = orderIndex,
                    isActive = true
                )
            )
            onComplete?.invoke()
        }
    }

    fun updateSchoolClass(schoolClass: SchoolClass, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateSchoolClass(schoolClass)
            onComplete?.invoke()
        }
    }

    fun deleteSchoolClass(schoolClass: SchoolClass, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteSchoolClass(schoolClass)
            onComplete?.invoke()
        }
    }

    fun clearAllMembersDatabase() {
        viewModelScope.launch {
            repository.clearAllMembers()
        }
    }

    // -------------------------------------------------------------
    // Visit Record Operations
    // -------------------------------------------------------------

    fun addVisitRecord(
        memberId: Long,
        visitDate: String,
        notes: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.insertVisitRecord(
                VisitRecord(
                    memberId = memberId,
                    visitDate = visitDate.trim(),
                    notes = notes.trim()
                )
            )
            onComplete?.invoke()
        }
    }

    fun updateVisitRecord(record: VisitRecord, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateVisitRecord(record)
            onComplete?.invoke()
        }
    }

    fun deleteVisitRecord(record: VisitRecord, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteVisitRecord(record)
            onComplete?.invoke()
        }
    }

    fun getVisitsForMember(memberId: Long): kotlinx.coroutines.flow.Flow<List<VisitRecord>> =
        repository.getVisitsForMember(memberId)

    fun exportVisitsReport(
        context: Context,
        classFilter: String? = null,
        onComplete: ((File) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val file = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val freshMembers = repository.getAllMembersIncludingInactiveDirect()
                    val freshVisits = repository.getAllVisitRecordsDirect()
                    ExcelExporter.exportVisitsToExcel(
                        context = context,
                        members = freshMembers,
                        visits = freshVisits,
                        classFilter = classFilter
                    )
                }
                _lastExportedFile.value = file
                _exportMessage.value = "تم استخراج كشف الافتقاد بنجاح (${file.name})"
                onComplete?.invoke(file)
            } catch (e: Exception) {
                _exportMessage.value = "خطأ أثناء استخراج كشف الافتقاد: ${e.localizedMessage}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    // -------------------------------------------------------------
    // Excel Import Operations
    // -------------------------------------------------------------

    fun parseExcelForImport(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _importErrorMessage.value = null
            try {
                val preview = ExcelImporter.parseExcelForPreview(context, fileUri)
                _importPreview.value = preview
            } catch (e: Exception) {
                _importErrorMessage.value = "خطأ أثناء قراءة ملف Excel: ${e.localizedMessage ?: "تنسيق غير صالح"}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun confirmImportMembers(ignoreInvalidRows: Boolean = true) {
        val preview = _importPreview.value ?: return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = repository.importMembers(preview.previewRows, ignoreInvalidRows)
                _importResult.value = result
                _importPreview.value = null
            } catch (e: Exception) {
                _importErrorMessage.value = "فشل في حفظ البيانات المستوردة: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun dismissImportPreview() {
        _importPreview.value = null
        _importErrorMessage.value = null
    }

    fun dismissImportResult() {
        _importResult.value = null
    }

    fun downloadExcelTemplate(context: Context) {
        viewModelScope.launch {
            try {
                val templateFile = ExcelExporter.generateExcelTemplate(context)
                ExcelExporter.shareExcelFile(context, templateFile, "نموذج استيراد بيانات مدرسة الشمامسة")
            } catch (e: Exception) {
                _exportMessage.value = "تعذر إنشاء النموذج: ${e.localizedMessage}"
            }
        }
    }

    // -------------------------------------------------------------
    // Excel Export Operations
    // -------------------------------------------------------------

    fun exportFullReport(
        context: Context,
        customConfig: ExcelExportConfig? = null,
        onComplete: ((File) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportMessage.value = null
            try {
                val config = customConfig ?: _exportConfig.value.copy(
                    memberId = _reportSelectedMemberId.value,
                    groupId = _reportSelectedGroupId.value,
                    classFilter = _reportSelectedClass.value,
                    classFilters = _reportSelectedClasses.value,
                    periodType = _selectedPeriodType.value,
                    customStartDate = _customStartDate.value,
                    customEndDate = _customEndDate.value
                )

                val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // Directly query Room database - Single Source of Truth
                    val freshMembers = repository.getAllMembersIncludingInactiveDirect()
                    val freshGroups = repository.getGroupsDirect()
                    val freshAttendances = repository.getAllAttendancesDirect()
                    val freshHymns = repository.getAllHymnsDirect()
                    val freshHymnAssessments = repository.getAllHymnAssessmentsDirect()
                    val freshBibleAssessments = repository.getAllBibleAssessmentsDirect()
                    val freshExams = repository.getAllExamsDirect()
                    val freshExamResults = repository.getAllExamResultsDirect()
                    val freshVisits = repository.getAllVisitRecordsDirect()

                    ExcelExporter.exportToExcel(
                        context = context,
                        config = config,
                        members = freshMembers,
                        groups = freshGroups,
                        attendances = freshAttendances,
                        hymns = freshHymns,
                        hymnAssessments = freshHymnAssessments,
                        bibleAssessments = freshBibleAssessments,
                        exams = freshExams,
                        examResults = freshExamResults,
                        visitRecords = freshVisits
                    )
                }

                _lastExportedFile.value = file
                _exportMessage.value = "تم استخراج تقرير Excel بنجاح (${file.name})"
                onComplete?.invoke(file)
            } catch (e: Exception) {
                _exportMessage.value = "خطأ أثناء استخراج التقرير: ${e.localizedMessage}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun dismissExportMessage() {
        _exportMessage.value = null
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    com.example.data.remote.SupabaseConfig.init(application)
                    val db = DeaconsDatabase.getDatabase(application, kotlinx.coroutines.GlobalScope)
                    val auth = AuthManager(application)
                    val remote = com.example.data.remote.RemoteSync(db, auth, application)
                    val repo = DeaconsRepository(db, remote)
                    return DeaconsViewModel(application, repo, auth) as T
                }
            }
    }
}
