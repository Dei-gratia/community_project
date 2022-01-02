package com.nema.eduup.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.nema.eduup.R
import com.nema.eduup.databinding.FragmentLoginBinding
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.AppConstants.hideKeyboard
import com.nema.eduup.utils.AppConstants.isValidEmail
import com.nema.eduup.utils.AppConstants.setFocusAndKeyboard

class LoginFragment : Fragment(), View.OnClickListener, View.OnFocusChangeListener {

    private val TAG = LoginFragment::class.qualifiedName
    private lateinit var binding: FragmentLoginBinding
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
    private lateinit var user: User
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInResultLauncher: ActivityResultLauncher<Intent>
    private var isValidEmail = false
    private var isValidPassword = false

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val viewModel by lazy { ViewModelProvider(requireActivity())[AuthViewModel::class.java] }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        init()

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
        etPassword.addTextChangedListener(generalTextWatcher)
        etEmail.onFocusChangeListener = this
        etPassword.onFocusChangeListener = this
        btnLogin.setOnClickListener(this)
        tvRegister.setOnClickListener(this)
        tvForgotPassword.setOnClickListener(this)
        btnGoogleSignIn.setOnClickListener(this)

        return binding.root
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
                    loadRegisterFragment()
                }

                R.id.tv_forgot_password -> {
                    loadForgotPasswordFragment()
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

    private fun logInRegisteredUser() {
        if (validateCredentials()) {
            if (AppConstants.networkConnection(requireContext())) {
                showProgressDialog(resources.getString(R.string.please_wait))
                val email = etEmail.text.toString().trim() { it <= ' '}
                val password = etPassword.text.toString().trim { it <= ' '}
                viewModel.singInWithEmailAndPassword(email, password) { userDetails, error_message ->
                    if (userDetails != null) {
                        user = userDetails
                        loadMainActivity()
                    } else if (error_message != null) {
                        hideProgressDialog()
                        showErrorSnackBar(error_message, true)
                    }
                }
            }
        }
    }

    private fun validateCredentials(): Boolean {
        return when {
            !isValidEmail -> {
                etEmail.error = getString(R.string.enter_valid_email)
                etEmail.setFocusAndKeyboard()
                false
            }
            !isValidPassword -> {
                etPassword.error = getString(R.string.err_msg_enter_password)
                etPassword.setFocusAndKeyboard()
                false
            }
            else -> {
                hideKeyboard()
                true
            }
        }
    }

    private fun googleSignUp() {
        if (AppConstants.networkConnection(requireContext())) {
            showProgressDialog(resources.getString(R.string.please_wait))
            val signInIntent: Intent = (activity as AuthActivity).googleSignInClient.signInIntent
            googleSignInResultLauncher.launch(signInIntent)
        }
    }

    private fun googleSignUpSuccess(account: GoogleSignInAccount) {
        viewModel.signInWithGoogleAuthCredential(account) { userDetails, isNew, errorMessage ->
            hideProgressDialog()
            if (userDetails != null && isNew != null) {
                user = userDetails
                if (isNew) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Terms & conditions")
                        .setMessage("Seems this is your first time here \nAccept the Terms & Conditions to continue setting up you account")
                        .setPositiveButton("Accept") { _, _ ->
                            showProgressDialog("Setting up your account")
                            viewModel.addUserToFirestoreDatabase(user) {
                                loadMainActivity()
                            }
                        }
                        .setNegativeButton("Decline") { _, _ ->
                            viewModel.deleteUser()
                        }
                        .setCancelable(false)
                        .create()
                        .show()
                } else {
                    loadMainActivity()
                }
            }
            else if (errorMessage != null) {
                Log.d(TAG, errorMessage)
                showErrorSnackBar(errorMessage, true)
            }
        }
    }

    private fun loadRegisterFragment() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_frag).navigate(R.id.fragmentRegister)
    }

    private fun loadForgotPasswordFragment() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_frag).navigate(R.id.fragmentForgotPassword)
    }

    private fun loadMainActivity() {
        viewModel.storeUserDetails(user)
        hideProgressDialog()
        val intent = Intent(requireContext(), HomeActivity::class.java)
        //intent.putExtra(Constants.FRAGMENT_TO_LOAD, Constants.PROFILE_FRAGMENT)
        startActivity(intent)
        activity?.finish()
    }

    private val generalTextWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int,
            count: Int
        ) {
            when (s.hashCode()) {
                etEmail.text.hashCode() -> {
                    if (!etEmail.text.toString().isValidEmail()) {
                        etEmail.error = "Please enter a valid email address"
                        isValidEmail = false
                    }
                    else {
                        isValidEmail = true
                    }
                }
                etPassword.text.hashCode() -> {
                    if (etPassword.text.toString().isEmpty()) {
                        etPassword.error = "Please enter your password"
                        isValidPassword = false
                    }
                    else {
                        isValidPassword = true
                    }
                }

            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun afterTextChanged(s: Editable) {

        }
    }

    override fun onFocusChange(view: View?, hasFocus: Boolean) {
       if (view != null) {
           when (view.id) {
               R.id.etEmail, R.id.etPassword -> {
                   if (!hasFocus) {
                       hideKeyboard()
                   }
               }
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


}