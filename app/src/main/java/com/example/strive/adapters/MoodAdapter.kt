package com.example.strive.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.strive.R
import com.example.strive.models.MoodEntry

class MoodAdapter(
    private val onMoodClick: (MoodEntry) -> Unit
) : ListAdapter<MoodEntry, MoodAdapter.MoodViewHolder>(MoodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_entry, parent, false)
        return MoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emojiText: TextView = itemView.findViewById(R.id.tvEmoji)
        private val noteText: TextView = itemView.findViewById(R.id.tvNote)
        private val timeText: TextView = itemView.findViewById(R.id.tvTime)
        private val scoreText: TextView = itemView.findViewById(R.id.tvScore)

        fun bind(moodEntry: MoodEntry) {
            emojiText.text = moodEntry.emoji
            noteText.text = moodEntry.note ?: "No note"
            scoreText.text = "${moodEntry.score}/5"
            
            // Format timestamp from Long to readable format
            val formatter = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            timeText.text = formatter.format(java.util.Date(moodEntry.timestamp))
            
            itemView.setOnClickListener {
                onMoodClick(moodEntry)
            }
        }
    }

    class MoodDiffCallback : DiffUtil.ItemCallback<MoodEntry>() {
        override fun areItemsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean {
            return oldItem == newItem
        }
    }
}