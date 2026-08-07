package com.eina.app.ui.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.eina.app.data.prefs.SettingsRepository

/**
 * Feedback fisico dell'allenamento: beep di fine recupero, vibrazione e micro-feedback aptico.
 * Ogni canale e' disattivabile dalle impostazioni; se un canale e' spento la chiamata e' un no-op.
 */
class WorkoutFeedback(
    private val context: Context,
    private val settings: SettingsRepository
) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Fine del timer di recupero: suono di notifica + doppia vibrazione. */
    fun restTimerFinished() {
        if (settings.timerSoundEnabled.value) playBeep()
        if (settings.timerVibrationEnabled.value) vibrateWaveform(longArrayOf(0, 220, 130, 220))
    }

    /** Micro-feedback su azioni frequenti (serie completata, timer saltato). */
    fun haptic() {
        if (!settings.hapticsEnabled.value) return
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibrateWaveform(pattern: LongArray) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    // ToneGenerator non ha bisogno di asset audio: usa i toni di sistema e si rilascia da solo
    // poco dopo la riproduzione, evitando di tenere aperta una AudioTrack per tutta la sessione.
    private fun playBeep() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, TONE_DURATION_MS)
            Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, TONE_DURATION_MS + 200L)
        }
    }

    private companion object {
        const val TONE_VOLUME = 90
        const val TONE_DURATION_MS = 700
    }
}
