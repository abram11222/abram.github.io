package com.example.util

import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatus
import com.example.data.model.BibleAssessment
import com.example.data.model.BibleLesson
import com.example.data.model.Exam
import com.example.data.model.ExamResult
import com.example.data.model.Hymn
import com.example.data.model.HymnAssessment
import com.example.data.model.Member
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MemberOverdueInfo(
    val member: Member,
    val daysSinceLastAttendance: Int,
    val isAttendanceOverdue: Boolean,
    val overdueHymns: List<Hymn>,
    val overdueBibleLessons: List<BibleLesson>,
    val overdueExams: List<Exam>,
    val hasAnyOverdue: Boolean,
    val summaryReasons: List<String>,
    val primaryReason: String
)

object OverdueCalculator {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun calculateOverdueForMembers(
        members: List<Member>,
        attendances: List<Attendance>,
        hymns: List<Hymn>,
        hymnAssessments: List<HymnAssessment>,
        bibleLessons: List<BibleLesson>,
        bibleAssessments: List<BibleAssessment>,
        exams: List<Exam>,
        examResults: List<ExamResult>,
        settings: OverdueGraceSettings,
        currentDate: Date = Date()
    ): List<MemberOverdueInfo> {
        val nowMillis = currentDate.time
        val activeMembers = members.filter { it.isActive }
        val activeHymns = hymns.filter { it.isActive }
        val activeBibleLessons = bibleLessons.filter { it.isActive }
        val activeExams = exams.filter { it.isActive }

        // Group attendances by member
        val presentAttendancesByMember = attendances
            .filter { it.status == AttendanceStatus.PRESENT }
            .groupBy { it.memberId }

        val hymnAssessmentsByMember = hymnAssessments.groupBy { it.memberId }
            .mapValues { entry -> entry.value.map { it.hymnId }.toSet() }

        val bibleAssessmentsByMember = bibleAssessments.groupBy { it.memberId }
            .mapValues { entry ->
                val byId = entry.value.mapNotNull { it.lessonId }.toSet()
                val byName = entry.value.map { it.lessonName.trim() }.toSet()
                Pair(byId, byName)
            }

        val examResultsByMember = examResults.groupBy { it.memberId }
            .mapValues { entry -> entry.value.map { it.examId }.toSet() }

        return activeMembers.map { member ->
            val reasons = mutableListOf<String>()

            // 1. Attendance Overdue Check
            val memberPresents = presentAttendancesByMember[member.id]
            val latestDateStr = memberPresents?.maxByOrNull { it.date }?.date
            val daysSinceLastAttendance: Int = if (latestDateStr != null) {
                try {
                    val date = dateFormatter.parse(latestDateStr)
                    if (date != null) {
                        val diff = nowMillis - date.time
                        (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                    } else {
                        val diff = nowMillis - member.createdAt
                        (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                    }
                } catch (_: Exception) {
                    0
                }
            } else {
                // Never attended: calculate from member creation date
                val diff = nowMillis - member.createdAt
                (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            }

            val isAttendanceOverdue = daysSinceLastAttendance >= settings.attendanceAbsenceDays
            if (isAttendanceOverdue) {
                if (latestDateStr != null) {
                    reasons.add("غائب منذ $daysSinceLastAttendance يوم (آخر حضور: $latestDateStr)")
                } else {
                    reasons.add("لم يسجل أي حضور منذ $daysSinceLastAttendance يوم")
                }
            }

            // 2. Hymns Overdue Check
            val assessedHymnIds = hymnAssessmentsByMember[member.id] ?: emptySet()
            val overdueHymns = activeHymns.filter { hymn ->
                if (hymn.id in assessedHymnIds) return@filter false
                if (hymn.deadline.isNotBlank()) {
                    try {
                        val deadlineDate = dateFormatter.parse(hymn.deadline)
                        if (deadlineDate != null && nowMillis > deadlineDate.time) {
                            return@filter true
                        }
                    } catch (_: Exception) {}
                }
                val duration = if (hymn.durationInDays > 0) hymn.durationInDays else settings.hymnOverdueDays
                duration <= settings.hymnOverdueDays
            }

            if (overdueHymns.isNotEmpty()) {
                val hymnNames = overdueHymns.take(2).joinToString("، ") { it.name }
                val extra = if (overdueHymns.size > 2) " و${overdueHymns.size - 2} آخرين" else ""
                reasons.add("تأخر في تسميع ألحان: $hymnNames$extra (حد السماح: ${settings.hymnOverdueDays} يوم)")
            }

            // 3. Bible Lessons Overdue Check
            val assessedBiblePair = bibleAssessmentsByMember[member.id]
            val assessedBibleIds = assessedBiblePair?.first ?: emptySet()
            val assessedBibleNames = assessedBiblePair?.second ?: emptySet()
            val overdueBibleLessons = activeBibleLessons.filter { lesson ->
                val isAssessed = lesson.id in assessedBibleIds || lesson.name.trim() in assessedBibleNames
                if (isAssessed) return@filter false
                if (lesson.deadline.isNotBlank()) {
                    try {
                        val deadlineDate = dateFormatter.parse(lesson.deadline)
                        if (deadlineDate != null && nowMillis > deadlineDate.time) {
                            return@filter true
                        }
                    } catch (_: Exception) {}
                }
                val duration = if (lesson.durationInDays > 0) lesson.durationInDays else settings.bibleOverdueDays
                duration <= settings.bibleOverdueDays
            }

            if (overdueBibleLessons.isNotEmpty()) {
                val lessonNames = overdueBibleLessons.take(2).joinToString("، ") { it.name }
                val extra = if (overdueBibleLessons.size > 2) " و${overdueBibleLessons.size - 2} آخرين" else ""
                reasons.add("تأخر في حفظ الكتاب المقدس: $lessonNames$extra (حد السماح: ${settings.bibleOverdueDays} يوم)")
            }

            // 4. Exams Overdue Check
            val takenExamIds = examResultsByMember[member.id] ?: emptySet()
            val overdueExams = activeExams.filter { exam ->
                if (exam.id in takenExamIds) return@filter false
                if (exam.deadline.isNotBlank()) {
                    try {
                        val deadlineDate = dateFormatter.parse(exam.deadline)
                        if (deadlineDate != null && nowMillis > deadlineDate.time) {
                            return@filter true
                        }
                    } catch (_: Exception) {}
                }
                val duration = if (exam.durationInDays > 0) exam.durationInDays else settings.examOverdueDays
                duration <= settings.examOverdueDays
            }

            if (overdueExams.isNotEmpty()) {
                val examNames = overdueExams.take(2).joinToString("، ") { it.name }
                val extra = if (overdueExams.size > 2) " و${overdueExams.size - 2} آخرين" else ""
                reasons.add("تأخر في تقديم امتحانات: $examNames$extra (حد السماح: ${settings.examOverdueDays} يوم)")
            }

            val hasAnyOverdue = isAttendanceOverdue || overdueHymns.isNotEmpty() || overdueBibleLessons.isNotEmpty() || overdueExams.isNotEmpty()
            val primaryReason = reasons.firstOrNull() ?: "لا توجد متأخرات"

            MemberOverdueInfo(
                member = member,
                daysSinceLastAttendance = daysSinceLastAttendance,
                isAttendanceOverdue = isAttendanceOverdue,
                overdueHymns = overdueHymns,
                overdueBibleLessons = overdueBibleLessons,
                overdueExams = overdueExams,
                hasAnyOverdue = hasAnyOverdue,
                summaryReasons = reasons,
                primaryReason = primaryReason
            )
        }
    }
}
