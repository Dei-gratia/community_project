/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.nema.eduup.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.nema.eduup.R
import com.nema.eduup.utils.AlarmScheduler
import com.nema.eduup.utils.AppConstants


class AppGlobalReceiver : BroadcastReceiver() {

  companion object {
    const val NOTIFICATION_ID = "notification_id"
  }

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context != null && intent != null && intent.action != null) {
      val viewModel by lazy { ViewModelProvider(context as ViewModelStoreOwner)[ReminderViewModel::class.java] }


      // Handle the action to set the Medicine Administered
      if (intent.action!!.equals(context.getString(R.string.action_studied), ignoreCase = true)) {

        val extras = intent.extras
        if (extras != null) {

          val notificationId = extras.getInt(NOTIFICATION_ID)

          val reminderId = extras.getInt(AppConstants.KEY_ID)
          val noteOpened = extras.getBoolean(AppConstants.KEY_ADMINISTERED)

          // Lookup the reminder for sanity

           viewModel.getReminderById(reminderId){
             if (it != null) {

               // Update the database
               it.opened = noteOpened
               viewModel.updateReminder(it)

               // Remove the alarm
               AlarmScheduler.removeAlarmsForReminder(context, it)
             }
           }

          // finally, cancel the notification
          if (notificationId != -1) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(notificationId)
            notificationManager.cancelAll() // testing
          }
        }
      }
    }
  }
}