package com.nema.eduup.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.databinding.ActivityLoginBinding
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.User
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager

class LoginActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: Button
    private lateinit var firstNames: String
    private lateinit var familyName: String
    private lateinit var email: String
    private lateinit var userId: String
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>


    private val auth by lazy { FirebaseAuth.getInstance() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.clientId))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                try {
                    val account: GoogleSignInAccount? =
                        GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
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
        btnLogin.setOnClickListener(this)
        tvRegister.setOnClickListener(this)
        tvForgotPassword.setOnClickListener(this)
        btnGoogleSignIn.setOnClickListener(this)

    }

    private fun init() {
        etEmail = binding.etEmail
        etPassword = binding.etPassword
        tvForgotPassword = binding.tvForgotPassword
        tvRegister = binding.tvRegister
        btnLogin = binding.btnLogin
        btnGoogleSignIn = binding.btnGoogleSignIn
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.tv_register -> {
                    val intent = Intent(this, RegisterActivity::class.java)
                    startActivity(intent)
                }

                R.id.tv_forgot_password -> {
                    val intent = Intent(this, ForgotPasswordActivity::class.java)
                    startActivity(intent)
                }

                R.id.btnLogin -> {
                    logInRegisteredUser()
                }

                R.id.btnGoogleSignIn -> {
                    googleSignUp()
                }
            }
        }
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


        private fun logInRegisteredUser() {
        if(validateLoginDetails()) {
            if (ConnectionManager().isNetworkAvailable(this)) {
                showProgressDialog(resources.getString(R.string.please_wait))

                val email = etEmail.text.toString().trim() { it <= ' '}
                val password = etPassword.text.toString().trim { it <= ' '}

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            FirestoreUtil.getCurrentUser {
                                storeUser(it)
                                loadMainActivity()
                            }
                        } else {
                            hideProgressDialog()
                            showErrorSnackBar(task.exception!!.message.toString(), true)
                        }
                    }
            }else {
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
    }

    fun googleSignUp() {
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


    private fun googleSignUpSuccess(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken,null)
        auth.signInWithCredential(credential).addOnCompleteListener {task->
            if(task.isSuccessful) {
                val isNew = task.result!!.additionalUserInfo!!.isNewUser
                val firebaseUser: FirebaseUser = task.result!!.user!!
                firstNames = account.givenName.toString()
                familyName = account.familyName.toString()
                email = account.email.toString()
                val user = User(
                    firebaseUser.uid,
                    firstNames,
                    familyName,
                    email
                )

                if (isNew){
                    hideProgressDialog()
                    showProgressDialog("Setting up your account")

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Terms & conditions")
                        .setMessage("Seems this is your first time here \nAccept the Terms & Conditions to continue setting up you account")
                        .setPositiveButton("Accept") { _, _ ->
                            Log.d("LoadMainActivity", "not new")
                            FirestoreUtil.registerUser( user){
                                Log.d("LoadMainActivity", "new")
                                storeUser(user)
                                loadMainActivity()
                            }
                        }
                        .setNegativeButton("Decline") { _, _ ->
                            firebaseUser.delete()
                        }
                        .setCancelable(false)
                        .create()
                        .show()
                } else{
                        Log.d("LoadMainActivity", "not new")
                    FirestoreUtil.getCurrentUser {
                        storeUser(it)
                        loadMainActivity()
                    }
                }
            }
            else {
                hideProgressDialog()
                showErrorSnackBar(task.exception!!.message.toString(), true)
            }
        }

    }


    private fun validateLoginDetails(): Boolean{
        return when {
            TextUtils.isEmpty(etEmail.text.toString().trim() { it <= ' '}) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_email), true)
                etEmail.setFocusAndKeyboard()
                false
            }
            TextUtils.isEmpty(etPassword.text.toString().trim() { it <= ' '}) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_password), true)
                etPassword.setFocusAndKeyboard()
                false
            }
            else -> {
                hideKeyboard()
                true
            }

        }
    }

    private fun loadMainActivity() {
            hideProgressDialog()
            val intent = Intent(this, MainActivity::class.java)
            //intent.putExtra(Constants.FRAGMENT_TO_LOAD, Constants.PROFILE_FRAGMENT)
            startActivity(intent)
            finish()

    }

    private fun storeUser(userInfo: User) {
        val gson = Gson()
        val json = gson.toJson(userInfo)
        sharedPreferences.edit().putString(AppConstants.CURRENT_USER, json).apply()
    }



}