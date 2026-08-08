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
- Donazioni volontarie (Ko-fi), nessun Play Billing, nessun vantaggio sbloccato in cambio della donazione.

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

App SOLO in light mode (deciso in Fase 10): niente schema scuro, niente isSystemInDarkTheme.
Background:              #FBF6F2   surface/card: #FFFFFF
Accento primario:        #F97348   (arancio tramonto, CTA e stati attivi)
Accento scuro / soft:    #D4501F / #FFEADF

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

### Stile "island" (adottato dopo la Fase 5 — vale per tutte le schermate, nuove e vecchie)

La UI e' costruita su contenitori flottanti ("isole") su background pieno: niente barre ancorate
ai bordi, niente divider, niente elevation Material di default. La separazione la fa l'ombra
morbida, non il bordo.

```
Corner radius isola:      28dp   (IslandShape)
Corner radius tile:       24dp   (TileShape)
Corner radius card:       20dp   (CardShape, legacy)
Pill:                     50%    (PillShape — bottoni, chip, nav)
Gutter laterale schermo:  24dp   (Spacing.xl)
Spazio fra isole:         12dp   (Spacing.md)
Ombra isola:              8-18dp, alpha 0.10-0.12 light / 0.6 dark (Modifier.islandShadow)

Superficie incassata (campi, chip inattive, tracce grafico): #F5EDE7
Testo secondario: #8A7D75   Bordo tenue: #F0E5DD
```

Regole:
- Ogni schermata usa `IslandScreen` (scroll) o `IslandListScreen` (liste lunghe) — danno background,
  inset di sistema e spazio di coda per la nav flottante.
- Intestazione: `ScreenHeader` (titolo grande + sottotitolo, back tondo a sinistra, azione tonda a destra).
  Niente `TopAppBar`.
- Navigazione: `IslandNavBar`, pill flottante che non tocca i bordi; la voce attiva si espande con
  etichetta. Visibile solo sui 4 tab principali.
- Timer di recupero: isola flottante sopra il contenuto (`BottomTimerBar`), con barra di avanzamento.
- Controlli: `IslandButton` / `IslandSecondaryButton` / `IslandChip` / `IslandTextField` /
  `IslandNumberField`. Non usare Button, FilterChip, OutlinedTextField Material di default.
- Metriche: `StatTile` (bento, icona + etichetta + numero grande) e `MiniBarChart` (Canvas puro).
- Stati vuoti: `IslandEmptyState`, mai numeri finti come segnaposto.
- FAB Material: sostituito da `IslandIconButton` nell'header.

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
    val description: String,           // inglese: e' anche il fallback delle altre lingue
    val descriptionIt: String? = null, // aggiunte in Fase 16 (DB v3, MIGRATION_2_3)
    val descriptionFr: String? = null,
    val loggingInstructions: String,   // vuota per la libreria: la frase viene da weightType
                                       // via strings.xml. La riempie solo un esercizio custom.
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

**Fase 5b — Restyle UI/UX "island"** *(fatta)*
DoD: design system island in `ui/theme` + `ui/components`; tutte le schermate delle Fasi 0-5
riscritte sui nuovi componenti; nav flottante a pill; Dashboard e Progressi con layout bento e
stati vuoti (dati reali in Fase 6).

**Fase 6 — Dashboard e progressi** *(fatta)*
DoD: dashboard storico allenamenti; grafici volume/PR; heatmap stile GitHub; schermata peso corporeo che alimenta `bodyweightSnapshotKg` ai nuovi set.

**Fase 7 — Condivisione stile Strava** *(fatta)*
DoD: immagine riepilogo sessione generata e condivisibile via `Intent.ACTION_SEND`.

**Fase 8 — Impostazioni, tema, donazioni** *(fatta)*
DoD: voce "Offrimi un caffè" apre l'URL Ko-fi (`https://ko-fi.com/gnottero`,
`DONATION_URL` in `ui/settings/DonationLauncher.kt`) in Custom Tabs, fallback
ACTION_VIEW; sezione Info con versione, nota privacy e licenze. Il toggle tema non serve più:
dalla Fase 10 l'app è light-only.

**Fase 10 — UX allenamento + palette viola** *(fatta)*
DoD: salvataggio routine riporta ad "Allena" (la routine e' un template: le sue serie non
partono compilate ne' segnate come svolte, i target restano segnaposto grigi); sessione in
stile Hevy con header durata/volume/serie + barra di progresso, card esercizio compatta con
menu unico e tabella serie/precedente/kg/rip/check; recupero modificabile durante
l'allenamento; palette light-only bianco+viola; fine timer con suono e vibrazione; schermata
Impostazioni con toggle per suono, vibrazione timer e feedback aptico.

**Fase 11 — Sessione persistente e rifiniture** *(fatta)*
DoD: un allenamento si chiude solo con "Termina" (con conferma); uscendo resta in corso e si
rientra dal banner "Riprendi" in Allena, che blocca l'avvio di una seconda sessione; feedback
aptico su tutti i controlli (bottoni, chip, menu, switch, nav), disattivabile da Impostazioni;
peso limitato a 999 kg su serie, target di routine e peso corporeo; serie chiusa senza valori
eredita quelli dell'ultima volta (poi il target); preset di recupero in FlowRow; card storico
con durata/volume/serie in riquadri; immagine condivisibile light con marchio stilizzato,
durata e soli dettagli essenziali.

**Fase 12 — Rifiniture UX** *(fatta)*
DoD: icona app = marchio della card condivisibile; card dello storico minimali (giorno, ora,
metriche su una riga, esercizi); segnaposto di peso e ripetizioni sempre proposti (ultima volta,
target di routine, ultimo valore registrato per quell'esercizio); azioni dell'esercizio in corso
e delle routine in un foglio dal basso con righe grandi; tempo di recupero come foglio con
stepper e griglia di durate che si applicano subito; picker esercizi con ricerca e filtro per
gruppo muscolare; eliminazione routine con conferma; vibrazione dei tap su canale non attenuato
(EFFECT_HEAVY_CLICK) perche' quella precedente era impercettibile.

**Fase 13 — Gesti, note e recupero a rulli** *(fatta)*
DoD: allenamento annullabile (elimina la sessione, distinta da "Termina" che salva); tocco lungo
al posto dei tre puntini su card esercizio in sessione, riga serie, routine ed esercizio di
routine; tempo di recupero scelto con rulli stile sveglia (`DurationWheelPicker` +
`RestTimeSheet` condivisi fra routine e allenamento); tasto "Modifica" tolto dalla card routine,
finito nel foglio col tocco lungo insieme a "Elimina"; "Riproduci" spostato dall'editor routine
all'header dell'allenamento in corso; nota libera per esercizio, sulla routine (`notes` su
`routine_exercises`) e sulla sessione (`notes` su `workout_exercises`, ereditata all'avvio e
modificabile senza toccare il template) — DB alla versione 2 con `MIGRATION_1_2`; logo unico in
`res/drawable/ic_eina_logo.xml`, usato da ShareCard e da `EinaLogo` in Compose; il nome
dell'esercizio in sessione apre la sua scheda; banner "Riprendi" con durata che scorre dal vivo.

**Fase 15 — Palette arancio e rifiniture di navigazione** *(fatta)*
DoD: palette accento su arancio tramonto (#F97348) ovunque, logo e icona compresi; card dello
storico col nome della routine come titolo e data/ora a destra; tocco sulla card routine in
"Allena" apre la routine (il tondo la avvia); "Nuova" tolto da "Allena" e "Storico" ridotto a
bottone tondo con icona; tab della nav senza saveState/restoreState (ripristinavano le schermate
di dettaglio e rendevano la Dashboard irraggiungibile); condivisione come sticker di storia
Instagram (`com.instagram.share.ADD_TO_STORY`) con fallback al chooser di sistema; Impostazioni
con "Cancella storico allenamenti" a conferma.

**Fase 16 — Multilingua e overlay di condivisione** *(fatta)*
DoD: inglese (default, `values/`), italiano e francese; tutte le stringhe delle schermate in
`strings.xml`, comprese le etichette di muscoli, attrezzatura e tipo di carico (nel DB restano
chiavi inglesi, la traduzione avviene al disegno); date, iniziali dei giorni e decimali seguono
la lingua attiva; `loggingInstructions` derivate da `weightType` invece che salvate nel seed;
selettore lingua in Impostazioni (Sistema/English/Italiano/Français) applicato riscrivendo la
Configuration in `attachBaseContext`, senza aggiungere appcompat; catalogo ridotto a 197
esercizi comuni con descrizioni tradotte; DB alla versione 3 con `MIGRATION_2_3`, e
`ExerciseSeeder` che sincronizza la libreria per nome senza toccare esercizi custom o esercizi
ancora usati da routine e storico; condivisione con due stili — tessera opaca e overlay
trasparente da appoggiare sulla propria foto — che si salva in galleria, finisce negli appunti e
apre la fotocamera storie di Instagram.

**Fase 9 — Rifinitura** *(fatta)*
DoD: R8 + shrinkResources attivi sulla release (20,5 MB → 2,2 MB), regole in
`app/proguard-rules.pro`; release firmata con la chiave di debug finché non esiste un
keystore di distribuzione; avvio a freddo 592 ms (`am start -W`); edge case gestiti
(galleria assente e copia file fallita mostrate in `CreateExerciseScreen`,
`launchPlaylist` ritorna `false` senza app né browser e l'allenamento mostra un toast,
seed fallito non abbatte l'avvio).

---

## Asset da preparare TU prima di lanciare Claude Code (per non farlo bloccare a metà)

- [x] Nome definitivo e package name → **Eina**, `com.<org>.eina`
- [x] URL donazioni → **Ko-fi**: `https://ko-fi.com/gnottero` (`DONATION_URL` in
  `ui/settings/DonationLauncher.kt`). Ko-fi e non Buy Me a Coffee perché BMC accetta solo
  Stripe per i nuovi account, mentre Ko-fi incassa direttamente su PayPal.
- [x] Dataset esercizi → **fatto**: l'export integrale resta in `eina_exercises_seed.json` (873
  esercizi da free-exercise-db), ma il catalogo dell'app è il sottoinsieme curato di **197
  esercizi comuni** generato da `tools/curate_exercises.py` a partire da
  `tools/common_exercises.txt`. Ogni descrizione è tradotta in italiano e francese
  (`tools/translations/*.json` → `descriptionIt`/`descriptionFr`). Per rigenerare
  `app/src/main/assets/seed/exercises.json` basta rilanciare lo script; se cambiano i
  contenuti, alza `CATALOG_VERSION` in `ExerciseSeeder`.
- [x] Icona app → **fatta**: marchio Eina (tessera arancio + tre barre bianche) come adaptive icon in `res/drawable/ic_launcher_foreground.xml`, stesso segno disegnato in `ui/share/ShareCard.kt`
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
