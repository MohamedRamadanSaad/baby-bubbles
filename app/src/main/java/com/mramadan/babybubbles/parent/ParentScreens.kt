package com.mramadan.babybubbles.parent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mramadan.babybubbles.R
import com.mramadan.babybubbles.SpeakLang
import com.mramadan.babybubbles.game.BubbleColors
import com.mramadan.babybubbles.game.GameMode
import com.mramadan.babybubbles.game.SpeedLevel
import com.mramadan.babybubbles.game.drawBubbleGloss
import com.mramadan.babybubbles.game.drawClearBubble
import com.mramadan.babybubbles.game.drawColorBubble
import com.mramadan.babybubbles.game.drawPainterCentered
import com.mramadan.babybubbles.ui.Palette
import org.json.JSONArray
import kotlin.random.Random

/** Arabic-Indic digits for parent-facing numbers. */
fun Int.arabicDigits(): String = toString().map { c -> if (c in '0'..'9') '٠' + (c - '0') else c }.joinToString("")

@Composable
fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}

private val Red = BubbleColors[0]
private val Yellow = BubbleColors[1]
private val Blue = BubbleColors[2]
private val Green = BubbleColors[3]
private val Violet = BubbleColors[5]
private val Teal = BubbleColors[6]

/** Draws a 24-unit icon path scaled to the canvas, as stroke or fill. */
internal fun DrawScope.iconPath(data: String, color: Color, strokeWidth: Float? = 2f, viewport: Float = 24f) {
    val path: Path = PathParser().parsePathString(data).toPath()
    val k = size.width / viewport
    scale(k, k, pivot = Offset.Zero) {
        drawPath(
            path,
            color,
            style = if (strokeWidth != null) {
                Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            } else {
                Fill
            },
        )
    }
}

private class ModeOption(val mode: GameMode, val label: String, val art: DrawScope.() -> Unit)

@Composable
fun HomeScreen(
    mode: GameMode,
    onModeChange: (GameMode) -> Unit,
    onSettings: () -> Unit,
    onInstructions: () -> Unit,
    onHunt: () -> Unit,
    onStart: () -> Unit,
) = Rtl {
    val cat = painterResource(R.drawable.animal_cat)
    val duck = painterResource(R.drawable.animal_duck)
    val tiger = painterResource(R.drawable.jungle_tiger)
    val monkey = painterResource(R.drawable.jungle_monkey)
    val car = painterResource(R.drawable.veh_car)
    val plane = painterResource(R.drawable.veh_airplane)
    val parrot = painterResource(R.drawable.bird_parrot)
    val apple = painterResource(R.drawable.fruit_apple)
    val banana = painterResource(R.drawable.fruit_banana)
    val fish = painterResource(R.drawable.sea_clownfish)
    val flagSa = painterResource(R.drawable.flag_sa)
    val flagEg = painterResource(R.drawable.flag_eg)

    val options = listOf(
        ModeOption(GameMode.COLORS, "الألوان") {
            drawColorBubble(Offset(28.dp.toPx(), 52.dp.toPx()), 22.dp.toPx(), Red)
            drawColorBubble(Offset(74.dp.toPx(), 56.dp.toPx()), 20.dp.toPx(), Blue)
            drawColorBubble(Offset(52.dp.toPx(), 22.dp.toPx()), 18.dp.toPx(), Green)
        },
        ModeOption(GameMode.ANIMALS, "الحيوانات") {
            inBubble(cat, Offset(30.dp.toPx(), 50.dp.toPx()), 28.dp.toPx())
            inBubble(duck, Offset(78.dp.toPx(), 30.dp.toPx()), 24.dp.toPx())
        },
        ModeOption(GameMode.JUNGLE, "الغابة") {
            drawRoundRect(Color(0xFFDFF3D6), cornerRadius = CornerRadius(16.dp.toPx()))
            inBubble(tiger, Offset(30.dp.toPx(), 50.dp.toPx()), 28.dp.toPx())
            inBubble(monkey, Offset(78.dp.toPx(), 30.dp.toPx()), 24.dp.toPx())
        },
        ModeOption(GameMode.VEHICLES, "المواصلات") {
            inBubble(car, Offset(30.dp.toPx(), 50.dp.toPx()), 28.dp.toPx())
            inBubble(plane, Offset(78.dp.toPx(), 30.dp.toPx()), 24.dp.toPx())
        },
        ModeOption(GameMode.BIRDS, "الطيور") {
            inBubble(parrot, Offset(52.dp.toPx(), 42.dp.toPx()), 34.dp.toPx())
        },
        ModeOption(GameMode.FRUITS, "الفواكه") {
            inBubble(apple, Offset(30.dp.toPx(), 50.dp.toPx()), 28.dp.toPx())
            inBubble(banana, Offset(78.dp.toPx(), 30.dp.toPx()), 24.dp.toPx())
        },
        ModeOption(GameMode.SEA, "البحر") {
            drawRoundRect(Color(0xFF4FB8F0), cornerRadius = CornerRadius(16.dp.toPx()))
            inBubble(fish, Offset(52.dp.toPx(), 42.dp.toPx()), 32.dp.toPx())
        },
        ModeOption(GameMode.FLAGS, "الأعلام") {
            flagCard(flagSa, Offset(34.dp.toPx(), 48.dp.toPx()), 52.dp.toPx())
            flagCard(flagEg, Offset(74.dp.toPx(), 30.dp.toPx()), 42.dp.toPx())
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Lavender)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AppLogo(Modifier.size(88.dp))
                Text("فقاعات", fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Purple)
                Text("لعب آمن لأصغر الأيدي", fontSize = 16.sp, color = Palette.Muted)
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .selectable(selected = false, role = Role.Button, onClick = onSettings),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(26.dp)) { gearIcon(Palette.Purple) }
            }
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .selectable(selected = false, role = Role.Button, onClick = onInstructions)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(Palette.Purple),
                    contentAlignment = Alignment.Center,
                ) { Text("؟", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                Text("تعليمات", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.Purple)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("اختر الوضع", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
            options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { o -> ModeCard(o.label, mode == o.mode, { onModeChange(o.mode) }, o.art) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        HuntCard(onHunt)

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(66.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Purple, contentColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
        ) {
            Canvas(Modifier.size(22.dp)) {
                iconPath("M8 5 L19 12 L8 19 Z", Color.White, strokeWidth = null)
            }
            Spacer(Modifier.width(12.dp))
            Text("ابدأ اللعب", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.TipBackground)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(24.dp)) {
                    iconPath("M4 9.5 H8 L13 5.5 V18.5 L8 14.5 H4 Z", Palette.TipIcon)
                    iconPath("M16.5 9 Q19 12 16.5 15 M19 6.5 Q23.5 12 19 17.5", Palette.TipIcon)
                }
            }
            Text(
                "للخروج أثناء اللعب: اضغط زر رفع الصوت ثانيتين ثم أجب عن السؤال. وعند ورود مكالمة يظهر السؤال فورًا.",
                fontSize = 14.5.sp,
                lineHeight = 22.sp,
                color = Palette.TipInk,
            )
        }

    }
}

/** A simple cog: ring plus eight teeth. */
internal fun DrawScope.gearIcon(color: Color) {
    val c = center
    val r = size.minDimension / 2f
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val dir = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
        drawLine(color, c + dir * (r * 0.55f), c + dir * (r * 0.95f), strokeWidth = r * 0.32f, cap = StrokeCap.Round)
    }
    drawCircle(color, radius = r * 0.62f, center = c)
    drawCircle(Color.White, radius = r * 0.26f, center = c)
}

internal fun DrawScope.inBubble(painter: Painter, center: Offset, r: Float) {
    drawClearBubble(center, r, strokeWidth = 2.dp.toPx())
    drawPainterCentered(painter, center, r * 1.5f)
    drawBubbleGloss(center, r)
}

internal fun DrawScope.flagCard(painter: Painter, center: Offset, width: Float) {
    val h = width * 0.75f
    translate(center.x - width / 2f, center.y - h / 2f) {
        with(painter) { draw(Size(width, h)) }
        drawRoundRect(Color.White, size = Size(width, h), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
}

@Composable
internal fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink)
        control()
    }
}

@Composable
internal fun SegmentGroup(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Palette.Track)
            .padding(3.dp),
        content = content,
    )
}

@Composable
private fun RowScope.ModeCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    art: DrawScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .weight(1f)
            .height(138.dp)
            .then(
                if (selected) Modifier.shadow(10.dp, shape, ambientColor = Palette.Purple, spotColor = Palette.Purple)
                else Modifier
            )
            .clip(shape)
            .background(Color.White)
            .border(if (selected) 3.dp else 2.dp, if (selected) Palette.Purple else Palette.CardBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(10.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Canvas(Modifier.size(104.dp, 84.dp), onDraw = art)
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Palette.Purple),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(15.dp)) {
                    iconPath("M5 12.5 L10 17 L19 7.5", Color.White, strokeWidth = 3f)
                }
            }
        }
    }
}

@Composable
internal fun Segment(label: String, selected: Boolean, minWidth: androidx.compose.ui.unit.Dp = 52.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minWidth = minWidth)
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) Palette.Purple else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) Color.White else Palette.Ink,
        )
    }
}

@Composable
internal fun CreditsDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val lines = remember {
        val sounds = runCatching {
            val json = context.assets.open("sound_credits.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                "${o.optString("title").removePrefix("File:")} — ${o.optString("artist")} — ${o.optString("license")}"
            }
        }.getOrDefault(emptyList())
        listOf(
            "الرسوم: رسوم أصلية لهذه اللعبة.",
            "الأعلام: flag-icons, Panayiotis Lipiridis — MIT License",
            "الخط: Baloo Bhaijaan 2 — SIL Open Font License",
            "الأصوات (ويكيميديا كومنز وبيكساباي):",
        ) + sounds
    }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("إغلاق") } },
        title = { Text("حقوق الأصوات والصور", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(lines) { Text(it, fontSize = 13.sp, lineHeight = 19.sp, color = Palette.Ink) }
            }
        },
    )
}

/** Why the parent question is on screen. */
enum class GateReason {
    /** The parent held Volume Up. */
    PARENT,

    /** The phone is ringing: the child cannot reach the call until the parent answers. */
    CALL,

    /** Something outside the game took the screen (a notification, another app, the screen turning off). */
    OUTSIDE,
}

private const val PHONE_ICON =
    "M6.6 10.8 C8 13.6 10.4 16 13.2 17.4 L15.4 15.2 C15.7 14.9 16.1 14.8 16.4 15 C17.5 15.4 18.7 15.6 20 15.6 C20.6 15.6 21 16 21 16.6 V20 C21 20.6 20.6 21 20 21 C10.6 21 3 13.4 3 4 C3 3.4 3.4 3 4 3 H7.5 C8.1 3 8.5 3.4 8.5 4 C8.5 5.3 8.7 6.5 9.1 7.6 C9.2 7.9 9.1 8.3 8.9 8.6 Z"

private data class SumQuestion(val a: Int, val b: Int, val options: List<Int>) {
    val answer get() = a + b
}

private fun newQuestion(): SumQuestion {
    // The sum is always below ten, and so are all the choices.
    val a = Random.nextInt(1, 8)
    val b = Random.nextInt(1, 10 - a)
    val options = mutableSetOf(a + b)
    while (options.size < 4) options += Random.nextInt(2, 10)
    return SumQuestion(a, b, options.shuffled())
}

/**
 * The parent question. For a held key or anything outside the game it closes by itself after
 * 10 s and a wrong answer returns to the game. For a call it stays while the phone rings, and a
 * wrong answer only brings a new question after a short pause, so random taps cannot get through.
 */
@Composable
fun ParentGate(reason: GateReason, onSuccess: () -> Unit, onDismiss: () -> Unit) = Rtl {
    var question by remember { mutableStateOf(newQuestion()) }
    var locked by remember { mutableStateOf(false) }
    val remaining = remember { Animatable(1f) }
    val timed = reason != GateReason.CALL

    if (timed) {
        LaunchedEffect(Unit) {
            remaining.animateTo(0f, tween(durationMillis = 10_000, easing = LinearEasing))
            onDismiss()
        }
    }
    LaunchedEffect(locked) {
        if (locked) {
            kotlinx.coroutines.delay(2_000)
            question = newQuestion()
            locked = false
        }
    }
    val pulse by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 1f,
        targetValue = if (reason == GateReason.CALL) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Scrim.copy(alpha = 0.62f))
            // Swallow every touch so the child cannot pop bubbles behind the gate.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (reason == GateReason.CALL) {
                Box(
                    Modifier
                        .size(64.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(Color(0xFFE5F4E4)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(32.dp)) { iconPath(PHONE_ICON, Color(0xFF2E9E4F), strokeWidth = null) }
                }
                Text("مكالمة واردة", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Text(
                    "أجب عن السؤال لتخرج من اللعبة وترد على المكالمة.",
                    fontSize = 15.sp, lineHeight = 22.sp, color = Palette.Muted, textAlign = TextAlign.Center,
                )
            } else {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Palette.Track),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(30.dp)) {
                        val k = size.width / 24f
                        scale(k, k, pivot = Offset.Zero) {
                            drawRoundRect(
                                Palette.Purple,
                                topLeft = Offset(5f, 10.5f),
                                size = Size(14f, 10f),
                                cornerRadius = CornerRadius(2.5f),
                                style = Stroke(2f),
                            )
                            drawCircle(Palette.Purple, radius = 1.6f, center = Offset(12f, 15.5f))
                        }
                        iconPath("M8 10.5 V8 A4 4 0 0 1 16 8 V10.5", Palette.Purple)
                    }
                }
                Text(
                    if (reason == GateReason.OUTSIDE) "حدث شيء خارج اللعبة — للأب فقط" else "للأب فقط",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.Muted, textAlign = TextAlign.Center,
                )
            }
            Text(
                if (locked) "إجابة خاطئة… سؤال جديد بعد لحظة"
                else "كم يساوي ${question.a.arabicDigits()} + ${question.b.arabicDigits()}؟",
                fontSize = if (locked) 20.sp else 34.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Palette.Ink,
                textAlign = TextAlign.Center,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (o in question.options) {
                    OutlinedButton(
                        onClick = {
                            when {
                                locked -> Unit
                                o == question.answer -> onSuccess()
                                timed -> onDismiss()
                                else -> locked = true
                            }
                        },
                        enabled = !locked,
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(2.dp, Palette.AnswerBorder),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(o.arabicDigits(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                    }
                }
            }
            if (timed) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Palette.ProgressTrack)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(remaining.value)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Palette.Purple)
                        )
                    }
                    Text(
                        "تعود اللعبة تلقائيًا خلال ١٠ ثوانٍ، والإجابة الخاطئة تعيدها فورًا",
                        fontSize = 13.5.sp,
                        color = Palette.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** The main points a parent should know, shown in the opening notice and the instructions. */
private val ParentPoints = listOf(
    "للخروج من اللعبة: اضغط زر رفع الصوت ثانيتين، ثم أجب عن سؤال الجمع.",
    "عند ورود مكالمة يظهر السؤال فورًا، ولا يستطيع الطفل الرد أو الخروج قبل الإجابة. بعد الإجابة تظهر لك شاشة المكالمة باسم المتصل كالمعتاد.",
    "أي شيء يحدث خارج اللعبة، مثل إشعار أو إطفاء الشاشة، يُظهر السؤال أيضًا.",
    "تنخفض إضاءة الشاشة أثناء اللعب إلى الحد المضبوط في الإعدادات (النصف تلقائيًا) إذا كانت أعلى منه، وتعود كما كانت بعد الخروج.",
    "لا توجد إعلانات، ولا اتصال بالإنترنت، ولا نجمع أي بيانات.",
)

@Composable
private fun PointsList(points: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        points.forEach { p ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Palette.Purple)
                )
                Text(p, fontSize = 15.sp, lineHeight = 23.sp, color = Palette.Ink)
            }
        }
    }
}

/** Shown when the app opens, before the parent hands the phone to the child. */
@Composable
fun IntroDialog(onClose: (hideNextTime: Boolean) -> Unit) = Rtl {
    var hide by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onClose(hide) },
        title = { Text("تنبيه للوالدين", fontWeight = FontWeight.ExtraBold, color = Palette.Purple) },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PointsList(ParentPoints.take(3))
                Text(
                    "سنطلب إذن «الهاتف» مرة واحدة، فقط لنعرف أن الهاتف يرن فنُظهر سؤال الأب. لا نقرأ رقم المتصل ولا جهات الاتصال.",
                    fontSize = 14.sp, lineHeight = 21.sp, color = Palette.Muted,
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(selected = hide, role = Role.Checkbox, onClick = { hide = !hide })
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.material3.Checkbox(checked = hide, onCheckedChange = null)
                    Text("لا تُظهر هذا التنبيه مرة أخرى", fontSize = 14.sp, color = Palette.Ink)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onClose(hide) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Purple, contentColor = Color.White),
            ) { Text("فهمت", fontWeight = FontWeight.Bold) }
        },
    )
}

/** The instructions button on the home screen opens this. */
@Composable
fun InstructionsDialog(onClose: () -> Unit) = Rtl {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("التعليمات", fontWeight = FontWeight.ExtraBold, color = Palette.Purple) },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PointsList(ParentPoints)
                HorizontalDivider(color = Palette.Divider)
                PointsList(
                    listOf(
                        "اختر قسمًا ثم اضغط «ابدأ اللعب». كل فقاعة يفرقعها الطفل يخرج منها شيء ويُسمع صوته واسمه.",
                        "صيد الطيور: لعبة لاعبَين بالمنظار وزر «ضرب»، والفائز تظهر له الألعاب النارية.",
                        "من الإعدادات: سرعة الفقاعات، وعددها، والإضاءة، ولغة الأسماء، وتسجيل صوت بابا وماما، والأذكار أثناء اللعب.",
                    )
                )
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("إغلاق", fontWeight = FontWeight.Bold) } },
    )
}


/** The app icon: purple tile with the smiling bubble. */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Box(
        modifier
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Palette.Purple, spotColor = Palette.Purple)
            .clip(RoundedCornerShape(24.dp))
    ) {
        androidx.compose.foundation.Image(
            painterResource(R.drawable.ic_launcher_background), contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        )
        androidx.compose.foundation.Image(
            painterResource(R.drawable.ic_launcher_foreground), contentDescription = "فقاعات",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(1.35f),
        )
    }
}


/** Entry to the two-player bird hunt, below the bubble modes. */
@Composable
private fun HuntCard(onClick: () -> Unit) {
    val bird = painterResource(R.drawable.bird_parrot)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFFFE9C7), Color(0xFFFFD1D1))))
            .border(2.dp, Color(0xFFFFB38A), RoundedCornerShape(22.dp))
            .selectable(selected = false, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Canvas(Modifier.size(76.dp)) {
            drawPainterCentered(bird, Offset(size.width * 0.52f, size.height * 0.5f), size.width * 0.85f)
            val c = Offset(size.width * 0.58f, size.height * 0.42f)
            val r = size.width * 0.2f
            drawCircle(Color(0xFFFF3B3B), radius = r, center = c, style = Stroke(3.dp.toPx()))
            drawCircle(Color(0xFFFF3B3B), radius = 3.dp.toPx(), center = c)
            for (k in 0 until 4) {
                val a = k * Math.PI.toFloat() / 2f
                val dir = Offset(kotlin.math.cos(a), kotlin.math.sin(a))
                drawLine(Color(0xFFFF3B3B), c + dir * (r * 0.5f), c + dir * (r * 1.45f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Column(Modifier.weight(1f)) {
            Text("صيد الطيور", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
            Text("لعبة لاعبَين: من يجمع نقاطًا أكثر يفوز", fontSize = 14.sp, color = Palette.Muted)
        }
    }
}
