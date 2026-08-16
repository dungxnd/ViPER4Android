package com.dxnd.viper4android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "eq_presets",
    indices = [Index(value = ["name_key", "band_count"], unique = true)],
)
data class EqPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "name_key")
    val nameKey: String? = null,
    @ColumnInfo(name = "band_count")
    val bandCount: Int,
    val bands: String,
)
