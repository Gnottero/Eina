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

        val range = TimeRangeFilter.between(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end))

        val samples = runCatching {
            healthClient.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = range))
                .records
                .flatMap { record -> record.samples }
                .map { HeartRateSample(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) }
                .sortedBy { it.timeMillis }
        }.getOrDefault(emptyList())

        val activeKcal = runCatching {
            healthClient.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRangeFilter = range))
                .records
                .sumOf { it.energy.inKilocalories }
        }.getOrDefault(0.0)

        val kcal = if (activeKcal > 0.0) {
            activeKcal
        } else {
            runCatching {
                healthClient.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRangeFilter = range))
                    .records
                    .sumOf { it.energy.inKilocalories }
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

/** Dati dell'orologio per una sessione: gia' ridotti a quel che il riepilogo mostra. */
data class WorkoutVitals(
    val samples: List<HeartRateSample>,
    val avgBpm: Int?,
    val maxBpm: Int?,
    val kcal: Double?
)
