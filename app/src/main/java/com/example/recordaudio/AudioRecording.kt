package com.example.recordaudio

import android.app.Application
import android.net.Uri
import android.os.Environment
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.io.File

class AudioRecording : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@AudioRecording)
            modules(appModules)
        }
    }
}