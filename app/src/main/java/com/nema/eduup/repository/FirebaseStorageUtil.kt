package com.nema.eduup.repository

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.nema.eduup.utils.AppConstants
import java.util.*
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.ListenerRegistration

import com.google.firebase.storage.ListResult
import com.google.firebase.storage.ktx.storageMetadata
import java.io.File
import kotlin.collections.ArrayList


object FirebaseStorageUtil {
    private val TAG = FirebaseStorageUtil::class.qualifiedName
    private val storageInstance: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private var uploadProgress: MutableLiveData<Double> = MutableLiveData()

    fun uploadImageToCloudStorage(imageBytes: ByteArray, storagePath: String, onListen: (LiveData<Double>) -> Unit,onComplete: (Uri) -> Unit){
        val ref = storageInstance.getReference(storagePath)
        val metadata = storageMetadata {

        }
        ref.putBytes(imageBytes)
            .addOnProgressListener { taskSnapshot ->
                val progress: Double =
                    100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                uploadProgress.value = progress
                onListen(uploadProgress)
            }
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

    fun downloadImageFromCloudStorage(imageUrl: String, onComplete: (String) -> Unit) {
        val imageRef = storageInstance.getReferenceFromUrl(imageUrl)
        val name = imageRef.name.split(".")[0]
        val fileExtension = imageRef.name.split(".")[1]
        val localImage = File.createTempFile(name, fileExtension)
        imageRef.getFile(localImage)
            .addOnSuccessListener {
                val imageUri = Uri.fromFile(localImage)
                onComplete(imageUri.toString())

            }
            .addOnFailureListener{ exception ->
                Log.e(TAG, exception.message, exception)
            }
    }

    fun getImageDownloadUrls(storageRef: String, onComplete: (String) -> Unit) {
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