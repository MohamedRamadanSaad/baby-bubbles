package com.mramadan.babybubbles.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mramadan.babybubbles.AzkarSlot
import com.mramadan.babybubbles.AzkarVolume
import com.mramadan.babybubbles.AzkarWhich
import com.mramadan.babybubbles.LivePrefs
import com.mramadan.babybubbles.NameVoice
import com.mramadan.babybubbles.SoundType
import com.mramadan.babybubbles.SpeakLang
import com.mramadan.babybubbles.game.BubbleColors
import com.mramadan.babybubbles.game.Catalog
import com.mramadan.babybubbles.game.GameMode
import com.mramadan.babybubbles.game.SpeedLevel
import com.mramadan.babybubbles.game.drawColorBubble
import com.mramadan.babybubbles.ui.Palette
import com.mramadan.babybubbles.voice.Parent
import com.mramadan.babybubbles.voice.VoiceStore
import kotlinx.coroutines.delay

private val ModeNames = mapOf(
    GameMode.COLORS to "الألوان والأذكار",
    GameMode.ANIMALS to "الحيوانات",
    GameMode.JUNGLE to "حيوانات الغابة",
    GameMode.VEHICLES to "المواصلات",
    GameMode.BIRDS to "الطيور",
    GameMode.FRUITS to "الفواكه",
    GameMode.SEA to "البحر",
    GameMode.FLAGS to "الأعلام",
)

/** What can be recorded in a category: its characters, or the praise phrases for colors. */
internal fun recordItems(mode: GameMode): List<Pair<String, Int>> =
    if (mode == GameMode.COLORS) Catalog.praise.map { it to 0 }
    else Catalog.forMode(mode).map { it.arabic to it.image }

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .selectable(selected = false, role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            // In a right-to-left screen, "back" points to the right.
            Canvas(Modifier.size(22.dp)) { iconPath("M9 5 L16 12 L9 19", Palette.Ink, strokeWidth = 2.6f) }
        }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.Muted)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            content = content,
        )
    }
}

@Composable
private fun PurpleSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Palette.Purple,
            uncheckedThumbColor = Palette.Muted,
            uncheckedTrackColor = Palette.Track,
            uncheckedBorderColor = Palette.CardBorder,
        ),
    )
}

@Composable
fun SettingsScreen(
    prefs: LivePrefs,
    slotStatus: (AzkarSlot) -> String?,
    recordingSlot: AzkarSlot?,
    previewSlot: AzkarSlot?,
    onPickAzkar: (AzkarSlot) -> Unit,
    onRecordAzkar: (AzkarSlot) -> Unit,
    onPreviewAzkar: (AzkarSlot) -> Unit,
    onRecordings: () -> Unit,
    onBack: () -> Unit,
) = Rtl {
    BackHandler(onBack = onBack)
    var showCredits by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Lavender)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader("الإعدادات", onBack)

        SectionCard("اللعب") {
            SettingRow("سرعة الفقاعات") {
                SegmentGroup {
                    Segment("بطيئة", prefs.speed == SpeedLevel.SLOW) { prefs.speed = SpeedLevel.SLOW }
                    Segment("متوسطة", prefs.speed == SpeedLevel.NORMAL) { prefs.speed = SpeedLevel.NORMAL }
                    Segment("سريعة", prefs.speed == SpeedLevel.FAST) { prefs.speed = SpeedLevel.FAST }
                }
            }
            HorizontalDivider(color = Palette.Divider)
            Text("أقصى عدد للفقاعات على الشاشة", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
            SegmentGroup {
                for (n in listOf(2, 3, 4, 6, 8, 10)) {
                    Segment(n.arabicDigits(), prefs.maxBubbles == n, minWidth = 42.dp) { prefs.maxBubbles = n }
                }
            }
            Text("عدد أقل يناسب الأطفال الأصغر سنًّا.", fontSize = 13.sp, color = Palette.Muted,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
            HorizontalDivider(color = Palette.Divider)
            SettingRow("الأصوات") {
                SegmentGroup {
                    Segment("فرقعات فقط", prefs.soundType == SoundType.POPS_ONLY) { prefs.soundType = SoundType.POPS_ONLY }
                    Segment("أصوات المكونات", prefs.soundType == SoundType.ITEM_SOUNDS) { prefs.soundType = SoundType.ITEM_SOUNDS }
                }
            }
            HorizontalDivider(color = Palette.Divider)
            SettingRow("اهتزاز خفيف عند الفرقعة") { PurpleSwitch(prefs.vibrate) { prefs.vibrate = it } }
        }

        val namesOn = prefs.soundType == SoundType.ITEM_SOUNDS
        SectionCard("نطق الأسماء والأذكار") {
            Column(Modifier.alpha(if (namesOn) 1f else 0.45f)) {
                Text("الصوت الذي ينطق", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                SegmentGroup {
                    Segment("الجهاز", prefs.nameVoice == NameVoice.DEVICE) { prefs.nameVoice = NameVoice.DEVICE }
                    Segment("بابا", prefs.nameVoice == NameVoice.DAD) { prefs.nameVoice = NameVoice.DAD }
                    Segment("ماما", prefs.nameVoice == NameVoice.MOM) { prefs.nameVoice = NameVoice.MOM }
                    Segment("الاثنان", prefs.nameVoice == NameVoice.BOTH) { prefs.nameVoice = NameVoice.BOTH }
                }
                Text(
                    when (prefs.nameVoice) {
                        NameVoice.DEVICE -> "يقرأ الهاتف الأسماء بصوته."
                        NameVoice.DAD -> "يُسمع صوت بابا المسجل، وإن لم يُسجَّل شيء يُستخدم صوت ماما ثم صوت الجهاز."
                        NameVoice.MOM -> "يُسمع صوت ماما المسجل، وإن لم يُسجَّل شيء يُستخدم صوت بابا ثم صوت الجهاز."
                        NameVoice.BOTH -> "بابا وماما بالتناوب: كل فقاعة بصوت واحد منهما."
                    },
                    fontSize = 13.sp, lineHeight = 19.sp, color = Palette.Muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                HorizontalDivider(color = Palette.Divider, modifier = Modifier.padding(vertical = 6.dp))
                SettingRow("لغة صوت الجهاز") {
                    SegmentGroup {
                        Segment("عربي", prefs.speakLang == SpeakLang.AR) { prefs.speakLang = SpeakLang.AR }
                        Segment("English", prefs.speakLang == SpeakLang.EN) { prefs.speakLang = SpeakLang.EN }
                    }
                }
            }
            Button(
                onClick = onRecordings,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Purple, contentColor = Color.White),
            ) {
                Canvas(Modifier.size(20.dp)) { micIcon(Color.White) }
                Spacer(Modifier.width(10.dp))
                Text("تسجيل صوت بابا وماما", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (!namesOn) {
                Text("النطق متوقف لأن خيار «فرقعات فقط» مفعّل.", fontSize = 13.sp, color = Palette.Muted,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
        }

        SectionCard("الأذكار في الخلفية") {
            SettingRow("تشغيل الأذكار أثناء اللعب") { PurpleSwitch(prefs.azkarOn) { prefs.azkarOn = it } }
            HorizontalDivider(color = Palette.Divider)
            SettingRow("أي أذكار") {
                SegmentGroup {
                    Segment("الصباح", prefs.azkarWhich == AzkarWhich.MORNING) { prefs.azkarWhich = AzkarWhich.MORNING }
                    Segment("المساء", prefs.azkarWhich == AzkarWhich.EVENING) { prefs.azkarWhich = AzkarWhich.EVENING }
                    Segment("حسب الوقت", prefs.azkarWhich == AzkarWhich.AUTO) { prefs.azkarWhich = AzkarWhich.AUTO }
                }
            }
            if (prefs.azkarWhich == AzkarWhich.AUTO) {
                Text("الصباح حتى الثالثة عصرًا، والمساء بعدها.", fontSize = 13.sp, color = Palette.Muted,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            for (slot in AzkarSlot.entries) {
                HorizontalDivider(color = Palette.Divider)
                AzkarSlotRow(
                    title = slot.arabic,
                    status = slotStatus(slot),
                    recording = recordingSlot == slot,
                    previewing = previewSlot == slot,
                    canPreview = slotStatus(slot) != null,
                    busy = recordingSlot != null && recordingSlot != slot,
                    onPick = { onPickAzkar(slot) },
                    onRecord = { onRecordAzkar(slot) },
                    onPreview = { onPreviewAzkar(slot) },
                )
            }
            HorizontalDivider(color = Palette.Divider)
            SettingRow("مستوى صوت الأذكار") {
                SegmentGroup {
                    Segment("منخفض", prefs.azkarVolume == AzkarVolume.LOW) { prefs.azkarVolume = AzkarVolume.LOW }
                    Segment("متوسط", prefs.azkarVolume == AzkarVolume.MEDIUM) { prefs.azkarVolume = AzkarVolume.MEDIUM }
                    Segment("عالٍ", prefs.azkarVolume == AzkarVolume.HIGH) { prefs.azkarVolume = AzkarVolume.HIGH }
                }
            }
            Text(
                "لكل من الصباح والمساء: سجّلها بصوتك، أو اختر ملفًا صوتيًا محفوظًا على هاتفك. تعمل بهدوء خلف اللعب وتتكرر، وتتوقف أثناء رنين المكالمة.",
                fontSize = 13.sp, lineHeight = 19.sp, color = Palette.Muted,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
        }

        TextButton(onClick = { showCredits = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("حقوق الأصوات والصور", fontSize = 14.sp, color = Palette.Muted)
        }
    }
    if (showCredits) CreditsDialog { showCredits = false }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.micIcon(color: Color) {
    iconPath("M12 3 a3 3 0 0 0 -3 3 v6 a3 3 0 0 0 6 0 V6 a3 3 0 0 0 -3 -3 Z", color, strokeWidth = null)
    iconPath("M5 11 a7 7 0 0 0 14 0 M12 18 V21 M8 21 H16", color, strokeWidth = 2f)
}

@Composable
fun RecordHomeScreen(
    store: VoiceStore,
    parent: Parent,
    onParentChange: (Parent) -> Unit,
    onOpen: (GameMode) -> Unit,
    onBack: () -> Unit,
) = Rtl {
    BackHandler(onBack = onBack)
    val cards = GameMode.entries
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Lavender)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("تسجيل الأصوات", onBack)
        Text("اختر من سيسجّل، ثم افتح قسمًا وسجّل اسم كل شيء بصوتك.", fontSize = 14.sp, color = Palette.Muted)
        SegmentGroup {
            Segment("صوت بابا", parent == Parent.DAD) { onParentChange(Parent.DAD) }
            Segment("صوت ماما", parent == Parent.MOM) { onParentChange(Parent.MOM) }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            itemsIndexed(cards) { _, mode ->
                val items = recordItems(mode)
                val done = store.count(parent, mode, items.size)
                val firstImage = items.firstOrNull { it.second != 0 }?.second
                Column(
                    Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .border(2.dp, if (done == items.size) Color(0xFF6BCB77) else Palette.CardBorder, RoundedCornerShape(22.dp))
                        .selectable(selected = false, role = Role.Button) { onOpen(mode) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        if (firstImage != null) {
                            Image(painterResource(firstImage), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(64.dp))
                        } else {
                            Canvas(Modifier.size(64.dp)) {
                                drawColorBubble(Offset(size.width * 0.35f, size.height * 0.6f), size.width * 0.28f, BubbleColors[0])
                                drawColorBubble(Offset(size.width * 0.68f, size.height * 0.38f), size.width * 0.24f, BubbleColors[2])
                            }
                        }
                    }
                    Text(ModeNames[mode] ?: "", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                    Text("سُجّل ${done.arabicDigits()} من ${items.size.arabicDigits()}", fontSize = 13.sp, color = Palette.Muted)
                }
            }
        }
    }
}

@Composable
fun RecordItemsScreen(
    mode: GameMode,
    parent: Parent,
    store: VoiceStore,
    isRecording: () -> Boolean,
    onStartRecording: (Int) -> Boolean,
    onStopRecording: () -> Unit,
    onPlay: (Int) -> Unit,
    onBack: () -> Unit,
) = Rtl {
    var recordingIndex by remember { mutableStateOf<Int?>(null) }
    var version by remember { mutableIntStateOf(0) }
    val leave = {
        if (recordingIndex != null) onStopRecording()
        recordingIndex = null
        onBack()
    }
    BackHandler(onBack = leave)

    // Names are short: stop automatically after five seconds.
    LaunchedEffect(recordingIndex) {
        if (recordingIndex != null) {
            delay(5_000)
            if (isRecording()) onStopRecording()
            recordingIndex = null
            version++
        }
    }

    val items = recordItems(mode)
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Lavender)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("${ModeNames[mode]} — صوت ${parent.arabic}", leave)
        Text("اضغط زر التسجيل وقل الاسم المكتوب فوق الصورة، ثم اضغط مرة أخرى للإيقاف.", fontSize = 14.sp, lineHeight = 20.sp, color = Palette.Muted)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            itemsIndexed(items) { index, (name, image) ->
                // Reading [version] re-checks the files after a recording changes.
                val recorded = version >= 0 && store.has(parent, mode, index)
                val recordingThis = recordingIndex == index
                Column(
                    Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .border(
                            2.dp,
                            when {
                                recordingThis -> Color(0xFFFF4D4D)
                                recorded -> Color(0xFF6BCB77)
                                else -> Palette.CardBorder
                            },
                            RoundedCornerShape(22.dp),
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, textAlign = TextAlign.Center)
                    Box(Modifier.fillMaxWidth().aspectRatio(1.3f), contentAlignment = Alignment.Center) {
                        if (image != 0) {
                            Image(painterResource(image), contentDescription = name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        } else {
                            Canvas(Modifier.size(80.dp)) {
                                drawColorBubble(center, size.width * 0.42f, BubbleColors[(index * 3) % BubbleColors.size])
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RoundButton(
                            background = if (recordingThis) Color(0xFFFF4D4D) else Palette.Purple,
                            label = if (recordingThis) "إيقاف التسجيل" else "تسجيل",
                            enabled = recordingIndex == null || recordingThis,
                            onClick = {
                                if (recordingThis) {
                                    onStopRecording()
                                    recordingIndex = null
                                    version++
                                } else if (onStartRecording(index)) {
                                    recordingIndex = index
                                }
                            },
                        ) {
                            if (recordingThis) {
                                drawRoundRect(Color.White, topLeft = Offset(size.width * 0.28f, size.height * 0.28f),
                                    size = androidx.compose.ui.geometry.Size(size.width * 0.44f, size.height * 0.44f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
                            } else {
                                micIcon(Color.White)
                            }
                        }
                        RoundButton(
                            background = Color(0xFF2E9E4F),
                            label = "استماع",
                            enabled = recorded && recordingIndex == null,
                            onClick = { onPlay(index) },
                        ) { iconPath("M8 5 L19 12 L8 19 Z", Color.White, strokeWidth = null) }
                        RoundButton(
                            background = Palette.Track,
                            label = "حذف",
                            enabled = recorded && recordingIndex == null,
                            onClick = {
                                store.delete(parent, mode, index)
                                version++
                            },
                        ) {
                            iconPath("M5 7 H19 M10 11 V17 M14 11 V17 M6 7 L7 20 H17 L18 7 M9 7 V4 H15 V7", Palette.Muted, strokeWidth = 2f)
                        }
                    }
                    Text(
                        when {
                            recordingThis -> "جارٍ التسجيل…"
                            recorded -> "تم التسجيل"
                            else -> "لم يُسجَّل"
                        },
                        fontSize = 12.sp,
                        color = when {
                            recordingThis -> Color(0xFFD93636)
                            recorded -> Color(0xFF2E9E4F)
                            else -> Palette.Muted
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundButton(
    background: Color,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    Box(
        Modifier
            .size(46.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(background)
            .selectable(selected = false, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp), onDraw = icon)
    }
}


@Composable
private fun AzkarSlotRow(
    title: String,
    status: String?,
    recording: Boolean,
    previewing: Boolean,
    canPreview: Boolean,
    busy: Boolean,
    onPick: () -> Unit,
    onRecord: () -> Unit,
    onPreview: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
            Text(
                when {
                    recording -> "جارٍ التسجيل…"
                    status != null -> status
                    else -> "لم تُحدد بعد"
                },
                fontSize = 13.sp,
                color = when {
                    recording -> Color(0xFFD93636)
                    status != null -> Color(0xFF2E9E4F)
                    else -> Palette.Muted
                },
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction(if (recording) "إيقاف التسجيل" else "تسجيل بصوتي", enabled = !busy && !previewing,
                tint = if (recording) Color(0xFFFF4D4D) else Palette.Purple, onClick = onRecord)
            SmallAction("اختيار ملف", enabled = !recording && !busy, tint = Palette.Purple, onClick = onPick)
            SmallAction(if (previewing) "إيقاف" else "استماع", enabled = canPreview && !recording && !busy,
                tint = Color(0xFF2E9E4F), onClick = onPreview)
        }
    }
}

@Composable
private fun SmallAction(label: String, enabled: Boolean, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .selectable(selected = false, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}
