package com.example.recordaudio.recording

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import com.example.recordaudio.FileIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import java.io.FileDescriptor
import java.util.*
import java.util.concurrent.TimeUnit

class RecordingViewModel : ViewModel(), KoinComponent {

    private val TAG: String = "TAG -- ${RecordingViewModel::class.java.simpleName} -->"

    private var recordingItem: Uri? = null
    private var mediaRecorder: MediaRecorder? = null

    private val contentValues: ContentValues by lazy { ContentValues() }

    private fun prepareData() = contentValues.apply {
        put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fileName = FileIO.NewFileName
            put(MediaStore.MediaColumns.TITLE, fileName)
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                FileIO.AudioDir.absolutePath.replaceFirst("/", "")
                //"${Environment.DIRECTORY_MUSIC}${File.separator}${FileIO.AudioDirName}"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        } else {
            val absoluteFilePath = FileIO.getNewFile().absolutePath
            val fileName = absoluteFilePath.substring(
                absoluteFilePath.lastIndexOf("/"), absoluteFilePath.length
            ).replace("/", "")
            put(MediaStore.MediaColumns.TITLE, fileName)
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.DATA, absoluteFilePath)
        }
    }

    private fun setupMediaRecorder(fileDescriptor: FileDescriptor) = MediaRecorder().apply {
        this.setAudioSource(MediaRecorder.AudioSource.MIC)
        this.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        this.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
        this.setOutputFile(fileDescriptor)
        try {
            this.prepare()
            this.start()
        } catch (e: Exception) {
            println("$TAG ${e.message}")
        }
    }.also {
        mediaRecorder = it
    }

    private suspend fun insertAudio(resolver: ContentResolver) = withContext(Dispatchers.IO) {
        prepareData()

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val availableVolumes = MediaStore.getExternalVolumeNames(context)
            println("$TAG $availableVolumes")
        }*/

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        try {
            recordingItem = resolver.insert(collection, contentValues)
            recordingItem?.run {
                //val outputStream = resolver.openOutputStream(this, "w")
                //context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, this))
                resolver.openFileDescriptor(this, "w")?.use {
                    it.run { setupMediaRecorder(this.fileDescriptor) }
                }
            }
        } catch (e: Exception) {
            println("$TAG ${e.message}")
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            this.stop()
            this.release()
        }
        mediaRecorder = null
    }

    private fun refreshMediaStore(context: Context) =
        MediaScannerConnection.scanFile(
            context, arrayOf(FileIO.AudioDir.toString()), arrayOf("audio/mp3")
        ) { path, uri ->
            println("$TAG Scanned --> $path")
            println("$TAG uri --> $uri")
        }

    //@RequiresApi(Build.VERSION_CODES.Q)
    suspend fun startRecording(resolver: ContentResolver) = insertAudio(resolver)

    suspend fun stopRecording(context: Context) {
        stopRecording()
        refreshMediaStore(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            withContext(Dispatchers.IO) {
                if ((contentValues.size() > 0) && (recordingItem != null)) {
                    contentValues.clear()
                    contentValues.apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    context.contentResolver.update(recordingItem!!, contentValues, null, null)
                }
            }
        }
        recordingItem = null
    }

    private fun fetchRecordingList(resolver: ContentResolver) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.SIZE
        )

        //val projection:Array<String> = arrayOf("","")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder
            )
            cursor?.run {
                cursor.moveToFirst()
                do {
                    val mId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val mDateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val mDisplayName =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val mFileSize = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                    val id = cursor.getLong(mId)
                    val dateAdded = Date(TimeUnit.SECONDS.toMillis(cursor.getLong(mDateAdded)))
                    val displayName = cursor.getString(mDisplayName)
                    val fileSize = cursor.getString(mFileSize)

                    val content =
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    println("$TAG $displayName -- $fileSize -- $content")
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            println("$TAG ${e.message}")
        } finally {
            cursor?.close()
        }
    }
}