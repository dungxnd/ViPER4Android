package com.dxnd.viper4android.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.dxnd.viper4android.utils.FileLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton audio-output device detector.
 *
 * **Lifecycle**: Process-scoped. The [AudioDeviceCallback] is registered once in `init`
 * and lives for the entire application process lifecycle. The Android framework releases
 * all callbacks automatically when the process dies.
 *
 * Do **not** expose a public `stop()` method on this class. Because it is a Hilt
 * `@Singleton`, calling `stop()` from any one consumer would silently break routing
 * detection for every other consumer. Use [stopForTest] in unit/instrumented tests only.
 */
class AudioOutputDetector(
    context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Initialized to DEFAULT_SPEAKER; the init block sets the real value AFTER the callback
    // is registered so no device-change event can be missed between construction and first read.
    private val _activeDevice = MutableStateFlow(AudioDevice.DEFAULT_SPEAKER)
    val activeDevice: StateFlow<AudioDevice> = _activeDevice.asStateFlow()

    private val callback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                val device = detectActiveDevice(audioManager)
                // Debug level: fires on every BT/USB connect event — keep it cheap.
                FileLogger.d(
                    "AudioOutput",
                    "Device added → active=${device.name} (headphone=${device.isHeadphone}, id=${device.id})",
                )
                _activeDevice.value = device
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                val device = detectActiveDevice(audioManager)
                FileLogger.d(
                    "AudioOutput",
                    "Device removed → active=${device.name} (headphone=${device.isHeadphone}, id=${device.id})",
                )
                _activeDevice.value = device
            }
        }

    init {
        // Register callback FIRST so no device event fires in the gap before initial detection.
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        val initialDevice = detectActiveDevice(audioManager)
        _activeDevice.value = initialDevice
        // Info level: fires exactly once per process start — acceptable cost.
        FileLogger.i(
            "AudioOutput",
            "Init: headphone=${initialDevice.isHeadphone} device=${initialDevice.name} id=${initialDevice.id}",
        )
    }

    /**
     * Unregisters the [AudioDeviceCallback]. **For tests only.**
     * Never call this from production code — see class KDoc.
     */
    @VisibleForTesting
    internal fun stopForTest() {
        audioManager.unregisterAudioDeviceCallback(callback)
    }

    @Suppress("NewApi") // BLE constants (API 31/33) are safe: minSdk=28, set at compile time.
    companion object {
        // Priority for Android <13 fallback sort (higher = preferred routing).
        private const val PRIORITY_BT = 4
        private const val PRIORITY_USB = 3
        private const val PRIORITY_WIRED = 2
        private const val PRIORITY_SPEAKER = 0
        private const val PRIORITY_OTHER = 1

        /**
         * Bluetooth device types. Includes BLE_HEADSET (API 31), BLE_SPEAKER (API 31),
         * and BLE_BROADCAST (API 33) — all referenced as compile-time constants under
         * @Suppress("NewApi") since the companion is never instantiated before the API is
         * available at runtime on the devices that actually expose these types.
         */
        private val BT_TYPES =
            setOf(
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_HEARING_AID,
                AudioDeviceInfo.TYPE_BLE_HEADSET,   // API 31
                AudioDeviceInfo.TYPE_BLE_SPEAKER,   // API 31
                AudioDeviceInfo.TYPE_BLE_BROADCAST, // API 33
            )

        /** USB audio devices. TYPE_USB_DEVICE covers external DACs/dongles (e.g. Apple, Fiio). */
        private val USB_TYPES =
            setOf(
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE, // API 23 — external DACs
            )

        /**
         * Android Auto wired projection device type.
         * When the phone is in AOAP accessory mode (USB cable to head unit), Android
         * registers the car head unit as TYPE_USB_ACCESSORY. Audio is routed to a
         * normal MixerThread for this output, so session-0 INSERT effects apply.
         */
        private const val TYPE_USB_ACCESSORY = AudioDeviceInfo.TYPE_USB_ACCESSORY

        /**
         * Wired/line-level outputs. TYPE_LINE_ANALOG, TYPE_LINE_DIGITAL, and TYPE_AUX_LINE are
         * all API 23 (below minSdk=28) so no version guard is required.
         */
        private val WIRED_TYPES =
            setOf(
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_LINE_ANALOG,  // car AUX / external amps
                AudioDeviceInfo.TYPE_LINE_DIGITAL, // SPDIF / optical
                AudioDeviceInfo.TYPE_AUX_LINE,     // auxiliary line-level
            )

        /** Union of all external output types used for headphone detection. */
        private val HEADPHONE_TYPES = BT_TYPES + USB_TYPES + WIRED_TYPES

        private fun detectActiveDevice(audioManager: AudioManager): AudioDevice {
            val routedOutputs =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val mediaAttrs =
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    audioManager.getAudioDevicesForAttributes(mediaAttrs).filter { it.isSink }
                } else {
                    emptyList()
                }

            val outputs =
                routedOutputs.ifEmpty {
                    audioManager
                        .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                        .filter { it.isSink }
                        // On Android <13, getDevices() returns all connected outputs in an
                        // arbitrary order — not the active route. Sort by priority so we
                        // consistently prefer BT > USB > Wired over the built-in speaker.
                        .sortedByDescending { getDevicePriority(it.type) }
                }

            for (dev in outputs) {
                FileLogger.d(
                    "AudioOutput",
                    "  output: type=${dev.type} name=${dev.productName} addr=${dev.address} id=${dev.id}",
                )
            }

            // Android Auto wired projection (AOAP accessory mode): TYPE_USB_ACCESSORY is
            // registered when the phone connects via USB cable to a head unit. Audio output
            // goes through a standard MixerThread so session-0 INSERT effects apply.
            // Also covers legacy AOA 2.0 audio docks — same session-0 behaviour is correct.
            if (outputs.any { it.type == TYPE_USB_ACCESSORY }) {
                FileLogger.i("AudioOutput", "AOAP/Android Auto accessory detected (TYPE_USB_ACCESSORY) → global mode")
                return AudioDevice.ANDROID_AUTO
            }

            // When a real BT device (non-zero MAC) is present, drop zero-MAC BT entries.
            // Android adds the SCO call-audio profile alongside A2DP with address "00:00:00:00:00:00";
            // that second onAudioDevicesAdded event must not override the real BT speaker.
            val hasBtWithRealMac = outputs.any { it.type in BT_TYPES && it.address.isRealBtAddress() }
            val candidates = if (hasBtWithRealMac) {
                outputs.filter { it.type !in BT_TYPES || it.address.isRealBtAddress() }
            } else {
                outputs
            }

            val headphone = candidates.firstOrNull { it.type in HEADPHONE_TYPES }
            if (headphone != null) {
                // getAddress() is API 28; minSdk=28, so no version guard required.
                val productName = headphone.productName?.toString()?.takeIf { it.isNotBlank() }
                return when {
                    headphone.type in BT_TYPES ->
                        AudioDevice(
                            id = buildDeviceId("bt", headphone.address, productName, headphone.type, headphone.id),
                            name = productName ?: getBtTypeName(headphone.type),
                            type = headphone.type,
                            isHeadphone = true,
                        )
                    headphone.type in USB_TYPES ->
                        AudioDevice(
                            id = buildDeviceId("usb", headphone.address, productName, headphone.type, headphone.id),
                            name = productName ?: "USB Audio",
                            type = headphone.type,
                            isHeadphone = true,
                        )
                    else ->
                        AudioDevice(
                            id = AudioDevice.ID_WIRED,
                            name = productName ?: "Wired Headphone",
                            type = headphone.type,
                            isHeadphone = true,
                        )
                }
            }

            return AudioDevice.DEFAULT_SPEAKER
        }

        private fun getDevicePriority(type: Int): Int =
            when (type) {
                in BT_TYPES -> PRIORITY_BT
                TYPE_USB_ACCESSORY -> PRIORITY_USB  // Android Auto wired
                in USB_TYPES -> PRIORITY_USB
                in WIRED_TYPES -> PRIORITY_WIRED
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> PRIORITY_SPEAKER
                else -> PRIORITY_OTHER
            }

        /**
         * Builds a stable, collision-safe device ID using a priority chain:
         *  1. Non-blank, non-zero hardware address (MAC for BT, USB bus path) — stable & unique.
         *  2. Non-blank productName + type — stable per model; avoids type-only collisions.
         *  3. Session-scoped device id — not stable across reconnects (creates orphan rows)
         *     but guarantees NO collision with a different physical device.
         */
        private fun buildDeviceId(
            prefix: String,
            address: String,
            productName: String?,
            type: Int,
            sessionId: Int,
        ): String {
            if (address.isRealBtAddress()) return address
            if (!productName.isNullOrBlank()) {
                return "${prefix}_${sanitizeName(productName)}_t${type}"
            }
            return "${prefix}_id_${sessionId}"
        }

        /**
         * Returns true if this is a non-blank hardware address that is NOT the all-zero
         * placeholder ("00:00:00:00:00:00") Android assigns to SCO profile devices.
         */
        private fun String.isRealBtAddress(): Boolean =
            isNotBlank() && this != "00:00:00:00:00:00"

        /** Lowercases and collapses non-alphanumeric runs to a single '_'. */
        private fun sanitizeName(name: String): String {
            val cleaned = name.lowercase(java.util.Locale.ROOT)
                .replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
                .trim('_')
            return cleaned.ifBlank { "dev_${name.hashCode().toUInt()}" }
        }

        private fun getBtTypeName(type: Int): String =
            when (type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
                AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing Aid"
                AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE Headset"
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE Speaker"
                AudioDeviceInfo.TYPE_BLE_BROADCAST -> "BLE Broadcast"
                else -> "Bluetooth"
            }
    }
}
