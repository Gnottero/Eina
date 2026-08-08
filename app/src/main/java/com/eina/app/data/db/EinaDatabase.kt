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
    version = 6,
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

        /**
         * Descrizioni tradotte. Le colonne nascono vuote: le riempie ExerciseSeeder al
         * primo avvio successivo, allineando la libreria al catalogo curato.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN descriptionIt TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN descriptionFr TEXT")
            }
        }

        /**
         * Nomi tradotti, stessa storia delle descrizioni: colonne vuote alla migrazione,
         * le riempie ExerciseSeeder al primo avvio successivo.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN nameIt TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN nameFr TEXT")
            }
        }

        /**
         * Quota di peso corporeo sollevata (vedi ExerciseEntity.bodyweightFactor). Nasce a 1
         * per tutti — il valore giusto per gli esercizi che sollevano davvero il corpo — e
         * ExerciseSeeder porta gli altri al loro valore al primo avvio successivo. Il volume
         * gia' salvato nello storico non si tocca: si ricalcola al volo dalle set, quindi i
         * totali passati si aggiornano da soli.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN bodyweightFactor REAL NOT NULL DEFAULT 1.0")
            }
        }

        /**
         * `isWarmup` diventa `setType` (vedi [SetType]): il booleano sapeva dire solo
         * riscaldamento si'/no, e cedimento e drop set non ci entravano. La colonna va sostituita,
         * non aggiunta, quindi la tabella si ricrea: SQLite sotto API 30 non sa togliere colonne.
         * Le serie gia' registrate diventano WARMUP o NORMAL, senza perdere nulla.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE set_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        workoutExerciseId INTEGER NOT NULL,
                        setIndex INTEGER NOT NULL,
                        targetReps INTEGER,
                        actualReps INTEGER,
                        weight REAL,
                        restSecondsPlanned INTEGER NOT NULL,
                        setType TEXT NOT NULL,
                        completedAt INTEGER,
                        isPR INTEGER NOT NULL,
                        bodyweightSnapshotKg REAL,
                        FOREIGN KEY(workoutExerciseId) REFERENCES workout_exercises(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO set_entries_new (
                        id, workoutExerciseId, setIndex, targetReps, actualReps, weight,
                        restSecondsPlanned, setType, completedAt, isPR, bodyweightSnapshotKg
                    )
                    SELECT id, workoutExerciseId, setIndex, targetReps, actualReps, weight,
                        restSecondsPlanned,
                        CASE isWarmup WHEN 1 THEN 'WARMUP' ELSE 'NORMAL' END,
                        completedAt, isPR, bodyweightSnapshotKg
                    FROM set_entries
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE set_entries")
                db.execSQL("ALTER TABLE set_entries_new RENAME TO set_entries")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_set_entries_workoutExerciseId ON set_entries (workoutExerciseId)")
            }
        }
    }
}
