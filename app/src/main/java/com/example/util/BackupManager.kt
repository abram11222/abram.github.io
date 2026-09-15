package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.database.DeaconsDatabase
import com.example.data.model.Attendance
import com.example.data.model.BibleAssessment
import com.example.data.model.BibleLesson
import com.example.data.model.EvaluationPeriod
import com.example.data.model.Exam
import com.example.data.model.ExamResult
import com.example.data.model.Group
import com.example.data.model.Hymn
import com.example.data.model.HymnAssessment
import com.example.data.model.Member
import com.example.data.model.SchoolClass
import com.example.data.model.Servant
import com.example.data.model.VisitRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val exportDate: String,
    val appVersion: String,
    val members: List<Member>,
    val groups: List<Group>,
    val servants: List<Servant>,
    val schoolClasses: List<SchoolClass>,
    val attendances: List<Attendance>,
    val hymns: List<Hymn>,
    val hymnAssessments: List<HymnAssessment>,
    val bibleLessons: List<BibleLesson>,
    val bibleAssessments: List<BibleAssessment>,
    val exams: List<Exam>,
    val examResults: List<ExamResult>,
    val visitRecords: List<VisitRecord>,
    val periods: List<EvaluationPeriod>
)

data class MergePreviewResult(
    val backupDate: String,
    val totalBackupMembers: Int,
    val newMembersCount: Int,
    val existingMembersCount: Int,
    val attendancesCount: Int,
    val hymnsCount: Int,
    val assessmentsCount: Int,
    val examsCount: Int,
    val visitsCount: Int
)

data class MergeExecutionResult(
    val insertedMembers: Int,
    val updatedMembers: Int,
    val insertedAttendances: Int,
    val insertedAssessments: Int,
    val insertedVisits: Int,
    val message: String
)

object BackupManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    suspend fun createBackupJson(
        context: Context,
        database: DeaconsDatabase
    ): File = withContext(Dispatchers.IO) {
        val members = database.memberDao().getAllMembersDirect()
        val groups = database.groupDao().getAllGroupsDirect()
        val servants = database.servantDao().getAllServantsDirect()
        val schoolClasses = database.schoolClassDao().getAllSchoolClassesDirect()
        val attendances = database.attendanceDao().getAllAttendancesDirect()
        val hymns = database.hymnDao().getAllHymnsDirect()
        val hymnAssessments = database.hymnAssessmentDao().getAllHymnAssessmentsDirect()
        val bibleLessons = database.bibleLessonDao().getAllBibleLessonsDirect()
        val bibleAssessments = database.bibleAssessmentDao().getAllBibleAssessmentsDirect()
        val exams = database.examDao().getAllExamsDirect()
        val examResults = database.examDao().getAllExamResultsDirect()
        val visitRecords = database.visitRecordDao().getAllVisitRecordsDirect()
        val periods = database.evaluationPeriodDao().getAllPeriodsDirect()

        val rootJson = JSONObject().apply {
            put("backupVersion", 2)
            put("app", "خدمة الشمامسة - كنيسة أبي سيفين والعزب ودير الملاك")
            put("exportDate", dateFormat.format(Date()))

            // Members Array
            val membersArray = JSONArray()
            members.forEach { m: Member ->
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("fullName", m.fullName)
                    put("birthDate", m.birthDate)
                    put("phone", m.phone)
                    put("parentPhone", m.parentPhone)
                    put("governorate", m.governorate)
                    put("center", m.center)
                    put("area", m.area)
                    put("street", m.street)
                    put("houseNumber", m.houseNumber)
                    put("addressDetails", m.addressDetails)
                    put("schoolClass", m.schoolClass)
                    put("notes", m.notes)
                    put("groupId", m.groupId ?: JSONObject.NULL)
                    put("isActive", m.isActive)
                    put("profileImage", m.profileImage ?: JSONObject.NULL)
                }
                membersArray.put(obj)
            }
            put("members", membersArray)

            // Groups
            val groupsArray = JSONArray()
            groups.forEach { g: Group ->
                groupsArray.put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("description", g.description)
                })
            }
            put("groups", groupsArray)

            // Classes
            val classesArray = JSONArray()
            schoolClasses.forEach { c: SchoolClass ->
                classesArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("orderIndex", c.orderIndex)
                })
            }
            put("schoolClasses", classesArray)

            // Attendances
            val attArray = JSONArray()
            attendances.forEach { a: Attendance ->
                attArray.put(JSONObject().apply {
                    put("id", a.id)
                    put("memberId", a.memberId)
                    put("date", a.date)
                    put("type", a.type.name)
                    put("status", a.status.name)
                    put("massNumber", a.massNumber ?: JSONObject.NULL)
                    put("notes", a.notes)
                })
            }
            put("attendances", attArray)

            // Hymns
            val hymnsArray = JSONArray()
            hymns.forEach { h: Hymn ->
                hymnsArray.put(JSONObject().apply {
                    put("id", h.id)
                    put("name", h.name)
                    put("category", h.category)
                    put("maxScore", h.maxScore)
                    put("notes", h.notes)
                    put("isActive", h.isActive)
                    put("durationInDays", h.durationInDays)
                    put("deadline", h.deadline)
                })
            }
            put("hymns", hymnsArray)

            // Hymn Assessments
            val hymnAssessmentsArray = JSONArray()
            hymnAssessments.forEach { ha: HymnAssessment ->
                hymnAssessmentsArray.put(JSONObject().apply {
                    put("id", ha.id)
                    put("memberId", ha.memberId)
                    put("hymnId", ha.hymnId)
                    put("date", ha.date)
                    put("score", ha.score)
                    put("maxScore", ha.maxScore)
                    put("notes", ha.notes)
                })
            }
            put("hymnAssessments", hymnAssessmentsArray)

            // Bible Lessons
            val bibleLessonsArray = JSONArray()
            bibleLessons.forEach { bl: BibleLesson ->
                bibleLessonsArray.put(JSONObject().apply {
                    put("id", bl.id)
                    put("name", bl.name)
                    put("notes", bl.notes)
                    put("isActive", bl.isActive)
                    put("durationInDays", bl.durationInDays)
                    put("deadline", bl.deadline)
                })
            }
            put("bibleLessons", bibleLessonsArray)

            // Bible Assessments
            val bibleAssessmentsArray = JSONArray()
            bibleAssessments.forEach { ba: BibleAssessment ->
                bibleAssessmentsArray.put(JSONObject().apply {
                    put("id", ba.id)
                    put("memberId", ba.memberId)
                    put("date", ba.date)
                    put("lessonName", ba.lessonName)
                    put("score", ba.score)
                    put("maxScore", ba.maxScore)
                    put("notes", ba.notes)
                    put("lessonId", ba.lessonId ?: JSONObject.NULL)
                })
            }
            put("bibleAssessments", bibleAssessmentsArray)

            // Exams
            val examsArray = JSONArray()
            exams.forEach { e: Exam ->
                examsArray.put(JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("date", e.date)
                    put("maxScore", e.maxScore)
                    put("notes", e.notes)
                    put("isActive", e.isActive)
                    put("durationInDays", e.durationInDays)
                    put("deadline", e.deadline)
                })
            }
            put("exams", examsArray)

            // Exam Results
            val examResultsArray = JSONArray()
            examResults.forEach { er: ExamResult ->
                examResultsArray.put(JSONObject().apply {
                    put("id", er.id)
                    put("examId", er.examId)
                    put("memberId", er.memberId)
                    put("score", er.score)
                    put("maxScore", er.maxScore)
                    put("date", er.date)
                    put("notes", er.notes)
                })
            }
            put("examResults", examResultsArray)

            // Visits
            val visitsArray = JSONArray()
            visitRecords.forEach { v: VisitRecord ->
                visitsArray.put(JSONObject().apply {
                    put("id", v.id)
                    put("memberId", v.memberId)
                    put("visitDate", v.visitDate)
                    put("notes", v.notes)
                })
            }
            put("visitRecords", visitsArray)
        }

        val backupsDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupsDir, "deacons_backup_$timeStamp.json")
        FileOutputStream(backupFile).use { fos ->
            fos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
        }
        backupFile
    }

    suspend fun parseBackupUri(context: Context, uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalArgumentException("تعذر قراءة ملف النسخة الاحتياطية")

        val root = JSONObject(jsonString)
        val exportDate = root.optString("exportDate", "غير محدد")
        val appVersion = root.optString("backupVersion", "1")

        val membersList = mutableListOf<Member>()
        val membersArray = root.optJSONArray("members") ?: JSONArray()
        for (i in 0 until membersArray.length()) {
            val obj = membersArray.getJSONObject(i)
            membersList.add(
                Member(
                    id = obj.optLong("id", 0L),
                    fullName = obj.optString("fullName", "").trim(),
                    birthDate = obj.optString("birthDate", "").trim(),
                    phone = obj.optString("phone", "").trim(),
                    parentPhone = obj.optString("parentPhone", "").trim(),
                    governorate = obj.optString("governorate", "المنيا").trim(),
                    center = obj.optString("center", "مطاي").trim(),
                    area = obj.optString("area", "").trim(),
                    street = obj.optString("street", "").trim(),
                    houseNumber = obj.optString("houseNumber", "").trim(),
                    addressDetails = obj.optString("addressDetails", "").trim(),
                    schoolClass = obj.optString("schoolClass", "").trim(),
                    notes = obj.optString("notes", "").trim(),
                    groupId = if (obj.isNull("groupId")) null else obj.optLong("groupId"),
                    isActive = obj.optBoolean("isActive", true),
                    profileImage = if (obj.isNull("profileImage")) null else obj.optString("profileImage")
                )
            )
        }

        val attendancesList = mutableListOf<Attendance>()
        val attArray = root.optJSONArray("attendances") ?: JSONArray()
        for (i in 0 until attArray.length()) {
            val obj = attArray.getJSONObject(i)
            try {
                attendancesList.add(
                    Attendance(
                        id = obj.optLong("id", 0L),
                        memberId = obj.getLong("memberId"),
                        date = obj.optString("date", ""),
                        type = com.example.data.model.AttendanceType.valueOf(obj.optString("type", "SERVICE")),
                        status = com.example.data.model.AttendanceStatus.valueOf(obj.optString("status", "PRESENT")),
                        massNumber = if (obj.isNull("massNumber")) null else obj.optInt("massNumber"),
                        notes = obj.optString("notes", "")
                    )
                )
            } catch (_: Exception) {}
        }

        val hymnsList = mutableListOf<Hymn>()
        val hymnsArray = root.optJSONArray("hymns") ?: JSONArray()
        for (i in 0 until hymnsArray.length()) {
            val obj = hymnsArray.getJSONObject(i)
            hymnsList.add(
                Hymn(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", ""),
                    category = obj.optString("category", "عام"),
                    maxScore = obj.optDouble("maxScore", 10.0),
                    notes = obj.optString("notes", ""),
                    isActive = obj.optBoolean("isActive", true),
                    durationInDays = obj.optInt("durationInDays", 14),
                    deadline = obj.optString("deadline", "")
                )
            )
        }

        val hymnAssessmentsList = mutableListOf<HymnAssessment>()
        val haArray = root.optJSONArray("hymnAssessments") ?: JSONArray()
        for (i in 0 until haArray.length()) {
            val obj = haArray.getJSONObject(i)
            hymnAssessmentsList.add(
                HymnAssessment(
                    id = obj.optLong("id", 0L),
                    memberId = obj.getLong("memberId"),
                    hymnId = obj.getLong("hymnId"),
                    date = obj.optString("date", ""),
                    score = obj.optDouble("score", 0.0),
                    maxScore = obj.optDouble("maxScore", 10.0),
                    notes = obj.optString("notes", "")
                )
            )
        }

        val bibleLessonsList = mutableListOf<BibleLesson>()
        val blArray = root.optJSONArray("bibleLessons") ?: JSONArray()
        for (i in 0 until blArray.length()) {
            val obj = blArray.getJSONObject(i)
            bibleLessonsList.add(
                BibleLesson(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", ""),
                    notes = obj.optString("notes", ""),
                    isActive = obj.optBoolean("isActive", true),
                    durationInDays = obj.optInt("durationInDays", 7),
                    deadline = obj.optString("deadline", "")
                )
            )
        }

        val bibleAssessmentsList = mutableListOf<BibleAssessment>()
        val baArray = root.optJSONArray("bibleAssessments") ?: JSONArray()
        for (i in 0 until baArray.length()) {
            val obj = baArray.getJSONObject(i)
            bibleAssessmentsList.add(
                BibleAssessment(
                    id = obj.optLong("id", 0L),
                    memberId = obj.getLong("memberId"),
                    date = obj.optString("date", ""),
                    lessonName = obj.optString("lessonName", ""),
                    score = obj.optDouble("score", 0.0),
                    maxScore = obj.optDouble("maxScore", 10.0),
                    notes = obj.optString("notes", ""),
                    lessonId = if (obj.isNull("lessonId")) null else obj.optLong("lessonId")
                )
            )
        }

        val examsList = mutableListOf<Exam>()
        val examsArray = root.optJSONArray("exams") ?: JSONArray()
        for (i in 0 until examsArray.length()) {
            val obj = examsArray.getJSONObject(i)
            examsList.add(
                Exam(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", ""),
                    date = obj.optString("date", ""),
                    maxScore = obj.optDouble("maxScore", 100.0),
                    notes = obj.optString("notes", ""),
                    isActive = obj.optBoolean("isActive", true),
                    durationInDays = obj.optInt("durationInDays", 30),
                    deadline = obj.optString("deadline", "")
                )
            )
        }

        val examResultsList = mutableListOf<ExamResult>()
        val erArray = root.optJSONArray("examResults") ?: JSONArray()
        for (i in 0 until erArray.length()) {
            val obj = erArray.getJSONObject(i)
            examResultsList.add(
                ExamResult(
                    id = obj.optLong("id", 0L),
                    examId = obj.getLong("examId"),
                    memberId = obj.getLong("memberId"),
                    score = obj.optDouble("score", 0.0),
                    maxScore = obj.optDouble("maxScore", 100.0),
                    date = obj.optString("date", ""),
                    notes = obj.optString("notes", "")
                )
            )
        }

        val visitsList = mutableListOf<VisitRecord>()
        val vArray = root.optJSONArray("visitRecords") ?: JSONArray()
        for (i in 0 until vArray.length()) {
            val obj = vArray.getJSONObject(i)
            visitsList.add(
                VisitRecord(
                    id = obj.optLong("id", 0L),
                    memberId = obj.getLong("memberId"),
                    visitDate = obj.optString("visitDate", ""),
                    notes = obj.optString("notes", "")
                )
            )
        }

        val classesList = mutableListOf<SchoolClass>()
        val clArray = root.optJSONArray("schoolClasses") ?: JSONArray()
        for (i in 0 until clArray.length()) {
            val obj = clArray.getJSONObject(i)
            classesList.add(
                SchoolClass(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", ""),
                    orderIndex = obj.optInt("orderIndex", 0)
                )
            )
        }

        BackupData(
            exportDate = exportDate,
            appVersion = appVersion,
            members = membersList,
            groups = emptyList(),
            servants = emptyList(),
            schoolClasses = classesList,
            attendances = attendancesList,
            hymns = hymnsList,
            hymnAssessments = hymnAssessmentsList,
            bibleLessons = bibleLessonsList,
            bibleAssessments = bibleAssessmentsList,
            exams = examsList,
            examResults = examResultsList,
            visitRecords = visitsList,
            periods = emptyList()
        )
    }

    suspend fun generateMergePreview(
        backupData: BackupData,
        database: DeaconsDatabase
    ): MergePreviewResult = withContext(Dispatchers.IO) {
        val currentMembers = database.memberDao().getAllMembersDirect()
        val currentMemberNames = currentMembers.map { it.fullName.trim() }.toSet()
        val currentMemberPhones = currentMembers.mapNotNull { it.phone.ifBlank { null } }.toSet()

        var newMembers = 0
        var existingMembers = 0

        backupData.members.forEach { bm: Member ->
            val isExisting = bm.fullName.trim() in currentMemberNames || (bm.phone.isNotBlank() && bm.phone in currentMemberPhones)
            if (isExisting) {
                existingMembers++
            } else {
                newMembers++
            }
        }

        MergePreviewResult(
            backupDate = backupData.exportDate,
            totalBackupMembers = backupData.members.size,
            newMembersCount = newMembers,
            existingMembersCount = existingMembers,
            attendancesCount = backupData.attendances.size,
            hymnsCount = backupData.hymns.size,
            assessmentsCount = backupData.hymnAssessments.size + backupData.bibleAssessments.size,
            examsCount = backupData.exams.size,
            visitsCount = backupData.visitRecords.size
        )
    }

    suspend fun executeMerge(
        backupData: BackupData,
        database: DeaconsDatabase
    ): MergeExecutionResult = withContext(Dispatchers.IO) {
        val currentMembers = database.memberDao().getAllMembersDirect()
        val memberMapByName = currentMembers.associateBy { it.fullName.trim() }.toMutableMap()
        val memberMapByPhone = currentMembers.filter { it.phone.isNotBlank() }.associateBy { it.phone.trim() }.toMutableMap()

        var insertedMembersCount = 0
        var updatedMembersCount = 0

        // Old ID to New/Actual Database ID mapping
        val idMapping = mutableMapOf<Long, Long>()

        // 1. Merge Members
        backupData.members.forEach { bm: Member ->
            val existing = memberMapByName[bm.fullName.trim()] ?: (if (bm.phone.isNotBlank()) memberMapByPhone[bm.phone.trim()] else null)
            if (existing != null) {
                idMapping[bm.id] = existing.id
                // Update empty fields
                val updated = existing.copy(
                    phone = if (existing.phone.isBlank()) bm.phone else existing.phone,
                    parentPhone = if (existing.parentPhone.isBlank()) bm.parentPhone else existing.parentPhone,
                    street = if (existing.street.isBlank()) bm.street else existing.street,
                    houseNumber = if (existing.houseNumber.isBlank()) bm.houseNumber else existing.houseNumber,
                    addressDetails = if (existing.addressDetails.isBlank()) bm.addressDetails else existing.addressDetails,
                    schoolClass = if (existing.schoolClass.isBlank()) bm.schoolClass else existing.schoolClass,
                    notes = if (existing.notes.isBlank()) bm.notes else existing.notes
                )
                if (updated != existing) {
                    database.memberDao().updateMember(updated)
                    updatedMembersCount++
                }
            } else {
                val newId = database.memberDao().insertMember(
                    bm.copy(id = 0L)
                )
                idMapping[bm.id] = newId
                insertedMembersCount++
            }
        }

        // 2. Merge School Classes
        val existingClasses = database.schoolClassDao().getAllSchoolClassesDirect().map { it.name.trim() }.toSet()
        backupData.schoolClasses.forEach { sc: SchoolClass ->
            if (sc.name.trim() !in existingClasses) {
                database.schoolClassDao().insertSchoolClass(sc.copy(id = 0L))
            }
        }

        // 3. Merge Hymns
        val existingHymns = database.hymnDao().getAllHymnsDirect().associateBy { it.name.trim() }
        val hymnIdMapping = mutableMapOf<Long, Long>()
        backupData.hymns.forEach { h: Hymn ->
            val exHymn = existingHymns[h.name.trim()]
            if (exHymn != null) {
                hymnIdMapping[h.id] = exHymn.id
            } else {
                val newId = database.hymnDao().insertHymn(h.copy(id = 0L))
                hymnIdMapping[h.id] = newId
            }
        }

        // 4. Merge Bible Lessons
        val existingBibleLessons = database.bibleLessonDao().getAllBibleLessonsDirect().associateBy { it.name.trim() }
        val bibleLessonIdMapping = mutableMapOf<Long, Long>()
        backupData.bibleLessons.forEach { bl: BibleLesson ->
            val exLesson = existingBibleLessons[bl.name.trim()]
            if (exLesson != null) {
                bibleLessonIdMapping[bl.id] = exLesson.id
            } else {
                val newId = database.bibleLessonDao().insertBibleLesson(bl.copy(id = 0L))
                bibleLessonIdMapping[bl.id] = newId
            }
        }

        // 5. Merge Exams
        val existingExams = database.examDao().getAllExamsDirect().associateBy { it.name.trim() }
        val examIdMapping = mutableMapOf<Long, Long>()
        backupData.exams.forEach { e: Exam ->
            val exExam = existingExams[e.name.trim()]
            if (exExam != null) {
                examIdMapping[e.id] = exExam.id
            } else {
                val newId = database.examDao().insertExam(e.copy(id = 0L))
                examIdMapping[e.id] = newId
            }
        }

        // 6. Merge Attendances
        var insertedAttCount = 0
        val currentAtts = database.attendanceDao().getAllAttendancesDirect()
        val attKeySet = currentAtts.map { "${it.memberId}_${it.date}_${it.type.name}" }.toSet()

        backupData.attendances.forEach { att: Attendance ->
            val actualMemberId = idMapping[att.memberId]
            if (actualMemberId != null) {
                val key = "${actualMemberId}_${att.date}_${att.type.name}"
                if (key !in attKeySet) {
                    database.attendanceDao().insertAttendance(
                        att.copy(id = 0L, memberId = actualMemberId)
                    )
                    insertedAttCount++
                }
            }
        }

        // 7. Merge Hymn Assessments
        var insertedAssessmentsCount = 0
        val currentHAs = database.hymnAssessmentDao().getAllHymnAssessmentsDirect()
        val haKeySet = currentHAs.map { "${it.memberId}_${it.hymnId}" }.toSet()

        backupData.hymnAssessments.forEach { ha: HymnAssessment ->
            val actualMemberId = idMapping[ha.memberId]
            val actualHymnId = hymnIdMapping[ha.hymnId]
            if (actualMemberId != null && actualHymnId != null) {
                val key = "${actualMemberId}_${actualHymnId}"
                if (key !in haKeySet) {
                    database.hymnAssessmentDao().insertHymnAssessment(
                        ha.copy(id = 0L, memberId = actualMemberId, hymnId = actualHymnId)
                    )
                    insertedAssessmentsCount++
                }
            }
        }

        // 8. Merge Bible Assessments
        backupData.bibleAssessments.forEach { ba: BibleAssessment ->
            val actualMemberId = idMapping[ba.memberId]
            val actualLessonId = ba.lessonId?.let { bibleLessonIdMapping[it] }
            if (actualMemberId != null) {
                database.bibleAssessmentDao().insertBibleAssessment(
                    ba.copy(id = 0L, memberId = actualMemberId, lessonId = actualLessonId)
                )
                insertedAssessmentsCount++
            }
        }

        // 9. Merge Exam Results
        val currentExamResults = database.examDao().getAllExamResultsDirect()
        val erKeySet = currentExamResults.map { "${it.memberId}_${it.examId}" }.toSet()

        backupData.examResults.forEach { er: ExamResult ->
            val actualMemberId = idMapping[er.memberId]
            val actualExamId = examIdMapping[er.examId]
            if (actualMemberId != null && actualExamId != null) {
                val key = "${actualMemberId}_${actualExamId}"
                if (key !in erKeySet) {
                    database.examDao().insertExamResult(
                        er.copy(id = 0L, memberId = actualMemberId, examId = actualExamId)
                    )
                    insertedAssessmentsCount++
                }
            }
        }

        // 10. Merge Visits
        var insertedVisitsCount = 0
        backupData.visitRecords.forEach { vr: VisitRecord ->
            val actualMemberId = idMapping[vr.memberId]
            if (actualMemberId != null) {
                database.visitRecordDao().insertVisitRecord(
                    vr.copy(id = 0L, memberId = actualMemberId)
                )
                insertedVisitsCount++
            }
        }

        MergeExecutionResult(
            insertedMembers = insertedMembersCount,
            updatedMembers = updatedMembersCount,
            insertedAttendances = insertedAttCount,
            insertedAssessments = insertedAssessmentsCount,
            insertedVisits = insertedVisitsCount,
            message = "تم دمج النسخة الاحتياطية بنجاح: إضافة $insertedMembersCount مخدوم جديد، تحديث $updatedMembersCount مخدوم، وإضافة $insertedAttCount حضور و $insertedAssessmentsCount تقييم."
        )
    }
}
