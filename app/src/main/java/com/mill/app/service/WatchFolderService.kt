package com.mill.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mill.app.R
import com.mill.app.data.Job
import com.mill.app.data.JobStore
import com.mill.app.data.Presets
import java.io.File
import java.util.UUID

class WatchFolderService : Service() {

    private var observer: FileObserver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = "mill_watch"
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(ch) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(ch, "Mill watch folder", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(WATCH_ID, buildNotification("Watching folder…"))

        val path = intent?.getStringExtra("watch_path") ?: return START_NOT_STICKY
        val presetId = intent.getStringExtra("preset_id") ?: "mp3_320"

        observer?.stopWatching()
        observer = object : FileObserver(File(path), CREATE or CLOSE_WRITE) {
            override fun onEvent(event: Int, name: String?) {
                if (name == null || name.startsWith(".")) return
                val f = File(path, name)
                if (!f.isFile) return
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in setOf(
                        "mp4","mkv","webm","avi","mov","flv","ts","m2ts","wmv","3gp",
                        "mp3","flac","ogg","m4a","aac","wav","opus","wma","ape"
                    )) return

                val preset = Presets.all.find { it.id == presetId } ?: return
                val job = Job(
                    id = UUID.randomUUID().toString(),
                    inputUri = Uri.fromFile(f).toString(),
                    inputName = name.replace(Regex("[^A-Za-z0-9._-]"), "_"),
                    presetId = preset.id,
                    presetName = preset.name,
                    outputPath = null
                )
                JobStore.addJob(this@WatchFolderService, job)

                // Kick the queue
                startService(Intent(this@WatchFolderService, ConvertService::class.java))
            }
        }
        observer?.startWatching()

        return START_STICKY
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, "mill_watch")
            .setContentTitle("Mill — Watching folder")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mill_notification)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        observer?.stopWatching()
        super.onDestroy()
    }

    companion object { private const val WATCH_ID = 1002 }
}
