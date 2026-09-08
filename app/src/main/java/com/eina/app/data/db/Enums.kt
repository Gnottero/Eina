package com.eina.app.data.db

enum class WeightType {
    FREE_WEIGHT, BODYWEIGHT, BODYWEIGHT_PLUS_LOAD, ASSISTED, MACHINE_STACK, TIME_BASED,

    /**
     * Treadmill, bike, elliptical: there is no load to lift, distance and time are recorded.
     *
     * DECISIONE: no new column on the sets. Just as `actualReps` already holds the seconds of
     * timed exercises, here `weight` holds kilometres and `actualReps` minutes — same table, only
     * labels and reading change. Minutes and not seconds, because a treadmill session is written
     * as "30", not "1800".
     */
    DISTANCE_BASED
}

/**
 * Whether the set has a load to type. For bodyweight the weight is the user's own (from
 * `bodyweightSnapshotKg`), for timed exercises there is none, and for distance the decimal field
 * holds kilometres: in all these cases the kg column disappears from the table.
 */
val WeightType.usesWeight: Boolean
    get() = this != WeightType.BODYWEIGHT &&
        this != WeightType.TIME_BASED &&
        this != WeightType.DISTANCE_BASED

/** For timed exercises `actualReps` holds seconds: the label changes, the field does not. */
val WeightType.usesDuration: Boolean
    get() = this == WeightType.TIME_BASED

/** For distance exercises `weight` holds kilometres and `actualReps` minutes. */
val WeightType.usesDistance: Boolean
    get() = this == WeightType.DISTANCE_BASED

/** Whether the table shows the decimal field: kg for loads, km for distance. */
val WeightType.usesDecimalField: Boolean
    get() = usesWeight || usesDistance

/**
 * Nature of a single set. Every type is lifted weight: volume, records and rest count a warmup like
 * any other set, because the kilograms are on the bar either way. The type only changes how the row
 * reads — a warmup carries its letter instead of a number in the working sequence.
 */
enum class SetType { WARMUP, NORMAL, FAILURE, DROP }

/**
 * Whether the set takes a number in the working sequence. A warmup does not, so a warmup in the
 * middle does not steal the number from the set after it. This says nothing about volume, records
 * or rest, which count every set.
 */
val SetType.countsAsWorking: Boolean
    get() = this != SetType.WARMUP

enum class PlaylistType { SPOTIFY, YOUTUBE_MUSIC }
