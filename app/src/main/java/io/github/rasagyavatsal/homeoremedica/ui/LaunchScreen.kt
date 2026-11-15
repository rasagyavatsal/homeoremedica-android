package io.github.rasagyavatsal.homeoremedica.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import io.github.rasagyavatsal.homeoremedica.ui.theme.Ease
import io.github.rasagyavatsal.homeoremedica.ui.theme.Theme
import kotlinx.coroutines.delay

private const val HOLD_MILLIS = 250
private const val FADE_MILLIS = 350

/** Whether the launch screen still covers the app, so entrances underneath wait to be seen. */
val LocalLaunching = compositionLocalOf { false }

/**
 * The whole name written out left to right, as with a pen, over the app as it opens. It calls [onReveal] as it
 * starts to fade and show the app, and [onDone] once it is gone.
 */
@Composable
fun LaunchScreen(onReveal: () -> Unit, onDone: () -> Unit) {
    val pen = remember { Animatable(0f) }
    val shown = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        pen.animateTo(1f, tween(PEN_MILLIS, easing = PenEasing))
        delay(HOLD_MILLIS.toLong())
        onReveal()
        shown.animateTo(0f, tween(FADE_MILLIS, easing = Ease))
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = shown.value }
            .background(Theme.colors.canvas)
            // The app underneath is not touched through it.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
        contentAlignment = Alignment.Center,
    ) {
        Brand(fontSize = 44.sp, pen = { pen.value })
    }
}
