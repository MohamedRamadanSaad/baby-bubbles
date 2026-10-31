package com.mramadan.babybubbles.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bright, high-contrast colors. Index i also selects musical note i. */
val BubbleColors = listOf(
    Color(0xFFFF4D4D), Color(0xFFFF9F1C), Color(0xFFFFD93D), Color(0xFF6BCB77),
    Color(0xFF2EC4B6), Color(0xFF4D96FF), Color(0xFF9B5DE5), Color(0xFFF15BB5),
    Color(0xFFA3E635), Color(0xFF38BDF8),
)

const val POP_DURATION = 0.2f
const val PARTICLE_LIFE = 0.6f

class Bubble(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val colorIndex: Int,
    val wobblePhase: Float,
) {
    var age = 0f
    /** Negative while alive; seconds since the pop once popped. */
    var popT = -1f
    val popping: Boolean get() = popT >= 0f
}

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
) {
    var life = 0f
}

/**
 * Pure game state: spawning, movement, hit testing and pop effects.
 * Units are pixels; [density] converts the dp-based tuning values.
 */
class GameEngine(private val onPop: (colorIndex: Int) -> Unit) {
    val bubbles = mutableListOf<Bubble>()
    val particles = mutableListOf<Particle>()

    var width = 0f
        private set
    var height = 0f
        private set
    private var density = 1f
    private var spawnTimer = 0f
    private val rnd = Random.Default

    private val minAlive = 4
    private val maxAlive = 7

    fun resize(w: Float, h: Float, d: Float) {
        width = w
        height = h
        density = d
    }

    fun reset() {
        bubbles.clear()
        particles.clear()
        spawnTimer = 0f
    }

    fun update(dt: Float) {
        if (width <= 0f || height <= 0f) return

        spawnTimer -= dt
        val alive = bubbles.count { !it.popping }
        if (alive < minAlive || (spawnTimer <= 0f && alive < maxAlive)) {
            spawn()
            spawnTimer = 0.8f + rnd.nextFloat() * 0.6f
        }

        val wobble = 22f * density
        for (b in bubbles) {
            if (b.popping) {
                b.popT += dt
            } else {
                b.age += dt
                b.x += b.vx * dt + cos(b.age * 1.6f + b.wobblePhase) * wobble * dt
                b.y += b.vy * dt + sin(b.age * 1.2f + b.wobblePhase) * wobble * 0.5f * dt
            }
        }
        bubbles.removeAll { it.popping && it.popT > POP_DURATION || isGone(it) }

        val gravity = 500f * density
        for (p in particles) {
            p.life += dt
            p.vy += gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
        }
        particles.removeAll { it.life > PARTICLE_LIFE }
    }

    /** Pops the top-most bubble under [p]. Returns true if one popped. */
    fun tap(p: Offset): Boolean {
        for (b in bubbles.asReversed()) {
            if (b.popping) continue
            val dx = p.x - b.x
            val dy = p.y - b.y
            val r = b.radius * 1.15f // forgiving touch area for small fingers
            if (dx * dx + dy * dy <= r * r) {
                pop(b)
                return true
            }
        }
        return false
    }

    private fun pop(b: Bubble) {
        b.popT = 0f
        val color = BubbleColors[b.colorIndex]
        repeat(12) { i ->
            val angle = (i / 12f) * 2f * Math.PI.toFloat() + rnd.nextFloat() * 0.4f
            val speed = (160f + rnd.nextFloat() * 220f) * density
            particles += Particle(
                x = b.x + cos(angle) * b.radius * 0.6f,
                y = b.y + sin(angle) * b.radius * 0.6f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 120f * density,
                color = color,
                size = (5f + rnd.nextFloat() * 6f) * density,
            )
        }
        onPop(b.colorIndex)
    }

    private fun isGone(b: Bubble): Boolean {
        if (b.age > 40f) return true
        if (b.age < 2f) return false
        val m = b.radius * 1.5f
        return b.x < -m || b.x > width + m || b.y < -m || b.y > height + m
    }

    private fun spawn() {
        val r = (60f + rnd.nextFloat() * 30f) * density // 120–180 dp wide
        val speed = (70f + rnd.nextFloat() * 70f) * density
        val drift = (rnd.nextFloat() - 0.5f) * 40f * density
        val color = rnd.nextInt(BubbleColors.size)
        val phase = rnd.nextFloat() * 6.28f
        val side = rnd.nextInt(10)

        val bubble = when {
            side < 6 -> Bubble( // from the bottom, floating up
                x = r + rnd.nextFloat() * (width - 2 * r).coerceAtLeast(1f),
                y = height + r, vx = drift, vy = -speed,
                radius = r, colorIndex = color, wobblePhase = phase,
            )
            side < 8 -> { // from the left or right edge
                val fromLeft = rnd.nextBoolean()
                Bubble(
                    x = if (fromLeft) -r else width + r,
                    y = r + rnd.nextFloat() * (height - 2 * r).coerceAtLeast(1f),
                    vx = if (fromLeft) speed else -speed, vy = drift,
                    radius = r, colorIndex = color, wobblePhase = phase,
                )
            }
            else -> Bubble( // from the top, drifting down
                x = r + rnd.nextFloat() * (width - 2 * r).coerceAtLeast(1f),
                y = -r, vx = drift, vy = speed * 0.8f,
                radius = r, colorIndex = color, wobblePhase = phase,
            )
        }
        bubbles += bubble
    }
}
