package com.nema.eduup.profile

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.*
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.repository.FirebaseStorageUtil
import java.util.*
import androidx.lifecycle.LifecycleOwner

import androidx.lifecycle.DefaultLifecycleObserver
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.auth.User
import com.nema.eduup.repository.UserRepository
import com.nema.eduup.utils.AppConstants
import kotlinx.coroutines.launch


class UserProfileViewModel(private val app: Application): AndroidViewModel(app) , DefaultLifecycleObserver {

    private val sharedPreferences: SharedPreferences = app.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE)
    private lateinit var peopleListenerRegistration: ListenerRegistration
    private var users : MutableLiveData<List<User>> = MutableLiveData()
    private var userRepository = UserRepository
    private var firebaseStorageUtil = FirebaseStorageUtil


    private val firestoreInstance by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun updateUserProfileData(userHashMap: HashMap<String, Any>, onComplete: (User) -> Unit){
        viewModelScope.launch {
            userRepository.updateUserProfileData(userHashMap) {
                onComplete(it)
            }
        }
    }

    fun getCurrentUserDetails(onComplete: (User) -> Unit) {
        viewModelScope.launch {
            userRepository.getCurrentUser {
                onComplete(it)
                storeUserDetails(it)
            }
        }
    }


    fun uploadFile(fileURI: Uri?, fileExtension: String, storagePath: String, onComplete: (Uri?) -> Unit){
        firebaseStorageUtil.uploadFileToCloudStorage(fileURI, fileExtension, storagePath) {
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

    fun storeUserDetails(userInfo: User) {
        val gson = Gson()
        val json = gson.toJson(userInfo)
        sharedPreferences.edit().putString(AppConstants.CURRENT_USER, json).apply()
    }

    fun downloadImage(imageUrl: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            firebaseStorageUtil.downloadImageFromCloudStorage(imageUrl) {
                onComplete(it)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
    }

}