package com.nema.eduup.activities.viewmodels

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.nema.eduup.roomDatabase.Download
import com.nema.eduup.roomDatabase.DownloadDao
import com.nema.eduup.roomDatabase.NoteRoomDatabase

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    val allDownloads: LiveData<List<Download>>
    private val downloadDao: DownloadDao

    init {
        val noteDb = NoteRoomDatabase.getDatabase(application)
        downloadDao = noteDb!!.downloadDao()
        allDownloads = downloadDao.allDownloads
    }

    fun insertDownload(download: Download) {
        InsertAsyncTask(downloadDao).execute(download)
    }

    companion object {
        private class InsertAsyncTask(private val downloadDao: DownloadDao) : AsyncTask<Download, Void, Void>() {
            override fun doInBackground(vararg downloads: Download): Void? {
                downloadDao.insert(downloads[0])
                return null
            }

        }
    }
}