package com.twoeightnine.root.xvii.model

import android.content.Context
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.model.attachments.Attachment
import com.twoeightnine.root.xvii.model.attachments.isSticker
import com.twoeightnine.root.xvii.model.attachments.photosCount
import com.twoeightnine.root.xvii.utils.matchesChatId
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Message2(

        @SerializedName("id")
        @Expose
        val id: Int = 0,

        @SerializedName("peer_id")
        @Expose
        val peerId: Int = 0,

        @SerializedName("date")
        @Expose
        val date: Int = 0,

        @SerializedName("from_id")
        @Expose
        val fromId: Int = 0,

        @SerializedName("text")
        @Expose
        val text: String = "",

        @SerializedName("out")
        @Expose
        val out: Int = 0,

//        @SerializedName("action")
//        @Expose
//        val action: String? = null,
//
//        @SerializedName("action_text")
//        @Expose
//        val actionText: String? = null,
//
//        @SerializedName("action_mid")
//        @Expose
//        val actionMid: String? = null,

        @SerializedName("fwd_messages")
        @Expose
        val fwdMessages: ArrayList<Message2>? = arrayListOf(),

        @SerializedName("attachments")
        @Expose
        val attachments: ArrayList<Attachment>? = arrayListOf(),

        // ------------------- manually added values
        @SerializedName("read")
        @Expose
        var read: Boolean = false,

        @SerializedName("name")
        @Expose
        var name: String? = null,

        @SerializedName("photo")
        @Expose
        var photo: String? = null
) : Parcelable {

    fun isOut() = out == 1

    fun isSticker() = attachments != null && attachments.isSticker()

    fun hasPhotos() = attachments != null && attachments.photosCount() > 0

    fun isChat() = peerId.matchesChatId()

    fun getResolvedMessage(context: Context?) = when {
        context == null || text.isNotBlank() -> text
        attachments != null && attachments.isSticker() -> context.getString(R.string.sticker)
        attachments != null && attachments.isNotEmpty() -> {
            val count = attachments.size
            context.resources.getQuantityString(R.plurals.attachments, count, count)
        }
        fwdMessages != null && fwdMessages.isNotEmpty() -> {
            val count = fwdMessages.size
            context.resources.getQuantityString(R.plurals.messages, count, count)
        }
        else -> text
    }
}