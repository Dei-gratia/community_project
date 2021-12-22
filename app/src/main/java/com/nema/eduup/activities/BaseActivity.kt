package com.nema.eduup.activities

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.nema.eduup.R
import com.nema.eduup.utils.BetterActivityResult

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatDelegate
import com.nema.eduup.utils.AppConstants
import android.R.attr

import android.R.attr.theme

import android.preference.PreferenceManager
import android.util.Patterns


open class BaseActivity : AppCompatActivity() {

    private var doubleBackToExistPressedOnce = false

    private lateinit var mProgressDialog: Dialog
    private lateinit var tvProgressText: TextView
    protected val activityLauncher: BetterActivityResult<Intent, ActivityResult> by lazy { BetterActivityResult.registerActivityForResult(this) }

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
                    this, R.color.colorSnackBarError
                )
            )
        } else {
            snackBarView.setBackgroundColor(
                ContextCompat.getColor(
                    this, R.color.colorSnackBarSuccess
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

    fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

    fun isValidPassword(password: String?) : Boolean {
        password?.let {
            val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{4,}$"
            val passwordMatcher = Regex(passwordPattern)

            return passwordMatcher.find(password) != null
        } ?: return false
    }

    fun showMassage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showLongMassage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        //val theme = sharedPreferences.getString(AppConstants.APP_THEME, "1")
        //if (!attr.theme.equals(theme), " none ") {
        //    recreate()
        //}
    }


}