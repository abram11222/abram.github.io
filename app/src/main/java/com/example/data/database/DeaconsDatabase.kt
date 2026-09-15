package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppSettingDao
import com.example.data.dao.AttendanceDao
import com.example.data.dao.BibleAssessmentDao
import com.example.data.dao.BibleLessonDao
import com.example.data.dao.EvaluationPeriodDao
import com.example.data.dao.ExamDao
import com.example.data.dao.GroupDao
import com.example.data.dao.HymnAssessmentDao
import com.example.data.dao.HymnDao
import com.example.data.dao.MemberBarcodeDao
import com.example.data.dao.MemberDao
import com.example.data.dao.SchoolClassDao
import com.example.data.dao.ServantDao
import com.example.data.dao.VisitRecordDao
import com.example.data.model.AppSetting
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromAttendanceType(value: AttendanceType): String = value.name

    @TypeConverter
    fun toAttendanceType(value: String): AttendanceType = AttendanceType.valueOf(value)

    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus): String = value.name

    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)
}

@Database(
    entities = [
        Member::class,
        MemberBarcode::class,
        Group::class,
        Servant::class,
        Attendance::class,
        Hymn::class,
        HymnAssessment::class,
        BibleLesson::class,
        BibleAssessment::class,
        Exam::class,
        ExamResult::class,
        EvaluationPeriod::class,
        SchoolClass::class,
        VisitRecord::class,
        AppSetting::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DeaconsDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun memberBarcodeDao(): MemberBarcodeDao
    abstract fun groupDao(): GroupDao
    abstract fun servantDao(): ServantDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun hymnDao(): HymnDao
    abstract fun hymnAssessmentDao(): HymnAssessmentDao
    abstract fun bibleLessonDao(): BibleLessonDao
    abstract fun bibleAssessmentDao(): BibleAssessmentDao
    abstract fun examDao(): ExamDao
    abstract fun evaluationPeriodDao(): EvaluationPeriodDao
    abstract fun schoolClassDao(): SchoolClassDao
    abstract fun visitRecordDao(): VisitRecordDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: DeaconsDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DeaconsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeaconsDatabase::class.java,
                    "deacons_service_database.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CLASSES = listOf(
            "أولى ابتدائي",
            "ثانية ابتدائي",
            "ثالثة ابتدائي",
            "رابعة ابتدائي",
            "خامسة ابتدائي",
            "سادسة ابتدائي",
            "أولى إعدادي",
            "ثانية إعدادي",
            "ثالثة إعدادي",
            "أولى ثانوي",
            "ثانية ثانوي",
            "ثالثة ثانوي"
        )

        suspend fun populateInitialData(db: DeaconsDatabase) {
            val existingClasses = db.schoolClassDao().getAllSchoolClassesDirect()
            if (existingClasses.isEmpty()) {
                DEFAULT_CLASSES.forEachIndexed { index, className ->
                    db.schoolClassDao().insertSchoolClass(
                        SchoolClass(
                            name = className,
                            orderIndex = index,
                            isActive = true
                        )
                    )
                }
            }
        }
    }
}
