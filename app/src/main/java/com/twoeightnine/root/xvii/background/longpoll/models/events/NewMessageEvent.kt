package com.twoeightnine.root.xvii.background.longpoll.models.events

import android.content.Context
import com.google.gson.internal.LinkedTreeMap
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.utils.matchesUserId

data class NewMessageEvent(
        val id: Int,
        private val flags: Int,
        val peerId: Int,
        val timeStamp: Int,
        val text: String,
        val info: MessageInfo
) : BaseLongPollEvent() {

    companion object {
        const val FLAG_UNREAD = 1
        const val FLAG_OUT = 2
    }

    val title
        get() = info.title

    override fun getType() = TYPE_NEW_MESSAGE

    fun isUnread() = (flags and FLAG_UNREAD) > 0

    fun isOut() = (flags and FLAG_OUT) > 0

    fun hasMedia() = info.attachmentsCount > 0 || info.getForwardedCount() > 0

    fun isUser() = peerId.matchesUserId()

    fun hasEmoji() = info.emoji

    fun getResolvedMessage(context: Context?, hideContent: Boolean = false) = when {
        context == null -> text
        text.isNotBlank() && hideContent -> context.getString(R.string.hidden_message)
        text.isNotBlank() -> text
        info.isSticker -> context.getString(R.string.sticker)
        info.attachmentsCount != 0 -> {
            val count = info.attachmentsCount
            context.resources.getQuantityString(R.plurals.attachments, count, count)
        }
        info.getForwardedCount() > 0 -> {
            val count = info.getForwardedCount()
            context.resources.getQuantityString(R.plurals.messages, count, count)
        }
        else -> text
    }

    data class MessageInfo(
            val title: String = "",
            val from: Int = 0,
            val emoji: Boolean = false,
            val forwarded: String = "",
            val attachmentsCount: Int = 0,
            val isSticker: Boolean = false
    ) {
        companion object {

            private const val TITLE = "title"
            private const val FROM = "from"
            private const val EMOJI = "emoji"
            private const val FWD = "fwd"
            private const val ATTACH1_TYPE = "attach1_type"
            private const val TYPE_STICKER = "sticker"

            fun fromLinkedTreeMap(data: LinkedTreeMap<String, Any>): MessageInfo {
                var attachmentsCount = 0
                for (i in 10 downTo 1) {
                    if ("attach$i" in data) {
                        attachmentsCount = i
                        break
                    }
                }
                return MessageInfo(
                        title = (data[TITLE] as? String) ?: "",
                        from = (data[FROM] as? String)?.toInt() ?: 0,
                        emoji = (data[EMOJI] as? String) == "1",
                        forwarded = (data[FWD] as? String) ?: "",
                        attachmentsCount = attachmentsCount,
                        isSticker = (ATTACH1_TYPE in data && data[ATTACH1_TYPE] == TYPE_STICKER)
                )
            }
        }

        fun getForwardedCount() = if (forwarded.isEmpty()) 0 else forwarded.split(",").size
    }
}