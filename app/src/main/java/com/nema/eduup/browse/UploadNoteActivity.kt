package com.nema.eduup.browse

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
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import com.nema.eduup.BaseActivity
import com.nema.eduup.R
import com.nema.eduup.databinding.ActivityUploadNoteBinding
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import java.lang.Exception
import java.util.*

class UploadNoteActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityUploadNoteBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var toolbar: Toolbar
    private lateinit var etNoteSubject: AutoCompleteTextView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteDescription: EditText
    private lateinit var etNoteBody: EditText
    private lateinit var txtFileName: TextView
    private lateinit var txtUploadFile: TextView
    private lateinit var btnSave: Button
    private lateinit var noteLevel: String
    private lateinit var noteSubject: String
    private lateinit var noteTitle: String
    private lateinit var noteDescription: String
    private lateinit var noteBody: String
    private lateinit var spinnerLevel: Spinner
    private lateinit var chooseFileResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var askStoragePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var requestStoragePermissionsResultLauncher: ActivityResultLauncher<Intent>
    private var selectedFileUri: Uri? = null
    private var uploadedFileURI: String = ""
    private var selectedFileType: String = "text"
    private var levelPosition = 0
    private val resultIntent = Intent()

    private val viewModel by lazy { ViewModelProvider(this)[UploadNoteViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = ActivityUploadNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setupActionBar()

        val levels = arrayOf("All Levels","College", "A Level", "O Level", "Primary")
        val spinnerAdapter = ArrayAdapter<String>(
            this, R.layout.spinner_item, levels)
        spinnerLevel.adapter = spinnerAdapter
        if (intent.hasExtra(AppConstants.USER_LEVEL)) {
            val userLevel = intent.getStringExtra(AppConstants.USER_LEVEL)
            levelPosition = spinnerAdapter.getPosition(userLevel)
        }
        spinnerLevel.setSelection(levelPosition)

        requestStoragePermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
            if (result.resultCode == RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        showFileChooser()
                    } else {
                        Toast.makeText(this, resources.getString(R.string.read_store_permission_denied),
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        askStoragePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map: MutableMap<String, Boolean> ->
            if (!map.values.contains(false)){
                showFileChooser()
            } else {
                Toast.makeText(this, resources.getString(R.string.read_store_permission_denied),
                    Toast.LENGTH_LONG).show()
            }
        }

        chooseFileResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                txtFileName.visibility = View.VISIBLE
                val data: Intent? = result.data
                selectedFileUri = data?.data
                if (selectedFileUri != null) {
                    val file = DocumentFile.fromSingleUri(this, selectedFileUri!!)
                    txtFileName.text = file?.name
                    selectedFileType = AppConstants.getFileExtension(this, selectedFileUri).toString()
                }
            }
        }

        btnSave.setOnClickListener(this)
        txtUploadFile.setOnClickListener(this)

    }

    private fun init() {
        toolbar = binding.toolbarUploadNoteActivity
        spinnerLevel = binding.uploadSpinnerLevel
        etNoteSubject = binding.etSubject
        etNoteTitle = binding.etTitle
        etNoteDescription = binding.etNoteDescription
        etNoteBody = binding.etNoteBody
        txtFileName = binding.tvFileName
        txtUploadFile = binding.tvUploadFile
        btnSave = binding.btnSaveUpload

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

    override fun onClick(view: View?) {
        if (view != null){
            when (view.id) {
                R.id.btn_save_upload -> {
                    saveNote()
                }

                R.id.tv_upload_file -> {
                    checkPermission()
                }
            }
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showFileChooser()
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
                showFileChooser()
            }else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                askStoragePermissions.launch(permissions)
            }
        }
    }


    private fun saveNote() {
        noteLevel = spinnerLevel.selectedItem.toString()
        noteSubject =  etNoteSubject.text.toString()
        noteTitle = etNoteTitle.text.toString()
        noteDescription = etNoteDescription.text.toString()
        noteBody = etNoteBody.text.toString()


        if (TextUtils.isEmpty(noteTitle)){
            setResult(Activity.RESULT_CANCELED, resultIntent)
        }
        if ( TextUtils.isEmpty(noteDescription) && TextUtils.isEmpty(noteBody)) {
            setResult(Activity.RESULT_CANCELED, resultIntent)
        } else {
            if (selectedFileUri != null){
                showProgressDialog(resources.getString(R.string.please_wait))
                val fileExtension = AppConstants.getFileExtension(this, selectedFileUri)
                if (fileExtension != null) {
                    val storagePath = AppConstants.NOTES + "/" + noteLevel + "/" + noteSubject + "/" + noteTitle
                    val storageRef = "$storagePath${System.currentTimeMillis()}.${fileExtension}"
                    viewModel.uploadFile( selectedFileUri, fileExtension, storageRef) {
                        uploadedFileURI = it.toString()
                        uploadNote()
                    }
                }

            } else {
                showProgressDialog(resources.getString(R.string.please_wait))
                uploadNote()
            }

        }
    }

    private fun uploadNote() {
        val newNote = Note(
            UUID.randomUUID().toString(),
            noteSubject,
            noteTitle,
            noteDescription,
            noteBody,
            uploadedFileURI,
            selectedFileType,
            noteLevel,
            Calendar.getInstance().timeInMillis,
            0.0,
            0,
            false
        )
        resultIntent.putExtra(AppConstants.UPLOAD_NOTE, newNote)
        setResult(Activity.RESULT_OK, resultIntent)
        hideProgressDialog()
        finish()
    }

    private fun showFileChooser(){
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        chooseFileResultLauncher.launch(Intent.createChooser(intent, "Select a file"))
    }
}