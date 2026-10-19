package com.mill.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mill.app.data.FavoriteCombos
import com.mill.app.data.OutputDirStore
import com.mill.app.data.Presets
import com.mill.app.databinding.ActivitySettingsBinding
import com.mill.app.service.WatchFolderService
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val pickOutputDir = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let {
        OutputDirStore.saveTree(this, it)
        refresh()
    } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rowOutput.setOnClickListener { pickOutputDir.launch(null) }
        binding.rowOutputReset.setOnClickListener {
            OutputDirStore.clear(this); refresh()
        }
        binding.rowCombos.setOnClickListener { showCombosDialog() }
        binding.rowWatch.setOnClickListener { startWatchFolder() }
        binding.rowWatchStop.setOnClickListener {
            stopService(Intent(this, WatchFolderService::class.java))
            refresh()
        }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val tree = OutputDirStore.getTree(this)
        binding.outputStatus.text = if (tree != null)
            Uri.decode(tree.toString()).takeLast(60)
        else "Default (Music/Mill, Movies/Mill)"

        val combos = FavoriteCombos.load(this)
        binding.combosCount.text = "${combos.size} saved"
    }

    private fun showCombosDialog() {
        val combos = FavoriteCombos.load(this)
        if (combos.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("No saved combos")
                .setMessage("Open a conversion dialog and tap 'Save combo' to create one.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val labels = combos.map { "${it.name}  (${it.presetIds.size} presets)" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Favorite combos")
            .setItems(labels) { _, which ->
                val combo = combos[which]
                MaterialAlertDialogBuilder(this)
                    .setTitle(combo.name)
                    .setMessage(combo.presetIds.joinToString("\n") { id ->
                        Presets.all.find { it.id == id }?.name ?: id
                    })
                    .setPositiveButton("Delete") { _, _ ->
                        FavoriteCombos.remove(this, combo.id); refresh()
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
            .show()
    }

    private fun startWatchFolder() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Pick a folder to watch")
            .setMessage("Any new media file in this folder will be auto-converted with the chosen preset.")
            .setPositiveButton("Continue") { _, _ ->
                pickOutputDir.launch(null)  // reuse SAF picker
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
