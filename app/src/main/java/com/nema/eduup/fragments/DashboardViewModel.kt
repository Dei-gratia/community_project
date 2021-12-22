package com.nema.eduup.fragments

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.roomDatabase.NoteDao
import com.nema.eduup.roomDatabase.NoteRoomDatabase

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    val allNotes: LiveData<List<Note>>
    private val noteDao: NoteDao

    init {
        val noteDb = NoteRoomDatabase.getDatabase(application)
        noteDao = noteDb!!.noteDao()
        allNotes = noteDao.allNotes
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
}