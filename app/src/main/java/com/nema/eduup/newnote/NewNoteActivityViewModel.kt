package com.nema.eduup.newnote

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.nema.eduup.repository.FirebaseStorageUtil
import com.nema.eduup.repository.NoteRepository
import com.nema.eduup.roomDatabase.Note

class NewNoteActivityViewModel(app: Application): AndroidViewModel(app) , DefaultLifecycleObserver {

    private val firebaseStorageUtil = FirebaseStorageUtil
    private val noteRepository = NoteRepository

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun uploadFileToFirebaseStorage(fileURI: Uri?, fileExtension: String, storagePath: String, onComplete: (Uri?) -> Unit) {
        firebaseStorageUtil.uploadFileToCloudStorage(fileURI, fileExtension, storagePath) {
            onComplete(it)
        }
    }

    fun addNoteToFirestore(note: Note, collection: CollectionReference, onComplete: () -> Unit) {
        noteRepository.addNoteToFirestore(note,collection) {
            onComplete()
        }
    }

    fun updateNote(note: Note, documentReference: DocumentReference, onComplete: () -> Unit) {
        noteRepository.updateNote(note, documentReference) {
            onComplete ()
        }
    }

}