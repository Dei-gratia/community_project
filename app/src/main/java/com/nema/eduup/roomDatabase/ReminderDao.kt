package com.nema.eduup.roomDatabase

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: Reminder)

    @get:Query("SELECT * FROM reminder ORDER BY time DESC")
    val getAllReminders: LiveData<List<Reminder>>

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("SELECT * FROM reminder WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: String): Reminder

    @Update
    suspend fun updateReminder(reminder: Reminder)
}