package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.DeaconsDatabase
import com.example.data.model.AppSetting
import com.example.util.NotificationHelper
import com.example.util.OverdueCalculator
import com.example.util.OverdueGraceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext

class OverdueCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = DeaconsDatabase.getDatabase(applicationContext, GlobalScope)
            val settingDao = db.appSettingDao()

            // 1. Fetch persisted settings
            val attDays = settingDao.getSettingDirect(OverdueGraceSettings.KEY_ATTENDANCE_DAYS)?.value?.toIntOrNull()
                ?: OverdueGraceSettings.DEFAULT.attendanceAbsenceDays
            val hymnDays = settingDao.getSettingDirect(OverdueGraceSettings.KEY_HYMN_DAYS)?.value?.toIntOrNull()
                ?: OverdueGraceSettings.DEFAULT.hymnOverdueDays
            val bibleDays = settingDao.getSettingDirect(OverdueGraceSettings.KEY_BIBLE_DAYS)?.value?.toIntOrNull()
                ?: OverdueGraceSettings.DEFAULT.bibleOverdueDays
            val examDays = settingDao.getSettingDirect(OverdueGraceSettings.KEY_EXAM_DAYS)?.value?.toIntOrNull()
                ?: OverdueGraceSettings.DEFAULT.examOverdueDays
            val notifEnabled = settingDao.getSettingDirect(OverdueGraceSettings.KEY_NOTIFICATIONS_ENABLED)?.value?.toBooleanStrictOrNull()
                ?: OverdueGraceSettings.DEFAULT.notificationsEnabled

            val settings = OverdueGraceSettings(
                attendanceAbsenceDays = attDays,
                hymnOverdueDays = hymnDays,
                bibleOverdueDays = bibleDays,
                examOverdueDays = examDays,
                notificationsEnabled = notifEnabled
            )

            // 2. Fetch data directly from Room DB
            val members = db.memberDao().getAllMembersDirect()
            val attendances = db.attendanceDao().getAllAttendancesDirect()
            val hymns = db.hymnDao().getAllHymnsDirect()
            val hymnAssessments = db.hymnAssessmentDao().getAllHymnAssessmentsDirect()
            val bibleLessons = db.bibleLessonDao().getAllBibleLessonsDirect()
            val bibleAssessments = db.bibleAssessmentDao().getAllBibleAssessmentsDirect()
            val exams = db.examDao().getAllExamsDirect()
            val examResults = db.examDao().getAllExamResultsDirect()

            // 3. Compute overdue statuses
            val overdueList = OverdueCalculator.calculateOverdueForMembers(
                members = members,
                attendances = attendances,
                hymns = hymns,
                hymnAssessments = hymnAssessments,
                bibleLessons = bibleLessons,
                bibleAssessments = bibleAssessments,
                exams = exams,
                examResults = examResults,
                settings = settings
            )

            val overdueMembers = overdueList.filter { it.hasAnyOverdue }

            // 4. Dispatch notifications if enabled and there are overdue members
            if (settings.notificationsEnabled && overdueMembers.isNotEmpty()) {
                NotificationHelper.showSummaryOverdueNotification(
                    context = applicationContext,
                    overdueMembers = overdueMembers
                )
            }

            // 5. Update timestamp
            settingDao.setSetting(
                AppSetting(
                    key = OverdueGraceSettings.KEY_LAST_CHECK_TIMESTAMP,
                    value = System.currentTimeMillis().toString()
                )
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
