package com.abhi.madadwala_1

import android.os.Bundle
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.abhi.madadwala_1.data.PreferenceManager
import com.abhi.madadwala_1.ui.viewmodel.AuthViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.abhi.madadwala_1.ui.navigation.NavGraph
import com.abhi.madadwala_1.ui.theme.MadadwalaTheme
import com.abhi.madadwala_1.ui.viewmodel.PaymentViewModel
import com.razorpay.PaymentResultWithDataListener
import com.razorpay.PaymentData
import kotlinx.coroutines.launch
import android.content.Intent

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private lateinit var paymentViewModel: PaymentViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Already granted
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        val preferenceManager = PreferenceManager(this)
        val title = intent.getStringExtra("title")
        val message = intent.getStringExtra("message") ?: intent.getStringExtra("body")
        
        if (title != null || message != null) {
            lifecycleScope.launch {
                preferenceManager.saveNotification(
                    com.abhi.madadwala_1.data.NotificationData(
                        title ?: "New Notification",
                        message ?: ""
                    )
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lock screen for calls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val preferenceManager = PreferenceManager(this)
        val authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        authViewModel.initAnalytics(this)
        
        com.abhi.madadwala_1.utils.NotificationHelper(this).createNotificationChannels()
        
        askNotificationPermission()
        handleNotificationIntent(intent)

        lifecycleScope.launch {
            preferenceManager.appLanguage.collect { code ->
                val locale = java.util.Locale(code)
                java.util.Locale.setDefault(locale)
                val config = resources.configuration
                if (config.locales[0].language != code) {
                    config.setLocale(locale)
                    resources.updateConfiguration(config, resources.displayMetrics)
                    recreate()
                }
            }
        }

        paymentViewModel = ViewModelProvider(this)[PaymentViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            MadadwalaTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        lifecycleScope.launch {
            paymentViewModel.onPaymentSuccess(
                razorpayPaymentId,
                data?.orderId,
                data?.signature
            )
        }
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        lifecycleScope.launch {
            paymentViewModel.onPaymentError(code, response)
        }
    }
}
