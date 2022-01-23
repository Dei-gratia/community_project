package com.nema.eduup.utils

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.DocumentsContract
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import java.lang.Exception
import java.util.*
import android.text.format.DateUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.nema.eduup.R
import com.nema.eduup.quiz.Question
import com.nema.eduup.repository.FirebaseStorageUtil
import java.io.File
import java.util.regex.Pattern


object AppConstants {

    private val TAG = AppConstants::class.qualifiedName
    const val APP_THEME: String = "appTheme"
    const val EDUUP_PREFERENCES = "EduUpPreferences"
    const val USERS = "users"
    const val REMINDERS = "reminders"
    const val PUBLIC_NOTES = "publicnotes"
    const val PUBLIC_QUIZZES = "publicquizzes"
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
    const val USER_IMAGE_URI = "userImageUrl"
    const val IMAGE_URI = "imageUri"
    const val IMAGE_TITLE = "imageTitle"
    const val SELECTED_IMAGE_BYTES = "selectedImageBytes"
    const val NOTE_ID = "noteId"
    const val NOTE_TITLE = "noteTitle"
    const val HISTORY = "history"
    const val ALL_SUBJECTS = "allSubjects"
    const val ALL_LEVELS = "allLevels"
    const val NOTIFICATION_CHANNEL_ID = "10001"

    //view note constants
    const val PERSONAL_NOTE = "personalNote"

    //quizzes
    const val QUIZZES = "quizzes"
    const val QUIZ = "quiz"
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val SUBJECT = "subject"
    const val LEVEL = "level"
    const val TOTAL_QUESTIONS = "totalQuestions"
    const val DURATION = "duration"
    const val COLLEGE = "college"
    const val QUIZ_ID = "quizId"
    const val QUESTIONS = "questions"
    const val QUIZZES_LEVEL = "quizzesLevel"
    const val QUIZZES_SUBJECT = "quizzesSubject"
    const val NOTES_LEVEL = "notesLevel"
    const val NOTES_SUBJECT = "notesSubject"
    const val CORRECT_QUESTIONS = "correctQuestions"
    const val SKIPPED_QUESTION = "skippedQuestions"
    const val WRONG_QUESTIONS = "wrongQuestions"

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
    const val NICKNAME = "nickname"
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

    // key for passed in data
    const val KEY_DATA = "reminder_data"
    const val KEY_ID = "id"
    const val KEY_ADMINISTERED = "administered"

    // opcodes for success
    const val REMINDER_CREATED = 0
    const val REMINDER_UPDATED = 1
    const val REMINDER_DELETED = 2

    // error states for validation
    const val ERROR_NO_NAME = 0
    const val ERROR_NO_TIME = 2
    const val ERROR_NO_DAYS = 3
    const val ERROR_NO_DESC = 4
    const val ERROR_SAVE_FAILED = 5
    const val ERROR_DELETE_FAILED = 6
    const val ERROR_UPDATE_FAILED = 7
    const val STUDY_REQUEST_CODE = 8

    val levels = arrayOf("All Levels","College", "A Level", "O Level", "Primary")


    fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

    fun isValidPassword(password: String?) : Boolean {
        password?.let {
            val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{4,}$"
            val passwordMatcher = Regex(passwordPattern)

            return passwordMatcher.find(password) != null
        } ?: return false
    }

    fun ImageView.setTint(@ColorRes colorRes: Int) {
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)))
    }

    fun getFileExtension(activity: Activity, uri: Uri?): String? {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(activity.contentResolver.getType(uri!!))
    }

    fun uriExist(context: Context, uri: Uri): Boolean {
        return when(uri.scheme) {
            "content" -> {
                if (DocumentsContract.isDocumentUri(context, uri))
                    documentUriExists(context,uri)

                else // Content URI is not from a document provider
                    contentUriExists(context,uri)
            }
            "file" -> {
                File(uri.path).exists()
            }
            else -> false
        }

    }

    private fun documentUriExists(context: Context, uri: Uri): Boolean =
        resolveUri(context,uri, DocumentsContract.Document.COLUMN_DOCUMENT_ID)

    private fun contentUriExists(context: Context,uri: Uri): Boolean =
        resolveUri(context,uri, BaseColumns._ID)

    private fun resolveUri(context: Context, uri: Uri, column: String): Boolean {

        val cursor = context.contentResolver.query(uri,
            arrayOf(column), // Empty projections are bad for performance
            null,
            null,
            null)

        val result = cursor?.moveToFirst() ?: false

        cursor?.close()

        return result
    }

    fun networkConnection(context: Context): Boolean {
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
            dialog.setNegativeButton("Exit") { mDialog, _ ->
                mDialog.cancel()
            }
            dialog.create()
            dialog.show()
            return false
        }
    }

    fun Double.roundTo(n : Int) : Double {
        return "%.${n}f".format(this).toDouble()
    }

    fun String.isValidMobile() : Boolean {
        val patterns =  "^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$"
        return Pattern.compile(patterns).matcher(this).matches()
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

    fun fileTypeImage(fileType: String): Int{
        var imgFileTypeId = 0
        when (fileType) {
            "pdf" -> {
                imgFileTypeId = R.drawable.image_pdf
            }
            "docx" -> {
                imgFileTypeId = R.drawable.word_file_image
            }
            "txt" -> {
                imgFileTypeId = R.drawable.ic_note_alt_black_24
            }
            "png" -> {
                imgFileTypeId = R.drawable.image_icon
            }
            "mp4" -> {
                imgFileTypeId = R.drawable.video_image
            }
            "ppt" -> {
                imgFileTypeId = R.drawable.ppt_image
            }
        }
        return imgFileTypeId
    }

    fun isYesterday(d: Date): Boolean {
        return DateUtils.isToday(d.time + DateUtils.DAY_IN_MILLIS)
    }

    fun View.setFocusAndKeyboard(){
        this.requestFocus()
        this.showKeyboard()
    }

    fun View.showKeyboard() {
        this.requestFocus()
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun openDownloadedAttachment(context: Context, attachmentUri: Uri, attachmentMimeType: String) {
        var attachmentUri: Uri? = attachmentUri
        if (attachmentUri != null) {
            // Get Content Uri.
            if (ContentResolver.SCHEME_FILE == attachmentUri.scheme) {
                // FileUri - Convert it to contentUri.
                val file = File(attachmentUri.path)
                attachmentUri =
                    FileProvider.getUriForFile(context, "com.nema.eduup.provider", file)
            }
            val openAttachmentIntent = Intent(Intent.ACTION_VIEW)
            openAttachmentIntent.setDataAndType(attachmentUri, attachmentMimeType)
            openAttachmentIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.startActivity(openAttachmentIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.read_store_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }



    fun String.intOrString(): Boolean {
        return when(toIntOrNull()) {
            null -> false
            else -> true
        }
    }

    val subjects = arrayListOf(
        AppConstants.ALL_SUBJECTS,"Accounting", "Business law", "Business management", "Consumer education", "Entrepreneurial skills", "Introduction to business", "Marketing", "Personal finance)",
        "Animation", "App development", "Audio production", "Computer programming", "Computer repair", "Film production", "Graphic design",
        "Media technology", "Music production", "Typing", "Video game development", "Web design", "Web programming", "Word processing",
        "American literature", "British literature", "Contemporary literature", "Creative writing", "Communication skills", "Debate",
        "English language and composition", "English literature and composition", "Humanities", "Journalism", "Literary analysis", "Modern literature", "Poetry",
        "Popular literature", "Rhetoric", "Technical writing", "Works of Shakespeare", "World literature", "Written and oral communication",
        "Chemistry of foods", "CPR training", "Culinary arts", "Early childhood development", "Early childhood education", "Family studies",
        "Fashion and retail merchandising", "Fashion construction", "Home economics", "Interior design", "Nutrition",
        "American Sign Language", "Ancient Greek", "Arabic", "Chinese", "French", "German", "Hebrew", "Italian", "Japanese", "Korean", "Latin",
        "Portuguese", "Russian", "Spanish",
        "Algebra 1", "Algebra 2", "Calculus", "Computer math", "Consumer math", "Fundamentals of math", "Geometry", "Integrated math", "Mathematics",
        "Math applications", "Multivariable calculus", "Practical math", "Pre-algebra", "Pre-calculus", "Probability", "Quantitative literacy",
        "Statistics", "Trigonometry",
        "Choir", "Concert band", "Dance", "Drama", "Guitar", "Jazz band", "Marching band", "Music theory", "Orchestra", "Percussion", "Piano", "Theater technology", "World music",
        "Aerobics", "Gymnastics", "Health", "Lifeguard training", "Pilates", "Racket sports", "Specialized sports", "Swimming", "Weight training", "Yoga",
        "Agriculture", "Astronomy", "Biology", "Botany", "Chemistry", "Earth science", "Electronics", "Environmental science",
        "Environmental studies", "Forensic science", "Geology", "Marine biology", "Oceanography", "Physical science", "Physics", "Zoology",
        "Cultural anthropology", "Current affairs", "European history", "Geography", "Global studies", "History", "Human geography", "International relations", "Law",
        "Macroeconomics", "Microeconomics", "Modern world studies", "Physical anthropology", "Political studies", "Psychology",
        "Religious studies", "Sociology", "Women's studies", "World history", "World politics", "World religions",
        "3-D art", "African history", "Ceramics", "Digital media", "Drawing", "Film production", "Jewelry design", "Painting", "Photography", "Printmaking", "Sculpture",
        "Auto body repair", "Auto mechanics", "Building construction", "Computer-aided drafting", "Cosmetology", "Criminal justice", "Driver education", "Electronics", "Fire science",
        "Heating and cooling systems", "Hospitality and tourism", "JROTC (Junior Reserve Officers' Training Corps)", "Metalworking", "Networking", "Plumbing", "Production technology", "Refrigeration fundamentals", "Robotics", "Woodworking",
        "Research", "Seminar", "Art history", "Music theory", "Studio art: 2-D design", "Studio art: 3-D design", "Studio art: drawing", "English Language and Composition",
        "English Literature and Composition", "Comparative government and politics", "Human Geography", "Macroeconomics", "Microeconomics", "Psychology", "Math & Computer Science", "Computer Science "
    )

}

