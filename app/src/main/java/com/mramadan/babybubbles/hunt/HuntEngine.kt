package com.mramadan.babybubbles.hunt

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.ui.graphics.Color
import com.mramadan.babybubbles.R
import com.mramadan.babybubbles.game.Cloud
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** A bird that can be caught: faster birds are rarer and worth more. */
class BirdType(
    @DrawableRes val image: Int,
    @RawRes val sound: Int,
    val arabic: String,
    val points: Int,
    /** dp per second */
    val speed: Float,
    /** dp */
    val size: Float,
    val weight: Int,
)

val HuntBirds = listOf(
    BirdType(R.drawable.bird_sparrow, R.raw.snd_sparrow, "عصفور", 10, 75f, 60f, 5),
    BirdType(R.drawable.bird_pigeon, R.raw.snd_pigeon, "حمامة", 15, 100f, 66f, 4),
    BirdType(R.drawable.bird_crow, R.raw.snd_crow, "غراب", 20, 125f, 66f, 3),
    BirdType(R.drawable.bird_parrot, R.raw.snd_parrot, "ببغاء", 30, 155f, 72f, 2),
    BirdType(R.drawable.bird_falcon, R.raw.snd_falcon, "صقر", 50, 205f, 74f, 1),
)

class HuntBird(val type: Int, var x: Float, val baseY: Float, val dir: Float, val phase: Float) {
    var t = 0f
    /** Negative while flying; seconds since it was caught. */
    var hitT = -1f
}

class Feather(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, val spin: Float) {
    var life = 0f
}

class FloatText(val x: Float, var y: Float, val text: String) {
    var life = 0f
}

class Flash(val x: Float, val y: Float) {
    var life = 0f
}

interface HuntEvents {
    fun onShot()
    fun onHit(type: Int)
    fun onTimeUp()
}

const val HIT_TIME = 0.35f
const val FEATHER_LIFE = 1.1f
const val TEXT_LIFE = 0.9f
const val FLASH_LIFE = 0.25f

/**
 * One player's turn: birds fly across the top half, the crosshair can't go below
 * the middle of the screen, and each catch adds that bird's points.
 */
class HuntEngine(private val events: HuntEvents) {
    val birds = mutableListOf<HuntBird>()
    val feathers = mutableListOf<Feather>()
    val texts = mutableListOf<FloatText>()
    val flashes = mutableListOf<Flash>()
    val clouds = mutableListOf<Cloud>()

    var width = 0f
        private set
    var height = 0f
        private set
    var density = 1f
        private set
    var time = 0f
        private set

    /** Bottom edge of the score bar, in pixels; birds and crosshair stay below it. */
    var topLimit = 0f

    var crossX = -1f
        private set
    var crossY = -1f
        private set

    var score = 0
        private set
    var timeLeft = 0f
        private set
    var running = false
        private set

    private var spawnTimer = 0f
    private var lastShot = -10f
    private val rnd = Random.Default
    private val maxBirds = 5

    val hillsHeight: Float get() = width * 140f / 390f

    /** Lowest point the crosshair (and the birds) may reach: the middle of the screen. */
    val maxY: Float get() = height * 0.5f

    fun start(seconds: Int) {
        birds.clear()
        feathers.clear()
        texts.clear()
        flashes.clear()
        score = 0
        timeLeft = seconds.toFloat()
        running = true
        spawnTimer = 0f
        crossX = -1f
        crossY = -1f
    }

    fun resize(w: Float, h: Float, d: Float) {
        width = w
        height = h
        density = d
        if (crossX < 0f && w > 0f) {
            crossX = w / 2f
            crossY = ((topLimit + maxY) / 2f).coerceAtLeast(h * 0.3f)
        }
        if (clouds.isEmpty() && w > 0f) {
            clouds += Cloud(w * 0.55f, topLimit + 10f * d, 0.9f, 8f * d, seed = 5)
            clouds += Cloud(w * 0.05f, topLimit + 90f * d, 0.7f, 5f * d, seed = 6)
        }
    }

    fun moveCrosshair(dx: Float, dy: Float) {
        if (width <= 0f) return
        val m = 24f * density
        crossX = (crossX + dx).coerceIn(m, width - m)
        crossY = (crossY + dy).coerceIn(topLimit + m, maxY)
    }

    fun birdY(b: HuntBird): Float = b.baseY + sin(b.t * 3f + b.phase) * 10f * density

    /** Shoots at the crosshair; catches at most one bird. */
    fun fire() {
        if (!running || time - lastShot < 0.25f) return
        lastShot = time
        flashes += Flash(crossX, crossY)
        events.onShot()
        var best: HuntBird? = null
        var bestD = Float.MAX_VALUE
        for (b in birds) {
            if (b.hitT >= 0f) continue
            val half = HuntBirds[b.type].size * density * 0.55f
            val dx = b.x - crossX
            val dy = birdY(b) - crossY
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < half && dist < bestD) {
                best = b
                bestD = dist
            }
        }
        val hit = best ?: return
        hit.hitT = 0f
        val type = HuntBirds[hit.type]
        score += type.points
        val y = birdY(hit)
        texts += FloatText(hit.x, y, "+${type.points}")
        val colors = listOf(Color.White, Color(0xFFFFD93D), Color(0xFFBFE6FF), Color(0xFFFF9DB0))
        repeat(12) { i ->
            val a = (i / 12f) * 2f * PI.toFloat() + rnd.nextFloat() * 0.4f
            val sp = (80f + rnd.nextFloat() * 140f) * density
            feathers += Feather(hit.x, y, cos(a) * sp, sin(a) * sp - 60f * density, colors[i % colors.size], rnd.nextFloat() * 6f - 3f)
        }
        events.onHit(hit.type)
    }

    fun update(dt: Float) {
        if (width <= 0f) return
        time += dt
        for (c in clouds) {
            c.x += c.speed * dt
            if (c.x > width + 10f * density) c.x = -200f * density * c.scale
        }

        if (running) {
            timeLeft -= dt
            if (timeLeft <= 0f) {
                timeLeft = 0f
                running = false
                events.onTimeUp()
            }
        }

        spawnTimer -= dt
        if (running && spawnTimer <= 0f && birds.count { it.hitT < 0f } < maxBirds) {
            spawn()
            spawnTimer = 0.7f + rnd.nextFloat() * 0.9f
        }

        for (b in birds) {
            b.t += dt
            if (b.hitT >= 0f) b.hitT += dt else b.x += b.dir * HuntBirds[b.type].speed * density * dt
        }
        birds.removeAll { b ->
            val s = HuntBirds[b.type].size * density
            b.hitT > HIT_TIME || b.x < -s * 1.5f || b.x > width + s * 1.5f
        }

        val gravity = 220f * density
        for (f in feathers) {
            f.life += dt
            f.vx *= 0.97f
            f.vy = f.vy * 0.97f + gravity * dt
            f.x += f.vx * dt
            f.y += f.vy * dt
        }
        feathers.removeAll { it.life > FEATHER_LIFE }

        for (t in texts) {
            t.life += dt
            t.y -= 50f * density * dt
        }
        texts.removeAll { it.life > TEXT_LIFE }

        for (f in flashes) f.life += dt
        flashes.removeAll { it.life > FLASH_LIFE }
    }

    private fun spawn() {
        val total = HuntBirds.sumOf { it.weight }
        var pick = rnd.nextInt(total)
        var type = 0
        for ((i, b) in HuntBirds.withIndex()) {
            if (pick < b.weight) {
                type = i
                break
            }
            pick -= b.weight
        }
        val s = HuntBirds[type].size * density
        val dir = if (rnd.nextBoolean()) 1f else -1f
        val minY = topLimit + s * 0.6f + 10f * density
        val maxY = (maxY - s * 0.4f).coerceAtLeast(minY + 1f)
        birds += HuntBird(
            type = type,
            x = if (dir > 0f) -s else width + s,
            baseY = minY + rnd.nextFloat() * (maxY - minY),
            dir = dir,
            phase = rnd.nextFloat() * 6.28f,
        )
    }
}
