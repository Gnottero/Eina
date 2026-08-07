package com.eina.app.ui.feedback

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Tap aptico dei controlli island. Fornito una volta in MainActivity a partire da
 * [WorkoutFeedback]: i componenti riutilizzabili lo invocano senza dipendere da Koin,
 * e con l'aptica disattivata resta un no-op.
 */
val LocalHapticTap = staticCompositionLocalOf<() -> Unit> { {} }
