package com.nema.eduup.discussions.people

import android.net.Uri
import androidx.lifecycle.*
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.repository.FirebaseStorageUtil
import androidx.lifecycle.LifecycleOwner

import androidx.lifecycle.DefaultLifecycleObserver
import com.google.firebase.firestore.FirebaseFirestore
import com.nema.eduup.auth.User
import com.nema.eduup.discussions.groups.GroupChatChannel
import com.nema.eduup.repository.DiscussionRepository
import kotlinx.coroutines.launch


class PeopleFragmentViewModel: ViewModel(), DefaultLifecycleObserver {


    private lateinit var groupsListenerRegistration: ListenerRegistration
    private lateinit var peopleListenerRegistration: ListenerRegistration
    private var groups : MutableLiveData<List<GroupChatChannel>> = MutableLiveData()
    private var users : MutableLiveData<List<User>> = MutableLiveData()
    private var firebaseStorageUtil = FirebaseStorageUtil
    private var discussionRepository = DiscussionRepository

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
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

    fun users(): LiveData<List<User>>{
        viewModelScope.launch {
            peopleListenerRegistration = discussionRepository.addUsersListener {
                users.value = it
            }
        }
        return users
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        discussionRepository.removeListener(peopleListenerRegistration)
    }

}