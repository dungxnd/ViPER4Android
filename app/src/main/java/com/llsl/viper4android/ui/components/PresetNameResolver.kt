package com.dxnd.viper4android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dxnd.viper4android.data.model.DsPreset
import com.dxnd.viper4android.data.model.EqPreset
import com.dxnd.viper4android.viper.ViperDispatcher

@Composable
fun resolvePresetName(preset: EqPreset): String {
    val resId = preset.nameKey?.let { ViperDispatcher.EQ_PRESET_NAME_RES[it] }
    return if (resId != null) stringResource(resId) else preset.name
}

@Composable
fun resolvePresetName(preset: DsPreset): String {
    val resId = preset.nameKey?.let { ViperDispatcher.DS_PRESET_NAME_RES[it] }
    return if (resId != null) stringResource(resId) else preset.name
}
