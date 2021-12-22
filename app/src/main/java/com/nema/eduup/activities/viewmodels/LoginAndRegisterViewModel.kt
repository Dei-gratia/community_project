package com.nema.eduup.activities.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.nema.eduup.models.User
import com.nema.eduup.repository.FirestoreUtil

class LoginAndRegisterViewModel: ViewModel() {

    private val TAG = LoginAndRegisterViewModel::class.qualifiedName
    private var firestoreUtil = FirestoreUtil

    private val auth by lazy { FirebaseAuth.getInstance() }

    fun registerUserWithPasswordAndEmail(userDetails: User, password: String, onComplete: () -> Unit) {
        firestoreUtil.registerUserWithPasswordAndEmail(userDetails,password) {
            onComplete()
        }
    }

    fun addUserToFirestore(user: User, onComplete: () -> Unit) {
        firestoreUtil.addUserToFirestoreDatabase(user){
            onComplete()
        }
    }

    fun googleSignUpSuccess(account: GoogleSignInAccount, onComplete: (isNew: Boolean, user: User, error: String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(account.idToken,null)
        auth.signInWithCredential(credential).addOnCompleteListener { task->
            if(task.isSuccessful) {
                val isNew = task.result!!.additionalUserInfo!!.isNewUser
                val firebaseUser: FirebaseUser = task.result!!.user!!
                val firstNames = account.givenName.toString()
                val familyName = account.familyName.toString()
                val email = account.email.toString()
                val user = User(
                    firebaseUser.uid,
                    firstNames,
                    familyName,
                    email
                )

                if (isNew){
                    onComplete(true, user, null)
                }
                else{
                    onComplete(false, user, null)
                }
            }
            else {
                onComplete(false, User(), task.exception!!.message.toString() )
                Log.d(TAG, task.exception!!.message.toString())
            }
        }

    }

}