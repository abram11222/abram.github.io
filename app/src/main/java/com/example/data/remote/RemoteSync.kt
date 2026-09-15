package com.example.data.remote

import androidx.room.withTransaction
import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import com.example.data.database.DeaconsDatabase
import com.example.data.model.AppSetting
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
import com.example.data.model.MemberBarcode
import com.example.data.model.SchoolClass
import com.example.data.model.Servant
import com.example.data.model.VisitRecord
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.UnknownHostException

/**
 * Bridges Room (local cache / offline-first) with Supabase (remote source of truth).
 *
 *  - [syncFromRemote]: pulls server data safely into Room.
 *  - [pushToRemote]: uploads all local data from Room to Supabase.
 *  - [smartSync]: two-way synchronization without wiping local records.
 *  - [testConnection]: tests connectivity and reports diagnostic status.
 *  - [upsert] / [delete]: mirror local writes to the server.
 */
class RemoteSync(
    private val database: DeaconsDatabase,
    private val auth: AuthManager,
    private val context: Context
) {
    private val pendingPrefs = context.getSharedPreferences("supabase_pending_sync", Context.MODE_PRIVATE)

    private val api get() = SupabaseClient.api
    private val moshi get() = SupabaseClient.moshi
    private val json = "application/json; charset=utf-8".toMediaType()

    fun getAuthBearer(): String {
        val token = auth.accessToken
        require(!token.isNullOrBlank() && token != "offline_token" && token.startsWith("eyJ")) {
            "يجب تسجيل الدخول بحساب نشط قبل مزامنة البيانات."
        }
        return "Bearer $token"
    }

    // ----------------------- CONNECTION TEST -----------------------

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseConfig.isConfigured()) {
                throw RuntimeException("لم يتم ضبط رابط مشروع Supabase بعد. يرجى إدخال الرابط والمفتاح في الإعدادات.")
            }
            val bearer = getAuthBearer()
            val url = SupabaseConfig.REST_BASE + "school_classes?select=count"
            val resp = try {
                api.getRows(url, SupabaseConfig.ANON_KEY, bearer)
            } catch (e: UnknownHostException) {
                throw RuntimeException("تعذر العثور على عنوان السيرفر (${e.message ?: "Unknown Host"}). تأكد من كتابة الرابط بشكل صحيح وأنك متصل بالإنترنت.")
            } catch (e: Exception) {
                throw RuntimeException("فشل الاتصال: ${e.localizedMessage ?: e.message}")
            }

            if (resp.isSuccessful) {
                "تم الاتصال بنجاح بالسيرفر السحابي (الجداول متوفرة وجاهزة للمزامنة)"
            } else when (resp.code()) {
                404 -> "تم الوصول للسيرفر، ولكن الجداول لم تُنشأ بعد (404). يرجى تنفيذ ملف backend/supabase_schema.sql في SQL Editor على Supabase."
                401, 403 -> "خطأ في المصادقة (${resp.code()}): مفتاح Anon Key غير صحيح أو الصلاحيات تمنع الوصول."
                else -> "خطأ من السيرفر (كود ${resp.code()}): ${resp.errorBody()?.string().orEmpty()}"
            }
        }
    }

    // ----------------------- FULL PULL (server → Room) -----------------------

    suspend fun syncFromRemote(forceReplace: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseConfig.isConfigured()) {
                throw RuntimeException("لم يتم ضبط رابط مشروع Supabase بعد. يرجى إدخال الرابط والمفتاح أولاً.")
            }
            val db = database
            // Flush offline writes before replacing Room with the server snapshot.
            flushPendingWrites().getOrThrow()
            // FETCH everything over the network FIRST (no DB transaction held).
            val groups       = fetchAll(T_GROUPS, Group::class.java)
            val servants     = fetchAll(T_SERVANTS, Servant::class.java)
            val members      = fetchAll(T_MEMBERS, Member::class.java)
            val barcodes     = fetchAll(T_BARCODES, MemberBarcode::class.java)
            val visits       = fetchAll(T_VISITS, VisitRecord::class.java)
            val attendances  = fetchAll(T_ATTENDANCES, Attendance::class.java)
            val hymns        = fetchAll(T_HYMNS, Hymn::class.java)
            val hymnAss      = fetchAll(T_HYMN_ASS, HymnAssessment::class.java)
            val lessons      = fetchAll(T_BIBLE_LESSONS, BibleLesson::class.java)
            val bibleAss     = fetchAll(T_BIBLE_ASS, BibleAssessment::class.java)
            val exams        = fetchAll(T_EXAMS, Exam::class.java)
            val examRes      = fetchAll(T_EXAM_RESULTS, ExamResult::class.java)
            val periods      = fetchAll(T_PERIODS, EvaluationPeriod::class.java)
            val classes      = fetchAll(T_CLASSES, SchoolClass::class.java)
            val settings     = fetchAll(T_SETTINGS, AppSetting::class.java)

            val localMemberCount = db.memberDao().getAllMembersIncludingInactiveDirect().size

            // Safety check: Don't wipe local database if server has 0 members and local has data
            if (members.isEmpty() && localMemberCount > 0 && !forceReplace && auth.currentUser.value?.isAdmin == true) {
                // Only an Admin may bootstrap an empty remote database from local legacy data.
                pushAllLocalToRemote().getOrThrow()
                return@runCatching "السيرفر كان فارغاً؛ تم رفع بيانات الهاتف المحلية ($localMemberCount مخدوم) إلى السيرفر بنجاح."
            }

            // Replace the local cache with exactly what the authenticated user is allowed to see.
            // This prevents stale data from remaining visible after a class assignment is revoked.
            db.withTransaction {
                db.clearAllTables()
                if (groups.isNotEmpty()) db.groupDao().insertGroups(groups)
                if (servants.isNotEmpty()) db.servantDao().insertServants(servants)
                if (members.isNotEmpty()) db.memberDao().insertMembers(members)
                if (barcodes.isNotEmpty()) db.memberBarcodeDao().insertBarcodes(barcodes)
                visits.forEach { db.visitRecordDao().insertVisitRecord(it) }
                if (attendances.isNotEmpty()) db.attendanceDao().insertAttendances(attendances)
                if (hymns.isNotEmpty()) db.hymnDao().insertHymns(hymns)
                if (hymnAss.isNotEmpty()) db.hymnAssessmentDao().insertHymnAssessments(hymnAss)
                if (lessons.isNotEmpty()) db.bibleLessonDao().insertBibleLessons(lessons)
                if (bibleAss.isNotEmpty()) db.bibleAssessmentDao().insertBibleAssessments(bibleAss)
                if (exams.isNotEmpty()) db.examDao().insertExams(exams)
                if (examRes.isNotEmpty()) db.examDao().insertExamResults(examRes)
                if (periods.isNotEmpty()) db.evaluationPeriodDao().insertPeriods(periods)
                if (classes.isNotEmpty()) db.schoolClassDao().insertSchoolClasses(classes)
                if (settings.isNotEmpty()) db.appSettingDao().setSettings(settings)
            }

            "تم سحب البيانات من السيرفر بنجاح (${members.size} مخدوم، ${attendances.size} حضور)"
        }
    }

    // ----------------------- PUSH (Room → server) -----------------------

    suspend fun pushToRemote(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            flushPendingWrites().getOrThrow()
            "تمت مزامنة التغييرات المحلية المعلقة بنجاح"
        }
    }

    /** One-time legacy migration used only when an Admin connects an empty server. */
    private suspend fun pushAllLocalToRemote(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseConfig.isConfigured()) {
                throw RuntimeException("لم يتم ضبط رابط مشروع Supabase بعد. يرجى إدخال الرابط والمفتاح أولاً.")
            }
            val db = database
            val groups = db.groupDao().getAllGroupsDirect()
            val servants = db.servantDao().getAllServantsDirect()
            val classes = db.schoolClassDao().getAllSchoolClassesDirect()
            val members = db.memberDao().getAllMembersIncludingInactiveDirect()
            val barcodes = db.memberBarcodeDao().getAllBarcodesSync()
            val visits = db.visitRecordDao().getAllVisitRecordsDirect()
            val attendances = db.attendanceDao().getAllAttendancesDirect()
            val hymns = db.hymnDao().getAllHymnsIncludingInactiveDirect()
            val hymnAss = db.hymnAssessmentDao().getAllHymnAssessmentsDirect()
            val lessons = db.bibleLessonDao().getAllBibleLessonsDirect()
            val bibleAss = db.bibleAssessmentDao().getAllBibleAssessmentsDirect()
            val exams = db.examDao().getAllExamsDirect()
            val examRes = db.examDao().getAllExamResultsDirect()
            val periods = db.evaluationPeriodDao().getAllPeriodsDirect()
            val settings = db.appSettingDao().getAllSettingsDirect()

            var pushCount = 0
            // Upsert in FK dependency order
            groups.forEach { upsert(T_GROUPS, it); pushCount++ }
            servants.forEach { upsert(T_SERVANTS, it); pushCount++ }
            classes.forEach { upsert(T_CLASSES, it); pushCount++ }
            members.forEach { upsert(T_MEMBERS, it); pushCount++ }
            barcodes.forEach { upsert(T_BARCODES, it); pushCount++ }
            visits.forEach { upsert(T_VISITS, it); pushCount++ }
            attendances.forEach { upsert(T_ATTENDANCES, it); pushCount++ }
            hymns.forEach { upsert(T_HYMNS, it); pushCount++ }
            hymnAss.forEach { upsert(T_HYMN_ASS, it); pushCount++ }
            lessons.forEach { upsert(T_BIBLE_LESSONS, it); pushCount++ }
            bibleAss.forEach { upsert(T_BIBLE_ASS, it); pushCount++ }
            exams.forEach { upsert(T_EXAMS, it); pushCount++ }
            examRes.forEach { upsert(T_EXAM_RESULTS, it); pushCount++ }
            periods.forEach { upsert(T_PERIODS, it); pushCount++ }
            settings.forEach { upsert(T_SETTINGS, it); pushCount++ }

            "تمت مزامنة بيانات الهاتف مع السيرفر السحابي بنجاح (${members.size} مخدوم، ${attendances.size} حضور)"
        }
    }

    // ----------------------- SMART SYNC (Two-Way) -----------------------

    suspend fun smartSync(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Upload only queued local modifications.
            flushPendingWrites().getOrThrow()
            // 2. Pull a fresh, permission-filtered server snapshot.
            syncFromRemote(forceReplace = false).getOrThrow()
            "تمت المزامنة الذكية بنجاح مع السيرفر السحابي"
        }
    }

    suspend fun <T> fetchAll(table: String, clazz: Class<T>): List<T> {
        val bearer = getAuthBearer()
        val url = SupabaseConfig.REST_BASE + "$table?order=id.asc"
        val resp = try {
            api.getRows(url, SupabaseConfig.ANON_KEY, bearer)
        } catch (e: UnknownHostException) {
            throw RuntimeException("تعذر الاتصال بالسيرفر. تأكد من رابط المشروع واتصال الإنترنت.")
        } catch (e: Exception) {
            throw RuntimeException("فشل الاتصال بجدول $table: ${e.localizedMessage ?: e.message}")
        }

        if (!resp.isSuccessful) {
            val errBody = resp.errorBody()?.string().orEmpty()
            val msg = when (resp.code()) {
                401 -> "خطأ مصادقة (401): المفتاح (Anon Key) غير صحيح"
                403 -> "غير مصرح (403): صلاحيات RLS تمنع الوصول لجدول $table"
                404 -> "الجدول '$table' غير موجود (404). نفّذ ملف SQL في Supabase"
                else -> "فشل جلب '$table' (كود ${resp.code()}): $errBody"
            }
            throw RuntimeException(msg)
        }
        val raw = resp.body()?.string() ?: throw RuntimeException("استجابة فارغة لجدول '$table'")
        val type = Types.newParameterizedType(MutableList::class.java, clazz)
        @Suppress("UNCHECKED_CAST")
        val adapter = moshi.adapter<Any>(type) as JsonAdapter<List<T>>
        return adapter.fromJson(raw) ?: emptyList()
    }

    // ----------------------- WRITE MIRROR (Room → server) -------------------

    suspend fun <T : Any> upsert(table: String, entity: T) {
        if (!SupabaseConfig.isConfigured()) return
        val cls = entity::class.java
        @Suppress("UNCHECKED_CAST")
        val adapter = moshi.adapter(cls) as JsonAdapter<Any>
        var jsonStr = adapter.serializeNulls().toJson(entity)

        // Member photos are local file paths in Room. Upload them to Storage and store a
        // stable public URL in the remote member row so other devices can display them.
        if (table == T_MEMBERS) {
            jsonStr = prepareMemberPhotoPayload(jsonStr)
        }

        try {
            upsertJsonNow(table, jsonStr)
        } catch (e: IOException) {
            enqueueUpsert(table, jsonStr)
            // Offline-first: the local Room write remains successful and will be retried later.
        }
    }

    private suspend fun upsertJsonNow(table: String, jsonStr: String) {
        val bearer = getAuthBearer()
        val rb = jsonStr.toRequestBody(json)
        val url = SupabaseConfig.REST_BASE + table
        val resp = api.postRows(url, SupabaseConfig.ANON_KEY, bearer, "return=representation,resolution=merge-duplicates", rb)
        if (resp.isSuccessful) return
        val err = resp.errorBody()?.string().orEmpty()
        throw RuntimeException("فشل رفع بيانات $table (كود ${resp.code()}): $err")
    }

    private suspend fun prepareMemberPhotoPayload(jsonStr: String): String {
        val obj = JSONObject(jsonStr)
        val path = obj.optString("profileImage").takeIf { it.isNotBlank() } ?: return jsonStr
        if (path.startsWith("https://") || path.startsWith("http://")) return jsonStr
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return jsonStr
        val memberId = obj.optLong("id", 0L)
        if (memberId <= 0L) return jsonStr
        val remotePath = "members/$memberId.jpg"
        uploadPhotoObject(remotePath, file)
        obj.put("profileImage", SupabaseConfig.getProjectUrl().trimEnd('/') + "/storage/v1/object/public/member-photos/$remotePath")
        return obj.toString()
    }

    private suspend fun uploadPhotoObject(remotePath: String, file: File) {
        val bytes = file.readBytes()
        val resp = api.uploadStorageObject(
            SupabaseConfig.BASE_URL + "storage/v1/object/member-photos/$remotePath",
            SupabaseConfig.ANON_KEY, getAuthBearer(), "image/jpeg", "true", bytes.toRequestBody("image/jpeg".toMediaType())
        )
        if (!resp.isSuccessful) throw RuntimeException("فشل رفع صورة المخدوم (كود ${resp.code()})")
    }

    suspend fun delete(table: String, id: Long) {
        if (!SupabaseConfig.isConfigured()) return
        try {
            deleteNow(table, id)
        } catch (e: IOException) {
            enqueueDelete(table, id)
        }
    }

    private suspend fun deleteNow(table: String, id: Long) {
        val bearer = getAuthBearer()
        val url = SupabaseConfig.REST_BASE + "$table?id=eq.$id"
        val resp = api.deleteRow(url, SupabaseConfig.ANON_KEY, bearer)
        if (!resp.isSuccessful) {
            throw RuntimeException("فشل حذف بيانات $table (كود ${resp.code()}): ${resp.errorBody()?.string().orEmpty()}")
        }
    }

    private fun pendingArray(): JSONArray = runCatching {
        JSONArray(pendingPrefs.getString(KEY_PENDING, "[]") ?: "[]")
    }.getOrDefault(JSONArray())

    private fun savePendingArray(array: JSONArray) {
        pendingPrefs.edit { putString(KEY_PENDING, array.toString()) }
    }

    private fun enqueueUpsert(table: String, jsonStr: String) {
        val a = pendingArray()
        a.put(JSONObject().put("op", "upsert").put("table", table).put("payload", JSONObject(jsonStr)))
        savePendingArray(a)
    }

    private fun enqueueDelete(table: String, id: Long) {
        val a = pendingArray()
        a.put(JSONObject().put("op", "delete").put("table", table).put("id", id))
        savePendingArray(a)
    }

    private suspend fun flushPendingWrites(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseConfig.isConfigured()) return@runCatching ""
            val original = pendingArray()
            if (original.length() == 0) return@runCatching ""
            val remaining = JSONArray()
            for (i in 0 until original.length()) {
                val op = original.getJSONObject(i)
                try {
                    when (op.optString("op")) {
                        "upsert" -> {
                            val table = op.getString("table")
                            var payload = op.getJSONObject("payload").toString()
                            if (table == T_MEMBERS) payload = prepareMemberPhotoPayload(payload)
                            upsertJsonNow(table, payload)
                        }
                        "delete" -> deleteNow(op.getString("table"), op.getLong("id"))
                    }
                } catch (e: IOException) {
                    remaining.put(op)
                }
            }
            savePendingArray(remaining)
            if (remaining.length() > 0) throw IOException("لا يزال هناك ${remaining.length()} تغيير محلي في انتظار الاتصال.")
            ""
        }
    }

    companion object {

        private const val KEY_PENDING = "pending_operations"

        // Table names (match Supabase schema)
        const val T_GROUPS = "groups"
        const val T_SERVANTS = "servants"
        const val T_MEMBERS = "members"
        const val T_BARCODES = "member_barcodes"
        const val T_VISITS = "visit_records"
        const val T_ATTENDANCES = "attendances"
        const val T_HYMNS = "hymns"
        const val T_HYMN_ASS = "hymn_assessments"
        const val T_BIBLE_LESSONS = "bible_lessons"
        const val T_BIBLE_ASS = "bible_assessments"
        const val T_EXAMS = "exams"
        const val T_EXAM_RESULTS = "exam_results"
        const val T_PERIODS = "evaluation_periods"
        const val T_CLASSES = "school_classes"
        const val T_SETTINGS = "app_settings"
    }
}

