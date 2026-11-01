package com.mramadan.babybubbles

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
import com.mramadan.babybubbles.game.AnimalCatalog
import com.mramadan.babybubbles.game.DUCK_INDEX
import com.mramadan.babybubbles.game.GameEngine
import com.mramadan.babybubbles.game.GameEvents
import com.mramadan.babybubbles.game.GameMode
import com.mramadan.babybubbles.game.GameScreen
import com.mramadan.babybubbles.game.SoundBank
import com.mramadan.babybubbles.game.Speaker
import com.mramadan.babybubbles.game.WalkerKind
import com.mramadan.babybubbles.parent.HomeScreen
import com.mramadan.babybubbles.parent.ParentGate
import com.mramadan.babybubbles.ui.BabyTheme

class MainActivity : ComponentActivity(), GameEvents {

    private var playing by mutableStateOf(false)
    private var showGate by mutableStateOf(false)
    private var mode by mutableStateOf(GameMode.COLORS)
    private var speakLang by mutableStateOf(SpeakLang.AR)
    private var vibrate by mutableStateOf(true)

    private lateinit var settings: Settings
    private lateinit var sounds: SoundBank
    private lateinit var speaker: Speaker
    private lateinit var engine: GameEngine

    private val handler = Handler(Looper.getMainLooper())
    private var pendingName: Runnable? = null

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

    private val blockBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit // the child cannot back out
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        settings = Settings(this)
        mode = settings.mode
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
                        GameScreen(engine)
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
        sounds.playColor(colorIndex)
        haptic()
    }

    override fun onAnimalPop(animalIndex: Int) {
        sounds.playAnimal(animalIndex)
        haptic()
        pendingName?.let { handler.removeCallbacks(it) }
        val info = AnimalCatalog[animalIndex]
        val lang = speakLang
        val say = Runnable {
            speaker.say(if (lang == SpeakLang.AR) info.arabic else info.english, lang.locale)
        }
        pendingName = say
        handler.postDelayed(say, 850)
    }

    override fun onWalkerTap(kind: WalkerKind) {
        when (kind) {
            WalkerKind.DUCK -> sounds.playAnimal(DUCK_INDEX, withPop = false)
            WalkerKind.SNAIL -> sounds.playNote(2)
            WalkerKind.BUTTERFLY -> sounds.playNote(7)
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
        engine.start(mode)
        playing = true
        blockBack.isEnabled = true
        hideSystemBars()
        // Without device-owner rights this becomes screen pinning: the system asks once to confirm.
        runCatching { startLockTask() }
    }

    private fun exitPlay() {
        showGate = false
        playing = false
        blockBack.isEnabled = false
        resetVolumeHold()
        cancelPendingName()
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
