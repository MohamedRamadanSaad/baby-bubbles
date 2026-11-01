package com.mramadan.babybubbles

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.mramadan.babybubbles.game.SpeedLevel
import com.mramadan.babybubbles.game.WalkerKind
import com.mramadan.babybubbles.game.defaultPopSound
import com.mramadan.babybubbles.parent.CallOverlay
import com.mramadan.babybubbles.parent.HomeScreen
import com.mramadan.babybubbles.parent.ParentGate
import com.mramadan.babybubbles.ui.BabyTheme

class MainActivity : ComponentActivity(), GameEvents {

    private var playing by mutableStateOf(false)
    private var showGate by mutableStateOf(false)
    private var ringing by mutableStateOf(false)
    private var mode by mutableStateOf(GameMode.COLORS)
    private var speed by mutableStateOf(SpeedLevel.NORMAL)
    private var speakLang by mutableStateOf(SpeakLang.AR)
    private var vibrate by mutableStateOf(true)
    private var characters by mutableStateOf<List<Character>>(emptyList())

    private lateinit var settings: Settings
    private lateinit var sounds: SoundBank
    private lateinit var speaker: Speaker
    private lateinit var engine: GameEngine

    private val handler = Handler(Looper.getMainLooper())
    private var pendingName: Runnable? = null
    private var praiseIndex = 0

    private val audioManager by lazy { getSystemService(AudioManager::class.java) }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
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
            ringing = audioManager?.mode == AudioManager.MODE_RINGTONE
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

        settings = Settings(this)
        mode = settings.mode
        speed = settings.speed
        speakLang = settings.speakLang
        vibrate = settings.vibrate

        sounds = SoundBank(this)
        speaker = Speaker(this)
        engine = GameEngine(this)
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
                    HomeScreen(
                        mode = mode,
                        onModeChange = { mode = it; settings.mode = it },
                        speed = speed,
                        onSpeedChange = { speed = it; settings.speed = it },
                        speakLang = speakLang,
                        onSpeakLangChange = { speakLang = it; settings.speakLang = it },
                        vibrate = vibrate,
                        onVibrateChange = { vibrate = it; settings.vibrate = it },
                        onStart = { startPlay() },
                    )
                }
            }
        }
    }

    // ---- Game events -------------------------------------------------------

    override fun onColorPop(colorIndex: Int) {
        sounds.playPop()
        haptic()
        // سُبْحَانَ اللَّه، الْحَمْدُ لِلَّه، اللَّهُ أَكْبَر in turn, never cut mid-phrase.
        if (speaker.sayIfIdle(Catalog.praise[praiseIndex % Catalog.praise.size], SpeakLang.AR.locale)) {
            praiseIndex++
        }
    }

    override fun onCharacterPop(index: Int) {
        val ch = characters.getOrNull(index) ?: return
        haptic()
        val ownSound = ch.sound != 0
        if (ownSound) {
            sounds.playPop(rate = 1.3f, volume = 0.25f)
            sounds.play(ch.sound)
        } else {
            sounds.play(mode.defaultPopSound(), volume = 0.7f)
        }
        pendingName?.let { handler.removeCallbacks(it) }
        val lang = speakLang
        val say = Runnable { speaker.say(if (lang == SpeakLang.AR) ch.arabic else ch.english, lang.locale) }
        pendingName = say
        handler.postDelayed(say, if (ownSound) 900 else 350)
    }

    override fun onWalkerTap(kind: WalkerKind) {
        when (kind) {
            WalkerKind.DUCK -> sounds.play(R.raw.snd_duck)
            WalkerKind.CAR -> sounds.play(R.raw.snd_car)
            WalkerKind.BIRD -> sounds.play(R.raw.snd_sparrow)
            WalkerKind.FISH, WalkerKind.FISH2 -> sounds.play(R.raw.snd_blub)
            WalkerKind.SNAIL, WalkerKind.BUTTERFLY -> sounds.playPop(rate = 1.4f)
        }
        haptic()
    }

    private fun haptic() {
        if (!vibrate) return
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // ---- Play session and lock ---------------------------------------------

    private fun startPlay() {
        characters = Catalog.forMode(mode)
        engine.start(mode, characters.size, speed)
        praiseIndex = 0
        ringing = false
        playing = true
        blockBack.isEnabled = true
        hideSystemBars()
        handler.removeCallbacks(callWatch)
        handler.post(callWatch)
        // Without device-owner rights this becomes screen pinning: the system asks once to confirm.
        runCatching { startLockTask() }
    }

    private fun exitPlay() {
        showGate = false
        playing = false
        ringing = false
        blockBack.isEnabled = false
        handler.removeCallbacks(callWatch)
        resetVolumeHold()
        cancelPendingName()
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
        }
    }

    override fun onPause() {
        super.onPause()
        resetVolumeHold()
        cancelPendingName()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        sounds.release()
        speaker.release()
        super.onDestroy()
    }

    private fun cancelPendingName() {
        pendingName?.let { handler.removeCallbacks(it) }
        pendingName = null
        speaker.stop()
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
