package com.mramadan.babybubbles.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Bright, high-contrast colors for colors mode. */
val BubbleColors = listOf(
    Color(0xFFFF4D4D), Color(0xFFFFD93D), Color(0xFF4D96FF), Color(0xFF6BCB77),
    Color(0xFFF15BB5), Color(0xFF9B5DE5), Color(0xFF2EC4B6), Color(0xFFFF9F1C),
    Color(0xFFA3E635), Color(0xFF38BDF8),
)

enum class SpeedLevel(val factor: Float) { SLOW(0.55f), NORMAL(1f), FAST(1.6f) }

enum class WalkerKind { DUCK, SNAIL, BUTTERFLY, CAR, BIRD, FISH, FISH2 }

interface GameEvents {
    fun onColorPop(colorIndex: Int)
    fun onCharacterPop(index: Int)
    fun onWalkerTap(kind: WalkerKind)
}

const val COLOR_POP = 0.22f
const val PARTICLE_LIFE = 0.6f
const val RING_LIFE = 0.45f
const val SPARKLE_LIFE = 0.8f
const val HOP_TIME = 0.5f

fun popDuration(style: PopStyle) = when (style) {
    PopStyle.JUMP -> 0.8f
    PopStyle.DRIVE -> 0.9f
    PopStyle.RISE -> 1.3f
}

class Bubble(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val colorIndex: Int,
    /** Index into the mode's character list, or -1 for a plain color bubble. */
    val character: Int,
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

class Ring(val x: Float, val y: Float, val radius: Float, val color: Color) {
    var life = 0f
}

class Sparkle(val x: Float, var y: Float, val color: Color) {
    var life = 0f
}

class Walker(val kind: WalkerKind, var x: Float, var dir: Float, val speed: Float, val lane: Float) {
    var t = 0f
    /** Negative when not hopping; seconds into the hop otherwise. */
    var hopT = -1f
}

class Cloud(var x: Float, val y: Float, val scale: Float, val speed: Float, val seed: Int = 0)

/**
 * Pure game state: spawning, movement, hit testing and pop effects.
 * Units are pixels; [density] converts the dp-based tuning values.
 */
class GameEngine(private val events: GameEvents) {
    var mode = GameMode.COLORS
        private set
    var speed = SpeedLevel.NORMAL
        private set
    private var characterCount = 0

    val bubbles = mutableListOf<Bubble>()
    val particles = mutableListOf<Particle>()
    val rings = mutableListOf<Ring>()
    val sparkles = mutableListOf<Sparkle>()
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
    private var lastCharacter = -1
    private var sceneReady = false
    private val rnd = Random.Default

    private var maxAlive = 6
    /** Below this many, a new bubble comes straight away so the screen never feels empty. */
    private val minAlive: Int get() = ((maxAlive * 0.6f) + 0.5f).toInt().coerceIn(1, maxAlive)

    /** Height of the grassy hills at the bottom of the land scenes. */
    val hillsHeight: Float get() = width * 140f / 390f

    val style: PopStyle get() = mode.popStyle

    fun start(newMode: GameMode, count: Int, newSpeed: SpeedLevel, maxBubbles: Int) {
        maxAlive = maxBubbles.coerceIn(1, 12)
        mode = newMode
        characterCount = count
        speed = newSpeed
        bubbles.clear()
        particles.clear()
        rings.clear()
        sparkles.clear()
        walkers.clear()
        clouds.clear()
        spawnTimer = 0f
        time = 0f
        sceneReady = false
    }

    fun resize(w: Float, h: Float, d: Float) {
        width = w
        height = h
        density = d
    }

    fun update(dt: Float) {
        if (width <= 0f || height <= 0f) return
        time += dt
        if (!sceneReady) setupScene()

        val sf = speed.factor
        spawnTimer -= dt * sf
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
                b.x += (b.vx * sf) * dt + cos(b.age * 1.6f + b.wobblePhase) * wobble * dt
                b.y += (b.vy * sf) * dt + sin(b.age * 1.2f + b.wobblePhase) * wobble * 0.5f * dt
            }
        }
        separate(dt)
        val dur = if (mode == GameMode.COLORS) COLOR_POP else popDuration(style)
        bubbles.removeAll { if (it.popping) it.popT > dur else isGone(it) }

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

        for (s in sparkles) {
            s.life += dt
            s.y -= 60f * density * dt
        }
        sparkles.removeAll { it.life > SPARKLE_LIFE }

        for (w in walkers) updateWalker(w, dt)

        for (c in clouds) {
            c.x += c.speed * dt
            if (c.x > width + 10f * density) c.x = -200f * density * c.scale
        }
    }

    /**
     * Keeps bubbles from hiding each other: they may touch and overlap a little,
     * but are gently pushed apart once more than about a fifth of them overlaps.
     */
    private fun separate(dt: Float) {
        val k = (dt * 6f).coerceAtMost(1f)
        for (i in bubbles.indices) {
            val a = bubbles[i]
            if (a.popping) continue
            for (j in i + 1 until bubbles.size) {
                val b = bubbles[j]
                if (b.popping) continue
                val dx = b.x - a.x
                val dy = b.y - a.y
                val minD = (a.radius + b.radius) * 0.8f
                val d2 = dx * dx + dy * dy
                if (d2 >= minD * minD) continue
                val d = sqrt(d2).coerceAtLeast(0.01f)
                val push = (minD - d) * 0.5f * k
                val nx = if (d2 < 0.0001f) 1f else dx / d
                val ny = if (d2 < 0.0001f) 0f else dy / d
                a.x -= nx * push
                a.y -= ny * push
                b.x += nx * push
                b.y += ny * push
            }
        }
    }

    /** Pops the top-most bubble under [p], or makes a background walker hop. */
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
        for (w in walkers.asReversed()) {
            if (w.hopT < 0f && walkerRect(w).inflate(10f * density).contains(p)) {
                w.hopT = 0f
                events.onWalkerTap(w.kind)
                return true
            }
        }
        return false
    }

    fun walkerSize(kind: WalkerKind): Pair<Float, Float> = when (kind) {
        WalkerKind.DUCK -> 80f * density to 70f * density
        WalkerKind.SNAIL -> 90f * density to 60f * density
        WalkerKind.BUTTERFLY -> 54f * density to 45f * density
        WalkerKind.CAR -> 96f * density to 96f * density
        WalkerKind.BIRD -> 64f * density to 64f * density
        WalkerKind.FISH, WalkerKind.FISH2 -> 72f * density to 72f * density
    }

    /** Where a background walker is drawn this frame (also its touch area). */
    fun walkerRect(w: Walker): Rect {
        val (ww, hh) = walkerSize(w.kind)
        val d = density
        val hop = if (w.hopT >= 0f) -sin(PI.toFloat() * w.hopT / HOP_TIME) * 34f * d else 0f
        val top = when (w.kind) {
            WalkerKind.DUCK -> height - hillsHeight * 0.30f - hh - abs(sin(w.t * 9f)) * 3f * d
            WalkerKind.SNAIL -> height - hillsHeight * 0.08f - hh
            WalkerKind.BUTTERFLY -> height - hillsHeight - 50f * d + sin(w.t * 2.2f) * 22f * d - hh / 2f
            WalkerKind.CAR -> roadCenterY() - hh * 0.72f - abs(sin(w.t * 14f)) * 1.5f * d
            WalkerKind.BIRD -> height * w.lane + sin(w.t * 3f) * 14f * d - hh / 2f
            WalkerKind.FISH, WalkerKind.FISH2 -> height * w.lane + sin(w.t * 1.6f) * 12f * d - hh / 2f
        }
        return Rect(w.x, top + hop, w.x + ww, top + hop + hh)
    }

    /** Vertical middle of the road drawn in vehicles mode. */
    fun roadCenterY(): Float = height - hillsHeight * 0.22f

    private fun updateWalker(w: Walker, dt: Float) {
        w.t += dt
        if (w.hopT >= 0f) {
            w.hopT += dt
            if (w.hopT > HOP_TIME) w.hopT = -1f
        }
        val (ww, _) = walkerSize(w.kind)
        w.x += w.dir * w.speed * dt
        when (w.kind) {
            WalkerKind.DUCK, WalkerKind.SNAIL -> {
                val margin = 8f * density
                if (w.x < margin) {
                    w.x = margin
                    w.dir = 1f
                } else if (w.x > width - margin - ww) {
                    w.x = width - margin - ww
                    w.dir = -1f
                }
            }
            else -> {
                if (w.dir > 0f && w.x > width + ww) w.x = -ww
                if (w.dir < 0f && w.x < -ww * 2f) w.x = width + ww * 0.5f
            }
        }
    }

    private fun setupScene() {
        sceneReady = true
        val d = density
        when (mode) {
            GameMode.ANIMALS -> {
                walkers += Walker(WalkerKind.DUCK, width * 0.12f, 1f, 38f * d, 0f)
                walkers += Walker(WalkerKind.SNAIL, width * 0.62f, -1f, 12f * d, 0f)
                walkers += Walker(WalkerKind.BUTTERFLY, width * 0.4f, 1f, 45f * d, 0f)
            }
            GameMode.VEHICLES -> walkers += Walker(WalkerKind.CAR, width * 0.1f, 1f, 70f * d, 0f)
            GameMode.BIRDS -> {
                walkers += Walker(WalkerKind.BIRD, width * 0.2f, 1f, 55f * d, 0.2f)
                walkers += Walker(WalkerKind.BIRD, width * 0.7f, 1f, 40f * d, 0.33f)
            }
            GameMode.FRUITS, GameMode.FLAGS ->
                walkers += Walker(WalkerKind.BUTTERFLY, width * 0.3f, 1f, 45f * d, 0f)
            GameMode.JUNGLE -> {
                walkers += Walker(WalkerKind.BUTTERFLY, width * 0.3f, 1f, 45f * d, 0f)
                walkers += Walker(WalkerKind.BIRD, width * 0.6f, 1f, 50f * d, 0.22f)
            }
            GameMode.SEA -> {
                walkers += Walker(WalkerKind.FISH, width * 0.1f, 1f, 35f * d, 0.3f)
                walkers += Walker(WalkerKind.FISH2, width * 0.8f, -1f, 28f * d, 0.62f)
            }
            GameMode.COLORS -> Unit
        }
        if (mode != GameMode.SEA) {
            clouds += Cloud(width * 0.55f, 46f * d, 1f, 9f * d, seed = 1)
            clouds += Cloud(width * 0.12f, 120f * d, 0.7f, 6f * d, seed = 2)
            clouds += Cloud(-90f * d, 196f * d, 0.85f, 7f * d, seed = 3)
        }
    }

    private fun pop(b: Bubble) {
        b.popT = 0f
        if (b.character >= 0) {
            burst(b, 14) { i ->
                when (i % 3) {
                    0 -> BubbleColors[rnd.nextInt(BubbleColors.size)]
                    1 -> Color.White
                    else -> Color(0xFFBFE6FF)
                }
            }
            rings += Ring(b.x, b.y, b.radius, Color.White)
            events.onCharacterPop(b.character)
        } else {
            val color = BubbleColors[b.colorIndex]
            burst(b, 12) { color }
            rings += Ring(b.x, b.y, b.radius, color)
            sparkles += Sparkle(b.x + b.radius * 0.5f, b.y - b.radius * 0.6f, Color(0xFFFFD93D))
            sparkles += Sparkle(b.x - b.radius * 0.45f, b.y - b.radius * 0.3f, Color.White)
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
        if (b.age > 60f) return true
        if (b.age < 2f) return false
        val m = b.radius * 1.5f
        return b.x < -m || b.x > width + m || b.y < -m || b.y > height + m
    }

    private fun nextCharacter(): Int {
        if (characterCount <= 0) return -1
        var a: Int
        do {
            a = rnd.nextInt(characterCount)
        } while (a == lastCharacter && characterCount > 1)
        lastCharacter = a
        return a
    }

    private class Spawn(val x: Float, val y: Float, val vx: Float, val vy: Float)

    private fun candidate(r: Float, speed: Float, drift: Float): Spawn {
        val side = rnd.nextInt(10)
        return when {
            side < 6 -> Spawn( // from the bottom, floating up
                r + rnd.nextFloat() * (width - 2 * r).coerceAtLeast(1f), height + r, drift, -speed,
            )
            side < 8 -> { // from the left or right edge
                val fromLeft = rnd.nextBoolean()
                Spawn(
                    if (fromLeft) -r else width + r,
                    r + rnd.nextFloat() * (height * 0.7f - 2 * r).coerceAtLeast(1f),
                    if (fromLeft) speed else -speed, drift,
                )
            }
            else -> Spawn( // from the top, drifting down
                r + rnd.nextFloat() * (width - 2 * r).coerceAtLeast(1f), -r, drift, speed * 0.8f,
            )
        }
    }

    private fun spawn() {
        val withCharacter = mode != GameMode.COLORS
        val r = if (withCharacter) (66f + rnd.nextFloat() * 24f) * density else (60f + rnd.nextFloat() * 30f) * density
        val speed = (70f + rnd.nextFloat() * 70f) * density
        val drift = (rnd.nextFloat() - 0.5f) * 40f * density

        // Try several entry points and take the one farthest from every bubble
        // already on screen, so new bubbles come from different places.
        var best = candidate(r, speed, drift)
        var bestScore = -1f
        repeat(8) {
            val c = candidate(r, speed, drift)
            var nearest = Float.MAX_VALUE
            for (o in bubbles) {
                if (o.popping) continue
                val dx = o.x - c.x
                val dy = o.y - c.y
                nearest = minOf(nearest, sqrt(dx * dx + dy * dy) - o.radius)
            }
            if (nearest > bestScore) {
                bestScore = nearest
                best = c
            }
        }

        bubbles += Bubble(
            x = best.x, y = best.y, vx = best.vx, vy = best.vy, radius = r,
            colorIndex = rnd.nextInt(BubbleColors.size),
            character = if (withCharacter) nextCharacter() else -1,
            wobblePhase = rnd.nextFloat() * 6.28f,
        )
    }
}
