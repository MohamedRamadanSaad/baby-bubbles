package com.mramadan.babybubbles.hunt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mramadan.babybubbles.game.SceneArt
import com.mramadan.babybubbles.game.drawWorld
import com.mramadan.babybubbles.parent.Rtl
import com.mramadan.babybubbles.parent.Segment
import com.mramadan.babybubbles.parent.SegmentGroup
import com.mramadan.babybubbles.parent.arabicDigits
import com.mramadan.babybubbles.ui.Baloo
import com.mramadan.babybubbles.ui.Palette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class HuntPhase { PLAYING, TURN_DONE, VS, RESULT }

/** A two-player match: player one plays a timed turn, then player two, then the winner is shown. */
class HuntMatch(val names: List<String>, val seconds: Int) {
    val scores = mutableStateListOf(0, 0)
    var player by mutableIntStateOf(0)
    var phase by mutableStateOf(HuntPhase.PLAYING)
}

private fun clock(seconds: Float): String {
    val s = kotlin.math.ceil(seconds).toInt().coerceAtLeast(0)
    val sec = s % 60
    return "${(s / 60).arabicDigits()}:${if (sec < 10) "٠" else ""}${sec.arabicDigits()}"
}

// ---------------------------------------------------------------- setup

@Composable
fun HuntSetupScreen(
    initialNames: Pair<String, String>,
    initialSeconds: Int,
    onStart: (String, String, Int) -> Unit,
    onBack: () -> Unit,
) = Rtl {
    BackHandler(onBack = onBack)
    var name1 by remember { mutableStateOf(initialNames.first) }
    var name2 by remember { mutableStateOf(initialNames.second) }
    var seconds by remember { mutableIntStateOf(initialSeconds) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Lavender)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .selectable(selected = false, role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(22.dp)) {
                    val k = size.width / 24f
                    drawLine(Palette.Ink, Offset(9f * k, 5f * k), Offset(16f * k, 12f * k), 2.6f * k, StrokeCap.Round)
                    drawLine(Palette.Ink, Offset(16f * k, 12f * k), Offset(9f * k, 19f * k), 2.6f * k, StrokeCap.Round)
                }
            }
            Text("صيد الطيور", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
        }
        Text(
            "لعبة لاعبَين: يلعب الأول دوره كاملًا، ثم الثاني، ومن يجمع نقاطًا أكثر يفوز. حرّك النيشان بإصبعك، واضغط زر «ضرب».",
            fontSize = 15.sp, lineHeight = 22.sp, color = Palette.Muted,
        )
        OutlinedTextField(
            value = name1, onValueChange = { name1 = it.take(20) }, singleLine = true,
            label = { Text("اسم اللاعب الأول") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = name2, onValueChange = { name2 = it.take(20) }, singleLine = true,
            label = { Text("اسم اللاعب الثاني") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        Text("مدة كل دور", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink)
        SegmentGroup {
            Segment("دقيقة", seconds == 60) { seconds = 60 }
            Segment("دقيقتان", seconds == 120) { seconds = 120 }
            Segment("٣ دقائق", seconds == 180) { seconds = 180 }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("نقاط كل طائر", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
            for (b in HuntBirds) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(b.image), contentDescription = null, modifier = Modifier.size(40.dp))
                    Text(b.arabic, fontSize = 16.sp, color = Palette.Ink, modifier = Modifier.weight(1f))
                    Text("${b.points.arabicDigits()} نقطة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.Purple)
                }
            }
            Text("الطيور الأسرع أندر وتستحق نقاطًا أكثر.", fontSize = 13.sp, color = Palette.Muted)
        }
        Button(
            onClick = {
                onStart(name1.trim().ifBlank { "اللاعب الأول" }, name2.trim().ifBlank { "اللاعب الثاني" }, seconds)
            },
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Purple, contentColor = Color.White),
        ) { Text("ابدأ المباراة", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
    }
}

// ---------------------------------------------------------------- the match

@Composable
fun HuntFlow(
    match: HuntMatch,
    engine: HuntEngine,
    art: SceneArt,
    onFire: () -> Unit,
    onTurnDoneNext: () -> Unit,
    onVsDone: () -> Unit,
    onAgain: () -> Unit,
    onCelebrate: () -> Unit,
) {
    when (match.phase) {
        HuntPhase.PLAYING, HuntPhase.TURN_DONE -> Box {
            HuntPlayScreen(engine, art, match.names[match.player], onFire)
            if (match.phase == HuntPhase.TURN_DONE) {
                TurnDoneCard(
                    name = match.names[match.player],
                    score = match.scores[match.player],
                    next = if (match.player == 0) "دور ${match.names[1]}" else "اعرض النتيجة",
                    onNext = onTurnDoneNext,
                )
            }
        }
        HuntPhase.VS -> VsScreen(match.names[0], match.scores[0], match.names[1], onVsDone)
        HuntPhase.RESULT -> ResultScreen(match.names, match.scores.toList(), onAgain, onCelebrate)
    }
}

@Composable
private fun HuntPlayScreen(engine: HuntEngine, art: SceneArt, playerName: String, onFire: () -> Unit) {
    var frame by remember { mutableLongStateOf(0L) }
    val density = LocalDensity.current.density
    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) engine.update(((now - last) / 1_000_000_000f).coerceAtMost(0.05f))
                last = now
                frame = now
            }
        }
    }
    val painters = HuntBirds.map { painterResource(it.image) }
    val measurer = rememberTextMeasurer()
    val pointsStyle = TextStyle(
        fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Color(0xFFFFD93D),
        shadow = Shadow(Color(0x992B2550), Offset(0f, 3f), 6f),
    )
    @Suppress("UNUSED_VARIABLE")
    val tick = frame // recompose the score bar every frame

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(engine) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        engine.moveCrosshair(drag.x * 1.15f, drag.y * 1.15f)
                    }
                }
        ) {
            @Suppress("UNUSED_VARIABLE")
            val t = frame
            engine.resize(size.width, size.height, density)
            drawWorld(engine.time, density, engine.hillsHeight, engine.clouds, null, art, phase = 0.12f)

            // The crosshair can't go lower than this line.
            drawLine(
                Color.White.copy(alpha = 0.35f), Offset(0f, engine.maxY), Offset(size.width, engine.maxY),
                strokeWidth = 2f * density,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f * density, 10f * density)),
            )

            for (b in engine.birds) {
                val type = HuntBirds[b.type]
                val s = type.size * density
                val y = engine.birdY(b)
                val q = if (b.hitT >= 0f) (b.hitT / HIT_TIME).coerceIn(0f, 1f) else 0f
                val grow = 1f + q * 0.5f
                val flip = if (b.dir < 0f) -1f else 1f
                val flap = if (b.hitT >= 0f) 1f else 0.8f + 0.2f * abs(sin(b.t * 10f))
                rotate(q * 40f * flip, pivot = Offset(b.x, y)) {
                    scale(scaleX = flip * grow, scaleY = flap * grow, pivot = Offset(b.x, y)) {
                        translate(b.x - s / 2f, y - s / 2f) {
                            with(painters[b.type]) { draw(Size(s, s), alpha = 1f - q) }
                        }
                    }
                }
            }

            for (f in engine.feathers) {
                val a = (1f - f.life / FEATHER_LIFE).coerceIn(0f, 1f)
                rotate(f.spin * f.life * 120f, pivot = Offset(f.x, f.y)) {
                    drawOval(f.color, topLeft = Offset(f.x - 6f * density, f.y - 2.5f * density), size = Size(12f * density, 5f * density), alpha = a)
                }
            }

            for (fl in engine.flashes) {
                val q = fl.life / FLASH_LIFE
                drawCircle(Color.White, radius = (14f + 30f * q) * density, center = Offset(fl.x, fl.y), alpha = 1f - q, style = Stroke(4f * density * (1f - q) + 1f))
            }

            for (tx in engine.texts) {
                val layout = measurer.measure(tx.text, pointsStyle)
                drawText(
                    layout,
                    topLeft = Offset(tx.x - layout.size.width / 2f, tx.y - layout.size.height),
                    alpha = (1f - tx.life / TEXT_LIFE).coerceIn(0f, 1f),
                )
            }

            drawCrosshair(Offset(engine.crossX, engine.crossY), density)
        }

        Rtl {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.9f))
                    .safeDrawingPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .onGloballyPositioned { engine.topLimit = it.positionInRoot().y + it.size.height },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(playerName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.Purple, maxLines = 1, modifier = Modifier.weight(1f))
                    Text("${engine.score.arabicDigits()} نقطة", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                    Spacer(Modifier.width(12.dp))
                    val urgent = engine.timeLeft <= 10f
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (urgent) Color(0xFFFF4D4D) else Palette.Track)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(clock(engine.timeLeft), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (urgent) Color.White else Palette.Purple)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    HuntBirds.forEachIndexed { i, b ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painters[i], contentDescription = b.arabic, modifier = Modifier.size(30.dp))
                            Text(b.points.arabicDigits(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                        }
                    }
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (engine.time < 6f) {
                    Text(
                        "حرّك إصبعك على الشاشة لتحريك النيشان",
                        fontSize = 14.sp, color = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Palette.Scrim.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                Box(
                    Modifier
                        .size(108.dp)
                        .shadow(14.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFFFF8A65), Color(0xFFE53935))))
                        .pointerInput(Unit) { detectTapGestures(onPress = { onFire() }) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Canvas(Modifier.size(28.dp)) { drawCrosshair(center, density * 0.45f, Color.White) }
                        Text("ضرب", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCrosshair(c: Offset, d: Float, color: Color = Color(0xFFFF3B3B)) {
    val r = 26f * d
    drawCircle(Color(0x552B2550), radius = r, center = c, style = Stroke(7f * d))
    drawCircle(color, radius = r, center = c, style = Stroke(3.5f * d))
    drawCircle(color, radius = 3.5f * d, center = c)
    for (k in 0 until 4) {
        val a = k * PI.toFloat() / 2f
        val dir = Offset(cos(a), sin(a))
        drawLine(color, c + dir * (r * 0.45f), c + dir * (r * 1.4f), strokeWidth = 3.2f * d, cap = StrokeCap.Round)
    }
}

@Composable
private fun TurnDoneCard(name: String, score: Int, next: String, onNext: () -> Unit) = Rtl {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Scrim.copy(alpha = 0.55f))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("انتهى الوقت!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
            Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.Purple)
            Text("${score.arabicDigits()} نقطة", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9F1C))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Purple, contentColor = Color.White),
            ) { Text(next, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun VsScreen(name1: String, score1: Int, name2: String, onGo: () -> Unit) = Rtl {
    var count by remember { mutableIntStateOf(3) }
    LaunchedEffect(Unit) {
        delay(1200)
        for (i in 3 downTo 1) {
            count = i
            delay(900)
        }
        count = 0
        delay(700)
        onGo()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF9B7BFF), Color(0xFF4B22C4))))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PlayerChip(name1, "${score1.arabicDigits()} نقطة")
            Box(
                Modifier
                    .size(110.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFFFE066), Color(0xFFFF9F1C)))),
                contentAlignment = Alignment.Center,
            ) { Text("VS", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
            PlayerChip(name2, "دوره الآن")
            Text(
                if (count > 0) count.arabicDigits() else "انطلق!",
                fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            )
        }
    }
}

@Composable
private fun PlayerChip(name: String, sub: String) {
    Column(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
        Text(sub, fontSize = 15.sp, color = Palette.Muted)
    }
}

@Composable
private fun ResultScreen(names: List<String>, scores: List<Int>, onAgain: () -> Unit, onCelebrate: () -> Unit) = Rtl {
    LaunchedEffect(Unit) { onCelebrate() }
    val tie = scores[0] == scores[1]
    val winner = if (scores[0] >= scores[1]) 0 else 1
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1F4B), Color(0xFF4B22C4)))),
    ) {
        Fireworks(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Canvas(Modifier.size(96.dp)) { drawTrophy() }
            if (tie) {
                Text("تعادل!", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD93D))
                Text("أحسنتما كلاكما", fontSize = 20.sp, color = Color.White)
            } else {
                Text("الفائز", fontSize = 22.sp, color = Color.White)
                Text(
                    names[winner], fontSize = 46.sp, lineHeight = 54.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD93D), textAlign = TextAlign.Center,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (i in 0..1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            names[i], fontSize = 19.sp, fontWeight = if (!tie && i == winner) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = Palette.Ink, modifier = Modifier.weight(1f),
                        )
                        Text("${scores[i].arabicDigits()} نقطة", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.Purple)
                    }
                }
            }
            Button(
                onClick = onAgain,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F1C), contentColor = Color.White),
            ) { Text("مباراة جديدة", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

private fun DrawScope.drawTrophy() {
    val w = size.width
    val gold = Brush.verticalGradient(listOf(Color(0xFFFFE066), Color(0xFFFFB020)))
    // Handles.
    drawCircle(Color(0xFFFFC93C), radius = w * 0.16f, center = Offset(w * 0.2f, w * 0.3f), style = Stroke(w * 0.06f))
    drawCircle(Color(0xFFFFC93C), radius = w * 0.16f, center = Offset(w * 0.8f, w * 0.3f), style = Stroke(w * 0.06f))
    // Cup.
    val cup = androidx.compose.ui.graphics.Path().apply {
        moveTo(w * 0.22f, w * 0.12f)
        lineTo(w * 0.78f, w * 0.12f)
        quadraticTo(w * 0.78f, w * 0.58f, w * 0.5f, w * 0.62f)
        quadraticTo(w * 0.22f, w * 0.58f, w * 0.22f, w * 0.12f)
        close()
    }
    drawPath(cup, gold)
    drawRect(Color(0xFFFFB020), topLeft = Offset(w * 0.45f, w * 0.6f), size = Size(w * 0.1f, w * 0.16f))
    drawRoundRect(Color(0xFFFFC93C), topLeft = Offset(w * 0.28f, w * 0.76f), size = Size(w * 0.44f, w * 0.12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f))
    drawCircle(Color.White.copy(alpha = 0.6f), radius = w * 0.05f, center = Offset(w * 0.36f, w * 0.26f))
}

private class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color) {
    var life = 0f
    var px = x
    var py = y
}

private class FireworksState {
    val sparks = mutableListOf<Spark>()
    var w = 0f
    var h = 0f
    var timer = 0f
}

/** Bursts of coloured sparks over the winner screen. */
@Composable
private fun Fireworks(modifier: Modifier) {
    val state = remember { FireworksState() }
    var frame by remember { mutableLongStateOf(0L) }
    val density = LocalDensity.current.density
    val colors = listOf(Color(0xFFFF4D4D), Color(0xFFFFD93D), Color(0xFF4D96FF), Color(0xFF6BCB77), Color(0xFFF15BB5), Color(0xFFFFFFFF), Color(0xFF2EC4B6))
    LaunchedEffect(Unit) {
        var last = 0L
        val rnd = Random.Default
        while (true) {
            withFrameNanos { now ->
                if (last != 0L && state.w > 0f) {
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                    state.timer -= dt
                    if (state.timer <= 0f) {
                        state.timer = 0.35f + rnd.nextFloat() * 0.45f
                        val cx = state.w * (0.15f + rnd.nextFloat() * 0.7f)
                        val cy = state.h * (0.12f + rnd.nextFloat() * 0.4f)
                        val c1 = colors[rnd.nextInt(colors.size)]
                        val c2 = colors[rnd.nextInt(colors.size)]
                        repeat(40) { i ->
                            val a = (i / 40f) * 2f * PI.toFloat()
                            val sp = (120f + rnd.nextFloat() * 160f) * density
                            state.sparks += Spark(cx, cy, cos(a) * sp, sin(a) * sp, if (i % 2 == 0) c1 else c2)
                        }
                    }
                    for (s in state.sparks) {
                        s.px = s.x
                        s.py = s.y
                        s.life += dt
                        s.vx *= 0.985f
                        s.vy = s.vy * 0.985f + 140f * density * dt
                        s.x += s.vx * dt
                        s.y += s.vy * dt
                    }
                    state.sparks.removeAll { it.life > 1.4f }
                }
                last = now
                frame = now
            }
        }
    }
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE")
        val t = frame
        state.w = size.width
        state.h = size.height
        for (s in state.sparks) {
            val a = (1f - s.life / 1.4f).coerceIn(0f, 1f)
            drawLine(s.color, Offset(s.px, s.py), Offset(s.x, s.y), strokeWidth = 3f * density, cap = StrokeCap.Round, alpha = a)
            drawCircle(s.color, radius = 2.6f * density * (0.5f + a), center = Offset(s.x, s.y), alpha = a)
        }
    }
}
