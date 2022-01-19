package com.nema.eduup.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nema.eduup.R
import com.nema.eduup.reminder.ReminderDialog
import com.nema.eduup.roomDatabase.Reminder
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.WEEK_OF_YEAR

object AlarmScheduler {

    private val TAG = AlarmScheduler::class.qualifiedName

    fun scheduleAlarmsForReminder(context: Context, reminder: Reminder) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val days = context.resources.getStringArray(R.array.days)

        val sdf = SimpleDateFormat("EEEE")
        val d = Date(reminder.time)
        val day: String = sdf.format(d)
        val alarmIntent = createPendingIntent(context, reminder, day)
        val dayOfWeek = getDayOfWeek(days, day)
        Log.e(TAG , "$reminder, $dayOfWeek, $d, $day")
        scheduleAlarm(reminder, dayOfWeek, alarmIntent, alarmMgr)

    }


    private fun scheduleAlarm(reminder: Reminder, dayOfWeek: Int, alarmIntent: PendingIntent?, alarmMgr: AlarmManager) {
        val datetimeToAlarm = Calendar.getInstance(Locale.getDefault())
        datetimeToAlarm.timeInMillis = reminder.time

        val today = Calendar.getInstance(Locale.getDefault())
        if (shouldNotifyToday(dayOfWeek, today, datetimeToAlarm)) {
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
                datetimeToAlarm.timeInMillis, (1000 * 60 * 60 * 24 * 7).toLong(), alarmIntent)

            return
        }

        datetimeToAlarm.roll(WEEK_OF_YEAR, 1)
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
            datetimeToAlarm.timeInMillis, (1000 * 60 * 60 * 24 * 7).toLong(), alarmIntent)
    }


    private fun createPendingIntent(context: Context, reminder: Reminder, day: String?): PendingIntent? {
        val intent = Intent(context.applicationContext, AlarmReceiver::class.java).apply {

            action = context.getString(R.string.action_notify_continue_studying)

            type = "$day-${reminder.name}-${reminder.type.name}"

            putExtra(AppConstants.KEY_ID, reminder.id)
        }

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    }


    private fun shouldNotifyToday(dayOfWeek: Int, today: Calendar, datetimeToAlarm: Calendar): Boolean {
        return dayOfWeek == today.get(Calendar.DAY_OF_WEEK) &&
                today.get(Calendar.HOUR_OF_DAY) <= datetimeToAlarm.get(Calendar.HOUR_OF_DAY) &&
                today.get(Calendar.MINUTE) <= datetimeToAlarm.get(Calendar.MINUTE)
    }


    fun updateAlarmsForReminder(context: Context, reminder: Reminder) {
        if (!reminder.opened) {
            scheduleAlarmsForReminder(context, reminder)
        } else {
            removeAlarmsForReminder(context, reminder)
        }
    }


    fun removeAlarmsForReminder(context: Context, reminder: Reminder) {
        val intent = Intent(context.applicationContext, AlarmReceiver::class.java)
        intent.action = context.getString(R.string.action_notify_continue_studying)
        intent.putExtra(AppConstants.KEY_ID, reminder.id)

        val sdf = SimpleDateFormat("EEEE")
        val d = Date(reminder.time)
        val day: String = sdf.format(d)
        val type = String.format(Locale.getDefault(), "%s-%s-%s-%s", day, reminder.name, reminder.desc, reminder.type.name)

        intent.type = type
        val alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmMgr.cancel(alarmIntent)

    }


    private fun getDayOfWeek(days: Array<String>, dayOfWeek: String): Int {
        return when {
            dayOfWeek.equals(days[0], ignoreCase = true) -> Calendar.SUNDAY
            dayOfWeek.equals(days[1], ignoreCase = true) -> Calendar.MONDAY
            dayOfWeek.equals(days[2], ignoreCase = true) -> Calendar.TUESDAY
            dayOfWeek.equals(days[3], ignoreCase = true) -> Calendar.WEDNESDAY
            dayOfWeek.equals(days[4], ignoreCase = true) -> Calendar.THURSDAY
            dayOfWeek.equals(days[5], ignoreCase = true) -> Calendar.FRIDAY
            else -> Calendar.SATURDAY
        }
    }
}