package com.mramadan.babybubbles

import android.content.Context
import com.mramadan.babybubbles.game.GameMode
import com.mramadan.babybubbles.game.SpeedLevel
import java.util.Locale

enum class SpeakLang(val locale: Locale) {
    AR(Locale.forLanguageTag("ar")),
    EN(Locale.ENGLISH),
}

/** Parent choices, kept on the device only. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var mode: GameMode
        get() = runCatching { GameMode.valueOf(prefs.getString("mode", null) ?: "COLORS") }
            .getOrDefault(GameMode.COLORS)
        set(value) = prefs.edit().putString("mode", value.name).apply()

    var speakLang: SpeakLang
        get() = runCatching { SpeakLang.valueOf(prefs.getString("speak", null) ?: "AR") }
            .getOrDefault(SpeakLang.AR)
        set(value) = prefs.edit().putString("speak", value.name).apply()

    var vibrate: Boolean
        get() = prefs.getBoolean("vibrate", true)
        set(value) = prefs.edit().putBoolean("vibrate", value).apply()

    var speed: SpeedLevel
        get() = runCatching { SpeedLevel.valueOf(prefs.getString("speed", null) ?: "NORMAL") }
            .getOrDefault(SpeedLevel.NORMAL)
        set(value) = prefs.edit().putString("speed", value.name).apply()
}
