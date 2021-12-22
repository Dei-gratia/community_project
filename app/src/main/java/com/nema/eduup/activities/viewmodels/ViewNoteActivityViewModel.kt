package com.nema.eduup.activities.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.models.Rating
import com.nema.eduup.repository.FirestoreUtil
import com.nema.eduup.roomDatabase.Download
import com.nema.eduup.roomDatabase.DownloadDao
import com.nema.eduup.roomDatabase.NoteRoomDatabase
import kotlinx.coroutines.launch


class ViewNoteActivityViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver  {

    private lateinit var ratingsListener: ListenerRegistration
    private var firestoreUtil = FirestoreUtil
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
        viewModelScope.launch {
            downloadDao.insert(download)
        }
    }


    fun getUserRating(documentReference: DocumentReference, onComplete: (Rating?) -> Unit) {
        viewModelScope.launch {
            firestoreUtil.loadUserReview(documentReference){
                onComplete(it)
            }
        }
    }

    fun noteRatings(collection: CollectionReference): LiveData<List<Rating>>{
        viewModelScope.launch {
            ratingsListener = firestoreUtil.addRatingListener(collection) { listRatings, numRatings ->
                noteRatings.value = listRatings
                numRating.value = numRatings
            }
        }
        return  noteRatings
    }

    fun deleteReview(documentReference: DocumentReference) {
        viewModelScope.launch {
            firestoreUtil.deleteReview(documentReference)
        }
    }

    fun updateRating(noteRef: DocumentReference, rating: Rating, onComplete: (Task<Void>) -> Unit){
        viewModelScope.launch {
            val ratingUpdate = firestoreUtil.updateRating(noteRef, rating)
            onComplete(ratingUpdate)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        firestoreUtil.removeListener(ratingsListener)
    }


}