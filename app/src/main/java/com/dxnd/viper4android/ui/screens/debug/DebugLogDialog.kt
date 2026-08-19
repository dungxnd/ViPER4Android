package com.dxnd.viper4android.ui.screens.debug

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dxnd.viper4android.R
import com.dxnd.viper4android.ui.theme.log_level_debug
import com.dxnd.viper4android.ui.theme.log_level_error
import com.dxnd.viper4android.ui.theme.log_level_info
import com.dxnd.viper4android.ui.theme.log_level_unspecified
import com.dxnd.viper4android.ui.theme.log_level_warn
import com.dxnd.viper4android.ui.theme.log_source_app
import com.dxnd.viper4android.ui.theme.log_source_driver

private enum class SourceFilter { ALL, APP, DRIVER }

private enum class LevelFilter { ALL, INFO, DEBUG, WARN, ERROR }

@Composable
fun DebugLogDialog(
    onDisableDebug: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val state = remember { DebugLogState() }
    val listState = rememberLazyListState()
    var sourceFilter by remember { mutableStateOf(SourceFilter.ALL) }
    var levelFilter by remember { mutableStateOf(LevelFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    DisposableEffect(state) {
        state.start(includeFileHistory = true)
        onDispose { state.shutdown() }
    }

    val filteredEntries by remember(state) {
        derivedStateOf {
            val query = searchQuery
            state.visibleEntries.filter { entry ->
                matchesSource(entry, sourceFilter) &&
                    matchesLevel(entry, levelFilter) &&
                    (query.isEmpty() || entry.raw.contains(query, ignoreCase = true))
            }
        }
    }

    LaunchedEffect(filteredEntries.size) {
        if (filteredEntries.isNotEmpty()) {
            listState.animateScrollToItem(filteredEntries.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debug_log_title)) },
        text = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val listHeight = maxHeight * 0.6f
                Column(modifier = Modifier.fillMaxWidth()) {
                    SearchField(searchQuery) { searchQuery = it }
                    SourceFilterRow(sourceFilter) { sourceFilter = it }
                    LevelFilterRow(levelFilter) { levelFilter = it }

                    StreamErrorBanner(state.appStreamError, state.driverStreamError)

                    Text(
                        text = "${filteredEntries.size} / ${state.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    LazyColumn(
                        state = listState,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(listHeight),
                    ) {
                        items(filteredEntries) { entry ->
                            Text(
                                text = entry.raw,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = colorForEntry(entry),
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDisableDebug) {
                    Text(stringResource(R.string.debug_disable_debug))
                }
                TextButton(onClick = { state.clear() }) {
                    Text(stringResource(R.string.action_clear))
                }
                TextButton(
                    onClick = {
                        val uri = state.exportAsText(context)
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    },
                ) {
                    Text(stringResource(R.string.debug_export_txt))
                }
            }
        },
    )
}

@Composable
private fun SearchField(
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        placeholder = { Text(stringResource(R.string.debug_search_hint)) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
    )
}

@Composable
private fun SourceFilterRow(
    selected: SourceFilter,
    onSelect: (SourceFilter) -> Unit,
) {
    FilterRow {
        SourceFilter.entries.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(sourceLabel(option), style = MaterialTheme.typography.labelSmall) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorForSource(option).copy(alpha = 0.2f),
                        selectedLabelColor = colorForSource(option),
                    ),
                modifier = Modifier.height(28.dp),
            )
        }
    }
}

@Composable
private fun LevelFilterRow(
    selected: LevelFilter,
    onSelect: (LevelFilter) -> Unit,
) {
    FilterRow {
        LevelFilter.entries.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(levelLabel(option), style = MaterialTheme.typography.labelSmall) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorForLevel(option).copy(alpha = 0.2f),
                        selectedLabelColor = colorForLevel(option),
                    ),
                modifier = Modifier.height(28.dp),
            )
        }
    }
}

@Composable
private fun FilterRow(content: @Composable () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun StreamErrorBanner(
    appError: String?,
    driverError: String?,
) {
    if (appError == null && driverError == null) return
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        appError?.let {
            Text(
                text = "App log: $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        driverError?.let {
            Text(
                text = "Driver log: $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun sourceLabel(option: SourceFilter): String =
    when (option) {
        SourceFilter.ALL -> stringResource(R.string.debug_filter_all)
        SourceFilter.APP -> stringResource(R.string.debug_filter_app)
        SourceFilter.DRIVER -> stringResource(R.string.debug_filter_driver)
    }

@Composable
private fun levelLabel(option: LevelFilter): String =
    when (option) {
        LevelFilter.ALL -> stringResource(R.string.debug_filter_all)
        LevelFilter.INFO -> stringResource(R.string.debug_filter_info)
        LevelFilter.DEBUG -> stringResource(R.string.debug_filter_debug)
        LevelFilter.WARN -> stringResource(R.string.debug_filter_warn)
        LevelFilter.ERROR -> stringResource(R.string.debug_filter_error)
    }

private fun matchesSource(
    entry: LogEntry,
    filter: SourceFilter,
): Boolean =
    when (filter) {
        SourceFilter.ALL -> true
        SourceFilter.APP -> entry.source == LogSource.APP
        SourceFilter.DRIVER -> entry.source == LogSource.DRIVER
    }

private fun matchesLevel(
    entry: LogEntry,
    filter: LevelFilter,
): Boolean =
    when (filter) {
        LevelFilter.ALL -> true
        LevelFilter.INFO -> entry.level == LogLevel.INFO
        LevelFilter.DEBUG -> entry.level == LogLevel.DEBUG
        LevelFilter.WARN -> entry.level == LogLevel.WARN
        LevelFilter.ERROR -> entry.level == LogLevel.ERROR
    }

private fun colorForSource(source: SourceFilter): Color =
    when (source) {
        SourceFilter.ALL -> log_level_unspecified
        SourceFilter.APP -> log_source_app
        SourceFilter.DRIVER -> log_source_driver
    }

private fun colorForLevel(level: LevelFilter): Color =
    when (level) {
        LevelFilter.ALL -> log_level_unspecified
        LevelFilter.INFO -> log_level_info
        LevelFilter.DEBUG -> log_level_debug
        LevelFilter.WARN -> log_level_warn
        LevelFilter.ERROR -> log_level_error
    }

private fun colorForEntry(entry: LogEntry): Color =
    when (entry.level) {
        LogLevel.ERROR -> log_level_error
        LogLevel.WARN -> log_level_warn
        LogLevel.INFO -> log_level_info
        LogLevel.DEBUG -> log_level_debug
        LogLevel.UNKNOWN -> log_level_unspecified
    }
