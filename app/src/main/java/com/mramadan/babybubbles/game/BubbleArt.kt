package com.mramadan.babybubbles.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter

private val ShadowInk = Color(0xFF2B2550)

private fun DrawScope.softShadow(center: Offset, r: Float, strength: Float) {
    val c = Offset(center.x, center.y + r * 0.14f)
    val shadow = ShadowInk.copy(alpha = strength)
    drawCircle(
        brush = Brush.radialGradient(
            0f to shadow,
            0.75f to shadow.copy(alpha = strength * 0.8f),
            1f to Color.Transparent,
            center = c,
            radius = r * 1.22f,
        ),
        radius = r * 1.22f,
        center = c,
    )
}

/** Glossy solid bubble: white glint, light tint, base color, darker rim. */
fun DrawScope.drawColorBubble(
    center: Offset,
    r: Float,
    base: Color,
    alpha: Float = 1f,
    shadow: Boolean = true,
) {
    if (shadow) softShadow(center, r, 0.18f * alpha)
    val light = lerp(base, Color.White, 0.55f)
    val dark = lerp(base, Color.Black, 0.15f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White,
            0.06f to Color.White,
            0.13f to light,
            0.62f to base,
            1f to dark,
            center = Offset(center.x - r * 0.36f, center.y - r * 0.44f),
            radius = r * 1.98f,
        ),
        radius = r,
        center = center,
        alpha = alpha,
    )
}

/** See-through soap bubble with a white rim; an animal is drawn inside it. */
fun DrawScope.drawClearBubble(
    center: Offset,
    r: Float,
    strokeWidth: Float,
    alpha: Float = 1f,
    shadow: Boolean = true,
) {
    if (shadow) softShadow(center, r, 0.14f * alpha)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.95f),
            0.07f to Color.White.copy(alpha = 0.95f),
            0.15f to Color(0xFFEBF5FF).copy(alpha = 0.6f),
            0.68f to Color(0xFFD2E8FF).copy(alpha = 0.4f),
            1f to Color(0xFFA591FF).copy(alpha = 0.38f),
            center = Offset(center.x - r * 0.4f, center.y - r * 0.48f),
            radius = r * 2.04f,
        ),
        radius = r,
        center = center,
        alpha = alpha,
    )
    drawCircle(
        color = Color.White,
        radius = r - strokeWidth / 2f,
        center = center,
        alpha = 0.9f * alpha,
        style = Stroke(strokeWidth),
    )
}

/** The tilted shine drawn on top of a clear bubble. */
fun DrawScope.drawBubbleGloss(center: Offset, r: Float, alpha: Float = 1f) {
    val c = Offset(center.x - r * 0.45f, center.y - r * 0.65f)
    val w = r * 0.45f
    val h = r * 0.21f
    rotate(-32f, pivot = c) {
        drawOval(
            color = Color.White,
            topLeft = Offset(c.x - w / 2f, c.y - h / 2f),
            size = Size(w, h),
            alpha = 0.85f * alpha,
        )
    }
}

fun DrawScope.drawPainterCentered(painter: Painter, center: Offset, side: Float, alpha: Float = 1f) {
    translate(center.x - side / 2f, center.y - side / 2f) {
        with(painter) { draw(Size(side, side), alpha = alpha) }
    }
}

/** Two beamed eighth notes, [height] tall, centered on [center]. */
fun DrawScope.drawMusicNote(center: Offset, height: Float, color: Color, alpha: Float) {
    val k = height / 40f
    fun pt(x: Float, y: Float) = Offset(center.x + (x - 68f) * k, center.y + (y - 72f) * k)
    val w = 4f * k
    drawLine(color, pt(62f, 86f), pt(62f, 58f), w, StrokeCap.Round, alpha = alpha)
    drawLine(color, pt(62f, 58f), pt(84f, 52f), w, StrokeCap.Round, alpha = alpha)
    drawLine(color, pt(84f, 52f), pt(84f, 80f), w, StrokeCap.Round, alpha = alpha)
    drawOval(color, topLeft = pt(50f, 81.5f), size = Size(14f * k, 11f * k), alpha = alpha)
    drawOval(color, topLeft = pt(72f, 75.5f), size = Size(14f * k, 11f * k), alpha = alpha)
}
