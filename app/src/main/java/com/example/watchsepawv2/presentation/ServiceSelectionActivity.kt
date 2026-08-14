package com.example.watchsepawv2.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.watchsepawv2.R

class ServiceSelectionActivity : ComponentActivity() {
    private val activityRecognitionPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                selectSamsungHealthServices()
            } else {
                Toast.makeText(
                    this,
                    "Activity recognition permission is required for Samsung fall events",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_selection)

        findViewById<Button>(R.id.btnAfePlus).setOnClickListener {
            MyPreferenceData(this).setFallMode(MyPreferenceData.FALL_MODE_CUSTOM)
            SamsungHealthEventManager.unregister(this)
            notifyBackgroundServiceFallModeChanged()
            navigateToMain()
        }

        findViewById<Button>(R.id.btnHealthServices).setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                selectSamsungHealthServices()
            } else {
                activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    private fun selectSamsungHealthServices() {
        MyPreferenceData(this).setFallMode(MyPreferenceData.FALL_MODE_SAMSUNG)
        SamsungHealthEventManager.register(this)
        notifyBackgroundServiceFallModeChanged()
        navigateToMain()
    }

    private fun notifyBackgroundServiceFallModeChanged() {
        val serviceIntent = Intent(this, BackgroundService::class.java).apply {
            action = BackgroundService.ACTION_SET_FALL_MODE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, standbymain::class.java))
        finish()
    }
}
