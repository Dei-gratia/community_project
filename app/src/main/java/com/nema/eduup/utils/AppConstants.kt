package com.nema.eduup.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import java.lang.Exception
import java.util.*
import android.text.format.DateUtils
import androidx.core.content.ContextCompat.startActivity


object AppConstants {

    const val APP_THEME: String = "appTheme"
    const val EDUUP_PREFERENCES = "EduUpPreferences"
    const val USERS = "users"
    const val REMINDERS = "reminders"
    const val PUBLIC_NOTES = "publicNotes"
    const val BOOKMARKS = "bookmarks"
    const val NOTE_RATINGS = "noteRatings"
    const val USER_NAME = "user_name"
    const val USER_ID = "user_id"
    const val USER_EMAIL = "user_name"
    const val USER_IMAGE_URL = "user_image_url"
    const val USER_LEVEL = "user_level"
    const val CURRENT_USER = "current_user"
    const val READ_STORAGE_PERMISSION_CODE = 0
    const val PICK_IMAGE_REQUEST_CODE = 1
    const val SLIDER_IMAGE_URLS = "sliderImageURLS"

    //view note constants
    const val PERSONAL_NOTE = "personalNote"

    //messaging constants
    const val GROUP_CHAT_CHANNELS = "groupChatChannels"
    const val CHAT_CHANNELS = "chatChannels"
    const val ACTIVE_GROUP_CHANNELS = "activeGroupChannels"
    const val ACTIVE_CHAT_CHANNELS = "activeChatChannels"
    const val CHAT_FILES = "chatChannels"
    const val CHANNEL_ID = "channelId"
    const val TIME = "time"
    const val DISCUSSION_STATUS = "discussionStatus"
    const val MESSAGES = "messages"
    const val USER_IDS = "userIds"
    const val GROUP_IMAGE_URL = "groupImageUrl"
    const val GROUP_NAME = "groupName"

    //firestore user fields
    const val FIRST_NAMES = "firstNames"
    const val FAMILY_NAME = "familyName"
    const val EMAIL = "email"
    const val IMAGE_URL = "imageUrl"
    const val MOBILE = "mobile"
    const val GENDER = "gender"
    const val SCHOOL_LEVEL = "schoolLevel"
    const val ABOUT = "about"
    const val SCHOOL = "school"
    const val PROGRAM = "program"
    const val COMPLETE_PROFILE = "profileCompleted"
    const val REGISTRATION_TOKENS = "registrationTokens"

    const val MALE: String = "Male"
    const val FEMALE: String = "Female"

    //firestore note fields
    const val NOTE = "note"
    const val NOTES = "notes"
    const val DATE = "date"

    //firebase storage folders
    const val USER_FILES = "userFiles"
    const val PROFILE_PHOTOS = "profilePhotos"
    const val NOTES_FILES = "noteFiles"
    const val GROUP_FILES = "groupFiles"

    //new note constants
    const val NEW_NOTE = "new_note"
    const val UPLOAD_NOTE = "upload_note"


    fun getFileExtension(activity: Activity, uri: Uri?): String? {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(activity.contentResolver.getType(uri!!))
    }

    fun checkInternetConnection(context: Context): Boolean {
        if (ConnectionManager().isNetworkAvailable(context)){
            return true
        }
        else {
            val dialog = android.app.AlertDialog.Builder(context)
            dialog.setTitle("Error")
            dialog.setMessage("No Internet Connection")
            dialog.setPositiveButton("Open Settings") { _, _ ->
                val settingsIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                context.startActivity(settingsIntent)
            }
            dialog.setNegativeButton("Exit") { _, _ ->

            }
            dialog.create()
            dialog.show()
        }
        return false
    }

    fun Date.getTimeAgo(): String {
        val calendar = Calendar.getInstance()
        calendar.time = this

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val currentCalendar = Calendar.getInstance()

        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
        val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentCalendar.get(Calendar.MINUTE)

        return if (year < currentYear ) {
            val interval = currentYear - year
            if (interval == 1) "$interval year ago" else "$interval years ago"
        } else if (month < currentMonth) {
            val interval = currentMonth - month
            if (interval == 1) "$interval month ago" else "$interval months ago"
        } else  if (day < currentDay) {
            val interval = currentDay - day
            if (interval == 1) "$interval day ago" else "$interval days ago"
        } else if (hour < currentHour) {
            val interval = currentHour - hour
            if (interval == 1) "$interval hour ago" else "$interval hours ago"
        } else if (minute < currentMinute) {
            val interval = currentMinute - minute
            if (interval == 1) "$interval minute ago" else "$interval minutes ago"
        } else {
            "a moment ago"
        }
    }

    fun isYesterday(d: Date): Boolean {
        return DateUtils.isToday(d.time + DateUtils.DAY_IN_MILLIS)
    }


}

