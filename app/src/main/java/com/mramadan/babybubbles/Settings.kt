package com.mramadan.babybubbles

import android.content.Context
import com.mramadan.babybubbles.game.GameMode
import com.mramadan.babybubbles.game.SpeedLevel
import java.util.Locale

enum class SpeakLang(val locale: Locale) {
    AR(Locale.forLanguageTag("ar")),
    EN(Locale.US),
}

/** What a pop sounds like: just the pop, or the thing's own sound and name. */
enum class SoundType { POPS_ONLY, ITEM_SOUNDS }

/** Whose voice says the names: the phone, or the parents' own recordings. */
enum class NameVoice { DEVICE, DAD, MOM, BOTH }

enum class AzkarVolume(val level: Float) { LOW(0.18f), MEDIUM(0.32f), HIGH(0.5f) }

/** Which azkar play behind the game; AUTO picks morning before 3 pm, evening after. */
enum class AzkarWhich { MORNING, EVENING, AUTO }

enum class AzkarSlot(val key: String, val arabic: String) { MORNING("morning", "أذكار الصباح"), EVENING("evening", "أذكار المساء") }

/** Parent choices, kept on the device only. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private inline fun <reified E : Enum<E>> enumPref(key: String, default: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, null) ?: default.name) }.getOrDefault(default)

    private fun put(key: String, value: String?) = prefs.edit().putString(key, value).apply()

    var mode: GameMode
        get() = enumPref("mode", GameMode.COLORS)
        set(value) = put("mode", value.name)

    var speakLang: SpeakLang
        get() = enumPref("speak", SpeakLang.AR)
        set(value) = put("speak", value.name)

    var vibrate: Boolean
        get() = prefs.getBoolean("vibrate", true)
        set(value) = prefs.edit().putBoolean("vibrate", value).apply()

    var speed: SpeedLevel
        get() = enumPref("speed", SpeedLevel.NORMAL)
        set(value) = put("speed", value.name)

    var soundType: SoundType
        get() = enumPref("soundType", SoundType.ITEM_SOUNDS)
        set(value) = put("soundType", value.name)

    var nameVoice: NameVoice
        get() = enumPref("nameVoice", NameVoice.DEVICE)
        set(value) = put("nameVoice", value.name)

    var azkarOn: Boolean
        get() = prefs.getBoolean("azkarOn", false)
        set(value) = prefs.edit().putBoolean("azkarOn", value).apply()

    var azkarUri: String?
        get() = prefs.getString("azkarUri", null)
        set(value) = put("azkarUri", value)

    var azkarName: String?
        get() = prefs.getString("azkarName", null)
        set(value) = put("azkarName", value)

    var azkarVolume: AzkarVolume
        get() = enumPref("azkarVolume", AzkarVolume.MEDIUM)
        set(value) = put("azkarVolume", value.name)

    /** Most bubbles on screen at once. */
    var maxBubbles: Int
        get() = prefs.getInt("maxBubbles", 6)
        set(value) = prefs.edit().putInt("maxBubbles", value).apply()

    var azkarWhich: AzkarWhich
        get() = enumPref("azkarWhich", AzkarWhich.AUTO)
        set(value) = put("azkarWhich", value.name)

    /** A picked file for a slot (an earlier single file counts as the morning one). */
    fun slotUri(slot: AzkarSlot): String? =
        prefs.getString("${slot.key}Uri", null) ?: if (slot == AzkarSlot.MORNING) prefs.getString("azkarUri", null) else null

    fun slotName(slot: AzkarSlot): String? =
        prefs.getString("${slot.key}Name", null) ?: if (slot == AzkarSlot.MORNING) prefs.getString("azkarName", null) else null

    fun setSlot(slot: AzkarSlot, uri: String?, name: String?) {
        prefs.edit().putString("${slot.key}Uri", uri).putString("${slot.key}Name", name)
            .apply { if (slot == AzkarSlot.MORNING) remove("azkarUri").remove("azkarName") }
            .apply()
    }
}

/** The same settings as Compose state, so screens update as the parent changes them. */
class LivePrefs(private val store: Settings) {
    private val _mode = androidx.compose.runtime.mutableStateOf(store.mode)
    var mode: GameMode
        get() = _mode.value
        set(v) { _mode.value = v; store.mode = v }

    private val _speed = androidx.compose.runtime.mutableStateOf(store.speed)
    var speed: SpeedLevel
        get() = _speed.value
        set(v) { _speed.value = v; store.speed = v }

    private val _speakLang = androidx.compose.runtime.mutableStateOf(store.speakLang)
    var speakLang: SpeakLang
        get() = _speakLang.value
        set(v) { _speakLang.value = v; store.speakLang = v }

    private val _vibrate = androidx.compose.runtime.mutableStateOf(store.vibrate)
    var vibrate: Boolean
        get() = _vibrate.value
        set(v) { _vibrate.value = v; store.vibrate = v }

    private val _soundType = androidx.compose.runtime.mutableStateOf(store.soundType)
    var soundType: SoundType
        get() = _soundType.value
        set(v) { _soundType.value = v; store.soundType = v }

    private val _nameVoice = androidx.compose.runtime.mutableStateOf(store.nameVoice)
    var nameVoice: NameVoice
        get() = _nameVoice.value
        set(v) { _nameVoice.value = v; store.nameVoice = v }

    private val _azkarOn = androidx.compose.runtime.mutableStateOf(store.azkarOn)
    var azkarOn: Boolean
        get() = _azkarOn.value
        set(v) { _azkarOn.value = v; store.azkarOn = v }

    private val _azkarUri = androidx.compose.runtime.mutableStateOf(store.azkarUri)
    var azkarUri: String?
        get() = _azkarUri.value
        set(v) { _azkarUri.value = v; store.azkarUri = v }

    private val _azkarName = androidx.compose.runtime.mutableStateOf(store.azkarName)
    var azkarName: String?
        get() = _azkarName.value
        set(v) { _azkarName.value = v; store.azkarName = v }

    private val _azkarVolume = androidx.compose.runtime.mutableStateOf(store.azkarVolume)
    var azkarVolume: AzkarVolume
        get() = _azkarVolume.value
        set(v) { _azkarVolume.value = v; store.azkarVolume = v }

    private val _maxBubbles = androidx.compose.runtime.mutableStateOf(store.maxBubbles)
    var maxBubbles: Int
        get() = _maxBubbles.value
        set(v) { _maxBubbles.value = v; store.maxBubbles = v }

    private val _azkarWhich = androidx.compose.runtime.mutableStateOf(store.azkarWhich)
    var azkarWhich: AzkarWhich
        get() = _azkarWhich.value
        set(v) { _azkarWhich.value = v; store.azkarWhich = v }

    private val slotUris = AzkarSlot.entries.associateWith { androidx.compose.runtime.mutableStateOf(store.slotUri(it)) }
    private val slotNames = AzkarSlot.entries.associateWith { androidx.compose.runtime.mutableStateOf(store.slotName(it)) }

    fun slotUri(slot: AzkarSlot): String? = slotUris.getValue(slot).value
    fun slotName(slot: AzkarSlot): String? = slotNames.getValue(slot).value

    fun setSlot(slot: AzkarSlot, uri: String?, name: String?) {
        slotUris.getValue(slot).value = uri
        slotNames.getValue(slot).value = name
        store.setSlot(slot, uri, name)
    }

    /** The slot to play right now. */
    fun currentSlot(hour: Int): AzkarSlot = when (azkarWhich) {
        AzkarWhich.MORNING -> AzkarSlot.MORNING
        AzkarWhich.EVENING -> AzkarSlot.EVENING
        AzkarWhich.AUTO -> if (hour in 3..14) AzkarSlot.MORNING else AzkarSlot.EVENING
    }
}
