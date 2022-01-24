package com.nema.eduup.home

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.repository.NoteRepository
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants

class HomeActivityViewModel(app: Application): AndroidViewModel(app) {

    private lateinit var currentUser: User
    private var noteRepository = NoteRepository
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val sharedPreferences = app.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences

    val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
    var navDrawerDisplaySelectionName = "com.nema.eduup.activities.viewmodels.HomeFragmentViewModel.navDrawerDisplaySelection"
    var navDrawerDisplaySelection = R.id.fragmentHome
    var recentlyViewNoteIdsName = "com.nema.eduup.activities.viewmodels.HomeFragmentViewModel.recentlyViewedNoteIds"

    private val maxRecentlyViewedNotes = 10
    val recentlyViewedNotes = ArrayList<Note>(maxRecentlyViewedNotes)

    fun addNoteToFirestore(note: Note, collection: CollectionReference, onComplete: () -> Unit) {
        noteRepository.addNoteToFirestore(note,collection) {
            onComplete()
        }
    }

    fun addNoteToFirestore1(note: Note, documentReference: DocumentReference, onComplete: () -> Unit) {
        noteRepository.addNoteToFirestore1(note,documentReference) {
            onComplete()
        }
    }


}