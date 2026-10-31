package com.mramadan.babybubbles.parent

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Arabic-Indic digits for parent-facing numbers. */
fun Int.arabicDigits(): String = toString().map { c -> if (c in '0'..'9') '٠' + (c - '0') else c }.joinToString("")

@Composable
fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onStart: () -> Unit) = Rtl {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F3FF))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("فقاعات", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
        Text("لعبة الطفل الأولى", fontSize = 18.sp, color = Color(0xFF6B7280))

        Spacer(Modifier.height(32.dp))
        Text("اختر الوضع", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("الألوان") })
            FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("الحيوانات (قريبًا)") })
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) { Text("ابدأ اللعب", fontSize = 22.sp) }

        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("للأب", fontWeight = FontWeight.Bold)
                Text("عند البدء ستظهر نافذة من النظام تطلب تثبيت التطبيق، وافق عليها ليُقفل الهاتف على اللعبة.")
                Text("للخروج: اضغط زري رفع الصوت وخفضه معًا ثلاث ثوانٍ، ثم أجب عن السؤال.")
                Text("لحماية أقوى: فعّل خيار طلب رمز القفل قبل إلغاء التثبيت من إعدادات الأمان في هاتفك.")
                Text("اضبط مستوى الصوت قبل البدء، فأزرار الصوت معطلة أثناء اللعب.")
            }
        }
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

    LaunchedEffect(Unit) {
        delay(10_000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            // Swallow every touch so the child cannot pop bubbles behind the gate.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.Center,
    ) {
        Card(Modifier.padding(32.dp)) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("للأب فقط", style = MaterialTheme.typography.titleMedium)
                Text(
                    "كم يساوي ${a.arabicDigits()} + ${b.arabicDigits()}؟",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (o in options) {
                        OutlinedButton(
                            onClick = { if (o == answer) onSuccess() else onDismiss() },
                            modifier = Modifier.size(64.dp),
                        ) { Text(o.arabicDigits(), fontSize = 20.sp) }
                    }
                }
            }
        }
    }
}
