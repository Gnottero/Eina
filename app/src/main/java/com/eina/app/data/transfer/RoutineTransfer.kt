package com.eina.app.data.transfer

import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.RoutineSetEntity
import com.eina.app.data.db.SetType
import com.eina.app.data.db.WeightType
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Routine exchange format: a JSON file that can be sent by message or email.
 *
 * Exercises travel with their English name, the library key, so the importing device reuses the
 * exercise it already has. The file still carries weight type, muscles and equipment, so an
 * exercise unknown to the library is recreated as a custom one instead of failing the import.
 *
 * No ids: they are local to the exporting database and mean nothing elsewhere.
 */
object RoutineTransfer {

    const val FORMAT = "eina.routine"
    // v3: a custom exercise travels whole — translations, bodyweight factor, logging instructions
    // and base64 image. Files at v1 and v2 are still readable.
    const val VERSION = 3
    const val MIME_TYPE = "application/json"
    const val FILE_EXTENSION = "json"

    data class ExercisePayload(
        val name: String,
        val weightType: WeightType,
        val muscleGroupsPrimary: List<String>,
        val muscleGroupsSecondary: List<String>,
        val equipment: String?,
        val description: String,
        val sets: List<SetPayload>,
        val restSeconds: Int,
        val notes: String?,
        /** Superset group number; null means the exercise stands alone. */
        val supersetGroup: Int?,
        /**
         * Exercise created by the exporting user and absent from any library: it cannot be matched
         * by name and must be recreated from the fields below.
         */
        val isCustom: Boolean = false,
        val nameIt: String? = null,
        val nameFr: String? = null,
        val descriptionIt: String? = null,
        val descriptionFr: String? = null,
        val loggingInstructions: String = "",
        val bodyweightFactor: Double = 1.0,
        /** Custom exercise image, if it existed and fit within the size cap. */
        val media: MediaPayload? = null
    )

    /** Custom exercise image inside the file: base64 bytes plus the original extension. */
    data class MediaPayload(val base64: String, val extension: String?)

    data class SetPayload(
        val targetReps: Int?,
        val targetWeight: Double?,
        val setType: SetType
    )

    data class RoutinePayload(
        val name: String,
        val notes: String?,
        val linkedPlaylistUri: String?,
        val linkedPlaylistType: PlaylistType?,
        val exercises: List<ExercisePayload>
    )

    fun encode(
        routine: RoutineEntity,
        routineExercises: List<RoutineExerciseEntity>,
        setsByRoutineExercise: Map<Long, List<RoutineSetEntity>>,
        exercisesById: Map<Long, ExerciseEntity>,
        /** Custom exercise images by exerciseId; the caller reads them from disk. */
        mediaByExerciseId: Map<Long, MediaPayload> = emptyMap()
    ): String {
        val exercises = JSONArray()
        routineExercises.sortedBy { it.order }.forEach { routineExercise ->
            val exercise = exercisesById[routineExercise.exerciseId] ?: return@forEach
            exercises.put(
                JSONObject().apply {
                    put("name", exercise.name)
                    put("weightType", exercise.weightType.name)
                    put("muscleGroupsPrimary", JSONArray(exercise.muscleGroupsPrimary))
                    put("muscleGroupsSecondary", JSONArray(exercise.muscleGroupsSecondary))
                    put("equipment", exercise.equipment ?: JSONObject.NULL)
                    put("description", exercise.description)
                    put(
                        "sets",
                        JSONArray().apply {
                            setsByRoutineExercise[routineExercise.id].orEmpty()
                                .sortedBy { it.setIndex }
                                .forEach { set ->
                                    put(
                                        JSONObject().apply {
                                            put("targetReps", set.targetReps ?: JSONObject.NULL)
                                            put("targetWeight", set.targetWeight ?: JSONObject.NULL)
                                            put("setType", set.setType.name)
                                        }
                                    )
                                }
                        }
                    )
                    put("restSeconds", routineExercise.restSeconds)
                    put("notes", routineExercise.notes ?: JSONObject.NULL)
                    put("supersetGroup", routineExercise.supersetGroup ?: JSONObject.NULL)
                    // A library exercise is matched by name; a custom one must be recreated as is,
                    // image included.
                    if (exercise.isCustom) {
                        put("isCustom", true)
                        put("nameIt", exercise.nameIt ?: JSONObject.NULL)
                        put("nameFr", exercise.nameFr ?: JSONObject.NULL)
                        put("descriptionIt", exercise.descriptionIt ?: JSONObject.NULL)
                        put("descriptionFr", exercise.descriptionFr ?: JSONObject.NULL)
                        put("loggingInstructions", exercise.loggingInstructions)
                        put("bodyweightFactor", exercise.bodyweightFactor)
                        mediaByExerciseId[exercise.id]?.let { media ->
                            put(
                                "media",
                                JSONObject().apply {
                                    put("data", media.base64)
                                    put("extension", media.extension ?: JSONObject.NULL)
                                }
                            )
                        }
                    }
                }
            )
        }

        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put(
                "routine",
                JSONObject().apply {
                    put("name", routine.name)
                    put("notes", routine.notes ?: JSONObject.NULL)
                    put("linkedPlaylistUri", routine.linkedPlaylistUri ?: JSONObject.NULL)
                    put("linkedPlaylistType", routine.linkedPlaylistType?.name ?: JSONObject.NULL)
                }
            )
            put("exercises", exercises)
        }.toString(2)
    }

    /**
     * Reads an exchange file. Returns null when it is not an Eina routine or was written by a
     * newer version of the format: better to say so than to import half a routine.
     */
    fun decode(json: String): RoutinePayload? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        if (root.optString("format") != FORMAT) return null
        if (root.optInt("version", 0) !in 1..VERSION) return null
        val routine = root.optJSONObject("routine") ?: return null
        val exercises = root.optJSONArray("exercises") ?: JSONArray()

        val payloads = (0 until exercises.length()).mapNotNull { index ->
            val item = exercises.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ExercisePayload(
                name = name,
                weightType = runCatching { WeightType.valueOf(item.optString("weightType")) }
                    .getOrDefault(WeightType.FREE_WEIGHT),
                muscleGroupsPrimary = item.optJSONArray("muscleGroupsPrimary").toStringList(),
                muscleGroupsSecondary = item.optJSONArray("muscleGroupsSecondary").toStringList(),
                equipment = item.optNullableString("equipment"),
                description = item.optString("description"),
                sets = item.readSets(),
                restSeconds = item.optInt("restSeconds", 90).coerceIn(0, 3600),
                notes = item.optNullableString("notes"),
                // Added after v1: an older file simply has no supersets.
                supersetGroup = item.optNullableInt("supersetGroup"),
                // v3 fields, absent in older files, where an unknown exercise still became custom
                // but with minimal data.
                isCustom = item.optBoolean("isCustom", false),
                nameIt = item.optNullableString("nameIt"),
                nameFr = item.optNullableString("nameFr"),
                descriptionIt = item.optNullableString("descriptionIt"),
                descriptionFr = item.optNullableString("descriptionFr"),
                loggingInstructions = item.optString("loggingInstructions"),
                bodyweightFactor = (item.optNullableDouble("bodyweightFactor") ?: 1.0).coerceIn(0.0, 1.0),
                media = item.optJSONObject("media")?.readMedia()
            )
        }

        return RoutinePayload(
            name = routine.optString("name"),
            notes = routine.optNullableString("notes"),
            linkedPlaylistUri = routine.optNullableString("linkedPlaylistUri"),
            linkedPlaylistType = routine.optNullableString("linkedPlaylistType")
                ?.let { type -> runCatching { PlaylistType.valueOf(type) }.getOrNull() },
            exercises = payloads
        )
    }

    /**
     * Exercise sets. From v2 on they are a list; a v1 file only carries targetSets/targetReps/
     * targetWeight and expands into that many identical normal sets.
     */
    private fun JSONObject.readSets(): List<SetPayload> {
        val array = optJSONArray("sets")
        if (array != null) {
            return (0 until array.length()).mapNotNull { index ->
                val set = array.optJSONObject(index) ?: return@mapNotNull null
                SetPayload(
                    targetReps = set.optNullableInt("targetReps")?.coerceIn(1, 999),
                    targetWeight = set.optNullableDouble("targetWeight"),
                    setType = runCatching { SetType.valueOf(set.optString("setType")) }
                        .getOrDefault(SetType.NORMAL)
                )
            }
        }
        val count = optInt("targetSets", 3).coerceIn(1, 20)
        val reps = optInt("targetReps", 10).coerceIn(1, 999)
        val weight = optNullableDouble("targetWeight")
        return List(count) { SetPayload(targetReps = reps, targetWeight = weight, setType = SetType.NORMAL) }
    }

    /** Attached image: if the base64 is unreadable the exercise still arrives, without it. */
    private fun JSONObject.readMedia(): MediaPayload? {
        val data = optString("data").takeIf { it.isNotBlank() } ?: return null
        return MediaPayload(base64 = data, extension = optNullableString("extension"))
    }

    /** Image bytes, or null when the file carries a broken base64 payload. */
    fun decodeMedia(media: MediaPayload): ByteArray? =
        runCatching { Base64.decode(media.base64, Base64.NO_WRAP) }.getOrNull()

    fun encodeMedia(bytes: ByteArray, extension: String?): MediaPayload =
        MediaPayload(base64 = Base64.encodeToString(bytes, Base64.NO_WRAP), extension = extension)

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name).takeIf { it.isNotBlank() } else null

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (has(name) && !isNull(name)) getDouble(name) else null

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (has(name) && !isNull(name)) getInt(name) else null
}
