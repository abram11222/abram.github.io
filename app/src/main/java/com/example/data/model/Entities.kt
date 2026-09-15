package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AttendanceType {
    SERVICE,
    MASS
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    EXCUSED
}

@Entity(tableName = "school_classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val orderIndex: Int = 0,
    val isActive: Boolean = true
)

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val profileImage: String? = null,
    val schoolClass: String = "",
    val birthDate: String = "",
    val phone: String = "",
    val parentPhone: String = "",
    val governorate: String = "أسيوط",
    val center: String = "القوصية",
    val area: String = "مير",
    val street: String = "",
    val houseNumber: String = "",
    val addressDetails: String = "",
    val groupId: Long? = null,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "member_barcodes",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["memberId"], unique = true),
        androidx.room.Index(value = ["barcodeValue"], unique = true)
    ]
)
data class MemberBarcode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val barcodeValue: String, // e.g. "DCN-000042" (Code 128 format derived stably from Member ID)
    val barcodeFormat: String = "CODE_128",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "visit_records",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["memberId"])
    ]
)
data class VisitRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val visitDate: String, // YYYY-MM-DD
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "servants")
data class Servant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val role: String = "خادم",
    val isActive: Boolean = true
)

@Entity(
    tableName = "attendances",
    indices = [
        androidx.room.Index(value = ["memberId", "date", "type", "massNumber"], unique = true)
    ]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val date: String, // Format: YYYY-MM-DD
    val type: AttendanceType,
    val massNumber: Int? = null, // 1 or 2 for Mass in month
    val status: AttendanceStatus,
    val notes: String = "" // Holds excuse reason when status is EXCUSED
)

@Entity(tableName = "hymns")
data class Hymn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val maxScore: Double = 10.0,
    val category: String = "عام",
    val durationInDays: Int = 30, // مدة التسميع بالأيام
    val deadline: String = "", // YYYY-MM-DD
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(
    tableName = "hymn_assessments",
    indices = [
        androidx.room.Index(value = ["memberId", "hymnId"], unique = true)
    ]
)
data class HymnAssessment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val hymnId: Long,
    val date: String,
    val score: Double,
    val maxScore: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bible_lessons")
data class BibleLesson(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // اسم الدرس / الإصحاح
    val durationInDays: Int = 14, // مدة الإنجيل بالأيام
    val deadline: String = "", // YYYY-MM-DD
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bible_assessments",
    indices = [
        androidx.room.Index(value = ["memberId", "lessonName"])
    ]
)
data class BibleAssessment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val date: String,
    val lessonName: String,
    val score: Double,
    val maxScore: Double = 10.0,
    val notes: String = "",
    val lessonId: Long? = null
)

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val date: String = "",
    val maxScore: Double = 100.0,
    val durationInDays: Int = 7, // مدة الامتحان بالأيام
    val deadline: String = "", // YYYY-MM-DD
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "exam_results")
data class ExamResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examId: Long,
    val memberId: Long,
    val score: Double,
    val maxScore: Double = 100.0,
    val date: String = "",
    val notes: String = ""
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey
    val key: String,
    val value: String
)

@Entity(tableName = "evaluation_periods")
data class EvaluationPeriod(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startDate: String,
    val endDate: String,
    val requiredMassesCount: Int = 2,
    val notes: String = "",
    val isActive: Boolean = true
)
