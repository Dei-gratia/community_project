package com.nema.eduup.activities.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.models.FileMessage
import com.nema.eduup.models.ImageMessage
import com.nema.eduup.models.Message
import com.nema.eduup.repository.FirebaseStorageUtil
import com.nema.eduup.repository.FirestoreUtil
import java.util.*
import androidx.lifecycle.LifecycleOwner

import androidx.annotation.NonNull
import androidx.lifecycle.DefaultLifecycleObserver
import com.google.firebase.firestore.FirebaseFirestore
import com.nema.eduup.models.GroupChatChannel
import com.nema.eduup.utils.AppConstants
import kotlinx.coroutines.launch


class ChatActivityViewModel: ViewModel(), DefaultLifecycleObserver {


    private lateinit var messagesListener: ListenerRegistration
    private var messages : MutableLiveData<List<Message>> = MutableLiveData()
    private var firestoreUtil = FirestoreUtil


    private val firestoreInstance by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun getOrCreateChatChannel(otherUserId: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            firestoreUtil.getOrCreateChatChannel(otherUserId) { newChannelId ->
                onComplete(newChannelId)
            }
        }
    }

    fun getChannelMessages(collection: CollectionReference): LiveData<List<Message>>{
        viewModelScope.launch {
            messagesListener = firestoreUtil.addMessagesListener(collection) {
                messages.value = it
            }
        }
        return messages
    }

    fun sendMessage(message: Message, collection: CollectionReference, onComplete: () -> Unit) {
        viewModelScope.launch {
            firestoreUtil.sendMessage(message, collection) {
                onComplete()
            }
        }
    }

    fun uploadFile(fileURI: Uri?, fileExtension: String, storagePath: String, onComplete: (Uri?) -> Unit){
        FirebaseStorageUtil.uploadFileToCloudStorage(fileURI, fileExtension, storagePath) {
            onComplete(it)
            }
        }

    fun uploadImage(selectedImageBytes: ByteArray, storagePath: String, onComplete: (Uri?) -> Unit) {
        FirebaseStorageUtil.uploadImageToCloudStorage(selectedImageBytes, storagePath) {
            onComplete(it)
        }
    }

    fun addUserToGroup(channelId: String, onComplete: () -> Unit) {
        firestoreUtil.addUserToGroup(channelId){
            onComplete()
        }
    }

    fun getGroup(channelId: String, onComplete: (GroupChatChannel) -> Unit) {
        firestoreUtil.getGroup(channelId) {
            onComplete(it)
        }
    }


    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        firestoreUtil.removeListener(messagesListener)
    }

}