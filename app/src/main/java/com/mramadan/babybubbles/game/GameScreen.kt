package com.mramadan.babybubbles.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import com.mramadan.babybubbles.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val ColorsTop = Color(0xFFEFE9FF)
private val ColorsBottom = Color(0xFFE3F3FF)
private val SkyTop = Color(0xFFBFE6FF)
private val SkyMid = Color(0xFFE4F5FF)
private val SkyBottom = Color(0xFFF2FAFF)
private val Sun = Color(0xFFFFD95A)
private val NoteColor = Color(0xFF6A3DE8)

/** (x fraction, y fraction, radius dp) of the twinkling dots in the colors backdrop. */
private val Twinkles = listOf(
    Triple(0.15f, 0.31f, 5f), Triple(0.85f, 0.50f, 7f), Triple(0.51f, 0.72f, 4f),
    Triple(0.30f, 0.12f, 4f), Triple(0.72f, 0.90f, 6f), Triple(0.92f, 0.20f, 5f),
)

class SceneArt(
    val cloud: Painter,
    val hills: Painter,
    val duck: Painter,
    val snail: Painter,
    val butterfly: Painter,
)

@Composable
fun GameScreen(engine: GameEngine, modifier: Modifier = Modifier) {
    var frame by remember { mutableLongStateOf(0L) }
    val density = LocalDensity.current.density

    // Game loop: one update per display frame, dt capped so a stall never teleports bubbles.
    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                    engine.update(dt)
                }
                last = now
                frame = now
            }
        }
    }

    val animalPainters = AnimalCatalog.map { rememberVectorPainter(ImageVector.vectorResource(it.drawable)) }
    val scene = SceneArt(
        cloud = rememberVectorPainter(ImageVector.vectorResource(R.drawable.scene_cloud)),
        hills = rememberVectorPainter(ImageVector.vectorResource(R.drawable.scene_hills)),
        duck = rememberVectorPainter(ImageVector.vectorResource(R.drawable.walker_duck)),
        snail = rememberVectorPainter(ImageVector.vectorResource(R.drawable.walker_snail)),
        butterfly = rememberVectorPainter(ImageVector.vectorResource(R.drawable.walker_butterfly)),
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(engine) {
                // Every new finger (or a whole palm) pops what is under it.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            if (change.changedToDown()) {
                                engine.tap(change.position)
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        @Suppress("UNUSED_VARIABLE")
        val tick = frame // reading the frame state redraws the canvas every frame
        engine.resize(size.width, size.height, density)

        when (engine.mode) {
            GameMode.COLORS -> drawColorsBackdrop(engine)
            GameMode.ANIMALS -> drawAnimalScene(engine, scene)
        }

        for (b in engine.bubbles) {
            if (b.animal >= 0) drawAnimalBubble(b, animalPainters[b.animal], density)
            else drawColorBubbleState(b)
        }

        drawEffects(engine)
    }
}

private fun DrawScope.drawColorsBackdrop(e: GameEngine) {
    drawRect(Brush.verticalGradient(listOf(ColorsTop, ColorsBottom)))
    Twinkles.forEachIndexed { i, (fx, fy, r) ->
        drawCircle(
            color = Color.White,
            radius = r * e.density,
            center = Offset(size.width * fx, size.height * fy),
            alpha = 0.55f + 0.3f * sin(e.time * 1.5f + i),
        )
    }
}

private fun DrawScope.drawAnimalScene(e: GameEngine, art: SceneArt) {
    drawRect(Brush.verticalGradient(0f to SkyTop, 0.7f to SkyMid, 1f to SkyBottom))

    val d = e.density
    val sunCenter = Offset(66f * d, 78f * d)
    val pulse = 1f + 0.04f * sin(e.time * 1.2f)
    drawCircle(Sun, radius = 72f * d * pulse, center = sunCenter, alpha = 0.14f)
    drawCircle(Sun, radius = 56f * d * pulse, center = sunCenter, alpha = 0.28f)
    drawCircle(Sun, radius = 42f * d, center = sunCenter)

    for (c in e.clouds) {
        translate(c.x, c.y) {
            with(art.cloud) { draw(Size(120f * d * c.scale, 63f * d * c.scale), alpha = 0.95f) }
        }
    }

    val hh = e.hillsHeight
    translate(0f, size.height - hh) {
        with(art.hills) { draw(Size(size.width, hh)) }
    }

    for (w in e.walkers) {
        val rect = e.walkerRect(w)
        val painter = when (w.kind) {
            WalkerKind.DUCK -> art.duck
            WalkerKind.SNAIL -> art.snail
            WalkerKind.BUTTERFLY -> art.butterfly
        }
        val flip = if (w.dir < 0f) -1f else 1f
        val flap = if (w.kind == WalkerKind.BUTTERFLY) 0.55f + 0.45f * abs(cos(w.t * 11f)) else 1f
        scale(scaleX = flip * flap, scaleY = 1f, pivot = rect.center) {
            translate(rect.left, rect.top) {
                with(painter) { draw(rect.size) }
            }
        }
    }
}

private fun DrawScope.drawColorBubbleState(b: Bubble) {
    val p = if (b.popping) (b.popT / COLOR_POP).coerceIn(0f, 1f) else 0f
    drawColorBubble(
        center = Offset(b.x, b.y),
        r = b.radius * (1f + p * 0.35f),
        base = BubbleColors[b.colorIndex],
        alpha = 1f - p,
        shadow = !b.popping,
    )
}

private fun DrawScope.drawAnimalBubble(b: Bubble, painter: Painter, density: Float) {
    val c = Offset(b.x, b.y)
    val r = b.radius
    if (!b.popping) {
        drawClearBubble(c, r, strokeWidth = 3f * density)
        rotate(sin(b.age * 2f + b.wobblePhase) * 6f, pivot = c) {
            drawPainterCentered(painter, c, r * 1.5f)
        }
        drawBubbleGloss(c, r)
        return
    }

    // Popped: the shell bursts and the animal jumps out, then fades.
    val p = (b.popT / ANIMAL_POP).coerceIn(0f, 1f)
    if (p < 0.22f) {
        val q = p / 0.22f
        drawClearBubble(c, r * (1f + q * 0.4f), strokeWidth = 3f * density, alpha = 1f - q, shadow = false)
    }
    val grow = (p / 0.35f).coerceAtMost(1f)
    val ease = 1f - (1f - grow) * (1f - grow)
    val side = r * 1.5f * (1f + 0.45f * ease)
    val hop = -sin(PI.toFloat() * (p / 0.6f).coerceAtMost(1f)) * 34f * density
    val alpha = if (p < 0.6f) 1f else 1f - (p - 0.6f) / 0.4f
    drawPainterCentered(painter, Offset(c.x, c.y + hop), side, alpha)
}

private fun DrawScope.drawEffects(e: GameEngine) {
    for (ring in e.rings) {
        val q = (ring.life / RING_LIFE).coerceIn(0f, 1f)
        drawCircle(
            color = ring.color,
            radius = ring.radius * (1f + 0.6f * q),
            center = Offset(ring.x, ring.y),
            alpha = (1f - q) * 0.8f,
            style = Stroke(width = 5f * e.density * (1f - q) + 1f),
        )
    }
    for (p in e.particles) {
        drawCircle(
            color = p.color,
            radius = p.size,
            center = Offset(p.x, p.y),
            alpha = (1f - p.life / PARTICLE_LIFE).coerceIn(0f, 1f),
        )
    }
    for (n in e.notes) {
        val q = (n.life / NOTE_LIFE).coerceIn(0f, 1f)
        drawMusicNote(Offset(n.x, n.y), 34f * e.density, NoteColor, alpha = 1f - q)
    }
}
