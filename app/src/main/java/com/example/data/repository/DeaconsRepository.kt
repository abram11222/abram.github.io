package com.example.data.repository

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
import com.example.data.remote.RemoteSync
import com.example.util.BarcodeGenerator
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicLong

/**
 * Offline-first repository: reads from Room (so existing screens work unchanged),
 * mirrors writes to Supabase (when [remote] is set), and can pull the full server
 * dataset into Room via [syncFromRemote].
 */
class DeaconsRepository(
    private val database: DeaconsDatabase,
    private val remote: RemoteSync? = null
) {
    // ------------------------- REMOTE SYNC -------------------------
    suspend fun syncFromRemote(forceReplace: Boolean = false): Result<String> =
        remote?.syncFromRemote(forceReplace) ?: Result.success("تم بنجاح")

    suspend fun pushToRemote(): Result<String> =
        remote?.pushToRemote() ?: Result.success("تم بنجاح")

    suspend fun smartSync(): Result<String> =
        remote?.smartSync() ?: Result.success("تم بنجاح")

    suspend fun testCloudConnection(): Result<String> =
        remote?.testConnection() ?: Result.failure(Exception("الربط السحابي غير متوفر"))

    val isRemoteEnabled: Boolean get() = remote != null

    // Room used auto-increment IDs. That is unsafe in a multi-device offline
    // system because two phones can generate the same ID. New local records
    // therefore receive a high, time-based ID before being mirrored to Supabase.
    private val idGenerator = AtomicLong(System.currentTimeMillis() * 1000L)
    private fun nextLocalId(): Long = idGenerator.incrementAndGet()

    // SchoolClass Operations
    val allSchoolClasses: Flow<List<SchoolClass>> = database.schoolClassDao().getAllSchoolClasses()
    suspend fun getSchoolClassesDirect(): List<SchoolClass> = database.schoolClassDao().getAllSchoolClassesDirect()
    suspend fun insertSchoolClass(schoolClass: SchoolClass): Long {
        val id = if (schoolClass.id > 0) schoolClass.id else nextLocalId()
        database.schoolClassDao().insertSchoolClass(schoolClass.copy(id = id))
        val saved = schoolClass.copy(id = id)
        remote?.upsert(RemoteSync.T_CLASSES, saved)
        return id
    }
    suspend fun updateSchoolClass(schoolClass: SchoolClass) {
        database.schoolClassDao().updateSchoolClass(schoolClass)
        remote?.upsert(RemoteSync.T_CLASSES, schoolClass)
    }
    suspend fun deleteSchoolClass(schoolClass: SchoolClass) {
        database.schoolClassDao().deleteSchoolClass(schoolClass)
        remote?.delete(RemoteSync.T_CLASSES, schoolClass.id)
    }

    // Member Operations
    val allMembers: Flow<List<Member>> = database.memberDao().getAllMembers()
    fun getMemberById(id: Long): Flow<Member?> = database.memberDao().getMemberById(id)
    suspend fun getMemberByIdDirect(id: Long): Member? = database.memberDao().getMemberByIdDirect(id)
    fun getMembersByGroup(groupId: Long): Flow<List<Member>> = database.memberDao().getMembersByGroup(groupId)
    suspend fun insertMember(member: Member): Long {
        val id = if (member.id > 0) member.id else nextLocalId()
        database.memberDao().insertMember(member.copy(id = id))
        val saved = member.copy(id = id)
        if (id > 0) {
            val barcodeValue = BarcodeGenerator.getBarcodeId(id)
            val bcId = database.memberBarcodeDao().insertBarcode(
                MemberBarcode(
                    memberId = id,
                    barcodeValue = barcodeValue,
                    barcodeFormat = "CODE_128"
                )
            )
            remote?.upsert(RemoteSync.T_BARCODES, MemberBarcode(id = bcId, memberId = id, barcodeValue = barcodeValue, barcodeFormat = "CODE_128"))
        }
        remote?.upsert(RemoteSync.T_MEMBERS, saved)
        return id
    }
    suspend fun updateMember(member: Member) {
        database.memberDao().updateMember(member)
        remote?.upsert(RemoteSync.T_MEMBERS, member)
    }
    suspend fun deleteMember(member: Member) {
        member.profileImage?.let { path ->
            try {
                val file = java.io.File(path)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        database.withTransaction {
            database.memberBarcodeDao().deleteBarcodeByMemberId(member.id)
            database.attendanceDao().deleteAttendancesForMember(member.id)
            database.hymnAssessmentDao().deleteHymnAssessmentsForMember(member.id)
            database.bibleAssessmentDao().deleteBibleAssessmentsForMember(member.id)
            database.examDao().deleteExamResultsForMember(member.id)
            database.visitRecordDao().deleteVisitsForMember(member.id)
            database.memberDao().deleteMember(member)
        }
        // Server FK ON DELETE CASCADE handles related rows.
        remote?.delete(RemoteSync.T_MEMBERS, member.id)
    }
    val membersCount: Flow<Int> = database.memberDao().getMembersCount()

    // Barcode Operations
    val allBarcodes: Flow<List<MemberBarcode>> = database.memberBarcodeDao().getAllBarcodes()
    val allMemberBarcodes: Flow<List<MemberBarcode>> = allBarcodes
    fun getBarcodeForMember(memberId: Long): Flow<MemberBarcode?> = database.memberBarcodeDao().getBarcodeForMember(memberId)
    suspend fun getBarcodeForMemberSync(memberId: Long): MemberBarcode? = database.memberBarcodeDao().getBarcodeForMemberSync(memberId)
    suspend fun getBarcodeByValue(barcodeValue: String): MemberBarcode? = database.memberBarcodeDao().getBarcodeByValue(barcodeValue)
    suspend fun ensureMemberBarcode(memberId: Long): MemberBarcode {
        val existing = database.memberBarcodeDao().getBarcodeForMemberSync(memberId)
        if (existing != null) return existing
        val barcodeValue = BarcodeGenerator.getBarcodeId(memberId)
        val barcode = MemberBarcode(
            memberId = memberId,
            barcodeValue = barcodeValue,
            barcodeFormat = "CODE_128"
        )
        val id = if (barcode.id > 0) barcode.id else nextLocalId()
        database.memberBarcodeDao().insertBarcode(barcode.copy(id = id))
        val saved = barcode.copy(id = id)
        remote?.upsert(RemoteSync.T_BARCODES, saved)
        return saved
    }
    suspend fun insertMemberBarcode(barcode: MemberBarcode): Long {
        val id = if (barcode.id > 0) barcode.id else nextLocalId()
        database.memberBarcodeDao().insertBarcode(barcode.copy(id = id))
        remote?.upsert(RemoteSync.T_BARCODES, barcode.copy(id = id))
        return id
    }
    suspend fun insertMemberBarcodes(barcodes: List<MemberBarcode>) {
        database.memberBarcodeDao().insertBarcodes(barcodes)
        barcodes.forEach { remote?.upsert(RemoteSync.T_BARCODES, it) }
    }

    // Group Operations
    val allGroups: Flow<List<Group>> = database.groupDao().getAllGroups()
    fun getGroupById(id: Long): Flow<Group?> = database.groupDao().getGroupById(id)
    suspend fun insertGroup(group: Group): Long {
        val id = if (group.id > 0) group.id else nextLocalId()
        database.groupDao().insertGroup(group.copy(id = id))
        remote?.upsert(RemoteSync.T_GROUPS, group.copy(id = id))
        return id
    }
    suspend fun updateGroup(group: Group) {
        database.groupDao().updateGroup(group)
        remote?.upsert(RemoteSync.T_GROUPS, group)
    }
    suspend fun deleteGroup(group: Group) {
        database.groupDao().deleteGroup(group)
        remote?.delete(RemoteSync.T_GROUPS, group.id)
    }

    // Servant Operations
    val allServants: Flow<List<Servant>> = database.servantDao().getAllServants()
    suspend fun insertServant(servant: Servant): Long {
        val id = if (servant.id > 0) servant.id else nextLocalId()
        database.servantDao().insertServant(servant.copy(id = id))
        remote?.upsert(RemoteSync.T_SERVANTS, servant.copy(id = id))
        return id
    }
    suspend fun updateServant(servant: Servant) {
        database.servantDao().updateServant(servant)
        remote?.upsert(RemoteSync.T_SERVANTS, servant)
    }
    suspend fun deleteServant(servant: Servant) {
        database.servantDao().deleteServant(servant)
        remote?.delete(RemoteSync.T_SERVANTS, servant.id)
    }

    // Attendance Operations
    val allAttendances: Flow<List<Attendance>> = database.attendanceDao().getAllAttendances()
    fun getAttendancesForMember(memberId: Long): Flow<List<Attendance>> = database.attendanceDao().getAttendancesForMember(memberId)
    fun getAttendancesByDateAndType(date: String, type: AttendanceType): Flow<List<Attendance>> =
        database.attendanceDao().getAttendancesByDateAndType(date, type)
    fun getAttendancesInRange(startDate: String, endDate: String): Flow<List<Attendance>> =
        database.attendanceDao().getAttendancesInRange(startDate, endDate)
    suspend fun insertAttendance(attendance: Attendance): Long {
        val id = if (attendance.id > 0) attendance.id else nextLocalId()
        database.attendanceDao().insertAttendance(attendance.copy(id = id))
        remote?.upsert(RemoteSync.T_ATTENDANCES, attendance.copy(id = id))
        return id
    }
    suspend fun updateAttendance(attendance: Attendance) {
        database.attendanceDao().updateAttendance(attendance)
        remote?.upsert(RemoteSync.T_ATTENDANCES, attendance)
    }
    suspend fun deleteAttendance(attendance: Attendance) {
        database.attendanceDao().deleteAttendance(attendance)
        remote?.delete(RemoteSync.T_ATTENDANCES, attendance.id)
    }
    suspend fun deleteAttendanceForMemberAndDate(memberId: Long, date: String, type: AttendanceType, massNumber: Int?) =
        database.attendanceDao().deleteAttendanceForMemberAndDate(memberId, date, type, massNumber)

    suspend fun recordAttendance(
        memberId: Long,
        date: String,
        type: AttendanceType,
        status: AttendanceStatus,
        massNumber: Int? = null,
        notes: String = ""
    ) {
        val existing = database.attendanceDao().findExistingAttendance(memberId, date, type, massNumber)
        if (existing != null) {
            val updated = existing.copy(status = status, notes = notes)
            database.attendanceDao().updateAttendance(updated)
            remote?.upsert(RemoteSync.T_ATTENDANCES, updated)
        } else {
            val newAttendance = Attendance(
                id = nextLocalId(),
                memberId = memberId,
                date = date,
                type = type,
                massNumber = massNumber,
                status = status,
                notes = notes
            )
            database.attendanceDao().insertAttendance(newAttendance)
            remote?.upsert(RemoteSync.T_ATTENDANCES, newAttendance)
        }
    }

    // Hymn Operations
    val allHymns: Flow<List<Hymn>> = database.hymnDao().getAllHymns()
    val allHymnsIncludingInactive: Flow<List<Hymn>> = database.hymnDao().getAllHymnsIncludingInactive()
    fun getHymnById(id: Long): Flow<Hymn?> = database.hymnDao().getHymnById(id)
    suspend fun insertHymn(hymn: Hymn): Long {
        val id = if (hymn.id > 0) hymn.id else nextLocalId()
        database.hymnDao().insertHymn(hymn.copy(id = id))
        remote?.upsert(RemoteSync.T_HYMNS, hymn.copy(id = id))
        return id
    }
    suspend fun updateHymn(hymn: Hymn) {
        database.hymnDao().updateHymn(hymn)
        remote?.upsert(RemoteSync.T_HYMNS, hymn)
    }
    suspend fun deleteHymn(hymn: Hymn, forceArchiveIfAssessed: Boolean = true) {
        val assessmentCount = database.hymnDao().countAssessmentsForHymn(hymn.id)
        if (assessmentCount > 0 && forceArchiveIfAssessed) {
            database.hymnDao().updateHymn(hymn.copy(isActive = false))
            remote?.upsert(RemoteSync.T_HYMNS, hymn.copy(isActive = false))
        } else {
            database.hymnDao().deleteHymn(hymn)
            remote?.delete(RemoteSync.T_HYMNS, hymn.id)
        }
    }

    // Hymn Assessment Operations
    val allHymnAssessments: Flow<List<HymnAssessment>> = database.hymnAssessmentDao().getAllHymnAssessments()
    fun getHymnAssessmentsForMember(memberId: Long): Flow<List<HymnAssessment>> =
        database.hymnAssessmentDao().getHymnAssessmentsForMember(memberId)
    suspend fun getAssessmentForMemberAndHymn(memberId: Long, hymnId: Long): HymnAssessment? =
        database.hymnAssessmentDao().getAssessmentForMemberAndHymn(memberId, hymnId)
    suspend fun insertHymnAssessment(assessment: HymnAssessment): Long {
        val id = if (assessment.id > 0) assessment.id else nextLocalId()
        database.hymnAssessmentDao().insertHymnAssessment(assessment.copy(id = id))
        remote?.upsert(RemoteSync.T_HYMN_ASS, assessment.copy(id = id))
        return id
    }
    suspend fun updateHymnAssessment(assessment: HymnAssessment) {
        val updated = assessment.copy(updatedAt = System.currentTimeMillis())
        database.hymnAssessmentDao().updateHymnAssessment(updated)
        remote?.upsert(RemoteSync.T_HYMN_ASS, updated)
    }
    suspend fun deleteHymnAssessment(assessment: HymnAssessment) {
        database.hymnAssessmentDao().deleteHymnAssessment(assessment)
        remote?.delete(RemoteSync.T_HYMN_ASS, assessment.id)
    }

    // Bible Lesson & Assessment Operations
    val allBibleLessons: Flow<List<BibleLesson>> = database.bibleLessonDao().getAllBibleLessons()
    val activeBibleLessons: Flow<List<BibleLesson>> = database.bibleLessonDao().getAllActiveBibleLessons()
    suspend fun insertBibleLesson(lesson: BibleLesson): Long {
        val id = if (lesson.id > 0) lesson.id else nextLocalId()
        database.bibleLessonDao().insertBibleLesson(lesson.copy(id = id))
        remote?.upsert(RemoteSync.T_BIBLE_LESSONS, lesson.copy(id = id))
        return id
    }
    suspend fun updateBibleLesson(lesson: BibleLesson) {
        database.bibleLessonDao().updateBibleLesson(lesson)
        remote?.upsert(RemoteSync.T_BIBLE_LESSONS, lesson)
    }
    suspend fun deleteBibleLesson(lesson: BibleLesson, forceArchiveIfAssessed: Boolean = true) {
        val count = database.bibleLessonDao().countAssessmentsForLesson(lesson.name)
        if (count > 0 && forceArchiveIfAssessed) {
            database.bibleLessonDao().updateBibleLesson(lesson.copy(isActive = false))
            remote?.upsert(RemoteSync.T_BIBLE_LESSONS, lesson.copy(isActive = false))
        } else {
            database.bibleLessonDao().deleteBibleLesson(lesson)
            remote?.delete(RemoteSync.T_BIBLE_LESSONS, lesson.id)
        }
    }

    val allBibleAssessments: Flow<List<BibleAssessment>> = database.bibleAssessmentDao().getAllBibleAssessments()
    fun getBibleAssessmentsForMember(memberId: Long): Flow<List<BibleAssessment>> =
        database.bibleAssessmentDao().getBibleAssessmentsForMember(memberId)
    suspend fun insertBibleAssessment(assessment: BibleAssessment): Long {
        val id = if (assessment.id > 0) assessment.id else nextLocalId()
        database.bibleAssessmentDao().insertBibleAssessment(assessment.copy(id = id))
        remote?.upsert(RemoteSync.T_BIBLE_ASS, assessment.copy(id = id))
        return id
    }
    suspend fun updateBibleAssessment(assessment: BibleAssessment) {
        database.bibleAssessmentDao().updateBibleAssessment(assessment)
        remote?.upsert(RemoteSync.T_BIBLE_ASS, assessment)
    }
    suspend fun deleteBibleAssessment(assessment: BibleAssessment) {
        database.bibleAssessmentDao().deleteBibleAssessment(assessment)
        remote?.delete(RemoteSync.T_BIBLE_ASS, assessment.id)
    }

    // Exam Operations
    val allExams: Flow<List<Exam>> = database.examDao().getAllExams()
    val activeExams: Flow<List<Exam>> = database.examDao().getActiveExams()
    val allExamResults: Flow<List<ExamResult>> = database.examDao().getAllExamResults()
    fun getExamResultsForMember(memberId: Long): Flow<List<ExamResult>> =
        database.examDao().getExamResultsForMember(memberId)
    suspend fun insertExam(exam: Exam): Long {
        val id = if (exam.id > 0) exam.id else nextLocalId()
        database.examDao().insertExam(exam.copy(id = id))
        remote?.upsert(RemoteSync.T_EXAMS, exam.copy(id = id))
        return id
    }
    suspend fun updateExam(exam: Exam) {
        database.examDao().updateExam(exam)
        remote?.upsert(RemoteSync.T_EXAMS, exam)
    }
    suspend fun deleteExam(exam: Exam, forceArchiveIfResults: Boolean = true) {
        val count = database.examDao().countResultsForExam(exam.id)
        if (count > 0 && forceArchiveIfResults) {
            database.examDao().updateExam(exam.copy(isActive = false))
            remote?.upsert(RemoteSync.T_EXAMS, exam.copy(isActive = false))
        } else {
            database.examDao().deleteExam(exam)
            remote?.delete(RemoteSync.T_EXAMS, exam.id)
        }
    }
    suspend fun insertExamResult(result: ExamResult): Long {
        val id = if (result.id > 0) result.id else nextLocalId()
        database.examDao().insertExamResult(result.copy(id = id))
        remote?.upsert(RemoteSync.T_EXAM_RESULTS, result.copy(id = id))
        return id
    }
    suspend fun updateExamResult(result: ExamResult) {
        database.examDao().updateExamResult(result)
        remote?.upsert(RemoteSync.T_EXAM_RESULTS, result)
    }
    suspend fun deleteExamResult(result: ExamResult) {
        database.examDao().deleteExamResult(result)
        remote?.delete(RemoteSync.T_EXAM_RESULTS, result.id)
    }

    // Visit Record Operations
    val allVisitRecords: Flow<List<VisitRecord>> = database.visitRecordDao().getAllVisitRecords()
    suspend fun getAllVisitRecordsDirect(): List<VisitRecord> = database.visitRecordDao().getAllVisitRecordsDirect()
    fun getVisitsForMember(memberId: Long): Flow<List<VisitRecord>> = database.visitRecordDao().getVisitsForMember(memberId)
    suspend fun getVisitsForMemberDirect(memberId: Long): List<VisitRecord> = database.visitRecordDao().getVisitsForMemberDirect(memberId)
    suspend fun insertVisitRecord(record: VisitRecord): Long {
        val id = if (record.id > 0) record.id else nextLocalId()
        database.visitRecordDao().insertVisitRecord(record.copy(id = id))
        remote?.upsert(RemoteSync.T_VISITS, record.copy(id = id))
        return id
    }
    suspend fun updateVisitRecord(record: VisitRecord) {
        database.visitRecordDao().updateVisitRecord(record)
        remote?.upsert(RemoteSync.T_VISITS, record)
    }
    suspend fun deleteVisitRecord(record: VisitRecord) {
        database.visitRecordDao().deleteVisitRecord(record)
        remote?.delete(RemoteSync.T_VISITS, record.id)
    }

    // Evaluation Period Operations
    val allPeriods: Flow<List<EvaluationPeriod>> = database.evaluationPeriodDao().getAllPeriods()
    suspend fun insertPeriod(period: EvaluationPeriod): Long {
        val id = if (period.id > 0) period.id else nextLocalId()
        database.evaluationPeriodDao().insertPeriod(period.copy(id = id))
        remote?.upsert(RemoteSync.T_PERIODS, period.copy(id = id))
        return id
    }
    suspend fun updatePeriod(period: EvaluationPeriod) {
        database.evaluationPeriodDao().updatePeriod(period)
        remote?.upsert(RemoteSync.T_PERIODS, period)
    }
    suspend fun deletePeriod(period: EvaluationPeriod) {
        database.evaluationPeriodDao().deletePeriod(period)
        remote?.delete(RemoteSync.T_PERIODS, period.id)
    }

    // App Settings Operations
    val allSettings: Flow<List<com.example.data.model.AppSetting>> = database.appSettingDao().getAllSettings()
    fun getSetting(key: String): Flow<com.example.data.model.AppSetting?> = database.appSettingDao().getSetting(key)
    suspend fun getSettingDirect(key: String): com.example.data.model.AppSetting? = database.appSettingDao().getSettingDirect(key)
    suspend fun setSetting(key: String, value: String) {
        val setting = com.example.data.model.AppSetting(key = key, value = value)
        database.appSettingDao().setSetting(setting)
        remote?.upsert(RemoteSync.T_SETTINGS, setting)
    }

    suspend fun getServiceAttendanceTarget(): Int {
        val setting = database.appSettingDao().getSettingDirect("service_attendance_target")
        return setting?.value?.toIntOrNull() ?: 4
    }

    suspend fun setServiceAttendanceTarget(target: Int) {
        setSetting("service_attendance_target", target.coerceIn(1, 10).toString())
    }

    suspend fun getServiceMinistryAttendanceTarget(): Int {
        val setting = database.appSettingDao().getSettingDirect("service_ministry_attendance_target")
        return setting?.value?.toIntOrNull() ?: 4
    }

    suspend fun setServiceMinistryAttendanceTarget(target: Int) {
        setSetting("service_ministry_attendance_target", target.coerceIn(1, 10).toString())
    }

    suspend fun getOverdueGraceSettings(): com.example.util.OverdueGraceSettings {
        val settingDao = database.appSettingDao()
        val attDays = settingDao.getSettingDirect(com.example.util.OverdueGraceSettings.KEY_ATTENDANCE_DAYS)?.value?.toIntOrNull()
            ?: com.example.util.OverdueGraceSettings.DEFAULT.attendanceAbsenceDays
        val hymnDays = settingDao.getSettingDirect(com.example.util.OverdueGraceSettings.KEY_HYMN_DAYS)?.value?.toIntOrNull()
            ?: com.example.util.OverdueGraceSettings.DEFAULT.hymnOverdueDays
        val bibleDays = settingDao.getSettingDirect(com.example.util.OverdueGraceSettings.KEY_BIBLE_DAYS)?.value?.toIntOrNull()
            ?: com.example.util.OverdueGraceSettings.DEFAULT.bibleOverdueDays
        val examDays = settingDao.getSettingDirect(com.example.util.OverdueGraceSettings.KEY_EXAM_DAYS)?.value?.toIntOrNull()
            ?: com.example.util.OverdueGraceSettings.DEFAULT.examOverdueDays
        val notifEnabled = settingDao.getSettingDirect(com.example.util.OverdueGraceSettings.KEY_NOTIFICATIONS_ENABLED)?.value?.toBooleanStrictOrNull()
            ?: com.example.util.OverdueGraceSettings.DEFAULT.notificationsEnabled

        return com.example.util.OverdueGraceSettings(
            attendanceAbsenceDays = attDays,
            hymnOverdueDays = hymnDays,
            bibleOverdueDays = bibleDays,
            examOverdueDays = examDays,
            notificationsEnabled = notifEnabled
        )
    }

    suspend fun saveOverdueGraceSettings(settings: com.example.util.OverdueGraceSettings) {
        setSetting(com.example.util.OverdueGraceSettings.KEY_ATTENDANCE_DAYS, settings.attendanceAbsenceDays.toString())
        setSetting(com.example.util.OverdueGraceSettings.KEY_HYMN_DAYS, settings.hymnOverdueDays.toString())
        setSetting(com.example.util.OverdueGraceSettings.KEY_BIBLE_DAYS, settings.bibleOverdueDays.toString())
        setSetting(com.example.util.OverdueGraceSettings.KEY_EXAM_DAYS, settings.examOverdueDays.toString())
        setSetting(com.example.util.OverdueGraceSettings.KEY_NOTIFICATIONS_ENABLED, settings.notificationsEnabled.toString())
    }

    // Direct Getters for Background / Sync Processing / Excel Generation
    suspend fun getAllMembersDirect(): List<Member> = database.memberDao().getAllMembersDirect()
    suspend fun getAllMembersIncludingInactiveDirect(): List<Member> = database.memberDao().getAllMembersIncludingInactiveDirect()
    suspend fun getMembersDirect(): List<Member> = database.memberDao().getAllMembersDirect()
    suspend fun getGroupsDirect(): List<Group> = database.groupDao().getAllGroupsDirect()
    suspend fun getAllAttendancesDirect(): List<Attendance> = database.attendanceDao().getAllAttendancesDirect()
    suspend fun getAllHymnsDirect(): List<Hymn> = database.hymnDao().getAllHymnsDirect()
    suspend fun getAllHymnAssessmentsDirect(): List<HymnAssessment> = database.hymnAssessmentDao().getAllHymnAssessmentsDirect()
    suspend fun getAllBibleAssessmentsDirect(): List<BibleAssessment> = database.bibleAssessmentDao().getAllBibleAssessmentsDirect()
    suspend fun getAllExamsDirect(): List<Exam> = database.examDao().getAllExamsDirect()
    suspend fun getAllExamResultsDirect(): List<ExamResult> = database.examDao().getAllExamResultsDirect()

    // Import from Excel with Duplicate Prevention and Auto-Group Resolution
    suspend fun importMembers(
        parsedRows: List<com.example.util.ExcelImporter.ParsedMemberRow>,
        ignoreInvalidRows: Boolean = true
    ): com.example.util.ExcelImporter.ImportResult {
        var importedCount = 0
        var updatedCount = 0
        var ignoredCount = 0
        var errorsCount = 0

        val existingMembers = database.memberDao().getAllMembersDirect()
        val existingGroups = database.groupDao().getAllGroupsDirect().toMutableList()
        val groupNameToId = existingGroups.associate { it.name.trim() to it.id }.toMutableMap()

        for (row in parsedRows) {
            if (!row.isValid) {
                if (ignoreInvalidRows) {
                    ignoredCount++
                    continue
                } else {
                    errorsCount++
                    continue
                }
            }

            try {
                // Resolve Group ID or create group if doesn't exist
                val groupName = row.groupName.ifBlank { "عام" }
                val groupId = groupNameToId.getOrPut(groupName) {
                    val newGroup = Group(name = groupName, description = "مجموعة مستوردة من Excel")
                    val newId = database.groupDao().insertGroup(newGroup)
                    existingGroups.add(newGroup.copy(id = newId))
                    remote?.upsert(RemoteSync.T_GROUPS, newGroup.copy(id = newId))
                    newId
                }

                // Check duplicate: by ID first, or by (fullName + phone)
                var matchedMember: Member? = null
                if (row.id != null && row.id > 0) {
                    matchedMember = existingMembers.find { it.id == row.id }
                }
                if (matchedMember == null) {
                    matchedMember = existingMembers.find {
                        it.fullName.trim().equals(row.fullName.trim(), ignoreCase = true) &&
                                (it.phone.trim() == row.phone.trim() || row.phone.isBlank())
                    }
                }

                if (matchedMember != null) {
                    // Update existing
                    val updated = matchedMember.copy(
                        fullName = row.fullName,
                        groupId = groupId,
                        schoolClass = row.schoolClass.ifBlank { matchedMember.schoolClass },
                        birthDate = row.birthDate.ifBlank { matchedMember.birthDate },
                        phone = row.phone.ifBlank { matchedMember.phone },
                        parentPhone = row.parentPhone.ifBlank { matchedMember.parentPhone },
                        governorate = row.governorate.ifBlank { matchedMember.governorate },
                        center = row.center.ifBlank { matchedMember.center },
                        area = row.area.ifBlank { matchedMember.area },
                        street = row.street.ifBlank { matchedMember.street },
                        houseNumber = row.houseNumber.ifBlank { matchedMember.houseNumber },
                        addressDetails = row.addressDetails.ifBlank { matchedMember.addressDetails },
                        notes = if (row.notes.isNotBlank()) row.notes else matchedMember.notes,
                        updatedAt = System.currentTimeMillis()
                    )
                    database.memberDao().updateMember(updated)
                    remote?.upsert(RemoteSync.T_MEMBERS, updated)
                    updatedCount++
                } else {
                    // Insert new
                    val newMember = Member(
                        fullName = row.fullName,
                        groupId = groupId,
                        schoolClass = row.schoolClass,
                        birthDate = row.birthDate,
                        phone = row.phone,
                        parentPhone = row.parentPhone,
                        governorate = row.governorate,
                        center = row.center,
                        area = row.area,
                        street = row.street,
                        houseNumber = row.houseNumber,
                        addressDetails = row.addressDetails,
                        notes = row.notes,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    val newId = database.memberDao().insertMember(newMember)
                    importedCount++
                    remote?.upsert(RemoteSync.T_MEMBERS, newMember.copy(id = newId))
                }
            } catch (e: Exception) {
                errorsCount++
            }
        }

        val summary = "تمت المعالجة: تم استيراد $importedCount جديد، وتحديث $updatedCount موجود، وتجاهل $ignoredCount صف."
        return com.example.util.ExcelImporter.ImportResult(
            importedCount = importedCount,
            updatedCount = updatedCount,
            ignoredCount = ignoredCount,
            errorsCount = errorsCount,
            summaryMessage = summary
        )
    }

    suspend fun clearAllMembers() {
        database.memberDao().clearAllMembers()
    }

    // Reset/Seed if empty
    suspend fun checkAndSeedIfEmpty() {
        DeaconsDatabase.populateInitialData(database)
    }
}
