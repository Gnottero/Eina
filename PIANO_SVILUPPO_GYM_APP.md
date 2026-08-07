# Piano di Sviluppo — Eina (Gym Tracker FOSS, alternativa a Hevy)

## 0. Obiettivo e principi guida

App Android nativa, gratuita e open source, per il tracciamento degli allenamenti in palestra. Nessun account, nessun backend cloud, nessuna funzione social in-app. Tutto locale sul device, leggero e veloce. Sostenuta da donazioni volontarie (Buy Me a Coffee), non da vendite o abbonamenti.

Principi che guidano ogni scelta tecnica successiva:
- **Local-first**: nessun server, nessun login, nessuna raccolta dati.
- **Leggerezza**: minime dipendenze, avvio istantaneo, nessun servizio in background superfluo.
- **No social interno**: l'unica "condivisione" è l'export di un'immagine riepilogo (stile Strava) verso app esterne (Instagram, ecc.), non un feed o profili pubblici.
- **Design pulito e "premium"**: interfaccia ispirata alle Apple Human Interface Guidelines / Apple Health, reinterpretata su Android senza scimmiottare iOS in modo posticcio.
- **Contesto durante l'allenamento**: l'utente non deve mai indovinare cosa fare — ogni serie mostra cosa ha fatto l'ultima volta, il timer di recupero è sempre a portata di mano, e i miglioramenti vengono celebrati sul momento.

---

## 1. Stack tecnico consigliato

| Ambito | Scelta | Perché |
|---|---|---|
| Linguaggio | Kotlin | standard moderno Android |
| UI | Jetpack Compose + Material 3 (fortemente personalizzato, vedi sez. 2) | dynamic theming, light/dark nativo, meno boilerplate di XML |
| Database locale | Room (SQLite) | leggero, query type-safe, nessuna dipendenza cloud |
| DI | Koin (o manuale) | Koin più leggero di Hilt/Dagger per un progetto solo-app |
| Immagini/GIF | Coil (con supporto GIF via `ImageDecoderDecoder`) | libreria immagini più leggera di Glide su Compose |
| Grafici progressi | Vico (patrykandpatrick/vico) o Canvas custom per l'heatmap/anelli | Vico è leggera e fatta apposta per Compose |
| Riordino liste (esercizi/serie in workout) | Piccola libreria "reorderable" per Compose (drag handle) o implementazione custom con `pointerInput` | evita di reinventare drag&drop, footprint minimo |
| Export immagine condivisione | `Canvas`/`GraphicsLayer.toImageBitmap()` + `Intent.ACTION_SEND` | nessuna libreria esterna necessaria |
| Browser in-app per donazioni | Android Custom Tabs (`androidx.browser`) | apre il link Buy Me a Coffee senza uscire dall'app |
| Deep link playlist | `Intent.ACTION_VIEW` con URI nativi (`spotify:playlist:ID`, intent con package esplicito per YouTube Music) | avvia la riproduzione direttamente nel servizio collegato, non solo apre l'app |
| Min SDK | API 26 (Android 8) o 28 se vuoi ridurre ulteriormente la superficie di test | copre >95% dei device attivi |

Niente Firebase, niente Retrofit/rete se non per il download iniziale (opzionale) della libreria esercizi e per l'apertura del link di donazione. Questo tiene l'APK piccolo e l'app avviabile offline al 100%.

---

## 2. Direzione UI/UX: stile Apple Human Interface (ispirazione Apple Health)

### 2.1 Tipografia
SF Pro non è utilizzabile fuori dall'ecosistema Apple (licenza). Alternative gratuite molto simili: **Inter** (consigliata) o Manrope/General Sans. Titoli grandi e bold che si restringono scrollando, header di sezione piccoli e maiuscoli, come le grouped list di iOS.

### 2.2 Colore
Sfondo neutro, card con curve morbide (~16-20dp) e ombra soffusa minima. Palette di accento **per categoria** (es. rosso per cardio, arancio per forza, verde per mobilità), un solo accento primario per CTA. Stessa palette usata sia sui badge icona sia sul body diagram (sez. 5).

### 2.3 Componenti
Card arrotondate, liste raggruppate, badge icona colorati, switch/bottoni/slider ristilizzati rispetto al Material di default. Bottom nav minimale.

### 2.4 Struttura a pagine e navigazione
Bottom nav a 4 voci, coerente con la parsimonia di Apple Health:
1. **Dashboard** — storico allenamenti, anelli settimanali, accesso rapido a "Continua routine"
2. **Allena** — lista routine + "Allenamento libero" + schermata allenamento in corso
3. **Esercizi** — libreria con filtri e diagramma muscolare
4. **Progressi** — grafici dettagliati, PR, heatmap, peso corporeo

Le Impostazioni (tema, donazioni, info/licenza) vivono dietro un'icona in alto nella Dashboard, per non affollare la tab bar.

Transizioni fluide tramite le API di navigazione/shared-element di Compose (una card che si espande nella schermata di dettaglio anziché un semplice "push" a scatti) per dare la sensazione di continuità tipica delle transizioni iOS.

### 2.5 Timer di recupero: barra persistente in basso
Durante l'allenamento, il timer di recupero vive in una **barra fissa in fondo allo schermo**, sopra la bottom nav (come una mini-player bar): sempre visibile, non blocca la navigazione tra le schede esercizio. Dettagli in sez. 6.

### 2.6 Cosa NON copiare
Niente icone/asset Apple, niente font SF Pro/SF Symbols, niente gesture di sistema in conflitto con quelle standard Android.

---

## 3. Modello dati (schema Room)

```
Exercise
 - id, name
 - description                 // spiegazione del movimento
 - loggingInstructions         // come interpretare il campo "peso" per QUESTO esercizio
 - weightType: enum            // FREE_WEIGHT | BODYWEIGHT | BODYWEIGHT_PLUS_LOAD | ASSISTED | MACHINE_STACK | TIME_BASED
 - muscleGroupsPrimary[], muscleGroupsSecondary[]
 - equipment, mediaUri, isCustom (bool), source

Routine
 - id, name, notes
 - linkedPlaylistUri, linkedPlaylistType (SPOTIFY | YOUTUBE_MUSIC)

RoutineExercise                // template: cosa fare per ogni esercizio della routine
 - id, routineId, exerciseId, order
 - targetSets, targetReps, targetWeight (nullable), restSeconds

WorkoutSession
 - id, routineId (nullable, allenamento libero), startTime, endTime

WorkoutExercise               // istanza dell'esercizio dentro una sessione: permette di
 - id, sessionId, exerciseId, order   // aggiungere/rimuovere/riordinare esercizi mentre alleni

SetEntry
 - id, workoutExerciseId, setIndex
 - targetReps, actualReps, weight
 - restSecondsPlanned, isWarmup, completedAt
 - isPR (bool, calcolato al salvataggio)

BodyMetric
 - id, date, bodyweightKg
```

La separazione `Routine`/`RoutineExercise` (template) da `WorkoutSession`/`WorkoutExercise`/`SetEntry` (istanza reale) è ciò che permette di modificare liberamente l'allenamento in corso — aggiungere una serie, saltare un esercizio, cambiarne l'ordine — senza toccare la routine originale.

---

## 4. Libreria esercizi: dataset di partenza

- **`yuhonas/free-exercise-db`**: dominio pubblico, ~800 esercizi in JSON con muscoli primari/secondari, equipment, istruzioni. Fornisce immagini JPG in sequenza (non GIF vere) — componibili in un loop animato lato client.
- **`ExerciseDB`**: dataset più ricco (11.000+ esercizi, GIF vere) ma distribuito come servizio hostato a pagamento/rate limit — introduce una dipendenza di rete esterna incompatibile con "app leggera, offline, no backend".

**Raccomandazione invariata, e ora eseguita**: `free-exercise-db` (873 esercizi) è stato scaricato e convertito allo schema di Eina — vedi `seed/eina_exercises_seed.json` e lo script `seed/convert_free_exercise_db.py`. La classificazione di `weightType` è euristica (basata su equipment/categoria/nome): 274 esercizi su 873 sono marcati `needsReview: true` per una revisione manuale mirata (principalmente esercizi con bande/palla medica/attrezzo "other", e l'intera categoria "stretching" che è ambigua per un tracker di forza). Le descrizioni restano in inglese per la v1.

Nota sulle immagini: il dataset fornisce ~2 frame JPG per esercizio (~38KB l'uno); bundlare tutta la libreria costerebbe ~65-70MB di APK, da bilanciare col principio di leggerezza (dettagli e opzioni in `CLAUDE.md`).

---

## 5. Scheda esercizio: body diagram + descrizione + istruzioni di logging

Ogni esercizio nella libreria (e nel dettaglio richiamato durante il logging) mostra tre elementi complementari, non alternativi:

1. **Body diagram**: silhouette fronte/retro con i path SVG colorati secondo la palette per-categoria (sez. 2.2) — muscoli primari con colore pieno, secondari con colore più tenue/trasparente. Colpo d'occhio immediato su "cosa lavora".
2. **Descrizione dell'esercizio** (`description`): testo breve su esecuzione ed eventuali note tecniche (postura, range di movimento).
3. **Istruzioni di logging del peso** (`loggingInstructions`): frase specifica su *come* interpretare il campo peso per quell'esercizio, perché non è ovvio e cambia per tipo:
   - `FREE_WEIGHT` (es. panca, squat con bilanciere): "inserisci il peso totale sollevato (bilanciere + dischi)".
   - `BODYWEIGHT` (es. trazioni, push-up): "il peso è il tuo corpo — non serve inserire nulla, verrà usato il tuo ultimo peso corporeo registrato".
   - `BODYWEIGHT_PLUS_LOAD` (es. trazioni zavorrate): "inserisci solo il sovraccarico aggiunto, il peso corporeo viene sommato automaticamente".
   - `ASSISTED` (es. dip assistiti): "inserisci il peso di assistenza scaricato dalla macchina (verrà sottratto dal tuo peso corporeo nel calcolo del volume)".
   - `MACHINE_STACK`: "inserisci il peso indicato sullo stack della macchina".
   - `TIME_BASED` (es. plank): campo reps sostituito da durata.

Questo campo è ciò che rende il logging comprensibile anche per esercizi ambigui, ed è anche il dato che pilota quale input mostrare nella UI di logging set (sez. 6) e come calcolare volume/PR (sez. 6-7).

---

## 6. Sessione di allenamento: schede editabili, set, timer, "ultima volta", record

### 6.1 Layout: schede esercizio in sequenza
La schermata "allenamento in corso" mostra gli esercizi come **schede successive in una lista verticale** (una `LazyColumn` di `ExerciseCard`), ciascuna con le proprie serie sotto forma di righe. Durante l'allenamento è possibile, in ogni momento:
- aggiungere un nuovo esercizio (in coda o in una posizione scelta),
- rimuovere un esercizio,
- riordinare gli esercizi (drag handle),
- aggiungere/rimuovere singole serie all'interno di una scheda.

Tutte queste modifiche agiscono su `WorkoutExercise`/`SetEntry` (l'istanza), non sulla `Routine` originale — la routine resta un template riutilizzabile.

### 6.2 Input per singola serie
Ogni riga-serie espone, in base al `weightType` dell'esercizio (sez. 5):
- **Reps** (o durata per `TIME_BASED`)
- **Peso** (nascosto/precompilato/derivato a seconda del tipo, come da istruzioni di logging)
- **Recupero** per quella serie (default preso da `RoutineExercise.restSeconds` se presente, editabile per singola serie)

Al tap su "fine serie", la serie viene marcata `completedAt` e **parte automaticamente il timer di recupero** con la durata impostata per quella serie.

### 6.3 Timer di recupero: barra in basso
- Vive in una barra fissa in fondo allo schermo (sez. 2.5), sopra la bottom nav.
- Mostra il conto alla rovescia in grande.
- Due bottoni **-15s / +15s** per aggiustare al volo la durata rimanente.
- Un bottone/gesto per **saltare** il recupero e passare subito alla serie successiva.
- Notifica sonora/vibrazione leggera allo scadere, coerente con l'estetica "calma" del design system.

### 6.4 Riferimento "ultima volta"
Per ogni serie in corso di compilazione, viene mostrato come sottotitolo/placeholder il valore registrato **l'ultima volta che quell'esercizio è stato eseguito** (non necessariamente l'ultimo allenamento in assoluto, ma l'ultima sessione che includeva quell'esercizio), allo stesso indice di serie: es. "Precedente: 60 kg × 8". Query: ultima `WorkoutSession` (per `startTime`) che contiene un `WorkoutExercise` per quell'`exerciseId`, con i relativi `SetEntry` ordinati per `setIndex`.

### 6.5 Reward al superamento del record
Quando una serie completata (non warmup) supera il record personale per quell'esercizio, l'app marca `isPR = true` e mostra un badge/animazione (stella o trofeo, coerente con la palette accento) accanto alla riga, più un leggero feedback aptico. Il criterio di "record" dipende dal `weightType`:
- `FREE_WEIGHT` / `MACHINE_STACK` / `ASSISTED`: nuovo **peso massimo** mai registrato per l'esercizio.
- `BODYWEIGHT`: nuovo **massimo numero di reps** in una singola serie (il peso è costante = corpo).
- `BODYWEIGHT_PLUS_LOAD`: nuovo massimo di **carico totale stimato** (peso corporeo al momento + sovraccarico).
- `TIME_BASED`: nuova **durata massima**.

---

## 7. Peso corporeo e volume per esercizi a corpo libero

- Schermata dedicata (in Progressi) per loggare il **peso corporeo** nel tempo (`BodyMetric`), con grafico andamento.
- Per gli esercizi `BODYWEIGHT` e `BODYWEIGHT_PLUS_LOAD`, il volume di una serie = (ultimo peso corporeo registrato prima/durante la sessione, + eventuale sovraccarico) × reps.
- Se l'utente non ha mai registrato il peso corporeo, l'app lo richiede una tantum al primo utilizzo di un esercizio a corpo libero (mai in modo invasivo/ricorrente oltre il necessario) e permette di aggiornarlo quando vuole.
- Per `ASSISTED`, il volume sottrae il peso di assistenza dal peso corporeo.

---

## 8. Playlist: avvio diretto nel servizio collegato

- La `Routine` salva l'URL della playlist (Spotify o YouTube Music) incollato dall'utente.
- Da un link Spotify tipo `https://open.spotify.com/playlist/ID` si ricava l'URI nativo `spotify:playlist:ID`: aprendolo con `Intent.ACTION_VIEW`, Spotify (se installato) si apre **avviando direttamente la riproduzione** di quella playlist — non solo la app sulla home.
- Per YouTube Music, l'intent punta esplicitamente al package `com.google.android.apps.youtube.music` con l'URL `https://music.youtube.com/playlist?list=ID`: l'app si apre sulla schermata della playlist (l'avvio automatico della riproduzione non è garantito quanto su Spotify — va verificato in fase di sviluppo e, se necessario, l'utente farà un tap in più su "play").
- Se l'app collegata non è installata sul device, fallback al link web nel browser di sistema.
- Un bottone "▶︎ Riproduci" ben visibile nella schermata routine/allenamento in corso richiama questo intent.

---

## 9. Funzionalità core → mapping requisiti (riepilogo)

| Requisito | Dove è trattato |
|---|---|
| Gestione workout, schede editabili, set con reps/peso/recupero | Sez. 6 |
| Timer di recupero in basso, skippabile, ±15s | Sez. 6.3 |
| Esercizi custom + GIF | Sez. 4 (flusso invariato: form + storage locale) |
| Libreria con body diagram + descrizione + istruzioni logging | Sez. 5 |
| Playlist collegata, avvio diretto nel servizio | Sez. 8 |
| Light/Dark mode | Sez. 2 |
| No social in-app | Principi (sez. 0) |
| Condivisione stile Strava | Invariato — vedi Fase 7 in roadmap |
| Dashboard allenamenti passati | Tab "Dashboard" (sez. 2.4) |
| "Ultima volta" per esercizio | Sez. 6.4 |
| Reward al superamento del massimale | Sez. 6.5 |
| Peso corporeo e volume bodyweight | Sez. 7 |
| Donazioni Buy Me a Coffee | Sez. 10 |
| Navbar in basso, navigazione fluida, più pagine | Sez. 2.4 |

---

## 10. Donazioni: Buy Me a Coffee

- Nessun SDK di pagamento in-app: usa l'URL pubblico della tua pagina (es. `https://buymeacoffee.com/tuonome`).
- In Impostazioni, voce "☕ Offrimi un caffè" che apre il link con **Custom Tabs**.
- Nessun Play Billing necessario: è una donazione pura, senza vantaggi in cambio (niente "rimuovi pubblicità", niente funzioni premium) — prassi standard per app FOSS, compatibile anche con F-Droid.

---

## 11. Roadmap a fasi (pensata per esecuzione con Claude Code)

**Fase 0 — Setup progetto + design system**
Scaffold Compose/Room/Navigation. Palette, tipografia (Inter), componenti base (card, badge, liste raggruppate), bottom nav a 4 voci. Criterio: app che builda, naviga tra le 4 tab vuote con lo stile visivo target.

**Fase 1 — Data layer completo**
Tutte le entità Room di sez. 3 (`Exercise` con `weightType`/`description`/`loggingInstructions`, `Routine`+`RoutineExercise`, `WorkoutSession`+`WorkoutExercise`+`SetEntry`, `BodyMetric`) e DAO/repository di base. Criterio: schema migrato, coperto da test unitari minimi sulle query chiave (max peso storico, ultima sessione per esercizio).

**Fase 2 — Sessione di allenamento (cuore dell'app)**
Schermata "allenamento in corso": schede esercizio in sequenza editabili (aggiungi/rimuovi/riordina esercizi e serie), input set secondo `weightType`, riferimento "ultima volta", timer di recupero in basso con ±15s e skip, badge reward al PR. Criterio: un allenamento intero è loggabile con tutte queste interazioni, dati persistiti correttamente.

**Fase 3 — Libreria esercizi**
Import/seed di `free-exercise-db` arricchito con `weightType`/`description`/`loggingInstructions`. UI libreria con filtri, dettaglio con body diagram colorato + descrizione + istruzioni logging. Criterio: libreria offline sfogliabile e coerente con la sez. 5.

**Fase 4 — Esercizi custom**
Form creazione esercizio utente con GIF da galleria (storage interno). Criterio: esercizio custom usabile in una routine come uno di libreria.

**Fase 5 — Routine e playlist**
Editor routine (`RoutineExercise`: set/reps/peso/recupero target per esercizio). Campo playlist con bottone "Riproduci" che avvia la riproduzione nel servizio collegato (sez. 8). Criterio: da una routine parte un allenamento pre-compilato e la playlist si avvia con un tap.

**Fase 6 — Dashboard e progressi**
Dashboard storico allenamenti. Grafici volume/PR per esercizio, anelli settimanali, heatmap stile GitHub, schermata peso corporeo con calcolo volume bodyweight (sez. 7). Criterio: dashboard e progressi con dati reali dallo storico.

**Fase 7 — Condivisione stile Strava**
Generazione immagine riepilogo sessione (Compose → Bitmap) + share intent verso Instagram/altre app.

**Fase 8 — Impostazioni, tema e donazioni**
Toggle light/dark manuale, info app/licenza, bottone "Offrimi un caffè" (Custom Tabs).

**Fase 9 — Rifinitura e performance**
Avvio a freddo, ProGuard/R8, baseline profile, test su device di fascia bassa, coerenza del design system, edge case (permessi galleria, storage pieno, app playlist non installata).

---

## 12. Nota su licenza e nome del progetto

Essendo FOSS, scegli una licenza permissiva/copyleft coerente con i dataset usati (es. GPL-3.0 o MIT per il codice; verifica sempre la licenza specifica del dataset esercizi che importi). Le donazioni via Buy Me a Coffee restano un supporto volontario, non un acquisto, e non richiedono modifiche al modello di licenza.

---

## 13. Prossimi passi pratici

1. Nome scelto: **Eina** (gioco di parole su "Einaudi") — inizializza il repo con package `com.<org>.eina`.
2. Crea la pagina Buy Me a Coffee e tieni pronto l'URL per la Fase 8.
3. Genera lo scaffold + design system (Fase 0) — primo blocco da dare a Claude Code.
4. Usa il file `CLAUDE.md` (già pronto, con schema dati in Kotlin, design tokens concreti e logica di dominio scritta) come contesto per Claude Code, così può lavorare in autonomia fase per fase con il minimo di decisioni ambigue da prendere al volo.
