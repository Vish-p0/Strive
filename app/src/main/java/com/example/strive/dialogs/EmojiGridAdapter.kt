package com.example.strive.dialogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.strive.R
import com.example.strive.util.EmojiInfo
import com.example.strive.util.EmojiPalette
import com.google.android.material.card.MaterialCardView

class EmojiGridAdapter(
    private val emojis: List<EmojiInfo>,
    private val onEmojiClick: (EmojiInfo) -> Unit
) : RecyclerView.Adapter<EmojiGridAdapter.EmojiViewHolder>() {

    private var selectedEmoji: EmojiInfo? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emoji_grid, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(emojis[position])
    }

    override fun getItemCount() = emojis.size

    fun setSelectedEmoji(emojiInfo: EmojiInfo) {
        val oldSelected = selectedEmoji
        selectedEmoji = emojiInfo
        
        // Notify old selection to update
        oldSelected?.let { old ->
            val oldIndex = emojis.indexOf(old)
            if (oldIndex != -1) notifyItemChanged(oldIndex)
        }
        
        // Notify new selection to update
        val newIndex = emojis.indexOf(emojiInfo)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    inner class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val emojiText: TextView = itemView.findViewById(R.id.tvEmoji)

        fun bind(emojiInfo: EmojiInfo) {
            emojiText.text = emojiInfo.emoji
            
            // Update selection state
            if (selectedEmoji == emojiInfo) {
                cardView.strokeWidth = 4
                cardView.strokeColor = itemView.context.getColor(android.R.color.holo_blue_light)
            } else {
                cardView.strokeWidth = 0
            }
            
            cardView.setOnClickListener {
                onEmojiClick(emojiInfo)
            }
        }
    }
}