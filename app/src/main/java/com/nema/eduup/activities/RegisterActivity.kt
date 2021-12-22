package com.nema.eduup.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.nema.eduup.service.MyFirebaseMessagingService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.databinding.ActivityRegisterBinding
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.User
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager

class RegisterActivity : BaseActivity(), View.OnClickListener {

    private val TAG = RegisterActivity::class.qualifiedName
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var toolbarRegisterActivity: Toolbar
    private lateinit var etFirstNames: EditText
    private lateinit var etFamilyName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var cbTermsAndCondition: CheckBox
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var btnGoogleSignUp: Button
    private lateinit var firstNames: String
    private lateinit var familyName: String
    private lateinit var displayName: String
    private lateinit var email: String
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setupActionBar()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.clientId))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                try {
                    val account: GoogleSignInAccount? =GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                    if (account != null) {
                        googleSignUpSuccess(account)
                    }
                } catch (e: ApiException){
                    hideProgressDialog()
                    Toast.makeText(this,e.toString(), Toast.LENGTH_SHORT).show()
                }

            }else {
                hideProgressDialog()
            }
        }

        etEmail.addTextChangedListener(generalTextWatcher)
        etFirstNames.addTextChangedListener(generalTextWatcher)
        etConfirmPassword.addTextChangedListener(generalTextWatcher)
        etPassword.addTextChangedListener(generalTextWatcher)
        etFamilyName.addTextChangedListener(generalTextWatcher)

        tvLogin.setOnClickListener(this)
        btnRegister.setOnClickListener(this)
        btnGoogleSignUp.setOnClickListener(this)
    }

    private fun init() {
        toolbarRegisterActivity = binding.toolbarRegisterActivity
        etFirstNames = binding.etFirstName
        etFamilyName = binding.etLastName
        etEmail = binding.etEmail
        etPassword = binding.etPassword
        etConfirmPassword = binding.etConfirmPassword
        cbTermsAndCondition = binding.cbTermsAndCondition
        btnRegister = binding.btnRegister
        tvLogin = binding.txtLogin
        btnGoogleSignUp = binding.btnGoogleSignUp
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbarRegisterActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24)
        }

        toolbarRegisterActivity.setNavigationOnClickListener { onBackPressed() }
    }

    private val generalTextWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int,
            count: Int
        ) {
            when (s.hashCode()) {
                etEmail.text.hashCode() -> {
                    if (!etEmail.text.toString().isValidEmail()) {
                        etEmail.error = "Please enter a email address"
                    }
                }
                etFirstNames.text.hashCode() -> {
                    if (etFirstNames.text.toString().length < 3) {
                        etFirstNames.error = "Name should be at least 3 characters"
                    }
                }
                etFamilyName.text.hashCode() -> {
                    if (etFamilyName.text.toString() == "") {
                        etFamilyName.error = "Please enter valid Family Name"
                    }
                }

                etPassword.text.hashCode() -> {
                    if (!isValidPassword(etPassword.text.toString())) {
                        etPassword.error = "Password must container at least One capital letter, one number and one symbol"
                    }
                }

                etConfirmPassword.text.hashCode() -> {
                    if (etConfirmPassword.text.toString() != etPassword.text.toString()) {
                        etConfirmPassword.error = "Password must match"
                    }
                }
            }
        }

        override fun beforeTextChanged(
            s: CharSequence, start: Int, count: Int,
            after: Int
        ) {

        }

        override fun afterTextChanged(s: Editable) {

        }
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.txtLogin -> {
                    onBackPressed()
                }

                R.id.btn_register -> {
                    registerUser()
                }

                R.id.btnGoogleSignUp -> {
                    googleSignUp()
                }
            }
        }
    }


    private fun validateRegisterDetails(): Boolean {
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

            TextUtils.isEmpty(etPassword.text.toString().trim() { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_password), true)
                etPassword.setFocusAndKeyboard()
                false
            }

            TextUtils.isEmpty(etConfirmPassword.text.toString().trim() { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_confirm_password), true)
                etConfirmPassword.setFocusAndKeyboard()
                false
            }

            etPassword.text.toString().trim {it <= ' '} != etConfirmPassword.text.toString()
                .trim { it <= ' '} -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_password_and_confirm_password_mismatch), true)
                etConfirmPassword.setFocusAndKeyboard()
                false
            }


            !cbTermsAndCondition.isChecked -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_agree_terms_and_condition), true)
                cbTermsAndCondition.requestFocus()
                false
            }

            else -> {
                hideKeyboard()
                true
            }
        }
    }

    private fun registerUser(){
        if(validateRegisterDetails()) {
            if (ConnectionManager().isNetworkAvailable(this)) {
                showProgressDialog(resources.getString(R.string.please_wait))

                val email: String = etEmail.text.toString().trim() {it <= ' '}
                val password: String = etPassword.text.toString().trim() {it <= ' '}

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        hideProgressDialog()
                        if (task.isSuccessful) {
                            showProgressDialog("Setting up your account")
                            val firebaseUser: FirebaseUser = task.result!!.user!!

                            val user = User(
                                firebaseUser.uid,
                                etFirstNames.text.toString().trim() { it <= ' ' },
                                etFamilyName.text.toString().trim() { it <= ' ' },
                                etEmail.text.toString().trim() { it <= ' ' }
                            )

                            FirestoreUtil.registerUser(user) {
                                loadMainActivity()
                            }

                        } else {
                            showErrorSnackBar(task.exception!!.message.toString(), true)
                        }
                    }
            }else {
                val dialog = android.app.AlertDialog.Builder(this)
                dialog.setTitle("Error")
                dialog.setMessage("No Internet Connection")
                dialog.setPositiveButton("Open Settings") { _, _ ->
                    val settingsIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    startActivity(settingsIntent)
                }
                dialog.setNegativeButton("Exit") { _, _ ->
                    ActivityCompat.finishAffinity(this)
                }
                dialog.create()
                dialog.show()
            }

        }
    }

    fun googleSignUp() {
        if(cbTermsAndCondition.isChecked) {
            if (ConnectionManager().isNetworkAvailable(this)) {
                showProgressDialog(resources.getString(R.string.please_wait))
                val signInIntent: Intent = googleSignInClient.signInIntent
                resultLauncher.launch(signInIntent)
            } else {
                val dialog = android.app.AlertDialog.Builder(this)
                dialog.setTitle("Error")
                dialog.setMessage("No Internet Connection")
                dialog.setPositiveButton("Open Settings") { text, listener ->
                    val settingsIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    startActivity(settingsIntent)
                }
                dialog.setNegativeButton("Exit") { text, listener ->
                    ActivityCompat.finishAffinity(this)
                }
                dialog.create()
                dialog.show()
            }
        }
        else{
            showErrorSnackBar(resources.getString(R.string.err_msg_agree_terms_and_condition), true)
        }
    }


    private fun googleSignUpSuccess(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken,null)
        auth.signInWithCredential(credential).addOnCompleteListener {task->
            if(task.isSuccessful) {
                val isNew = task.result!!.additionalUserInfo!!.isNewUser
                val firebaseUser: FirebaseUser = task.result!!.user!!
                firstNames = account.givenName.toString()
                familyName = account.familyName.toString()
                email = account.email.toString()
                if (isNew){
                    hideProgressDialog()
                    showProgressDialog("Setting up your account")
                    val user = User(
                        firebaseUser.uid,
                        firstNames,
                        familyName,
                        email
                    )

                    FirestoreUtil.registerUser(user){
                        loadMainActivity()
                    }
                }
                else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Welcome back $firstNames $familyName")
                        .setMessage("Seams you already have an account")
                        .setPositiveButton("Continue") { _, _ ->
                            loadMainActivity()
                        }
                        .setCancelable(false)
                        .create()
                        .show()
                }

            }
            else {
                hideProgressDialog()
                showErrorSnackBar(task.exception!!.message.toString(), true)
            }
        }

    }

    private fun loadMainActivity() {
        FirestoreUtil.getCurrentUser {
            storeUserDetails(it)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (it.isComplete) {
                    MyFirebaseMessagingService.addTokenToFirestore(it.result.toString())
                }
            }
            hideProgressDialog()
        }

    }

    private fun storeUserDetails(userInfo: User) {
        val gson = Gson()
        val json = gson.toJson(userInfo)
        sharedPreferences.edit().putString(AppConstants.CURRENT_USER, json).apply()
    }


}