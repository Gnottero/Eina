package com.eina.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eina.app.ui.dashboard.DashboardScreen
import com.eina.app.ui.library.LibraryScreen
import com.eina.app.ui.progress.ProgressScreen
import com.eina.app.ui.workout.ActiveWorkoutScreen
import com.eina.app.ui.workout.WorkoutScreen

sealed class EinaDestination(val route: String, val labelRes: Int) {
    data object Dashboard : EinaDestination("dashboard", R.string.nav_dashboard)
    data object Workout : EinaDestination("workout", R.string.nav_workout)
    data object Library : EinaDestination("library", R.string.nav_library)
    data object Progress : EinaDestination("progress", R.string.nav_progress)
}

private val bottomNavDestinations = listOf(
    EinaDestination.Dashboard,
    EinaDestination.Workout,
    EinaDestination.Library,
    EinaDestination.Progress
)

@Composable
fun EinaNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconFor(destination), contentDescription = stringResource(destination.labelRes)) },
                        label = { androidx.compose.material3.Text(stringResource(destination.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = EinaDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(EinaDestination.Dashboard.route) { DashboardScreen() }
            composable(EinaDestination.Workout.route) {
                WorkoutScreen(onSessionStarted = { sessionId ->
                    navController.navigate("workout/active/$sessionId")
                })
            }
            composable(
                route = "workout/active/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                ActiveWorkoutScreen(
                    sessionId = sessionId,
                    onFinished = {
                        navController.popBackStack(EinaDestination.Workout.route, inclusive = false)
                    }
                )
            }
            composable(EinaDestination.Library.route) { LibraryScreen() }
            composable(EinaDestination.Progress.route) { ProgressScreen() }
        }
    }
}

private fun iconFor(destination: EinaDestination) = when (destination) {
    EinaDestination.Dashboard -> Icons.Outlined.Home
    EinaDestination.Workout -> Icons.Outlined.FitnessCenter
    EinaDestination.Library -> Icons.Outlined.FavoriteBorder
    EinaDestination.Progress -> Icons.Outlined.DateRange
}
