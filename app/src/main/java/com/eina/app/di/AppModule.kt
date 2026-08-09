package com.eina.app.di

import androidx.room.Room
import com.eina.app.data.db.EinaDatabase
import com.eina.app.data.prefs.SettingsRepository
import com.eina.app.data.repository.RoutineRepository
import com.eina.app.data.repository.StatsRepository
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.data.health.HealthConnectSource
import com.eina.app.data.health.WorkoutHealthSync
import com.eina.app.data.seed.ExerciseSeeder
import com.eina.app.data.transfer.ExerciseMediaStore
import com.eina.app.ui.components.StopwatchController
import com.eina.app.ui.dashboard.DashboardViewModel
import com.eina.app.ui.feedback.WorkoutFeedback
import com.eina.app.ui.history.HistoryViewModel
import com.eina.app.ui.history.SessionDetailViewModel
import com.eina.app.ui.library.CreateExerciseViewModel
import com.eina.app.ui.library.ExerciseDetailViewModel
import com.eina.app.ui.library.LibraryViewModel
import com.eina.app.ui.progress.BodyWeightViewModel
import com.eina.app.ui.progress.ProgressViewModel
import com.eina.app.ui.routine.RoutineEditorViewModel
import com.eina.app.ui.routine.RoutineListViewModel
import com.eina.app.ui.settings.SettingsViewModel
import com.eina.app.ui.workout.ActiveWorkoutViewModel
import com.eina.app.ui.workout.RestTimerController
import com.eina.app.ui.workout.WorkoutViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(get(), EinaDatabase::class.java, EinaDatabase.DATABASE_NAME)
            .addMigrations(
                EinaDatabase.MIGRATION_1_2,
                EinaDatabase.MIGRATION_2_3,
                EinaDatabase.MIGRATION_3_4,
                EinaDatabase.MIGRATION_4_5,
                EinaDatabase.MIGRATION_5_6,
                EinaDatabase.MIGRATION_6_7,
                EinaDatabase.MIGRATION_7_8,
                EinaDatabase.MIGRATION_8_9
            )
            .build()
    }

    single { get<EinaDatabase>().exerciseDao() }
    single { get<EinaDatabase>().routineDao() }
    single { get<EinaDatabase>().routineExerciseDao() }
    single { get<EinaDatabase>().routineSetDao() }
    single { get<EinaDatabase>().workoutSessionDao() }
    single { get<EinaDatabase>().workoutExerciseDao() }
    single { get<EinaDatabase>().setEntryDao() }
    single { get<EinaDatabase>().bodyMetricDao() }
    single { get<EinaDatabase>().statsDao() }

    single {
        WorkoutRepository(
            workoutSessionDao = get(),
            workoutExerciseDao = get(),
            setEntryDao = get(),
            exerciseDao = get(),
            bodyMetricDao = get(),
            routineExerciseDao = get(),
            routineSetDao = get(),
            routineDao = get()
        )
    }

    single { ExerciseMediaStore(androidContext()) }

    single {
        RoutineRepository(
            routineDao = get(),
            routineExerciseDao = get(),
            routineSetDao = get(),
            exerciseDao = get(),
            mediaStore = get()
        )
    }

    single { StatsRepository(statsDao = get(), bodyMetricDao = get()) }

    single { SettingsRepository(androidContext()) }

    single { WorkoutFeedback(androidContext(), get()) }

    single { ExerciseSeeder(get(), get()) }

    // Dati dell'orologio: sorgente Health Connect e il pezzo che li attacca alla sessione.
    single { HealthConnectSource(androidContext()) }
    single { WorkoutHealthSync(sessionDao = get(), source = get(), settings = get()) }

    // Cronometro condiviso: si avvia in Dashboard e si ritrova durante l'allenamento.
    single { StopwatchController() }

    // Recupero condiviso: sopravvive all'uscita dalla schermata dell'allenamento in corso.
    single { RestTimerController(get()) }

    viewModel { WorkoutViewModel(get()) }
    viewModel { (sessionId: Long) -> ActiveWorkoutViewModel(get(), get(), get(), get(), sessionId) }
    viewModel { LibraryViewModel(get()) }
    viewModel { (exerciseId: Long) -> ExerciseDetailViewModel(get(), get(), exerciseId) }
    viewModel { CreateExerciseViewModel(get(), androidContext()) }
    viewModel { RoutineListViewModel(get(), get()) }
    viewModel { (routineId: Long) -> RoutineEditorViewModel(get(), androidContext(), routineId) }
    viewModel { DashboardViewModel(get()) }
    viewModel { ProgressViewModel(get()) }
    viewModel { BodyWeightViewModel(get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { (sessionId: Long) -> SessionDetailViewModel(get(), get(), get(), sessionId) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
