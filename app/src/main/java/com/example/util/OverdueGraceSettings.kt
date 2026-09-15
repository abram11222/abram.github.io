package com.example.util

data class OverdueGraceSettings(
    val attendanceAbsenceDays: Int = 14,
    val hymnOverdueDays: Int = 30,
    val bibleOverdueDays: Int = 14,
    val examOverdueDays: Int = 7,
    val notificationsEnabled: Boolean = true
) {
    companion object {
        const val KEY_ATTENDANCE_DAYS = "grace_attendance_days"
        const val KEY_HYMN_DAYS = "grace_hymn_days"
        const val KEY_BIBLE_DAYS = "grace_bible_days"
        const val KEY_EXAM_DAYS = "grace_exam_days"
        const val KEY_NOTIFICATIONS_ENABLED = "grace_notifications_enabled"
        const val KEY_LAST_CHECK_TIMESTAMP = "last_overdue_check_timestamp"

        val DEFAULT = OverdueGraceSettings()
    }
}
