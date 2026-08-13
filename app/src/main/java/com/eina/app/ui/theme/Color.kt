package com.eina.app.ui.theme

import androidx.compose.ui.graphics.Color

// Palette definitiva: app solo in light mode.
// Restyle: i neutri perdono la dominante marrone (il fondo #FBF6F2 ingialliva le foto degli
// esercizi e faceva sembrare le isole bianche "sporche"). Ora sono grigi appena tiepidi, alla
// maniera dei fondi raggruppati di iOS: il colore in pagina lo mette solo l'accento.
val LightBackground = Color(0xFFF4F3F1)
val LightSurface = Color(0xFFFFFFFF)

/** Accento primario: arancio tramonto. CTA, stati attivi, valori in evidenza. */
val AccentPrimary = Color(0xFFF97348)
val AccentPrimaryDark = Color(0xFFD4501F)
val AccentPrimarySoft = Color(0xFFFFEADF)

/**
 * Rampa dell'accento, da ambra a magenta. Serve per le superfici grandi (hero della Dashboard,
 * anelli di progresso, marchio): un arancio pieno su un'area larga si legge come un blocco di
 * vernice, la rampa gli da' profondita' senza aggiungere colori estranei alla palette.
 */
val AccentRampStart = Color(0xFFFFA23A)
val AccentRampEnd = Color(0xFFF9436B)

val LightSurfaceSunken = Color(0xFFEFEEEB)

/**
 * Incassato chiaro, per le superfici che stanno *dentro* una card bianca e occupano molto spazio:
 * la tabella delle serie del riepilogo e i campi di peso e ripetizioni. Con il grigio pieno quei
 * riquadri pesavano piu' dei numeri che contengono. Sul fondo della pagina resta
 * [LightSurfaceSunken]: li' un grigio piu' chiaro del fondo non si vedrebbe affatto.
 */
val LightSurfaceSunkenSoft = Color(0xFFF7F6F4)
val LightTextSecondary = Color(0xFF7C7A75)
val LightOutlineSubtle = Color(0xFFE7E5E1)
val LightOnBackground = Color(0xFF1C1B19)

/**
 * Colore per grandezza misurata. Non e' una tavolozza nuova: sono gli stessi colori dei gruppi
 * muscolari, riusati con un significato fisso. Serve perche' le schermate erano bianche con un
 * solo arancio: ogni numero ha ora la sua tinta e le metriche si riconoscono a colpo d'occhio
 * anche senza leggere l'etichetta.
 *
 * Il colore sta sull'icona e sull'etichetta, mai sul numero: il valore resta nero, altrimenti in
 * una riga di tre metriche non si capisce piu' quale sia quella importante.
 */
object MetricColors {
    val Volume = AccentPrimary
    val Sets = Color(0xFF9775FA)
    val Duration = Color(0xFF4D96FF)
    val Records = AccentRampEnd
    val Bodyweight = Color(0xFF20C997)
    val Streak = Color(0xFFF06595)
    // Dati dell'orologio: il cuore prende il rosso della palette muscolare, le calorie l'ambra
    // che apre la rampa dell'accento. Nessun colore nuovo in tavolozza.
    val Heart = Color(0xFFFF6B6B)
    val Calories = Color(0xFFFFA23A)
}

object MuscleGroupColors {
    val ChestPush = Color(0xFFFF6B6B)
    val BackPull = Color(0xFF4D96FF)
    val Legs = Color(0xFF51CF66)
    val Shoulders = Color(0xFF9775FA)
    val Arms = Color(0xFFFFB020)
    val Core = Color(0xFF20C997)
    val Cardio = Color(0xFFF06595)
    // Fase 32: il collo era l'unico muscolo della categoria "Altro", che aveva il grigio di
    // chi non sa dove mettersi. Ora e' una categoria sua e ha un colore suo.
    val Neck = Color(0xFFA9746E)
}
