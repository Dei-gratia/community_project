package com.nema.eduup.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.nema.eduup.auth.User
import com.nema.eduup.utils.AppConstants

object UserRepository {
    private val TAG = AuthRepository::class.qualifiedName
    private lateinit var firebaseUser: FirebaseUser
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserDocRef by lazy { firestoreInstance.collection(AppConstants.USERS).document(
        getCurrentUserID()
    ) }


    fun getCurrentUserID(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = "-1"
        if (currentUser != null) {
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun getCurrentUser(onComplete: (User) -> Unit) {
       currentUserDocRef.get()
            .addOnSuccessListener {
                val user = it.toObject(User::class.java)!!
                onComplete(user)
            }
    }

    fun updateUserProfileData(userHashMap: HashMap<String, Any>, onComplete: (User) -> Unit) {
        firestoreInstance.collection(AppConstants.USERS)
            .document(getCurrentUserID())
            .update(userHashMap)
            .addOnSuccessListener {
                getCurrentUser {
                    onComplete(it)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error while updating user details.", e)
            }
    }

    fun getUser(userId: String, onComplete: (User) -> Unit) {
        firestoreInstance.collection(AppConstants.USERS).document(userId).get()
            .addOnSuccessListener {
                val user = it.toObject(User::class.java)!!
                Log.d("User get", user.toString())
                onComplete(user)
            }
    }



    fun setFCMRegistrationTokens(registrationTokens: MutableList<String>) {
        currentUserDocRef.update(mapOf("registrationTokens" to registrationTokens))
    }

    fun getFCMRegistrationTokens(onComplete: (tokens: MutableList<String>) -> Unit) {
        currentUserDocRef.get().addOnSuccessListener {
            val user = it.toObject(User::class.java)!!
            onComplete(user.registrationTokens)
        }
    }

}