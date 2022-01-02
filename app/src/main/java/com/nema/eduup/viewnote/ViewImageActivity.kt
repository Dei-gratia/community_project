package com.nema.eduup.viewnote

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.appbar.MaterialToolbar
import com.nema.eduup.R
import com.nema.eduup.databinding.ActivityViewImageBinding
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.GlideLoader
import java.io.IOException
import java.lang.Exception
import java.io.ByteArrayOutputStream

class ViewImageActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = ViewImageActivity::class.qualifiedName
    private lateinit var chooseImageResultLauncher: ActivityResultLauncher<String>
    private lateinit var askStoragePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var requestStoragePermissionsResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityViewImageBinding
    private lateinit var toolbar: MaterialToolbar
    private lateinit var imgFullImage: ImageView
    private lateinit var tvImageTitle: TextView
    private lateinit var uploadProgressDialog: Dialog
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var tvProgressText: TextView
    private lateinit var btnSavePhoto: Button
    private lateinit var selectedImageBytes: ByteArray
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 0.0f
    private var selectedImageFileUri: Uri? = null
    private var imageUri: Uri? = null
    private var imageTitle = "View Image"
    private val resultIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (intent.hasExtra(AppConstants.IMAGE_URI)) {
            imageUri = intent.getStringExtra(AppConstants.IMAGE_URI)?.toUri()
        }
        if (intent.hasExtra(AppConstants.IMAGE_TITLE)) {
            imageTitle = intent.getStringExtra(AppConstants.IMAGE_TITLE).toString()
        }
        init()
        setupActionBar()
        loadImage()

        requestStoragePermissionsResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            showImageChooser()
                        } else {
                            Toast.makeText(
                                this,
                                resources.getString(R.string.read_store_permission_denied),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

        askStoragePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map: MutableMap<String, Boolean> ->
                if (!map.values.contains(false)) {
                    showImageChooser()
                } else {
                    Toast.makeText(this, resources.getString(R.string.read_store_permission_denied), Toast.LENGTH_LONG).show()
                }
            }


        chooseImageResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { chooserImageUri: Uri? ->
            Log.e(TAG, "$chooserImageUri")
            if(chooserImageUri != null) {
                try {
                    selectedImageFileUri = chooserImageUri
                    GlideLoader(this).loadImage(selectedImageFileUri!!, imgFullImage)
                    if (selectedImageFileUri != imageUri) {
                        btnSavePhoto.visibility = View.VISIBLE
                    }
                    else {
                        btnSavePhoto.visibility = View.INVISIBLE
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, resources.getString(R.string.image_selection_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnSavePhoto.setOnClickListener(this)
    }

    private fun init() {
        toolbar = binding.toolbarViewImageActivity
        tvImageTitle = binding.tvImageTitle
        btnSavePhoto = binding.btnSaveImage
        btnSavePhoto.visibility = View.INVISIBLE
        imgFullImage = binding.imgFullImage
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24)
            actionBar.title = null
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun loadImage() {
        tvImageTitle.text = imageTitle
        imageUri?.let { GlideLoader(this).loadImage(it, imgFullImage) }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_view_photo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_update_photo -> {
                checkPermission()
                return true
            }

            R.id.action_share -> {

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun compressImage() {
        if (selectedImageFileUri != null) {
            val fileExtension = AppConstants.getFileExtension(this, selectedImageFileUri)
            if (fileExtension != null) {
                val selectedImageBmp: Bitmap = if(Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(
                        contentResolver,
                        selectedImageFileUri
                    )

                }else {
                    val source = contentResolver?.let {
                        ImageDecoder.createSource(
                            it,
                            selectedImageFileUri!!
                        )
                    }
                    source?.let { ImageDecoder.decodeBitmap(it) }!!
                }
                val outputStream = ByteArrayOutputStream()
                selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                selectedImageBytes = outputStream.toByteArray()
                setImage()
            }
        }
    }


    private fun setImage() {
        if (selectedImageFileUri == null){
            setResult(Activity.RESULT_CANCELED, resultIntent)
            finish()
        }
        else {
            resultIntent.putExtra(AppConstants.SELECTED_IMAGE_BYTES, selectedImageBytes)
            resultIntent.putExtra(AppConstants.USER_IMAGE_URI, selectedImageFileUri.toString())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showImageChooser()
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(
                        String.format(
                            "package:%s",
                           applicationContext?.packageName
                        )
                    )
                    requestStoragePermissionsResultLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    requestStoragePermissionsResultLauncher.launch(intent)
                }
            }
        } else {
            if ((ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED)
                &&
                (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED)
            ) {
                showImageChooser()
            } else {
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                askStoragePermissions.launch(permissions)
            }
        }
    }

    private fun showImageChooser() {
        chooseImageResultLauncher.launch("image/*")
    }

    fun showProgressDialog() {
        uploadProgressDialog = Dialog(this)
        uploadProgressDialog.setContentView(R.layout.dialog_upload_progress)
        uploadProgressBar = uploadProgressDialog.findViewById(R.id.uploadProgressBar)
        tvProgressText = uploadProgressDialog.findViewById(R.id.tv_upload_progress)
        uploadProgressDialog.setCancelable(false)
        uploadProgressDialog.setCanceledOnTouchOutside(false)

        uploadProgressDialog.show()
    }

    fun hideProgressDialog() {
        uploadProgressDialog.dismiss()
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when(view.id) {
                R.id.btn_save_image -> {
                    Toast.makeText(this, "Save", Toast.LENGTH_LONG).show()
                    compressImage()
                }
            }
        }
    }

}