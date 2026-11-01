package com.mramadan.babybubbles.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.mramadan.babybubbles.R
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun GameScreen(engine: GameEngine, characters: List<Character>, modifier: Modifier = Modifier) {
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

    val painters = key(characters) { characters.map { painterResource(it.image) } }
    val scene = SceneArt(
        cloud = painterResource(R.drawable.scene_cloud),
        hills = painterResource(R.drawable.scene_hills),
        duck = painterResource(R.drawable.walker_duck),
        snail = painterResource(R.drawable.walker_snail),
        butterfly = painterResource(R.drawable.walker_butterfly),
        car = painterResource(R.drawable.veh_car),
        bird = painterResource(R.drawable.bird_sparrow),
        fish = painterResource(R.drawable.sea_clownfish),
        fish2 = painterResource(R.drawable.sea_bluetang),
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

        if (engine.mode == GameMode.SEA) drawSeaScene(engine, scene) else drawLandScene(engine, scene)

        for (b in engine.bubbles) {
            if (b.character >= 0 && b.character < painters.size) {
                drawCharacterBubble(engine, b, painters[b.character], density)
            } else {
                drawColorBubbleState(b)
            }
        }

        drawEffects(engine)
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

/** Draws a flag picture in a rounded 4:3 frame with a white border. */
private fun DrawScope.drawFlag(painter: Painter, center: Offset, width: Float, alpha: Float, density: Float) {
    val h = width * 0.75f
    val rect = Rect(center.x - width / 2f, center.y - h / 2f, center.x + width / 2f, center.y + h / 2f)
    val corner = CornerRadius(width * 0.08f)
    val clip = Path().apply { addRoundRect(RoundRect(rect, corner)) }
    clipPath(clip) {
        translate(rect.left, rect.top) { with(painter) { draw(Size(width, h), alpha = alpha) } }
    }
    drawRoundRect(
        color = Color.White,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = corner,
        style = Stroke(2.5f * density),
        alpha = alpha,
    )
}

private fun DrawScope.drawCharacter(
    e: GameEngine,
    painter: Painter,
    center: Offset,
    side: Float,
    alpha: Float,
    density: Float,
) {
    if (e.mode.isFlag) drawFlag(painter, center, side * 1.02f, alpha, density)
    else drawPainterCentered(painter, center, side, alpha)
}

private fun DrawScope.drawCharacterBubble(e: GameEngine, b: Bubble, painter: Painter, density: Float) {
    val c = Offset(b.x, b.y)
    val r = b.radius
    val side = r * 1.5f
    if (!b.popping) {
        drawClearBubble(c, r, strokeWidth = 3f * density)
        rotate(sin(b.age * 2f + b.wobblePhase) * (if (e.mode.isFlag) 3f else 6f), pivot = c) {
            drawCharacter(e, painter, c, side, 1f, density)
        }
        drawBubbleGloss(c, r)
        return
    }

    val dur = popDuration(e.style)
    val p = (b.popT / dur).coerceIn(0f, 1f)
    val shellEnd = 0.22f * (0.8f / dur)
    if (p < shellEnd) {
        val q = p / shellEnd
        drawClearBubble(c, r * (1f + q * 0.4f), strokeWidth = 3f * density, alpha = 1f - q, shadow = false)
    }

    when (e.style) {
        PopStyle.JUMP -> {
            // The character jumps out, grows a little, then fades.
            val grow = (p / 0.35f).coerceAtMost(1f)
            val ease = 1f - (1f - grow) * (1f - grow)
            val hop = -sin(PI.toFloat() * (p / 0.6f).coerceAtMost(1f)) * 34f * density
            val alpha = if (p < 0.6f) 1f else 1f - (p - 0.6f) / 0.4f
            drawCharacter(e, painter, Offset(c.x, c.y + hop), side * (1f + 0.45f * ease), alpha, density)
        }
        PopStyle.DRIVE -> {
            // Vehicles bounce once and drive off to the right.
            val dx = p * p * size.width * 0.9f
            val bounce = -sin(PI.toFloat() * (p / 0.3f).coerceAtMost(1f)) * 18f * density
            val alpha = if (p < 0.7f) 1f else 1f - (p - 0.7f) / 0.3f
            drawCharacter(e, painter, Offset(c.x + dx, c.y + bounce), side * 1.15f, alpha, density)
        }
        PopStyle.RISE -> {
            // Flags are never burst: the bubble pops and the flag rises gently away.
            val dy = -p * 150f * density
            val alpha = if (p < 0.5f) 1f else 1f - (p - 0.5f) / 0.5f
            rotate(sin(p * 8f) * 4f, pivot = Offset(c.x, c.y + dy)) {
                drawCharacter(e, painter, Offset(c.x, c.y + dy), side * (1f + 0.15f * p), alpha, density)
            }
        }
    }
}

/** A four-point twinkle star. */
private fun DrawScope.drawSparkle(center: Offset, r: Float, color: Color, alpha: Float) {
    val star = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticTo(center.x, center.y, center.x + r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + r)
        quadraticTo(center.x, center.y, center.x - r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - r)
        close()
    }
    drawPath(star, color, alpha = alpha)
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
    for (s in e.sparkles) {
        val q = (s.life / SPARKLE_LIFE).coerceIn(0f, 1f)
        val r = 16f * e.density * (0.6f + 0.6f * sin(PI.toFloat() * q))
        drawSparkle(Offset(s.x, s.y), r, s.color, alpha = 1f - q)
    }
}
