package com.nema.eduup.home

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.repository.FirebaseStorageUtil
import com.nema.eduup.repository.NoteRepository
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.roomDatabase.NoteDao
import com.nema.eduup.roomDatabase.NoteRoomDatabase
import kotlinx.coroutines.launch

class HomeFragmentViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver {
    private lateinit var remindersListenerRegistration: ListenerRegistration
    private lateinit var notesListenerRegistration: ListenerRegistration
    private val noteRepository = NoteRepository
    private val firebaseStorageUtil = FirebaseStorageUtil
    private var reminders : MutableLiveData<List<Note>> = MutableLiveData()
    private var notes : MutableLiveData<List<Note>> = MutableLiveData()
    val allNotes: LiveData<List<Note>>
    private val noteDao: NoteDao

    init {
        val noteDb = NoteRoomDatabase.getDatabase(application)
        noteDao = noteDb!!.noteDao()
        allNotes = noteDao.allNotes
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun getSliderImages(storageRef: String, onComplete: (String)-> Unit) {
        viewModelScope.launch {
            firebaseStorageUtil.getImageDownloadUrls(storageRef){
                onComplete(it)
            }
        }
    }

    fun addNoteToFirestore(note: Note, collection: CollectionReference, onComplete: () -> Unit) {
        noteRepository.addNoteToFirestore(note,collection) {
            onComplete()
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

    fun insert(note: Note) {
        InsertAsyncTask(noteDao).execute(note)
    }

    companion object {
        private class InsertAsyncTask(private val noteDao: NoteDao) : AsyncTask<Note, Void, Void>() {
            override fun doInBackground(vararg notes: Note): Void? {
                noteDao.insert(notes[0])
                return null
            }

        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        noteRepository.removeListener(notesListenerRegistration)
    }
}