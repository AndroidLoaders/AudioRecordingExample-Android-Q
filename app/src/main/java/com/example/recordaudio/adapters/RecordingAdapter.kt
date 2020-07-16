package com.example.recordaudio.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.recordaudio.R
import com.example.recordaudio.recordinglist.Recording
import kotlinx.android.synthetic.main.list_item_recording.view.*

class RecordingAdapter : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    companion object {
        const val RECORDING_STATE_PLAY: Int = 1
        const val RECORDING_STATE_PAUSED: Int = 2
        const val RECORDING_STATE_STOPPED: Int = 0
    }

    private val recordingList = mutableListOf<Recording>()

    private lateinit var onItemClick: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val customView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_recording, parent, false)
        return RecordingViewHolder(customView)
    }

    override fun getItemCount(): Int = recordingList.size

    override fun onBindViewHolder(holder: RecordingViewHolder, index: Int) =
        holder.bindData(getItem(index))

    interface OnItemClickListener {
        fun onItemClicked(recording: Recording, position: Int)
    }

    fun setOnItemClickListener(onItemClick: OnItemClickListener) {
        this.onItemClick = onItemClick
    }

    fun setData(data: MutableList<Recording>) {
        recordingList.clear()
        recordingList.addAll(data)
        notifyDataSetChanged()
    }

    fun getItem(index: Int): Recording = recordingList[index]

    fun isRecording(recording: Recording, index: Int, state: Int) {
        recording.state = state
        notifyItemChanged(index)
    }

    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        private val ivPlayPause: ImageView = itemView.ivPlayPause

        init {
            itemView.rootLayout.setOnClickListener(this)
            ivPlayPause.setOnClickListener(this)
        }

        fun bindData(recording: Recording) {
            itemView.tvName.text = recording.title
            itemView.tvCreatedOn.text = recording.duration
            ivPlayPause.setImageResource(
                if (recording.state == RECORDING_STATE_PLAY) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        override fun onClick(view: View?) {
            val position = adapterPosition
            when (view?.id) {
                R.id.rootLayout, R.id.ivPlayPause -> {
                    val recording = recordingList[position]
                    onItemClick.onItemClicked(recording, position)
                }
            }
        }
    }
}