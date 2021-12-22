package com.nema.eduup.firebase

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.nema.eduup.utils.AppConstants
import java.net.URL
import java.util.*

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

    fun uploadFileToCloudStorage(activity: Activity, fileURI: Uri?, storagePath: String, onComplete: (Uri?) -> Unit) {
        val noteSRef = storageInstance.reference.child(
            "$storagePath${System.currentTimeMillis()}.${AppConstants.getFileExtension(activity, fileURI)}"
        )

        noteSRef.putFile(fileURI!!).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata!!.reference!!.downloadUrl
                .addOnSuccessListener { uri ->
                    onComplete(uri)
                }
                .addOnFailureListener{ exception ->
                    onComplete(null)
                    Log.e(activity.javaClass.simpleName, exception.message, exception)
                }
        }
    }
}