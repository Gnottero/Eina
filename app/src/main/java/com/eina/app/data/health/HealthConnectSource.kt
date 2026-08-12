package com.eina.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.eina.app.data.db.HeartRateSample
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Quel che l'orologio ha misurato durante l'allenamento, letto da Health Connect.
 *
 * Eina non parla con lo smartwatch: parla con il magazzino dove l'app dell'orologio (Galaxy Wear,
 * Fitbit, Zepp, Health Connect di Google…) deposita i suoi dati. Cosi' funziona con qualunque
 * orologio senza scrivere un'app companion, e resta local-first — nessuna rete, nessun account:
 * i dati sono gia' sul telefono e l'app li legge in sola lettura, sulla sola finestra
 * dell'allenamento.
 *
 * Se Health Connect non c'e' (telefono vecchio, provider non installato) o il permesso non e'
 * stato dato, ogni lettura torna null e l'app va avanti come prima.
 */
class HealthConnectSource(private val context: Context) {

    /** Permessi richiesti: battiti piu' le due forme di calorie, perche' non tutti scrivono le attive. */
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient?
        get() = if (isAvailable) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null

    /** Vero solo se ci sono tutti e tre: con un permesso a meta' la lettura sarebbe monca. */
    suspend fun hasPermissions(): Boolean = withContext(Dispatchers.IO) {
        val granted = runCatching {
            client?.permissionController?.getGrantedPermissions()
        }.getOrNull().orEmpty()
        permissions.all { it in granted }
    }

    /**
     * Battiti e calorie fra [start] ed [end]. Ritorna null se non c'e' niente da leggere: nessun
     * provider, nessun permesso, o semplicemente un allenamento fatto senza orologio al polso.
     *
     * Le calorie attive hanno la precedenza sulle totali: quelle totali comprendono il metabolismo
     * basale, e sommarlo direbbe che si bruciano calorie stando fermi — vero, ma non e'
     * l'allenamento.
     */
    suspend fun readWorkoutVitals(start: Long, end: Long): WorkoutVitals? = withContext(Dispatchers.IO) {
        if (end <= start) return@withContext null
        val healthClient = client ?: return@withContext null
        if (!hasPermissions()) return@withContext null

        // La finestra si interroga allargata di un margine, poi si ritaglia a mano. Le braccialette
        // non scrivono un record che comincia e finisce con l'allenamento: depositano blocchi
        // sincronizzati a pacchetti, e le calorie spesso come un unico record lungo (a volte
        // dell'intera giornata). Chiedendo la finestra esatta, un allenamento breve trovava zero
        // record e il riepilogo restava vuoto pur avendo i dati sul telefono.
        val range = TimeRangeFilter.between(
            Instant.ofEpochMilli(start - QUERY_MARGIN_MS),
            Instant.ofEpochMilli(end + QUERY_MARGIN_MS)
        )

        val allSamples = runCatching {
            healthClient.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = range))
                .records
                .flatMap { record -> record.samples }
                .map { HeartRateSample(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) }
                .sortedBy { it.timeMillis }
        }.getOrDefault(emptyList())

        // Prima i battiti davvero dentro l'allenamento; solo se non ce n'e' nemmeno uno si tiene
        // quel che cade nel margine — meglio un battito misurato un minuto prima che niente su un
        // allenamento piu' corto dell'intervallo di campionamento della bracciale.
        val samples = allSamples.filter { it.timeMillis in start..end }.ifEmpty { allSamples }

        val activeKcal = runCatching {
            healthClient.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRangeFilter = range))
                .records
                .sumOf { it.energy.inKilocalories.overlapShare(it.startTime, it.endTime, start, end) }
        }.getOrDefault(0.0)

        val kcal = if (activeKcal > 0.0) {
            activeKcal
        } else {
            runCatching {
                healthClient.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRangeFilter = range))
                    .records
                    .sumOf { it.energy.inKilocalories.overlapShare(it.startTime, it.endTime, start, end) }
            }.getOrDefault(0.0)
        }

        if (samples.isEmpty() && kcal <= 0.0) return@withContext null
        WorkoutVitals(
            samples = samples,
            avgBpm = samples.takeIf { it.isNotEmpty() }?.map { it.bpm }?.average()?.toInt(),
            maxBpm = samples.maxOfOrNull { it.bpm },
            kcal = kcal.takeIf { it > 0.0 }
        )
    }
}

/** Margine con cui si allarga la finestra di lettura: vedi [HealthConnectSource.readWorkoutVitals]. */
private const val QUERY_MARGIN_MS = 10 * 60 * 1000L

/**
 * La quota di calorie di un record che ricade davvero nell'allenamento, in proporzione al tempo:
 * un record lungo (o giornaliero) non si somma intero, altrimenti dieci minuti di palestra
 * direbbero le calorie della giornata. Un record istantaneo o piu' corto della finestra entra tutto.
 */
private fun Double.overlapShare(
    recordStart: Instant,
    recordEnd: Instant,
    windowStart: Long,
    windowEnd: Long
): Double {
    val from = recordStart.toEpochMilli()
    val to = recordEnd.toEpochMilli()
    val duration = to - from
    if (duration <= 0L) return if (from in windowStart..windowEnd) this else 0.0
    val overlap = minOf(to, windowEnd) - maxOf(from, windowStart)
    if (overlap <= 0L) return 0.0
    return this * (overlap.toDouble() / duration.toDouble()).coerceAtMost(1.0)
}

/** Dati dell'orologio per una sessione: gia' ridotti a quel che il riepilogo mostra. */
data class WorkoutVitals(
    val samples: List<HeartRateSample>,
    val avgBpm: Int?,
    val maxBpm: Int?,
    val kcal: Double?
)
