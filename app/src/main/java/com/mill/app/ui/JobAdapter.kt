package com.mill.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mill.app.R
import com.mill.app.data.Job
import com.mill.app.data.JobStatus
import com.mill.app.databinding.ItemJobBinding

class JobAdapter(
    private val onRemove: (Job) -> Unit,
    private val onShare: (Job) -> Unit
) : RecyclerView.Adapter<JobAdapter.VH>() {

    private val items = mutableListOf<Job>()

    fun submit(newItems: List<Job>) {
        items.clear(); items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemJobBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemJobBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(job: Job) {
            b.title.text = job.inputName
            b.preset.text = job.presetName

            val statusText = when (job.status) {
                JobStatus.QUEUED -> "Queued"
                JobStatus.RUNNING -> "${(job.progress * 100).toInt()}% — ${job.message}"
                JobStatus.DONE -> "Done"
                JobStatus.FAILED -> "Failed: ${job.message}"
                JobStatus.CANCELLED -> "Cancelled"
            }
            b.status.text = statusText

            b.progressBar.visibility =
                if (job.status == JobStatus.RUNNING) View.VISIBLE else View.GONE
            b.progressBar.progress = (job.progress * 100).toInt()

            val statusColor = when (job.status) {
                JobStatus.DONE -> 0xFF8BC34A.toInt()
                JobStatus.FAILED -> 0xFFE57373.toInt()
                JobStatus.RUNNING -> 0xFFFFFFFF.toInt()
                JobStatus.CANCELLED -> 0xFF888888.toInt()
                JobStatus.QUEUED -> 0xFF888888.toInt()
            }
            b.status.setTextColor(statusColor)

            b.btnRemove.visibility =
                if (job.status == JobStatus.QUEUED || job.status == JobStatus.DONE ||
                    job.status == JobStatus.FAILED || job.status == JobStatus.CANCELLED)
                    View.VISIBLE else View.GONE
            b.btnRemove.setOnClickListener { onRemove(job) }

            b.btnShare.visibility = if (job.status == JobStatus.DONE) View.VISIBLE else View.GONE
            b.btnShare.setOnClickListener { onShare(job) }
        }
    }
}
