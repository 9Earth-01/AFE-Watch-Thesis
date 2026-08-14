package com.example.watchsepawv2.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

object FallAlertNotifier {
    private const val FALL_ALERT_CHANNEL_ID = "FALL_ALERT_CHANNEL"

    fun show(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val vibratePattern = longArrayOf(0, 600, 250, 600, 250, 800)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FALL_ALERT_CHANNEL_ID,
                "Fall Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a fall is detected"
                enableVibration(true)
                vibrationPattern = vibratePattern
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isInteractive) {
                @Suppress("DEPRECATION")
                powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "FallAlertNotifier:FallWake"
                ).apply {
                    acquire(3_000)
                    release()
                }
            }
        } catch (_: Exception) {
        }

        val fullScreenIntent = Intent(context, HelpActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val okIntent = Intent(context, FallActionReceiver::class.java)
            .setAction(FallActionReceiver.ACTION_OK)
        val notOkIntent = Intent(context, FallActionReceiver::class.java)
            .setAction(FallActionReceiver.ACTION_NOT_OK)

        val okPendingIntent = PendingIntent.getBroadcast(
            context,
            10,
            okIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notOkPendingIntent = PendingIntent.getBroadcast(
            context,
            11,
            notOkIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val okAction = NotificationCompat.Action.Builder(0, "OK", okPendingIntent).build()
        val notOkAction =
            NotificationCompat.Action.Builder(0, "Not OK", notOkPendingIntent).build()

        val notification = NotificationCompat.Builder(context, FALL_ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Fall detected")
            .setContentText("Tap to confirm you are safe")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(okAction)
            .addAction(notOkAction)
            .setVibrate(vibratePattern)
            .extend(NotificationCompat.WearableExtender().addAction(okAction).addAction(notOkAction))
            .build()

        notificationManager.notify(FallActionReceiver.NOTIF_ID, notification)

        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(vibratePattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibratePattern, -1)
            }
        } catch (_: Exception) {
        }

        try {
            context.startActivity(fullScreenIntent)
        } catch (_: Exception) {
        }
    }
}
