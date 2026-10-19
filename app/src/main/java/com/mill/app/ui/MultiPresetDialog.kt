package com.mill.app.ui

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.mill.app.data.FavoriteCombos
import com.mill.app.data.MediaKind
import com.mill.app.data.Preset
import com.mill.app.data.Presets
import com.mill.app.data.SizeEstimator

object MultiPresetDialog {

    fun show(
        context: Context,
        title: String,
        sourceUri: Uri?,
        onConfirm: (List<String>) -> Unit
    ) {
        val scroll = ScrollView(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        // Load combos fresh
        val combos = FavoriteCombos.load(context)
        val comboLabels = mutableListOf("— Load favorite combo —")
        combos.forEach { comboLabels.add(it.name) }

        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, comboLabels)
        }
        container.addView(spinner)

        val checkboxes = mutableListOf<Pair<String, CheckBox>>()

        fun section(label: String, presets: List<Preset>) {
            val header = TextView(context).apply {
                text = label
                setTextColor(0xFF888888.toInt())
                textSize = 12f
                setPadding(0, 24, 0, 8)
                letterSpacing = 0.1f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            container.addView(header)

            presets.forEach { p ->
                val cb = CheckBox(context).apply {
                    text = buildLabel(context, p, sourceUri)
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                }
                container.addView(cb)
                checkboxes.add(p.id to cb)
            }
        }

        section("VIDEO", Presets.all.filter { it.kind == MediaKind.VIDEO })
        section("AUDIO", Presets.all.filter { it.kind == MediaKind.AUDIO })

        // Auto-fill on combo selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, pos: Int, id: Long
            ) {
                if (pos == 0) return
                if (pos - 1 >= combos.size) return
                val selectedCombo = combos[pos - 1]
                val presetIds = selectedCombo.presetIds
                checkboxes.forEach { pair ->
                    pair.second.isChecked = pair.first in presetIds
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        scroll.addView(container)

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(scroll)
            .setPositiveButton("Convert") { _, _ ->
                val selected = checkboxes.filter { it.second.isChecked }.map { it.first }
                if (selected.isNotEmpty()) onConfirm(selected)
            }
            .setNeutralButton("Save combo") { _, _ ->
                val selected = checkboxes.filter { it.second.isChecked }.map { it.first }
                if (selected.isNotEmpty()) showSaveComboDialog(context, selected)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildLabel(context: Context, p: Preset, uri: Uri?): String {
        if (uri == null) return "${p.name} — ${p.description}"
        return try {
            val bytes = SizeEstimator.estimate(context, uri, p)
            if (bytes > 0) "${p.name} — ${p.description} (~${SizeEstimator.human(bytes)})"
            else "${p.name} — ${p.description}"
        } catch (_: Exception) { "${p.name} — ${p.description}" }
    }

    private fun showSaveComboDialog(context: Context, presetIds: List<String>) {
        val input = android.widget.EditText(context).apply {
            hint = "Combo name"
        }
        AlertDialog.Builder(context)
            .setTitle("Save combo")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) {
                    FavoriteCombos.add(context, com.mill.app.data.Combo(
                        id = "combo_${System.currentTimeMillis()}",
                        name = name,
                        presetIds = presetIds
                    ))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
