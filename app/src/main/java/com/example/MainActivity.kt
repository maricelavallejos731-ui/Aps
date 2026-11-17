package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AdminUsersScreen
import com.example.ui.screens.CalendarAndResultsScreen
import com.example.ui.screens.LiveTimingScreen
import com.example.ui.screens.SectorsAndCircuitsScreen
import com.example.ui.theme.ApexCrimson
import com.example.ui.theme.ApexRacingTimingTheme
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TelemetryCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ApexTimingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApexRacingTimingTheme {
                ApexMainScaffold()
            }
        }
    }
}

private enum class ApexRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    LIVE_TIMING("live_timing", "Live", Icons.Filled.Timer, Icons.Outlined.Timer, "nav_live_timing"),
    SECTORS("sectors", "Sectores", Icons.Filled.Map, Icons.Outlined.Map, "nav_sectors"),
    CALENDAR_RESULTS("calendar_results", "Copa/GP", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_calendar_results"),
    PRIVATE_ACCESS("private_access", "Privado", Icons.Filled.Lock, Icons.Outlined.Lock, "nav_private_access")
}

@Composable
fun ApexMainScaffold() {
    val viewModel: ApexTimingViewModel = viewModel(factory = ApexTimingViewModel.factory())
    val isUnlocked by viewModel.isMasterUnlocked.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(viewModel) {
        viewModel.uiMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = CarbonSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        ApexRoute.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val icon = when {
                                dest == ApexRoute.PRIVATE_ACCESS && isUnlocked ->
                                    if (selected) Icons.Filled.LockOpen else Icons.Outlined.LockOpen
                                selected -> dest.selectedIcon
                                else -> dest.unselectedIcon
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ApexCrimson,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = TelemetryCardElevated,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = CarbonSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        ApexRoute.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val icon = when {
                                dest == ApexRoute.PRIVATE_ACCESS && isUnlocked ->
                                    if (selected) Icons.Filled.LockOpen else Icons.Outlined.LockOpen
                                selected -> dest.selectedIcon
                                else -> dest.unselectedIcon
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ApexCrimson,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = TelemetryCardElevated,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ApexRoute.LIVE_TIMING.route
                    ) {
                        composable(ApexRoute.LIVE_TIMING.route) {
                            LiveTimingScreen(viewModel = viewModel, isWideScreen = isWideScreen)
                        }
                        composable(ApexRoute.SECTORS.route) {
                            SectorsAndCircuitsScreen(viewModel = viewModel, isWideScreen = isWideScreen)
                        }
                        composable(ApexRoute.CALENDAR_RESULTS.route) {
                            CalendarAndResultsScreen(viewModel = viewModel)
                        }
                        composable(ApexRoute.PRIVATE_ACCESS.route) {
                            AdminUsersScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
