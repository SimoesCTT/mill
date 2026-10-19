package com.mill.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mill.app.R
import com.mill.app.data.JobStatus
import com.mill.app.data.JobStore
import com.mill.app.data.Presets
import kotlinx.coroutines.*

class ConvertService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = "mill_convert"
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(ch) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(ch, "Mill conversions", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // CRITICAL: foreground FIRST, always, even for CANCEL action
        try {
            startForeground(NOTIF_ID, buildNotification("Mill", "Starting…", 0, 0))
        } catch (e: Exception) {
            android.util.Log.e("MillService", "startForeground failed: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }

        // Handle CANCEL BEFORE starting any new work
        if (intent?.action == ACTION_CANCEL) {
            android.util.Log.d("MillService", "Cancel requested")
            Converter.cancel()

            // Mark the running job as cancelled
            val running = JobStore.load(this).firstOrNull { it.status == JobStatus.RUNNING }
            if (running != null) {
                JobStore.update(this, running.id) {
                    it.copy(status = JobStatus.CANCELLED, message = "Cancelled by user")
                }
            }

            notify("Mill", "Cancelled", 0, 0)
            scope.launch {
                delay(2000)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        // Regular start — kick off the queue processor (only if not already running)
        if (!isRunning) {
            isRunning = true
            scope.launch {
                try {
                    processQueue()
                } finally {
                    isRunning = false
                    delay(500)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun processQueue() {
        while (true) {
            val jobs = JobStore.load(this)
            val next = jobs.firstOrNull { it.status == JobStatus.QUEUED }
            if (next == null) {
                notify("Mill", "Queue empty", 0, 0)
                return
            }

            val preset = Presets.all.find { it.id == next.presetId }
            if (preset == null) {
                JobStore.update(this, next.id) {
                    it.copy(status = JobStatus.FAILED, message = "Unknown preset")
                }
                continue
            }

            JobStore.update(this, next.id) {
                it.copy(status = JobStatus.RUNNING, startedAt = System.currentTimeMillis(), progress = 0f)
            }

            notify(
                "Mill — ${next.inputName}",
                "0% — Starting…",
                0, 0
            )

            val result = Converter.run(
                this,
                Uri.parse(next.inputUri),
                next.inputName,
                preset
            ) { p, msg ->
                JobStore.update(this, next.id) {
                    it.copy(progress = p, message = msg)
                }
                notify(
                    "Mill — ${next.inputName}",
                    "${(p * 100).toInt()}% — $msg",
                    0, (p * 100).toInt()
                )
            }

            val finalStatus = when {
                result.cancelled -> JobStatus.CANCELLED
                result.success -> JobStatus.DONE
                else -> JobStatus.FAILED
            }

            JobStore.update(this, next.id) {
                it.copy(
                    status = finalStatus,
                    progress = if (result.success) 1f else it.progress,
                    message = result.message,
                    outputPath = result.outputPath,
                    finishedAt = System.currentTimeMillis()
                )
            }
        }
    }

    private fun buildNotification(title: String, text: String, total: Int, progress: Int): Notification {
        val cancelIntent = Intent(this, ConvertService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPi = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, "mill_convert")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mill_notification)
            .setOngoing(true)
            .addAction(0, "Cancel", cancelPi)

        if (progress > 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun notify(title: String, text: String, total: Int, progress: Int) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIF_ID, buildNotification(title, text, total, progress))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val ACTION_CANCEL = "CANCEL"

        @Volatile private var isRunning = false
    }
}
