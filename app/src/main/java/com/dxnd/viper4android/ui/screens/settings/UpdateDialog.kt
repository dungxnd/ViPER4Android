package com.dxnd.viper4android.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dxnd.viper4android.R
import com.dxnd.viper4android.ui.theme.md_alert_caution
import com.dxnd.viper4android.ui.theme.md_alert_important
import com.dxnd.viper4android.ui.theme.md_alert_note
import com.dxnd.viper4android.ui.theme.md_alert_tip
import com.dxnd.viper4android.ui.theme.md_alert_warning
import com.dxnd.viper4android.utils.ReleaseInfo

@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    newerReleases: List<ReleaseInfo>,
    currentVersion: String,
    upToDate: Boolean,
    downloading: Boolean,
    downloadProgress: Int,
    onDownloadInstall: () -> Unit,
    onViewOnGithub: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val bodyColor = MaterialTheme.colorScheme.onSurface
    val headingColor = MaterialTheme.colorScheme.primary
    val linkColor = MaterialTheme.colorScheme.tertiary
    val codeColor = bodyColor.copy(alpha = 0.85f)
    val isDebug = com.dxnd.viper4android.BuildConfig.DEBUG
    val variantStr = stringResource(if (isDebug) R.string.build_variant_debug else R.string.build_variant_release)

    // Build (version, blocks) pairs for the changelog scroll area.
    // When an update is available: show all newer releases, newest-first.
    // When up-to-date: show just the latest release.
    val changelogSections: List<Pair<String, List<MdBlock>>> = remember(newerReleases, release) {
        if (newerReleases.isNotEmpty()) {
            newerReleases.map { r -> r.name.ifBlank { r.tagName } to parseBlocks(r.body) }
        } else {
            listOf(release.name.ifBlank { release.tagName } to parseBlocks(release.body))
        }
    }

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = {
            Text(stringResource(if (upToDate) R.string.update_up_to_date_title else R.string.update_available_title))
        },
        text = {
            Column {
                // Version header
                if (upToDate) {
                    Text(
                        text = stringResource(R.string.update_latest_version, release.name),
                        style = MaterialTheme.typography.titleSmall,
                        color = headingColor,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.update_new_version, release.name),
                        style = MaterialTheme.typography.titleSmall,
                        color = headingColor,
                    )
                }
                Text(
                    text = stringResource(R.string.update_current_version, currentVersion, variantStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                // Changelog scroll area
                if (changelogSections.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        changelogSections.forEachIndexed { idx, (version, blocks) ->
                            if (changelogSections.size > 1) {
                                if (idx > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                                Text(
                                    text = version,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = headingColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            blocks.forEach { block ->
                                BlockView(
                                    block = block,
                                    bodyColor = bodyColor,
                                    headingColor = headingColor,
                                    codeColor = codeColor,
                                    linkColor = linkColor,
                                    onUrlClick = { url ->
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                if (downloading) {
                    Text(
                        text = stringResource(R.string.update_downloading, downloadProgress),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (!upToDate) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDownloadInstall, enabled = !downloading) {
                        Text(stringResource(R.string.update_download_install, variantStr))
                    }
                }
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onViewOnGithub, enabled = !downloading) {
                    Text(stringResource(R.string.update_view_on_github))
                }
                TextButton(onClick = onDismiss, enabled = !downloading) {
                    Text(stringResource(R.string.action_close))
                }
            }
        },
    )
}

// ── Markdown block model ────────────────────────────────────────────────────

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val marker: String, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Quote(val alert: String?, val children: List<MdBlock>) : MdBlock
}

// ── Block renderer ──────────────────────────────────────────────────────────

@Composable
private fun BlockView(
    block: MdBlock,
    bodyColor: Color,
    headingColor: Color,
    codeColor: Color,
    linkColor: Color,
    onUrlClick: (String) -> Unit,
) {
    when (block) {
        is MdBlock.Heading -> {
            MdText(
                text = block.text,
                bodyColor = bodyColor,
                codeColor = codeColor,
                linkColor = linkColor,
                color = headingColor,
                fontWeight = FontWeight.Bold,
                fontSize = headingSize(block.level),
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                onUrlClick = onUrlClick,
            )
        }

        is MdBlock.Bullet -> {
            Row(modifier = Modifier.padding(start = 4.dp, top = 1.dp)) {
                Text(text = block.marker, color = bodyColor, fontSize = 14.sp)
                MdText(
                    text = block.text,
                    bodyColor = bodyColor,
                    codeColor = codeColor,
                    linkColor = linkColor,
                    color = bodyColor,
                    fontSize = 14.sp,
                    onUrlClick = onUrlClick,
                )
            }
        }

        is MdBlock.Paragraph -> {
            if (block.text.isNotBlank()) {
                MdText(
                    text = block.text,
                    bodyColor = bodyColor,
                    codeColor = codeColor,
                    linkColor = linkColor,
                    color = bodyColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp),
                    onUrlClick = onUrlClick,
                )
            }
        }

        is MdBlock.Quote -> {
            val accent = alertColor(block.alert, headingColor)
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
            ) {
                block.alert?.let {
                    Text(text = it, color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                block.children.forEach { child ->
                    BlockView(child, bodyColor, headingColor, codeColor, linkColor, onUrlClick)
                }
            }
        }
    }
}

// ── Inline text with clickable links ───────────────────────────────────────

private val TASK_MARKER = Regex("""^\[([ xX])] +""")
private val ALERT_MARKER = Regex("""^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)]\s*$""", RegexOption.IGNORE_CASE)
private val INLINE_TOKEN = Regex("""\*\*(.+?)\*\*|`([^`]+?)`|\[([^]]+?)]\(([^)]+?)\)|<(https?://[^>]+)>|(https?://\S+)""")

private fun headingSize(level: Int) =
    when (level) {
        1 -> 20.sp
        2 -> 17.sp
        3 -> 15.sp
        else -> 14.sp
    }

private fun alertColor(alert: String?, default: Color): Color =
    when (alert?.uppercase()) {
        "NOTE" -> md_alert_note
        "TIP" -> md_alert_tip
        "IMPORTANT" -> md_alert_important
        "WARNING" -> md_alert_warning
        "CAUTION" -> md_alert_caution
        else -> default
    }

// ── Inline text composable ─────────────────────────────────────────────────

/** Inline markdown spans. Supports **bold**, `code`, [text](url), <url>, bare https://. */
@Composable
private fun MdText(
    text: String,
    bodyColor: Color,
    codeColor: Color,
    linkColor: Color,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    onUrlClick: (String) -> Unit,
) {
    // Collect (start, end, url) triples for link spans.
    val linkRanges = mutableListOf<Triple<Int, Int, String>>()
    val annotated = buildInlineAnnotated(text, bodyColor, codeColor, linkColor, linkRanges)

    if (linkRanges.isEmpty()) {
        Text(
            text = annotated,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            modifier = modifier,
        )
    } else if (linkRanges.size == 1) {
        Text(
            text = annotated,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            modifier = modifier.clickable { onUrlClick(linkRanges.first().third) },
        )
    } else {
        // Multiple links: render each segment separately.
        Column(modifier = modifier) {
            renderSegments(text, bodyColor, codeColor, linkColor, color, fontSize, fontWeight, onUrlClick)
        }
    }
}

/** Renders text split into clickable / non-clickable segments. */
@Composable
private fun renderSegments(
    text: String,
    bodyColor: Color,
    codeColor: Color,
    linkColor: Color,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight?,
    onUrlClick: (String) -> Unit,
) {
    Row {
        var cursor = 0
        for (match in INLINE_TOKEN.findAll(text)) {
            if (match.range.first > cursor) {
                Text(
                    text = text.substring(cursor, match.range.first),
                    color = color,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                )
            }
            val bold = match.groupValues[1]
            val code = match.groupValues[2]
            val linkText = match.groupValues[3]
            val linkUrl = match.groupValues[4]
            val angleUrl = match.groupValues[5]
            val bareUrl = match.groupValues[6]
            when {
                bold.isNotEmpty() -> Text(text = bold, color = bodyColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
                code.isNotEmpty() -> Text(text = code, color = codeColor, fontSize = fontSize, fontFamily = FontFamily.Monospace)
                linkText.isNotEmpty() -> Text(
                    text = linkText,
                    color = linkColor,
                    fontSize = fontSize,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onUrlClick(linkUrl) },
                )
                angleUrl.isNotEmpty() -> Text(
                    text = angleUrl,
                    color = linkColor,
                    fontSize = fontSize,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onUrlClick(angleUrl) },
                )
                bareUrl.isNotEmpty() -> Text(
                    text = bareUrl,
                    color = linkColor,
                    fontSize = fontSize,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onUrlClick(bareUrl) },
                )
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            Text(text = text.substring(cursor), color = color, fontSize = fontSize, fontWeight = fontWeight)
        }
    }
}

// ── AnnotatedString builder (used when no link or single link) ─────────────

private fun buildInlineAnnotated(
    text: String,
    bodyColor: Color,
    codeColor: Color,
    linkColor: Color,
    linkRanges: MutableList<Triple<Int, Int, String>>,
): AnnotatedString =
    buildAnnotatedString {
        var cursor = 0
        for (match in INLINE_TOKEN.findAll(text)) {
            if (match.range.first > cursor) {
                withStyle(SpanStyle(color = bodyColor)) {
                    append(text.substring(cursor, match.range.first))
                }
            }
            val bold = match.groupValues[1]
            val code = match.groupValues[2]
            val linkText = match.groupValues[3]
            val linkUrl = match.groupValues[4]
            val angleUrl = match.groupValues[5]
            val bareUrl = match.groupValues[6]
            when {
                bold.isNotEmpty() ->
                    withStyle(SpanStyle(color = bodyColor, fontWeight = FontWeight.Bold)) { append(bold) }
                code.isNotEmpty() ->
                    withStyle(SpanStyle(color = codeColor, fontFamily = FontFamily.Monospace)) { append(code) }
                linkText.isNotEmpty() -> {
                    val start = length
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(linkText) }
                    linkRanges.add(Triple(start, length, linkUrl))
                }
                angleUrl.isNotEmpty() -> {
                    val start = length
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(angleUrl) }
                    linkRanges.add(Triple(start, length, angleUrl))
                }
                bareUrl.isNotEmpty() -> {
                    val start = length
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(bareUrl) }
                    linkRanges.add(Triple(start, length, bareUrl))
                }
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            withStyle(SpanStyle(color = bodyColor)) { append(text.substring(cursor)) }
        }
    }

// ── Markdown parser ────────────────────────────────────────────────────────

private fun parseBlocks(source: String): List<MdBlock> {
    val lines = source.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trimStart())
                i++
            }
            var alert: String? = null
            val contentLines = quoteLines.toMutableList()
            ALERT_MARKER.find(contentLines.firstOrNull() ?: "")?.let {
                alert = it.groupValues[1].uppercase()
                contentLines.removeAt(0)
            }
            blocks.add(MdBlock.Quote(alert, parseBlocks(contentLines.joinToString("\n"))))
        } else {
            parseLine(trimmed)?.let { blocks.add(it) }
            i++
        }
    }
    return blocks
}

private fun parseLine(trimmed: String): MdBlock? =
    when {
        trimmed.isBlank() -> null

        trimmed.startsWith("#") -> {
            val level = trimmed.takeWhile { it == '#' }.length
            MdBlock.Heading(level, trimmed.drop(level).trim())
        }

        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
            val rest = trimmed.drop(2)
            val task = TASK_MARKER.find(rest)
            if (task != null) {
                val glyph = if (task.groupValues[1] != " ") "\u2611 " else "\u2610 "
                MdBlock.Bullet(glyph, rest.substring(task.value.length))
            } else {
                MdBlock.Bullet("\u2022  ", rest)
            }
        }

        else -> MdBlock.Paragraph(trimmed)
    }
