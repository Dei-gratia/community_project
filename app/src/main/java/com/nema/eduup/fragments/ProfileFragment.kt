package com.nema.eduup.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.activities.MainActivity
import com.nema.eduup.activities.viewmodels.NewDiscussionActivityViewModel
import com.nema.eduup.activities.viewmodels.UserProfileViewModel
import com.nema.eduup.databinding.FragmentProfileBinding
import com.nema.eduup.firebase.FirebaseStorageUtil
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.User
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.GlideLoader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.URL

class ProfileFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var spinnerLevel: Spinner
    private lateinit var spinnerStream: Spinner
    private lateinit var imgUserPhoto: ImageView
    private lateinit var etFirstNames: EditText
    private lateinit var etFamilyName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etMobileNumber: EditText
    private lateinit var etSchool: EditText
    private lateinit var etProgram: AutoCompleteTextView
    private lateinit var tilProgram: TextInputLayout
    private lateinit var rbMale : RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var btnSave: Button
    private lateinit var selectedImageBytes: ByteArray
    private lateinit var askStoragePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var requestStoragePermissionsResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var chooseImageResultLauncher: ActivityResultLauncher<String>
    private var currentUser: User = User()
    private var selectedImageFileUri: Uri? = null
    private var userProfileImageURL: String = ""


    private val viewModel by lazy { ViewModelProvider(requireActivity())[UserProfileViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        showProgressDialog(resources.getString(R.string.please_wait))
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
        }

        init()
        val levels = arrayOf("Select Level","College", "A Level", "O Level", "Primary")
        val streams = arrayOf("Arts", "Commercials", "Sciences")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, levels)
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, streams)
        spinnerLevel.adapter = spinnerAdapter
        spinnerStream.adapter = adapter
        setUserDetails()
        var levelPosition = spinnerAdapter.getPosition("Select Level")
        if (currentUser.schoolLevel.isNotEmpty()){
            levelPosition = spinnerAdapter.getPosition(currentUser.schoolLevel)
        }
        spinnerLevel.setSelection(levelPosition)
        if (currentUser.schoolLevel == "A Level"){
            spinnerStream.setSelection(adapter.getPosition(currentUser.program))
        }
        if (currentUser.schoolLevel == "College"){
            etProgram.setText(currentUser.program)
        }
        spinnerLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val schoolLevel = spinnerLevel.selectedItem.toString()
                toggleProgram(schoolLevel)
                //Toast.makeText(requireContext(), spinnerStream.selectedItem.toString(), Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }


        etEmail.isEnabled = false

        requestStoragePermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                       showImageChooser()
                    } else {
                        Toast.makeText(requireContext(), resources.getString(R.string.read_store_permission_denied),
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        askStoragePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map: MutableMap<String, Boolean> ->
            if (!map.values.contains(false)){
                showImageChooser()
            } else {
                Toast.makeText(requireContext(), resources.getString(R.string.read_store_permission_denied),
                    Toast.LENGTH_LONG).show()
            }
        }

        chooseImageResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {imageIri: Uri? ->
            if(imageIri != null) {
                try {
                    selectedImageFileUri = imageIri
                    //ivUserPhoto.setImageURI(Uri.parse(selectedImageFileUri.toString()))
                    GlideLoader(requireContext()).loadImage(selectedImageFileUri!!, imgUserPhoto)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), resources.getString(R.string.image_selection_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }

        imgUserPhoto.setOnClickListener(this)
        btnSave.setOnClickListener(this)
        return binding.root
    }

    private fun init(){
        imgUserPhoto = binding.imgUserPhoto
        etFirstNames = binding.etFirstName
        etFamilyName = binding.etLastName
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
    }

    private fun setUserDetails() {
        hideProgressDialog()
        etFirstNames.setText(currentUser.firstNames)
        etFamilyName.setText(currentUser.familyName)
        etEmail.setText(currentUser.email)
        val mobile = currentUser.mobile
        if (mobile != 0L){
            etMobileNumber.setText(mobile.toString())
        }
        etSchool.setText(currentUser.school)
        if (currentUser.imageUrl.isNotBlank()){
            GlideLoader(requireContext()).loadImage(URL(currentUser.imageUrl), imgUserPhoto)
        }
    }

    private fun toggleProgram(selectedLevel: String) {
        when (selectedLevel){
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

    private fun validateRegisterDetails(): Boolean {
        val schoolLevel = spinnerLevel.selectedItem.toString()
        return when {
            TextUtils.isEmpty(etFirstNames.text.toString().trim() { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_first_name), true)
                etFirstNames.setFocusAndKeyboard()
                false
            }

            TextUtils.isEmpty(etFamilyName.text.toString().trim() { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_last_name), true)
                etFamilyName.setFocusAndKeyboard()
                false
            }
            TextUtils.isEmpty(etEmail.text.toString().trim() { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_email), true)
                etEmail.setFocusAndKeyboard()
                false
            }

            TextUtils.isEmpty(etMobileNumber.text.toString().trim() { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_mobile_number), true)
                etMobileNumber.setFocusAndKeyboard()
                false
            }

            TextUtils.isEmpty(etSchool.text.toString().trim() { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_school), true)
                etSchool.setFocusAndKeyboard()
                false
            }

            schoolLevel=="Select Level" ->{
                showErrorSnackBar(resources.getString(R.string.err_msg_select_level), true)
                spinnerLevel.requestFocus()
                spinnerLevel.performClick()
                false
            }

            else -> {
                hideKeyboard()
                true
            }
        }
    }

    private fun updateUserProfileDetails() {
        Toast.makeText(requireContext(), resources.getString(R.string.msg_profile_updating), Toast.LENGTH_SHORT).show()
        val userHashMap = HashMap<String, Any>()
        val firstNames = etFirstNames.text.toString().trim {it <= ' '}
        val familyName = etFamilyName.text.toString().trim {it <= ' '}
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
            storeUserDetails(user)
            Toast.makeText(requireContext(), resources.getString(R.string.msg_profile_update_success), Toast.LENGTH_SHORT).show()
        }

        /*FirestoreUtil.updateUserProfileData(userHashMap){
            Toast.makeText(requireContext(), resources.getString(R.string.msg_profile_update_success), Toast.LENGTH_SHORT).show()
        }*/

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

    private fun showErrorSnackBar(message: String, errorMessage: Boolean){
        (activity as MainActivity).showErrorSnackBar(message, errorMessage)
    }

    private fun showProgressDialog(text: String){
        (activity as MainActivity).showProgressDialog(text)
    }

    private fun hideProgressDialog() {
        (activity as MainActivity).hideProgressDialog()
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.img_user_photo -> {
                    checkPermission()
                }

                R.id.btn_save -> {
                    saveUserDetail()
                }
            }
        }
    }

    private fun saveUserDetail() {
        if (validateRegisterDetails()) {
            if (selectedImageFileUri != null) {
                val fileExtension = AppConstants.getFileExtension(requireActivity(), selectedImageFileUri)
                if (fileExtension != null) {
                    var selectedImageBmp: Bitmap
                    val storagePath = AppConstants.USER_FILES + "/" + currentUser.id + "/" + AppConstants.PROFILE_PHOTOS
                    if(Build.VERSION.SDK_INT < 28) {
                        selectedImageBmp = MediaStore.Images.Media.getBitmap(
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
                        selectedImageBmp = source?.let { ImageDecoder.decodeBitmap(it) }!!
                    }
                    val outputStream = ByteArrayOutputStream()
                    selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    selectedImageBytes = outputStream.toByteArray()

                    viewModel.uploadImage(selectedImageBytes, storagePath) {
                        userProfileImageURL = it.toString()
                        updateUserProfileDetails()
                    }

                }
            } else {
                updateUserProfileDetails()
            }

        }

    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showImageChooser()
            }
            else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s",
                        context?.applicationContext?.packageName
                    ))
                    requestStoragePermissionsResultLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    requestStoragePermissionsResultLauncher.launch(intent)
                }
            }
        }else {
            if ((ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                &&
                (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                showImageChooser()
            }else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                askStoragePermissions.launch(permissions)
            }
        }
    }

    private fun showImageChooser() {
        chooseImageResultLauncher.launch("image/*")
    }

    private fun storeUserDetails(userInfo: User) {
        val gson = Gson()
        val json = gson.toJson(userInfo)
        sharedPreferences.edit().putString(AppConstants.CURRENT_USER, json).apply()
    }


}