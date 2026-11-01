package com.mramadan.babybubbles.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bright, high-contrast colors. Index i also selects musical note i. */
val BubbleColors = listOf(
    Color(0xFFFF4D4D), Color(0xFFFFD93D), Color(0xFF4D96FF), Color(0xFF6BCB77),
    Color(0xFFF15BB5), Color(0xFF9B5DE5), Color(0xFF2EC4B6), Color(0xFFFF9F1C),
    Color(0xFFA3E635), Color(0xFF38BDF8),
)

enum class GameMode { COLORS, ANIMALS }

enum class WalkerKind { DUCK, SNAIL, BUTTERFLY }

interface GameEvents {
    fun onColorPop(colorIndex: Int)
    fun onAnimalPop(animalIndex: Int)
    fun onWalkerTap(kind: WalkerKind)
}

const val COLOR_POP = 0.22f
const val ANIMAL_POP = 0.8f
const val PARTICLE_LIFE = 0.6f
const val RING_LIFE = 0.45f
const val NOTE_LIFE = 0.9f
const val HOP_TIME = 0.5f

class Bubble(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val colorIndex: Int,
    /** Index into [AnimalCatalog], or -1 for a plain color bubble. */
    val animal: Int,
    val wobblePhase: Float,
) {
    var age = 0f
    /** Negative while alive; seconds since the pop once popped. */
    var popT = -1f
    val popping: Boolean get() = popT >= 0f
    val popDuration: Float get() = if (animal >= 0) ANIMAL_POP else COLOR_POP
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

class Ring(val x: Float, val y: Float, val radius: Float, val color: Color) {
    var life = 0f
}

class FloatingNote(val x: Float, var y: Float) {
    var life = 0f
}

class Walker(val kind: WalkerKind, var x: Float, var dir: Float, val speed: Float) {
    var t = 0f
    /** Negative when not hopping; seconds into the hop otherwise. */
    var hopT = -1f
}

class Cloud(var x: Float, val y: Float, val scale: Float, val speed: Float)

/**
 * Pure game state: spawning, movement, hit testing and pop effects.
 * Units are pixels; [density] converts the dp-based tuning values.
 */
class GameEngine(private val events: GameEvents) {
    var mode = GameMode.COLORS
        private set

    val bubbles = mutableListOf<Bubble>()
    val particles = mutableListOf<Particle>()
    val rings = mutableListOf<Ring>()
    val notes = mutableListOf<FloatingNote>()
    val walkers = mutableListOf<Walker>()
    val clouds = mutableListOf<Cloud>()

    var width = 0f
        private set
    var height = 0f
        private set
    var density = 1f
        private set
    var time = 0f
        private set

    private var spawnTimer = 0f
    private var lastAnimal = -1
    private val rnd = Random.Default

    private val minAlive = 4
    private val maxAlive = 7

    /** Height of the grassy hills at the bottom of the animals scene. */
    val hillsHeight: Float get() = width * 140f / 390f

    fun start(newMode: GameMode) {
        mode = newMode
        bubbles.clear()
        particles.clear()
        rings.clear()
        notes.clear()
        walkers.clear()
        clouds.clear()
        spawnTimer = 0f
        time = 0f
    }

    fun resize(w: Float, h: Float, d: Float) {
        width = w
        height = h
        density = d
    }

    fun update(dt: Float) {
        if (width <= 0f || height <= 0f) return
        time += dt
        if (mode == GameMode.ANIMALS && walkers.isEmpty()) setupScene()

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
        bubbles.removeAll { if (it.popping) it.popT > it.popDuration else isGone(it) }

        val gravity = 500f * density
        for (p in particles) {
            p.life += dt
            p.vy += gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
        }
        particles.removeAll { it.life > PARTICLE_LIFE }

        for (r in rings) r.life += dt
        rings.removeAll { it.life > RING_LIFE }

        for (n in notes) {
            n.life += dt
            n.y -= 70f * density * dt
        }
        notes.removeAll { it.life > NOTE_LIFE }

        for (w in walkers) updateWalker(w, dt)

        for (c in clouds) {
            c.x += c.speed * dt
            if (c.x > width + 20f * density) c.x = -120f * density * c.scale
        }
    }

    /** Pops the top-most bubble under [p], or makes a background animal hop. */
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
        if (mode == GameMode.ANIMALS) {
            for (w in walkers.asReversed()) {
                if (w.hopT < 0f && walkerRect(w).inflate(10f * density).contains(p)) {
                    w.hopT = 0f
                    events.onWalkerTap(w.kind)
                    return true
                }
            }
        }
        return false
    }

    fun walkerSize(kind: WalkerKind): Pair<Float, Float> = when (kind) {
        WalkerKind.DUCK -> 80f * density to 70f * density
        WalkerKind.SNAIL -> 90f * density to 60f * density
        WalkerKind.BUTTERFLY -> 54f * density to 45f * density
    }

    /** Where a background animal is drawn this frame (also its touch area). */
    fun walkerRect(w: Walker): Rect {
        val (ww, hh) = walkerSize(w.kind)
        val d = density
        val hop = if (w.hopT >= 0f) -sin(PI.toFloat() * w.hopT / HOP_TIME) * 34f * d else 0f
        val top = when (w.kind) {
            WalkerKind.DUCK -> height - hillsHeight * 0.30f - hh - abs(sin(w.t * 9f)) * 3f * d
            WalkerKind.SNAIL -> height - hillsHeight * 0.08f - hh
            WalkerKind.BUTTERFLY -> height - hillsHeight - 50f * d + sin(w.t * 2.2f) * 22f * d - hh / 2f
        }
        return Rect(w.x, top + hop, w.x + ww, top + hop + hh)
    }

    private fun updateWalker(w: Walker, dt: Float) {
        w.t += dt
        if (w.hopT >= 0f) {
            w.hopT += dt
            if (w.hopT > HOP_TIME) w.hopT = -1f
        }
        val (ww, _) = walkerSize(w.kind)
        w.x += w.dir * w.speed * dt
        if (w.kind == WalkerKind.BUTTERFLY) {
            if (w.x > width + ww) w.x = -ww
        } else {
            val margin = 8f * density
            if (w.x < margin) {
                w.x = margin
                w.dir = 1f
            } else if (w.x > width - margin - ww) {
                w.x = width - margin - ww
                w.dir = -1f
            }
        }
    }

    private fun setupScene() {
        val d = density
        walkers += Walker(WalkerKind.DUCK, width * 0.12f, 1f, 38f * d)
        walkers += Walker(WalkerKind.SNAIL, width * 0.62f, -1f, 12f * d)
        walkers += Walker(WalkerKind.BUTTERFLY, width * 0.4f, 1f, 45f * d)
        clouds += Cloud(width * 0.58f, 52f * d, 1f, 9f * d)
        clouds += Cloud(width * 0.3f, 118f * d, 0.67f, 6f * d)
        clouds += Cloud(-60f * d, 190f * d, 0.8f, 7f * d)
    }

    private fun pop(b: Bubble) {
        b.popT = 0f
        if (b.animal >= 0) {
            burst(b, 14) { i ->
                when (i % 3) {
                    0 -> BubbleColors[rnd.nextInt(BubbleColors.size)]
                    1 -> Color.White
                    else -> Color(0xFFBFE6FF)
                }
            }
            rings += Ring(b.x, b.y, b.radius, Color.White)
            events.onAnimalPop(b.animal)
        } else {
            val color = BubbleColors[b.colorIndex]
            burst(b, 12) { color }
            rings += Ring(b.x, b.y, b.radius, color)
            notes += FloatingNote(b.x + b.radius * 0.5f, b.y - b.radius * 0.6f)
            events.onColorPop(b.colorIndex)
        }
    }

    private inline fun burst(b: Bubble, count: Int, colorAt: (Int) -> Color) {
        repeat(count) { i ->
            val angle = (i / count.toFloat()) * 2f * PI.toFloat() + rnd.nextFloat() * 0.4f
            val speed = (160f + rnd.nextFloat() * 220f) * density
            particles += Particle(
                x = b.x + cos(angle) * b.radius * 0.6f,
                y = b.y + sin(angle) * b.radius * 0.6f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 120f * density,
                color = colorAt(i),
                size = (5f + rnd.nextFloat() * 6f) * density,
            )
        }
    }

    private fun isGone(b: Bubble): Boolean {
        if (b.age > 40f) return true
        if (b.age < 2f) return false
        val m = b.radius * 1.5f
        return b.x < -m || b.x > width + m || b.y < -m || b.y > height + m
    }

    private fun nextAnimal(): Int {
        var a: Int
        do {
            a = rnd.nextInt(AnimalCatalog.size)
        } while (a == lastAnimal && AnimalCatalog.size > 1)
        lastAnimal = a
        return a
    }

    private fun spawn() {
        val animals = mode == GameMode.ANIMALS
        val r = if (animals) (66f + rnd.nextFloat() * 24f) * density else (60f + rnd.nextFloat() * 30f) * density
        val speed = (70f + rnd.nextFloat() * 70f) * density
        val drift = (rnd.nextFloat() - 0.5f) * 40f * density
        val color = rnd.nextInt(BubbleColors.size)
        val animal = if (animals) nextAnimal() else -1
        val phase = rnd.nextFloat() * 6.28f
        val side = rnd.nextInt(10)

        val bubble = when {
            side < 6 -> Bubble( // from the bottom, floating up
                x = r + rnd.nextFloat() * (width - 2 * r).coerceAtLeast(1f),
                y = height + r, vx = drift, vy = -speed,
                radius = r, colorIndex = color, animal = animal, wobblePhase = phase,
            )
            side < 8 -> { // from the left or right edge
                val fromLeft = rnd.nextBoolean()
                Bubble(
                    x = if (fromLeft) -r else width + r,
                    y = r + rnd.nextFloat() * (height - 2 * r).coerceAtLeast(1f),
                    vx = if (fromLeft) speed else -speed, vy = drift,
                    radius = r, colorIndex = color, animal = animal, wobblePhase = phase,
                )
            }
            else -> Bubble( // from the top, drifting down
                x = r + rnd.nextFloat() * (width - 2 * r).coerceAtLeast(1f),
                y = -r, vx = drift, vy = speed * 0.8f,
                radius = r, colorIndex = color, animal = animal, wobblePhase = phase,
            )
        }
        bubbles += bubble
    }
}
