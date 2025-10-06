package com.example.strive.util

/**
 * Emoji palette with 20 emojis, their names, and mood scores (1-5)
 */
data class EmojiInfo(val emoji: String, val name: String, val score: Int)

object EmojiPalette {
    val EMOJIS = listOf(
        // Good moods first (scores 4-5)
        EmojiInfo("😁", "Good", 5),
        EmojiInfo("😃", "Very Happy", 5),
        EmojiInfo("🤩", "Excited", 5),
        EmojiInfo("☺️", "Happy", 4),
        EmojiInfo("🙂", "Slightly Happy", 4),
        EmojiInfo("😮‍💨", "Relieved", 4),
        // Neutral moods (score 3)
        EmojiInfo("😐", "Neutral", 3),
        EmojiInfo("😶", "Meh", 3),
        EmojiInfo("🤔", "Thoughtful", 3),
        EmojiInfo("😧", "Surprised", 3),
        EmojiInfo("🙂‍↕️", "Relieved/Anxious", 3),
        // Lower moods (score 2)
        EmojiInfo("😵‍💫", "Confused", 2),
        EmojiInfo("😞", "Disappointed", 2),
        EmojiInfo("😫", "Anxious", 2),
        EmojiInfo("😓", "Tired", 2),
        EmojiInfo("😟", "Embarrassed", 2),
        // Sad moods last (score 1)
        EmojiInfo("😢", "Sad", 1),
        EmojiInfo("😭", "Crying", 1),
        EmojiInfo("😤", "Angry", 1),
        EmojiInfo("😡", "Furious", 1)
    )

    private val emojiToInfoMap = EMOJIS.associateBy { it.emoji }

    fun getEmojiInfo(emoji: String): EmojiInfo? = emojiToInfoMap[emoji]

    fun getEmojiName(emoji: String): String {
        return emojiToInfoMap[emoji]?.name ?: "Mood"
    }

    fun getEmojiScore(emoji: String): Int {
        return emojiToInfoMap[emoji]?.score ?: 3
    }
}

