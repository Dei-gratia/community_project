package com.nema.eduup.repository

import android.util.Log
import android.view.View
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.*
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object FirestoreUtil {


    private val TAG = com.nema.eduup.repository.FirestoreUtil::class.qualifiedName
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserDocRef by lazy { firestoreInstance.collection(AppConstants.USERS).document(getCurrentUserID()) }
    private val chatChannelsCollectionRef by lazy { firestoreInstance.collection(AppConstants.CHAT_CHANNELS) }
    private val groupChatChannelsCollectionRef by lazy { firestoreInstance.collection(AppConstants.GROUP_CHAT_CHANNELS) }

    private val currentUserId = getCurrentUserID()


    fun registerUserWithPasswordAndEmail(userDetails: User, password: String, onComplete: () -> Unit){
        val email = userDetails.email
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser = task.result!!.user!!

                    val user = User(
                        firebaseUser.uid,
                        userDetails.firstNames,
                        userDetails.familyName,
                        userDetails.email
                    )

                    addUserToFirestoreDatabase(user) {
                        onComplete()
                    }

                } else {
                    Log.e(TAG, task.exception!!.message.toString())
                }
            }

    }

    fun logInRegisteredUser(email: String, password: String, onComplete: (User) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    FirestoreUtil.getCurrentUser {
                        onComplete(it)
                    }
                } else {
                    Log.d(TAG,  task.exception!!.message.toString())
                }
            }
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

    fun updateRating(noteRef: DocumentReference, rating: Rating): Task<Void> {

        val ratingRef = noteRef.collection(AppConstants.NOTE_RATINGS).document(currentUserId)

        // In a transaction, add the new rating and update the aggregate totals
        return firestoreInstance.runTransaction { transaction ->

            val note = transaction.get(noteRef).toObject<Note>()!!

            // Compute new number of ratings
            val newNumRatings = note.numRating.plus(1)


            // Compute new average rating
            val oldRatingTotal = note.numRating.let { note.avgRating.times(it) }

            val newAvgRating = (oldRatingTotal.plus(rating.rateValue)).div(newNumRatings)


            note.numRating = newNumRatings
            note.avgRating = newAvgRating

            // Update restaurant
            transaction.set(noteRef, note)

            // Update rating

            transaction.set(ratingRef, rating)

            null
        }

    }

    fun deleteReview(documentReference: DocumentReference) {
        documentReference
            .delete()

    }

    fun setFCMRegistrationTokens(registrationTokens: MutableList<String>) {
        currentUserDocRef.update(mapOf("registrationTokens" to registrationTokens))
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

    fun getFCMRegistrationTokens(onComplete: (tokens: MutableList<String>) -> Unit) {
        currentUserDocRef.get().addOnSuccessListener {
            val user = it.toObject(User::class.java)!!
            onComplete(user.registrationTokens)
        }
    }

    fun addBookmark(note: Note){
        firestoreInstance.collection(AppConstants.USERS).document(this.getCurrentUserID()).collection(AppConstants.BOOKMARKS).document(note.id)
            .set(note)
            .addOnSuccessListener { result ->

            }
            .addOnFailureListener { e -> Log.e(TAG, e.message.toString(), e)}
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

    fun getUser(userId: String, onComplete: (User) -> Unit) {
        firestoreInstance.collection(AppConstants.USERS).document(userId).get()
            .addOnSuccessListener {
                val user = it.toObject(User::class.java)!!
                Log.d("User get", user.toString())
                onComplete(user)
            }
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

    fun loadNotesByIds(notesCollection: CollectionReference, noteIds: ArrayList<String>,onComplete: (ArrayList<Note>) -> Unit) {
        notesCollection
            .whereIn("id", noteIds)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Retrieving reminders")
                val notesList = java.util.ArrayList<Note>()
                for (document in result) {
                    notesList.add(parseNoteDocument(document))
                }
                onComplete(notesList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting reminders", e)
                return@addOnFailureListener
            }
    }


    fun loadUserReview(documentReference: DocumentReference, onComplete: (Rating?) -> Unit) {
        documentReference.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val review = document.toObject(Rating::class.java)!!
                    onComplete (review)
                }else {
                    onComplete (null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, e.message.toString())
            }
    }

    fun addRatingListener(collection: CollectionReference, onListen: (MutableList<Rating>, Int) -> Unit): ListenerRegistration {
        return collection
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Ratings listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val ratings = mutableListOf<Rating>()
                var numRating = 0
                for (document in querySnapshot!!.documentChanges) {
                    if (document.type == DocumentChange.Type.ADDED ) {
                        numRating += 1
                        if (document.document.id != currentUserId) {
                            ratings.add(document.document.toObject())
                        }
                    }
                    else if (document.type == DocumentChange.Type.REMOVED && document.document.id != currentUserId) {
                        numRating -= 1
                        if (document.document.id != currentUserId) {
                            ratings.remove(document.document.toObject())
                        }
                    }
                }
                onListen(ratings, numRating)
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
                                        onListen(message)
                                        Log.d("Messages text message", message.toString())
                                    } else if (document.document["type"] == MessageType.IMAGE) {
                                        val message =
                                            document.document.toObject(ImageMessage::class.java)
                                        onListen(message)
                                    } else {
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

    fun addUsersListener(onListen: (MutableList<User>) -> Unit): ListenerRegistration {
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
                        Log.d("Group", it.toString())
                        groups.add(it.toObject(GroupChatChannel::class.java)!!)
                    }
                }
                onListen(groups)

            }
    }

    fun addRemindersListener(collection: CollectionReference, onListen: (MutableList<Note>) -> Unit): ListenerRegistration{
       return collection
           .orderBy(AppConstants.DATE, Query.Direction.DESCENDING)
           .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
               if (firebaseFirestoreException != null) {
                   Log.e("FIRESTORE", "Reminders listener error.", firebaseFirestoreException)
                   return@addSnapshotListener
               }
               val reminders = mutableListOf<Note>()
               if (querySnapshot!!.isEmpty) {
                   seedReminders(){

                   }
               }
               for (document in querySnapshot!!.documentChanges) {
                   if (document.type == DocumentChange.Type.ADDED) {
                       reminders.add(parseNoteDocument(document.document))
                   }
                   else if (document.type == DocumentChange.Type.REMOVED) {
                       reminders.remove(parseNoteDocument(document.document))
                   }
               }
               onListen(reminders)
           }

    }

    fun addNotesListener(collection: CollectionReference, onListen: (MutableList<Note>) -> Unit): ListenerRegistration{
        return collection
            .orderBy(AppConstants.DATE, Query.Direction.DESCENDING)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Notes listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val notes = mutableListOf<Note>()
                for (document in querySnapshot!!.documentChanges) {
                    if (document.type == DocumentChange.Type.ADDED) {
                        notes.add(parseNoteDocument(document.document))
                    }
                    else if (document.type == DocumentChange.Type.REMOVED) {
                        notes.remove(parseNoteDocument(document.document))
                    }
                }
                onListen(notes)

            }
    }

    fun addBookmarksListener(collection: CollectionReference, onListen: (MutableList<Note>) -> Unit): ListenerRegistration {
        return collection
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Bookmarks listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                val bookmarks = mutableListOf<Note>()
                for (document in querySnapshot!!.documentChanges) {
                    if (document.type == DocumentChange.Type.ADDED) {
                        bookmarks.add(parseNoteDocument(document.document))
                    }
                    else if (document.type == DocumentChange.Type.REMOVED) {
                        bookmarks.remove(parseNoteDocument(document.document))
                    }
                }
                onListen(bookmarks)
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

    fun removeBookmark(note: Note){
        firestoreInstance.collection(AppConstants.USERS).document(this.getCurrentUserID()).collection(AppConstants.BOOKMARKS).document(note.id)
            .delete()
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