package com.nema.eduup.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.nema.eduup.models.*
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.squareup.okhttp.internal.DiskLruCache
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object FirestoreUtil {


    private val TAG = FirestoreUtil::class.qualifiedName
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserDocRef by lazy { firestoreInstance.collection(AppConstants.USERS).document(getCurrentUserID()) }
    private val chatChannelsCollectionRef by lazy { firestoreInstance.collection(AppConstants.CHAT_CHANNELS) }
    private val groupChatChannelsCollectionRef by lazy { firestoreInstance.collection(AppConstants.GROUP_CHAT_CHANNELS) }


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

    fun getUser(userId: String, onComplete: (User) -> Unit) {
        firestoreInstance.collection(AppConstants.USERS).document(userId).get()
            .addOnSuccessListener {
                val user = it.toObject(User::class.java)!!
                Log.d("User get", user.toString())
                onComplete(user)
            }
    }


    fun registerUser( userInfo: User, onComplete: () -> Unit) {
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

    fun updateUserProfileData(userHashMap: HashMap<String, Any>, onComplete: () -> Unit) {
        firestoreInstance.collection(AppConstants.USERS)
            .document(getCurrentUserID())
            .update(userHashMap)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error while updating user details.", e)
            }
    }

    fun getFCMRegistrationTokens(onComplete: (tokens: MutableList<String>) -> Unit) {
        currentUserDocRef.get().addOnSuccessListener {
            val user = it.toObject(User::class.java)!!
            onComplete(user.registrationTokens)
        }
    }

    fun setFCMRegistrationTokens(registrationTokens: MutableList<String>) {
        currentUserDocRef.update(mapOf("registrationTokens" to registrationTokens))
    }

    fun loadData(onComplete: (ArrayList<Note>) -> Unit) {
        val remindersCollection = firestoreInstance.collection(AppConstants.REMINDERS)
        remindersCollection
            .orderBy(AppConstants.DATE, Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Retrieving reminders")
                val remindersList = java.util.ArrayList<Note>()
                for (document in result) {
                    remindersList.add(parseNoteDocument(document))
                }
                if (remindersList.isEmpty()) {
                    seedReminders{
                        onComplete(it)
                    }
                } else {
                    onComplete(remindersList)
                }

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting reminders", e)
                return@addOnFailureListener
            }
    }

    fun loadNotesByIds(notesCollection: CollectionReference, noteIds: ArrayList<String>,onComplete: (ArrayList<Note>) -> Unit) {
        notesCollection
            .whereIn("id", noteIds)
            //.orderBy(AppConstants.DATE, Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Retrieving reminders")
                val notesList = java.util.ArrayList<Note>()
                for (document in result) {
                    notesList.add(parseNoteDocument(document))
                }
                if (notesList.isEmpty()) {
                    seedReminders{
                        onComplete(it)
                    }
                } else {
                    onComplete(notesList)
                }

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting reminders", e)
                return@addOnFailureListener
            }
    }

    fun addNoteToFirestore(note: Note, collection: CollectionReference, onComplete: () -> Unit) {
        collection
            .add(note)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding note", e)
                return@addOnFailureListener
            }
    }

    fun seedReminders(onComplete: (ArrayList<Note>) -> Unit) {
        val remindersList = java.util.ArrayList<Note>()
        val remindersCollection = firestoreInstance.collection(AppConstants.REMINDERS)
        var note =
            Note(
                "",
                "",
                "Welcome to EduUp",
                "This is a great app to get and manage all your study materials",
                "",
                "",
                "",
                "all",
                Calendar.getInstance().timeInMillis,
                0.0,
                0,
                true
            )
        addNoteToFirestore(note, remindersCollection){
            remindersList.add(note)
        }

        note = Note(
            "",
            "",
            "EduUp",
            "Educational app to help your get all the most relevant study material in one place",
            "",
            "",
            "",
            "all",
            Calendar.getInstance().timeInMillis - 10000,
            0.0,
            0,
            true
        )

        addNoteToFirestore(note, remindersCollection){
            remindersList.add(note)
            onComplete(remindersList)
        }
    }

    fun addBookmark(note: Note){
        firestoreInstance.collection(AppConstants.USERS).document(this.getCurrentUserID()).collection(AppConstants.BOOKMARKS).document(note.id)
            .set(note)
            .addOnSuccessListener { result ->

            }
            .addOnFailureListener { e -> Log.e(TAG, e.message.toString(), e)}
    }

    fun removeBookmark(note: Note){
        firestoreInstance.collection(AppConstants.USERS).document(this.getCurrentUserID()).collection(AppConstants.BOOKMARKS).document(note.id)
            .delete()
    }

    fun sendMessage(message: Message, channelId: String, onComplete: () -> Unit) {
        chatChannelsCollectionRef.document(channelId)
            .collection(AppConstants.MESSAGES)
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

    fun getOrCreateChatChannel(otherUserId: String, onComplete: (String) -> Unit) {
        currentUserDocRef.collection(AppConstants.ACTIVE_CHAT_CHANNELS)
            .document(otherUserId).get().addOnSuccessListener {
                if (it.exists()) {
                    val channelId = it[AppConstants.CHANNEL_ID] as String
                    onComplete(channelId)
                    return@addOnSuccessListener
                }
                val currentUserId = getCurrentUserID()
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

    fun addUserToGroup(channelId: String, onComplete: () -> Unit) {
        currentUserDocRef.collection(AppConstants.ACTIVE_GROUP_CHANNELS)
            .document(channelId).get().addOnSuccessListener {
                if (it.exists()) {
                    onComplete()
                    return@addOnSuccessListener
                }
                val currentUserId = getCurrentUserID()
                firestoreInstance.collection(AppConstants.USERS).document(currentUserId)
                    .collection(AppConstants.ACTIVE_GROUP_CHANNELS)
                    .document(channelId)
                    .set(mapOf(AppConstants.CHANNEL_ID to channelId))
                groupChatChannelsCollectionRef
                    .document(channelId)
                    //.update(mapOf(AppConstants.USER_IDS to currentUserId))
                    .update(AppConstants.USER_IDS, FieldValue.arrayUnion(currentUserId))

                onComplete()
            }

    }

    fun createGroup(name: String, groupImageUrl: String, securityMode: String, onComplete: (String) -> Unit){
        val currentUserId = getCurrentUserID()
        val newChannel = groupChatChannelsCollectionRef.document()
        newChannel.set(GroupChatChannel(newChannel.id,name, currentUserId, groupImageUrl,"about", securityMode,mutableListOf(currentUserId)))

        firestoreInstance.collection(AppConstants.USERS).document(currentUserId)
            .collection(AppConstants.ACTIVE_GROUP_CHANNELS)
            .document(newChannel.id)
            .set(mapOf(AppConstants.CHANNEL_ID to newChannel.id))

        val channelId = newChannel.id
        onComplete(channelId)
    }

    fun getUserActiveChatChannels(onComplete: (HashMap<String, String>) -> Unit) {
        currentUserDocRef.collection(AppConstants.ACTIVE_CHAT_CHANNELS)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Retrieving activeChatChannels")
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
                Log.d(TAG, "Retrieving activeChatChannels")
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
                    Log.d("User get", group.toString())
                    onComplete(group)
                }
            }
    }

    fun addActiveDiscussionsListener(userActiveChatChannels: ArrayList<String>, onListen: (Message) -> Unit): ListenerRegistration {
        Log.d("Messages Useractivechannels", userActiveChatChannels.toString())
        return firestoreInstance.collection(AppConstants.CHAT_CHANNELS)
            .whereIn(AppConstants.CHANNEL_ID, userActiveChatChannels)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Discussions listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val discussions = mutableListOf<Message>()
                querySnapshot!!.documents.forEach {
                    firestoreInstance.collection(AppConstants.CHAT_CHANNELS).document(it.id)
                        .collection(AppConstants.MESSAGES)
                        .orderBy(AppConstants.TIME, Query.Direction.DESCENDING )
                        .limit(1)
                        .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                            if (firebaseFirestoreException != null) {
                                Log.e("FIRESTORE", "Last Message listener error.", firebaseFirestoreException)
                                return@addSnapshotListener
                            }
                            for (document in querySnapshot!!.documentChanges) {
                                Log.d("Messages document", document.toString())
                                if (document.type == DocumentChange.Type.ADDED) {
                                    Log.d("Messages document", document.toString())
                                    if (document.document["type"] == MessageType.TEXT) {
                                        val message =
                                            document.document.toObject(TextMessage::class.java)
                                        //discussions.add(message)
                                        onListen(message)
                                        Log.d("Messages text message", message.toString())
                                    } else if (document.document["type"] == MessageType.IMAGE) {
                                        val message =
                                            document.document.toObject(ImageMessage::class.java)
                                        //discussions.add(message)
                                        onListen(message)
                                    } else {
                                        val message =
                                            document.document.toObject(FileMessage::class.java)
                                        //discussions.add(message)
                                        onListen(message)
                                    }

                                }
                                //Log.d("Messages", discussions.toString())
                                //println("Messages are $discussions \n \n \n message 1 is ${discussions[0]} \n \n ")
                                //onListen(discussions)
                            }

                        }
                }

            }
    }

    fun addLastMessageListener(channelId: String, onListen: (Message) -> Unit): ListenerRegistration{
        return firestoreInstance.collection(AppConstants.CHAT_CHANNELS).document(channelId)
            .collection(AppConstants.MESSAGES)
            .orderBy(AppConstants.TIME, Query.Direction.DESCENDING )
            .limit(1)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Last Message listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                for (document in querySnapshot!!.documentChanges) {
                    Log.d("Messages document", document.toString())
                    if (document.type == DocumentChange.Type.ADDED) {
                        Log.d("Messages document", document.toString())
                        if (document.document["type"] == MessageType.TEXT) {
                            val message =
                                document.document.toObject(TextMessage::class.java)
                            //discussions.add(message)
                            onListen(message)
                            Log.d("Messages text message", message.toString())
                        } else if (document.document["type"] == MessageType.IMAGE) {
                            val message =
                                document.document.toObject(ImageMessage::class.java)
                            //discussions.add(message)
                            onListen(message)
                        } else {
                            val message =
                                document.document.toObject(FileMessage::class.java)
                            //discussions.add(message)
                            onListen(message)
                        }

                    }
                }

            }
    }

    fun addGroupLastMessageListener(channelId: String, onListen: (Message) -> Unit): ListenerRegistration{
        return firestoreInstance.collection(AppConstants.GROUP_CHAT_CHANNELS).document(channelId)
            .collection(AppConstants.MESSAGES)
            .orderBy(AppConstants.TIME, Query.Direction.DESCENDING )
            .limit(1)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Last Message listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                for (document in querySnapshot!!.documentChanges) {
                    Log.d("Messages document", document.toString())
                    if (document.type == DocumentChange.Type.ADDED) {
                        Log.d("Messages document", document.toString())
                        if (document.document["type"] == MessageType.TEXT) {
                            val message =
                                document.document.toObject(TextMessage::class.java)
                            //discussions.add(message)
                            onListen(message)
                            Log.d("Messages text message", message.toString())
                        } else if (document.document["type"] == MessageType.IMAGE) {
                            val message =
                                document.document.toObject(ImageMessage::class.java)
                            //discussions.add(message)
                            onListen(message)
                        } else {
                            val message =
                                document.document.toObject(FileMessage::class.java)
                            //discussions.add(message)
                            onListen(message)
                        }

                    }
                }

            }
    }

    fun addUsersListener(onListen: (List<User>) -> Unit): ListenerRegistration {
        return firestoreInstance.collection(AppConstants.USERS)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Users listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val users = mutableListOf<User>()
                querySnapshot!!.documents.forEach {
                    if (it.id != FirebaseAuth.getInstance().currentUser?.uid)
                        users.add(it.toObject(User::class.java)!!)
                }
                onListen(users)

            }
    }

    fun addGroupsListener(onListen: (List<GroupChatChannel>) -> Unit): ListenerRegistration {
        return firestoreInstance.collection(AppConstants.GROUP_CHAT_CHANNELS)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Groups listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val groups = mutableListOf<GroupChatChannel>()
                querySnapshot!!.documents.forEach {
                    if (it.exists()) {
                        Log.d("Group", it.toString())
                        groups.add(it.toObject(GroupChatChannel::class.java)!!)
                    }
                }
                onListen(groups)

            }
    }

    fun addNotesListener(collection: CollectionReference, onListen: (Note, String) -> Unit): ListenerRegistration{
        return collection
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Notes listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val notes = mutableListOf<Note>()
                for (document in querySnapshot!!.documentChanges) {
                    if (document.type == DocumentChange.Type.ADDED) {
                        onListen(parseNoteDocument(document.document), "1")
                    }
                    else if (document.type == DocumentChange.Type.REMOVED) {
                        onListen(parseNoteDocument(document.document), "2")
                    }
                }

            }
    }

    fun addBookmarksListener(collection: CollectionReference, onListen: (Note, String) -> Unit): ListenerRegistration {
        return collection
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Bookmarks listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val bookmarks = mutableListOf<Note>()
                for (document in querySnapshot!!.documentChanges) {
                    if (document.type == DocumentChange.Type.ADDED) {
                        onListen(parseNoteDocument(document.document), "1")
                    }
                    else if (document.type == DocumentChange.Type.REMOVED) {
                        onListen(parseNoteDocument(document.document), "2")
                    }
                }

            }

    }

    fun removeListener(registration: ListenerRegistration) = registration.remove()

    private fun parseNoteDocument(document: DocumentSnapshot): Note {
        return Note(
            document.id,
            document.getString("subject")!!,
            document.getString("title")!!,
            document.getString("description")!!,
            document.getString("body")!!,
            document.getString("fileUrl")!!,
            document.getString("fileType")!!,
            document.getString("level")!!,
            document.getLong("date")!!,
            document.getDouble("avgRating")!!,
            document.getLong("numRating")!!,
            document.getBoolean("reminders")!!
        )
    }
}