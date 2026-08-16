package com.dxnd.viper4android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dxnd.viper4android.data.dao.DeviceSettingsDao
import com.dxnd.viper4android.data.dao.DsPresetDao
import com.dxnd.viper4android.data.dao.EqPresetDao
import com.dxnd.viper4android.data.dao.PresetDao
import com.dxnd.viper4android.data.model.DeviceSettings
import com.dxnd.viper4android.data.model.DsPreset
import com.dxnd.viper4android.data.model.EqPreset
import com.dxnd.viper4android.data.model.Preset

@Database(
    entities = [Preset::class, EqPreset::class, DsPreset::class, DeviceSettings::class],
    version = 6,
    exportSchema = true,
)
abstract class ViperDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao

    abstract fun eqPresetDao(): EqPresetDao

    abstract fun dsPresetDao(): DsPresetDao

    abstract fun deviceSettingsDao(): DeviceSettingsDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `eq_presets` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`band_count` INTEGER NOT NULL, " +
                            "`bands` TEXT NOT NULL)",
                    )
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `ds_presets` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`x_low` INTEGER NOT NULL, " +
                            "`x_high` INTEGER NOT NULL, " +
                            "`y_low` INTEGER NOT NULL, " +
                            "`y_high` INTEGER NOT NULL, " +
                            "`side_gain_low` INTEGER NOT NULL, " +
                            "`side_gain_high` INTEGER NOT NULL)",
                    )
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `device_settings` (" +
                            "`device_id` TEXT NOT NULL PRIMARY KEY, " +
                            "`device_name` TEXT NOT NULL, " +
                            "`is_headphone` INTEGER NOT NULL, " +
                            "`settings_json` TEXT NOT NULL, " +
                            "`last_connected` INTEGER NOT NULL)",
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE eq_presets ADD COLUMN name_key TEXT DEFAULT NULL")
                    val builtins =
                        mapOf(
                            "Acoustic" to "eq_preset_acoustic",
                            "Bass Booster" to "eq_preset_bass_booster",
                            "Bass Reducer" to "eq_preset_bass_reducer",
                            "Classical" to "eq_preset_classical",
                            "Deep" to "eq_preset_deep",
                            "Flat" to "eq_preset_flat",
                            "R&B" to "eq_preset_rnb",
                            "Rock" to "eq_preset_rock",
                            "Small Speakers" to "eq_preset_small_speakers",
                            "Treble Booster" to "eq_preset_treble_booster",
                            "Treble Reducer" to "eq_preset_treble_reducer",
                            "Vocal Booster" to "eq_preset_vocal_booster",
                        )
                    for ((oldName, key) in builtins) {
                        db.execSQL(
                            "UPDATE eq_presets SET name_key = ? WHERE name = ?",
                            arrayOf(key, oldName),
                        )
                    }
                    db.execSQL("ALTER TABLE ds_presets ADD COLUMN name_key TEXT DEFAULT NULL")
                    val keptBuiltins =
                        mapOf(
                            "Extreme Headphone (v2)" to "ds_device_extreme_headphone_v2",
                            "High-End Headphone (v2)" to "ds_device_high_end_headphone_v2",
                            "Common Headphone (v2)" to "ds_device_common_headphone_v2",
                            "Low-End Headphone (v2)" to "ds_device_low_end_headphone_v2",
                            "Common Earphone (v2)" to "ds_device_common_earphone_v2",
                            "Extreme Headphone (v1)" to "ds_device_extreme_headphone_v1",
                            "High-End Headphone (v1)" to "ds_device_high_end_headphone_v1",
                            "Common Headphone (v1)" to "ds_device_common_headphone_v1",
                            "Common Earphone (v1)" to "ds_device_common_earphone_v1",
                        )
                    for ((oldName, key) in keptBuiltins) {
                        db.execSQL(
                            "UPDATE ds_presets SET name_key = ? WHERE name = ?",
                            arrayOf(key, oldName),
                        )
                    }
                }
            }

        val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS `presets`")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `presets` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`settings_json` TEXT NOT NULL, " +
                            "`created_at` INTEGER NOT NULL, " +
                            "`updated_at` INTEGER NOT NULL)",
                    )
                    db.execSQL("DELETE FROM `device_settings`")

                    db.execSQL(
                        "DELETE FROM eq_presets WHERE name_key IS NOT NULL AND id NOT IN (" +
                            "SELECT MIN(id) FROM eq_presets WHERE name_key IS NOT NULL " +
                            "GROUP BY name_key, band_count)",
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_eq_presets_name_key_band_count` " +
                            "ON `eq_presets` (`name_key`, `band_count`)",
                    )
                    db.execSQL(
                        "DELETE FROM ds_presets WHERE name_key IS NOT NULL AND id NOT IN (" +
                            "SELECT MIN(id) FROM ds_presets WHERE name_key IS NOT NULL " +
                            "GROUP BY name_key)",
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_ds_presets_name_key` " +
                            "ON `ds_presets` (`name_key`)",
                    )
                }
            }
    }
}
