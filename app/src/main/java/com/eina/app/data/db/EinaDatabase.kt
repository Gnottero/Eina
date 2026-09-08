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
    version = 12,
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
         * Per-exercise notes, added to both the routine template and the session, so a note can be
         * edited during a workout without touching the routine. Migrated rather than recreated:
         * the workout history cannot be rebuilt.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN notes TEXT")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN notes TEXT")
            }
        }

        /**
         * Translated descriptions. The columns start empty and are filled by ExerciseSeeder on the
         * next launch, when it aligns the library with the curated catalog.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN descriptionIt TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN descriptionFr TEXT")
            }
        }

        /** Translated names; like the descriptions, filled by ExerciseSeeder on the next launch. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN nameIt TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN nameFr TEXT")
            }
        }

        /**
         * Lifted bodyweight share (see ExerciseEntity.bodyweightFactor). It starts at 1 for every
         * exercise — the right value for those that do lift the body — and ExerciseSeeder corrects
         * the others on the next launch. Stored volume is untouched: it is recomputed from the
         * sets, so past totals fix themselves.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN bodyweightFactor REAL NOT NULL DEFAULT 1.0")
            }
        }

        /**
         * `isWarmup` becomes `setType` (see [SetType]): the boolean could only say warmup yes/no,
         * with no room for failure and drop sets. The column is replaced rather than added, so the
         * table is recreated — SQLite below API 30 cannot drop columns. Recorded sets become
         * WARMUP or NORMAL, losing nothing.
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
         * Supersets: a group number on both the template and the session, so a round can be
         * composed in the routine and adjusted during the workout. It starts null, so no stored
         * exercise joins a superset on its own.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN supersetGroup INTEGER")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN supersetGroup INTEGER")
            }
        }

        /**
         * Routine sets become rows (`routine_sets`), one per set with its own type, so a routine
         * can express "one warmup and two failure sets" instead of a plain set count. The three
         * target columns on the routine are dropped, so `routine_exercises` is recreated — SQLite
         * below API 30 cannot drop columns. Every stored exercise produces its `targetSets` NORMAL
         * rows with the values it had, so no routine changes shape.
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
                // One row per planned set: the recursive CTE counts from 0 to targetSets-1.
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
         * Rest becomes a column of the session exercise. Recorded sessions take it from their
         * first set, which is exactly where the UI used to read it from.
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

        /**
         * A free comment on the workout, written from the summary. A new nullable column, so
         * recorded workouts stay as they were.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN notes TEXT")
            }
        }

        /**
         * The routine load is a property of each planned set. Previously a session retained only
         * reps and reused the first working-set load for every row, so a lighter warmup immediately
         * received the working weight. Existing sessions keep the old fallback through a nullable
         * column; newly created sessions preserve each exact target.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_entries ADD COLUMN targetWeight REAL")
            }
        }

        /**
         * Watch data on the session: average and maximum heart rate, estimated calories and the
         * sample series behind the summary chart. New nullable columns, so recorded workouts stay
         * as they were.
         */
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
