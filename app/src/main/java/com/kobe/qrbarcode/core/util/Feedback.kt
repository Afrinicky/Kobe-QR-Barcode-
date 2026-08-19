package com.kobe.qrbarcode.core.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Short haptic + audible confirmation when a code is recognised. */
object Feedback {

    fun onScan(context: Context, vibrate: Boolean, sound: Boolean) {
        if (vibrate) vibrate(context, 45)
        if (sound) beep()
    }

    fun vibrate(context: Context, milliseconds: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    private fun beep() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                { tone.release() },
                250
            )
        }
    }
}
