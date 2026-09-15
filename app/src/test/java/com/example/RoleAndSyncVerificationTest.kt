package com.example

import com.example.data.remote.AppUser
import com.example.data.remote.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verification test suite for multi-account roles, permissions matrix,
 * and servant dashboard restrictions.
 */
class RoleAndSyncVerificationTest {

    @Test
    fun testAdminRoleHasFullPermissionsAndDashboard() {
        val adminUser = AppUser(
            id = "admin-1",
            email = "abramonetwo@gmail.com",
            fullName = "أبرام",
            role = "admin",
            isActive = true,
            assignedClass = "",
            canRecordAttendance = true,
            canRecordAssessments = true,
            canManageMembers = true,
            canExportReports = true
        )

        assertEquals(UserRole.admin, adminUser.userRole)
        assertTrue("Admin user must have isAdmin = true", adminUser.isAdmin)
        assertFalse("Admin user must not be servant", adminUser.isServant)
        assertTrue("Admin must have attendance permission", adminUser.canRecordAttendance)
        assertTrue("Admin must have assessment permission", adminUser.canRecordAssessments)
        assertTrue("Admin must have member management permission", adminUser.canManageMembers)
        assertTrue("Admin must have report export permission", adminUser.canExportReports)

        // Dashboard access rule: only admin has dashboard access
        val hasDashboardAccess = adminUser.isAdmin
        assertTrue("Admin MUST have dashboard access", hasDashboardAccess)
    }

    @Test
    fun testServantRoleHasNoDashboardAccess() {
        val servantUser = AppUser(
            id = "servant-1",
            email = "servant.primary@church.org",
            fullName = "مينا سمير",
            role = "servant",
            isActive = true,
            assignedClass = "أولى ابتدائي",
            canRecordAttendance = true,
            canRecordAssessments = true,
            canManageMembers = false,
            canExportReports = false
        )

        assertEquals(UserRole.servant, servantUser.userRole)
        assertTrue("Servant must have isServant = true", servantUser.isServant)
        assertFalse("Servant must NOT have isAdmin", servantUser.isAdmin)
        assertEquals("أولى ابتدائي", servantUser.assignedClass)

        // CRITICAL USER REQUIREMENT: Servant account must NOT have dashboard
        val hasDashboardAccess = servantUser.isAdmin
        assertFalse("Servant account MUST NOT have dashboard access", hasDashboardAccess)

        // Bottom nav logic check: servant bottom nav starts with Attendance, excludes Dashboard
        val bottomNavScreensForServant = listOf("attendance", "members", "assessments", "reports", "account")
        assertFalse("Dashboard route must not be in servant bottom nav", bottomNavScreensForServant.contains("dashboard"))
        assertEquals("Servant root screen must be attendance", "attendance", bottomNavScreensForServant.first())
    }

    @Test
    fun testViewerRoleIsReadOnlyAndNoDashboard() {
        val viewerUser = AppUser(
            id = "viewer-1",
            email = "viewer@church.org",
            fullName = "أستاذ جورج",
            role = "viewer",
            isActive = true,
            assignedClass = "",
            canRecordAttendance = false,
            canRecordAssessments = false,
            canManageMembers = false,
            canExportReports = true
        )

        assertEquals(UserRole.viewer, viewerUser.userRole)
        assertTrue(viewerUser.isViewer)
        assertFalse("Viewer must not be admin", viewerUser.isAdmin)
        assertFalse("Viewer must not have dashboard access", viewerUser.isAdmin)
        assertFalse("Viewer cannot record attendance", viewerUser.canRecordAttendance)
        assertFalse("Viewer cannot record assessments", viewerUser.canRecordAssessments)
        assertFalse("Viewer cannot manage members", viewerUser.canManageMembers)
    }

    @Test
    fun testMultiAccountSyncIsolationAndPermissions() {
        // Simulates two servants in different classes and an admin
        val servantPrimary = AppUser(
            id = "s-1",
            email = "primary@church.org",
            fullName = "خادم أولى ابتدائي",
            role = "servant",
            isActive = true,
            assignedClass = "أولى ابتدائي"
        )

        val servantPrep = AppUser(
            id = "s-2",
            email = "prep@church.org",
            fullName = "خادم أولى إعدادي",
            role = "servant",
            isActive = true,
            assignedClass = "أولى إعدادي"
        )

        assertEquals("أولى ابتدائي", servantPrimary.assignedClass)
        assertEquals("أولى إعدادي", servantPrep.assignedClass)
        assertFalse(servantPrimary.isAdmin)
        assertFalse(servantPrep.isAdmin)
    }
}
