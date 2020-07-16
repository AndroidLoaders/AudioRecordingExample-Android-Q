package com.example.recordaudio

import android.os.Build
import android.os.Environment
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object FileIO {

    val NewFileName: String = generateNewFileName()

    private val TAG = "TAG -- ${FileIO::class.java.simpleName} -->"

    private const val AudioDirName = "AudioRecording"

    private fun generateNewFileName(): String {
        val formatter: DateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)
        val fileName = formatter.format(Date(System.currentTimeMillis()))
        println("$TAG Recording_$fileName.mp3")
        return "Recording_$fileName.mp3"
    }

    val AudioDir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val file = File(Environment.DIRECTORY_MUSIC, AudioDirName)
        file
    } else {
        val file = File(Environment.getExternalStorageDirectory(), AudioDirName)
        file
    }

    fun getNewFile(): File {
        try {
            val file = AudioDir
            if (!file.exists()) file.mkdirs()
            val newFile = File(file, NewFileName)
            newFile.createNewFile()
            return newFile
        } catch (e: Exception) {
            println("$TAG ${e.message}")
        }
        return File("")
    }
}