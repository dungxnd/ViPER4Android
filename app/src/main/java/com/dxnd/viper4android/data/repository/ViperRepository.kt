package com.dxnd.viper4android.data.repository

import android.media.audiofx.AudioEffect
import android.os.Build
import com.dxnd.viper4android.viper.ViperEffect
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dxnd.viper4android.data.dao.DeviceSettingsDao
import com.dxnd.viper4android.data.dao.DsPresetDao
import com.dxnd.viper4android.data.dao.EqPresetDao
import com.dxnd.viper4android.data.dao.PresetDao
import com.dxnd.viper4android.data.model.DeviceSettings
import com.dxnd.viper4android.data.model.DsPreset
import com.dxnd.viper4android.data.model.EqPreset
import com.dxnd.viper4android.data.model.Preset
import com.dxnd.viper4android.effect.EffectState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViperRepository
    @Inject
    constructor(
        private val presetDao: PresetDao,
        private val eqPresetDao: EqPresetDao,
        private val dsPresetDao: DsPresetDao,
        private val deviceSettingsDao: DeviceSettingsDao,
        private val dataStore: DataStore<Preferences>,
    ) {
        fun getAllPresets(): Flow<List<Preset>> = presetDao.getAll()

        suspend fun getPresetById(id: Long): Preset? = presetDao.getById(id)

        suspend fun getPresetByName(name: String): Preset? = presetDao.getByName(name)

        suspend fun savePreset(preset: Preset): Long = presetDao.insert(preset)

        suspend fun updatePreset(preset: Preset) = presetDao.update(preset)

        suspend fun deletePreset(preset: Preset) = presetDao.delete(preset)

        suspend fun deletePresetById(id: Long) = presetDao.deleteById(id)

        suspend fun deleteAllPresets() = presetDao.deleteAll()

        fun getEqPresetsByBandCount(bandCount: Int): Flow<List<EqPreset>> = eqPresetDao.getByBandCount(bandCount)

        suspend fun getEqPresetById(id: Long): EqPreset? = eqPresetDao.getById(id)

        suspend fun saveEqPreset(preset: EqPreset): Long = eqPresetDao.insert(preset)

        suspend fun renameEqPreset(
            id: Long,
            name: String,
        ) = eqPresetDao.rename(id, name)

        suspend fun deleteEqPresetById(id: Long) = eqPresetDao.deleteById(id)

        fun getAllDsPresets(): Flow<List<DsPreset>> = dsPresetDao.getAll()

        suspend fun getDsPresetById(id: Long): DsPreset? = dsPresetDao.getById(id)

        suspend fun saveDsPreset(preset: DsPreset): Long = dsPresetDao.insert(preset)

        suspend fun renameDsPreset(
            id: Long,
            name: String,
        ) = dsPresetDao.rename(id, name)

        suspend fun deleteDsPresetById(id: Long) = dsPresetDao.deleteById(id)

        /**
         * Emits the device-id + [EffectState] that [ViperService] most recently loaded from
         * the database and applied to the DSP engine on a hardware device-switch event.
         * [MainViewModel] observes this to update its UI state **without** performing a
         * parallel Room read or re-dispatching to the DSP — eliminating the dual-consumer race.
         */
        private val _activeDeviceState = MutableStateFlow<Pair<String, EffectState>?>(null)
        val activeDeviceState: StateFlow<Pair<String, EffectState>?> = _activeDeviceState.asStateFlow()

        /** Called exclusively by [ViperService] after it has loaded and applied a device switch. */
        fun publishActiveDeviceState(deviceId: String, state: EffectState) {
            _activeDeviceState.value = deviceId to state
        }

        fun getAllDeviceSettings(): Flow<List<DeviceSettings>> = deviceSettingsDao.getAll()

        suspend fun getDeviceSettings(deviceId: String): DeviceSettings? = deviceSettingsDao.getByDeviceId(deviceId)

        suspend fun saveDeviceSettings(settings: DeviceSettings) = deviceSettingsDao.upsert(settings)

        suspend fun renameDevice(
            deviceId: String,
            name: String,
        ) = deviceSettingsDao.rename(deviceId, name)

        suspend fun deleteDeviceSettings(deviceId: String) = deviceSettingsDao.deleteByDeviceId(deviceId)

        suspend fun updateDeviceLastConnected(deviceId: String) =
            deviceSettingsDao.updateLastConnected(deviceId, System.currentTimeMillis())

        fun getBooleanPreference(
            key: String,
            default: Boolean = false,
        ): Flow<Boolean> =
            flow {
                ensureV2Initialized()
                emitAll(dataStore.data.map { it[booleanPreferencesKey(key)] ?: default })
            }

        // noinspection PrivateApi
        // noinspection DiscouragedPrivateApi
        val aidlMode: Boolean by lazy {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@lazy false

            // Check 1: Module install indicator written by install.sh.
            // install.sh writes "aidl" or "legacy" to $MODPATH/aidl_mode.txt after running
            // its full HAL detection (FS signals, VINTF, Binder service check).  This is the
            // ground truth for what was actually installed — trust it before any runtime probe.
            val modulePaths = listOf(
                "/data/adb/modules/ViPER4Android/aidl_mode.txt",
                "/data/adb/modules/ViPER4Android-RE/aidl_mode.txt",
            )
            for (path in modulePaths) {
                val content = runCatching { java.io.File(path).readText().trim() }.getOrNull()
                if (content == "aidl") return@lazy true
                if (content == "legacy") return@lazy false
            }

            // Check 2: Registered audio effects in the framework.
            // If the driver loaded correctly, queryEffects() tells us which pipeline is active.
            // If the driver is NOT loaded (wrong install / missing HAL), neither type will appear
            // and we fall through to Check 3.
            val descriptors = runCatching { AudioEffect.queryEffects() }.getOrNull()
            if (descriptors != null) {
                val hasAidlEffect = descriptors.any { it.type == ViperEffect.EFFECT_TYPE_UUID_AIDL }
                val hasLegacyEffect = descriptors.any { it.type == ViperEffect.EFFECT_TYPE_UUID }
                if (hasAidlEffect) return@lazy true
                if (hasLegacyEffect) return@lazy false
            }

            // Check 3: REMOVED — ServiceManager.listServices() is NOT a reliable AIDL indicator.
            // On Android 13+, audioserver registers its own in-process IFactory AIDL proxy in
            // the regular ServiceManager even when the underlying vendor HAL is pure HIDL
            // (e.g. android.hardware.audio.effect@7.0::IEffectsFactory via hwbinder).
            // Querying ServiceManager would return "found" on every Android 13+ Qualcomm/OEM
            // device that runs HIDL vendor audio → guaranteed false-positive → versionCode=-1.
            // The ground truth is lshal (hwbinder) or the VINTF manifest, not ServiceManager.

            // Check 4: Safe default — legacy.
            // API level alone is NOT a reliable indicator: many Android 15 OEMs ship the
            // legacy audio effect HAL.  Defaulting to legacy means the driver simply won't
            // load (graceful failure) rather than crashing AudioEffect with an AIDL type UUID
            // on a device that has no AIDL HAL (→ versionCode=-1, samplingRate=unknown).
            false
        }

        suspend fun setBooleanPreference(
            key: String,
            value: Boolean,
        ) {
            ensureV2Initialized()
            dataStore.edit { it[booleanPreferencesKey(key)] = value }
        }

        fun getIntPreference(
            key: String,
            default: Int = 0,
        ): Flow<Int> =
            flow {
                ensureV2Initialized()
                emitAll(dataStore.data.map { it[intPreferencesKey(key)] ?: default })
            }

        suspend fun setIntPreference(
            key: String,
            value: Int,
        ) {
            ensureV2Initialized()
            dataStore.edit { it[intPreferencesKey(key)] = value }
        }

        fun getStringPreference(
            key: String,
            default: String = "",
        ): Flow<String> =
            flow {
                ensureV2Initialized()
                emitAll(dataStore.data.map { it[stringPreferencesKey(key)] ?: default })
            }

        suspend fun setStringPreference(
            key: String,
            value: String,
        ) {
            ensureV2Initialized()
            dataStore.edit { it[stringPreferencesKey(key)] = value }
        }

        @Volatile private var initDone = false
        private val initMutex = Mutex()

        suspend fun ensureV2Initialized() {
            if (initDone) return
            initMutex.withLock {
                if (initDone) return
                val flag =
                    dataStore.data.map {
                        it[booleanPreferencesKey(PREF_V2_INITIALIZED)] ?: false
                    }
                if (flag.first()) {
                    initDone = true
                    return
                }
                dataStore.edit { prefs ->
                    prefs.clear()
                    prefs[booleanPreferencesKey(PREF_V2_INITIALIZED)] = true
                }
                initDone = true
            }
        }

        companion object {
            const val PREF_MASTER_ENABLE = "master_enable"
            const val PREF_AUTO_START = "auto_start"
            const val PREF_GLOBAL_MODE = "global_mode"
            const val PREF_DEBUG_MODE = "debug_mode"
            const val PREF_V2_INITIALIZED = "v2_initialized"
        }
    }
