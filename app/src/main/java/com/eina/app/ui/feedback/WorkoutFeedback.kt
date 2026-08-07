package com.eina.app.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
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

    /**
     * Micro-feedback su ogni tocco dei controlli. EFFECT_TICK e' impercettibile su molti
     * dispositivi: si usa un one-shot breve ad ampiezza piena, con attributi "touch" cosi'
     * la vibrazione non viene soppressa quando la suoneria e' in silenzioso.
     */
    fun haptic() {
        if (!settings.hapticsEnabled.value) return
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        vibrator.vibrateCompat(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE), alarm = false)
    }

    private fun vibrateWaveform(pattern: LongArray) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        vibrator.vibrateCompat(VibrationEffect.createWaveform(pattern, -1), alarm = true)
    }

    /**
     * Gli usage "touch" vengono azzerati dal sistema quando la vibrazione al tocco e' disattivata
     * nelle impostazioni del telefono: il toggle dell'app non avrebbe alcun effetto percepibile.
     * Si usa quindi HARDWARE_FEEDBACK per i tap e ALARM per la fine del recupero.
     */
    private fun Vibrator.vibrateCompat(effect: VibrationEffect, alarm: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val usage = if (alarm) VibrationAttributes.USAGE_ALARM else VibrationAttributes.USAGE_HARDWARE_FEEDBACK
            vibrate(effect, VibrationAttributes.createForUsage(usage))
        } else {
            val attributes = AudioAttributes.Builder()
                .setUsage(if (alarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            vibrate(effect, attributes)
        }
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
