package com.example.recordaudio

import android.media.MediaPlayer
import com.example.recordaudio.adapters.RecordingAdapter
import com.example.recordaudio.recording.RecordingViewModel
import com.example.recordaudio.recordinglist.RecordingListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private val module = module {
    viewModel { RecordingViewModel() }
    viewModel { RecordingListViewModel() }

    factory { RecordingAdapter() }
    factory { MediaPlayer() }
}

val appModules = listOf(module)