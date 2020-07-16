package com.example.recordaudio.recordinglist

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.recordaudio.BaseFragment
import com.example.recordaudio.FileIO
import com.example.recordaudio.R
import com.example.recordaudio.adapters.RecordingAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.bottom_sheet_media_player.*
import kotlinx.android.synthetic.main.fragment_recording_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecordingListFragment : BaseFragment(), RecordingAdapter.OnItemClickListener {

    // https://riptutorial.com/android/example/23916/fetch-audio-mp3-files-from-specific-folder-of-device-or-fetch-all-files

    companion object {
        private val TAG: String = "TAG -- ${RecordingListFragment::class.java.simpleName} -->"
        private const val RESULT_GRANT_READ_FOLDER: Int = 0x1423
    }

    private val viewModel: RecordingListViewModel by viewModel()

    private val adapter: RecordingAdapter by inject()

    private val bottomSheetBehaviour: BottomSheetBehavior<ConstraintLayout> by lazy {
        BottomSheetBehavior.from(bottomSheetPlayer)
    }

    private var mediaPlayer: MediaPlayer? = null

    private var selectedPosition: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        adapter.setOnItemClickListener(this)
        return inflater.inflate(R.layout.fragment_recording_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getObservableStreams()

        initViews()

        Handler().postDelayed({
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.fetchRecordingList(requireContext().contentResolver)
            }
        }, 800)
    }

    override fun onDestroyView() {
        if (selectedPosition != -1) stopPlayingAudio()
        super.onDestroyView()
    }

    override fun onItemClicked(recording: Recording, index: Int) {
        if (selectedPosition == index) {
            pausePlayingAudio(recording, index)
            return
        }

        stopPlayingAudio()

        selectedPosition = index
        playAudio(recording, index)
    }

    private fun initViews() {
        rvList.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                setDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.list_item_divider)!!
                )
            })
        rvList.adapter = adapter
    }

    private fun getObservableStreams() = lifecycleScope.launch {
        viewModel.getRecordingStream().removeObservers(viewLifecycleOwner)
        viewModel.getRecordingStream().observe(viewLifecycleOwner, Observer {
            adapter.setData(it)
            progress.visibility = View.GONE
        })
    }

    private fun playAudio(recording: Recording, index: Int) {
        MediaPlayer().apply {
            setOnCompletionListener {
                stopPlayingAudio(true)
                selectedPosition = -1
            }
            try {
                setDataSource(requireContext(), recording.contentUri)
                prepare()
                start()
            } catch (e: Exception) {
                println("$TAG ${e.message}")
            }
        }.also { mediaPlayer = it }

        adapter.isRecording(recording, index, RecordingAdapter.RECORDING_STATE_PLAY)
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun pausePlayingAudio(recording: Recording, position: Int) = lifecycleScope.launch {
        if (recording.state == RecordingAdapter.RECORDING_STATE_PLAY) {
            mediaPlayer!!.pause()
            adapter.isRecording(recording, position, RecordingAdapter.RECORDING_STATE_PAUSED)
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            mediaPlayer!!.start()
            adapter.isRecording(recording, position, RecordingAdapter.RECORDING_STATE_PLAY)
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun stopPlayingAudio(isCollapse: Boolean = false) = lifecycleScope.launch {
        mediaPlayer?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                println("$TAG ${e.message}")
            }
            adapter.isRecording(
                adapter.getItem(selectedPosition), selectedPosition,
                RecordingAdapter.RECORDING_STATE_STOPPED
            )
            if (isCollapse) bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        }.takeIf { it != null }.also { mediaPlayer = null }
    }

    private fun openDirectory() {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            // Provide read access to files and sub-directories in the user-selected
            // directory.
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker when it loads.
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(FileIO.AudioDir.absolutePath)
                //Uri.parse("${Environment.DIRECTORY_MUSIC}${File.separator}${FileIO.AudioDirName}")
            )
        }

        startActivityForResult(intent, RESULT_GRANT_READ_FOLDER)
    }
}