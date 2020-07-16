package com.example.recordaudio.recordinglist

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.recordaudio.FileIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordingListViewModel : ViewModel() {

    private val TAG: String = "TAG -- ${RecordingListViewModel::class.java.simpleName} -->"

    private val recordings: MutableLiveData<MutableList<Recording>> = MutableLiveData()

    private suspend fun processCursorData(cursor: Cursor): MutableList<Recording> =
        withContext(Dispatchers.IO) {
            cursor.moveToFirst()

            val recordingList = mutableListOf<Recording>()

            // Cache column indices.
            val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            do {
                val id: Long = cursor.getLong(idColumn)
                val title: String = cursor.getString(titleColumn)
                val name: String = cursor.getString(nameColumn)
                val size: String = cursor.getString(sizeColumn)
                val createdDate: String = cursor.getString(dateAddedColumn)
                var duration = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val durationColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    duration = cursor.getInt(durationColumn)
                }
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )

                println("$TAG $id -- $title -- $name -- $duration -- $contentUri \n")

                recordingList.add(
                    Recording(
                        id.toString(), title, name, contentUri, size, createdDate,
                        duration.toString()
                    )
                )
            } while (cursor.moveToNext())

            recordingList
        }

    private suspend fun prepareCursorData(cursor: Cursor) {
        try {
            recordings.postValue(processCursorData(cursor))
        } catch (e: Exception) {
            println("$TAG ${e.message}")
        } finally {
            cursor.close()
        }
    }

    private suspend fun fetchRecordingListBelow29(resolver: ContentResolver) =
        withContext(Dispatchers.IO) {
            val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.AudioColumns.DATE_ADDED
            )
            try {
                resolver.query(
                    uri, projection, "${MediaStore.Audio.Media.DATA} LIKE ?",
                    arrayOf("%${FileIO.AudioDir}%"),
                    "${MediaStore.Audio.AudioColumns.DATE_ADDED} DESC"
                )?.use {
                    prepareCursorData(it)
                }
            } catch (e: Exception) {
                println("$TAG ${e.message}")
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun fetchRecordingListAbove29(resolver: ContentResolver) =
        withContext(Dispatchers.IO) {
            val uri: Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.RELATIVE_PATH, MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.AudioColumns.DATE_ADDED
            )
            try {
                resolver.query(
                    uri, projection,
                    "${MediaStore.Audio.Media.IS_PENDING} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
                    arrayOf("0", "%${FileIO.AudioDir}%"),
                    "${MediaStore.Audio.AudioColumns.DATE_ADDED} DESC"
                )?.use {
                    prepareCursorData(it)
                }
            } catch (e: Exception) {
                println("$TAG ${e.message}")
            }
        }

    fun getRecordingStream(): LiveData<MutableList<Recording>> = recordings

    suspend fun fetchRecordingList(resolver: ContentResolver) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) fetchRecordingListAbove29(resolver)
        else fetchRecordingListBelow29(resolver)
}