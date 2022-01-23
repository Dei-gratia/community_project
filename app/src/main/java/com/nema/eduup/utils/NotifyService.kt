package com.nema.eduup.utils

import android.app.Service
import android.app.NotificationChannel

import android.app.NotificationManager
import android.app.PendingIntent

import android.content.Intent
import android.os.Build

import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nema.eduup.R
import com.nema.eduup.quiz.PractiseQuestionsActivity
import java.lang.UnsupportedOperationException


class NotifyService: Service() {
    private val default_notification_channel_id = "default"

    fun NotifyService() {}


    override fun onBind(intent: Intent?): IBinder? {
        val notificationIntent = Intent(applicationContext, PractiseQuestionsActivity::class.java)
        notificationIntent.putExtra("fromNotification", true)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val mBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, default_notification_channel_id)
        mBuilder.setContentTitle("My Notification")
        mBuilder.setContentIntent(pendingIntent)
        mBuilder.setContentText("Notification Listener Service Example")
        mBuilder.setSmallIcon(R.drawable.ic_quiz_black_24)
        mBuilder.setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                "NOTIFICATION_CHANNEL_NAME",
                importance
            )
            mBuilder.setChannelId(AppConstants.NOTIFICATION_CHANNEL_ID)
            mNotificationManager.createNotificationChannel(notificationChannel)
        }
        mNotificationManager.notify(System.currentTimeMillis().toInt(), mBuilder.build())
        throw UnsupportedOperationException("Not yet implemented")
    }
}