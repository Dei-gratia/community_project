package com.nema.eduup.downloads

import android.app.Application
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.nema.eduup.roomDatabase.Download
import com.nema.eduup.roomDatabase.DownloadDao
import com.nema.eduup.roomDatabase.NoteRoomDatabase
import com.nema.eduup.viewnote.ViewNoteActivity
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = ViewNoteActivity::class.qualifiedName
    val allDownloads: LiveData<List<Download>>
    private val downloadDao: DownloadDao

    init {
        val noteDb = NoteRoomDatabase.getDatabase(application)
        downloadDao = noteDb!!.downloadDao()
        allDownloads = downloadDao.allDownloads
    }


    fun insertDownload(download: Download) {
        viewModelScope.launch {
            downloadDao.insert(download)
        }
    }
}