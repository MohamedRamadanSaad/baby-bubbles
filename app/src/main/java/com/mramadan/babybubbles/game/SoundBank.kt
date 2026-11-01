package com.mramadan.babybubbles.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
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

    private val notes = listOf(
        R.raw.note_0, R.raw.note_1, R.raw.note_2, R.raw.note_3, R.raw.note_4,
        R.raw.note_5, R.raw.note_6, R.raw.note_7, R.raw.note_8, R.raw.note_9,
    ).map { pool.load(context, it, 1) }

    private val pop = pool.load(context, R.raw.pop, 1)
    private val animals = AnimalCatalog.map { pool.load(context, it.sound, 1) }

    fun playColor(colorIndex: Int) {
        pool.play(pop, 0.35f, 0.35f, 1, 0, 1f)
        pool.play(notes[colorIndex % notes.size], 0.6f, 0.6f, 1, 0, 1f)
    }

    fun playAnimal(animalIndex: Int, withPop: Boolean = true) {
        if (withPop) pool.play(pop, 0.2f, 0.2f, 1, 0, 1.3f)
        pool.play(animals[animalIndex % animals.size], 0.85f, 0.85f, 2, 0, 1f)
    }

    fun playNote(index: Int) {
        pool.play(notes[index % notes.size], 0.5f, 0.5f, 1, 0, 1f)
    }

    fun release() = pool.release()
}

/** Says animal names with the phone's own voice engine, if it has the language. */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.setSpeechRate(0.85f)
            tts.setPitch(1.15f)
        }
    }

    fun say(text: String, locale: Locale) {
        if (!ready) return
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "animal-name")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
