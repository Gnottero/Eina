package com.eina.app.di

import androidx.room.Room
import com.eina.app.data.db.EinaDatabase
import com.eina.app.data.prefs.SettingsRepository
import com.eina.app.data.repository.RoutineRepository
import com.eina.app.data.repository.StatsRepository
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.data.seed.ExerciseSeeder
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
import com.eina.app.ui.workout.WorkoutViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(get(), EinaDatabase::class.java, EinaDatabase.DATABASE_NAME)
            .addMigrations(EinaDatabase.MIGRATION_1_2)
            .build()
    }

    single { get<EinaDatabase>().exerciseDao() }
    single { get<EinaDatabase>().routineDao() }
    single { get<EinaDatabase>().routineExerciseDao() }
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
            routineDao = get()
        )
    }

    single { RoutineRepository(routineDao = get(), routineExerciseDao = get(), exerciseDao = get()) }

    single { StatsRepository(statsDao = get(), bodyMetricDao = get()) }

    single { SettingsRepository(androidContext()) }

    single { WorkoutFeedback(androidContext(), get()) }

    single { ExerciseSeeder(get(), get()) }

    viewModel { WorkoutViewModel(get()) }
    viewModel { (sessionId: Long) -> ActiveWorkoutViewModel(get(), get(), sessionId) }
    viewModel { LibraryViewModel(get()) }
    viewModel { (exerciseId: Long) -> ExerciseDetailViewModel(get(), exerciseId) }
    viewModel { CreateExerciseViewModel(get(), androidContext()) }
    viewModel { RoutineListViewModel(get(), get()) }
    viewModel { (routineId: Long) -> RoutineEditorViewModel(get(), routineId) }
    viewModel { DashboardViewModel(get()) }
    viewModel { ProgressViewModel(get()) }
    viewModel { BodyWeightViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { (sessionId: Long) -> SessionDetailViewModel(get(), sessionId) }
    viewModel { SettingsViewModel(get()) }
}
