package com.example.data.remote

import android.content.Context
import androidx.core.content.edit
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles Supabase Auth (sign up / sign in / sign out), persists the session,
 * exposes the current [AppUser] (with role), and provides the admin operations
 * on the app_users table (list / activate / set role / rename).
 */
class AuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val api get() = SupabaseClient.api
    private val moshi get() = SupabaseClient.moshi

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    val accessToken: String? get() = prefs.getString(KEY_ACCESS_TOKEN, null)
    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()

    private val json = "application/json; charset=utf-8".toMediaType()
    private fun body(s: String) = s.toRequestBody(json)

    // Passwords are never stored locally. Supabase Auth is the single source of truth.

    // ----------------------------- AUTH -----------------------------

    /**
     * Real Supabase sign-up. A newly registered servant is created by the
     * Supabase Auth trigger in app_users and remains inactive until an admin
     * approves the account.
     */
    suspend fun signUp(email: String, password: String, fullName: String): Result<AppUser> = runCatching {
        val normEmail = email.trim().lowercase()
        val trimmedPass = password.trim()
        val cleanName = fullName.trim()

        require(normEmail.isNotBlank() && normEmail.contains("@")) { "يرجى إدخال بريد إلكتروني صالح" }
        require(trimmedPass.length >= 6) { "كلمة المرور يجب ألا تقل عن 6 خانات" }
        require(cleanName.isNotBlank()) { "يرجى كتابة الاسم بالكامل" }
        require(SupabaseConfig.isConfigured()) { "لم يتم ضبط اتصال Supabase." }

        val payload = JSONObject().apply {
            put("email", normEmail)
            put("password", trimmedPass)
            put("data", JSONObject().put("full_name", cleanName))
        }.toString()
        val resp = api.authSignUp(SupabaseConfig.AUTH_BASE + "signup", SupabaseConfig.ANON_KEY, body(payload))
        if (!resp.isSuccessful) throw RuntimeException(parseError(resp))

        val raw = resp.body()?.string().orEmpty()
        val obj = JSONObject(raw.ifBlank { "{}" })
        val access = obj.optString("access_token").takeIf { it.isNotBlank() }
        val refresh = obj.optString("refresh_token").takeIf { it.isNotBlank() }
        val userObj = obj.optJSONObject("user")
        val uid = userObj?.optString("id").orEmpty()

        // If email confirmation is enabled, signup succeeds without a session.
        if (access.isNullOrBlank() || refresh.isNullOrBlank() || uid.isBlank()) {
            return@runCatching AppUser(
                id = uid.ifBlank { "pending" },
                email = normEmail,
                fullName = cleanName,
                role = "servant",
                isActive = false,
                canRecordAttendance = false,
                canRecordAssessments = false,
                canManageMembers = false,
                canExportReports = false
            )
        }

        persistSession(access, refresh, uid, normEmail)
        val user = fetchCurrentUser() ?: throw RuntimeException("تم إنشاء الحساب لكن لم يمكن تحميل بيانات الصلاحيات.")
        if (user.isActive) {
            saveCurrentSession(user, isOffline = false)
            _currentUser.value = user
        } else {
            clearSession()
        }
        user
    }

    /** Real Supabase password sign-in. No local password fallback exists. */
    suspend fun signIn(email: String, password: String): Result<AppUser> = runCatching {
        val normEmail = email.trim().lowercase()
        val trimmedPass = password.trim()
        require(normEmail.isNotBlank()) { "يرجى إدخال البريد الإلكتروني" }
        require(trimmedPass.isNotBlank()) { "يرجى إدخال كلمة المرور" }
        require(SupabaseConfig.isConfigured()) { "لم يتم ضبط اتصال Supabase." }

        val payload = JSONObject().apply {
            put("email", normEmail)
            put("password", trimmedPass)
        }.toString()
        val resp = api.authSignIn(SupabaseConfig.AUTH_BASE + "token?grant_type=password", SupabaseConfig.ANON_KEY, body(payload))
        if (!resp.isSuccessful) throw RuntimeException(parseError(resp))

        val obj = JSONObject(resp.body()?.string().orEmpty())
        val access = obj.optString("access_token")
        val refresh = obj.optString("refresh_token")
        val userObj = obj.optJSONObject("user") ?: throw RuntimeException("لم يرجع Supabase بيانات المستخدم.")
        val uid = userObj.optString("id")
        require(access.isNotBlank() && refresh.isNotBlank() && uid.isNotBlank()) { "جلسة Supabase غير مكتملة." }

        persistSession(access, refresh, uid, userObj.optString("email", normEmail))
        val user = fetchCurrentUser() ?: throw RuntimeException("الحساب موجود لكن لم يتم إنشاء/قراءة ملف الصلاحيات app_users.")
        if (!user.isActive) {
            clearSession()
            throw RuntimeException("الحساب صحيح، لكنه ما زال في انتظار تفعيل الأدمن وتحديد الصلاحيات.")
        }
        saveCurrentSession(user, isOffline = false)
        _currentUser.value = user
        user
    }

    /** Restore a real Supabase session. Never fabricates an admin or user. */
    suspend fun loadSession(): AppUser? {
        val token = accessToken
        if (token.isNullOrBlank() || token == "offline_token") {
            clearSession()
            _currentUser.value = null
            return null
        }

        // Refresh first when possible. If the refresh token is unavailable, the
        // existing access token can still be checked below.
        refreshSessionIfPossible()
        return try {
            val user = fetchCurrentUser()
            if (user == null || !user.isActive) {
                if (user != null && !user.isActive) clearSession()
                _currentUser.value = null
                null
            } else {
                saveCurrentSession(user, isOffline = false)
                _currentUser.value = user
                user
            }
        } catch (_: java.io.IOException) {
            // Network unavailable: allow the last verified profile only while the JWT
            // itself is still within its expiry window. A revoked/expired session
            // therefore cannot silently become an unlimited offline login.
            val cached = readCachedUser()
            if (cached != null && !isAccessTokenExpired(token)) {
                _currentUser.value = cached
                cached
            } else {
                clearSession()
                _currentUser.value = null
                null
            }
        }
    }

    private fun isAccessTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split('.')
            if (parts.size < 2) return true
            val payload = android.util.Base64.decode(
                parts[1].replace('-', '+').replace('_', '/').padEnd(((parts[1].length + 3) / 4) * 4, '='),
                android.util.Base64.DEFAULT
            )
            val exp = JSONObject(String(payload, Charsets.UTF_8)).optLong("exp", 0L)
            exp <= 0L || exp <= System.currentTimeMillis() / 1000L
        } catch (_: Exception) { true }
    }

    fun signOut() {
        val token = accessToken
        if (!token.isNullOrBlank() && token != "offline_token") {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching { api.authSignOut(SupabaseConfig.AUTH_BASE + "logout", SupabaseConfig.ANON_KEY, "Bearer $token") }
            }
        }
        clearSession()
        _currentUser.value = null
    }

    // --------------------------- USER LOOKUP ---------------------------

    private fun readCachedUser(): AppUser? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return AppUser(
            id = id,
            email = email,
            fullName = prefs.getString(KEY_FULL_NAME, "") ?: "",
            role = prefs.getString(KEY_ROLE, "servant") ?: "servant",
            isActive = true,
            assignedClass = prefs.getString(KEY_ASSIGNED_CLASS, "") ?: "",
            assignedClasses = runCatching { JSONArray(prefs.getString(KEY_ASSIGNED_CLASSES, "[]") ?: "[]").let { a -> (0 until a.length()).map { i -> a.optString(i) }.filter { it.isNotBlank() } } }.getOrDefault(emptyList()),
            canRecordAttendance = prefs.getBoolean(KEY_CAN_ATTENDANCE, false),
            canRecordAssessments = prefs.getBoolean(KEY_CAN_ASSESSMENTS, false),
            canManageMembers = prefs.getBoolean(KEY_CAN_MEMBERS, false),
            canExportReports = prefs.getBoolean(KEY_CAN_REPORTS, false)
        )
    }

    private suspend fun refreshSessionIfPossible() {
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null).orEmpty()
        if (refresh.isBlank()) return
        runCatching {
            val payload = JSONObject().apply { put("refresh_token", refresh) }.toString()
            val resp = api.authRefresh(SupabaseConfig.AUTH_BASE + "token?grant_type=refresh_token", SupabaseConfig.ANON_KEY, body(payload))
            if (!resp.isSuccessful) return@runCatching
            val obj = JSONObject(resp.body()?.string().orEmpty())
            val access = obj.optString("access_token")
            val newRefresh = obj.optString("refresh_token", refresh)
            val user = obj.optJSONObject("user")
            val uid = user?.optString("id").orEmpty().ifBlank { prefs.getString(KEY_USER_ID, "") ?: "" }
            val email = user?.optString("email").orEmpty().ifBlank { prefs.getString(KEY_EMAIL, "") ?: "" }
            if (access.isNotBlank() && uid.isNotBlank()) persistSession(access, newRefresh, uid, email)
        }
    }

    suspend fun fetchCurrentUser(): AppUser? {
        val token = accessToken ?: return null
        val uid = prefs.getString(KEY_USER_ID, null) ?: return null
        return runCatching {
            val url = SupabaseConfig.REST_BASE + "app_users?id=eq.$uid"
            val resp = api.getRows(url, SupabaseConfig.ANON_KEY, "Bearer $token")
            if (!resp.isSuccessful) return null
            val arr = JSONArray(resp.body()?.string().orEmpty())
            if (arr.length() == 0) return null
            val o = arr.getJSONObject(0)
            AppUser(
                id = o.optString("id"),
                email = o.optString("email"),
                fullName = o.optString("full_name"),
                phone = o.optString("phone"),
                role = o.optString("role", "servant"),
                servantId = if (o.isNull("servant_id")) null else o.optLong("servant_id"),
                isActive = o.optBoolean("is_active", false),
                assignedClass = o.optString("assigned_class", ""),
                assignedClasses = runCatching { o.optJSONArray("assigned_classes")?.let { a -> (0 until a.length()).map { i -> a.optString(i) }.filter { it.isNotBlank() } } ?: emptyList() }.getOrDefault(emptyList()),
                canRecordAttendance = o.optBoolean("can_record_attendance", false),
                canRecordAssessments = o.optBoolean("can_record_assessments", false),
                canManageMembers = o.optBoolean("can_manage_members", false),
                canExportReports = o.optBoolean("can_export_reports", false)
            )
        }.getOrNull()
    }

    // --------------------------- ADMIN OPS ----------------------------

    /** Local cache is only a UI/offline cache; it is never an auth source. */
    fun getLocalUsers(): List<AppUser> {
        val json = prefs.getString(KEY_LOCAL_USERS, null) ?: return emptyList()
        return runCatching {
            val type = Types.newParameterizedType(MutableList::class.java, AppUser::class.java)
            val adapter = moshi.adapter<List<AppUser>>(type)
            adapter.fromJson(json).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun saveLocalUsers(list: List<AppUser>) {
        runCatching {
            val type = Types.newParameterizedType(MutableList::class.java, AppUser::class.java)
            val adapter = moshi.adapter<List<AppUser>>(type)
            prefs.edit { putString(KEY_LOCAL_USERS, adapter.toJson(list)) }
        }
    }

    private fun cacheUser(user: AppUser) {
        val current = getLocalUsers().toMutableList()
        val index = current.indexOfFirst { it.id == user.id || it.email.equals(user.email, true) }
        if (index >= 0) current[index] = user else current.add(user)
        saveLocalUsers(current)
    }

    suspend fun approveAndSetUser(
        id: String,
        role: String,
        assignedClasses: List<String>,
        canAttendance: Boolean,
        canAssessments: Boolean,
        canMembers: Boolean,
        canReports: Boolean
    ): Boolean {
        require(currentUser.value?.isAdmin == true) { "غير مصرح: هذه العملية للأدمن فقط." }
        val token = accessToken ?: throw RuntimeException("جلسة Supabase غير موجودة.")
        val payload = JSONObject().apply {
            put("role", role)
            put("is_active", true)
            val cleanClasses = assignedClasses.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            put("assigned_classes", JSONArray(cleanClasses))
            put("assigned_class", cleanClasses.firstOrNull() ?: "")
            put("can_record_attendance", canAttendance)
            put("can_record_assessments", canAssessments)
            put("can_manage_members", canMembers)
            put("can_export_reports", canReports)
        }.toString()
        val url = SupabaseConfig.REST_BASE + "app_users?id=eq.$id"
        val resp = api.patchRow(url, SupabaseConfig.ANON_KEY, "Bearer $token", "return=representation", body(payload))
        if (!resp.isSuccessful) throw RuntimeException(parseError(resp))
        val raw = resp.body()?.string().orEmpty()
        if (raw == "[]" || raw.isBlank()) throw RuntimeException("لم يتم العثور على الحساب في السيرفر.")
        listUsers()
        return true
    }

    suspend fun listUsers(): List<AppUser> {
        val token = accessToken ?: throw RuntimeException("يجب تسجيل الدخول بحساب أدمن.")
        if (_currentUser.value?.isAdmin != true) throw RuntimeException("غير مصرح: الأدمن فقط يمكنه إدارة الحسابات.")
        val url = SupabaseConfig.REST_BASE + "app_users?order=created_at.asc"
        val resp = api.getRows(url, SupabaseConfig.ANON_KEY, "Bearer $token")
        if (!resp.isSuccessful) throw RuntimeException(parseError(resp))
        val raw = resp.body()?.string().orEmpty()
        val type = Types.newParameterizedType(MutableList::class.java, AppUser::class.java)
        @Suppress("UNCHECKED_CAST")
        val adapter = moshi.adapter<Any>(type) as com.squareup.moshi.JsonAdapter<List<AppUser>>
        val users = adapter.fromJson(raw).orEmpty()
        saveLocalUsers(users)
        return users
    }

    suspend fun setUserActive(id: String, active: Boolean): Boolean {
        require(_currentUser.value?.isAdmin == true) { "غير مصرح: الأدمن فقط." }
        val token = accessToken ?: throw RuntimeException("جلسة Supabase غير موجودة.")
        val resp = api.patchRow(
            SupabaseConfig.REST_BASE + "app_users?id=eq.$id",
            SupabaseConfig.ANON_KEY, "Bearer $token", "return=representation",
            body(JSONObject().put("is_active", active).toString())
        )
        if (!resp.isSuccessful) throw RuntimeException(parseError(resp))
        listUsers(); return true
    }

    suspend fun setUserRole(id: String, role: String): Boolean {
        require(_currentUser.value?.isAdmin == true) { "غير مصرح: الأدمن فقط." }
        if (role in listOf("admin", "super_admin")) {
            require(_currentUser.value?.isSuperAdmin == true) { "غير مصرح: إنشاء أو تغيير دور الأدمن متاح للـ Super Admin فقط." }
        }
        require(role in listOf("admin", "servant", "viewer")) { "دور المستخدم غير صالح." }
        val token = accessToken ?: throw RuntimeException("جلسة Supabase غير موجودة.")
        val resp = api.patchRow(
            SupabaseConfig.REST_BASE + "app_users?id=eq.$id",
            SupabaseConfig.ANON_KEY, "Bearer $token", "return=representation",
            body(JSONObject().put("role", role).toString())
        )
        if (!resp.isSuccessful) throw RuntimeException(parseError(resp))
        listUsers(); return true
    }

    suspend fun setUserFullName(id: String, name: String): Boolean {
        require(_currentUser.value?.isAdmin == true) { "غير مصرح: الأدمن فقط." }
        val token = accessToken ?: throw RuntimeException("جلسة Supabase غير موجودة.")
        val resp = api.patchRow(
            SupabaseConfig.REST_BASE + "app_users?id=eq.$id",
            SupabaseConfig.ANON_KEY, "Bearer $token", "return=representation",
            body(JSONObject().put("full_name", name.trim()).toString())
        )
        if (!resp.isSuccessful) throw RuntimeException(parseError(resp))
        listUsers(); return true
    }

    suspend fun setUserPermissions(
        id: String,
        assignedClasses: List<String>,
        canAttendance: Boolean,
        canAssessments: Boolean,
        canMembers: Boolean,
        canReports: Boolean
    ): Boolean {
        require(_currentUser.value?.isAdmin == true) { "غير مصرح: الأدمن فقط." }
        val token = accessToken ?: throw RuntimeException("جلسة Supabase غير موجودة.")
        val cleanClasses = assignedClasses.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val payload = JSONObject().apply {
            put("assigned_classes", JSONArray(cleanClasses))
            put("assigned_class", cleanClasses.firstOrNull() ?: "")
            put("can_record_attendance", canAttendance)
            put("can_record_assessments", canAssessments)
            put("can_manage_members", canMembers)
            put("can_export_reports", canReports)
        }.toString()
        val resp = api.patchRow(SupabaseConfig.REST_BASE + "app_users?id=eq.$id", SupabaseConfig.ANON_KEY, "Bearer $token", "return=representation", body(payload))
        if (!resp.isSuccessful) throw RuntimeException(parseError(resp))
        listUsers(); return true
    }

    /**
     * Client apps must not use the service_role key to create Auth users.
     * Account creation therefore happens through the public Sign Up screen;
     * the admin only approves/configures the resulting app_users row.
     */
    suspend fun addServantUser(
        fullName: String,
        email: String,
        role: String,
        assignedClasses: List<String>,
        canAttendance: Boolean,
        canAssessments: Boolean,
        canMembers: Boolean,
        canReports: Boolean
    ): AppUser {
        throw RuntimeException("لأمان النظام، إنشاء حساب Auth يتم من شاشة إنشاء الحساب. بعد التسجيل يظهر هنا كطلب قيد المراجعة لتفعيله.")
    }

    suspend fun deleteUser(id: String): Boolean {
        // Do not delete only app_users: that would leave an orphaned Supabase Auth account.
        // Account removal is represented as deactivation from the Android client.
        // A Super Admin can later permanently remove Auth users through a server-side admin tool.
        return setUserActive(id, false)
    }

    // --------------------------- HELPERS ------------------------------

    private fun persistSession(token: String, refresh: String, uid: String, email: String) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refresh)
            putString(KEY_USER_ID, uid)
            putString(KEY_EMAIL, email)
        }
    }

    private fun saveCurrentSession(user: AppUser, isOffline: Boolean) {
        prefs.edit {
            putString(KEY_FULL_NAME, user.fullName)
            putString(KEY_ROLE, user.role)
            putBoolean(KEY_IS_OFFLINE, isOffline)
            putString(KEY_ASSIGNED_CLASS, user.effectiveAssignedClasses.firstOrNull().orEmpty())
            putString(KEY_ASSIGNED_CLASSES, JSONArray(user.effectiveAssignedClasses).toString())
            putBoolean(KEY_CAN_ATTENDANCE, user.canRecordAttendance)
            putBoolean(KEY_CAN_ASSESSMENTS, user.canRecordAssessments)
            putBoolean(KEY_CAN_MEMBERS, user.canManageMembers)
            putBoolean(KEY_CAN_REPORTS, user.canExportReports)
        }
    }

    private fun clearSession() = prefs.edit { clear() }

    private fun parseError(resp: retrofit2.Response<okhttp3.ResponseBody>): String {
        return try {
            val raw = resp.errorBody()?.string().orEmpty()
            val obj = JSONObject(raw)
            obj.optString("msg").ifBlank {
                obj.optString("error_description").ifBlank {
                    obj.optString("message").ifBlank { obj.optString("error", "HTTP ${resp.code()}") }
                }
            }
        } catch (_: Exception) { "HTTP ${resp.code()}" }
    }

    companion object {
        private const val PREFS = "deacons_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_ROLE = "role"
        private const val KEY_IS_OFFLINE = "is_offline"
        private const val KEY_ASSIGNED_CLASS = "assigned_class"
        private const val KEY_ASSIGNED_CLASSES = "assigned_classes"
        private const val KEY_CAN_ATTENDANCE = "can_attendance"
        private const val KEY_CAN_ASSESSMENTS = "can_assessments"
        private const val KEY_CAN_MEMBERS = "can_members"
        private const val KEY_CAN_REPORTS = "can_reports"
        private const val KEY_LOCAL_USERS = "local_users_json"
    }
}
