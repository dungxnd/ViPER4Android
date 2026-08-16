package com.dxnd.viper4android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ds_presets",
    indices = [Index(value = ["name_key"], unique = true)],
)
data class DsPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "name_key")
    val nameKey: String? = null,
    @ColumnInfo(name = "x_low") val xLow: Int,
    @ColumnInfo(name = "x_high") val xHigh: Int,
    @ColumnInfo(name = "y_low") val yLow: Int,
    @ColumnInfo(name = "y_high") val yHigh: Int,
    @ColumnInfo(name = "side_gain_low") val sideGainLow: Int,
    @ColumnInfo(name = "side_gain_high") val sideGainHigh: Int,
)
