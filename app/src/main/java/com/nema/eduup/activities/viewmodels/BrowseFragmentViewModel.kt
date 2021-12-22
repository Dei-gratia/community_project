package com.nema.eduup.activities.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.models.Message
import com.nema.eduup.models.User
import com.nema.eduup.repository.FirebaseStorageUtil
import com.nema.eduup.repository.FirestoreUtil
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import kotlinx.coroutines.launch

class BrowseFragmentViewModel(private val app: Application): AndroidViewModel(app), DefaultLifecycleObserver {

    private lateinit var currentUser: User
    private lateinit var notesListenerRegistration: ListenerRegistration
    private lateinit var remindersListenerRegistration: ListenerRegistration
    private lateinit var bookmarksListenerRegistration: ListenerRegistration
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var notes : MutableLiveData<List<Note>> = MutableLiveData()
    private var reminders : MutableLiveData<List<Note>> = MutableLiveData()
    private var bookmarks : MutableLiveData<List<Note>> = MutableLiveData()
    private var firestoreUtil = FirestoreUtil
    private var firebaseStorageUtil = FirebaseStorageUtil
    private val sharedPreferences = app.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences

    val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
    var isNewlyCreated = true
    var navDrawerDisplaySelectionName = "com.nema.eduup.activities.viewmodels.HomeFragmentViewModel.navDrawerDisplaySelection"
    var navDrawerDisplaySelection = R.id.fragmentHome
    var recentlyViewNoteIdsName = "com.nema.eduup.activities.viewmodels.HomeFragmentViewModel.recentlyViewedNoteIds"

    private val maxRecentlyViewedNotes = 10
    val recentlyViewedNotes = ArrayList<Note>(maxRecentlyViewedNotes)

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun addToRecentlyViewedNotes(note: Note) {
        Log.d("History note", note.toString())
        val existingIndex = recentlyViewedNotes.indexOf(note)
        if (existingIndex == -1) {
            recentlyViewedNotes.add(0, note)
            for (index in recentlyViewedNotes.lastIndex downTo maxRecentlyViewedNotes)
                recentlyViewedNotes.removeAt(index)
        }else {
            for (index in (existingIndex - 1) downTo 0)
                recentlyViewedNotes[index + 1] = recentlyViewedNotes[index]
            recentlyViewedNotes[0] = note
        }
    }

    fun saveState(outState: Bundle) {
        outState.putInt(navDrawerDisplaySelectionName, navDrawerDisplaySelection)
        val noteIds = arrayListOf<String>()
        for (note in recentlyViewedNotes) {
            noteIds.add(note.id)
        }
        Log.d("History id list", noteIds.toString())
        outState.putStringArrayList(recentlyViewNoteIdsName, noteIds)
    }

    fun restoreState(savedInstanceState: Bundle) {
        var userLevel = "All Levels"
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userLevel = currentUser.schoolLevel
        }
        val collection =
            firestoreInstance.collection(AppConstants.NOTES).document(AppConstants.PUBLIC_NOTES)
                .collection(userLevel)
        navDrawerDisplaySelection = savedInstanceState.getInt(navDrawerDisplaySelectionName)
        val noteIds = savedInstanceState.getStringArrayList(recentlyViewNoteIdsName)
        Log.d("History note isd saved", noteIds.toString())
        if (!noteIds.isNullOrEmpty()) {
            FirestoreUtil.loadNotesByIds(collection, noteIds){
                if (it.isNotEmpty()) {
                    recentlyViewedNotes.addAll(it)
                    Log.d("History from fire", it.toString())
                }
            }
        }
    }

    fun getReminders(collection: CollectionReference): LiveData<List<Note>> {
        viewModelScope.launch {
            remindersListenerRegistration = firestoreUtil.addRemindersListener(collection) {
                reminders.value = it
            }
        }
        return reminders
    }

    fun getNotes(collection: CollectionReference): LiveData<List<Note>> {
        viewModelScope.launch {
            notesListenerRegistration = firestoreUtil.addNotesListener(collection) {
                notes.value = it
            }
        }
        return notes
    }

    fun getBookmarks(collection: CollectionReference): LiveData<List<Note>> {
        viewModelScope.launch {
            bookmarksListenerRegistration = firestoreUtil.addBookmarksListener(collection) {
                bookmarks.value = it
            }
        }
        return  bookmarks
    }

    fun addBookmark(note: Note) {
        viewModelScope.launch {
            firestoreUtil.addBookmark(note)
        }
    }

    fun removeBookmark(note: Note) {
        viewModelScope.launch {
            firestoreUtil.removeBookmark(note)
        }
    }

    fun getSliderImages(storageRef: String, onComplete: (String)-> Unit) {
        viewModelScope.launch {
            firebaseStorageUtil.getImageDownloadUrls(storageRef){
                onComplete(it)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        firestoreUtil.removeListener(notesListenerRegistration)
    }


}