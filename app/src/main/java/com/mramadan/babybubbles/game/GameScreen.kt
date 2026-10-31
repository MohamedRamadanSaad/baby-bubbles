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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

private val skyTop = Color(0xFFE0F2FE)
private val skyBottom = Color(0xFFEDE9FE)

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

        drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom)))

        for (b in engine.bubbles) {
            val progress = if (b.popping) (b.popT / POP_DURATION).coerceIn(0f, 1f) else 0f
            val r = b.radius * (1f + progress * 0.35f)
            val alpha = 1f - progress
            val base = BubbleColors[b.colorIndex]
            val center = Offset(b.x, b.y)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(lerp(base, Color.White, 0.45f), base),
                    center = Offset(b.x - r * 0.3f, b.y - r * 0.3f),
                    radius = r * 1.3f,
                ),
                radius = r,
                center = center,
                alpha = alpha,
            )
            drawCircle(
                color = Color.White,
                radius = r * 0.22f,
                center = Offset(b.x - r * 0.35f, b.y - r * 0.38f),
                alpha = 0.55f * alpha,
            )
        }

        for (p in engine.particles) {
            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(p.x, p.y),
                alpha = (1f - p.life / PARTICLE_LIFE).coerceIn(0f, 1f),
            )
        }
    }
}
