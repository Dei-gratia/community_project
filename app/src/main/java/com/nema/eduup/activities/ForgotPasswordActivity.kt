package com.nema.eduup.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.nema.eduup.R
import com.nema.eduup.databinding.ActivityForgotPasswordBinding
import com.nema.eduup.utils.ConnectionManager

class ForgotPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var toolbarForgotPasswordActivity: Toolbar
    private lateinit var btnSubmit: Button
    private lateinit var etEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setupActionBar()

        btnSubmit.setOnClickListener {
            val email: String = etEmail.text.toString().trim { it <= ' ' }

        }
    }

    private fun init() {
        toolbarForgotPasswordActivity = binding.toolbarForgotPasswordActivity
        btnSubmit = binding.btnSubmit
        etEmail = binding.etEmailForgotPassword
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbarForgotPasswordActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = ""
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24)
        }

        toolbarForgotPasswordActivity.setNavigationOnClickListener { onBackPressed() }

    }
}