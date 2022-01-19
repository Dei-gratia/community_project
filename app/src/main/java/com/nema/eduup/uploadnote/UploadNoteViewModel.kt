package com.nema.eduup.uploadnote

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nema.eduup.repository.FirebaseStorageUtil

class UploadNoteViewModel(app: Application): AndroidViewModel(app), DefaultLifecycleObserver {

    private val firebaseStorageUtil = FirebaseStorageUtil

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun uploadFile(fileURI: Uri?, fileExtension: String, storagePath: String, onComplete: (Uri?) -> Unit){
       firebaseStorageUtil.uploadFileToCloudStorage( fileURI, fileExtension, storagePath) {
            onComplete(it)
        }
    }


}