package com.twoeightnine.root.xvii.network.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.twoeightnine.root.xvii.model.Conversation
import com.twoeightnine.root.xvii.model.Group
import com.twoeightnine.root.xvii.model.Message2
import com.twoeightnine.root.xvii.model.User
import com.twoeightnine.root.xvii.utils.matchesChatId
import com.twoeightnine.root.xvii.utils.matchesGroupId
import com.twoeightnine.root.xvii.utils.matchesUserId

data class MessagesResponse(

        @SerializedName("messages")
        @Expose
        val messages: ListResponse<Message2>,

        @SerializedName("profiles")
        @Expose
        val profiles: ArrayList<User>,

        @SerializedName("groups")
        @Expose
        val groups: ArrayList<Group>,

        @SerializedName("conversations")
        @Expose
        val conversations: ArrayList<Conversation>
) {
    fun getProfileById(id: Int) = profiles.find { it.id == id }

    fun getGroupById(id: Int) = groups.find { it.id == id }

    fun getConversationById(id: Int) = conversations.find { it.peer?.id == id }

    fun getNameForMessage(message: Message2) = when {
        message.peerId.matchesUserId() -> getProfileById(message.fromId)?.fullName
        message.peerId.matchesGroupId() -> getGroupById(-message.peerId)?.name
        message.peerId.matchesChatId() -> getConversationById(message.peerId)?.chatSettings?.title
        else -> null
    }

    fun getPhotoForMessage(message: Message2) = when {
        message.peerId.matchesUserId() -> getProfileById(message.fromId)?.photo100
        message.peerId.matchesGroupId() -> getGroupById(-message.peerId)?.photo100
        message.peerId.matchesChatId() -> getConversationById(message.peerId)?.chatSettings?.photo?.photo100
        else -> null
    }
}