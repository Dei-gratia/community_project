package com.nema.eduup.newnote

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.BaseActivity
import com.nema.eduup.BuildConfig
import com.nema.eduup.R
import com.nema.eduup.databinding.ActivityNewNoteBinding
import com.nema.eduup.auth.User
import com.nema.eduup.reminder.ReminderDialog
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.*
import com.nema.eduup.utils.AppConstants.hideKeyboard
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*

class NewNoteActivity : BaseActivity(), View.OnClickListener, View.OnFocusChangeListener {

    private val TAG = NewNoteActivity::class.qualifiedName
    private lateinit var binding: ActivityNewNoteBinding
    private lateinit var tvTitle: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var clTopElements: ConstraintLayout
    private lateinit var tvCreatedDate: TextView
    private lateinit var imgReminder: ImageView
    private lateinit var imgShare: ImageView
    private lateinit var acNoteSubject: AutoCompleteTextView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteDescription: EditText
    private lateinit var etNoteBody: EditText
    private lateinit var tvFileName: TextView
    private lateinit var tvFile: TextView
    private lateinit var btnSave: Button
    private lateinit var noteSubject: String
    private lateinit var noteTitle: String
    private lateinit var noteDescription: String
    private lateinit var noteBody: String
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var chooseFileResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var askStoragePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var requestStoragePermissionsResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var newNote: Note
    private var note: Note = Note()
    private var currentUser: User = User()
    private var selectedFileUri: Uri? = null
    private var selectedFileType: String = "text"
    private var uploadedFileURI: String = ""
    private var userLevel = ""
    private var noteId = "1"
    private var userId = "-1"

    private val viewModel by lazy { ViewModelProvider(this)[NewNoteActivityViewModel::class.java] }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences =getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = ActivityNewNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
            userLevel = currentUser.schoolLevel
        }
        init()
        setupActionBar()

        if (intent.hasExtra(AppConstants.NOTE)){
            note = intent.getParcelableExtra(AppConstants.NOTE)!!
            noteId = note.id
            setNoteDetails()
        }
        if (note == Note()) {
            tvTitle.text = "New Personal Note"
            acNoteSubject.requestFocus()
        }
        setTopElementsVisibility()

        if (this.proposalExists(noteId, note.title)) {
            tvFile.text = "open as pdf"
        } else {
            tvFile.text = "generate pdf"
        }

        NotificationHelper.createNotificationChannel(this,
            NotificationManagerCompat.IMPORTANCE_DEFAULT, false,
            getString(R.string.app_name), "App notification channel.")
        var name = getString(R.string.app_name)
        if (note.title.isNotBlank()) {
            name = note.title
        }
        NotificationHelper.createNotificationChannel(this,
            NotificationManagerCompat.IMPORTANCE_DEFAULT, true,
            name, "Continue where you left of.")

        val subjectAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this,
            android.R.layout.select_dialog_item, AppConstants.subjects.distinct().sorted())
        acNoteSubject.threshold = 1
        acNoteSubject.setAdapter(subjectAdapter)

        requestStoragePermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
            if (result.resultCode == RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        generatePdf {  }
                    } else {
                        Toast.makeText(this, resources.getString(R.string.read_store_permission_denied),
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        askStoragePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map: MutableMap<String, Boolean> ->
            if (!map.values.contains(false)){
                generatePdf {  }
            } else {
                Toast.makeText(this, resources.getString(R.string.read_store_permission_denied),
                    Toast.LENGTH_LONG).show()
            }
        }

        chooseFileResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                tvFileName.visibility = View.VISIBLE
                val data: Intent? = result.data
                selectedFileUri = data?.data
                if (selectedFileUri != null) {
                    try {
                        val file = DocumentFile.fromSingleUri(this, selectedFileUri!!)
                        tvFileName.text = file?.name
                        selectedFileType = AppConstants.getFileExtension(this, selectedFileUri).toString()

                    }catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, resources.getString(R.string.image_selection_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnSave.setOnClickListener(this)
        tvFile.setOnClickListener(this)
        imgShare.setOnClickListener(this)
        imgReminder.setOnClickListener(this)
        acNoteSubject.onFocusChangeListener = this
        etNoteTitle.onFocusChangeListener = this
        etNoteDescription.onFocusChangeListener = this
        etNoteBody.onFocusChangeListener = this
    }

    private fun init() {
        toolbar = binding.toolbarNewNoteActivity
        clTopElements = binding.clTopElements
        tvCreatedDate = binding.tvCreatedDate
        imgReminder = binding.imgReminder
        imgShare = binding.imgShare
        acNoteSubject = binding.etSubject
        etNoteTitle = binding.etTitle
        etNoteDescription = binding.etNoteDescription
        etNoteBody = binding.etNoteBody
        tvFileName = binding.txtFileName
        tvFile = binding.tvFile
        tvTitle = binding.tvTitle
        btnSave = binding.btnSave

    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24)
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setNoteDetails() {
        val formattedDate = DateUtils.getRelativeTimeSpanString(
            note.date,
            Calendar.getInstance().timeInMillis,
            DateUtils.MINUTE_IN_MILLIS
        )
        tvCreatedDate.text = formattedDate
        acNoteSubject.setText(note.subject)
        etNoteTitle.setText(note.title)
        etNoteDescription.setText(note.description)
        etNoteBody.setText(note.body)

    }


    private fun displayCreateReminder() {
        val args = Bundle()
        args.putString(AppConstants.KEY_DATA, noteId)
        args.putString(AppConstants.NOTE_TITLE, note.title)
        val reminderDialog = ReminderDialog.newInstance(args)
        reminderDialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_EduUp)
        reminderDialog.show(supportFragmentManager, ReminderDialog.TAG)
    }


    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        if (view != null) {
            when (view.id) {
                R.id.et_subject, R.id.et_title, R.id.et_note_description, R.id.et_note_body-> {
                    if (!hasFocus) {
                        hideKeyboard(view)
                    }
                }
            }
        }
    }

    override fun onClick(view: View?) {
        if (view != null){
            when (view.id) {
                R.id.btn_save -> {
                    hideKeyboard(view)
                    checkFile()
                }
                R.id.tv_file -> {
                    checkPdf("open")
                }
                R.id.img_reminder -> {
                    //setReminder()
                    displayCreateReminder()
                }
                R.id.img_share -> {
                    checkPdf("share")
                }
            }
        }
    }

    private fun generatePdf(onComplete: () -> Unit) {
        this.buildPdf(note){
            if (it == 1){
                onComplete()
            }
            else {
                val toast = Toast.makeText(this, resources.getString(R.string.pdf_generation_failed), Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
        }
    }

    private fun checkPdf(mode: String) {
        if (this.proposalExists(noteId, note.title)){
            checkMode(mode)
        }
        else {
            generatePdf {
                checkMode(mode)
            }
        }
        tvFile.text = "open as pdf"
    }

    private fun checkMode(mode: String) {
        val pdfs = this.getFiles(note.id)
        for (pdf in pdfs) {
            if (pdf.name == "${note.title}.pdf") {
                if (mode == "share"){
                    sharePdf(pdf)
                }
                else if (mode == "open"){
                    openPdf(pdf)
                }
            }
        }
    }


    private fun openPdf(pdf: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uriFromFile(this, pdf),"application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(Intent.createChooser(intent, "Share Note as pdf"))

    }

    private fun sharePdf(pdf: File) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM,  uriFromFile(this,pdf))
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(Intent.createChooser(intent, "Share Note as pdf"))
    }

    private fun uriFromFile(context:Context, file:File):Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
        } else {
            Uri.fromFile(file)
        }
    }

    private fun setReminder() {
        NotificationHelper.createSampleDataNotification(
            this,
            "Testing",
            "Testing notification",
            "Testing notification big text",
            false
        )
    }

    private fun setTopElementsVisibility() {
        if (note == Note()) {
            clTopElements.visibility = View.GONE
            tvFile.visibility = View.GONE
        }
        else {
            clTopElements.visibility = View.VISIBLE
            tvFile.visibility = View.VISIBLE
        }
    }


    private fun checkFile() {
        if (selectedFileUri != null){
            showProgressDialog(resources.getString(R.string.saving))
            val fileExtension = AppConstants.getFileExtension(this, selectedFileUri)
            val storagePath = AppConstants.USER_FILES + "/" + userId + "/" + AppConstants.NOTES_FILES + "/" + noteTitle
            if (fileExtension != null) {
                viewModel.uploadFileToFirebaseStorage(selectedFileUri, fileExtension , storagePath){
                    if (it != null){
                        uploadedFileURI = it.toString()
                        hideProgressDialog()
                        checkIsNewNote()

                    }
                    else {
                        hideProgressDialog()
                        Toast.makeText(this, "Error uploading File", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            checkIsNewNote()
        }
    }

    override fun onBackPressed() {
        if (checkChanges()) {
            val dialog = android.app.AlertDialog.Builder(this)
            dialog.setTitle("Save note")
            dialog.setMessage("Save changes")
            dialog.setPositiveButton("Save") { _, _ ->
                checkIsNewNote()
                super.onBackPressed()
            }
            dialog.setNegativeButton("No") { mDialog, _ ->
                super.onBackPressed()
            }
            dialog.create()
            dialog.show()
        }
        else {
            super.onBackPressed()
        }
    }

    private fun setNewNote() {
        noteSubject =  acNoteSubject.text.toString()
        noteTitle = etNoteTitle.text.toString()
        noteDescription = etNoteDescription.text.toString()
        noteBody = etNoteBody.text.toString()
        noteId = UUID.randomUUID().toString()
        var time = Calendar.getInstance().timeInMillis
        var avgRating = 0.0
        var numRating = 0L
        var reminder = false
        if (note != Note()) {
            noteId = note.id
            time = note.date
            avgRating = note.avgRating
            numRating = note.numRating
            reminder = note.reminders
        }
        newNote = Note(
            noteId,
            noteSubject,
            noteTitle,
            noteDescription,
            noteBody,
            uploadedFileURI,
            selectedFileType,
            userLevel,
            time,
            avgRating,
            numRating,
            reminder
        )

    }

    private fun checkChanges(): Boolean {
        setNewNote()
        if (!TextUtils.isEmpty(noteTitle)) {
            if (!(TextUtils.isEmpty(noteDescription) && TextUtils.isEmpty(noteBody))) {
                if (newNote != note) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkIsNewNote() {
        if (isNewNote()) {
            saveNewNote()
        }
        else {
            updateNote()
        }
    }

    private fun isNewNote():Boolean {
        return note == Note()
    }

    private fun updateNote() {
        if (checkChanges()) {
            val documentReference = firestoreInstance.collection(AppConstants.USERS).document(this.userId).collection(AppConstants.NOTES)
                .document(noteId)
            viewModel.updateNote(newNote, documentReference ){
                val toast = Toast.makeText(this, resources.getString(R.string.note_saved), Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                note = newNote
                generatePdf {}
                setTopElementsVisibility()
            }
        }
        else {
            val toast = Toast.makeText(this, resources.getString(R.string.note_saved), Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }

    private fun saveNewNote() {
        if (checkChanges()) {
            val collectionReference = firestoreInstance.collection(AppConstants.USERS).document(this.userId).collection(AppConstants.NOTES)
            viewModel.addNoteToFirestore(newNote, collectionReference ) {
                val toast =
                    Toast.makeText(this, resources.getString(R.string.note_saved), Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                note = newNote
                generatePdf{}
                setTopElementsVisibility()
            }
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                generatePdf {  }
            }
            else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s",
                        applicationContext?.packageName
                    ))
                    requestStoragePermissionsResultLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    requestStoragePermissionsResultLauncher.launch(intent)
                }
            }
        }else {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                generatePdf {  }
            }else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                askStoragePermissions.launch(permissions)
            }
        }
    }

    private fun showFileChooser(){
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        chooseFileResultLauncher.launch(Intent.createChooser(intent, "Select a file"))
    }

}