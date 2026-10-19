package com.mill.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mill.app.data.Job
import com.mill.app.data.JobStatus
import com.mill.app.data.JobStore
import com.mill.app.data.Presets
import com.mill.app.databinding.ActivityMainBinding
import com.mill.app.service.ConvertService
import com.mill.app.service.Converter
import com.mill.app.service.WatchFolderService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: JobAdapter
    private val handler = Handler(Looper.getMainLooper())

    private val pickBatchLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        if (uris.size == 1) choosePreset(uris[0])
        else choosePresetForBatch(uris)
    }

    private val pickSingleLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { choosePreset(it) } }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshQueue()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = JobAdapter(
            onRemove = { job ->
                JobStore.remove(this, job.id)
                refreshQueue()
            },
            onShare = { job ->
                job.outputPath?.let { path ->
                    val uri = Uri.parse("file://$path")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                    }
                    startActivity(Intent.createChooser(intent, "Share output"))
                }
            }
        )

        binding.jobList.layoutManager = LinearLayoutManager(this)
        binding.jobList.adapter = adapter

        requestPerms()

        binding.btnPick.setOnClickListener {
            pickBatchLauncher.launch(arrayOf("video/*", "audio/*"))
        }
        binding.btnSingle.setOnClickListener {
            pickSingleLauncher.launch(arrayOf("video/*", "audio/*"))
        }
        binding.btnSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnClearDone.setOnClickListener {
            JobStore.clearDone(this)
            refreshQueue()
        }
        binding.btnCancelAll.setOnClickListener {
            cancelAll()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshQueue()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun requestPerms() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                     Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !android.os.Environment.isExternalStorageManager()) {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .setData(Uri.parse("package:$packageName")))
            } catch (_: Exception) {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun choosePreset(uri: Uri) {
        val name = sanitizeName(uri.lastPathSegment)
        MultiPresetDialog.show(this, "Convert: $name", uri) { selectedIds ->
            selectedIds.forEach { id ->
                val p = Presets.all.find { it.id == id } ?: return@forEach
                enqueue(uri, name, p.id, p.name)
            }
            Toast.makeText(this, "Queued ${selectedIds.size} conversions", Toast.LENGTH_SHORT).show()
        }
    }

    private fun choosePresetForBatch(uris: List<Uri>) {
        MultiPresetDialog.show(this, "Convert ${uris.size} files", uris.firstOrNull()) { selectedIds ->
            var count = 0
            uris.forEach { uri ->
                val name = sanitizeName(uri.lastPathSegment)
                selectedIds.forEach { id ->
                    val p = Presets.all.find { it.id == id } ?: return@forEach
                    enqueue(uri, name, p.id, p.name)
                    count++
                }
            }
            Toast.makeText(this, "Queued $count jobs", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sanitizeName(raw: String?): String {
        val n = raw ?: return "input"
        return n.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "input" }
    }

    private fun enqueue(uri: Uri, name: String, presetId: String, presetName: String) {
        val job = Job(
            id = UUID.randomUUID().toString(),
            inputUri = uri.toString(),
            inputName = name,
            presetId = presetId,
            presetName = presetName,
            outputPath = null
        )
        JobStore.addJob(this, job)
        refreshQueue()
        startService()
    }

    private fun startService() {
        val intent = Intent(this, ConvertService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

        private fun cancelAll() {
        Thread {
            // 1. Kill the running FFmpeg session
            Converter.cancel()

            // 2. Mark the running job and all queued jobs as cancelled
            val jobs = JobStore.load(this)
            jobs.filter { it.status == JobStatus.RUNNING || it.status == JobStatus.QUEUED }
                .forEach {
                    JobStore.update(this, it.id) { j ->
                        j.copy(status = JobStatus.CANCELLED, message = "Cancelled by user")
                    }
                }

            // 3. Tell the service to stop its loop
            val stopIntent = Intent(this, ConvertService::class.java).apply {
                action = "CANCEL"
            }
            ContextCompat.startForegroundService(this, stopIntent)

            runOnUiThread { refreshQueue() }
        }.start()
    }

    private fun refreshQueue() {
        val jobs = JobStore.load(this).sortedBy {
            when (it.status) {
                JobStatus.RUNNING -> 0
                JobStatus.QUEUED -> 1
                else -> 2
            }
        }
        adapter.submit(jobs)
        binding.emptyState.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
    }
}
