package com.nema.eduup.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.nema.eduup.auth.User
import com.nema.eduup.roomDatabase.*
import com.nema.eduup.viewnote.ViewNoteActivity
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = ViewNoteActivity::class.qualifiedName
    val allReminders: LiveData<List<Reminder>>
    private val reminderDao: ReminderDao
    private lateinit var reminder: Reminder

    init {
        val noteDb = NoteRoomDatabase.getDatabase(application)
        reminderDao = noteDb!!.reminderDao()
        allReminders = reminderDao.getAllReminders
    }


    fun insertReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.insert(reminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.updateReminder(reminder)
        }
    }

    fun getReminderById(reminderId: Int, onComplete: (Reminder?) -> Unit){
        viewModelScope.launch {
            reminder = reminderDao.getReminderById(reminderId)
            onComplete(reminderDao.getReminderById(reminderId))
        }
    }


}