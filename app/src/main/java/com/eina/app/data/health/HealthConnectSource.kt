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

        // I numeri vengono dall'aggregazione sulla finestra esatta dell'allenamento, non dalla
        // somma dei record letti a mano. Health Connect, aggregando, fa due cose che leggendo i
        // record non si hanno: taglia i record a cavallo della finestra (le calorie sono spesso
        // un blocco lungo, a volte dell'intera giornata) e soprattutto **deduplica per
        // priorita' delle app**. Con l'orologio e il telefono che scrivono entrambi le calorie,
        // sommare i record contava due volte lo stesso sforzo: e' la ragione principale per cui
        // i numeri non tornavano. Anche la media dei battiti era una media aritmetica dei
        // campioni, quindi pesata su quanto fitto campiona ogni sorgente invece che sul tempo.
        val window = TimeRangeFilter.between(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end))

        val aggregate = runCatching {
            healthClient.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        HeartRateRecord.BPM_AVG,
                        HeartRateRecord.BPM_MAX,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = window
                )
            )
        }.getOrNull()

        val avgBpm = aggregate?.get(HeartRateRecord.BPM_AVG)?.toInt()
        val maxBpm = aggregate?.get(HeartRateRecord.BPM_MAX)?.toInt()
        // Le attive hanno la precedenza sulle totali: quelle totali comprendono il metabolismo
        // basale, e contarlo direbbe che l'allenamento ha bruciato anche lo stare fermi.
        val activeKcal = aggregate?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories
        val rawKcal = activeKcal?.takeIf { it > 0.0 }
            ?: aggregate?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories

        // Le calorie si sommano, quindi un orologio che ha misurato solo un pezzo di
        // allenamento produce un numero vero ma monco — e un numero monco, letto nel
        // riepilogo, e' un numero sbagliato: novanta minuti di palestra che dicono "2 kcal"
        // perche' la finestra sfiorava di un minuto l'ultimo blocco di dati. Sotto meta'
        // allenamento coperto si preferisce non dire niente.
        val coverage = runCatching {
            val active = healthClient
                .readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRangeFilter = window))
                .records.map { it.startTime.toEpochMilli() to it.endTime.toEpochMilli() }
            val intervals = active.ifEmpty {
                healthClient
                    .readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRangeFilter = window))
                    .records.map { it.startTime.toEpochMilli() to it.endTime.toEpochMilli() }
            }
            coveredShare(intervals, start, end)
        }.getOrDefault(0.0)
        val kcal = rawKcal?.takeIf { coverage >= MIN_COVERAGE }

        // La spezzata invece vuole i singoli campioni, che l'aggregazione non da'. Qui la
        // finestra si allarga di un margine, perche' una bracciale sincronizza a pacchetti e un
        // allenamento corto puo' cadere fra due blocchi; e si tiene una sola sorgente — quella
        // che ha campionato piu' fitto, cioe' l'orologio — altrimenti due provider disegnano
        // due tracce sovrapposte sullo stesso grafico.
        val margin = TimeRangeFilter.between(
            Instant.ofEpochMilli(start - QUERY_MARGIN_MS),
            Instant.ofEpochMilli(end + QUERY_MARGIN_MS)
        )
        val byOrigin = runCatching {
            healthClient.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = margin))
                .records
                .groupBy { it.metadata.dataOrigin.packageName }
                .mapValues { (_, records) ->
                    records
                        .flatMap { record -> record.samples }
                        .map { HeartRateSample(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) }
                        .sortedBy { it.timeMillis }
                        // Blocchi diversi della stessa sorgente si sovrappongono agli estremi e
                        // ripetono lo stesso battito: sulla spezzata era un punto disegnato due volte.
                        .distinctBy { it.timeMillis }
                }
        }.getOrDefault(emptyMap())

        // Solo i battiti misurati dentro l'allenamento. Prima, quando dentro la finestra non ce
        // n'era nessuno, si ripiegava su quelli del margine: il riepilogo mostrava allora la
        // frequenza di dieci minuti prima — cioe' di uno seduto — spacciata per media
        // dell'allenamento. Meglio nessun dato di un dato di un altro momento.
        val inWindow = byOrigin.mapValues { (_, samples) ->
            samples.filter { it.timeMillis in start..end }
        }
        val samples = inWindow.values.maxByOrNull { it.size }.orEmpty()

        // Due battiti in croce non fanno una media: con un orologio che campiona al minuto
        // sono due minuti di allenamento, e il numero grande in pagina direbbe piu' di quel
        // che sa.
        val hasHeartRate = samples.size >= MIN_HR_SAMPLES
        if (!hasHeartRate && kcal == null) return@withContext null
        WorkoutVitals(
            samples = if (hasHeartRate) samples else emptyList(),
            avgBpm = avgBpm?.takeIf { hasHeartRate },
            maxBpm = maxBpm?.takeIf { hasHeartRate },
            kcal = kcal?.takeIf { it > 0.0 }
        )
    }
}

/** Margine con cui si allarga la lettura dei campioni: vedi [HealthConnectSource.readWorkoutVitals]. */
private const val QUERY_MARGIN_MS = 10 * 60 * 1000L

/** Quanto dell'allenamento l'orologio deve aver misurato perche' le calorie valgano qualcosa. */
private const val MIN_COVERAGE = 0.5

/** Battiti minimi perche' media e massimo abbiano un senso. */
private const val MIN_HR_SAMPLES = 3

/** Quota della finestra coperta dagli intervalli, contando una volta sola le sovrapposizioni. */
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

/** Dati dell'orologio per una sessione: gia' ridotti a quel che il riepilogo mostra. */
data class WorkoutVitals(
    val samples: List<HeartRateSample>,
    val avgBpm: Int?,
    val maxBpm: Int?,
    val kcal: Double?
)
