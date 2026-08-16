package com.dxnd.viper4android.effect

import kotlin.math.ln
import kotlin.math.roundToInt

object ParamRaw {
    fun fetCompressorThresholdF(dB: Int): Float = (dB / -60.0).toFloat()

    fun fetCompressorThreshold(dB: Int): Int = (fetCompressorThresholdF(dB) * 100f).roundToInt()

    fun fetCompressorKneeF(dB: Int): Float = (dB / 60.0).toFloat()

    fun fetCompressorKnee(dB: Int): Int = (fetCompressorKneeF(dB) * 100f).roundToInt()

    fun fetCompressorGainF(dB: Int): Float = (dB / 60.0).toFloat()

    fun fetCompressorGain(dB: Int): Int = (fetCompressorGainF(dB) * 100f).roundToInt()

    fun fetCompressorAttackMsF(ms: Int): Float {
        val timeSec = ms / 1000.0
        if (timeSec <= 0) return 0f
        val value = (ln(timeSec) + 9.21034) / 7.600903
        return value.toFloat().coerceIn(0f, 2f)
    }

    fun fetCompressorAttackMs(ms: Int): Int = (fetCompressorAttackMsF(ms) * 100f).roundToInt().coerceIn(0, 200)

    fun fetCompressorReleaseMsF(ms: Int): Float {
        val timeSec = ms / 1000.0
        if (timeSec <= 0) return 0f
        val value = (ln(timeSec) + 5.298317) / 5.991465
        return value.toFloat().coerceIn(0f, 2f)
    }

    fun fetCompressorReleaseMs(ms: Int): Int = (fetCompressorReleaseMsF(ms) * 100f).roundToInt().coerceIn(0, 200)

    fun spectrumExtensionExciter(value: Int): Int = (value * 5.6).toInt()

    fun spectrumExtensionExciterF(value: Int): Float = ((value * 5.6) / 100.0).toFloat()

    fun fieldSurroundMidImage(value: Int): Int = value * 10 + 100

    fun fieldSurroundDepth(value: Int): Int = value * 75 + 200

    fun fieldSurroundWidening(value: Int): Int = value * 100

    fun dynamicSystemStrength(value: Int): Int = value * 20 + 100

    fun bassFrequency(value: Int): Int = value + 15

    fun diffSurroundDelay(ms: Int): Int = ms * 100

    fun reverbRoomSize(value: Int): Int = value * 10

    fun reverbWidth(value: Int): Int = value * 10

    fun reverbDamp(value: Int): Int = value * 10
}
