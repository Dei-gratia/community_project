package com.nema.eduup.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject
import com.nema.eduup.discussions.*
import com.nema.eduup.auth.User
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.viewnote.Rating
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object NoteRepository {


    private val TAG = NoteRepository::class.qualifiedName
    private val userRepository = UserRepository
    private val currentUserId = userRepository.getCurrentUserID()
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }


    fun updateRating(noteRef: DocumentReference, rating: Rating): Task<Void> {

        val ratingRef = noteRef.collection(AppConstants.NOTE_RATINGS).document(currentUserId)

        return firestoreInstance.runTransaction { transaction ->

            val note = transaction.get(noteRef).toObject<Note>()!!

            val newNumRatings = note.numRating.plus(1)

            val oldRatingTotal = note.numRating.let { note.avgRating.times(it) }

            val newAvgRating = (oldRatingTotal.plus(rating.rateValue)).div(newNumRatings)


            note.numRating = newNumRatings
            note.avgRating = newAvgRating

            transaction.set(noteRef, note)

            transaction.set(ratingRef, rating)

            null
        }

    }

    fun deleteDocument(documentReference: DocumentReference) {
        documentReference
            .delete()

    }

    fun addNoteToFirestore(note: Note, collection: CollectionReference, onComplete: () -> Unit) {
        collection
            .add(note)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding note", e)
                return@addOnFailureListener
            }
    }

    fun updateNote(note: Note, documentReference: DocumentReference, onComplete: () -> Unit) {
        documentReference
            .set(note)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating note", e)
                return@addOnFailureListener
            }
    }

    private fun seedReminders(onComplete: (ArrayList<Note>) -> Unit) {
        val remindersList = java.util.ArrayList<Note>()
        val remindersCollection = firestoreInstance.collection(AppConstants.REMINDERS)
        var note =
            Note(
                "",
                "",
                "Welcome to EduUp",
                "This is a great app to get and manage all your study materials",
                "",
                "",
                "",
                "all",
                Calendar.getInstance().timeInMillis,
                0.0,
                0,
                true
            )
        addNoteToFirestore(note, remindersCollection){
            remindersList.add(note)
        }

        note = Note(
            "",
            "",
            "EduUp",
            "Educational app to help your get all the most relevant study material in one place",
            "",
            "",
            "",
            "all",
            Calendar.getInstance().timeInMillis - 10000,
            0.0,
            0,
            true
        )

        addNoteToFirestore(note, remindersCollection){
            remindersList.add(note)
            onComplete(remindersList)
        }
    }

    fun addBookmark(noteId: String){
        firestoreInstance.collection(AppConstants.USERS).document(currentUserId).collection(AppConstants.BOOKMARKS).document(noteId)
            .set(mapOf(AppConstants.NOTE_ID to noteId))
            .addOnSuccessListener { result ->

            }
            .addOnFailureListener { e -> Log.e(TAG, e.message.toString(), e)}
    }

    fun removeBookmark(noteId: String){
        firestoreInstance.collection(AppConstants.USERS).document(currentUserId).collection(AppConstants.BOOKMARKS).document(noteId)
            .delete()
    }

    fun addToHistory(noteId: String){
        firestoreInstance.collection(AppConstants.USERS).document(currentUserId).collection(AppConstants.HISTORY).document(noteId)
            .set(mapOf(AppConstants.NOTE_ID to noteId))
            .addOnSuccessListener { result ->

            }
            .addOnFailureListener { e -> Log.e(TAG, e.message.toString(), e)}
    }



    fun getNotesByIds(notesCollection: CollectionReference, noteIds: ArrayList<String>, onComplete: (ArrayList<Note>) -> Unit) {
        val noteTasks = noteIds.map { notesCollection.document(it).get() }
        Tasks.whenAllSuccess<DocumentSnapshot>(noteTasks)
            .addOnSuccessListener { documentList ->
                val notesList = java.util.ArrayList<Note>()
                if (documentList.isNotEmpty()) {
                    for (document in documentList) {
                        notesList.add(parseNoteDocument(document))
                    }
                }
                onComplete(notesList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting notes", e)
                return@addOnFailureListener
            }
    }


    fun loadUserReview(documentReference: DocumentReference, onComplete: (Rating?) -> Unit) {
        documentReference.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val review = document.toObject(Rating::class.java)!!
                    onComplete (review)
                }else {
                    onComplete (null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, e.message.toString())
            }
    }

    fun addRatingListener(collection: CollectionReference, onListen: (MutableList<Rating>, Int) -> Unit): ListenerRegistration {
        val ratings = mutableListOf<Rating>()
        var numRating = 0
        return collection
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e(TAG, "Ratings listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }


                for (document in querySnapshot!!.documentChanges) {
                    when (document.type ) {
                        DocumentChange.Type.ADDED -> {
                            numRating += 1
                            if (document.document.id != currentUserId) {
                                ratings.add(document.document.toObject())
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            numRating -= 1
                            if (document.document.id != currentUserId) {
                                ratings.remove(document.document.toObject())
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            if (document.document.id != currentUserId){
                                val rating: Rating  = document.document.toObject()
                                val ratingPosition = ratings.indexOf(ratings.find { it.userName == rating.userName })
                                ratings[ratingPosition] = rating
                            }
                        }

                    }
                }
                onListen(ratings, numRating)
            }
    }





    fun addRemindersListener(collection: CollectionReference, onListen: (MutableList<Note>) -> Unit): ListenerRegistration{
        val reminders = mutableListOf<Note>()
        return collection
           .orderBy(AppConstants.DATE, Query.Direction.DESCENDING)
           .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
               if (firebaseFirestoreException != null) {
                   Log.e(TAG, "Reminders listener error.", firebaseFirestoreException)
                   return@addSnapshotListener
               }
               if (querySnapshot!!.isEmpty) {
                   seedReminders(){

                   }
               }
               for (document in querySnapshot!!.documentChanges) {
                   if (document.type == DocumentChange.Type.ADDED) {
                       reminders.add(parseNoteDocument(document.document))
                   }
                   else if (document.type == DocumentChange.Type.REMOVED) {
                       reminders.remove(parseNoteDocument(document.document))
                   }
               }
               onListen(reminders)
           }

    }

    fun addNotesListener(collection: CollectionReference, onListen: (MutableList<Note>) -> Unit): ListenerRegistration{
        val notes = mutableListOf<Note>()
        return collection
            .orderBy(AppConstants.DATE, Query.Direction.ASCENDING)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e(TAG, "Notes listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                for (document in querySnapshot!!.documentChanges) {
                    when (document.type) {
                        DocumentChange.Type.ADDED -> {
                            notes.add(0, parseNoteDocument(document.document))
                        }
                        DocumentChange.Type.REMOVED -> {
                            notes.remove(parseNoteDocument(document.document))
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val note = parseNoteDocument(document.document)
                            val notePosition = notes.indexOf(notes.find { it.id == note.id })
                            notes[notePosition] = note
                        }
                    }
                }
                onListen(notes)

            }
    }

    fun addLevelNotesListener(level: String, onListen: (MutableList<Note>) -> Unit): ListenerRegistration{
        val notes = mutableListOf<Note>()
        return firestoreInstance.collectionGroup("${level}${AppConstants.PUBLIC_NOTES}")
            .orderBy(AppConstants.DATE, Query.Direction.ASCENDING)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e(TAG, "Notes listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                for (document in querySnapshot!!.documentChanges) {
                    when (document.type) {
                        DocumentChange.Type.ADDED -> {
                            notes.add(0, parseNoteDocument(document.document))
                        }
                        DocumentChange.Type.REMOVED -> {
                            notes.remove(parseNoteDocument(document.document))
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val note = parseNoteDocument(document.document)
                            val notePosition = notes.indexOf(notes.find { it.id == note.id })
                            notes[notePosition] = note
                        }
                    }
                }
                onListen(notes)

            }
    }

    fun addBookmarksListener1(collection: CollectionReference, onListen: (MutableList<Note>) -> Unit): ListenerRegistration {
        val bookmarks = mutableListOf<Note>()
        return collection
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Bookmarks listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                for (document in querySnapshot!!.documentChanges) {
                    if (document.type == DocumentChange.Type.ADDED) {
                        bookmarks.add(parseNoteDocument(document.document))
                    }
                    else if (document.type == DocumentChange.Type.REMOVED) {
                        bookmarks.remove(parseNoteDocument(document.document))
                    }
                }
                onListen(bookmarks)
            }

    }

    fun addBookmarksListener(collection: CollectionReference, onListen: (MutableList<String>) -> Unit): ListenerRegistration {
        val bookmarks = mutableListOf<String>()
        return collection
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e("FIRESTORE", "Bookmarks listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                for (document in querySnapshot!!.documentChanges) {
                    if (document.type == DocumentChange.Type.ADDED) {
                        bookmarks.add(document.document.id)
                    }
                    else if (document.type == DocumentChange.Type.REMOVED) {
                        bookmarks.remove(document.document.id)
                    }
                }
                onListen(bookmarks)
            }

    }

    fun removeListener(registration: ListenerRegistration) = registration.remove()

    private fun parseNoteDocument(document: DocumentSnapshot): Note {
        return Note(
            document.id,
            document.getString("subject")!!,
            document.getString("title")!!,
            document.getString("description")!!,
            document.getString("body")!!,
            document.getString("fileUrl")!!,
            document.getString("fileType")!!,
            document.getString("level")!!,
            document.getLong("date")!!,
            document.getDouble("avgRating")!!,
            document.getLong("numRating")!!,
            document.getBoolean("reminders")!!
        )
    }
}