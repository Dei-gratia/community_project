package com.nema.eduup.activities.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.models.GroupChatChannel
import com.nema.eduup.models.Message
import com.nema.eduup.models.Rating
import com.nema.eduup.models.User
import com.nema.eduup.repository.FirestoreUtil
import com.nema.eduup.roomDatabase.Note

class FirestoreViewModel(application: Application): AndroidViewModel(application), LifecycleObserver {
    private val TAG = FirestoreViewModel::class.qualifiedName
    private lateinit var usersListener: ListenerRegistration
    private lateinit var messagesListener: ListenerRegistration
    private lateinit var notesListener: ListenerRegistration
    private lateinit var bookmarksListener: ListenerRegistration
    private lateinit var chatChannelsListener: ListenerRegistration
    private lateinit var googleSignInClient: GoogleSignInClient
    private var firestoreUtil = FirestoreUtil
    var users : MutableLiveData<List<User>> = MutableLiveData()
    var publicNotes : MutableLiveData<List<Note>> = MutableLiveData()
    var personalNotes : MutableLiveData<List<Note>> = MutableLiveData()
    val listNotes : MutableLiveData<List<Note>> = MutableLiveData()
    val notes: MutableLiveData<List<Note>> = MutableLiveData()
    var messages : MutableLiveData<List<Message>> = MutableLiveData()
    var ratings : MutableLiveData<List<Rating>> = MutableLiveData()
    var reminders : MutableLiveData<List<Note>> = MutableLiveData()
    var bookmarks : MutableLiveData<List<Note>> = MutableLiveData()
    var activeDiscussionLastMessage: MutableLiveData<Message> = MutableLiveData()
    var lastMessage: MutableLiveData<Message> = MutableLiveData()
    var numRating: MutableLiveData<Int> = MutableLiveData()
    val groupLastMessage: MutableLiveData<Message> = MutableLiveData()
    val groups: MutableLiveData<List<GroupChatChannel>> = MutableLiveData()

    private val auth by lazy { FirebaseAuth.getInstance() }

    fun registerUser(userDetails: User, password: String, onComplete: () -> Unit) {
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

    fun updateUserProfileData(userHashMap: HashMap<String, Any>, onComplete: (User) -> Unit){
        firestoreUtil.updateUserProfileData(userHashMap){
            onComplete(it)
        }
    }

    fun updateRating(noteRef: DocumentReference, rating: Rating, onComplete: (Task<Void>) -> Unit){
        val ratingUpdate = firestoreUtil.updateRating(noteRef, rating)
        onComplete(ratingUpdate)
    }

    fun deleteReview(documentReference: DocumentReference) {
        firestoreUtil.deleteReview(documentReference)
    }

    fun setFCMRegistrationTokens(registrationTokens: MutableList<String>) {
        firestoreUtil.setFCMRegistrationTokens(registrationTokens)
    }

    fun addNoteToFirestore(note: Note, collection: CollectionReference, onComplete: () -> Unit) {
        firestoreUtil.addNoteToFirestore(note,collection) {
            onComplete()
        }
    }

    fun getFCMRegistrationTokens(onComplete: (tokens: MutableList<String>) -> Unit) {
        firestoreUtil.getFCMRegistrationTokens{
            onComplete(it)
        }
    }

    fun addBookmark(note: Note) {
        firestoreUtil.addBookmark(note)
    }

    fun addUserToGroup(channelId: String, onComplete: () -> Unit) {
        firestoreUtil.addUserToGroup(channelId){
            onComplete()
        }
    }

    fun createGroup(name: String, groupImageUrl: String, securityMode: String, onComplete: (String) -> Unit) {
        firestoreUtil.createGroup(name, groupImageUrl, securityMode) {
            onComplete(it)
        }
    }

    fun getOrCreateChatChannel(otherUserId: String, onComplete: (String) -> Unit) {
        firestoreUtil.getOrCreateChatChannel(otherUserId) {
            onComplete(it)
        }
    }

    fun getCurrentUserID() {
        firestoreUtil.getCurrentUserID()
    }

    fun getCurrentUser(onComplete: (User) -> Unit){
        firestoreUtil.getCurrentUser {
            onComplete(it)
        }
    }

    fun getUser(userId: String, onComplete: (User) -> Unit) {
        firestoreUtil.getUser(userId){
            onComplete(it)
        }
    }

    fun getUserActiveChatChannels(onComplete: (HashMap<String, String>) -> Unit) {
        firestoreUtil.getUserActiveChatChannels {
            onComplete(it)
        }
    }

    fun getUserActiveGroupChannels(onComplete: (ArrayList<String>) -> Unit) {
        firestoreUtil.getUserActiveGroupChannels {
            onComplete(it)
        }
    }

    fun getGroup(channelId: String, onComplete: (GroupChatChannel) -> Unit) {
        firestoreUtil.getGroup(channelId) {
            onComplete(it)
        }
    }

    fun sendMessage(message: Message, collection: CollectionReference, onComplete: () -> Unit) {
        firestoreUtil.sendMessage(message, collection) {
            onComplete()
        }
    }

    fun sendGroupMessage(message: Message, channelId: String, onComplete: () -> Unit) {
        firestoreUtil.sendGroupMessage(message, channelId) {
            onComplete()
        }
    }

    fun loadReminders(collection: CollectionReference, onComplete: (LiveData<List<Note>>) -> Unit) {
        firestoreUtil.addRemindersListener(collection) {
            reminders.value = it
            onComplete(reminders)
        }
    }

    fun loadNotesByIds(notesCollection: CollectionReference, noteIds: ArrayList<String>,onComplete: (LiveData<List<Note>>) -> Unit) {
        firestoreUtil.loadNotesByIds(notesCollection, noteIds) {
            listNotes.value = it
            onComplete(listNotes)
        }
    }

    fun loadUserReview(documentReference: DocumentReference, onComplete: (Rating?) -> Unit) {
        firestoreUtil.loadUserReview(documentReference){
            onComplete(it)
        }
    }

    fun noteRatings(collection: CollectionReference, onListen: (LiveData<List<Rating>>, LiveData<Int>) -> Unit){
        firestoreUtil.addRatingListener(collection) { listRatings, numRatings ->
            ratings.value = listRatings
            numRating.value = numRatings
            onListen(ratings, numRating)
        }
    }

    fun activeDiscussions(userActiveChatChannels: ArrayList<String>, onListen: (LiveData<Message>) -> Unit){
        firestoreUtil.addActiveDiscussionsListener(userActiveChatChannels) {
            activeDiscussionLastMessage.value = it
            onListen(activeDiscussionLastMessage)
        }
    }

    fun lastMessage(collection: CollectionReference, onListen: (LiveData<Message>) -> Unit){
        firestoreUtil.addLastMessageListener(collection) {
            lastMessage.value = it
            onListen(lastMessage)
        }
    }

    fun users(onListen: (LiveData<List<User>>) -> Unit){
        firestoreUtil.addUsersListener {
            users.value = it
            onListen(users)
        }
    }

    fun groups(onListen: (LiveData<List<GroupChatChannel>>) -> Unit) {
        firestoreUtil.addGroupsListener {
            groups.value = it
            onListen(groups)
        }
    }

    fun notes(collection: CollectionReference, onListen: (LiveData<List<Note>>) -> Unit) {
        firestoreUtil.addNotesListener(collection) {
            notes.value = it
            onListen(notes)
        }
    }

    fun addBookmarksListener(collection: CollectionReference, onListen: (LiveData<List<Note>>) -> Unit) {
        firestoreUtil.addBookmarksListener(collection) {
            bookmarks.value = it
            onListen(bookmarks)
        }
    }

    fun addMessagesListener(collection: CollectionReference, onListen: (LiveData<List<Message>>) -> Unit){
        messagesListener = firestoreUtil.addMessagesListener(collection) {
            messages.value = it
            onListen(messages)
        }
    }

    fun removeBookmark(note: Note) {
        firestoreUtil.removeBookmark(note)
    }

    fun removeListener(registration: ListenerRegistration) {
        firestoreUtil.removeListener(registration)
    }


}