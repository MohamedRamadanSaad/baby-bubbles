package com.mramadan.babybubbles.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import com.mramadan.babybubbles.game.GameMode
import java.io.File

enum class Parent(val folder: String, val arabic: String) {
    DAD("dad", "بابا"),
    MOM("mom", "ماما"),
}

/**
 * The parents' recordings, one short file per item, kept only inside the app's
 * private storage on this phone. Nothing is uploaded anywhere.
 */
class VoiceStore(context: Context) {
    private val root = File(context.filesDir, "voices")

    fun file(parent: Parent, mode: GameMode, index: Int): File =
        File(File(root, parent.folder), "${mode.name.lowercase()}_$index.m4a")

    fun has(parent: Parent, mode: GameMode, index: Int): Boolean = file(parent, mode, index).let { it.exists() && it.length() > 0 }

    fun count(parent: Parent, mode: GameMode, size: Int): Int = (0 until size).count { has(parent, mode, it) }

    fun delete(parent: Parent, mode: GameMode, index: Int) {
        file(parent, mode, index).delete()
    }
}

/** Records one item's name to an AAC file. */
class Recorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var target: File? = null
    private var temp: File? = null

    val isRecording: Boolean get() = recorder != null

    fun start(file: File): Boolean {
        stop(save = false)
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        return runCatching {
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44100)
            r.setAudioEncodingBitRate(96_000)
            r.setOutputFile(tmp.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            target = file
            temp = tmp
            true
        }.getOrElse {
            tmp.delete()
            recorder = null
            false
        }
    }

    /** Stops recording; keeps the file when [save] and the recording worked. */
    fun stop(save: Boolean = true): Boolean {
        val r = recorder ?: return false
        recorder = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        val tmp = temp
        val dst = target
        temp = null
        target = null
        if (tmp == null || dst == null) return false
        return if (ok && save && tmp.length() > 0) {
            dst.delete()
            tmp.renameTo(dst)
        } else {
            tmp.delete()
            false
        }
    }
}

/** Plays one recording at a time (names, praise). */
class VoicePlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    val isPlaying: Boolean get() = player != null

    fun play(file: File, volume: Float = 1f, onDone: () -> Unit = {}) {
        stop()
        val mp = MediaPlayer()
        runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(file.absolutePath)
            mp.setVolume(volume, volume)
            mp.setOnCompletionListener {
                if (player === it) player = null
                it.release()
                onDone()
            }
            mp.prepare()
            mp.start()
            player = mp
        }.onFailure {
            mp.release()
            onDone()
        }
    }

    fun stop() {
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
    }
}

/** Loops the parent's chosen azkar file softly behind the game. */
class AzkarPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun start(uri: Uri, volume: Float): Boolean {
        stop()
        val mp = MediaPlayer()
        return runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(context, uri)
            mp.isLooping = true
            mp.setVolume(volume, volume)
            mp.prepare()
            mp.start()
            player = mp
            true
        }.getOrElse {
            mp.release()
            false
        }
    }

    fun pause() {
        runCatching { player?.pause() }
    }

    fun resume() {
        runCatching { player?.start() }
    }

    fun stop() {
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
    }
}
