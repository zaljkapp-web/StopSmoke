package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ShiftScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.VacationScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.IceBlue
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SmokeShiftTheme
import com.example.viewmodel.SmokingViewModel

class MainActivity : ComponentActivity() {

    // Request notification permission dynamically for Android 13+ (API 33+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission outcome logged if needed, the app continues gracefully either way
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prompt notification permission if missing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SmokeShiftTheme {
                val viewModel: SmokingViewModel = viewModel()
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .border(1.dp, SlateBorder)
                                .height(80.dp)
                                .testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentRoute == "dashboard",
                                onClick = {
                                    if (currentRoute != "dashboard") {
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Főoldal") },
                                label = { Text("Főoldal") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = IceBlue,
                                    indicatorColor = IceBlue,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("nav_tab_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentRoute == "stats",
                                onClick = {
                                    if (currentRoute != "stats") {
                                        navController.navigate("stats") {
                                            popUpTo("dashboard")
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Statisztika") },
                                label = { Text("Statisztika") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = IceBlue,
                                    indicatorColor = IceBlue,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("nav_tab_stats")
                            )

                            NavigationBarItem(
                                selected = currentRoute == "shifts",
                                onClick = {
                                    if (currentRoute != "shifts") {
                                        navController.navigate("shifts") {
                                            popUpTo("dashboard")
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Műszak") },
                                label = { Text("Műszak") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = IceBlue,
                                    indicatorColor = IceBlue,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("nav_tab_shifts")
                            )

                            NavigationBarItem(
                                selected = currentRoute == "vacation",
                                onClick = {
                                    if (currentRoute != "vacation") {
                                        navController.navigate("vacation") {
                                            popUpTo("dashboard")
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.BeachAccess, contentDescription = "Szabi") },
                                label = { Text("Szabi") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = IceBlue,
                                    indicatorColor = IceBlue,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("nav_tab_vacation")
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(DarkBg)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(viewModel = viewModel)
                        }
                        composable("stats") {
                            StatsScreen(viewModel = viewModel)
                        }
                        composable("shifts") {
                            ShiftScreen(viewModel = viewModel)
                        }
                        composable("vacation") {
                            VacationScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
