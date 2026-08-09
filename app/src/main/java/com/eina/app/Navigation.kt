package com.eina.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.eina.app.R
import com.eina.app.ui.components.IslandNavBar
import com.eina.app.ui.components.IslandNavItem
import com.eina.app.ui.dashboard.DashboardScreen
import com.eina.app.ui.history.HistoryScreen
import com.eina.app.ui.history.SessionDetailScreen
import com.eina.app.ui.library.CreateExerciseScreen
import com.eina.app.ui.library.ExerciseDetailScreen
import com.eina.app.ui.library.LibraryScreen
import com.eina.app.ui.progress.BodyWeightScreen
import com.eina.app.ui.progress.ProgressScreen
import com.eina.app.ui.routine.RoutineEditorScreen
import com.eina.app.ui.settings.SettingsScreen
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // La nav flottante resta solo sui 4 tab principali: nelle schermate di dettaglio
    // il contenuto usa tutta l'altezza (vedi allenamento in corso, con il suo timer).
    val showNavBar = bottomNavDestinations.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = EinaDestination.Dashboard.route
        ) {
            composable(EinaDestination.Dashboard.route) {
                DashboardScreen(
                    onStartWorkoutClick = { navController.navigate(EinaDestination.Workout.route) },
                    onHistoryClick = { navController.navigate("history") },
                    onSessionClick = { sessionId -> navController.navigate("history/session/$sessionId") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable(EinaDestination.Workout.route) {
                WorkoutScreen(
                    onSessionStarted = { sessionId ->
                        navController.navigate("workout/active/$sessionId")
                    },
                    onCreateRoutineClick = {
                        navController.navigate("routines/edit/0")
                    },
                    onEditRoutineClick = { routineId ->
                        navController.navigate("routines/edit/$routineId")
                    }
                )
            }
            composable(
                route = "routines/edit/{routineId}",
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
                RoutineEditorScreen(
                    routineId = routineId,
                    onBack = { navController.popBackStack() },
                    // Il nome dell'esercizio apre la sua scheda, come nell'allenamento.
                    onOpenExercise = { exerciseId -> navController.navigate("library/exercise/$exerciseId") },
                    // Salvata la routine si torna ad "Allena": l'editor e' un passaggio, non una destinazione.
                    onSaved = {
                        if (!navController.popBackStack(EinaDestination.Workout.route, inclusive = false)) {
                            navController.navigate(EinaDestination.Workout.route)
                        }
                    }
                )
            }
            composable(
                route = "workout/active/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                ActiveWorkoutScreen(
                    sessionId = sessionId,
                    // Uscire non chiude la sessione: resta aperta e si rientra da "Allena".
                    onExit = { navController.popBackStack() },
                    // Annullata, la sessione non esiste piu': si torna ad "Allena", non al riepilogo.
                    onCancelled = {
                        if (!navController.popBackStack(EinaDestination.Workout.route, inclusive = false)) {
                            navController.navigate(EinaDestination.Workout.route)
                        }
                    },
                    onOpenExercise = { exerciseId ->
                        navController.navigate("library/exercise/$exerciseId")
                    },
                    onFinished = {
                        // A fine allenamento si atterra sul riepilogo, con lo streak in evidenza
                        // e l'immagine da condividere a portata di header.
                        navController.popBackStack(EinaDestination.Workout.route, inclusive = false)
                        navController.navigate("history/session/$sessionId?justFinished=true")
                    }
                )
            }
            composable(EinaDestination.Library.route) {
                LibraryScreen(
                    onExerciseClick = { exerciseId ->
                        navController.navigate("library/exercise/$exerciseId")
                    },
                    onCreateExerciseClick = {
                        navController.navigate("library/create")
                    }
                )
            }
            composable("library/create") {
                CreateExerciseScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "library/exercise/{exerciseId}",
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: return@composable
                ExerciseDetailScreen(
                    exerciseId = exerciseId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("history") {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onSessionClick = { sessionId -> navController.navigate("history/session/$sessionId") }
                )
            }
            composable(
                route = "history/session/{sessionId}?justFinished={justFinished}",
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.LongType },
                    navArgument("justFinished") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                SessionDetailScreen(
                    sessionId = sessionId,
                    justFinished = backStackEntry.arguments?.getBoolean("justFinished") == true,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(EinaDestination.Progress.route) {
                ProgressScreen(
                    onBodyWeightClick = { navController.navigate("progress/bodyweight") },
                    onExerciseClick = { exerciseId -> navController.navigate("library/exercise/$exerciseId") }
                )
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("progress/bodyweight") {
                BodyWeightScreen(onBack = { navController.popBackStack() })
            }
        }

        AnimatedVisibility(
            visible = showNavBar,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            IslandNavBar(
                items = bottomNavDestinations.map { destination ->
                    IslandNavItem(
                        label = stringResource(destination.labelRes),
                        icon = iconFor(destination),
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            // Niente saveState/restoreState: salvavano anche le schermate di
                            // dettaglio aperte dal tab (Impostazioni, Storico) e al ritorno
                            // ripristinavano quelle in cima, per cui il tab sembrava irraggiungibile.
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = destination == EinaDestination.Dashboard
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            )
        }
    }
}

private fun iconFor(destination: EinaDestination) = when (destination) {
    EinaDestination.Dashboard -> Icons.Outlined.Home
    EinaDestination.Workout -> Icons.Outlined.FitnessCenter
    EinaDestination.Library -> Icons.AutoMirrored.Outlined.MenuBook
    EinaDestination.Progress -> Icons.Outlined.BarChart
}
