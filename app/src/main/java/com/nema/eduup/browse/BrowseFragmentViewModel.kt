package com.nema.eduup.browse

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.auth.User
import com.nema.eduup.repository.FirebaseStorageUtil
import com.nema.eduup.repository.NoteRepository
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.viewnote.ViewNoteActivity
import kotlinx.coroutines.launch

class BrowseFragmentViewModel(app: Application): AndroidViewModel(app), DefaultLifecycleObserver {

    private val TAG = BrowseFragmentViewModel::class.qualifiedName
    private lateinit var currentUser: User
    private lateinit var notesListenerRegistration: ListenerRegistration
    private lateinit var remindersListenerRegistration: ListenerRegistration
    private lateinit var bookmarksListenerRegistration: ListenerRegistration
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var notes : MutableLiveData<List<Note>> = MutableLiveData()
    private var reminders : MutableLiveData<List<Note>> = MutableLiveData()
    private var bookmarks : MutableLiveData<List<HashMap<String, Any>>> = MutableLiveData()
    private var noteRepository = NoteRepository
    private var firebaseStorageUtil = FirebaseStorageUtil
    private val sharedPreferences = app.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences


    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun getNotesByIds1(notesCollection: CollectionReference, noteIds: ArrayList<String>,onComplete: (ArrayList<Note>) -> Unit) {
        noteRepository.getNotesByIds1(notesCollection, noteIds) {
            onComplete(it)
        }
    }

    fun getNotesByIds(level: String, noteIds: ArrayList<String>,onComplete: (ArrayList<Note>) -> Unit) {
        noteRepository.getNotesByIds(level, noteIds) {
            onComplete(it)
        }
    }

    fun getReminders(collection: CollectionReference): LiveData<List<Note>> {
        viewModelScope.launch {
            remindersListenerRegistration = noteRepository.addRemindersListener(collection) {
                reminders.value = it
            }
        }
        return reminders
    }

    fun getNotes(collection: CollectionReference): LiveData<List<Note>> {
        viewModelScope.launch {
            notesListenerRegistration = noteRepository.addNotesListener(collection) {
                notes.value = it
            }
        }
        return notes
    }

    fun getLevelNotes(level: String): LiveData<List<Note>> {
        viewModelScope.launch {
            notesListenerRegistration = noteRepository.addLevelNotesListener(level) {
                notes.value = it
            }
        }
        return notes
    }

    fun getBookmarks(collection: CollectionReference): LiveData<List<HashMap<String, Any>>> {
        viewModelScope.launch {
            bookmarksListenerRegistration = noteRepository.addHistoryListener(collection) {
                bookmarks.value = it
            }
        }
        return  bookmarks
    }

    fun addBookmark(noteId: String) {
        viewModelScope.launch {
            noteRepository.addBookmark(noteId)
        }
    }

    fun removeBookmark(noteId: String) {
        viewModelScope.launch {
            noteRepository.removeBookmark(noteId)
        }
    }

    fun addToHistory(noteId: String){
       viewModelScope.launch {
           noteRepository.addToHistory(noteId)
       }
    }


    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (::notesListenerRegistration.isInitialized) {
            noteRepository.removeListener(notesListenerRegistration)
        }
        if (::bookmarksListenerRegistration.isInitialized) {
            noteRepository.removeListener(bookmarksListenerRegistration)
        }
    }


}