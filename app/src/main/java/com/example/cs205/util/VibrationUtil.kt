package com.example.cs205.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object VibrationUtil {
    private const val TAG = "VibrationUtil"

    fun vibrate(context: Context, durationMs: Long = 100) {
        Log.d(TAG, "Attempting to vibrate for $durationMs ms")
        
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            Log.d(TAG, "Vibration triggered using VibrationEffect")
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
            Log.d(TAG, "Vibration triggered using legacy API")
        }
    }
} 