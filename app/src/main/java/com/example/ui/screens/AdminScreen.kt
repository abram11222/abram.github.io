package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import com.example.ui.components.CloudSyncCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.AppUser
import com.example.data.remote.UserRole
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AbsentRedBg
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenBg
import com.example.ui.viewmodel.DeaconsViewModel

private val ROLES = listOf(
    UserRole.admin to "أدمن (مدير النظام)",
    UserRole.servant to "خادم فصل",
    UserRole.viewer to "مشاهدة فقط"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: DeaconsViewModel) {
    LaunchedEffect(Unit) { viewModel.loadAdminUsers() }

    val users by viewModel.adminUsers.collectAsStateWithLifecycle()
    val message by viewModel.adminMessage.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val syncMsg by viewModel.syncMessage.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val schoolClasses by viewModel.schoolClasses.collectAsStateWithLifecycle()
    val currentSessionUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isCurrentUserSuperAdmin = currentSessionUser?.isSuperAdmin == true

    val classNames = remember(schoolClasses, members) {
        val list = mutableListOf("كل الفصول")
        val fromDb = schoolClasses.map { it.name }
        val fromMembers = members.map { it.schoolClass }.filter { it.isNotBlank() }
        list.addAll((fromDb + fromMembers).distinct())
        list
    }

    var userToEditPermissions by remember { mutableStateOf<AppUser?>(null) }
    var userToDelete by remember { mutableStateOf<AppUser?>(null) }

    val adminCount = users.count { it.userRole == UserRole.admin || it.userRole == UserRole.super_admin }
    val servantCount = users.count { it.userRole == UserRole.servant }
    val viewerCount = users.count { it.userRole == UserRole.viewer }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Title & Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "إدارة الخدام والصلاحيات",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BurgundyPrimary
                )
                Text(
                    text = "تحديد صلاحيات كل حساب والفصول المخصصة للمتابعة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        }

        Spacer(Modifier.height(12.dp))

        // Role Metrics Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminMetricCard(title = "الحسابات", count = users.size, color = BurgundyPrimary, modifier = Modifier.weight(1f))
            AdminMetricCard(title = "المدراء (أدمن)", count = adminCount, color = Color(0xFFC2185B), modifier = Modifier.weight(1f))
            AdminMetricCard(title = "الخدام", count = servantCount, color = GoldSecondary, modifier = Modifier.weight(1f))
            AdminMetricCard(title = "مشاهدة", count = viewerCount, color = Color(0xFF00897B), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // Cloud Sync & Remote Control
        CloudSyncCard(viewModel = viewModel)

        message?.let {
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BurgundyPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Pending requests banner
        val pendingUsers = users.filter { !it.isActive }
        if (pendingUsers.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GoldSecondary.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldSecondary)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BurgundyPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "طلبات تسجيل جديدة بانتظار موافقتك (${pendingUsers.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BurgundyPrimary
                        )
                    }
                    Text(
                        "قام هؤلاء الخدام بإنشاء حساباتهم. اضغط 'اعتماد وتحديد الصلاحيات' لتفعيلهم وتحديد فصولهم ليتمكنوا من الدخول:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Users List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(users, key = { it.id }) { user ->
                AdminUserCard(
                    user = user,
                    isCurrentUser = user.id == currentSessionUser?.id,
                    canManageAdmins = isCurrentUserSuperAdmin,
                    onEditPermissions = { userToEditPermissions = user },
                    onToggleActive = { viewModel.setUserActive(user.id, !user.isActive) },
                    onDelete = { userToDelete = user }
                )
            }
        }
    }

    // Edit Permissions Dialog
    userToEditPermissions?.let { user ->
        EditUserPermissionsDialog(
            user = user,
            classOptions = classNames,
            canManageAdmins = isCurrentUserSuperAdmin,
            onDismiss = { userToEditPermissions = null },
            onSave = { assignedClasses, canAtt, canAssess, canMem, canRep, role ->
                // Ensure approved user is active
                viewModel.setUserActive(user.id, true)
                viewModel.setUserRole(user.id, role)
                viewModel.setUserPermissions(
                    id = user.id,
                    assignedClasses = assignedClasses,
                    canAttendance = canAtt,
                    canAssessments = canAssess,
                    canMembers = canMem,
                    canReports = canRep
                )
                userToEditPermissions = null
            }
        )
    }

    // Confirm Delete Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("تأكيد تعطيل الحساب") },
            text = { Text("هل أنت متأكد من رغبتك في تعطيل حساب الخادم (${user.fullName.ifBlank { user.email }})؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(user.id)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("تعطيل الحساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminUserCard(
    user: AppUser,
    isCurrentUser: Boolean,
    canManageAdmins: Boolean,
    onEditPermissions: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val role = user.userRole ?: UserRole.servant
    val roleLabel = when (role) {
        UserRole.super_admin -> "المالك (Super Admin)"
        UserRole.admin -> "مسؤول النظام (أدمن)"
        UserRole.servant -> "خادم فصل"
        UserRole.viewer -> "مشاهدة فقط"
    }

    val roleColor = when (role) {
        UserRole.super_admin -> GoldSecondary
        UserRole.admin -> BurgundyPrimary
        UserRole.servant -> GoldSecondary
        UserRole.viewer -> Color(0xFF546E7A)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (!user.isActive) androidx.compose.foundation.BorderStroke(1.5.dp, GoldSecondary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (!user.isActive) GoldSecondary.copy(alpha = 0.08f)
            else if (isCurrentUser) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            if (!user.isActive) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GoldSecondary.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⏳ حساب قيد الانتظار — في انتظار اعتماد الأدمن",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                    }
                }
            }

            // Header Row: Avatar, Name, Email, Active Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = roleColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user.fullName.firstOrNull() ?: user.email.firstOrNull() ?: 'خ').toString(),
                            fontWeight = FontWeight.Bold,
                            color = roleColor,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.fullName.ifBlank { user.email },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCurrentUser) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BurgundyPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "أنت الآن",
                                    fontSize = 10.sp,
                                    color = BurgundyPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Active / Inactive Switch
                Switch(
                    checked = user.isActive,
                    enabled = !isCurrentUser && (canManageAdmins || (role != UserRole.admin && role != UserRole.super_admin)),
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(checkedThumbColor = PresentGreen)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Role and Assigned Class Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = roleColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = roleColor, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = roleLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = roleColor)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (user.effectiveAssignedClasses.isNotEmpty()) "الفصول: ${user.effectiveAssignedClasses.joinToString("، ")}" else "كل الفصول",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Permissions Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PermissionBadge(title = "الحضور", granted = user.canRecordAttendance, modifier = Modifier.weight(1f))
                PermissionBadge(title = "الألحان", granted = user.canRecordAssessments, modifier = Modifier.weight(1f))
                PermissionBadge(title = "المخدومين", granted = user.canManageMembers, modifier = Modifier.weight(1f))
                PermissionBadge(title = "التقارير", granted = user.canExportReports, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEditPermissions,
                    enabled = !isCurrentUser && (canManageAdmins || (role != UserRole.admin && role != UserRole.super_admin)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!user.isActive) PresentGreen else BurgundyPrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        if (!user.isActive) Icons.Default.CheckCircle else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (!user.isActive) "اعتماد وتحديد الصلاحيات" else "تعديل الصلاحيات والفصل",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isCurrentUser && (canManageAdmins || (role != UserRole.admin && role != UserRole.super_admin))) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AbsentRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBadge(title: String, granted: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (granted) PresentGreenBg else AbsentRedBg
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (granted) PresentGreen else AbsentRed,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (granted) PresentGreen else AbsentRed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditUserPermissionsDialog(
    user: AppUser,
    classOptions: List<String>,
    canManageAdmins: Boolean,
    onDismiss: () -> Unit,
    onSave: (assignedClasses: List<String>, canAtt: Boolean, canAssess: Boolean, canMem: Boolean, canRep: Boolean, role: String) -> Unit
) {
    var selectedRole by remember { mutableStateOf((user.userRole ?: UserRole.servant).name) }
    var selectedClasses by remember { mutableStateOf(user.effectiveAssignedClasses.toSet()) }

    var canAttendance by remember { mutableStateOf(user.canRecordAttendance) }
    var canAssessments by remember { mutableStateOf(user.canRecordAssessments) }
    var canMembers by remember { mutableStateOf(user.canManageMembers) }
    var canReports by remember { mutableStateOf(user.canExportReports) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("تعديل صلاحيات الخادم", fontWeight = FontWeight.Bold)
                Text(user.fullName.ifBlank { user.email }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Role Selection
                Text("الدور في الخدمة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ROLES.filter { it.first != UserRole.admin || canManageAdmins }.forEach { (role, label) ->
                        FilterChip(
                            selected = selectedRole == role.name,
                            onClick = {
                                selectedRole = role.name
                                if (role == UserRole.admin) {
                                    canAttendance = true
                                    canAssessments = true
                                    canMembers = true
                                    canReports = true
                                } else if (role == UserRole.viewer) {
                                    canAttendance = false
                                    canAssessments = false
                                    canMembers = false
                                    canReports = false
                                }
                            },
                            label = { Text(label.split(" ").first(), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BurgundyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Allowed classes (multiple)
                Text("الفصول المسموح بها:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(Modifier.verticalScroll(rememberScrollState()).height(150.dp)) {
                    classOptions.filter { it != "كل الفصول" }.forEach { cls ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = cls in selectedClasses, onCheckedChange = { checked ->
                                selectedClasses = if (checked) selectedClasses + cls else selectedClasses - cls
                            })
                            Text(cls, fontSize = 13.sp)
                        }
                    }
                }
                Text(
                    if (selectedClasses.isEmpty()) "لم يتم اختيار فصل" else "المحدد: ${selectedClasses.joinToString("، ")}",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Granular Permissions
                Text("الصلاحيات الممنوحة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                PermissionSwitchRow(label = "تسجيل الحضور والغياب", checked = canAttendance, onCheckedChange = { canAttendance = it })
                PermissionSwitchRow(label = "رصد وتسميع الألحان", checked = canAssessments, onCheckedChange = { canAssessments = it })
                PermissionSwitchRow(label = "إضافة وتعديل المخدومين", checked = canMembers, onCheckedChange = { canMembers = it })
                PermissionSwitchRow(label = "تصدير تقارير Excel", checked = canReports, onCheckedChange = { canReports = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedClasses.toList(), canAttendance, canAssessments, canMembers, canReports, selectedRole)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Text("حفظ الصلاحيات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun PermissionSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
        )
    }
}
