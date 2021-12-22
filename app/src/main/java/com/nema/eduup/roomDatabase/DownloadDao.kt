package com.nema.eduup.roomDatabase

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DownloadDao {
    @Insert
    fun insert(download: Download)

    @get:Query("SELECT * FROM download ORDER BY date DESC")
    val allDownloads: LiveData<List<Download>>
}