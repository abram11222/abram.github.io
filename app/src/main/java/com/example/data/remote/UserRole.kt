package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class UserRole { super_admin, admin, servant, viewer }

/**
 * The app_users row in Supabase / Local storage.
 * A user is only "active" once an admin sets is_active = true.
 */
@JsonClass(generateAdapter = true)
data class AppUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String,
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "role") val role: String = "servant",
    @Json(name = "servant_id") val servantId: Long? = null,
    @Json(name = "is_active") val isActive: Boolean = false,
    @Json(name = "assigned_class") val assignedClass: String = "",
    @Json(name = "assigned_classes") val assignedClasses: List<String> = emptyList(),
    @Json(name = "can_record_attendance") val canRecordAttendance: Boolean = false,
    @Json(name = "can_record_assessments") val canRecordAssessments: Boolean = false,
    @Json(name = "can_manage_members") val canManageMembers: Boolean = false,
    @Json(name = "can_export_reports") val canExportReports: Boolean = false
) {
    val effectiveAssignedClasses: List<String> get() = assignedClasses.ifEmpty { assignedClass.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty() }
    val userRole: UserRole? get() = runCatching { UserRole.valueOf(role) }.getOrNull()
    val isAdmin: Boolean get() = userRole == UserRole.admin || userRole == UserRole.super_admin
    val isSuperAdmin: Boolean get() = userRole == UserRole.super_admin
    val isServant: Boolean get() = userRole == UserRole.servant
    val isViewer: Boolean get() = userRole == UserRole.viewer
}
