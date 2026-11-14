package io.github.rasagyavatsal.homeoremedica.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.Context
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.github.rasagyavatsal.homeoremedica.BuildConfig
import io.github.rasagyavatsal.homeoremedica.answer.citedNumbers
import io.github.rasagyavatsal.homeoremedica.answer.label
import io.github.rasagyavatsal.homeoremedica.answer.parseAnswer
import io.github.rasagyavatsal.homeoremedica.answer.referencedNumbers
import io.github.rasagyavatsal.homeoremedica.api.Citation
import io.github.rasagyavatsal.homeoremedica.ui.theme.Fonts
import io.github.rasagyavatsal.homeoremedica.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Ports the web app's answer view (src/web/Answer.tsx) and its references (src/web/Thread.tsx).

/**
 * An answer's Markdown, with source labels such as `[2]` as chips, then [note]. Once it has stopped
 * streaming, its references follow; [onSource] opens one of them, and [activeSource] is the one open.
 */
@Composable
fun Answer(
    question: String,
    content: String,
    sources: List<Citation>,
    streaming: Boolean,
    activeSource: Int?,
    onSource: (Int) -> Unit,
    modifier: Modifier = Modifier,
    note: @Composable () -> Unit = {},
    /** Lays out the answer's buttons, such as beside its ratings; by default they sit at the end. */
    actions: (@Composable (buttons: @Composable RowScope.() -> Unit) -> Unit)? = null,
) {
    val blocks = remember(content, sources.size) { parseAnswer(content, sources.size) }
    Column(modifier) {
        SelectionContainer {
            MarkdownText(
                blocks,
                onCitation = onSource,
                citationLabel = { number ->
                    sources.getOrNull(number - 1)?.let { "Source $number: ${it.label}, ${it.bookTitle}" } ?: "Source $number"
                },
            )
        }
        note()
        // Listed once the answer is done, so the list does not shift while it streams.
        if (!streaming) {
            val compact = LocalCompact.current
            val references = @Composable {
                if (sources.isNotEmpty()) {
                    References(sources, remember(content, sources.size) { citedNumbers(content, sources.size) }, activeSource, onSource)
                }
            }
            val buttons: @Composable RowScope.() -> Unit = {
                if (compact) {
                    CopyButton(content)
                    ShareButton(content)
                    ReportButton(question, content)
                } else {
                    ReportButton(question, content)
                    CopyButton(content)
                }
            }
            val row = @Composable {
                if (actions != null) {
                    Box(Modifier.padding(top = if (compact) 4.dp else 12.dp)) { actions(buttons) }
                } else if (compact) {
                    // The first icon lines up with the answer's text.
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp).offset(x = -ActionInset), content = buttons)
                } else {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End, content = buttons)
                }
            }
            // Phones put the answer's actions straight under it, where a thumb expects them.
            if (compact) {
                row()
                references()
            } else {
                references()
                row()
            }
        }
    }
}

/** A phone's action button, and how far its icon sits in from its edge. */
private val ActionSize = 48.dp
private val ActionIcon = 22.dp
private val ActionInset = (ActionSize - ActionIcon) / 2

/** How far the conversation's items sit from the screen's sides, which a row that scrolls sideways reaches past. */
val LocalGutter = compositionLocalOf { 16.dp }

/**
 * One row under an answer that counts the passages it cites, or every passage it retrieved if it
 * cites none, as the web app's; it opens the first of them, and the sheet swipes between the rest.
 */
@Composable
private fun References(sources: List<Citation>, cited: Set<Int>, active: Int?, onOpen: (Int) -> Unit) {
    val colors = Theme.colors
    val listed = referencedNumbers(sources.size, cited)
    val first = listed.firstOrNull() ?: return
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.pressed()
    val highlighted = pressed || active in listed
    val count = if (listed.size == 1) "1 reference" else "${listed.size} references"
    Row(
        Modifier
            .padding(top = if (LocalCompact.current) 12.dp else 24.dp)
            .rise(sources, 320)
            .clip(Pill)
            .background(if (highlighted) colors.accentSoft else colors.surface)
            .border(1.dp, if (highlighted) colors.accentBorder else colors.border, Pill)
            .pressable(label = "Open references", interactionSource = interactions) { onOpen(first) }
            .semantics(mergeDescendants = true) { contentDescription = count }
            .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (number in listed.take(3)) {
                Box(
                    Modifier
                        .defaultMinSize(minWidth = 20.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (highlighted) colors.accentSoftHover else colors.accentSoft)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        number.toString(),
                        style = TextStyle(
                            color = colors.accentText,
                            fontFamily = Fonts.sans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                }
            }
        }
        BasicText(count, style = TextStyle(color = if (highlighted) colors.text else colors.textMuted, fontFamily = Fonts.sans, fontSize = 14.sp))
        Icon(Icons.Chevron, colors.textMuted, 16.dp)
    }
}

private const val REPORT_EMAIL = "rasagyavatsal@outlook.com"

/** Play requires a way to report AI answers: this drafts an email with the exchange, or shares it without an email app. */
private fun report(context: Context, question: String, answer: String) {
    val subject = "Report: HomeoRemedica answer"
    val body = "What is wrong with this answer?\n\n\n" +
        "---\nQuestion:\n$question\n\nAnswer:\n$answer\n\nApp version ${BuildConfig.VERSION_NAME}"
    val email = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, body)
    try {
        context.startActivity(email)
    } catch (_: ActivityNotFoundException) {
        val share = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, "To: $REPORT_EMAIL\n\n$body")
        context.startActivity(Intent.createChooser(share, "Report answer"))
    }
}

/** Offers the answer to another app through the system's share sheet. */
private fun share(context: Context, answer: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, answer)
    context.startActivity(Intent.createChooser(send, "Share answer"))
}

/** One of an answer's actions: an icon that darkens when pressed, large enough for a finger on phones. */
@Composable
private fun ActionButton(icon: ImageVector, label: String, description: String = label, onClick: () -> Unit) {
    val colors = Theme.colors
    val compact = LocalCompact.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.pressed()
    Box(
        Modifier
            .size(if (compact) ActionSize else 30.dp)
            .clip(if (compact) Pill else RoundedCornerShape(8.dp))
            .background(if (pressed) colors.surfaceHover else Color.Transparent)
            .pressable(label = label, interactionSource = interactions, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, if (pressed) colors.text else colors.textMuted, if (compact) ActionIcon else 15.dp, contentDescription = description)
    }
}

@Composable
private fun ReportButton(question: String, answer: String) {
    val context = LocalContext.current
    ActionButton(Icons.Flag, "Report answer") { report(context, question, answer) }
}

@Composable
private fun ShareButton(answer: String) {
    val context = LocalContext.current
    ActionButton(Icons.Share, "Share answer") { share(context, answer) }
}

@Composable
private fun CopyButton(text: String) {
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    ActionButton(if (copied) Icons.Check else Icons.Copy, "Copy answer", if (copied) "Copied" else "Copy answer") {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("HomeoRemedica answer", text)))
            copied = true
        }
    }
}
