package com.dxnd.viper4android.audio

data class AudioDevice(
    val id: String,
    val name: String,
    val type: Int,
    val isHeadphone: Boolean,
    /**
     * True when this device is an Android Auto wired USB connection
     * (AUDIO_DEVICE_OUT_USB_ACCESSORY / AudioDeviceInfo.TYPE_USB_ACCESSORY).
     * ViperService uses this flag to force global (session-0) mode so the
     * AAP projection stream is processed regardless of session broadcast.
     */
    val isAndroidAuto: Boolean = false,
) {
    companion object {
        const val ID_SPEAKER = "speaker"
        const val ID_WIRED = "wired_headphone"
        const val ID_ANDROID_AUTO = "android_auto"

        val DEFAULT_SPEAKER = AudioDevice(ID_SPEAKER, "Speaker", 0, false)
        val ANDROID_AUTO = AudioDevice(ID_ANDROID_AUTO, "Android Auto", 0, true, isAndroidAuto = true)
    }
}
