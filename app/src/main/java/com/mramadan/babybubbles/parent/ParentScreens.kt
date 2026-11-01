package com.mramadan.babybubbles.parent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.vectorResource
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
import com.mramadan.babybubbles.game.drawBubbleGloss
import com.mramadan.babybubbles.game.drawClearBubble
import com.mramadan.babybubbles.game.drawColorBubble
import com.mramadan.babybubbles.game.drawPainterCentered
import com.mramadan.babybubbles.ui.Palette
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
private fun DrawScope.iconPath(data: String, color: Color, strokeWidth: Float? = 2f, viewport: Float = 24f) {
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

@Composable
fun HomeScreen(
    mode: GameMode,
    onModeChange: (GameMode) -> Unit,
    speakLang: SpeakLang,
    onSpeakLangChange: (SpeakLang) -> Unit,
    vibrate: Boolean,
    onVibrateChange: (Boolean) -> Unit,
    onStart: () -> Unit,
) = Rtl {
    val cat = rememberVectorPainter(ImageVector.vectorResource(R.drawable.animal_cat))
    val duck = rememberVectorPainter(ImageVector.vectorResource(R.drawable.animal_duck))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Lavender)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(96.dp, 70.dp)) {
                drawColorBubble(Offset(26.dp.toPx(), 40.dp.toPx()), 22.dp.toPx(), Yellow, shadow = false)
                drawColorBubble(Offset(70.dp.toPx(), 44.dp.toPx()), 20.dp.toPx(), Teal, shadow = false)
                drawColorBubble(Offset(49.dp.toPx(), 25.dp.toPx()), 25.dp.toPx(), Violet, shadow = false)
            }
            Text(
                "فقاعات",
                fontSize = 46.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Palette.Purple,
            )
            Text("لعب آمن لأصغر الأيدي", fontSize = 16.sp, color = Palette.Muted)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("اختر الوضع", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeCard("الألوان", mode == GameMode.COLORS, { onModeChange(GameMode.COLORS) }) {
                    drawColorBubble(Offset(32.dp.toPx(), 60.dp.toPx()), 26.dp.toPx(), Red)
                    drawColorBubble(Offset(83.dp.toPx(), 65.dp.toPx()), 23.dp.toPx(), Blue)
                    drawColorBubble(Offset(58.dp.toPx(), 26.dp.toPx()), 22.dp.toPx(), Green)
                }
                ModeCard("الحيوانات", mode == GameMode.ANIMALS, { onModeChange(GameMode.ANIMALS) }) {
                    animalInBubble(cat, Offset(33.dp.toPx(), 59.dp.toPx()), 33.dp.toPx())
                    animalInBubble(duck, Offset(90.dp.toPx(), 32.dp.toPx()), 28.dp.toPx())
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("نطق أسماء الحيوانات", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink)
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Palette.Track)
                        .padding(3.dp)
                ) {
                    Segment("عربي", speakLang == SpeakLang.AR) { onSpeakLangChange(SpeakLang.AR) }
                    Segment("English", speakLang == SpeakLang.EN) { onSpeakLangChange(SpeakLang.EN) }
                }
            }
            HorizontalDivider(color = Palette.Divider)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("اهتزاز خفيف عند الفرقعة", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink)
                Switch(
                    checked = vibrate,
                    onCheckedChange = onVibrateChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Palette.Purple,
                        uncheckedThumbColor = Palette.Muted,
                        uncheckedTrackColor = Palette.Track,
                        uncheckedBorderColor = Palette.CardBorder,
                    ),
                )
            }
        }

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
                "للخروج أثناء اللعب: اضغط زري رفع الصوت وخفضه معًا ثلاث ثوانٍ.",
                fontSize = 14.5.sp,
                lineHeight = 22.sp,
                color = Palette.TipInk,
            )
        }
    }
}

private fun DrawScope.animalInBubble(painter: Painter, center: Offset, r: Float) {
    drawClearBubble(center, r, strokeWidth = 2.dp.toPx())
    drawPainterCentered(painter, center, r * 1.5f)
    drawBubbleGloss(center, r)
}

@Composable
private fun RowScope.ModeCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    art: DrawScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .weight(1f)
            .height(176.dp)
            .then(
                if (selected) Modifier.shadow(10.dp, shape, ambientColor = Palette.Purple, spotColor = Palette.Purple)
                else Modifier
            )
            .clip(shape)
            .background(Color.White)
            .border(if (selected) 3.dp else 2.dp, if (selected) Palette.Purple else Palette.CardBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Canvas(Modifier.size(120.dp, 96.dp), onDraw = art)
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Palette.Purple),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    iconPath("M5 12.5 L10 17 L19 7.5", Color.White, strokeWidth = 3f)
                }
            }
        }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 58.dp)
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) Palette.Purple else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) Color.White else Palette.Ink,
        )
    }
}

/** Shown after the volume-key hold. Wrong answer or 10 s of silence returns to the game. */
@Composable
fun ParentGate(onSuccess: () -> Unit, onDismiss: () -> Unit) = Rtl {
    val question = remember {
        val a = Random.nextInt(2, 8)
        val b = Random.nextInt(2, 8)
        val answer = a + b
        val options = mutableSetOf(answer)
        while (options.size < 4) options += Random.nextInt(4, 16)
        Triple(a, b, options.shuffled() to answer)
    }
    val (a, b, rest) = question
    val (options, answer) = rest
    val remaining = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        remaining.animateTo(0f, tween(durationMillis = 10_000, easing = LinearEasing))
        onDismiss()
    }

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
            Text("للأب فقط", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.Muted)
            Text(
                "كم يساوي ${a.arabicDigits()} + ${b.arabicDigits()}؟",
                fontSize = 34.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Palette.Ink,
                textAlign = TextAlign.Center,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (o in options) {
                    OutlinedButton(
                        onClick = { if (o == answer) onSuccess() else onDismiss() },
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(2.dp, Palette.AnswerBorder),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(o.arabicDigits(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                    }
                }
            }
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
