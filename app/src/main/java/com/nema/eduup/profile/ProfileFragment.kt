package com.nema.eduup.profile

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.databinding.FragmentProfileBinding
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.AppConstants.hideKeyboard
import com.nema.eduup.utils.AppConstants.isValidEmail
import com.nema.eduup.utils.AppConstants.isValidMobile
import com.nema.eduup.utils.GlideLoader
import java.io.IOException
import java.lang.Exception
import java.net.URL
import android.view.MotionEvent
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nema.eduup.auth.AuthActivity
import com.nema.eduup.utils.AppConstants.setFocusAndKeyboard
import com.nema.eduup.utils.AppConstants.uriExist
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.HashMap
import com.nema.eduup.viewnote.ViewImageActivity


class ProfileFragment : Fragment(), View.OnClickListener, View.OnFocusChangeListener
{

    private val TAG = HomeActivity::class.qualifiedName
    private lateinit var binding: FragmentProfileBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var spinnerLevel: Spinner
    private lateinit var spinnerStream: Spinner
    private lateinit var imgUserPhoto: ImageView
    private lateinit var etFirstNames: EditText
    private lateinit var etFamilyName: EditText
    private lateinit var etNickname: EditText
    private lateinit var etEmail: EditText
    private lateinit var imgEditEmail: ImageView
    private lateinit var etMobileNumber: EditText
    private lateinit var etSchool: EditText
    private lateinit var etProgram: AutoCompleteTextView
    private lateinit var tilProgram: TextInputLayout
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var btnSave: Button
    private lateinit var pullToRefresh: SwipeRefreshLayout
    private lateinit var askStoragePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var requestStoragePermissionsResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var chooseImageResultLauncher: ActivityResultLauncher<String>
    private lateinit var startViewImageActivityForResult: ActivityResultLauncher<Intent>
    private lateinit var rlProgress: RelativeLayout
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var tvUploadProgress: TextView
    private lateinit var uploadProgressDialog: Dialog
    private lateinit var tvProgressText: TextView
    private var currentUser: User = User()
    private var selectedImageFileUri: Uri? = null
    private var userProfileImageUri: Uri? = null
    private var completeProfileSnackBarView: Snackbar? = null
    private var userProfileImageURL: String = ""
    private var isValidEmail = true
    private var isValidFirstNames = true
    private var isValidFamilyName = true
    private var isValidNickname = true
    private var isValidMobileNumber = false
    private var isValidSchool = false
    private var isValidProgram = false
    private var isImageFitToScreen = false
    private var imageUploadProgress = 0
    private var selectedImageBytes: ByteArray? = null

    private val viewModel by lazy { ViewModelProvider(requireActivity())[UserProfileViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        sharedPreferences = activity?.getSharedPreferences(
            AppConstants.EDUUP_PREFERENCES,
            Context.MODE_PRIVATE
        ) as SharedPreferences
        binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        init()
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        userProfileImageUri = sharedPreferences.getString(AppConstants.USER_IMAGE_URI, null)?.toUri()
        if (!json.isNullOrBlank()) {
            currentUser = Gson().fromJson(json, User::class.java)
            userProfileImageURL = currentUser.imageUrl
        }
        val levels = arrayOf("Select Level", "College", "A Level", "O Level", "Primary")
        val streams = arrayOf("Arts", "Commercials", "Sciences")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, levels)
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, streams)
        spinnerLevel.adapter = spinnerAdapter
        spinnerStream.adapter = adapter

        setUserDetails()
        pullToRefresh.setOnRefreshListener {
            setUserDetails()
            pullToRefresh.isRefreshing = false
        }

        var levelPosition = spinnerAdapter.getPosition("Select Level")
        if (currentUser.schoolLevel.isNotEmpty()) {
            levelPosition = spinnerAdapter.getPosition(currentUser.schoolLevel)
        }
        spinnerLevel.setSelection(levelPosition)
        if (currentUser.schoolLevel == "A Level") {
            spinnerStream.setSelection(adapter.getPosition(currentUser.program))
        }
        if (currentUser.schoolLevel == "College" && currentUser.program.isNotBlank()) {
            isValidProgram = true
            etProgram.setText(currentUser.program)
        }
        etEmail.isEnabled = false

        if (currentUser.profileCompleted == 0) {
            showCompleteProfileSnackBar()
        }

        spinnerLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val schoolLevel = spinnerLevel.selectedItem.toString()
                toggleProgram(schoolLevel)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }

        requestStoragePermissionsResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            showImageChooser()
                        } else {
                            Toast.makeText(
                                requireContext(),
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
                    Toast.makeText(requireContext(), resources.getString(R.string.read_store_permission_denied), Toast.LENGTH_LONG).show()
                }
            }

        chooseImageResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {imageUri: Uri? ->
            if(imageUri != null) {
                try {
                    selectedImageFileUri = imageUri
                    GlideLoader(requireContext()).loadImage(selectedImageFileUri!!, imgUserPhoto)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), resources.getString(R.string.image_selection_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }

        startViewImageActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            Log.e(TAG, "back")
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageBytes = result.data?.getByteArrayExtra(AppConstants.SELECTED_IMAGE_BYTES)!!
                selectedImageFileUri = result.data?.getStringExtra(AppConstants.USER_IMAGE_URI)?.toUri()
                Log.e(TAG, "onresult  ${selectedImageBytes.toString()} $selectedImageFileUri")
                onNewImageSelected(selectedImageFileUri)

            }
        }

        etFirstNames.addTextChangedListener(generalTextWatcher)
        etFamilyName.addTextChangedListener(generalTextWatcher)
        etNickname.addTextChangedListener(generalTextWatcher)
        etEmail.addTextChangedListener(generalTextWatcher)
        etMobileNumber.addTextChangedListener(generalTextWatcher)
        etSchool.addTextChangedListener(generalTextWatcher)
        etProgram.addTextChangedListener(generalTextWatcher)
        etFirstNames.onFocusChangeListener = this
        etFamilyName.onFocusChangeListener = this
        etNickname.onFocusChangeListener = this
        etEmail.onFocusChangeListener = this
        etMobileNumber.onFocusChangeListener = this
        etSchool.onFocusChangeListener = this
        etProgram.onFocusChangeListener = this
        imgUserPhoto.setOnClickListener(this)
        btnSave.setOnClickListener(this)
        imgEditEmail.setOnClickListener(this)

        return binding.root
    }


    private fun init() {
        imgUserPhoto = binding.imgUserPhoto
        imgEditEmail = binding.imgEditEmail
        etFirstNames = binding.etFirstNames
        etFamilyName = binding.etFamilyName
        etNickname = binding.etNickName
        etEmail = binding.etEmail
        etMobileNumber = binding.etMobileNumber
        etSchool = binding.etSchool
        etProgram = binding.etProgram
        tilProgram = binding.tilProgram
        rbMale = binding.rbMale
        rbFemale = binding.rbFemale
        spinnerLevel = binding.spinnerLevel
        spinnerStream = binding.spinnerStream
        btnSave = binding.btnSave
        pullToRefresh = (activity as HomeActivity).pullToRefreshHome
    }

    private fun toggleProgram(selectedLevel: String) {
        when (selectedLevel) {
            "College" -> {
                tilProgram.visibility = View.VISIBLE
                spinnerStream.visibility = View.GONE

            }
            "A Level" -> {
                spinnerStream.visibility = View.VISIBLE
                tilProgram.visibility = View.GONE
            }
            else -> {
                spinnerStream.visibility = View.GONE
                tilProgram.visibility = View.GONE
            }
        }
    }

    private fun setUserDetails() {
        etFirstNames.setText(currentUser.firstNames)
        etFamilyName.setText(currentUser.familyName)
        etEmail.setText(currentUser.email)
        etNickname.setText(currentUser.nickname)
        val mobile = currentUser.mobile
        if (mobile != 0L) {
            isValidMobileNumber = true
            etMobileNumber.setText(mobile.toString())
        }
        if (currentUser.school.isNotBlank()) {
            isValidSchool = true
        }
        etSchool.setText(currentUser.school)
        if (currentUser.imageUrl.isNotBlank()) {
            if (userProfileImageUri != null) {
                if (uriExist(requireContext(), userProfileImageUri!!)) {
                    Log.e(TAG, "from memory")
                    GlideLoader(requireContext()).loadImage(userProfileImageUri!!, imgUserPhoto)
                }
                else {
                    Log.e(TAG, "from cloud not found")
                   loadUserImageFromCloud()
                }
            }else {
                Log.e(TAG, "from cloud")
                loadUserImageFromCloud()
            }
        }


    }

    private fun loadUserImageFromCloud() {
        GlideLoader(requireContext()).loadImage(URL(currentUser.imageUrl), imgUserPhoto)
        downloadImage(currentUser.imageUrl)
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.img_user_photo -> {
                    viewFullImage()
                    //checkPermission()
                }
                R.id.img_edit_email -> {
                    changeEmailUser()
                }
                R.id.btn_save -> {
                    Toast.makeText(requireContext(), resources.getString(R.string.msg_profile_updating), Toast.LENGTH_SHORT).show()
                    uploadImage()
                }
            }
        }
    }


    private fun onNewImageSelected(imageUri: Uri?) {
        if(imageUri != null) {
            try {
                selectedImageFileUri = imageUri
                //ivUserPhoto.setImageURI(Uri.parse(selectedImageFileUri.toString()))
                GlideLoader(requireContext()).loadImage(selectedImageFileUri!!, imgUserPhoto)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), resources.getString(R.string.image_selection_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun viewFullImage() {
        val intent = Intent(requireContext(), ViewImageActivity::class.java)
        intent.putExtra(AppConstants.IMAGE_URI, userProfileImageUri.toString())
        intent.putExtra(AppConstants.IMAGE_TITLE, "Profile photo")
        startViewImageActivityForResult.launch(intent)
    }


    private fun validateRegisterDetails(): Boolean {
        val schoolLevel = spinnerLevel.selectedItem.toString()
        return when {
            !isValidFirstNames -> {
                etFirstNames.error = resources.getString(R.string.name_must_be_3_characters)
                etFirstNames.setFocusAndKeyboard()
                false
            }
            !isValidFamilyName -> {
                etFamilyName.error = resources.getString(R.string.err_msg_enter_family_name)
                etFamilyName.setFocusAndKeyboard()
                false
            }
            !isValidFamilyName -> {
                etFamilyName.error = resources.getString(R.string.err_msg_enter_family_name)
                etFamilyName.setFocusAndKeyboard()
                false
            }
            !isValidNickname -> {
                etNickname.error = resources.getString(R.string.err_nick_name_too_short)
                etNickname.setFocusAndKeyboard()
                false
            }
            !isValidEmail -> {
                etEmail.error = resources.getString(R.string.enter_valid_email)
                etEmail.setFocusAndKeyboard()
                false
            }
            !isValidMobileNumber -> {
                etMobileNumber.error = resources.getString(R.string.enter_valid_phone_number)
                etMobileNumber.setFocusAndKeyboard()
                false
            }
            !isValidSchool -> {
                etSchool.error =
                    resources.getString(R.string.enter_school)
                etSchool.setFocusAndKeyboard()
                false
            }

            schoolLevel=="Select Level" ->{
                showErrorSnackBar(resources.getString(R.string.err_msg_select_level), true)
                spinnerLevel.requestFocus()
                spinnerLevel.performClick()
                false
            }

            schoolLevel == "College" && !isValidProgram -> {
                etProgram.error =
                    resources.getString(R.string.enter_program)
                etProgram.setFocusAndKeyboard()
                false

            }


            else -> {
                hideKeyboard()
                true
            }
        }
    }

    private fun changeEmailUser() {
        val dialog = android.app.AlertDialog.Builder(context)
        dialog.setTitle("Change Email")
        dialog.setMessage("This will change the email associated with this account")
        dialog.setPositiveButton("Proceed") { _, _ ->

        }
        dialog.setNegativeButton("Cancel") { mDialog, _ ->
            mDialog.cancel()
        }
        dialog.create()
        dialog.show()
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
                            context?.applicationContext?.packageName
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
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED)
                &&
                (ContextCompat.checkSelfPermission(
                    requireContext(),
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

    private fun checkProfileImage() {
        if (validateRegisterDetails()) {
            if (selectedImageFileUri != null) {
                showImageUploadProgressDialog()
                imageUploadProgress = uploadProgressBar.progress
                val fileExtension = AppConstants.getFileExtension(requireActivity(), selectedImageFileUri)
                if (fileExtension != null) {
                    val selectedImageBmp: Bitmap = if(Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(
                            context?.contentResolver,
                            selectedImageFileUri
                        )

                    }else {
                        val source = context?.contentResolver?.let {
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
                    val storagePath = AppConstants.USER_FILES + "/" + currentUser.id + "/" + AppConstants.PROFILE_PHOTOS + "/" + "${UUID.nameUUIDFromBytes(selectedImageBytes)}.$fileExtension"
                    viewModel.uploadImage(selectedImageBytes!!, storagePath, {
                        it.observe(viewLifecycleOwner, Observer { progress ->
                            Log.e(TAG, "progress is $progress")
                            imageUploadProgress = progress.toInt()
                            uploadProgressBar.progress = imageUploadProgress
                            tvProgressText.text = imageUploadProgress.toString() + "/" + uploadProgressBar.max
                        })

                    }) { uri ->
                        userProfileImageURL = uri.toString()
                        updateUserProfileDetails()
                        hideImageUploadProgressDialog()
                    }

                }
            } else {
                updateUserProfileDetails()
            }

        }

    }

    private fun uploadImage() {
        if (validateRegisterDetails()) {
            if (selectedImageBytes != null) {
                showImageUploadProgressDialog()
                imageUploadProgress = uploadProgressBar.progress
                val fileExtension = AppConstants.getFileExtension(requireActivity(), selectedImageFileUri)
                val storagePath = AppConstants.USER_FILES + "/" + currentUser.id + "/" + AppConstants.PROFILE_PHOTOS + "/" + "${UUID.nameUUIDFromBytes(selectedImageBytes)}.$fileExtension"
                viewModel.uploadImage(selectedImageBytes!!, storagePath, {
                    it.observe(viewLifecycleOwner, Observer { progress ->
                        Log.e(TAG, "progress is $progress")
                        imageUploadProgress = progress.toInt()
                        uploadProgressBar.progress = imageUploadProgress
                        tvProgressText.text = imageUploadProgress.toString() + "/" + uploadProgressBar.max
                    })

                }) { uri ->
                    userProfileImageURL = uri.toString()
                    updateUserProfileDetails()
                    hideImageUploadProgressDialog()
                }
            } else {
                updateUserProfileDetails()
            }

        }
    }

    fun showImageUploadProgressDialog() {
        uploadProgressDialog = Dialog(requireContext())
        uploadProgressDialog.setContentView(R.layout.dialog_upload_progress)
        uploadProgressBar = uploadProgressDialog.findViewById(R.id.uploadProgressBar)
        tvProgressText = uploadProgressDialog.findViewById(R.id.tv_upload_progress)
        uploadProgressDialog.setCancelable(false)
        uploadProgressDialog.setCanceledOnTouchOutside(false)

        uploadProgressDialog.show()
    }

    fun hideImageUploadProgressDialog() {
        uploadProgressDialog.dismiss()
    }

    private fun updateUserProfileDetails() {
        showProgressDialog(resources.getString(R.string.please_wait))
        val userHashMap = HashMap<String, Any>()
        val firstNames = etFirstNames.text.toString().trim {it <= ' '}
        val familyName = etFamilyName.text.toString().trim {it <= ' '}
        val nickname = etNickname.text.toString().trim {it <= ' '}
        val mobileNumber = etMobileNumber.text.toString().trim {it <= ' '}
        val school = etSchool.text.toString().trim {it <= ' '}
        val schoolLevel = spinnerLevel.selectedItem.toString()
        var program = schoolLevel
        if (schoolLevel == "College") {
            program = etProgram.text.toString().trim {it <= ' '}
        }else if (schoolLevel == "A Level") {
            program = spinnerStream.selectedItem.toString()
        }

        val gender = if (rbMale.isChecked) {
            AppConstants.MALE
        } else {
            AppConstants.FEMALE
        }

        if (userProfileImageURL.isNotEmpty()) {
            userHashMap[AppConstants.IMAGE_URL] = userProfileImageURL
        }
        if (firstNames.isNotEmpty()) {
            userHashMap[AppConstants.FIRST_NAMES] = firstNames
        }
        if (familyName.isNotEmpty()) {
            userHashMap[AppConstants.FAMILY_NAME] = familyName
        }
        if (nickname.isNotEmpty()) {
            userHashMap[AppConstants.NICKNAME] = nickname
        }
        if (mobileNumber.isNotEmpty()) {
            userHashMap[AppConstants.MOBILE] = mobileNumber.toLong()
        }
        if (school.isNotEmpty()) {
            userHashMap[AppConstants.SCHOOL] = school
        }


        if (schoolLevel != "Select Level") {
            userHashMap[AppConstants.SCHOOL_LEVEL] = schoolLevel
        }

        if (program.isNotEmpty()) {
            userHashMap[AppConstants.PROGRAM] = program
        }
        userHashMap[AppConstants.GENDER] = gender

        userHashMap[AppConstants.COMPLETE_PROFILE] = 1

        viewModel.updateUserProfileData(userHashMap) {  user ->
            viewModel.storeUserDetails(user)

            val toast = Toast.makeText(requireContext(), resources.getString(R.string.msg_profile_update_success), Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
            if (selectedImageFileUri != null) {
                if (user.imageUrl.isNotEmpty()) {
                    downloadImage(user.imageUrl)
                }
            }
            hideProgressDialog()
        }

    }

    private fun downloadImage(imageUrl: String) {
        Log.e(TAG, "called $imageUrl")
        viewModel.downloadImage(imageUrl) {
            Log.e(TAG, "oncomplete $it")
            sharedPreferences.edit().putString(AppConstants.USER_IMAGE_URI, it).apply()
            //GlideLoader(requireContext()).loadImage(Uri.parse(it), imgUserPhoto)
        }
    }

    private val generalTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int,
            count: Int
        ) {
            when (s.hashCode()) {
                etEmail.text.hashCode() -> {
                    if (!etEmail.text.toString().isValidEmail()) {
                        etEmail.error = getString(R.string.enter_valid_email)
                        isValidEmail = false
                    } else {
                        isValidEmail = true
                    }
                }
                etFirstNames.text.hashCode() -> {
                    if (etFirstNames.text.toString().length < 3) {
                        etFirstNames.error = getString(R.string.name_must_be_3_characters)
                        isValidFirstNames = false
                    } else {
                        isValidFirstNames = true
                    }
                }
                etFamilyName.text.hashCode() -> {
                    if (etFamilyName.text.toString().isEmpty()) {
                        etFamilyName.error = getString(R.string.err_msg_enter_family_name)
                        isValidFamilyName = false
                    } else {
                        isValidFamilyName = true
                    }
                }

                etNickname.text.hashCode() -> {
                    if (etNickname.text.toString().length < 4) {
                        etNickname.error = getString(R.string.err_nick_name_too_short)
                        isValidNickname = false
                    } else {
                        isValidNickname = true
                    }
                }

                etMobileNumber.text.hashCode() -> {
                    val phone = etMobileNumber.text.toString()
                    if (!phone.isValidMobile()) {
                        etMobileNumber.error = getString(R.string.enter_valid_phone_number)
                        isValidMobileNumber = false
                    }
                    else {
                        isValidMobileNumber = true
                    }
                }

                etSchool.text.hashCode() -> {
                    if (etSchool.text.toString().isEmpty()) {
                        etSchool.error = getString(R.string.enter_program)
                        isValidSchool = false
                    }
                    else {
                        isValidSchool = true
                    }
                }

                etProgram.text.hashCode() -> {
                    if (etProgram.text.toString().isEmpty()) {
                        etProgram.error = getString(R.string.enter_program)
                        isValidProgram = false
                    }
                    else {
                        isValidProgram = true
                    }
                }
            }
        }

        override fun afterTextChanged(s: Editable?) {

        }


    }

    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        if (view != null) {
            when (view.id) {
                R.id.etEmail,R.id.et_first_names, R.id.et_family_name, R.id.et_nick_name,
                R.id.etMobileNumber, R.id.etSchool, R.id.et_program -> {
                    if (!hasFocus) {
                        hideKeyboard()
                    }
                }
            }
            completeProfileSnackBarView?.dismiss()
        }
    }

    private fun showCompleteProfileSnackBar() {
        completeProfileSnackBarView = Snackbar.make(binding.root, "Finish setting up your profile" , Snackbar.LENGTH_LONG)
        if (completeProfileSnackBarView != null) {
            val view = completeProfileSnackBarView!!.view
            val params = view.layoutParams as FrameLayout.LayoutParams
            view.setBackgroundColor(resources.getColor(R.color.colorSnackBarError))
            val tvSnackBarMessage = view.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
            tvSnackBarMessage.textAlignment = View.TEXT_ALIGNMENT_CENTER
            params.gravity = Gravity.TOP
            view.layoutParams = params
            //view.background = ContextCompat.getDrawable(requireContext(),R.drawable.app_gradient_color_background) // for custom background
            completeProfileSnackBarView!!.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
            completeProfileSnackBarView!!.show()

        }
    }

    private fun showErrorSnackBar(message: String, errorMessage: Boolean){
        (activity as HomeActivity).showErrorSnackBar(message, errorMessage)
    }

    fun hideCompleteProfileSnackBar(ev: MotionEvent){
        Log.e(TAG, "called")
        if (completeProfileSnackBarView != null) {
            if (completeProfileSnackBarView!!.isShown) {
                val sRect = Rect()
                completeProfileSnackBarView!!.view.getHitRect(sRect)

                if (!sRect.contains(ev.x.toInt(), ev.y.toInt())) {
                    completeProfileSnackBarView!!.dismiss()
                    completeProfileSnackBarView = null
                }
            }
        }
    }

    private fun showProgressDialog(text: String){
        (activity as HomeActivity).showProgressDialog(text)
    }

    private fun hideProgressDialog() {
        (activity as HomeActivity).hideProgressDialog()
    }



}