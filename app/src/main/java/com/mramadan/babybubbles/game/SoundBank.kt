package com.mramadan.babybubbles.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import androidx.annotation.RawRes
import com.mramadan.babybubbles.R
import java.util.Locale

/** Low-latency sounds, preloaded once. Volume is capped to protect small ears. */
class SoundBank(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val pop = pool.load(context, R.raw.pop, 1)
    private val ids: Map<Int, Int> = (
        Catalog.all.map { it.sound }.filter { it != 0 } +
            listOf(R.raw.snd_crunch, R.raw.snd_blub, R.raw.snd_bubbles, R.raw.snd_sparrow, R.raw.snd_car)
        ).distinct().associateWith { pool.load(context, it, 1) }

    fun playPop(rate: Float = 1f, volume: Float = 0.45f) {
        pool.play(pop, volume, volume, 1, 0, rate)
    }

    fun play(@RawRes res: Int, volume: Float = 0.85f) {
        ids[res]?.let { pool.play(it, volume, volume, 2, 0, 1f) }
    }

    fun release() = pool.release()
}

/** Speaks names and phrases with the phone's own voice engine, if it has the language. */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.setSpeechRate(0.85f)
            tts.setPitch(1.1f)
        }
    }

    fun say(text: String, locale: Locale) {
        if (!ready) return
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return
        // English is slowed right down so each sound of the word is clear for small children.
        val english = locale.language == "en"
        tts.setSpeechRate(if (english) 0.62f else 0.85f)
        tts.setPitch(if (english) 1.15f else 1.1f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak")
    }

    val isSpeaking: Boolean get() = ready && tts.isSpeaking

    /** Speaks only when nothing is being said, so fast taps don't chop phrases. */
    fun sayIfIdle(text: String, locale: Locale): Boolean {
        if (!ready || tts.isSpeaking) return false
        say(text, locale)
        return true
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
