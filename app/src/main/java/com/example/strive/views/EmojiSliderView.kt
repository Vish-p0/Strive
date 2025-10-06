package com.example.strive.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.strive.R
import com.example.strive.util.EmojiInfo
import com.example.strive.util.EmojiPalette
import com.google.android.material.card.MaterialCardView

/**
 * Horizontal emoji slider with snapping for quick mood logging
 */
class EmojiSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val snapHelper = PagerSnapHelper()
    var onEmojiSelectedListener: ((EmojiInfo) -> Unit)? = null
    private var onEmojiLongPressListener: ((EmojiInfo) -> Unit)? = null

    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        snapHelper.attachToRecyclerView(this)
        adapter = EmojiSliderAdapter()

        // Add peek effect by setting clip to padding false and padding
        clipToPadding = false
        setPadding(
            resources.getDimensionPixelSize(R.dimen.spacing_xl),
            0,
            resources.getDimensionPixelSize(R.dimen.spacing_xl),
            0
        )
    }

    fun setOnEmojiLongPressListener(listener: (EmojiInfo) -> Unit) {
        onEmojiLongPressListener = listener
    }

    private inner class EmojiSliderAdapter : Adapter<EmojiViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_emoji_slider, parent, false)
            return EmojiViewHolder(view)
        }

        override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
            holder.bind(EmojiPalette.EMOJIS[position])
        }

        override fun getItemCount(): Int = EmojiPalette.EMOJIS.size
    }

    private inner class EmojiViewHolder(itemView: View) : ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardEmoji)
        private val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
        private val tvEmojiName: TextView = itemView.findViewById(R.id.tvEmojiName)

        fun bind(emojiInfo: EmojiInfo) {
            tvEmoji.text = emojiInfo.emoji
            tvEmojiName.text = emojiInfo.name

            // Set content description for accessibility
            itemView.contentDescription = emojiInfo.name

            cardView.setOnClickListener {
                // Add scale animation
                cardView.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction {
                        cardView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                    }
                    .start()

                onEmojiSelectedListener?.invoke(emojiInfo)
            }

            cardView.setOnLongClickListener {
                onEmojiLongPressListener?.invoke(emojiInfo)
                true
            }
        }
    }
}

