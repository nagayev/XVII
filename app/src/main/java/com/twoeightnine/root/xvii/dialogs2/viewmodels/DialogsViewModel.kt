package com.twoeightnine.root.xvii.dialogs2.viewmodels

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.twoeightnine.root.xvii.App.Companion.context
import com.twoeightnine.root.xvii.background.longpoll.models.events.*
import com.twoeightnine.root.xvii.db.AppDb
import com.twoeightnine.root.xvii.dialogs2.models.Dialog
import com.twoeightnine.root.xvii.lg.Lg
import com.twoeightnine.root.xvii.managers.Prefs
import com.twoeightnine.root.xvii.model.WrappedLiveData
import com.twoeightnine.root.xvii.model.WrappedMutableLiveData
import com.twoeightnine.root.xvii.model.Wrapper
import com.twoeightnine.root.xvii.network.ApiService
import com.twoeightnine.root.xvii.network.response.BaseResponse
import com.twoeightnine.root.xvii.network.response.ConversationsResponse
import com.twoeightnine.root.xvii.utils.*
import javax.inject.Inject

class DialogsViewModel(
        private val api: ApiService,
        private val appDb: AppDb
) : ViewModel() {

    init {
        EventBus.subscribeLongPollEventReceived { event ->
            when (event) {
                is OnlineEvent -> changeStatus(event.userId, true)
                is OfflineEvent -> changeStatus(event.userId, false)
                is ReadOutgoingEvent -> changeReadState(event.peerId)
                is ReadIncomingEvent -> changeReadState(event.peerId)
                is NewMessageEvent -> addNewMessage(event)
            }
        }
    }

    private val dialogsLiveData = WrappedMutableLiveData<ArrayList<Dialog>>()

    fun getDialogs() = dialogsLiveData as WrappedLiveData<ArrayList<Dialog>>

    @SuppressLint("CheckResult")
    fun loadDialogs(offset: Int = 0) {
        if (!isOnline()) {
            if (offset == 0) {
                appDb.dialogsDao().getDialogs()
                        .compose(applySingleSchedulers())
                        .subscribe({ dialogs ->
                            dialogsLiveData.value = Wrapper(ArrayList(dialogs))
                        }, {
                            it.printStackTrace()
                            lw("error loading from cache: ${it.message}")
                            dialogsLiveData.value = Wrapper(error = it.message)
                        })
            }
            return
        }
        api.getConversations(COUNT_CONVERSATIONS, offset)
                .map { convertToDialogs(it) }
                .subscribeSmart({ dialogs ->
                    val existing = if (offset == 0) {
                        arrayListOf()
                    } else {
                        dialogsLiveData.value?.data ?: arrayListOf()
                    }
                    dialogsLiveData.value = Wrapper(existing.apply { addAll(dialogs) })
                    saveDialogsAsync(dialogs)
                }, ::onErrorOccurred)
    }

    fun readDialog(dialog: Dialog) {
        api.markAsRead("${dialog.messageId}")
                .subscribeSmart({}, ::onErrorOccurred)
    }

    fun deleteDialog(dialog: Dialog) {
        api.deleteConversation(dialog.peerId, COUNT_DELETE)
                .subscribeSmart({
                    dialogsLiveData.value?.data?.removeAll { it.peerId == dialog.peerId }
                    notifyDialogsChanged()
                    removeDialog(dialog)
                }, ::onErrorOccurred)
    }

    fun muteDialog(d: Dialog) {
        val dialog = dialogsLiveData.value?.data
                ?.firstOrNull { it.peerId == d.peerId } ?: return

        val muteList = Prefs.muteList
        dialog.isMute = !dialog.isMute
        if (dialog.isMute) {
            muteList.add(dialog.peerId)
        } else {
            muteList.remove(dialog.peerId)
        }
        Prefs.muteList = muteList
        notifyDialogsChanged()
        saveDialogAsync(dialog)
    }

    private fun convertToDialogs(resp: BaseResponse<ConversationsResponse>): BaseResponse<ArrayList<Dialog>> {
        val dialogs = arrayListOf<Dialog>()
        val response = resp.response
        val muteList = Prefs.muteList
        response?.items?.forEach { dm ->
            val message = dm.lastMessage
            dialogs.add(Dialog(
                    message.peerId,
                    message.id,
                    response.getTitleFor(dm) ?: "???",
                    response.getPhotoFor(dm),
                    message.getResolvedMessage(context),
                    message.date,
                    message.isOut(),
                    dm.conversation.isRead(),
                    dm.conversation.unreadCount,
                    response.isOnline(dm),
                    message.peerId in muteList
            ))
        }
        return BaseResponse(dialogs, resp.error)
    }

    private fun changeStatus(peerId: Int, isOnline: Boolean) {
        val dialog = dialogsLiveData.value?.data
                ?.find { it.peerId == peerId } ?: return

        dialog.isOnline = isOnline
        notifyDialogsChanged()
        saveDialogAsync(dialog)
    }

    private fun changeReadState(peerId: Int) {
        val dialog = dialogsLiveData.value?.data
                ?.find { it.peerId == peerId } ?: return

        with(dialog) {
            isRead = true
            unreadCount = 0
        }
        notifyDialogsChanged()
        saveDialogAsync(dialog)
    }

    private fun addNewMessage(event: NewMessageEvent) {
        val dialog = dialogsLiveData.value?.data
                ?.find { event.peerId == it.peerId }
        if (dialog != null) { // existing dialog
            with(dialog) {
                messageId = event.id
                isRead = false
                isOut = event.isOut()
                text = event.getResolvedMessage(context)
                timeStamp = event.timeStamp
                unreadCount++
            }
            notifyDialogsChanged()
            saveDialogAsync(dialog)
        } else { // new dialog
            api.getConversations(COUNT_NEW_CONVERSATION)
                    .map { convertToDialogs(it) }
                    .subscribeSmart({ dialogs ->
                        val newDialog = dialogs.find { it.peerId == event.peerId }
                                ?: return@subscribeSmart

                        dialogsLiveData.value?.data?.add(newDialog)
                        notifyDialogsChanged()
                        saveDialogAsync(newDialog)
                    }, ::onErrorOccurred)
        }
    }

    private fun notifyDialogsChanged() {
        val dialogs = dialogsLiveData.value?.data ?: return
        dialogsLiveData.value = Wrapper(ArrayList(dialogs.sortedByDescending { it.timeStamp }))
    }

    private fun onErrorOccurred(error: String) {
        dialogsLiveData.value = Wrapper(error = error)
    }

    @SuppressLint("CheckResult")
    private fun saveDialogsAsync(dialogs: ArrayList<Dialog>) {
        appDb.dialogsDao().insertDialogs(*dialogs.toTypedArray())
                .compose(applyCompletableSchedulers())
                .subscribe({
                    l("cached list")
                }, {
                    it.printStackTrace()
                    lw("cache list error: ${it.message}")
                })
    }

    @SuppressLint("CheckResult")
    private fun saveDialogAsync(dialog: Dialog) {
        appDb.dialogsDao().insertDialog(dialog)
                .compose(applyCompletableSchedulers())
                .subscribe({
                    l("cached one dialog")
                }, {
                    it.printStackTrace()
                    lw("cache one dialog error: ${it.message}")
                })
    }

    @SuppressLint("CheckResult")
    private fun removeDialog(dialog: Dialog) {
        appDb.dialogsDao().removeDialog(dialog)
                .compose(applySingleSchedulers())
                .subscribe({
                    l("removed from cache")
                }, {
                    it.printStackTrace()
                    lw("remove from cache err: ${it.message}")
                })
    }

    private fun l(s: String) {
        Lg.i("[dialogs] $s")
    }

    private fun lw(s: String) {
        Lg.wtf("[dialogs] $s")
    }

    companion object {
        const val COUNT_NEW_CONVERSATION = 3
        const val COUNT_CONVERSATIONS = 200
        const val COUNT_DELETE = 10000
    }

    class Factory @Inject constructor(
            private val api: ApiService,
            private val appDb: AppDb
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DialogsViewModel::class.java)) {
                return DialogsViewModel(api, appDb) as T
            }
            throw IllegalArgumentException("Unknown $modelClass")
        }
    }
}