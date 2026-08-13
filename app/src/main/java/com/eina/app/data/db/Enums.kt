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
 * Nature of a single set. Warmups stay out of volume, PRs and rest; failure and drop sets are work
 * sets in every respect, and their marker only helps reading the history.
 */
enum class SetType { WARMUP, NORMAL, FAILURE, DROP }

/** A warmup is not work: no volume, no PR, no rest timer. */
val SetType.countsAsWorking: Boolean
    get() = this != SetType.WARMUP

enum class PlaylistType { SPOTIFY, YOUTUBE_MUSIC }
