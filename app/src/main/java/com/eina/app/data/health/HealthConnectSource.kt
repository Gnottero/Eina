package com.eina.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.eina.app.data.db.HeartRateSample
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What the watch measured during the workout, read from Health Connect.
 *
 * The app does not talk to the watch but to the store its companion app (Galaxy Wear, Fitbit,
 * Zepp, Google Health Connect…) writes to. That works with any brand without a companion app and
 * stays local-first: read-only access, restricted to the workout window.
 *
 * If Health Connect is missing or the permission was not granted, every read returns null.
 */
class HealthConnectSource(private val context: Context) {

    /** Heart rate plus both calorie records: not every provider writes the active ones. */
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient?
        get() = if (isAvailable) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null

    /** True only with all three permissions: a partial grant yields incomplete readings. */
    suspend fun hasPermissions(): Boolean = withContext(Dispatchers.IO) {
        hasPermissions(client)
    }

    /**
     * Same check with the client already at hand: the [client] getter re-checks the SDK status and
     * calls `getOrCreate` on every access.
     */
    private suspend fun hasPermissions(healthClient: HealthConnectClient?): Boolean {
        val granted = runCatching {
            healthClient?.permissionController?.getGrantedPermissions()
        }.getOrNull().orEmpty()
        return permissions.all { it in granted }
    }

    /**
     * Heart rate and calories between [start] and [end]. Returns null when there is nothing to
     * read: no provider, no permission, or a workout done without a watch.
     */
    suspend fun readWorkoutVitals(start: Long, end: Long): WorkoutVitals? = withContext(Dispatchers.IO) {
        if (end <= start) return@withContext null
        val healthClient = client ?: return@withContext null
        if (!hasPermissions(healthClient)) return@withContext null

        // Some companion apps timestamp their records wrong: Mi Fitness writes them 90 minutes
        // ahead (a 08:00 workout showed up at 09:30 with identical values), so the exact window
        // finds nothing. [detectOffset] measures the shift; when data sits where it should, the
        // offset is zero and nothing changes.
        val (offset, coverage) = detectOffset(healthClient, start, end)
        val shiftedStart = start + offset
        val shiftedEnd = end + offset

        // The real workout window: the same one Health Connect aggregates over, so average, max,
        // calories and chart stay comparable with what the system app shows.
        val exactWindow = TimeRangeFilter.between(
            Instant.ofEpochMilli(shiftedStart),
            Instant.ofEpochMilli(shiftedEnd)
        )
        // Fallback with fifteen minutes of padding, used only when the exact window holds no
        // sample at all: bands align their blocks to the half hour and a short workout can fall
        // between two measurements. Always padding would fold into the average half an hour the
        // workout does not contain.
        val paddedWindow = TimeRangeFilter.between(
            Instant.ofEpochMilli(shiftedStart - WINDOW_PADDING_MS),
            Instant.ofEpochMilli(shiftedEnd + WINDOW_PADDING_MS)
        )

        // Numbers come from aggregation, not from summing records by hand: aggregation clips
        // records straddling the window (calorie records often span a whole day) and deduplicates
        // by app priority, so a watch and a phone both writing calories are not counted twice.
        // Averaging samples by hand would also weight the result by sampling density, not by time.
        suspend fun aggregateHeart(range: TimeRangeFilter) = runCatching {
            healthClient.aggregate(
                AggregateRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX),
                    timeRangeFilter = range
                )
            )
        }.getOrNull()

        // Exact window first; padding only if it is empty, and then the chart samples use it too,
        // so numbers and graph always describe the same interval.
        val exactHeart = aggregateHeart(exactWindow)
        val usesPadding = exactHeart?.get(HeartRateRecord.BPM_AVG) == null
        val heartAggregate = if (usesPadding) aggregateHeart(paddedWindow) else exactHeart
        val heartWindow = if (usesPadding) paddedWindow else exactWindow
        val heartFrom = if (usesPadding) shiftedStart - WINDOW_PADDING_MS else shiftedStart
        val heartTo = if (usesPadding) shiftedEnd + WINDOW_PADDING_MS else shiftedEnd
        val energyAggregate = runCatching {
            healthClient.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = exactWindow
                )
            )
        }.getOrNull()

        val avgBpm = heartAggregate?.get(HeartRateRecord.BPM_AVG)?.toInt()
        val maxBpm = heartAggregate?.get(HeartRateRecord.BPM_MAX)?.toInt()
        // Active calories win over total ones, which include basal metabolism and would credit
        // the workout with calories burned at rest.
        val activeKcal = energyAggregate?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories
        val rawKcal = activeKcal?.takeIf { it > 0.0 }
            ?: energyAggregate?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories

        // Calories are a sum, so a watch that measured only part of the workout yields a true but
        // truncated number — ninety minutes of training reported as "2 kcal" because the window
        // grazed the last data block. Below half coverage nothing is shown. The coverage comes
        // already measured from [detectOffset], which had to read the same intervals.
        val kcal = rawKcal?.takeIf { coverage >= MIN_COVERAGE }

        // The chart needs single samples, which aggregation does not provide. Only one source is
        // kept — the densest one, i.e. the watch — otherwise two providers draw overlapping lines.
        val byOrigin = runCatching {
            healthClient.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = heartWindow))
                .records
                .groupBy { it.metadata.dataOrigin.packageName }
                .mapValues { (_, records) ->
                    records
                        .flatMap { record -> record.samples }
                        .map { HeartRateSample(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) }
                        .sortedBy { it.timeMillis }
                        // Blocks of the same source overlap at their edges and repeat samples.
                        .distinctBy { it.timeMillis }
                }
        }.getOrDefault(emptyMap())

        val inWindow = byOrigin.mapValues { (_, samples) ->
            samples.filter { it.timeMillis in heartFrom..heartTo }
        }
        // Samples are moved back to the workout clock, not the one the companion app used.
        val samples = inWindow.values.maxByOrNull { it.size }.orEmpty()
            .map { if (offset == 0L) it else it.copy(timeMillis = it.timeMillis - offset) }

        // A couple of samples do not make an average: on a watch sampling once a minute that is
        // two minutes of workout.
        val hasHeartRate = samples.size >= MIN_HR_SAMPLES
        if (!hasHeartRate && kcal == null) return@withContext null
        WorkoutVitals(
            samples = if (hasHeartRate) samples else emptyList(),
            avgBpm = avgBpm?.takeIf { hasHeartRate },
            maxBpm = maxBpm?.takeIf { hasHeartRate },
            kcal = kcal?.takeIf { it > 0.0 }
        )
    }

    /**
     * How far the watch data is shifted from the phone clock.
     *
     * Zero in the normal case: the exact window is tried first and, if covered, returned at once.
     * Otherwise the search widens to a day around the workout, and each data block start is
     * aligned with the workout start. The candidate offset is rounded to the quarter hour — every
     * time zone is a multiple of fifteen minutes — and the offset covering most of the workout
     * wins, the smallest one on a tie. If not even the best covers half the workout the offset
     * stays zero, i.e. no data rather than someone's walk from another hour.
     *
     * Also returns the coverage reached with the chosen offset, the same figure that decides
     * whether the calories are worth showing.
     */
    private suspend fun detectOffset(client: HealthConnectClient, start: Long, end: Long): DataOffset {
        val exact = runCatching {
            coveredShare(
                readEnergyIntervals(
                    client,
                    TimeRangeFilter.between(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end))
                ),
                start,
                end
            )
        }.getOrDefault(0.0)
        if (exact >= MIN_COVERAGE) return DataOffset(0L, exact)

        val search = TimeRangeFilter.between(
            Instant.ofEpochMilli(start - MAX_OFFSET_MS),
            Instant.ofEpochMilli(end + MAX_OFFSET_MS)
        )
        val intervals = runCatching { readEnergyIntervals(client, search) }.getOrDefault(emptyList())
        if (intervals.isEmpty()) return DataOffset(0L, exact)

        var best = 0L
        var bestCoverage = exact
        intervals.distinctBy { it.first }.forEach { (blockStart, _) ->
            val candidate = ((blockStart - start).toDouble() / QUARTER_HOUR_MS).roundToLong() * QUARTER_HOUR_MS
            if (candidate == 0L || abs(candidate) > MAX_OFFSET_MS) return@forEach
            val coverage = coveredShare(intervals, start + candidate, end + candidate)
            val better = coverage > bestCoverage + 1e-6 ||
                (coverage > bestCoverage - 1e-6 && abs(candidate) < abs(best))
            if (better) {
                best = candidate
                bestCoverage = coverage
            }
        }
        // Below the threshold nothing moves: offset zero, and the reported coverage is the one of
        // the real window, not of the rejected offset.
        return if (bestCoverage >= MIN_COVERAGE) DataOffset(best, bestCoverage) else DataOffset(0L, exact)
    }

    /**
     * Intervals the watch wrote calories over: they trace when it was measuring, and serve both to
     * compute coverage and to locate data stamped elsewhere. Total calories are the fallback for
     * providers that do not write active ones.
     */
    private suspend fun readEnergyIntervals(
        client: HealthConnectClient,
        range: TimeRangeFilter
    ): List<Pair<Long, Long>> {
        val active = client
            .readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRangeFilter = range))
            .records.map { it.startTime.toEpochMilli() to it.endTime.toEpochMilli() }
        return active.ifEmpty {
            client
                .readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRangeFilter = range))
                .records.map { it.startTime.toEpochMilli() to it.endTime.toEpochMilli() }
        }
    }
}

/**
 * Where the watch data sits relative to the workout: [offset] is the shift to apply to the window,
 * [coverage] the share of the workout measured with that shift.
 */
private data class DataOffset(val offset: Long, val coverage: Double)

/** Padding applied to the heart rate window; see [HealthConnectSource.readWorkoutVitals]. */
private const val WINDOW_PADDING_MS = 15 * 60 * 1000L

/** Rounding step for the detected data offset; see detectOffset. */
private const val QUARTER_HOUR_MS = 15 * 60 * 1000L

/** How far to look for data when the workout window holds none. */
private const val MAX_OFFSET_MS = 24 * 60 * 60 * 1000L

/** Share of the workout the watch must have measured for the calories to be meaningful. */
private const val MIN_COVERAGE = 0.5

/** Minimum samples for average and maximum to mean anything. */
private const val MIN_HR_SAMPLES = 3

/** Share of the window covered by the intervals, counting overlaps once. */
private fun coveredShare(intervals: List<Pair<Long, Long>>, start: Long, end: Long): Double {
    val window = (end - start).toDouble()
    if (window <= 0.0) return 0.0
    var covered = 0L
    var cursor = start
    intervals
        .map { (from, to) -> maxOf(from, start) to minOf(to, end) }
        .filter { (from, to) -> to > from }
        .sortedBy { it.first }
        .forEach { (from, to) ->
            val begin = maxOf(from, cursor)
            if (to > begin) {
                covered += to - begin
                cursor = to
            }
        }
    return covered / window
}

/** Watch data for a session, already reduced to what the summary shows. */
data class WorkoutVitals(
    val samples: List<HeartRateSample>,
    val avgBpm: Int?,
    val maxBpm: Int?,
    val kcal: Double?
)
