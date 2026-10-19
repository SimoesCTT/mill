package com.mill.app.data

enum class JobStatus { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

data class Job(
    val id: String,
    val inputUri: String,
    val inputName: String,
    val presetId: String,
    val presetName: String,
    val outputPath: String?,
    val status: JobStatus = JobStatus.QUEUED,
    val progress: Float = 0f,
    val message: String = "",
    val startedAt: Long = 0L,
    val finishedAt: Long = 0L
)
