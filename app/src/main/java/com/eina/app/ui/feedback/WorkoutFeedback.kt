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
 * Physical workout feedback: rest-timer beep, vibration and tap haptics. Each channel can be
 * switched off in the settings, in which case the call is a no-op.
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

    /** End of the rest timer: notification tone plus a double vibration. */
    fun restTimerFinished() {
        if (settings.timerSoundEnabled.value) playBeep()
        if (settings.timerVibrationEnabled.value) vibrateWaveform(longArrayOf(0, 220, 130, 220))
    }

    /**
     * Tap feedback on every control. EFFECT_TICK and DEFAULT_AMPLITUDE are imperceptible on many
     * devices, so EFFECT_HEAVY_CLICK is preferred where available: linear actuators render it as a
     * sharp knock.
     *
     * That predefined effect is not guaranteed: on rotating-mass motors `areEffectsSupported`
     * answers NO and some firmwares do not fall back at all — the app asks for a vibration and
     * nothing happens, with no error. It is therefore requested only when the device claims
     * support, otherwise a one-shot is used: longer without amplitude control, since a rotating
     * motor has to spin up and 30 ms are not enough to feel it.
     */
    fun haptic() {
        if (!settings.hapticsEnabled.value) return
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        // Taps go through the unattenuated channel too: on feedback channels several vendors lower
        // or zero the amplitude, making the app switch produce nothing perceptible.
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
     * Only an explicit NO counts as unsupported: the query exists from Android 11 on, and below
     * that version (or on UNKNOWN) the predefined effect is worth trying.
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
     * The "touch" and "hardware feedback" usages are attenuated or zeroed by the system depending
     * on the phone settings, so every vibration goes through the unattenuated channel, kept very
     * short for taps.
     */
    private fun Vibrator.vibrateCompat(effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrate(effect, legacyAttributes)
        }
    }

    /** Single instance: the attributes never change and every tap used to build a new one. */
    private val legacyAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    // ToneGenerator needs no audio asset: it uses the system tones and is released shortly after
    // playback, instead of holding an AudioTrack open for the whole session.
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
