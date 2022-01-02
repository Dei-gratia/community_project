package com.nema.eduup.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.nema.eduup.discussions.*
import com.nema.eduup.auth.User
import com.nema.eduup.discussions.chat.*
import com.nema.eduup.discussions.groups.GroupChatChannel
import com.nema.eduup.utils.AppConstants
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object DiscussionRepository {


    private val TAG = DiscussionRepository::class.qualifiedName
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val userRepository = UserRepository
    private val currentUserId = userRepository.getCurrentUserID()
    private val currentUserDocRef by lazy { firestoreInstance.collection(AppConstants.USERS).document(currentUserId) }
    private val chatChannelsCollectionRef by lazy { firestoreInstance.collection(AppConstants.CHAT_CHANNELS) }
    private val groupChatChannelsCollectionRef by lazy { firestoreInstance.collection(AppConstants.GROUP_CHAT_CHANNELS) }



    fun addUserToGroup(channelId: String, onComplete: () -> Unit) {
        currentUserDocRef.collection(AppConstants.ACTIVE_GROUP_CHANNELS)
            .document(channelId).get().addOnSuccessListener {
                if (it.exists()) {
                    onComplete()
                    return@addOnSuccessListener
                }
                firestoreInstance.collection(AppConstants.USERS).document(currentUserId)
                    .collection(AppConstants.ACTIVE_GROUP_CHANNELS)
                    .document(channelId)
                    .set(mapOf(AppConstants.CHANNEL_ID to channelId))
                groupChatChannelsCollectionRef
                    .document(channelId)
                    .update(AppConstants.USER_IDS, FieldValue.arrayUnion(currentUserId))

                onComplete()
            }

    }

    fun createGroup(name: String, groupImageUrl: String, securityMode: String, onComplete: (String) -> Unit){
        val newChannel = groupChatChannelsCollectionRef.document()
        newChannel.set(GroupChatChannel(newChannel.id,name, currentUserId, groupImageUrl,"about", securityMode,mutableListOf(currentUserId)))

        firestoreInstance.collection(AppConstants.USERS).document(currentUserId)
            .collection(AppConstants.ACTIVE_GROUP_CHANNELS)
            .document(newChannel.id)
            .set(mapOf(AppConstants.CHANNEL_ID to newChannel.id))

        val channelId = newChannel.id
        onComplete(channelId)
    }

    fun getOrCreateChatChannel(otherUserId: String, onComplete: (String) -> Unit) {
        currentUserDocRef.collection(AppConstants.ACTIVE_CHAT_CHANNELS)
            .document(otherUserId).get().addOnSuccessListener {
                if (it.exists()) {
                    val channelId = it[AppConstants.CHANNEL_ID] as String
                    onComplete(channelId)
                    return@addOnSuccessListener
                }
                val newChannel = chatChannelsCollectionRef.document()
                newChannel.set(ChatChannel(newChannel.id, mutableListOf(currentUserId, otherUserId)))

                firestoreInstance.collection(AppConstants.USERS).document(currentUserId)
                    .collection(AppConstants.ACTIVE_CHAT_CHANNELS)
                    .document(otherUserId)
                    .set(mapOf(AppConstants.CHANNEL_ID to newChannel.id))

                firestoreInstance.collection(AppConstants.USERS).document(otherUserId)
                    .collection(AppConstants.ACTIVE_CHAT_CHANNELS)
                    .document(currentUserId)
                    .set(mapOf(AppConstants.CHANNEL_ID to newChannel.id))

                val channelId = newChannel.id
                onComplete(channelId)
            }
    }

    fun getUserActiveChatChannels(onComplete: (HashMap<String, String>) -> Unit) {
        currentUserDocRef.collection(AppConstants.ACTIVE_CHAT_CHANNELS)
            .get()
            .addOnSuccessListener { result ->
                val activeChatChannels = HashMap<String, String>()
                for (document in result) {
                    document.getString(AppConstants.CHANNEL_ID)?.let {
                        activeChatChannels[document.id] = it }
                }
                onComplete(activeChatChannels)

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting Active chat channels", e)
                return@addOnFailureListener
            }

    }

    fun getUserActiveGroupChannels(onComplete: (ArrayList<String>) -> Unit) {
        currentUserDocRef.collection(AppConstants.ACTIVE_GROUP_CHANNELS)
            .get()
            .addOnSuccessListener { result ->
                val activeChatChannels = ArrayList<String>()
                for (document in result) {
                    document.getString(AppConstants.CHANNEL_ID)?.let {
                        activeChatChannels.add(it)  }
                }
                onComplete(activeChatChannels)

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting Active chat channels", e)
                return@addOnFailureListener
            }

    }

    fun getGroup(channelId: String, onComplete: (GroupChatChannel) -> Unit) {
        firestoreInstance.collection(AppConstants.GROUP_CHAT_CHANNELS).document(channelId).get()
            .addOnSuccessListener {
                if (it.exists()){
                    val group = it.toObject(GroupChatChannel::class.java)!!
                    onComplete(group)
                }
            }
    }

    fun sendMessage(message: Message, collection: CollectionReference, onComplete: () -> Unit) {
        collection
            .add(message)
            .addOnSuccessListener {
                onComplete()
            }
    }

    fun sendGroupMessage(message: Message, channelId: String, onComplete: () -> Unit) {
        groupChatChannelsCollectionRef.document(channelId)
            .collection(AppConstants.MESSAGES)
            .add(message)
            .addOnSuccessListener {
                onComplete()
            }
    }

    fun addActiveDiscussionsListener(userActiveChatChannels: ArrayList<String>, onListen: (Message) -> Unit): ListenerRegistration {
        return firestoreInstance.collection(AppConstants.CHAT_CHANNELS)
            .whereIn(AppConstants.CHANNEL_ID, userActiveChatChannels)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Discussions listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                querySnapshot!!.documents.forEach {
                    firestoreInstance.collection(AppConstants.CHAT_CHANNELS).document(it.id)
                        .collection(AppConstants.MESSAGES)
                        .orderBy(AppConstants.TIME, Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                            if (firebaseFirestoreException != null) {
                                Log.e(
                                    "FIRESTORE",
                                    "Last Message listener error.",
                                    firebaseFirestoreException
                                )
                                return@addSnapshotListener
                            }
                            for (document in querySnapshot!!.documentChanges) {
                                if (document.type == DocumentChange.Type.ADDED) {
                                    when (document.document["type"]) {
                                        MessageType.TEXT -> {
                                            val message =
                                                document.document.toObject(TextMessage::class.java)
                                            onListen(message)
                                        }
                                        MessageType.IMAGE -> {
                                            val message =
                                                document.document.toObject(ImageMessage::class.java)
                                            onListen(message)
                                        }
                                        else -> {
                                            val message =
                                                document.document.toObject(FileMessage::class.java)
                                            onListen(message)
                                        }
                                    }
                                }
                            }
                        }
                }
            }
    }

    fun addLastMessageListener(collection: CollectionReference, onListen: (Message) -> Unit): ListenerRegistration{
        return collection
            .orderBy(AppConstants.TIME, Query.Direction.DESCENDING )
            .limit(1)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Last Message listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                for (document in querySnapshot!!.documentChanges) {
                    if (document.type == DocumentChange.Type.ADDED) {
                        when (document.document["type"]) {
                            MessageType.TEXT -> {
                                val message =
                                    document.document.toObject(TextMessage::class.java)
                                onListen(message)
                            }
                            MessageType.IMAGE -> {
                                val message =
                                    document.document.toObject(ImageMessage::class.java)
                                onListen(message)
                            }
                            else -> {
                                val message =
                                    document.document.toObject(FileMessage::class.java)
                                onListen(message)
                            }
                        }
                    }
                }
            }
    }

    fun addUsersListener(onListen: (MutableList<User>) -> Unit): ListenerRegistration {
        val users = mutableListOf<User>()
        return firestoreInstance.collection(AppConstants.USERS)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Users listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                querySnapshot!!.documents.forEach {
                    if (it.id != FirebaseAuth.getInstance().currentUser?.uid)
                        users.add(it.toObject(User::class.java)!!)
                }
                onListen(users)
            }
    }

    fun addGroupsListener(onListen: (MutableList<GroupChatChannel>) -> Unit): ListenerRegistration {
        return firestoreInstance.collection(AppConstants.GROUP_CHAT_CHANNELS)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Groups listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val groups = mutableListOf<GroupChatChannel>()
                querySnapshot!!.documents.forEach {
                    if (it.exists()) {
                        groups.add(it.toObject(GroupChatChannel::class.java)!!)
                    }
                }
                onListen(groups)

            }
    }

    fun addMessagesListener(collection: CollectionReference, onListen: (MutableList<Message>) -> Unit): ListenerRegistration  {
        return collection
            .orderBy(AppConstants.TIME)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Messages listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val messages = mutableListOf<Message>()
                for (document in querySnapshot!!.documentChanges) {
                    if (document.type == DocumentChange.Type.ADDED) {
                        when (document.document["type"]) {
                            MessageType.TEXT -> {
                                val message = document.document.toObject(TextMessage::class.java)
                                messages.add(message)
                            }

                            MessageType.IMAGE -> {
                                val message = document.document.toObject(ImageMessage::class.java)
                                messages.add(message)
                            }

                            else -> {
                                val message = document.document.toObject(FileMessage::class.java)
                                messages.add(message)
                            }
                        }
                    }
                }
                onListen(messages)
            }

    }

    fun removeListener(registration: ListenerRegistration) = registration.remove()

}