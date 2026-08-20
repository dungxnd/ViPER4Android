package com.dxnd.viper4android.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A Box that overlays scroll-hint arrows at the top and bottom edges whenever
 * the content is scrollable in that direction.
 *
 * Use [ScrollHintBox] with explicit [canScrollUp] / [canScrollDown] booleans when
 * you already have the scroll state outside (e.g. from [LazyListState]).
 *
 * See the overloads below for convenience wrappers that accept a [ScrollState] or
 * [LazyListState] directly.
 */
@Composable
fun ScrollHintBox(
    canScrollUp: Boolean,
    canScrollDown: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier = modifier) {
        content()
        ScrollArrowHint(
            visible = canScrollUp,
            alignment = Alignment.TopCenter,
            icon = Icons.Default.KeyboardArrowUp,
            color = surfaceColor.copy(alpha = 0.92f),
        )
        ScrollArrowHint(
            visible = canScrollDown,
            alignment = Alignment.BottomCenter,
            icon = Icons.Default.KeyboardArrowDown,
            color = surfaceColor.copy(alpha = 0.92f),
        )
    }
}

/**
 * Overload for [androidx.compose.foundation.ScrollState]-backed scrollable areas.
 */
@Composable
fun ScrollHintBox(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val canScrollUp by remember { derivedStateOf { scrollState.value > 0 } }
    val canScrollDown by remember { derivedStateOf { scrollState.canScrollForward } }
    ScrollHintBox(
        canScrollUp = canScrollUp,
        canScrollDown = canScrollDown,
        modifier = modifier,
        content = content,
    )
}

/**
 * Overload for [LazyListState]-backed lazy lists.
 */
@Composable
fun ScrollHintBox(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val canScrollUp by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollDown by remember { derivedStateOf { listState.canScrollForward } }
    ScrollHintBox(
        canScrollUp = canScrollUp,
        canScrollDown = canScrollDown,
        modifier = modifier,
        content = content,
    )
}
