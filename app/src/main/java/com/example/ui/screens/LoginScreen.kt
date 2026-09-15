package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.viewmodel.DeaconsViewModel

@Composable
fun LoginScreen(
    viewModel: DeaconsViewModel,
    onLoggedIn: () -> Unit
) {
    var isSignup by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    val loading by viewModel.authLoading.collectAsStateWithLifecycle()
    val message by viewModel.authMessage.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "خدمة الشمامسة",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = BurgundyPrimary
                )
                Text(
                    text = "إدارة المخدومين",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                if (isSignup) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("الاسم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("name_input")
                    )
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.clearAuthMessage(); email = it },
                    label = { Text("البريد الإلكتروني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("email_input")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.clearAuthMessage(); password = it },
                    label = { Text("كلمة المرور") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("password_input")
                )
                Spacer(Modifier.height(8.dp))

                message?.let {
                    Text(
                        text = it,
                        color = if (it.contains("انتظار", true) || it.contains("تم", true))
                            BurgundyPrimary else MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (isSignup) viewModel.signup(email, password, fullName) { ok ->
                            if (ok) { isSignup = false; password = "" } }
                        else viewModel.login(email, password) { ok -> if (ok) onLoggedIn() }
                    },
                    enabled = !loading && email.isNotBlank() && password.isNotBlank() &&
                            (!isSignup || fullName.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_button")
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (isSignup) "إنشاء حساب" else "تسجيل الدخول", fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                TextButton(
                    onClick = {
                        isSignup = !isSignup
                        viewModel.clearAuthMessage()
                    },
                    modifier = Modifier.testTag("toggle_signup_button")
                ) {
                    Text(
                        if (isSignup) "عندي حساب بالفعل — تسجيل الدخول" else "خادم جديد؟ إنشاء حساب الآن",
                        color = GoldSecondary.darker(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// small helper to darken gold for readable text on light surface
private fun androidx.compose.ui.graphics.Color.darker(): androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.lerp(this, androidx.compose.ui.graphics.Color.Black, 0.35f)
