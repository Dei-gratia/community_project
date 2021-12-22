package com.nema.eduup.activities

import android.app.Application
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.nema.eduup.databinding.ActivitySplashBinding
import com.nema.eduup.firebase.FirestoreUtil

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var tvAppName: TextView

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }else{
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }

            }, 2000
        )

        val typeface = Typeface.createFromAsset(assets, "Montserrat-Bold.ttf")
        tvAppName.typeface = typeface
    }
}