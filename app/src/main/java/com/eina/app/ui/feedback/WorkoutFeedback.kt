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
     * Micro-feedback su ogni tocco dei controlli. EFFECT_TICK e DEFAULT_AMPLITUDE risultano
     * impercettibili su molti dispositivi: dove c'e' si preferisce l'effetto di sistema
     * EFFECT_HEAVY_CLICK, che i vibratori lineari rendono come un colpo secco.
     *
     * Il predefinito pero' non e' garantito: sui motori a massa rotante (telefoni di fascia bassa,
     * molti Android non di punta) `areEffectsSupported` risponde NO e certi firmware non fanno
     * nessun ripiego — l'app chiede una vibrazione, non succede niente e nessuno segnala errore.
     * Quindi lo si chiede solo se il dispositivo dice di saperlo fare, e altrimenti si scende a un
     * one-shot: piu' lungo dove non c'e' controllo d'ampiezza, perche' un motore rotante deve
     * partire prima di farsi sentire e 30 ms non bastano a percepirlo.
     */
    fun haptic() {
        if (!settings.hapticsEnabled.value) return
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        // Canale non attenuato anche per i tap: sui canali "feedback" diversi produttori
        // abbassano (o azzerano) l'ampiezza in base alle impostazioni di sistema, e il toggle
        // dell'app non produceva nulla di percepibile. L'effetto resta comunque brevissimo.
        vibrator.vibrateCompat(vibrator.tapEffect())
    }

    private fun Vibrator.tapEffect(): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && supportsHeavyClick()) {
            return VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        return if (hasAmplitudeControl()) {
            VibrationEffect.createOneShot(TAP_DURATION_MS, MAX_AMPLITUDE)
        } else {
            VibrationEffect.createOneShot(ROTARY_TAP_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    }

    /**
     * Solo un NO esplicito conta come "non supportato": la domanda esiste da Android 11 e sotto
     * quella versione (o con risposta UNKNOWN) tanto vale provare il predefinito.
     */
    private fun Vibrator.supportsHeavyClick(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        val support = runCatching {
            areEffectsSupported(VibrationEffect.EFFECT_HEAVY_CLICK).firstOrNull()
        }.getOrNull() ?: return true
        return support != Vibrator.VIBRATION_EFFECT_SUPPORT_NO
    }

    private fun vibrateWaveform(pattern: LongArray) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        vibrator.vibrateCompat(VibrationEffect.createWaveform(pattern, -1))
    }

    /**
     * Gli usage "touch" e "hardware feedback" vengono attenuati o azzerati dal sistema in base
     * alle impostazioni del telefono: il toggle dell'app non produceva nulla di percepibile.
     * Tutte le vibrazioni dell'app passano quindi dal canale non attenuato, restando pero'
     * brevissime per i tap.
     */
    private fun Vibrator.vibrateCompat(effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrate(effect, legacyAttributes)
        }
    }

    /** Un'istanza sola: gli attributi non cambiano mai e ogni tap ne costruiva una nuova. */
    private val legacyAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
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
        const val TAP_DURATION_MS = 30L
        const val ROTARY_TAP_DURATION_MS = 45L
        const val MAX_AMPLITUDE = 255
    }
}
