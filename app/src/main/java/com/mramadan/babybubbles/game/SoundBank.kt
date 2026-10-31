package com.mramadan.babybubbles.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.mramadan.babybubbles.R

/** Low-latency sounds, preloaded once. Volume is capped to protect small ears. */
class SoundBank(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
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

    fun playPop(colorIndex: Int) {
        pool.play(pop, 0.35f, 0.35f, 1, 0, 1f)
        pool.play(notes[colorIndex % notes.size], 0.6f, 0.6f, 1, 0, 1f)
    }

    fun release() = pool.release()
}
