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
        RoutineSetEntity::class,
        WorkoutSessionEntity::class,
        WorkoutExerciseEntity::class,
        SetEntryEntity::class,
        BodyMetricEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EinaDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao
    abstract fun routineSetDao(): RoutineSetDao
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

        /**
         * Superset: un numero di gruppo sul template e sulla sessione, cosi' il giro si puo'
         * comporre nella routine e ritoccare durante l'allenamento. Nasce a null — nessun
         * esercizio gia' salvato entra in un superset senza che glielo si chieda.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN supersetGroup INTEGER")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN supersetGroup INTEGER")
            }
        }

        /**
         * Le serie della routine diventano righe (`routine_sets`), una per serie, col loro tipo:
         * la scheda puo' finalmente dire "un riscaldamento e due serie a cedimento" invece del
         * solo numero di serie. Le tre colonne target sulla routine non servono piu' e vanno
         * tolte, quindi `routine_exercises` si ricrea — SQLite sotto API 30 non sa togliere
         * colonne. Ogni esercizio gia' salvato produce le sue `targetSets` righe NORMAL coi
         * valori che aveva: nessuna scheda cambia forma.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_sets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        routineExerciseId INTEGER NOT NULL,
                        setIndex INTEGER NOT NULL,
                        targetReps INTEGER,
                        targetWeight REAL,
                        setType TEXT NOT NULL,
                        FOREIGN KEY(routineExerciseId) REFERENCES routine_exercises(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_routine_sets_routineExerciseId ON routine_sets (routineExerciseId)"
                )
                // Una riga per serie pianificata: la CTE ricorsiva conta da 0 a targetSets-1.
                db.execSQL(
                    """
                    INSERT INTO routine_sets (routineExerciseId, setIndex, targetReps, targetWeight, setType)
                    SELECT re.id, seq.n, re.targetReps, re.targetWeight, 'NORMAL'
                    FROM routine_exercises re
                    JOIN (
                        WITH RECURSIVE seq(n) AS (
                            SELECT 0 UNION ALL SELECT n + 1 FROM seq WHERE n < 99
                        )
                        SELECT n FROM seq
                    ) seq ON seq.n < re.targetSets
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE routine_exercises_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        routineId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        `order` INTEGER NOT NULL,
                        restSeconds INTEGER NOT NULL,
                        notes TEXT,
                        supersetGroup INTEGER,
                        FOREIGN KEY(routineId) REFERENCES routines(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id)
                            ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO routine_exercises_new (
                        id, routineId, exerciseId, `order`, restSeconds, notes, supersetGroup
                    )
                    SELECT id, routineId, exerciseId, `order`, restSeconds, notes, supersetGroup
                    FROM routine_exercises
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE routine_exercises")
                db.execSQL("ALTER TABLE routine_exercises_new RENAME TO routine_exercises")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_routineId ON routine_exercises (routineId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_exerciseId ON routine_exercises (exerciseId)")
            }
        }

        /**
         * Dati dell'orologio sulla sessione: frequenza media e massima, calorie stimate e la
         * serie dei battiti per la spezzata del riepilogo. Colonne nuove e nullable — gli
         * allenamenti gia' registrati non hanno niente da leggere e restano com'erano.
         */
        /**
         * Il recupero diventa una colonna dell'esercizio di sessione. Le sessioni gia' registrate
         * lo prendono dalla loro prima serie, che e' esattamente da dove lo leggeva la UI.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN restSeconds INTEGER NOT NULL DEFAULT 90")
                db.execSQL(
                    """
                    UPDATE workout_exercises SET restSeconds = COALESCE(
                        (SELECT restSecondsPlanned FROM set_entries
                         WHERE workoutExerciseId = workout_exercises.id
                         ORDER BY setIndex ASC LIMIT 1),
                        90
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN avgHeartRateBpm INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN maxHeartRateBpm INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN caloriesKcal REAL")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN heartRateSamples TEXT")
            }
        }
    }
}
