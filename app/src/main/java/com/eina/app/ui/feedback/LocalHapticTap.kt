package com.eina.app.ui.feedback

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Haptic tap of the island controls. Provided once in MainActivity from [WorkoutFeedback], so
 * reusable components can invoke it without depending on Koin; with haptics off it is a no-op.
 */
val LocalHapticTap = staticCompositionLocalOf<() -> Unit> { {} }
