package com.dxnd.viper4android.audio

data class AudioDevice(
    val id: String,
    val name: String,
    val type: Int,
    val isHeadphone: Boolean,
) {
    companion object {
        const val ID_SPEAKER = "speaker"
        const val ID_WIRED = "wired_headphone"

        val DEFAULT_SPEAKER = AudioDevice(ID_SPEAKER, "Speaker", 0, false)
    }
}
