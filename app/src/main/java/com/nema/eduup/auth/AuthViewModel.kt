package com.nema.eduup.auth

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import com.nema.eduup.repository.AuthRepository
import com.nema.eduup.utils.AppConstants
import kotlinx.coroutines.launch


class AuthViewModel (private val app: Application): AndroidViewModel(app) {

    private val authRepository = AuthRepository
    private val sharedPreferences: SharedPreferences = app.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE)

    fun registerUserWithPasswordAndEmail(userDetails: User, password: String, onComplete: (User?, String?) -> Unit) {
        authRepository.registerUserWithPasswordAndEmail(userDetails,password) { user, error_message ->
            onComplete(user, error_message)
        }
    }

    fun singInWithEmailAndPassword(email: String, password: String, onComplete: (User?, String?) -> Unit) {
        authRepository.signInWithEmailAndPassword(email, password) { user, error_message ->
            onComplete(user, error_message)
        }
    }

    fun signInWithGoogleAuthCredential(account: GoogleSignInAccount, onComplete: (User?, Boolean?, String?) -> Unit)  {
        authRepository.signInWithGoogleAuthCredential(account) {user, isNew, errorMessage ->
            onComplete(user, isNew, errorMessage)
        }
    }

    fun addUserToFirestoreDatabase(userInfo: User, onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.addUserToFirestoreDatabase(userInfo){
                onComplete()
            }
        }
    }

    fun resetPassword(email: String, onComplete: (String?) -> Unit) {
        authRepository.resetPassword(email) { error_message ->
            onComplete(error_message)
        }
    }

    fun deleteUser() {
        authRepository.deleteUser()
    }

    fun storeUserDetails(userInfo: User) {
        val gson = Gson()
        val json = gson.toJson(userInfo)
        sharedPreferences.edit().putString(AppConstants.CURRENT_USER, json).apply()
    }

}