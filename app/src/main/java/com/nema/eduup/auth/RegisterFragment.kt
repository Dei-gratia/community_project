package com.nema.eduup.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputLayout
import com.nema.eduup.R
import com.nema.eduup.databinding.FragmentRegisterBinding
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.AppConstants.hideKeyboard
import com.nema.eduup.utils.AppConstants.isValidEmail
import com.nema.eduup.utils.AppConstants.isValidPassword
import com.nema.eduup.utils.AppConstants.setFocusAndKeyboard

class RegisterFragment : Fragment(), View.OnClickListener, View.OnFocusChangeListener {

    private val TAG = RegisterFragment::class.qualifiedName
    private lateinit var binding: FragmentRegisterBinding
    private lateinit var toolbarRegisterActivity: Toolbar
    private lateinit var tilFirstNames: TextInputLayout
    private lateinit var tilFamilyName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etFirstNames: EditText
    private lateinit var etFamilyName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var cbTermsAndCondition: CheckBox
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var btnGoogleSignUp: Button
    private lateinit var user: User
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInResultLauncher: ActivityResultLauncher<Intent>
    private var isValidEmail = false
    private var isValidFirstNames = false
    private var isValidFamilyName = false
    private var isValidPassword = false
    private var passwordsMatch = false


    private val viewModel by lazy { ViewModelProvider(requireActivity())[AuthViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)
        init()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.clientId))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(),gso)

        googleSignInResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val data: Intent? = result.data
                try {
                    val account: GoogleSignInAccount? =
                        GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                    if (account != null) {
                        googleSignUpSuccess(account)
                    }
                } catch (e: ApiException){
                    hideProgressDialog()
                    Toast.makeText(requireContext(),e.toString(), Toast.LENGTH_SHORT).show()
                }
            }
            else {
                hideProgressDialog()
                showErrorSnackBar("Sign in Cancelled", true)
            }
        }


        etEmail.addTextChangedListener(generalTextWatcher)
        etFirstNames.addTextChangedListener(generalTextWatcher)
        etConfirmPassword.addTextChangedListener(generalTextWatcher)
        etPassword.addTextChangedListener(generalTextWatcher)
        etFamilyName.addTextChangedListener(generalTextWatcher)
        etEmail.onFocusChangeListener = this
        etFamilyName.onFocusChangeListener = this
        etFirstNames.onFocusChangeListener = this
        etPassword.onFocusChangeListener = this
        etConfirmPassword.onFocusChangeListener = this
        tvLogin.setOnClickListener(this)
        btnRegister.setOnClickListener(this)
        btnGoogleSignUp.setOnClickListener(this)
        return binding.root
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.txtLogin -> {
                    activity?.onBackPressed()
                }

                R.id.btn_register -> {
                    registerUserWithEmailAndPassword()
                }

                R.id.btnGoogleSignUp -> {
                    googleSignUp()
                }
            }
        }
    }

    private fun init() {
        etFirstNames = binding.etFirstNames
        etFamilyName = binding.etFamilyName
        etEmail = binding.etEmail
        etPassword = binding.etPassword
        etConfirmPassword = binding.etConfirmPassword
        tilFirstNames = binding.tilFirstName
        tilFamilyName = binding.tilFamilyName
        tilEmail = binding.tilEmail
        tilPassword = binding.tilPassword
        tilConfirmPassword = binding.tilConfirmPassword
        cbTermsAndCondition = binding.cbTermsAndCondition
        btnRegister = binding.btnRegister
        tvLogin = binding.txtLogin
        btnGoogleSignUp = binding.btnGoogleSignUp
    }

    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        if (view != null) {
            when (view.id) {
                R.id.etEmail,R.id.et_first_names, R.id.et_family_name,
                R.id.etEmailForgotPassword, R.id.etPassword -> {
                    if (!hasFocus) {
                        hideKeyboard()
                    }
                }
            }
        }
    }

    private fun registerUserWithEmailAndPassword() {
        if (validateUserDetails()) {
            if (AppConstants.networkConnection(requireContext())) {
                showProgressDialog(resources.getString(R.string.please_wait))
                val firstNames = etFirstNames.text.toString().trim() { it <= ' ' }
                val familyName = etFamilyName.text.toString().trim() { it <= ' ' }
                val email: String = etEmail.text.toString().trim() {it <= ' '}
                val password: String = etPassword.text.toString().trim() {it <= ' '}
                val newUser = User (
                    "",
                    firstNames,
                    familyName,
                    "$firstNames $familyName",
                    email
                        )
                viewModel.registerUserWithPasswordAndEmail(newUser, password) { userDetails, error_message ->
                    if (userDetails != null) {
                        user = userDetails
                        loadMainActivity()
                    }
                    else if (error_message != null){
                        hideProgressDialog()
                        showErrorSnackBar(error_message, true)
                    }
                }
            }
        }
    }

    private fun googleSignUp() {
        if(cbTermsAndCondition.isChecked) {
            if (AppConstants.networkConnection(requireContext())) {
                showProgressDialog(resources.getString(R.string.please_wait))
                val signInIntent: Intent = (activity as AuthActivity).googleSignInClient.signInIntent
                googleSignInResultLauncher.launch(signInIntent)
            }
        }
        else{
            showErrorSnackBar(resources.getString(R.string.err_msg_agree_terms_and_condition), true)
        }
    }

    private fun googleSignUpSuccess(account: GoogleSignInAccount) {
        viewModel.signInWithGoogleAuthCredential(account) { userDetails, isNew, errorMessage ->
            hideProgressDialog()
            if (userDetails != null && isNew != null) {
                user = userDetails
                if (isNew) {
                    viewModel.addUserToFirestoreDatabase(user) {
                        loadMainActivity()
                    }
                }
                else {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Welcome back ${userDetails.firstNames} ${userDetails.familyName}")
                        .setMessage("Seams you already have an account")
                        .setPositiveButton("Continue") { _, _ ->
                            loadMainActivity()
                        }
                        .setCancelable(false)
                        .create()
                        .show()
                }
            }
            else if (errorMessage != null) {
                Log.d(TAG, errorMessage)
                showErrorSnackBar(errorMessage, true)
            }
        }
    }

    private fun loadMainActivity() {
        viewModel.storeUserDetails(user)
        hideProgressDialog()
        val intent = Intent(requireContext(), HomeActivity::class.java)
        //intent.putExtra(Constants.FRAGMENT_TO_LOAD, Constants.PROFILE_FRAGMENT)
        startActivity(intent)
        activity?.finish()
    }

    private fun validateUserDetails(): Boolean{
        return when {
            !isValidFirstNames -> {
                tilFirstNames.error = resources.getString(R.string.name_must_be_3_characters)
                etFirstNames.setFocusAndKeyboard()
                false
            }
            !isValidFamilyName -> {
                tilFamilyName.error = resources.getString(R.string.err_msg_enter_family_name)
                etFamilyName.setFocusAndKeyboard()
                false
            }
            !isValidEmail -> {
                tilEmail.error = resources.getString(R.string.enter_valid_email)
                etEmail.setFocusAndKeyboard()
                false
            }
            !isValidPassword -> {
                tilPassword.error = resources.getString(R.string.password_must_be)
                etPassword.setFocusAndKeyboard()
                false
            }
            !passwordsMatch -> {
                tilConfirmPassword.error = resources.getString(R.string.err_msg_password_and_confirm_password_mismatch)
                etConfirmPassword.setFocusAndKeyboard()
                false
            }

            !cbTermsAndCondition.isChecked -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_agree_terms_and_condition), true)
                cbTermsAndCondition.error = resources.getString(R.string.err_msg_agree_terms_and_condition)
                cbTermsAndCondition.requestFocus()
                false
            }

            else -> {
                cbTermsAndCondition.error = null
                hideKeyboard()
                true
            }
        }
    }

    private fun showErrorSnackBar(message: String, errorMessage: Boolean){
        (activity as AuthActivity).showErrorSnackBar(message, errorMessage)
    }

    private fun showProgressDialog(text: String){
        (activity as AuthActivity).showProgressDialog(text)
    }

    private fun hideProgressDialog() {
        (activity as AuthActivity).hideProgressDialog()
    }

    private val generalTextWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int,
            count: Int
        ) {
            when (s.hashCode()) {
                etEmail.text.hashCode() -> {
                    if (!etEmail.text.toString().isValidEmail()) {
                        tilEmail.error = getString(R.string.enter_valid_email)
                        isValidEmail = false
                    }
                    else {
                        isValidEmail = true
                        tilEmail.error = null
                    }
                }
                etFirstNames.text.hashCode() -> {
                    if (etFirstNames.text.toString().length < 3) {
                        tilFirstNames.error = getString(R.string.name_must_be_3_characters)
                        isValidFirstNames = false
                    }
                    else {
                        isValidFirstNames = true
                        tilFirstNames.error = null
                    }
                }
                etFamilyName.text.hashCode() -> {
                    if (etFamilyName.text.toString().isEmpty()) {
                        tilFamilyName.error = getString(R.string.err_msg_enter_family_name)
                        isValidFamilyName = false
                    }
                    else {
                        isValidFamilyName = true
                        tilFamilyName.error = null
                    }
                }

                etPassword.text.hashCode() -> {
                    if (!isValidPassword(etPassword.text.toString()) || etPassword.text.toString().length < 8) {
                        tilPassword.error = getString(R.string.password_must_be)
                        isValidPassword = false
                    }
                    else {
                        isValidPassword = true
                        tilPassword.error = null
                    }
                }

                etConfirmPassword.text.hashCode() -> {
                    passwordsMatch = etConfirmPassword.text.toString() == etPassword.text.toString()
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

}