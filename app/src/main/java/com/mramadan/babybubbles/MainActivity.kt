package com.mramadan.babybubbles

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mramadan.babybubbles.game.GameEngine
import com.mramadan.babybubbles.game.GameScreen
import com.mramadan.babybubbles.game.SoundBank
import com.mramadan.babybubbles.parent.HomeScreen
import com.mramadan.babybubbles.parent.ParentGate

class MainActivity : ComponentActivity() {

    private var playing by mutableStateOf(false)
    private var showGate by mutableStateOf(false)

    private lateinit var sounds: SoundBank
    private lateinit var engine: GameEngine

    // Parent key: hold Volume Up + Volume Down together for 3 seconds.
    private val handler = Handler(Looper.getMainLooper())
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

        sounds = SoundBank(this)
        engine = GameEngine { colorIndex -> sounds.playPop(colorIndex) }
        onBackPressedDispatcher.addCallback(this, blockBack)

        setContent {
            MaterialTheme {
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
                    HomeScreen(onStart = { startPlay() })
                }
            }
        }
    }

    private fun startPlay() {
        engine.reset()
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
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        sounds.release()
        super.onDestroy()
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
