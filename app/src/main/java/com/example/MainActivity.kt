package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AssessmentsScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MemberProfileScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.DeaconsTheme
import com.example.ui.theme.GoldSecondary
import com.example.ui.viewmodel.DeaconsViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "الرئيسية", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    data object Members : Screen("members", "المخدومين", Icons.Filled.People, Icons.Outlined.People)
    data object Attendance : Screen("attendance", "الحضور", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object Assessments : Screen("assessments", "التقييمات", Icons.Filled.Assignment, Icons.Outlined.Assignment)
    data object Reports : Screen("reports", "التقارير", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp)
    data object Settings : Screen("settings", "الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object Account : Screen("account", "حسابي", Icons.Filled.AdminPanelSettings, Icons.Filled.Person)
}

class MainActivity : ComponentActivity() {

    private val viewModel: DeaconsViewModel by viewModels {
        DeaconsViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeaconsTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DeaconsApp(
                        viewModel = viewModel,
                        initialIntent = intent
                    )
                }
            }
        }
    }
}

@Composable
fun DeaconsApp(
    viewModel: DeaconsViewModel,
    initialIntent: Intent? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    var restoring by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { viewModel.restoreSession { ok -> restoring = false } }

    if (restoring) {
        Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BurgundyPrimary)
        }
        return
    }

    if (user == null) {
        LoginScreen(viewModel = viewModel, onLoggedIn = { /* user StateFlow updates → recomposes */ })
        return
    }

    val bottomNavItems = if (isAdmin) {
        listOf(
            Screen.Dashboard,
            Screen.Members,
            Screen.Attendance,
            Screen.Reports,
            Screen.Settings,
            Screen.Account
        )
    } else {
        listOf(
            Screen.Attendance,
            Screen.Members,
            Screen.Assessments,
            Screen.Reports,
            Screen.Account
        )
    }
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }
    val rootRoute = if (isAdmin) Screen.Dashboard.route else Screen.Attendance.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(rootRoute) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BurgundyPrimary,
                                selectedTextColor = BurgundyPrimary,
                                indicatorColor = GoldSecondary.copy(alpha = 0.2f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(if (showBottomBar) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp))
        ) {
            composable("splash") {
                SplashScreen(
                    onSplashFinished = {
                        val navigateTo = initialIntent?.getStringExtra("navigate_to")
                        val targetMemberId = initialIntent?.getLongExtra("member_id", -1L) ?: -1L

                        if (navigateTo == "member_profile" && targetMemberId > 0) {
                            navController.navigate(rootRoute) { popUpTo("splash") { inclusive = true } }
                            navController.navigate("member_profile/$targetMemberId")
                        } else {
                            navController.navigate(rootRoute) { popUpTo("splash") { inclusive = true } }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                if (!isAdmin) {
                    // Servants are strictly forbidden from viewing the Dashboard
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Attendance.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BurgundyPrimary)
                    }
                } else {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToMembers = { navController.navigate(Screen.Members.route) },
                        onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                        onNavigateToAssessments = { navController.navigate(Screen.Assessments.route) },
                        onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                        onSelectMember = { memberId -> navController.navigate("member_profile/$memberId") },
                        onNavigateToAdmin = { navController.navigate("admin") }
                    )
                }
            }

            composable(Screen.Members.route) {
                MembersScreen(
                    viewModel = viewModel,
                    onSelectMember = { memberId -> navController.navigate("member_profile/$memberId") }
                )
            }

            composable(
                route = "member_profile/{memberId}",
                arguments = listOf(navArgument("memberId") { type = NavType.LongType })
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getLong("memberId") ?: 0L
                MemberProfileScreen(
                    memberId = memberId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Attendance.route) {
                AttendanceScreen(
                    viewModel = viewModel,
                    onSelectMember = { memberId -> navController.navigate("member_profile/$memberId") }
                )
            }

            composable(Screen.Assessments.route) {
                AssessmentsScreen(
                    viewModel = viewModel,
                    onSelectMember = { memberId -> navController.navigate("member_profile/$memberId") }
                )
            }


            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel,
                    onSelectMember = { memberId -> navController.navigate("member_profile/$memberId") }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onSelectMember = { memberId -> navController.navigate("member_profile/$memberId") }
                )
            }

            composable(Screen.Account.route) {
                AccountScreen(
                    viewModel = viewModel,
                    onNavigateToAdmin = if (isAdmin) ({ navController.navigate("admin") }) else null
                )
            }

            composable("admin") {
                if (!isAdmin) {
                    LaunchedEffect(Unit) {
                        navController.navigate(rootRoute) {
                            popUpTo("admin") { inclusive = true }
                        }
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BurgundyPrimary)
                    }
                } else {
                    AdminScreen(viewModel = viewModel)
                }
            }
        }
    }
}
