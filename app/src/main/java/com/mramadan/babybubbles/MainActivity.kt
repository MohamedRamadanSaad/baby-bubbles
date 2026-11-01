package com.mramadan.babybubbles

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mramadan.babybubbles.game.Catalog
import com.mramadan.babybubbles.game.Character
import com.mramadan.babybubbles.game.GameEngine
import com.mramadan.babybubbles.game.GameEvents
import com.mramadan.babybubbles.game.GameMode
import com.mramadan.babybubbles.game.GameScreen
import com.mramadan.babybubbles.game.SoundBank
import com.mramadan.babybubbles.game.Speaker
import com.mramadan.babybubbles.game.WalkerKind
import com.mramadan.babybubbles.game.defaultPopSound
import com.mramadan.babybubbles.parent.CallOverlay
import com.mramadan.babybubbles.parent.HomeScreen
import com.mramadan.babybubbles.parent.ParentGate
import com.mramadan.babybubbles.parent.RecordHomeScreen
import com.mramadan.babybubbles.parent.RecordItemsScreen
import com.mramadan.babybubbles.parent.SettingsScreen
import com.mramadan.babybubbles.ui.BabyTheme
import com.mramadan.babybubbles.voice.AzkarPlayer
import com.mramadan.babybubbles.voice.Parent
import com.mramadan.babybubbles.voice.Recorder
import com.mramadan.babybubbles.voice.VoicePlayer
import com.mramadan.babybubbles.voice.VoiceStore
import java.io.File

sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data object RecordHome : Screen
    data class RecordItems(val mode: GameMode) : Screen
}

class MainActivity : ComponentActivity(), GameEvents {

    private var playing by mutableStateOf(false)
    private var showGate by mutableStateOf(false)
    private var ringing by mutableStateOf(false)
    private var screen by mutableStateOf<Screen>(Screen.Home)
    private var recordParent by mutableStateOf(Parent.DAD)
    private var characters by mutableStateOf<List<Character>>(emptyList())
    private var playMode = GameMode.COLORS

    private lateinit var prefs: LivePrefs
    private lateinit var sounds: SoundBank
    private lateinit var speaker: Speaker
    private lateinit var engine: GameEngine
    private lateinit var voices: VoiceStore
    private lateinit var recorder: Recorder
    private lateinit var voicePlayer: VoicePlayer
    private lateinit var azkar: AzkarPlayer

    private val handler = Handler(Looper.getMainLooper())
    private var pendingName: Runnable? = null
    private var praiseIndex = 0
    private var nextIsDad = true
    private var azkarPausedForCall = false

    private val audioManager by lazy { getSystemService(AudioManager::class.java) }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
    }

    private var afterMicGranted: (() -> Unit)? = null
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) afterMicGranted?.invoke()
        afterMicGranted = null
    }

    private var pickingSlot: AzkarSlot? = null
    private var recordingSlot by mutableStateOf<AzkarSlot?>(null)
    private var previewSlot by mutableStateOf<AzkarSlot?>(null)

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val slot = pickingSlot ?: return@registerForActivityResult
        pickingSlot = null
        if (uri != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            prefs.setSlot(slot, uri.toString(), displayName(uri))
            prefs.azkarOn = true
        }
    }

    // Parent key: hold Volume Up + Volume Down together for 3 seconds.
    private var volUpDown = false
    private var volDownDown = false
    private var holdArmed = false
    private val openGate = Runnable { if (playing) showGate = true }

    /**
     * While the phone rings, Android itself takes the volume keys (to silence the
     * ringer), so the key combo can't reach the game. We watch for ringing instead
     * and offer an on-screen parent button that leads to the same gate.
     */
    private val callWatch = object : Runnable {
        override fun run() {
            if (!playing) return
            val now = audioManager?.mode == AudioManager.MODE_RINGTONE
            if (now != ringing) {
                ringing = now
                if (now) {
                    azkar.pause()
                    azkarPausedForCall = true
                } else if (azkarPausedForCall) {
                    azkar.resume()
                    azkarPausedForCall = false
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    private val blockBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit // the child cannot back out
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefs = LivePrefs(Settings(this))
        sounds = SoundBank(this)
        speaker = Speaker(this)
        engine = GameEngine(this)
        voices = VoiceStore(this)
        recorder = Recorder(this)
        voicePlayer = VoicePlayer(this)
        azkar = AzkarPlayer(this)
        onBackPressedDispatcher.addCallback(this, blockBack)

        setContent {
            BabyTheme {
                if (playing) {
                    Box {
                        GameScreen(engine, characters)
                        if (ringing && !showGate) CallOverlay(onParent = { showGate = true })
                        if (showGate) {
                            ParentGate(
                                onSuccess = { exitPlay() },
                                onDismiss = { showGate = false },
                            )
                        }
                    }
                } else {
                    when (val s = screen) {
                        Screen.Home -> HomeScreen(
                            mode = prefs.mode,
                            onModeChange = { prefs.mode = it },
                            onSettings = { screen = Screen.Settings },
                            onStart = { startPlay() },
                        )
                        Screen.Settings -> SettingsScreen(
                            prefs = prefs,
                            slotStatus = ::slotStatus,
                            recordingSlot = recordingSlot,
                            previewSlot = previewSlot,
                            onPickAzkar = { slot ->
                                stopAzkarEditing()
                                pickingSlot = slot
                                runCatching { pickAudio.launch(arrayOf("audio/*")) }
                            },
                            onRecordAzkar = ::toggleAzkarRecording,
                            onPreviewAzkar = ::toggleAzkarPreview,
                            onRecordings = {
                                stopAzkarEditing()
                                screen = Screen.RecordHome
                            },
                            onBack = {
                                stopAzkarEditing()
                                screen = Screen.Home
                            },
                        )
                        Screen.RecordHome -> RecordHomeScreen(
                            store = voices,
                            parent = recordParent,
                            onParentChange = { recordParent = it },
                            onOpen = { screen = Screen.RecordItems(it) },
                            onBack = { screen = Screen.Settings },
                        )
                        is Screen.RecordItems -> RecordItemsScreen(
                            mode = s.mode,
                            parent = recordParent,
                            store = voices,
                            isRecording = { recorder.isRecording },
                            onStartRecording = { index -> startRecording(s.mode, index) },
                            onStopRecording = { recorder.stop() },
                            onPlay = { index -> voicePlayer.play(voices.file(recordParent, s.mode, index)) },
                            onBack = {
                                voicePlayer.stop()
                                screen = Screen.RecordHome
                            },
                        )
                    }
                }
            }
        }
    }

    // ---- Recording ---------------------------------------------------------

    private fun startRecording(mode: GameMode, index: Int): Boolean {
        voicePlayer.stop()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            afterMicGranted = null
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
            return false // the parent taps record again once allowed
        }
        return recorder.start(voices.file(recordParent, mode, index))
    }

    // ---- Morning and evening azkar ----------------------------------------

    /** A picked file wins; otherwise the parent's own recording; otherwise nothing. */
    private fun slotSource(slot: AzkarSlot): Uri? =
        prefs.slotUri(slot)?.let { Uri.parse(it) }
            ?: voices.azkarFile(slot).takeIf { voices.hasAzkar(slot) }?.let { Uri.fromFile(it) }

    private fun slotStatus(slot: AzkarSlot): String? = when {
        prefs.slotUri(slot) != null -> "ملف: " + (prefs.slotName(slot) ?: "ملف صوتي")
        voices.hasAzkar(slot) -> "تسجيل بصوتك"
        else -> null
    }

    private fun toggleAzkarRecording(slot: AzkarSlot) {
        if (recordingSlot == slot) {
            if (recorder.stop()) {
                prefs.setSlot(slot, null, null) // the new recording replaces any picked file
                prefs.azkarOn = true
            }
            recordingSlot = null
            return
        }
        stopAzkarEditing()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (recorder.start(voices.azkarFile(slot))) recordingSlot = slot
    }

    private fun toggleAzkarPreview(slot: AzkarSlot) {
        if (previewSlot == slot) {
            azkar.stop()
            previewSlot = null
            return
        }
        stopAzkarEditing()
        val src = slotSource(slot) ?: return
        if (azkar.start(src, prefs.azkarVolume.level.coerceAtLeast(0.5f))) previewSlot = slot
    }

    private fun stopAzkarEditing() {
        if (recordingSlot != null) {
            val slot = recordingSlot!!
            if (recorder.stop()) prefs.setSlot(slot, null, null)
            recordingSlot = null
        }
        if (previewSlot != null) {
            azkar.stop()
            previewSlot = null
        }
    }

    private fun displayName(uri: Uri): String =
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: "ملف صوتي"

    // ---- Voices ------------------------------------------------------------

    /** The parent recording to use for an item, following the chosen voice with fallbacks. */
    private fun recordingFor(mode: GameMode, index: Int): File? {
        val order = when (prefs.nameVoice) {
            NameVoice.DEVICE -> return null
            NameVoice.DAD -> listOf(Parent.DAD, Parent.MOM)
            NameVoice.MOM -> listOf(Parent.MOM, Parent.DAD)
            NameVoice.BOTH -> {
                val first = if (nextIsDad) Parent.DAD else Parent.MOM
                nextIsDad = !nextIsDad
                listOf(first, if (first == Parent.DAD) Parent.MOM else Parent.DAD)
            }
        }
        return order.firstOrNull { voices.has(it, mode, index) }?.let { voices.file(it, mode, index) }
    }

    private fun speakItem(mode: GameMode, index: Int, arabic: String, english: String) {
        val file = recordingFor(mode, index)
        if (file != null) {
            speaker.stop()
            voicePlayer.play(file)
        } else {
            val lang = prefs.speakLang
            speaker.say(if (lang == SpeakLang.AR) arabic else english, lang.locale)
        }
    }

    private val voiceBusy: Boolean get() = voicePlayer.isPlaying || speaker.isSpeaking

    // ---- Game events -------------------------------------------------------

    override fun onColorPop(colorIndex: Int) {
        sounds.playPop()
        haptic()
        if (prefs.soundType == SoundType.POPS_ONLY || voiceBusy) return
        // سُبْحَانَ اللَّه، الْحَمْدُ لِلَّه، اللَّهُ أَكْبَر in turn, never cut mid-phrase.
        val i = praiseIndex % Catalog.praise.size
        val file = recordingFor(GameMode.COLORS, i)
        if (file != null) voicePlayer.play(file) else speaker.say(Catalog.praise[i], SpeakLang.AR.locale)
        praiseIndex++
    }

    override fun onCharacterPop(index: Int) {
        val ch = characters.getOrNull(index) ?: return
        haptic()
        if (prefs.soundType == SoundType.POPS_ONLY) {
            sounds.playPop()
            return
        }
        val ownSound = ch.sound != 0
        if (ownSound) {
            sounds.playPop(rate = 1.3f, volume = 0.25f)
            sounds.play(ch.sound)
        } else {
            sounds.play(playMode.defaultPopSound(), volume = 0.7f)
        }
        pendingName?.let { handler.removeCallbacks(it) }
        val mode = playMode
        val say = Runnable { speakItem(mode, index, ch.arabic, ch.english) }
        pendingName = say
        handler.postDelayed(say, if (ownSound) 900 else 350)
    }

    override fun onWalkerTap(kind: WalkerKind) {
        if (prefs.soundType == SoundType.POPS_ONLY) {
            sounds.playPop(rate = 1.4f)
        } else {
            when (kind) {
                WalkerKind.DUCK -> sounds.play(R.raw.snd_duck)
                WalkerKind.CAR -> sounds.play(R.raw.snd_car)
                WalkerKind.BIRD -> sounds.play(R.raw.snd_sparrow)
                WalkerKind.FISH, WalkerKind.FISH2 -> sounds.play(R.raw.snd_blub)
                WalkerKind.SNAIL, WalkerKind.BUTTERFLY -> sounds.playPop(rate = 1.4f)
            }
        }
        haptic()
    }

    private fun haptic() {
        if (!prefs.vibrate) return
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // ---- Play session and lock ---------------------------------------------

    private fun startPlay() {
        playMode = prefs.mode
        characters = Catalog.forMode(playMode)
        engine.start(playMode, characters.size, prefs.speed, prefs.maxBubbles)
        praiseIndex = 0
        ringing = false
        azkarPausedForCall = false
        playing = true
        blockBack.isEnabled = true
        hideSystemBars()
        handler.removeCallbacks(callWatch)
        handler.post(callWatch)
        stopAzkarEditing()
        if (prefs.azkarOn) {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val slot = prefs.currentSlot(hour)
            (slotSource(slot) ?: AzkarSlot.entries.firstNotNullOfOrNull { slotSource(it) })
                ?.let { azkar.start(it, prefs.azkarVolume.level) }
        }
        // Without device-owner rights this becomes screen pinning: the system asks once to confirm.
        runCatching { startLockTask() }
    }

    private fun exitPlay() {
        showGate = false
        playing = false
        ringing = false
        screen = Screen.Home
        blockBack.isEnabled = false
        handler.removeCallbacks(callWatch)
        resetVolumeHold()
        cancelPendingName()
        azkar.stop()
        // Leaving the pinned screen lets the phone show the incoming call.
        runCatching { stopLockTask() }
        showSystemBars()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (playing && (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            val down = event.action == KeyEvent.ACTION_DOWN
            if (code == KeyEvent.KEYCODE_VOLUME_UP) volUpDown = down else volDownDown = down
            if (volUpDown && volDownDown) {
                if (!holdArmed) {
                    holdArmed = true
                    handler.postDelayed(openGate, 3_000)
                }
            } else {
                holdArmed = false
                handler.removeCallbacks(openGate)
            }
            return true // consumed: the child cannot change the volume either
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && playing) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        if (playing) {
            handler.removeCallbacks(callWatch)
            handler.post(callWatch)
            if (!azkarPausedForCall) azkar.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        resetVolumeHold()
        cancelPendingName()
        // A long azkar recording is kept if the parent leaves the app midway.
        stopAzkarEditing()
        if (recorder.isRecording) recorder.stop(save = false)
        if (playing) azkar.pause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        sounds.release()
        speaker.release()
        voicePlayer.stop()
        azkar.stop()
        recorder.stop(save = false)
        super.onDestroy()
    }

    private fun cancelPendingName() {
        pendingName?.let { handler.removeCallbacks(it) }
        pendingName = null
        speaker.stop()
        voicePlayer.stop()
    }

    private fun resetVolumeHold() {
        volUpDown = false
        volDownDown = false
        holdArmed = false
        handler.removeCallbacks(openGate)
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).run {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }
}
