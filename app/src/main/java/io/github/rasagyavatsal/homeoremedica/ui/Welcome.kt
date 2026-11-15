package io.github.rasagyavatsal.homeoremedica.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.rasagyavatsal.homeoremedica.ui.theme.Fonts
import io.github.rasagyavatsal.homeoremedica.ui.theme.Theme

// Ports the web app's empty chat (src/web/Welcome.tsx).

// The web app's `--duration-reveal`.
const val REVEAL_MILLIS = 480

/** When the greeting is half written, so the composer can rise in under it. */
const val TAGLINE_MILLIS = PEN_MILLIS / 2

/** The empty chat: a greeting for the time of day, picked once as the chat opens. On phones it sits by the message box. */
@Composable
fun Welcome(bottomPadding: Dp) {
    val line = remember { greeting() }
    val compact = LocalCompact.current
    Box(
        Modifier.fillMaxSize().padding(start = if (compact) 20.dp else 16.dp, end = if (compact) 20.dp else 16.dp, bottom = bottomPadding),
        contentAlignment = if (compact) Alignment.BottomStart else Alignment.Center,
    ) {
        if (compact) {
            Tagline(line, TextAlign.Start, Modifier.padding(bottom = 32.dp))
        } else {
            Tagline(line, TextAlign.Center, Modifier.padding(vertical = 64.dp))
        }
    }
}

/**
 * The greeting in the name's serif, written out as the name is once the launch screen lifts: one line after
 * another, as a pen would.
 */
@Composable
private fun Tagline(text: String, align: TextAlign, modifier: Modifier) {
    val launching = LocalLaunching.current
    val pen = remember { Animatable(0f) }
    LaunchedEffect(launching) { if (!launching) pen.animateTo(1f, tween(PEN_MILLIS, easing = PenEasing)) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text,
        style = TextStyle(
            color = Theme.colors.text,
            fontFamily = Fonts.brand,
            fontSize = 40.sp,
            lineHeight = 1.15.em,
            textAlign = align,
        ),
        onTextLayout = { layout = it },
        modifier = modifier.semantics { heading() }.drawWithContent {
            val lines = layout ?: return@drawWithContent
            // The pen's progress runs along the lines laid end to end.
            var ink = pen.value * (0 until lines.lineCount).sumOf { (lines.getLineRight(it) - lines.getLineLeft(it)).toDouble() }.toFloat()
            for (index in 0 until lines.lineCount) {
                if (ink <= 0f) break
                val left = lines.getLineLeft(index)
                val width = lines.getLineRight(index) - left
                // Serif tails can reach past the outer lines' boxes, so those keep a little room; inner edges stay
                // exact so one line's pen never shows the next.
                val slack = (lines.getLineBottom(index) - lines.getLineTop(index)) * 0.2f
                val top = lines.getLineTop(index) - if (index == 0) slack else 0f
                val bottom = lines.getLineBottom(index) + if (index == lines.lineCount - 1) slack else 0f
                clipRect(left, top, left + minOf(ink, width), bottom) {
                    this@drawWithContent.drawContent()
                }
                ink -= width
            }
        },
    )
}
