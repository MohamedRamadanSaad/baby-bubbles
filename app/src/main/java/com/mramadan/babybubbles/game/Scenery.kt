package com.mramadan.babybubbles.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Seconds for one full day: morning, rain, sunset, night, back to morning. */
const val SKY_CYCLE = 150f

class SceneArt(
    val cloud: Painter,
    val hills: Painter,
    val duck: Painter,
    val snail: Painter,
    val butterfly: Painter,
    val car: Painter,
    val bird: Painter,
    val fish: Painter,
    val fish2: Painter,
    val jungleTree: Painter,
)

private class SkyKey(val t: Float, val top: Color, val bottom: Color)

private val SkyKeys = listOf(
    SkyKey(0.00f, Color(0xFF9AD5FF), Color(0xFFFFE8C8)), // dawn
    SkyKey(0.08f, Color(0xFF7CC8FF), Color(0xFFE4F5FF)), // morning
    SkyKey(0.28f, Color(0xFF7CC8FF), Color(0xFFE4F5FF)),
    SkyKey(0.32f, Color(0xFF8A99A8), Color(0xFFC9D3DC)), // rain
    SkyKey(0.40f, Color(0xFF8A99A8), Color(0xFFC9D3DC)),
    SkyKey(0.44f, Color(0xFF8CCBF5), Color(0xFFFFE3B3)), // clearing, warm
    SkyKey(0.51f, Color(0xFF5B5BA8), Color(0xFFFF9E6B)), // sunset
    SkyKey(0.57f, Color(0xFF2A2D63), Color(0xFF8E5B8F)), // dusk
    SkyKey(0.63f, Color(0xFF0E1A40), Color(0xFF273C75)), // night
    SkyKey(0.93f, Color(0xFF0E1A40), Color(0xFF273C75)),
    SkyKey(1.00f, Color(0xFF9AD5FF), Color(0xFFFFE8C8)),
)

private fun ramp(p: Float, a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)

/** Where we are in the day, 0..1. */
fun dayPhase(time: Float): Float {
    val p = (time / SKY_CYCLE) % 1f
    return if (p < 0f) p + 1f else p
}

fun rainAmount(p: Float) = ramp(p, 0.30f, 0.32f) * (1f - ramp(p, 0.40f, 0.43f))
fun nightAmount(p: Float) = ramp(p, 0.55f, 0.63f) * (1f - ramp(p, 0.93f, 1f))
private fun sunsetAmount(p: Float) = ramp(p, 0.44f, 0.51f) * (1f - ramp(p, 0.51f, 0.57f))

private fun skyColors(p: Float): Pair<Color, Color> {
    for (i in 0 until SkyKeys.size - 1) {
        val a = SkyKeys[i]
        val b = SkyKeys[i + 1]
        if (p <= b.t) {
            val f = ((p - a.t) / (b.t - a.t)).coerceIn(0f, 1f)
            return lerp(a.top, b.top, f) to lerp(a.bottom, b.bottom, f)
        }
    }
    return SkyKeys.last().top to SkyKeys.last().bottom
}

private fun hash(i: Int, salt: Int): Float {
    val x = sin(i * 12.9898f + salt * 78.233f) * 43758.547f
    return x - floor(x)
}

private fun modulate(c: Color) = ColorFilter.tint(c, BlendMode.Modulate)

/** Night-time dimming applied to things standing in the scene. */
fun sceneTint(time: Float): Color {
    val p = dayPhase(time)
    return lerp(Color.White, Color(0xFF9AA6D6), nightAmount(p) * 0.55f)
}

private val SunYellow = Color(0xFFFFD95A)
private val SunOrange = Color(0xFFFF8A3D)
private val MoonColor = Color(0xFFF4F1DE)

/** Sky, sun or moon, stars, clouds, hills and (for vehicles) a road. */
fun DrawScope.drawLandScene(e: GameEngine, art: SceneArt) {
    val d = e.density
    val p = dayPhase(e.time)
    val night = nightAmount(p)
    val rain = rainAmount(p)
    val (top, bottom) = skyColors(p)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))

    // Stars and a shooting star at night.
    if (night > 0.01f) {
        for (i in 0 until 46) {
            val x = hash(i, 1) * size.width
            val y = hash(i, 2) * size.height * 0.6f
            val tw = 0.55f + 0.45f * sin(e.time * (1.5f + hash(i, 3) * 2f) + i)
            drawCircle(Color.White, radius = (1.2f + hash(i, 4) * 1.8f) * d, center = Offset(x, y), alpha = night * tw)
        }
        if (night > 0.8f) drawShootingStar(e.time, d, night)
    }

    val horizon = size.height - e.hillsHeight * 0.55f
    val peak = 90f * d

    // Moon (second half of the cycle).
    if (p >= 0.57f) {
        val u = (p - 0.57f) / 0.43f
        val c = Offset(size.width * (0.08f + 0.84f * u), horizon - sin(PI.toFloat() * u) * (horizon - peak))
        drawCircle(MoonColor, radius = 58f * d, center = c, alpha = 0.10f * night)
        drawCircle(MoonColor, radius = 44f * d, center = c, alpha = 0.16f * night)
        drawCircle(MoonColor, radius = 32f * d, center = c, alpha = night)
        drawCircle(Color(0xFFDAD5BE), radius = 6f * d, center = c + Offset(-9f * d, -6f * d), alpha = night)
        drawCircle(Color(0xFFDAD5BE), radius = 4f * d, center = c + Offset(10f * d, 8f * d), alpha = night)
        drawCircle(Color(0xFFDAD5BE), radius = 3f * d, center = c + Offset(6f * d, -12f * d), alpha = night)
    }

    // Sun (first half), crossing the sky and setting behind the hills.
    if (p <= 0.57f) {
        val u = p / 0.57f
        val c = Offset(size.width * (0.08f + 0.84f * u), horizon - sin(PI.toFloat() * u) * (horizon - peak))
        val sunColor = lerp(SunYellow, SunOrange, sunsetAmount(p))
        val dim = 1f - rain * 0.65f
        val pulse = 1f + 0.04f * sin(e.time * 1.2f)
        drawCircle(sunColor, radius = 72f * d * pulse, center = c, alpha = 0.14f * dim)
        drawCircle(sunColor, radius = 56f * d * pulse, center = c, alpha = 0.28f * dim)
        drawCircle(sunColor, radius = 42f * d, center = c, alpha = dim)
    }

    val cloudTint = lerp(lerp(Color.White, Color(0xFF8E9AAB), rain), Color(0xFF59648C), night * 0.8f)
    for (c in e.clouds) {
        val s = c.scale * (1f + rain * 0.35f)
        translate(c.x, c.y) {
            with(art.cloud) {
                draw(Size(120f * d * s, 63f * d * s), alpha = 0.95f, colorFilter = modulate(cloudTint))
            }
        }
    }

    val hh = e.hillsHeight
    if (e.mode == GameMode.JUNGLE) drawJungleTrees(e, art, cloudTint)
    val groundTint = lerp(lerp(Color.White, Color(0xFFC3CFC3), rain * 0.5f), Color(0xFF4A5A8C), night * 0.75f)
    translate(0f, size.height - hh) {
        with(art.hills) { draw(Size(size.width, hh), colorFilter = modulate(groundTint)) }
    }

    if (e.mode == GameMode.VEHICLES) drawRoad(e, groundTint)

    drawWalkers(e, art)

    if (rain > 0.01f) drawRain(e.time, d, rain)
}

private fun DrawScope.drawShootingStar(time: Float, d: Float, night: Float) {
    val period = 5f
    val slot = floor(time / period).toInt()
    val local = time - slot * period
    val dur = 0.9f
    if (local > dur) return
    val q = local / dur
    val start = Offset(size.width * (0.35f + hash(slot, 7) * 0.6f), 30f * d + hash(slot, 8) * size.height * 0.22f)
    val dir = Offset(-0.876f, 0.482f)
    val head = start + dir * (q * 230f * d)
    val tail = head - dir * (70f * d)
    drawLine(
        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0f), Color.White), start = tail, end = head),
        start = tail,
        end = head,
        strokeWidth = 3f * d,
        cap = StrokeCap.Round,
        alpha = (1f - q) * night,
    )
    drawCircle(Color.White, radius = 2.6f * d, center = head, alpha = (1f - q) * night)
}

private fun DrawScope.drawRain(time: Float, d: Float, amount: Float) {
    val fall = 700f * d
    val h = size.height + 40f * d
    for (i in 0 until 90) {
        val y = ((hash(i, 11) * h + time * fall * (0.8f + hash(i, 12) * 0.4f)) % h) - 20f * d
        val x = hash(i, 13) * (size.width + 40f * d) - y * 0.12f
        drawLine(
            color = Color(0xFFDDEEFF),
            start = Offset(x, y),
            end = Offset(x - 3f * d, y + 15f * d),
            strokeWidth = 2f * d,
            cap = StrokeCap.Round,
            alpha = 0.7f * amount,
        )
    }
}

private fun DrawScope.drawRoad(e: GameEngine, tint: Color) {
    val d = e.density
    val cy = e.roadCenterY()
    val half = 26f * d
    val asphalt = Color(0xFF6B7280).times(tint)
    drawRect(asphalt, topLeft = Offset(0f, cy - half), size = Size(size.width, half * 2))
    drawRect(Color(0xFF4B5563).times(tint), topLeft = Offset(0f, cy - half), size = Size(size.width, 3f * d))
    val dash = 26f * d
    var x = 6f * d
    while (x < size.width) {
        drawLine(Color.White.times(tint), Offset(x, cy), Offset(x + dash, cy), strokeWidth = 3.5f * d, cap = StrokeCap.Round, alpha = 0.9f)
        x += dash * 2f
    }
}

private fun Color.times(t: Color) = Color(red * t.red, green * t.green, blue * t.blue, alpha)

fun DrawScope.drawWalkers(e: GameEngine, art: SceneArt) {
    val tint = modulate(sceneTint(e.time))
    for (w in e.walkers) {
        val rect = e.walkerRect(w)
        val painter = when (w.kind) {
            WalkerKind.DUCK -> art.duck
            WalkerKind.SNAIL -> art.snail
            WalkerKind.BUTTERFLY -> art.butterfly
            WalkerKind.CAR -> art.car
            WalkerKind.BIRD -> art.bird
            WalkerKind.FISH -> art.fish
            WalkerKind.FISH2 -> art.fish2
        }
        val flip = if (w.dir < 0f) -1f else 1f
        val flap = when (w.kind) {
            WalkerKind.BUTTERFLY -> 0.55f + 0.45f * abs(cos(w.t * 11f))
            WalkerKind.BIRD -> 1f
            else -> 1f
        }
        val squash = if (w.kind == WalkerKind.BIRD) 0.85f + 0.15f * abs(sin(w.t * 9f)) else 1f
        scale(scaleX = flip * flap, scaleY = squash, pivot = rect.center) {
            translate(rect.left, rect.top) {
                with(painter) {
                    draw(rect.size, colorFilter = if (e.mode == GameMode.SEA) null else tint)
                }
            }
        }
    }
}

/** Underwater: blue water, light rays, rising bubbles, sand, seaweed and coral. */
fun DrawScope.drawSeaScene(e: GameEngine, art: SceneArt) {
    val d = e.density
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(0f to Color(0xFF6FD3FF), 0.5f to Color(0xFF2A8FD8), 1f to Color(0xFF0B4C9C)))

    // Sun rays from the surface.
    for (i in 0 until 4) {
        val sway = sin(e.time * 0.4f + i * 1.7f) * 20f * d
        val x0 = w * (0.15f + i * 0.24f) + sway
        val ray = Path().apply {
            moveTo(x0 - 18f * d, 0f)
            lineTo(x0 + 18f * d, 0f)
            lineTo(x0 + 90f * d + sway, h * 0.75f)
            lineTo(x0 + 20f * d + sway, h * 0.75f)
            close()
        }
        drawPath(ray, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0f)), endY = h * 0.75f))
    }

    // Small background bubbles rising.
    val rise = 60f * d
    for (i in 0 until 16) {
        val y = h - ((hash(i, 21) * h + e.time * rise * (0.7f + hash(i, 22) * 0.6f)) % h)
        val x = hash(i, 23) * w + sin(e.time * 1.3f + i) * 8f * d
        drawCircle(Color.White, radius = (3f + hash(i, 24) * 5f) * d, center = Offset(x, y), alpha = 0.45f, style = Stroke(1.5f * d))
    }

    // Sand.
    val sandTop = h - w * 0.2f
    val sand = Path().apply {
        moveTo(0f, sandTop + 10f * d)
        quadraticTo(w * 0.25f, sandTop - 14f * d, w * 0.5f, sandTop + 4f * d)
        quadraticTo(w * 0.78f, sandTop + 18f * d, w, sandTop - 6f * d)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(sand, Color(0xFFF2D9A0))
    for (i in 0 until 14) {
        drawCircle(Color(0xFFD9BC7E), radius = (2f + hash(i, 31) * 2f) * d,
            center = Offset(hash(i, 32) * w, sandTop + 24f * d + hash(i, 33) * (h - sandTop - 30f * d)))
    }

    // Seaweed swaying.
    val weedX = listOf(0.08f, 0.16f, 0.62f, 0.88f, 0.94f)
    weedX.forEachIndexed { i, fx ->
        val base = Offset(w * fx, sandTop + 12f * d)
        val tall = (70f + hash(i, 41) * 60f) * d
        val sway = sin(e.time * 1.2f + i) * 14f * d
        val weed = Path().apply {
            moveTo(base.x, base.y)
            quadraticTo(base.x - 18f * d + sway, base.y - tall * 0.5f, base.x + sway * 1.4f, base.y - tall)
        }
        drawPath(weed, if (i % 2 == 0) Color(0xFF3FA34D) else Color(0xFF2E8B57), style = Stroke(9f * d, cap = StrokeCap.Round))
    }

    // Coral.
    val coral = Offset(w * 0.36f, sandTop + 6f * d)
    for ((dx, dy, r) in listOf(Triple(0f, -10f, 12f), Triple(-14f, -2f, 9f), Triple(14f, -4f, 10f), Triple(-4f, -26f, 9f), Triple(10f, -22f, 7f))) {
        drawCircle(Color(0xFFFF7A93), radius = r * d, center = coral + Offset(dx * d, dy * d))
    }
    val coral2 = Offset(w * 0.76f, sandTop + 14f * d)
    for ((dx, dy, r) in listOf(Triple(0f, -8f, 10f), Triple(-12f, -2f, 8f), Triple(12f, 0f, 8f), Triple(2f, -22f, 7f))) {
        drawCircle(Color(0xFFFF9F6B), radius = r * d, center = coral2 + Offset(dx * d, dy * d))
    }

    drawWalkers(e, art)
}


/** Big leafy trees standing behind the hills in the jungle scene, dimmed at night. */
private fun DrawScope.drawJungleTrees(e: GameEngine, art: SceneArt, @Suppress("UNUSED_PARAMETER") cloudTint: Color) {
    val d = e.density
    val tint = modulate(sceneTint(e.time))
    val base = size.height - e.hillsHeight * 0.45f
    for ((fx, scale) in listOf(-0.06f to 1.25f, 0.62f to 1.0f, 0.3f to 0.75f, 0.86f to 0.85f)) {
        val w = 150f * d * scale
        val h = w * 160f / 120f
        val sway = sin(e.time * 0.7f + fx * 10f) * 1.5f
        rotate(sway, pivot = Offset(size.width * fx + w / 2f, base)) {
            translate(size.width * fx, base - h) {
                with(art.jungleTree) { draw(Size(w, h), colorFilter = tint) }
            }
        }
    }
}
