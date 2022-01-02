package com.nema.eduup.history

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.repository.NoteRepository
import com.nema.eduup.roomDatabase.Note
import kotlinx.coroutines.launch

class HistoryFragmentViewModel(private val app: Application): AndroidViewModel(app) ,
    DefaultLifecycleObserver {
    private lateinit var historyListenerRegistration: ListenerRegistration
    private var historyIds : MutableLiveData<List<String>> = MutableLiveData()
    private var noteRepository = NoteRepository



    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun getHistoryIds(collection: CollectionReference): LiveData<List<String>> {
        viewModelScope.launch {
            historyListenerRegistration = noteRepository.addBookmarksListener(collection) {
                historyIds.value = it
            }
        }
        return  historyIds
    }

    fun getNotesByIds(notesCollection: CollectionReference, noteIds: ArrayList<String>,onComplete: (ArrayList<Note>) -> Unit) {
        noteRepository.getNotesByIds(notesCollection, noteIds) {
            onComplete(it)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (::historyListenerRegistration.isInitialized) {
            noteRepository.removeListener(historyListenerRegistration)
        }
    }



}