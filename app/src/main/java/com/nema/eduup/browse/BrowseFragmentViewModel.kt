package com.nema.eduup.browse

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.repository.FirebaseStorageUtil
import com.nema.eduup.repository.NoteRepository
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import kotlinx.coroutines.launch

class BrowseFragmentViewModel(app: Application): AndroidViewModel(app), DefaultLifecycleObserver {

    private lateinit var currentUser: User
    private lateinit var notesListenerRegistration: ListenerRegistration
    private lateinit var remindersListenerRegistration: ListenerRegistration
    private lateinit var bookmarksListenerRegistration: ListenerRegistration
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var notes : MutableLiveData<List<Note>> = MutableLiveData()
    private var reminders : MutableLiveData<List<Note>> = MutableLiveData()
    private var bookmarks : MutableLiveData<List<String>> = MutableLiveData()
    private var noteRepository = NoteRepository
    private var firebaseStorageUtil = FirebaseStorageUtil
    private val sharedPreferences = app.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences


    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun getNotesByIds(notesCollection: CollectionReference, noteIds: ArrayList<String>,onComplete: (ArrayList<Note>) -> Unit) {
        noteRepository.getNotesByIds(notesCollection, noteIds) {
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

    fun getBookmarks(collection: CollectionReference): LiveData<List<String>> {
        viewModelScope.launch {
            bookmarksListenerRegistration = noteRepository.addBookmarksListener(collection) {
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