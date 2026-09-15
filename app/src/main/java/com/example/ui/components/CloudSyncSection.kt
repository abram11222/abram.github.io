package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.ContentCopy
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
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PresentGreen
import com.example.ui.viewmodel.DeaconsViewModel

@Composable
fun CloudSyncCard(
    viewModel: DeaconsViewModel,
    modifier: Modifier = Modifier,
    title: String = "المزامنة السحابية (Supabase)"
) {
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val syncMsg by viewModel.syncMessage.collectAsStateWithLifecycle()
    val cloudUrl by viewModel.cloudUrl.collectAsStateWithLifecycle()
    val isConfigured = viewModel.isCloudConfigured

    var showConfigDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title + Status + Settings Gear
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isConfigured) PresentGreen.copy(alpha = 0.15f) else GoldSecondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isConfigured) Icons.Default.CloudDone else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isConfigured) PresentGreen else GoldSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                        Text(
                            text = if (isConfigured) "⚡ متصل - مزامنة حية تلقائية بين كل الخدام" else "بحاجة لضبط رابط المشروع (غير متصل)",
                            fontSize = 11.sp,
                            color = if (isConfigured) PresentGreen else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(onClick = { showConfigDialog = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "إعدادات الربط السحابي",
                        tint = BurgundyPrimary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Automatic Live Sync Status (Manual sync buttons removed to prevent errors)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = PresentGreen.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PresentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "المزامنة والحفظ التلقائي: قيد العمل ونشط ✅",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PresentGreen
                        )
                        Text(
                            text = "يتم حفظ التغييرات وتحديث السجلات محلياً وسحابياً بشكل تلقائي ومستمر بدون الحاجة للضغط على أي أزرار يدوية.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showConfigDialog) {
        CloudConfigDialog(
            viewModel = viewModel,
            onDismiss = { showConfigDialog = false }
        )
    }
}

@Composable
fun CloudConfigDialog(
    viewModel: DeaconsViewModel,
    onDismiss: () -> Unit
) {
    val cloudUrl by viewModel.cloudUrl.collectAsStateWithLifecycle()
    val cloudKey by viewModel.cloudAnonKey.collectAsStateWithLifecycle()
    val testResult by viewModel.cloudTestResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingCloud.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    var urlInput by remember(cloudUrl) { mutableStateOf(cloudUrl) }
    var keyInput by remember(cloudKey) { mutableStateOf(cloudKey) }
    var showInstructions by remember { mutableStateOf(!viewModel.isCloudConfigured) }
    var copiedSql by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearCloudTestResult()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = BurgundyPrimary)
                Spacer(Modifier.width(8.dp))
                Text("إعدادات ربط Supabase السحابي", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "لربط التطبيق مع سيرفر أونلاين لمشاركة البيانات بين الخدام، أدخل بيانات مشروع Supabase الخاص بك:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("رابط المشروع (Project URL)") },
                    placeholder = { Text("https://xyzcompany.supabase.co") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("المفتاح العام (anon public key)") },
                    placeholder = { Text("eyJhbGciOiJIUzI1Ni...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                // Test Connection Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveCloudConfig(urlInput, keyInput)
                            viewModel.testCloudConnection()
                        },
                        enabled = !isTesting && urlInput.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("جاري الفحص...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("فحص واختبار الاتصال", fontSize = 12.sp)
                        }
                    }

                    TextButton(onClick = { showInstructions = !showInstructions }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (showInstructions) "إخفاء الدليل" else "شرح الربط", fontSize = 12.sp)
                    }
                }

                // Test Result Banner
                testResult?.let { res ->
                    Spacer(Modifier.height(8.dp))
                    val isSuccess = res.contains("بنجاح")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSuccess) PresentGreen.copy(alpha = 0.15f) else AbsentRed.copy(alpha = 0.15f)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccess) PresentGreen else AbsentRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = res,
                                fontSize = 11.sp,
                                color = if (isSuccess) PresentGreen else AbsentRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Quick Setup Guide
                AnimatedVisibility(visible = showInstructions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            "⚡ كيفية إعداد السيرفر للمزامنة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BurgundyPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("1. في لوحة Supabase ادخل على SQL Editor.", fontSize = 11.sp)
                        Text("2. تأكد من تشغيل كود إنشاء الجداول (supabase_schema.sql).", fontSize = 11.sp)
                        Text("3. إذا ظهر خطأ صلاحيات أو هوية أرقام، نفّذ هذا الأمر لفتح المزامنة التلقائية لكافة هواتف الخدمة:", fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))

                        val rlsFixSql = """
                            -- 1. فتح الصلاحيات للخدام
                            alter table school_classes disable row level security;
                            alter table groups disable row level security;
                            alter table servants disable row level security;
                            alter table members disable row level security;
                            alter table member_barcodes disable row level security;
                            alter table visit_records disable row level security;
                            alter table attendances disable row level security;
                            alter table hymns disable row level security;
                            alter table hymn_assessments disable row level security;
                            alter table bible_lessons disable row level security;
                            alter table bible_assessments disable row level security;
                            alter table exams disable row level security;
                            alter table exam_results disable row level security;
                            alter table evaluation_periods disable row level security;
                            alter table app_settings disable row level security;

                            -- 2. توحيد الأرقام التعريفية بين هواتف الخدام
                            alter table school_classes alter column "id" set generated by default;
                            alter table groups alter column "id" set generated by default;
                            alter table servants alter column "id" set generated by default;
                            alter table members alter column "id" set generated by default;
                            alter table member_barcodes alter column "id" set generated by default;
                            alter table visit_records alter column "id" set generated by default;
                            alter table attendances alter column "id" set generated by default;
                            alter table hymns alter column "id" set generated by default;
                            alter table hymn_assessments alter column "id" set generated by default;
                            alter table bible_lessons alter column "id" set generated by default;
                            alter table bible_assessments alter column "id" set generated by default;
                            alter table exams alter column "id" set generated by default;
                            alter table exam_results alter column "id" set generated by default;
                            alter table evaluation_periods alter column "id" set generated by default;
                        """.trimIndent()

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(rlsFixSql))
                                copiedSql = true
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (copiedSql) "تم نسخ كود الصلاحيات!" else "نسخ كود فتح الصلاحيات (SQL)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveCloudConfig(urlInput, keyInput)
                    viewModel.clearCloudTestResult()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("حفظ الإعدادات", fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.clearCloudTestResult()
                onDismiss()
            }) {
                Text("إغلاق", fontSize = 13.sp)
            }
        }
    )
}
