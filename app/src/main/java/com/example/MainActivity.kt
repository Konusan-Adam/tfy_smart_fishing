package com.example

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.ui.FishingAlarmScreen
import com.example.ui.theme.MatteBlackBg
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Kilit ekranı üzerinden gösterilebilme yetkisi & Ekran açık tutma
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Arka plan zırhı ve CPU WakeLock servisini başlat
        try {
            com.example.service.FishingWatchdogService.start(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val initialRodId = intent?.getIntExtra("EXTRA_ROD_ID", -1) ?: -1
        if (initialRodId != -1) {
            wakeScreenAndUnlock(initialRodId)
        }

        // 2. ViewModel'den gelen acil balık alarmında ekranı fiziksel olarak aydınlat ve öne getir
        lifecycleScope.launch {
            viewModel.wakeScreenEvent.collectLatest { rodId ->
                wakeScreenAndUnlock(rodId)
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MatteBlackBg
                ) {
                    FishingAlarmScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val rodId = intent.getIntExtra("EXTRA_ROD_ID", -1)
        if (rodId != -1) {
            wakeScreenAndUnlock(rodId)
        }
    }

    private fun wakeScreenAndUnlock(rodId: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                if (keyguardManager?.isKeyguardLocked == true) {
                    keyguardManager.requestDismissKeyguard(this, null)
                }
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }

            // 1. Ekran kapalıysa fiziksel ışığını (ekran panelini) aç
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                powerManager?.isInteractive == true
            } else {
                @Suppress("DEPRECATION")
                powerManager?.isScreenOn == true
            }

            if (!isScreenOn) {
                @Suppress("DEPRECATION")
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                    "BalikZili:LockScreenWakeup"
                )
                wakeLock?.acquire(10000L) // 10 saniye boyunca ekranı tam parlaklıkta aç
            }

            // 3. Ekranı vuran oltaya otomatik odakla
            viewModel.focusOnRod(rodId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
