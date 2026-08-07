# CLAUDE.md — Eina

## Modalità operativa (leggi prima di tutto)

- Lavori a fasi sequenziali (sez. "Fasi"). Completa una fase, verifica, poi passa alla successiva.
- **Non fermarti a chiedere conferme** su cose già decise in questo documento (stack, schema dati, palette, nomi campo). Se qualcosa non è specificato ed è ambiguo, scegli l'opzione più semplice coerente con i "Principi", annotala con un commento `// DECISIONE: ...` e prosegui — non bloccarti in attesa di risposta.
- Al termine di ogni fase: esegui `./gradlew assembleDebug` (o i test unitari pertinenti se già presenti) e **correggi gli errori prima di considerare la fase conclusa**.
- Fai un commit git separato per fase: `feat(faseN): descrizione`.
- Non aggiungere dipendenze non elencate nello stack tecnico senza un motivo concreto — se serve, spiega perché nel commit message.
- Se ti manca un asset esterno (es. dataset esercizi, icona app) e non è nella cartella `assets/seed/`, **non inventarlo**: crea un placeholder minimale, segnala la mancanza in un TODO nel codice, e continua con il resto della fase.

---

## Principi (non negoziabili)

- Local-first: nessun server, nessun login, nessuna raccolta dati.
- Leggerezza: minime dipendenze, avvio istantaneo.
- Nessuna funzione social interna. L'unica condivisione è export immagine verso app esterne.
- Design pulito, ispirato ad Apple Health ma con asset e font propri (mai SF Symbols/SF Pro, mai loghi Apple).
- Donazioni volontarie (Buy Me a Coffee), nessun Play Billing, nessun vantaggio sbloccato in cambio della donazione.

---

## Stack tecnico (fisso — non ridiscutere in fase di implementazione)

- Kotlin, Jetpack Compose, Material 3 come base per i componenti custom
- Room per la persistenza locale
- Koin per la DI
- Coil per immagini/GIF
- Vico (o Canvas custom) per i grafici
- `androidx.browser` (Custom Tabs) per il link donazioni
- Nessun networking a runtime tranne apertura Intent verso Spotify/YouTube Music
- minSdk 26

---

## Design tokens (valori di partenza — usa questi, non improvvisare una palette diversa)

```
Corner radius card:      20dp
Corner radius bottoni:   12dp
Spacing scale:           4 / 8 / 12 / 16 / 24 / 32 dp
Font:                    Inter (Regular / Medium / SemiBold / Bold)

Light — background:      #F7F7F8   surface/card: #FFFFFF
Dark  — background:      #1C1C1E   surface/card: #2C2C2E
Accento primario:        #FF6A3D   (arancio energico, CTA e stati attivi)

Colori per categoria muscolare (badge + body diagram):
  Petto/Push:    #FF6B6B
  Schiena/Pull:  #4D96FF
  Gambe:         #51CF66
  Spalle:        #9775FA
  Braccia:       #FFB020
  Core:          #20C997
  Cardio:        #F06595

Icone: Phosphor Icons o Material Symbols (outline). Mai SF Symbols.
```

---

## Struttura pacchetti

```
com.<org>.eina
 ├─ data/
 │   ├─ db/            (Room: entities, DAO, Database, TypeConverters)
 │   ├─ repository/
 │   └─ seed/           (import/parsing free-exercise-db arricchito)
 ├─ domain/             (logica PR, calcolo volume, query "ultima volta")
 ├─ ui/
 │   ├─ theme/          (Color.kt, Type.kt, Shape.kt — usa i token sopra)
 │   ├─ components/     (Card, Badge, BottomTimerBar, ExerciseCard, riusabili)
 │   ├─ dashboard/
 │   ├─ workout/        (lista routine, allenamento in corso)
 │   ├─ library/        (libreria esercizi, dettaglio esercizio)
 │   ├─ progress/       (grafici, anelli, heatmap, peso corporeo)
 │   └─ settings/       (tema, donazioni, info/licenza)
 └─ MainActivity.kt / Navigation.kt (bottom nav a 4 tab: Dashboard, Allena, Esercizi, Progressi)
```

---

## Schema dati — implementa ESATTAMENTE questo (nomi campo inclusi)

```kotlin
enum class WeightType {
    FREE_WEIGHT, BODYWEIGHT, BODYWEIGHT_PLUS_LOAD, ASSISTED, MACHINE_STACK, TIME_BASED
}

enum class PlaylistType { SPOTIFY, YOUTUBE_MUSIC }

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val loggingInstructions: String,
    val weightType: WeightType,
    val muscleGroupsPrimary: List<String>,     // TypeConverter: JSON string
    val muscleGroupsSecondary: List<String>,   // TypeConverter: JSON string
    val equipment: String? = null,
    val mediaUri: String? = null,              // GIF locale o bundled
    val isCustom: Boolean = false,
    val source: String? = null
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val linkedPlaylistUri: String? = null,
    val linkedPlaylistType: PlaylistType? = null
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"])
    ]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val order: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double? = null,
    val restSeconds: Int
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long? = null,
    val startTime: Long,
    val endTime: Long? = null
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val order: Int
)

@Entity(
    tableName = "set_entries",
    foreignKeys = [ForeignKey(entity = WorkoutExerciseEntity::class, parentColumns = ["id"], childColumns = ["workoutExerciseId"], onDelete = ForeignKey.CASCADE)]
)
data class SetEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val setIndex: Int,
    val targetReps: Int? = null,
    val actualReps: Int? = null,        // per TIME_BASED: durata in secondi
    val weight: Double? = null,
    val restSecondsPlanned: Int,
    val isWarmup: Boolean = false,
    val completedAt: Long? = null,
    val isPR: Boolean = false,
    val bodyweightSnapshotKg: Double? = null   // salvato al momento del set SOLO per BODYWEIGHT/BODYWEIGHT_PLUS_LOAD/ASSISTED,
                                                 // evita join complessi su BodyMetric per ricostruire il peso storico
)

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val bodyweightKg: Double
)
```

Nota: `bodyweightSnapshotKg` è una scelta di denormalizzazione deliberata rispetto al piano originale, per semplificare drasticamente il calcolo di PR/volume su esercizi a corpo libero senza query su finestre temporali. Implementala così, non tornare allo schema puramente normalizzato.

---

## Logica di dominio — implementa esattamente questa

```kotlin
fun isNewPR(
    weightType: WeightType,
    newSet: SetEntryEntity,
    historicalSets: List<SetEntryEntity> // tutte le set non-warmup già completate per lo stesso exerciseId
): Boolean {
    if (newSet.isWarmup) return false
    return when (weightType) {
        WeightType.FREE_WEIGHT, WeightType.MACHINE_STACK, WeightType.ASSISTED ->
            (newSet.weight ?: 0.0) > (historicalSets.maxOfOrNull { it.weight ?: 0.0 } ?: 0.0)

        WeightType.BODYWEIGHT ->
            (newSet.actualReps ?: 0) > (historicalSets.maxOfOrNull { it.actualReps ?: 0 } ?: 0)

        WeightType.BODYWEIGHT_PLUS_LOAD -> {
            val newLoad = (newSet.bodyweightSnapshotKg ?: 0.0) + (newSet.weight ?: 0.0)
            val maxHistLoad = historicalSets.maxOfOrNull { (it.bodyweightSnapshotKg ?: 0.0) + (it.weight ?: 0.0) } ?: 0.0
            newLoad > maxHistLoad
        }

        WeightType.TIME_BASED ->
            (newSet.actualReps ?: 0) > (historicalSets.maxOfOrNull { it.actualReps ?: 0 } ?: 0) // riusa actualReps come durata
    }
}

fun volumeForSet(weightType: WeightType, set: SetEntryEntity): Double {
    val reps = set.actualReps ?: 0
    return when (weightType) {
        WeightType.FREE_WEIGHT, WeightType.MACHINE_STACK ->
            (set.weight ?: 0.0) * reps
        WeightType.BODYWEIGHT ->
            (set.bodyweightSnapshotKg ?: 0.0) * reps
        WeightType.BODYWEIGHT_PLUS_LOAD ->
            ((set.bodyweightSnapshotKg ?: 0.0) + (set.weight ?: 0.0)) * reps
        WeightType.ASSISTED ->
            ((set.bodyweightSnapshotKg ?: 0.0) - (set.weight ?: 0.0)).coerceAtLeast(0.0) * reps
        WeightType.TIME_BASED ->
            0.0 // il "volume" per esercizi a tempo non è in kg; escludi dal totale kg sollevati
    }
}

// Query "ultima volta": ultima WorkoutSession (per startTime) che contiene un
// WorkoutExercise per lo stesso exerciseId, con i suoi SetEntry ordinati per setIndex.
```

---

## Deep link playlist

```
Spotify:  se linkedPlaylistUri contiene "open.spotify.com/playlist/<ID>",
          costruisci "spotify:playlist:<ID>" e apri con Intent.ACTION_VIEW.
YouTube Music:
          apri linkedPlaylistUri con Intent.ACTION_VIEW e
          setPackage("com.google.android.apps.youtube.music").
Fallback: se l'app target non è installata, apri l'URL originale nel browser di sistema.
```

---

## Fasi (ognuna è un blocco autonomo — build verde prima di proseguire)

**Fase 0 — Setup + design system**
DoD: app builda; bottom nav 4 tab funzionante; tema light/dark applica i token sopra; componenti base (Card, Badge) pronti in `ui/components`.

**Fase 1 — Data layer**
DoD: tutte le entity/DAO/Database compilano; TypeConverter per liste/enum funzionanti; query "ultima volta" e le funzioni di dominio sopra coperte da test unitari minimi.

**Fase 2 — Sessione di allenamento**
DoD: si può creare un allenamento libero, aggiungere/rimuovere/riordinare esercizi e serie a runtime, salvare reps/peso/recupero per set, vedere il riferimento "ultima volta", il timer parte da solo a fine serie con controlli -15s/+15s/skip, badge PR compare quando `isNewPR` è true.

**Fase 3 — Libreria esercizi**
DoD: seed da dataset arricchito (o placeholder se assente, vedi sez. Asset); libreria filtrabile; dettaglio esercizio con body diagram colorato + descrizione + `loggingInstructions`.

**Fase 4 — Esercizi custom**
DoD: form crea esercizio con GIF da galleria salvata in storage interno; l'esercizio custom è utilizzabile ovunque come uno di libreria.

**Fase 5 — Routine + playlist**
DoD: editor routine con set/reps/peso/recupero target; bottone "Riproduci" avvia il deep link come da sezione sopra.

**Fase 6 — Dashboard e progressi**
DoD: dashboard storico allenamenti; grafici volume/PR; heatmap stile GitHub; schermata peso corporeo che alimenta `bodyweightSnapshotKg` ai nuovi set.

**Fase 7 — Condivisione stile Strava**
DoD: immagine riepilogo sessione generata e condivisibile via `Intent.ACTION_SEND`.

**Fase 8 — Impostazioni, tema, donazioni**
DoD: toggle tema manuale; voce "Offrimi un caffè" apre l'URL Buy Me a Coffee in Custom Tabs.

**Fase 9 — Rifinitura**
DoD: ProGuard/R8 attivo, avvio a freddo ottimizzato, coerenza visiva su tutte le schermate, edge case gestiti (permessi galleria, app playlist assente).

---

## Asset da preparare TU prima di lanciare Claude Code (per non farlo bloccare a metà)

- [x] Nome definitivo e package name → **Eina**, `com.<org>.eina`
- [ ] URL Buy Me a Coffee
- [x] Dataset esercizi arricchito → **pronto**: `seed/eina_exercises_seed.json` (873 esercizi convertiti da free-exercise-db con `weightType`/`description`/`loggingInstructions`). Copialo in `app/src/main/assets/seed/exercises.json`. **274 esercizi hanno `needsReview: true`** (classificazione `weightType` incerta, o categoria "stretching" ambigua per un tracker di forza) — filtra su questo campo per una revisione manuale mirata, non serve rivederli tutti. Le `description` sono in inglese (lingua originale del dataset): per la v1 puoi tenerle così, una traduzione IT è un'iterazione successiva non bloccante.
- [ ] Icona app anche solo placeholder in `res/mipmap`
- [ ] **Decisione sulle immagini esercizio**: ogni esercizio ha in media 2 frame JPG da ~38KB l'uno → bundlare tutte le immagini di libreria (~1700 file) costerebbe ~65-70MB di APK, in conflitto col principio "leggera". Opzioni da decidere prima della Fase 3: (a) bundlare solo la prima immagine per esercizio (~33MB), (b) bundlare un sottoinsieme curato (es. i 150-200 esercizi più comuni) e usare Play Asset Delivery per il resto, (c) ricomprimere/ridimensionare le immagini prima del bundling. Lo script di conversione salva comunque tutti i path in `mediaFrames` per ogni esercizio, così qualunque opzione si scelga i dati sono già pronti.

---

## Prompt di avvio suggerito

```
Leggi CLAUDE.md per intero prima di iniziare. Esegui le fasi in ordine, una alla
volta, senza fermarti a chiedere conferme su cose già specificate nel documento.
Dopo ogni fase esegui ./gradlew assembleDebug, correggi eventuali errori, fai un
commit, poi passa alla fase successiva. Fermati solo se ti manca un asset esterno
elencato in "Asset da preparare" e non presente nel repo.
```
