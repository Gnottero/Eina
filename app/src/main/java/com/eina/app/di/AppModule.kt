package com.eina.app.di

import androidx.room.Room
import com.eina.app.data.db.EinaDatabase
import com.eina.app.data.repository.WorkoutRepository
import com.eina.app.data.seed.ExerciseSeeder
import com.eina.app.ui.library.CreateExerciseViewModel
import com.eina.app.ui.library.ExerciseDetailViewModel
import com.eina.app.ui.library.LibraryViewModel
import com.eina.app.ui.workout.ActiveWorkoutViewModel
import com.eina.app.ui.workout.WorkoutViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(get(), EinaDatabase::class.java, EinaDatabase.DATABASE_NAME).build()
    }

    single { get<EinaDatabase>().exerciseDao() }
    single { get<EinaDatabase>().routineDao() }
    single { get<EinaDatabase>().routineExerciseDao() }
    single { get<EinaDatabase>().workoutSessionDao() }
    single { get<EinaDatabase>().workoutExerciseDao() }
    single { get<EinaDatabase>().setEntryDao() }
    single { get<EinaDatabase>().bodyMetricDao() }

    single {
        WorkoutRepository(
            workoutSessionDao = get(),
            workoutExerciseDao = get(),
            setEntryDao = get(),
            exerciseDao = get(),
            bodyMetricDao = get()
        )
    }

    single { ExerciseSeeder(get(), get()) }

    viewModel { WorkoutViewModel(get()) }
    viewModel { (sessionId: Long) -> ActiveWorkoutViewModel(get(), sessionId) }
    viewModel { LibraryViewModel(get()) }
    viewModel { (exerciseId: Long) -> ExerciseDetailViewModel(get(), exerciseId) }
    viewModel { CreateExerciseViewModel(get(), androidContext()) }
}
