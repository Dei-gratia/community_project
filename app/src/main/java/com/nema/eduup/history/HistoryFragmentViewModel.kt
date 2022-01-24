package com.nema.eduup.history

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.repository.NoteRepository
import com.nema.eduup.roomDatabase.Note
import kotlinx.coroutines.launch

class HistoryFragmentViewModel(app: Application): AndroidViewModel(app) ,
    DefaultLifecycleObserver {
    private lateinit var historyListenerRegistration: ListenerRegistration
    private var history : MutableLiveData<List<HashMap<String, Any>>> = MutableLiveData()
    private var noteRepository = NoteRepository



    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun getHistory(collection: CollectionReference): LiveData<List<HashMap<String, Any>>> {
        viewModelScope.launch {
            historyListenerRegistration = noteRepository.addHistoryListener(collection) {
                history.value = it
            }
        }
        return  history
    }

    fun getHistoryNotes(level: String, noteIds: ArrayList<String>, onComplete: (ArrayList<Note>) -> Unit) {
        noteRepository.getNotesByIds(level, noteIds) {
            onComplete(it)
        }
    }

    fun addToHistory(noteId: String){
        viewModelScope.launch {
            noteRepository.addToHistory(noteId)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (::historyListenerRegistration.isInitialized) {
            noteRepository.removeListener(historyListenerRegistration)
        }
    }
}