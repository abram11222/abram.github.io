package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE isActive = 1 ORDER BY fullName ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members ORDER BY fullName ASC")
    fun getAllMembersIncludingInactive(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    fun getMemberById(id: Long): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberByIdDirect(id: Long): Member?

    @Query("SELECT * FROM members WHERE groupId = :groupId AND isActive = 1 ORDER BY fullName ASC")
    fun getMembersByGroup(groupId: Long): Flow<List<Member>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Member>)

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("SELECT * FROM members WHERE isActive = 1 ORDER BY fullName ASC")
    suspend fun getAllMembersDirect(): List<Member>

    @Query("SELECT * FROM members ORDER BY fullName ASC")
    suspend fun getAllMembersIncludingInactiveDirect(): List<Member>

    @Query("SELECT * FROM members WHERE fullName = :name AND phone = :phone LIMIT 1")
    suspend fun findMemberByNameAndPhone(name: String, phone: String): Member?

    @Query("SELECT COUNT(*) FROM members WHERE isActive = 1")
    fun getMembersCount(): Flow<Int>

    @Query("DELETE FROM members")
    suspend fun clearAllMembers()
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE isActive = 1 ORDER BY name ASC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM groups WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllGroupsDirect(): List<Group>

    @Query("SELECT * FROM groups WHERE name = :name LIMIT 1")
    suspend fun findGroupByName(name: String): Group?

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    fun getGroupById(id: Long): Flow<Group?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<Group>)

    @Update
    suspend fun updateGroup(group: Group)

    @Delete
    suspend fun deleteGroup(group: Group)
}

@Dao
interface ServantDao {
    @Query("SELECT * FROM servants WHERE isActive = 1 ORDER BY name ASC")
    fun getAllServants(): Flow<List<Servant>>

    @Query("SELECT * FROM servants WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllServantsDirect(): List<Servant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServant(servant: Servant): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServants(servants: List<Servant>)

    @Update
    suspend fun updateServant(servant: Servant)

    @Delete
    suspend fun deleteServant(servant: Servant)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendances ORDER BY date DESC")
    fun getAllAttendances(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances ORDER BY date DESC")
    suspend fun getAllAttendancesDirect(): List<Attendance>

    @Query("SELECT * FROM attendances WHERE memberId = :memberId ORDER BY date DESC")
    fun getAttendancesForMember(memberId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE date = :date AND type = :type")
    fun getAttendancesByDateAndType(date: String, type: AttendanceType): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE date >= :startDate AND date <= :endDate")
    fun getAttendancesInRange(startDate: String, endDate: String): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<Attendance>)

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Delete
    suspend fun deleteAttendance(attendance: Attendance)

    @Query("SELECT * FROM attendances WHERE memberId = :memberId AND date = :date AND type = :type AND (:massNumber IS NULL OR massNumber = :massNumber) LIMIT 1")
    suspend fun findExistingAttendance(memberId: Long, date: String, type: AttendanceType, massNumber: Int?): Attendance?

    @Query("DELETE FROM attendances WHERE memberId = :memberId AND date = :date AND type = :type")
    suspend fun deleteSpecificAttendance(memberId: Long, date: String, type: AttendanceType)

    @Query("DELETE FROM attendances WHERE memberId = :memberId AND date = :date AND type = :type AND (:massNumber IS NULL OR massNumber = :massNumber)")
    suspend fun deleteAttendanceForMemberAndDate(memberId: Long, date: String, type: AttendanceType, massNumber: Int?)

    @Query("DELETE FROM attendances WHERE memberId = :memberId")
    suspend fun deleteAttendancesForMember(memberId: Long)
}

@Dao
interface HymnDao {
    @Query("SELECT * FROM hymns WHERE isActive = 1 ORDER BY name ASC")
    fun getAllHymns(): Flow<List<Hymn>>

    @Query("SELECT * FROM hymns ORDER BY name ASC")
    fun getAllHymnsIncludingInactive(): Flow<List<Hymn>>

    @Query("SELECT * FROM hymns WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllHymnsDirect(): List<Hymn>

    @Query("SELECT * FROM hymns ORDER BY name ASC")
    suspend fun getAllHymnsIncludingInactiveDirect(): List<Hymn>

    @Query("SELECT * FROM hymns WHERE id = :id LIMIT 1")
    fun getHymnById(id: Long): Flow<Hymn?>

    @Query("SELECT COUNT(*) FROM hymn_assessments WHERE hymnId = :hymnId")
    suspend fun countAssessmentsForHymn(hymnId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHymn(hymn: Hymn): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHymns(hymns: List<Hymn>)

    @Update
    suspend fun updateHymn(hymn: Hymn)

    @Delete
    suspend fun deleteHymn(hymn: Hymn)
}

@Dao
interface HymnAssessmentDao {
    @Query("SELECT * FROM hymn_assessments ORDER BY date DESC")
    fun getAllHymnAssessments(): Flow<List<HymnAssessment>>

    @Query("SELECT * FROM hymn_assessments ORDER BY date DESC")
    suspend fun getAllHymnAssessmentsDirect(): List<HymnAssessment>

    @Query("SELECT * FROM hymn_assessments WHERE memberId = :memberId ORDER BY date DESC")
    fun getHymnAssessmentsForMember(memberId: Long): Flow<List<HymnAssessment>>

    @Query("SELECT * FROM hymn_assessments WHERE hymnId = :hymnId")
    fun getAssessmentsForHymn(hymnId: Long): Flow<List<HymnAssessment>>

    @Query("SELECT * FROM hymn_assessments WHERE memberId = :memberId AND hymnId = :hymnId LIMIT 1")
    suspend fun getAssessmentForMemberAndHymn(memberId: Long, hymnId: Long): HymnAssessment?

    @Query("SELECT * FROM hymn_assessments WHERE memberId = :memberId AND hymnId = :hymnId LIMIT 1")
    fun getAssessmentForMemberAndHymnFlow(memberId: Long, hymnId: Long): Flow<HymnAssessment?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHymnAssessment(assessment: HymnAssessment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHymnAssessments(assessments: List<HymnAssessment>)

    @Update
    suspend fun updateHymnAssessment(assessment: HymnAssessment)

    @Delete
    suspend fun deleteHymnAssessment(assessment: HymnAssessment)

    @Query("DELETE FROM hymn_assessments WHERE memberId = :memberId")
    suspend fun deleteHymnAssessmentsForMember(memberId: Long)

    @Query("DELETE FROM hymn_assessments WHERE hymnId = :hymnId")
    suspend fun deleteHymnAssessmentsForHymn(hymnId: Long)
}

@Dao
interface BibleLessonDao {
    @Query("SELECT * FROM bible_lessons WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveBibleLessons(): Flow<List<BibleLesson>>

    @Query("SELECT * FROM bible_lessons ORDER BY name ASC")
    fun getAllBibleLessons(): Flow<List<BibleLesson>>

    @Query("SELECT * FROM bible_lessons ORDER BY name ASC")
    suspend fun getAllBibleLessonsDirect(): List<BibleLesson>

    @Query("SELECT * FROM bible_lessons WHERE id = :id LIMIT 1")
    fun getBibleLessonById(id: Long): Flow<BibleLesson?>

    @Query("SELECT COUNT(*) FROM bible_assessments WHERE lessonName = :lessonName")
    suspend fun countAssessmentsForLesson(lessonName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBibleLesson(lesson: BibleLesson): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBibleLessons(lessons: List<BibleLesson>)

    @Update
    suspend fun updateBibleLesson(lesson: BibleLesson)

    @Delete
    suspend fun deleteBibleLesson(lesson: BibleLesson)
}

@Dao
interface BibleAssessmentDao {
    @Query("SELECT * FROM bible_assessments ORDER BY date DESC")
    fun getAllBibleAssessments(): Flow<List<BibleAssessment>>

    @Query("SELECT * FROM bible_assessments ORDER BY date DESC")
    suspend fun getAllBibleAssessmentsDirect(): List<BibleAssessment>

    @Query("SELECT * FROM bible_assessments WHERE memberId = :memberId ORDER BY date DESC")
    fun getBibleAssessmentsForMember(memberId: Long): Flow<List<BibleAssessment>>

    @Query("SELECT * FROM bible_assessments WHERE memberId = :memberId AND lessonName = :lessonName LIMIT 1")
    suspend fun getAssessmentForMemberAndLesson(memberId: Long, lessonName: String): BibleAssessment?

    @Query("SELECT COUNT(*) FROM bible_assessments WHERE lessonName = :lessonName")
    suspend fun countAssessmentsForLesson(lessonName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBibleAssessment(assessment: BibleAssessment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBibleAssessments(assessments: List<BibleAssessment>)

    @Update
    suspend fun updateBibleAssessment(assessment: BibleAssessment)

    @Delete
    suspend fun deleteBibleAssessment(assessment: BibleAssessment)

    @Query("DELETE FROM bible_assessments WHERE memberId = :memberId")
    suspend fun deleteBibleAssessmentsForMember(memberId: Long)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE isActive = 1 ORDER BY date DESC, id DESC")
    fun getActiveExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY date DESC, id DESC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY date DESC, id DESC")
    suspend fun getAllExamsDirect(): List<Exam>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    fun getExamById(id: Long): Flow<Exam?>

    @Query("SELECT COUNT(*) FROM exam_results WHERE examId = :examId")
    suspend fun countResultsForExam(examId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<Exam>)

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)

    @Query("SELECT * FROM exam_results")
    fun getAllExamResults(): Flow<List<ExamResult>>

    @Query("SELECT * FROM exam_results")
    suspend fun getAllExamResultsDirect(): List<ExamResult>

    @Query("SELECT * FROM exam_results WHERE memberId = :memberId")
    fun getExamResultsForMember(memberId: Long): Flow<List<ExamResult>>

    @Query("SELECT * FROM exam_results WHERE examId = :examId")
    fun getExamResultsForExam(examId: Long): Flow<List<ExamResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamResult(result: ExamResult): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamResults(results: List<ExamResult>)

    @Update
    suspend fun updateExamResult(result: ExamResult)

    @Delete
    suspend fun deleteExamResult(result: ExamResult)

    @Query("DELETE FROM exam_results WHERE memberId = :memberId")
    suspend fun deleteExamResultsForMember(memberId: Long)

    @Query("DELETE FROM exam_results WHERE examId = :examId")
    suspend fun deleteExamResultsForExam(examId: Long)
}

@Dao
interface EvaluationPeriodDao {
    @Query("SELECT * FROM evaluation_periods WHERE isActive = 1 ORDER BY startDate DESC")
    fun getAllPeriods(): Flow<List<EvaluationPeriod>>

    @Query("SELECT * FROM evaluation_periods WHERE isActive = 1 ORDER BY startDate DESC")
    suspend fun getAllPeriodsDirect(): List<EvaluationPeriod>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: EvaluationPeriod): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriods(periods: List<EvaluationPeriod>)

    @Update
    suspend fun updatePeriod(period: EvaluationPeriod)

    @Delete
    suspend fun deletePeriod(period: EvaluationPeriod)
}

@Dao
interface SchoolClassDao {
    @Query("SELECT * FROM school_classes WHERE isActive = 1 ORDER BY orderIndex ASC, id ASC")
    fun getAllSchoolClasses(): Flow<List<SchoolClass>>

    @Query("SELECT * FROM school_classes WHERE isActive = 1 ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllSchoolClassesDirect(): List<SchoolClass>

    @Query("SELECT * FROM school_classes WHERE name = :name LIMIT 1")
    suspend fun findSchoolClassByName(name: String): SchoolClass?

    @Query("SELECT * FROM school_classes WHERE id = :id LIMIT 1")
    fun getSchoolClassById(id: Long): Flow<SchoolClass?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolClass(schoolClass: SchoolClass): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolClasses(schoolClasses: List<SchoolClass>)

    @Update
    suspend fun updateSchoolClass(schoolClass: SchoolClass)

    @Delete
    suspend fun deleteSchoolClass(schoolClass: SchoolClass)
}

@Dao
interface VisitRecordDao {
    @Query("SELECT * FROM visit_records ORDER BY visitDate DESC, id DESC")
    fun getAllVisitRecords(): Flow<List<VisitRecord>>

    @Query("SELECT * FROM visit_records ORDER BY visitDate DESC, id DESC")
    suspend fun getAllVisitRecordsDirect(): List<VisitRecord>

    @Query("SELECT * FROM visit_records WHERE memberId = :memberId ORDER BY visitDate DESC, id DESC")
    fun getVisitsForMember(memberId: Long): Flow<List<VisitRecord>>

    @Query("SELECT * FROM visit_records WHERE memberId = :memberId ORDER BY visitDate DESC, id DESC")
    suspend fun getVisitsForMemberDirect(memberId: Long): List<VisitRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitRecord(visitRecord: VisitRecord): Long

    @Update
    suspend fun updateVisitRecord(visitRecord: VisitRecord)

    @Delete
    suspend fun deleteVisitRecord(visitRecord: VisitRecord)

    @Query("DELETE FROM visit_records WHERE memberId = :memberId")
    suspend fun deleteVisitsForMember(memberId: Long)
}

@Dao
interface MemberBarcodeDao {
    @Query("SELECT * FROM member_barcodes WHERE memberId = :memberId LIMIT 1")
    fun getBarcodeForMember(memberId: Long): Flow<MemberBarcode?>

    @Query("SELECT * FROM member_barcodes WHERE memberId = :memberId LIMIT 1")
    suspend fun getBarcodeForMemberSync(memberId: Long): MemberBarcode?

    @Query("SELECT * FROM member_barcodes WHERE barcodeValue = :barcodeValue LIMIT 1")
    suspend fun getBarcodeByValue(barcodeValue: String): MemberBarcode?

    @Query("SELECT * FROM member_barcodes")
    fun getAllBarcodes(): Flow<List<MemberBarcode>>

    @Query("SELECT * FROM member_barcodes")
    suspend fun getAllBarcodesSync(): List<MemberBarcode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: MemberBarcode): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBarcodes(barcodes: List<MemberBarcode>)

    @Update
    suspend fun updateBarcode(barcode: MemberBarcode)

    @Delete
    suspend fun deleteBarcode(barcode: MemberBarcode)

    @Query("DELETE FROM member_barcodes WHERE memberId = :memberId")
    suspend fun deleteBarcodeByMemberId(memberId: Long)
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<AppSetting?>

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingDirect(key: String): AppSetting?

    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsDirect(): List<AppSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSetting)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSettings(settings: List<AppSetting>)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}

