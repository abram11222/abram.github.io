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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AbsentRedBg
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenBg
import com.example.ui.viewmodel.DeaconsViewModel

@Composable
fun AccountScreen(
    viewModel: DeaconsViewModel,
    onNavigateToAdmin: (() -> Unit)?
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("حسابي والصلاحيات", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BurgundyPrimary)
        Spacer(Modifier.height(12.dp))

        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isAdmin) GoldSecondary.copy(alpha = 0.2f) else BurgundyPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.Person,
                            contentDescription = null,
                            tint = if (isAdmin) GoldSecondary else BurgundyPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = user?.fullName?.ifBlank { user?.email ?: "" } ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = BurgundyPrimary
                        )
                        Text(
                            text = user?.email ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val roleName = when (user?.userRole?.name) {
                                "admin" -> "👑 مسؤول النظام (أدمن)"
                                "servant" -> "خادم مخصص"
                                "viewer" -> "مشاهد فقط (Viewer)"
                                else -> "—"
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isAdmin) GoldSecondary.copy(alpha = 0.15f) else BurgundyPrimary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = roleName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdmin) GoldSecondary else BurgundyPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (!user?.assignedClass.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "فصل: ${user?.assignedClass}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Permissions Breakdown
                Text("مصفوفة الصلاحيات الممنوحة لهذا الحساب:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                PermissionRowItem(
                    title = "لوحة التحكم الرئيسية (Dashboard)",
                    granted = isAdmin,
                    detail = if (isAdmin) "متاح (إحصائيات كاملة لكل الفصول)" else "❌ غير متاح لحساب الخادم (مخصص للأدمن فقط)"
                )
                PermissionRowItem(
                    title = "تسجيل حضور وغياب الخدمة والقداسات",
                    granted = user?.canRecordAttendance == true || isAdmin,
                    detail = if (user?.canRecordAttendance == true || isAdmin) "مسموح الرصد والتعديل" else "مشاهدة فقط"
                )
                PermissionRowItem(
                    title = "رصد تقييمات الألحان والكتاب المقدس",
                    granted = user?.canRecordAssessments == true || isAdmin,
                    detail = if (user?.canRecordAssessments == true || isAdmin) "مسموح التسميع والرصد" else "مشاهدة فقط"
                )
                PermissionRowItem(
                    title = "إضافة وتعديل وحذف المخدومين",
                    granted = user?.canManageMembers == true || isAdmin,
                    detail = if (user?.canManageMembers == true || isAdmin) "مسموح إضافة وتعديل" else "غير مصرح"
                )
                PermissionRowItem(
                    title = "تصدير تقارير Excel وملفات PDF",
                    granted = user?.canExportReports == true || isAdmin,
                    detail = if (user?.canExportReports == true || isAdmin) "مسموح التصدير" else "غير مصرح"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Cloud Synchronization Card
        com.example.ui.components.CloudSyncCard(viewModel = viewModel)

        if (isAdmin && onNavigateToAdmin != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigateToAdmin,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("إدارة المستخدمين والصلاحيات (أدمن)", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text("تسجيل الخروج", fontSize = 14.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionRowItem(
    title: String,
    granted: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (granted) PresentGreenBg else AbsentRedBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (granted) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (granted) PresentGreen else AbsentRed,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 10.sp, color = if (granted) PresentGreen else AbsentRed)
        }
    }
}

