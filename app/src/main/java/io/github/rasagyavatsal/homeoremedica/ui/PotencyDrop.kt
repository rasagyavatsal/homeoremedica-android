package io.github.rasagyavatsal.homeoremedica.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.rasagyavatsal.homeoremedica.ui.theme.Theme
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

// The potency drop (Icons.Drop) drawn in motion, in its 24-unit box.

private const val TOP = 2f
private const val BOTTOM = 22.75f
private val Centre = Offset(12f, 15.25f)
private const val RING_RADIUS = 4.25f
private const val RING_WIDTH = 1.1f
private const val DOT_RADIUS = 1.3f

private val DropPath: Path = PathParser()
    .parsePathString("M12 2C12 2 4.5 10.25 4.5 15.25A7.5 7.5 0 0 0 19.5 15.25C19.5 10.25 12 2 12 2Z")
    .toPath()

private const val FILL_MILLIS = 5200
private const val TAP_MILLIS = 3200
private const val TAPS = 4
private const val TAP_SECONDS = 0.22f

private fun clamp(t: Float) = t.coerceIn(0f, 1f)
private fun easeOut(t: Float) = 1 - (1 - clamp(t)).pow(3)
private fun easeInOut(t: Float): Float = clamp(t).let { if (it < 0.5f) 4 * it * it * it else 1 - (-2 * it + 2).pow(3) / 2 }

/** Whether the phone has animations turned off, when the drop holds still. */
@Composable
private fun animationsOff(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** Seconds into a loop of [millis], or null when animations are off. */
@Composable
private fun loopSeconds(millis: Int, label: String): (() -> Float)? {
    if (animationsOff()) return null
    val time by rememberInfiniteTransition(label).animateFloat(
        initialValue = 0f,
        targetValue = millis / 1000f,
        animationSpec = infiniteRepeatable(tween(millis, easing = LinearEasing), RepeatMode.Restart),
        label = label,
    )
    return { time }
}

/** Scales the 24-unit box to the canvas and cuts what is drawn with [BlendMode.Clear] out of the drop. */
private fun DrawScope.inDropBox(draw: DrawScope.() -> Unit) = scale(size.minDimension / 24f, pivot = Offset.Zero) { draw() }

private fun DrawScope.cutRing(color: Color, start: Float, sweep: Float) = drawArc(
    color,
    startAngle = start,
    sweepAngle = sweep,
    useCenter = false,
    topLeft = Offset(Centre.x - RING_RADIUS, Centre.y - RING_RADIUS),
    size = Size(RING_RADIUS * 2, RING_RADIUS * 2),
    style = Stroke(RING_WIDTH),
    blendMode = BlendMode.Clear,
)

/**
 * The drop filling up, decoration for an empty screen: an outline fills in four steps, like rising potencies, its
 * ring and centre are cut in, then it empties and starts again.
 */
@Composable
fun FillingDrop(size: Dp, modifier: Modifier = Modifier) {
    val color = Theme.colors.accent
    val time = loopSeconds(FILL_MILLIS, "filling drop")
    Canvas(modifier.size(size).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val t = time?.invoke()
        // Animations off: the whole logo.
        val level = when {
            t == null -> 1f
            t < 3.2f -> {
                val step = minOf(3, (t / 0.8f).toInt())
                (step + easeOut((t - step * 0.8f) / 0.6f)) / 4f
            }
            t < 4.6f -> 1f
            else -> 1 - easeInOut((t - 4.6f) / 0.6f)
        }
        val ring = when {
            t == null -> 1f
            t < 4.6f -> easeOut((t - 3.3f) / 0.7f)
            else -> 1 - easeInOut((t - 4.6f) / 0.6f)
        }
        // The surface bobs while the drop fills.
        val bob = if (t != null && level < 1f) 0.35f * sin(t * 6f) else 0f
        inDropBox {
            drawPath(DropPath, color, style = Stroke(0.6f))
            clipPath(DropPath) {
                val top = BOTTOM - (BOTTOM - TOP + 0.6f) * level + bob
                drawRect(color, topLeft = Offset(0f, top), size = Size(24f, 24f))
            }
            if (ring > 0f) {
                cutRing(color, -90f, 360f * ring)
                drawCircle(color, DOT_RADIUS * ring, Centre, blendMode = BlendMode.Clear)
            }
        }
    }
}

/** The drop struck four times, as a remedy is succussed, then settling: for waiting on an answer. */
@Composable
fun TappingDrop(size: Dp, modifier: Modifier = Modifier) {
    val color = Theme.colors.accent
    val time = loopSeconds(TAP_MILLIS, "tapping drop")
    Canvas(
        modifier.size(size).graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            val t = time?.invoke() ?: return@graphicsLayer
            // Rocks on the bottom of the drop.
            transformOrigin = TransformOrigin(0.5f, 0.95f)
            if (t < TAPS * TAP_SECONDS) {
                val p = (t % TAP_SECONDS) / TAP_SECONDS
                val lift = sin(PI.toFloat() * p)
                translationY = -0.09f * this.size.height * lift
                rotationZ = -6f * lift
                // Squashed as it lands and leaves.
                val landed = p < 0.12f || p > 0.88f
                scaleY = if (landed) 0.9f else 1f
                scaleX = if (landed) 1.08f else 1f
            } else {
                val s = t - TAPS * TAP_SECONDS
                scaleY = 1 - 0.06f * exp(-4 * s) * sin(16 * s)
                scaleX = 2 - scaleY
            }
        },
    ) {
        inDropBox {
            drawPath(DropPath, color)
            cutRing(color, 0f, 360f)
            drawCircle(color, DOT_RADIUS, Centre, blendMode = BlendMode.Clear)
        }
    }
}
