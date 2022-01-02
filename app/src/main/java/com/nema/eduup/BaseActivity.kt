package com.nema.eduup

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatDelegate
import com.nema.eduup.utils.AppConstants


open class BaseActivity : AppCompatActivity() {

    private var doubleBackToExistPressedOnce = false
    private lateinit var mProgressDialog: Dialog
    private lateinit var tvProgressText: TextView

    private val sharedPreferences by lazy { getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        val theme = sharedPreferences.getString(AppConstants.APP_THEME, "1")
        if (theme == "1") {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

    }

    fun showErrorSnackBar(message: String, errorMessage: Boolean) {
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view

        if(!errorMessage) {
            snackBarView.setBackgroundColor(
                ContextCompat.getColor(
                    this, R.color.colorSnackBarSuccess
                )
            )
        } else {
            snackBarView.setBackgroundColor(
                ContextCompat.getColor(
                    this, R.color.colorSnackBarError
                )
            )
        }
        snackBar.show()
    }

    fun showProgressDialog(text: String) {
        mProgressDialog = Dialog(this)
        mProgressDialog.setContentView(R.layout.dialog_progress)
        tvProgressText = mProgressDialog.findViewById(R.id.tv_progress_text)
        tvProgressText.text = text

        mProgressDialog.setCancelable(false)
        mProgressDialog.setCanceledOnTouchOutside(false)

        mProgressDialog.show()
    }

    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }

    fun doubleBackToExit() {
        if (doubleBackToExistPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExistPressedOnce = true

        Toast.makeText(this, resources.getString(R.string.please_click_back_again_to_exit), Toast.LENGTH_LONG).show()

        @Suppress("DEPRECATION")
        Handler().postDelayed({doubleBackToExistPressedOnce = false}, 2000)
    }

    override fun onResume() {
        super.onResume()
        //val theme = sharedPreferences.getString(AppConstants.APP_THEME, "1")
        //if (!attr.theme.equals(theme), " none ") {
        //    recreate()
        //}
    }


}