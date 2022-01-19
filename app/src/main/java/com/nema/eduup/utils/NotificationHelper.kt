package com.nema.eduup.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nema.eduup.R
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.reminder.AppGlobalReceiver
import com.nema.eduup.roomDatabase.Reminder

object NotificationHelper {

    fun createNotificationChannel(context: Context, importance: Int, showBadge: Boolean, name: String, description: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


            val channelId = "${context.packageName}-$name"
            val channel = NotificationChannel(channelId, name, importance)
            channel.description = description
            channel.setShowBadge(showBadge)


            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createSampleDataNotification(context: Context, title: String, message: String, bigText: String, autoCancel: Boolean) {

        val channelId = "${context.packageName}-${context.getString(R.string.app_name)}"

        val notificationBuilder = NotificationCompat.Builder(context, channelId).apply {
            setSmallIcon(R.drawable.ic_edit_black_24)
            setContentTitle(title)
            setContentText(message)
            setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            priority = NotificationCompat.PRIORITY_DEFAULT // 7
            setAutoCancel(autoCancel)

            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            setContentIntent(pendingIntent)
        }


        val notificationManager = NotificationManagerCompat.from(context)

        notificationManager.notify(1001, notificationBuilder.build())
    }

    fun createNotificationForNote(context: Context, reminder: Reminder) {

        val groupBuilder = buildGroupNotification(context, reminder)

        val notificationBuilder = buildNotificationFor(context, reminder)

        val administerPendingIntent = createPendingIntentForAction(context, reminder)
        notificationBuilder?.addAction(
            R.drawable.ic_check_black_24,
            context.getString(R.string.study),
            administerPendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(reminder.type.ordinal, groupBuilder.build())
        notificationManager.notify(reminder.id, notificationBuilder.build())
    }

    private fun buildGroupNotification(context: Context, reminderData: Reminder): NotificationCompat.Builder {

            val channelId = "${context.packageName}-${reminderData.type.name}"
            return NotificationCompat.Builder(context, channelId).apply {
                setSmallIcon(R.drawable.ic_edit_black_24)
                setContentTitle(reminderData.type.name)
                setContentText(context.getString(R.string.group_notification_for, reminderData.type.name))
                setStyle(NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.group_notification_for, reminderData.type.name)))
                setAutoCancel(true)
                setGroupSummary(true) // 2
                setGroup(reminderData.type.name) // 3
            }
        }
    }

private fun buildNotificationFor(context: Context, reminder: Reminder): NotificationCompat.Builder {
    // 1
    val channelId = "${context.packageName}-${reminder.type.name}"
    return NotificationCompat.Builder(context, channelId).apply {
        setSmallIcon(R.drawable.ic_edit_black_24)
        setContentTitle(reminder.name)
        setAutoCancel(true)
        // 2
        val drawable = when (reminder.type) {
            Reminder.ReminderType.Study -> R.drawable.ic_event_note_black_24
            Reminder.ReminderType.Share -> R.drawable.ic_share_black_24
            else -> R.drawable.ic_edit_black_24
        }
        // 3
        setLargeIcon(BitmapFactory.decodeResource(context.resources, drawable))
        setContentText("${reminder.name}, ${reminder.desc}")
        // 4
        setGroup(reminder.type.name)
        if (reminder.note != null) {
            setStyle(NotificationCompat.BigTextStyle().bigText(reminder.note))
        }
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(AppConstants.KEY_ID, reminder.id)
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        setContentIntent(pendingIntent)
    }
}



private fun createPendingIntentForAction(context: Context, reminder: Reminder): PendingIntent? {
    // 1
    val administerIntent = Intent(context, AppGlobalReceiver::class.java).apply {
        action = context.getString(R.string.action_studied)
        putExtra(AppGlobalReceiver.NOTIFICATION_ID, reminder.id)
        putExtra(AppConstants.KEY_ID, reminder.id)
        putExtra(AppConstants.KEY_ADMINISTERED, true)
    }
// 2
    return PendingIntent.getBroadcast(context, AppConstants.STUDY_REQUEST_CODE, administerIntent, PendingIntent.FLAG_UPDATE_CURRENT)

}