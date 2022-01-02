package com.nema.eduup.repository

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nema.eduup.auth.User
import com.nema.eduup.utils.AppConstants


object AuthRepository {

    private val TAG = AuthRepository::class.qualifiedName
    private lateinit var firebaseUser: FirebaseUser
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserDocRef by lazy { firestoreInstance.collection(AppConstants.USERS).document(
        getCurrentUserID()
    ) }

    fun registerUserWithPasswordAndEmail(userDetails: User, password: String, onComplete: (User?, String?) -> Unit){
        val email = userDetails.email
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser = task.result!!.user!!

                    val user = User(
                        firebaseUser.uid,
                        userDetails.firstNames,
                        userDetails.familyName,
                        userDetails.nickname,
                        userDetails.email
                    )

                    addUserToFirestoreDatabase(user) {
                        onComplete(user, null)
                    }

                }
                else {
                    onComplete(null, task.exception!!.message.toString())
                    Log.e(TAG, task.exception!!.message.toString())
                }
            }

    }

    fun signInWithEmailAndPassword(email: String, password: String, onComplete: (User?, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                   getCurrentUser {
                        onComplete(it, null)
                    }
                } else {
                    onComplete(null, task.exception!!.message)
                    Log.d(TAG,  task.exception!!.message.toString())
                }
            }
    }


    fun signInWithGoogleAuthCredential(account: GoogleSignInAccount, onComplete: (User?, Boolean?, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(account.idToken,null)
        auth.signInWithCredential(credential).addOnCompleteListener {task->
            if(task.isSuccessful) {
                val isNew = task.result!!.additionalUserInfo!!.isNewUser
                firebaseUser = task.result!!.user!!
                val firstNames = account.givenName.toString()
                val familyName = account.familyName.toString()
                val email = account.email.toString()
                val user = User(
                    firebaseUser.uid,
                    firstNames,
                    familyName,
                    "$firstNames $familyName",
                    email
                )

                if (isNew){
                    onComplete(user, true, null)
                }
                else{
                    getCurrentUser {
                        onComplete(it, false, null)
                    }
                }
            }
            else {
                onComplete(null, null, task.exception!!.message.toString())
            }
        }

    }

    fun deleteUser() {
        firebaseUser.delete()
    }

    fun addUserToFirestoreDatabase(userInfo: User, onComplete: () -> Unit) {
        currentUserDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()) {
                firestoreInstance.collection(AppConstants.USERS)
                    .document(userInfo.id)
                    .set(userInfo, SetOptions.merge())
                    .addOnSuccessListener {
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error while registering the user", e)
                    }
            }
            else
                onComplete()
        }

    }

    private fun getCurrentUserID(): String {
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

    fun resetPassword(email: String, onComplete: (String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener {task ->
                if (task.isSuccessful) {
                   onComplete(null)
                }
                else {
                    Log.e(TAG, task.exception!!.message.toString())
                    onComplete(task.exception!!.message.toString())
                }
            }
    }



}