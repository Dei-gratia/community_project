package com.nema.eduup.viewnote

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.repository.NoteRepository
import com.nema.eduup.roomDatabase.Download
import com.nema.eduup.roomDatabase.DownloadDao
import com.nema.eduup.roomDatabase.NoteRoomDatabase
import kotlinx.coroutines.launch


class ViewNoteActivityViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver  {

    private val TAG = ViewNoteActivity::class.qualifiedName
    private lateinit var ratingsListener: ListenerRegistration
    private var noteRepository = NoteRepository
    private val downloadDao: DownloadDao
    val allDownloads: LiveData<List<Download>>
    var numRating: MutableLiveData<Int> = MutableLiveData()

    init {
        val noteDb = NoteRoomDatabase.getDatabase(application)
        downloadDao = noteDb!!.downloadDao()
        allDownloads = downloadDao.allDownloads
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    var noteRatings : MutableLiveData<List<Rating>> = MutableLiveData()

    fun insertDownload(download: Download) {
        Log.e(TAG, "insertDownload")
        viewModelScope.launch {
            downloadDao.insert(download)
        }
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


    fun getUserRating(documentReference: DocumentReference, onComplete: (Rating?) -> Unit) {
        viewModelScope.launch {
            noteRepository.loadUserReview(documentReference){
                onComplete(it)
            }
        }
    }

    fun getNoteRatings(collection: CollectionReference): LiveData<List<Rating>>{
        viewModelScope.launch {
            ratingsListener = noteRepository.addRatingListener(collection) { listRatings, numRatings ->
                noteRatings.value = listRatings
                numRating.value = numRatings
            }
        }
        return  noteRatings
    }

    fun deleteReview(documentReference: DocumentReference) {
        viewModelScope.launch {
            noteRepository.deleteDocument(documentReference)
        }
    }

    fun updateRating(noteRef: DocumentReference, rating: Rating, onComplete: (Task<Void>) -> Unit){
        viewModelScope.launch {
            val ratingUpdate = noteRepository.updateRating(noteRef, rating)
            onComplete(ratingUpdate)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        noteRepository.removeListener(ratingsListener)
    }


}