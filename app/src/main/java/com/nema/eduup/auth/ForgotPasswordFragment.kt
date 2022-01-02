package com.nema.eduup.auth

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.nema.eduup.R
import com.nema.eduup.databinding.FragmentForgotPasswordBinding
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.AppConstants.hideKeyboard
import com.nema.eduup.utils.AppConstants.isValidEmail
import com.nema.eduup.utils.AppConstants.setFocusAndKeyboard

class ForgotPasswordFragment : Fragment(), View.OnClickListener, View.OnFocusChangeListener {

    private lateinit var binding: FragmentForgotPasswordBinding
    private lateinit var toolbarForgotPassword: Toolbar
    private lateinit var btnResetEmail: Button
    private lateinit var etEmail: EditText
    private var isValidEmail = false

    private val viewModel by lazy { ViewModelProvider(requireActivity())[AuthViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentForgotPasswordBinding.inflate(layoutInflater, container, false)
        init()

        etEmail.addTextChangedListener(generalTextWatcher)
        etEmail.onFocusChangeListener = this
        btnResetEmail.setOnClickListener(this)
        return binding.root
    }

    private fun init() {
        toolbarForgotPassword = binding.toolbarForgotPassword
        btnResetEmail = binding.btnResetPassword
        etEmail = binding.etEmailForgotPassword
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.btnResetPassword -> {
                    resetPassword()
                }
            }
        }
    }

    private fun resetPassword() {
        if (validateEmail()) {
            if (AppConstants.networkConnection(requireContext())) {
                showProgressDialog(resources.getString(R.string.please_wait))
                val email: String = etEmail.text.toString().trim { it <= ' ' }
                viewModel.resetPassword(email) { error_message ->
                    hideProgressDialog()
                    if (error_message == null) {
                        Toast.makeText(requireContext(), resources.getString(R.string.email_sent_success), Toast.LENGTH_LONG).show()
                    }
                    else {
                        showErrorSnackBar(error_message, true)
                    }
                }
            }
        }
    }


    private fun validateEmail(): Boolean {
        return when {
            !isValidEmail -> {
                etEmail.error = getString(R.string.enter_valid_email)
                etEmail.setFocusAndKeyboard()
                false
            }
            else -> {
                hideKeyboard()
                true
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
                        etEmail.error = getString(R.string.enter_valid_email)
                        isValidEmail = false
                    }
                    else {
                        isValidEmail = true
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

    private fun showProgressDialog(text: String){
        (activity as AuthActivity).showProgressDialog(text)
    }

    private fun hideProgressDialog() {
        (activity as AuthActivity).hideProgressDialog()
    }

    private fun showErrorSnackBar(message: String, errorMessage: Boolean){
        (activity as AuthActivity).showErrorSnackBar(message, errorMessage)
    }

    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        if (view != null) {
            when (view.id) {
                R.id.etEmail-> {
                    if (!hasFocus) {
                        hideKeyboard()
                    }
                }
            }
        }
    }

}