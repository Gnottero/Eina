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

        // Prima di leggere si guarda *dove* stanno i dati. L'app dell'orologio dovrebbe
        // depositarli con l'ora in cui li ha misurati, ma non tutte lo fanno: Mi Fitness, per
        // dire, li stampiglia piu' avanti (sul telefono di prova un allenamento delle 08:00
        // compariva in Health Connect alle 09:30, stessi battiti e stesse calorie). Chiedendo
        // la finestra esatta si trovava il vuoto. [detectOffset] misura lo scarto e sposta la
        // finestra di conseguenza; se i dati sono al loro posto lo scarto e' zero e non cambia
        // niente.
        val offset = detectOffset(healthClient, start, end)
        val shiftedStart = start + offset
        val shiftedEnd = end + offset

        // Quindici minuti di margine: i blocchi di una bracciale sono allineati alla mezz'ora,
        // quindi i primi e gli ultimi minuti di allenamento cadono spesso in un blocco che
        // comincia poco prima o finisce poco dopo.
        val window = TimeRangeFilter.between(
            Instant.ofEpochMilli(shiftedStart - WINDOW_PADDING_MS),
            Instant.ofEpochMilli(shiftedEnd + WINDOW_PADDING_MS)
        )
        // Le calorie invece si chiedono sulla finestra esatta: si sommano, e mezz'ora in piu'
        // di giornata sarebbe mezz'ora di calorie che l'allenamento non ha bruciato.
        val exactWindow = TimeRangeFilter.between(
            Instant.ofEpochMilli(shiftedStart),
            Instant.ofEpochMilli(shiftedEnd)
        )

        // I numeri vengono dall'aggregazione, non dalla somma dei record letti a mano. Health
        // Connect, aggregando, fa due cose che leggendo i record non si hanno: taglia i record
        // a cavallo della finestra (le calorie sono spesso un blocco lungo, a volte dell'intera
        // giornata) e soprattutto **deduplica per priorita' delle app**. Con l'orologio e il
        // telefono che scrivono entrambi le calorie, sommare i record contava due volte lo
        // stesso sforzo. Anche la media dei battiti era una media aritmetica dei campioni,
        // quindi pesata su quanto fitto campiona ogni sorgente invece che sul tempo.
        val heartAggregate = runCatching {
            healthClient.aggregate(
                AggregateRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX),
                    timeRangeFilter = window
                )
            )
        }.getOrNull()
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
        // Le attive hanno la precedenza sulle totali: quelle totali comprendono il metabolismo
        // basale, e contarlo direbbe che l'allenamento ha bruciato anche lo stare fermi.
        val activeKcal = energyAggregate?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories
        val rawKcal = activeKcal?.takeIf { it > 0.0 }
            ?: energyAggregate?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories

        // Le calorie si sommano, quindi un orologio che ha misurato solo un pezzo di
        // allenamento produce un numero vero ma monco — e un numero monco, letto nel
        // riepilogo, e' un numero sbagliato: novanta minuti di palestra che dicono "2 kcal"
        // perche' la finestra sfiorava di un minuto l'ultimo blocco di dati. Sotto meta'
        // allenamento coperto si preferisce non dire niente.
        val coverage = coveredShare(
            readEnergyIntervals(healthClient, exactWindow),
            shiftedStart,
            shiftedEnd
        )
        val kcal = rawKcal?.takeIf { coverage >= MIN_COVERAGE }

        // La spezzata invece vuole i singoli campioni, che l'aggregazione non da'. Si tiene una
        // sola sorgente — quella che ha campionato piu' fitto, cioe' l'orologio — altrimenti
        // due provider disegnano due tracce sovrapposte sullo stesso grafico.
        val byOrigin = runCatching {
            healthClient.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = window))
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

        val inWindow = byOrigin.mapValues { (_, samples) ->
            samples.filter { it.timeMillis in (shiftedStart - WINDOW_PADDING_MS)..(shiftedEnd + WINDOW_PADDING_MS) }
        }
        // I campioni tornano all'ora dell'allenamento: la spezzata sta sotto le sue ore, non
        // sotto quelle in cui l'app dell'orologio ha creduto di trovarsi.
        val samples = inWindow.values.maxByOrNull { it.size }.orEmpty()
            .map { if (offset == 0L) it else it.copy(timeMillis = it.timeMillis - offset) }

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

    /**
     * Di quanto sono spostati i dati dell'orologio rispetto all'orologio del telefono.
     *
     * Zero quando i dati stanno dove dovrebbero, ed e' il caso normale: si prova prima la
     * finestra esatta e se e' coperta si esce subito. Altrimenti si guarda in un giorno intorno
     * all'allenamento, si prendono i blocchi di dati che l'orologio ha depositato e si prova ad
     * allineare l'inizio di ognuno con l'inizio dell'allenamento. Lo scarto candidato si
     * arrotonda al quarto d'ora — tutti i fusi del mondo sono multipli di quindici minuti, e
     * arrotondare evita di rincorrere il secondo esatto in cui la bracciale ha aperto il blocco.
     * Vince lo scarto che copre di piu' l'allenamento, a parita' il piu' piccolo; se nemmeno il
     * migliore copre mezzo allenamento si torna a zero, cioe' "niente dati" invece di prendere
     * la camminata di un'altra ora.
     */
    private suspend fun detectOffset(client: HealthConnectClient, start: Long, end: Long): Long {
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
        if (exact >= MIN_COVERAGE) return 0L

        val search = TimeRangeFilter.between(
            Instant.ofEpochMilli(start - MAX_OFFSET_MS),
            Instant.ofEpochMilli(end + MAX_OFFSET_MS)
        )
        val intervals = runCatching { readEnergyIntervals(client, search) }.getOrDefault(emptyList())
        if (intervals.isEmpty()) return 0L

        var best = 0L
        var bestCoverage = exact
        intervals.map { it.first }.distinct().forEach { blockStart ->
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
        return if (bestCoverage >= MIN_COVERAGE) best else 0L
    }

    /**
     * Gli intervalli su cui l'orologio ha scritto calorie: sono la traccia di quando stava
     * misurando, e servono sia a capire quanto ha coperto l'allenamento sia a ritrovare i dati
     * quando sono stampigliati altrove. Le totali fanno da riserva a chi non scrive le attive.
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

/** Margine con cui si allarga la finestra dei battiti: vedi [HealthConnectSource.readWorkoutVitals]. */
private const val WINDOW_PADDING_MS = 15 * 60 * 1000L

/** Passo con cui si arrotonda lo scarto dei dati dell'orologio: vedi detectOffset. */
private const val QUARTER_HOUR_MS = 15 * 60 * 1000L

/** Quanto lontano si cercano i dati quando nella finestra dell'allenamento non ce n'e'. */
private const val MAX_OFFSET_MS = 24 * 60 * 60 * 1000L

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
