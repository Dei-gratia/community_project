package com.nema.eduup.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.nema.eduup.R
import com.nema.eduup.reminder.ReminderViewModel

class AlarmReceiver: BroadcastReceiver(){


  override fun onReceive(context: Context?, intent: Intent?) {

    val viewModel by lazy { ViewModelProvider(context as ViewModelStoreOwner)[ReminderViewModel::class.java] }

    if (context != null && intent != null && intent.action != null) {

      if (intent.action!!.equals(context.getString(R.string.action_notify_continue_studying), ignoreCase = true)) {
        if (intent.extras != null) {

          intent.extras!!.getInt(AppConstants.KEY_ID)
            ?.let { viewModel.getReminderById(it){ reminder ->
              if (reminder != null) {

                NotificationHelper.createNotificationForNote(context, reminder)
              }
            }
          }
        }
      }
    }
  }
}