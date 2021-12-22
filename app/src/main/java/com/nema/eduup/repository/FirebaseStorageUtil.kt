package com.nema.eduup.repository

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.nema.eduup.utils.AppConstants
import java.util.*
import com.google.android.gms.tasks.OnSuccessListener

import com.google.firebase.storage.ListResult
import kotlin.collections.ArrayList


object FirebaseStorageUtil {
    private val TAG = FirebaseStorageUtil::class.qualifiedName
    private val storageInstance: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    fun uploadImageToCloudStorage(imageBytes: ByteArray, storagePath: String, onComplete: (Uri) -> Unit) {
        val ref = storageInstance.getReference("$storagePath/${UUID.nameUUIDFromBytes(imageBytes)}")
        ref.putBytes(imageBytes)
            .addOnSuccessListener {taskSnapshot ->
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        onComplete(uri)
                        }
                    }
            .addOnFailureListener{ exception ->

                Log.e(TAG, exception.message, exception)
            }
    }

    fun uploadFileToCloudStorage( fileURI: Uri?,fileExtension: String, storagePath: String, onComplete: (Uri?) -> Unit) {
        val noteSRef = storageInstance.reference.child(
            "$storagePath${System.currentTimeMillis()}.${fileExtension}"
        )

        noteSRef.putFile(fileURI!!).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata!!.reference!!.downloadUrl
                .addOnSuccessListener { uri ->
                    onComplete(uri)
                }
                .addOnFailureListener{ exception ->
                    onComplete(null)
                    Log.e(TAG, exception.message, exception)
                }
        }
    }

    fun getImageDownloadUrls(storageRef: String, onComplete: (String) -> Unit) {
        val imageUrls = arrayListOf<String>()
        storageInstance.reference.child(storageRef).listAll()
            .addOnSuccessListener {
                for (file in it.items) {
                    file.downloadUrl.addOnSuccessListener { uri ->
                        onComplete(uri.toString())
                    }
                }
            }

    }


}