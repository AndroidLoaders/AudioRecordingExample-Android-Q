package com.example.recordaudio.recordinglist

import android.net.Uri
import com.example.recordaudio.adapters.RecordingAdapter

data class Recording(
    val id: String, val title: String, val name: String, val contentUri: Uri, val size: String,
    val createdDate: String, val duration: String = "0",
    var state: Int = RecordingAdapter.RECORDING_STATE_STOPPED
)