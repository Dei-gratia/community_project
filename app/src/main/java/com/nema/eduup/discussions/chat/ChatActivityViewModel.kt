package com.nema.eduup.discussions.chat

import android.net.Uri
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.repository.FirebaseStorageUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import com.nema.eduup.repository.DiscussionRepository
import kotlinx.coroutines.launch

class ChatActivityViewModel: ViewModel(), DefaultLifecycleObserver {
    private lateinit var messagesListener: ListenerRegistration
    private var messages : MutableLiveData<List<Message>> = MutableLiveData()
    private var discussionRepository = DiscussionRepository
    private var firebaseStorageUtil = FirebaseStorageUtil

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun getOrCreateChatChannel(otherUserId: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            discussionRepository.getOrCreateChatChannel(otherUserId) { newChannelId ->
                onComplete(newChannelId)
            }
        }
    }

    fun getChannelMessages(collection: CollectionReference): LiveData<List<Message>>{
        viewModelScope.launch {
            messagesListener = discussionRepository.addMessagesListener(collection) {
                messages.value = it
            }
        }
        return messages
    }

    fun sendMessage(message: Message, collection: CollectionReference, onComplete: () -> Unit) {
        viewModelScope.launch {
            discussionRepository.sendMessage(message, collection) {
                onComplete()
            }
        }
    }

    fun uploadFile(fileURI: Uri?, fileExtension: String, storagePath: String, onComplete: (Uri?) -> Unit){
        firebaseStorageUtil.uploadFileToCloudStorage( fileURI, fileExtension, storagePath) {
            onComplete(it)
        }
    }

    fun uploadImage(selectedImageBytes: ByteArray, storagePath: String, onListen: (LiveData<Double>) -> Unit, onComplete: (Uri?) -> Unit) {
        viewModelScope.launch {
            firebaseStorageUtil.uploadImageToCloudStorage(selectedImageBytes, storagePath, { uploadProgress ->
                onListen(uploadProgress)
            }) {uri ->
                onComplete(uri)
            }
        }
    }

    fun addUserToGroup(channelId: String, onComplete: () -> Unit) {
        discussionRepository.addUserToGroup(channelId){
            onComplete()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        discussionRepository.removeListener(messagesListener)
    }

}