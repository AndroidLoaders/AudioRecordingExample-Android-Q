package com.example.recordaudio.recording

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.recordaudio.BaseFragment
import com.example.recordaudio.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_recording.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecordingFragment : BaseFragment(), View.OnClickListener {

    companion object {
        private val TAG: String = "TAG -- ${RecordingFragment::class.java.simpleName} -->"

        val PERMISSION_REQUESTS: Array<String> = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        const val CONST_PERMISSION_REQUESTS: Int = 110
        const val CONST_PERMISSION_FROM_SETTINGS: Int = 1101

        private const val OPEN_DIRECTORY_REQUEST_CODE = 0xf11e
        private const val DIRECTORY_NAME = "AudioRecording"

        private const val ACTION_RECORD_AUDIO: String = "record_audio"
        private const val ACTION_PLAY_RECORDING: String = "play_record"
    }

    private var action: String = ACTION_RECORD_AUDIO

    private val viewModel: RecordingViewModel by viewModel()

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_recording, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        ivStartRecording.setOnClickListener(this)
        ivShowRecordingList.setOnClickListener(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONST_PERMISSION_REQUESTS) {
            permissions.forEachIndexed { index, permission ->
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    showPermissionTextPopup(
                        R.string.permission_denied,
                        !shouldShowRequestPermissionRationale(permission), permission
                    )
                    return
                }
            }

            prepareToRecordAudio()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONST_PERMISSION_FROM_SETTINGS) {
            if (checkPermissions(PERMISSION_REQUESTS)) prepareToRecordAudio()
            else Snackbar.make(
                constraintLayout, R.string.can_not_handle_permission, Snackbar.LENGTH_LONG
            ).show()
        } /*else if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK) {
            val directoryUri = data?.data ?: return

            val flag = (data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
            requireActivity().contentResolver.takePersistableUriPermission(directoryUri, flag)
            //showDirectoryContents(directoryUri)

            val resolver = requireContext().contentResolver
            val fileDescriptor = resolver.openFileDescriptor(directoryUri, "w")
            viewModel.startRecording(fileDescriptor!!)
        }*/
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.ivShowRecordingList -> {
                action = ACTION_PLAY_RECORDING
                if (checkPermissions(PERMISSION_REQUESTS)) {
                    if (::navController.isInitialized)
                        navController.navigate(R.id.action_recordingFragment_to_recordingListFragment)
                } else requestPermissions(PERMISSION_REQUESTS)
            }
            R.id.ivStartRecording -> {
                action = ACTION_RECORD_AUDIO
                ivStartRecording.tag = if (ivStartRecording.tag.toString() == "0") {
                    // start recording
                    if (prepareToRecordAudio()) {
                        ivStartRecording.setImageResource(R.drawable.ic_start_recording)
                        "1"
                    } else "0"
                } else {
                    // stop recording
                    stopRecordingAudio()
                    ivStartRecording.setImageResource(R.drawable.ic_stop_recording)
                    "0"
                }
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        val callPermission = ContextCompat.checkSelfPermission(requireContext(), permission)
        return (callPermission == PackageManager.PERMISSION_GRANTED)
    }

    private fun checkPermissions(permissions: Array<String>): Boolean =
        permissions.none { !checkPermission(it) }

    private fun requestPermissions(permissions: Array<String>) =
        requestPermissions(permissions, CONST_PERMISSION_REQUESTS)

    private fun requestPermission(vararg permission: String) =
        requestPermissions(permission, CONST_PERMISSION_REQUESTS)

    private fun showPermissionTextPopup(
        @StringRes message: Int, navigateToSetting: Boolean, permission: String
    ) = AlertDialog.Builder(requireContext())
        .setTitle(R.string.permission).apply {
            this.setMessage(message)
            this.setPositiveButton(R.string.grant) { dialog, _ ->
                dialog.dismiss()
                if (navigateToSetting) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireActivity().packageName, null)
                    }
                    startActivityForResult(intent, CONST_PERMISSION_FROM_SETTINGS)
                } else requestPermission(permission)
            }
            this.setNegativeButton(R.string.not_now) { dialog, _ -> dialog.dismiss() }
        }.show()

    private fun prepareToRecordAudio() = if (checkPermissions(PERMISSION_REQUESTS)) {
        if (action == ACTION_RECORD_AUDIO)
            startRecordingAudio()
        true
    } else {
        requestPermissions(PERMISSION_REQUESTS)
        false
    }

    private fun startRecordingAudio() = lifecycleScope.launch(Dispatchers.IO) {
        viewModel.startRecording(requireContext().contentResolver)
    }

    private fun stopRecordingAudio() = lifecycleScope.launch(Dispatchers.IO) {
        viewModel.stopRecording(requireContext())
    }

    // https://developer.android.com/preview/privacy/storage#change-details
    // https://developer.android.com/training/data-storage/shared/media
    // https://github.com/android/storage-samples/blob/master/
    // https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
    // https://github.com/AndroidLoaders/android-DirectorySelection

    /*val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)*/

//        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
//            // this.addCategory(Intent.CATEGORY_OPENABLE)
//            this.addFlags(
//                Intent.FLAG_GRANT_READ_URI_PERMISSION
//                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
//                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
//            )
//            // this.type = "*/*"
//            // this.putExtra(Intent.EXTRA_TITLE, "AudioRecording/$fileName")
//        }, OPEN_DIRECTORY_REQUEST_CODE)

    /*private fun createFolder() {
        val contentResolver = requireContext().contentResolver
        val docUri = DocumentsContract.buildDocumentUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )
        var directoryUri: Uri? = null
        try {
            directoryUri = DocumentsContract.createDocument(
                contentResolver, docUri, DocumentsContract.Document.MIME_TYPE_DIR, DIRECTORY_NAME
            )
        } catch (e: IOException) {
            println("$TAG ${e.message}")
        }
    }*/
}