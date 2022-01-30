package com.nema.eduup.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.nema.eduup.BaseActivity
import com.nema.eduup.databinding.ActivitySplashBinding
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.utils.AppConstants

class SplashActivity : BaseActivity() {

    private val TAG = SplashActivity::class.qualifiedName
    private lateinit var sharedPreferences  : SharedPreferences
    private lateinit var binding: ActivitySplashBinding
    private lateinit var tvAppName: TextView
    private var themeMode = 2

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        themeMode = sharedPreferences.getInt(AppConstants.APP_THEME, 2)
        checkTheme()

        tvAppName = binding.txtAppNAme

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        Handler(Looper.getMainLooper()).postDelayed(
            {
                if(auth.currentUser != null){
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }else{
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                }

            }, 2000
        )

        val typeface = Typeface.createFromAsset(assets, "Montserrat-Bold.ttf")
        tvAppName.typeface = typeface
    }

    private fun checkTheme() {
        when (themeMode) {
            0 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                delegate.applyDayNight()
            }
            1 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                delegate.applyDayNight()
            }
            2 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                delegate.applyDayNight()
            }
        }
    }
}