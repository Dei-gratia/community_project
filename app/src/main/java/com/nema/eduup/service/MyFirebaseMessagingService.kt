package com.nema.eduup.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nema.eduup.repository.UserRepository


class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.notification != null) {
            Log.d("FCM", remoteMessage.data.toString())
        }
    }

    override fun onNewToken(token: String) {

        if (FirebaseAuth.getInstance().currentUser != null)
            addTokenToFirestore(token)
    }

    companion object {
        fun addTokenToFirestore(newRegistrationToken: String?) {
            if (newRegistrationToken == null ) throw NullPointerException("FCM token is null.")

            UserRepository.getFCMRegistrationTokens { tokens ->
                if (tokens.contains(newRegistrationToken))
                    return@getFCMRegistrationTokens

                tokens.add(newRegistrationToken)
                UserRepository.setFCMRegistrationTokens(tokens)
            }
        }
    }
}