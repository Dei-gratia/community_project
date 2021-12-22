package com.nema.eduup.activities.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.repository.FirebaseStorageUtil
import com.nema.eduup.repository.FirestoreUtil
import java.util.*
import androidx.lifecycle.LifecycleOwner

import androidx.annotation.NonNull
import androidx.lifecycle.DefaultLifecycleObserver
import com.google.firebase.firestore.FirebaseFirestore
import com.nema.eduup.models.*
import com.nema.eduup.utils.AppConstants
import kotlinx.coroutines.launch


class NewDiscussionActivityViewModel: ViewModel(), DefaultLifecycleObserver {


    private lateinit var groupsListenerRegistration: ListenerRegistration
    private lateinit var peopleListenerRegistration: ListenerRegistration
    private var groups : MutableLiveData<List<GroupChatChannel>> = MutableLiveData()
    private var users : MutableLiveData<List<User>> = MutableLiveData()
    private var firestoreUtil = FirestoreUtil


    private val firestoreInstance by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun createGroup(name: String, groupImageUrl: String, securityMode: String) {
        viewModelScope.launch {
            firestoreUtil.createGroup(name, groupImageUrl, securityMode) {

            }
        }
    }

    fun getGroups(): LiveData<List<GroupChatChannel>> {
        viewModelScope.launch {
            groupsListenerRegistration = firestoreUtil.addGroupsListener {
                groups.value = it
            }
        }
        return groups
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

    fun users(): LiveData<List<User>>{
        viewModelScope.launch {
            peopleListenerRegistration = firestoreUtil.addUsersListener {
                users.value = it
            }
        }
        return users
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        firestoreUtil.removeListener(groupsListenerRegistration)
        firestoreUtil.removeListener(peopleListenerRegistration)
    }

}