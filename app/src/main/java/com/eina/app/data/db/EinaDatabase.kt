package com.eina.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutExerciseEntity::class,
        SetEntryEntity::class,
        BodyMetricEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EinaDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun setEntryDao(): SetEntryDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun statsDao(): StatsDao

    companion object {
        const val DATABASE_NAME = "eina.db"

        /**
         * Note per esercizio: colonna aggiunta sia al template (routine) sia alla sessione, cosi'
         * la nota puo' essere ritoccata durante l'allenamento senza sporcare la routine.
         * Migrazione e non distruttiva: lo storico degli allenamenti non e' ricostruibile.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN notes TEXT")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN notes TEXT")
            }
        }
    }
}
